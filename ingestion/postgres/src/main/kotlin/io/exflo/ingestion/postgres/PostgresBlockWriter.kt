/*
 * Copyright (c) 2020 41North.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.exflo.ingestion.postgres

import io.exflo.ingestion.ExfloCliOptions.ProcessableEntity.BODY
import io.exflo.ingestion.ExfloCliOptions.ProcessableEntity.HEADER
import io.exflo.ingestion.ExfloCliOptions.ProcessableEntity.RECEIPTS
import io.exflo.ingestion.ExfloCliOptions.ProcessableEntity.TRACES
import io.exflo.ingestion.postgres.tasks.BodyImportTask
import io.exflo.ingestion.postgres.tasks.HeaderImportTask
import io.exflo.ingestion.postgres.tasks.ReceiptsImportTask
import io.exflo.ingestion.postgres.tasks.TraceImportTask
import io.exflo.ingestion.tracker.BlockReader
import io.exflo.ingestion.tracker.BlockWriter
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import javax.sql.DataSource
import kotlin.reflect.full.primaryConstructor

class PostgresBlockWriter(
    dataSource: DataSource,
    blockReader: BlockReader,
    cliOptions: ExfloPostgresCliOptions
) : BlockWriter {

    private val executor = Executors.newCachedThreadPool {
        val factory = Executors.defaultThreadFactory()
        val thread = factory.newThread(it)
        thread.name = "ExfloExecutorThread-%d"
        thread
    }

    private val tasks =
        cliOptions.processableEntity
            .run {
                when (this) {
                    HEADER -> listOf(HeaderImportTask::class)
                    BODY -> listOf(HeaderImportTask::class, BodyImportTask::class)
                    RECEIPTS -> listOf(HeaderImportTask::class, BodyImportTask::class, ReceiptsImportTask::class)
                    TRACES -> listOf(
                        HeaderImportTask::class,
                        BodyImportTask::class,
                        ReceiptsImportTask::class,
                        TraceImportTask::class
                    )
                    else -> throw IllegalArgumentException("Invalid import entity passed!")
                }
            }
            .mapNotNull { task -> task.primaryConstructor }
            .map { task -> task.call(blockReader, dataSource) }

    private lateinit var futures: List<Future<*>>

    override fun start() {
        futures = tasks.map { executor.submit(it) }
    }

    override fun stop() {
        tasks.forEach { it.stop() }
        futures.forEach { it.get(60, TimeUnit.SECONDS) }
        executor.shutdownNow()
    }
}
