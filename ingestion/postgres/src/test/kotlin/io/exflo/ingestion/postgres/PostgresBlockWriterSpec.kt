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

import com.fasterxml.jackson.databind.ObjectMapper
import io.exflo.ingestion.tracker.BlockReader
import io.kotlintest.Spec
import io.kotlintest.TestCase
import io.kotlintest.TestResult
import io.kotlintest.extensions.TopLevelTest
import io.kotlintest.specs.FunSpec
import io.mockk.every
import io.mockk.mockkClass
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.hyperledger.besu.ethereum.core.Hash
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject
import javax.sql.DataSource
import kotlin.time.ExperimentalTime

@ExperimentalTime
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PostgresBlockWriterSpec : FunSpec(), KoinTest {

  private val mapper: ObjectMapper by inject()

  private val dataSource = mockkClass(DataSource::class)

  private val blockReader = mockkClass(BlockReader::class)
    .apply {
      val self = this@apply
      every { self.chainHead() } returns Hash.ZERO
    }

  private val scope = TestCoroutineScope()

  override fun beforeSpecClass(spec: Spec, tests: List<TopLevelTest>) {
    startKoin { modules(KoinPostgresIngestionModules()) }
  }

  override fun afterSpecClass(spec: Spec, results: Map<TestCase, TestResult>) {
    stopKoin()
  }

  init {

    context("with a PostgresBlockWriter entity") {
      val writer = PostgresBlockWriter(mapper, dataSource, blockReader, ExfloPostgresCliOptions())

      scope.runBlockingTest {
        writer.run()
      }
      
    }
  }
}
