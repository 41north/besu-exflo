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

package io.exflo.ingestion.postgres.tasks

import com.fasterxml.jackson.databind.ObjectMapper
import io.exflo.ingestion.core.ImportTask
import io.exflo.ingestion.postgres.extensions.toEventRecords
import io.exflo.ingestion.postgres.extensions.toTransactionReceiptRecord
import io.exflo.ingestion.tracker.BlockReader
import io.exflo.postgres.jooq.Tables
import io.exflo.postgres.jooq.Tables.TRANSACTION
import io.exflo.postgres.jooq.tables.records.BlockHeaderRecord
import io.reactivex.rxjava3.core.Emitter
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.sql.Timestamp
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.sql.DataSource
import kotlin.system.measureTimeMillis
import org.apache.logging.log4j.LogManager
import org.hyperledger.besu.ethereum.core.Hash
import org.jooq.Cursor
import org.jooq.Record3
import org.jooq.SQLDialect
import org.jooq.impl.DSL

class ReceiptsImportTask(
  private val objectMapper: ObjectMapper,
  private val blockReader: BlockReader,
  dataSource: DataSource
) : ImportTask {

    private val log = LogManager.getLogger()

    private val dbContext = DSL.using(dataSource, SQLDialect.POSTGRES)

    private val pollInterval = Duration.ofSeconds(1)

    @Volatile
    private var running = true

    override fun stop() {
        running = false
    }

    override fun run() {

        while (running) {

            try {

                // reset
                var blockCount = 0

                Flowable
                    .generate(initialState, generator, disposeState)
                    .parallel()
                    .runOn(Schedulers.io())
                    .map { header ->

                        val hash = Hash.fromHexString(header.hash)
                        val receipts =
                            requireNotNull(blockReader.receipts(hash)) { "Receipts cannot be null, hash = $hash" }

                        var totalGasUsed = 0L

                        val records = dbContext.transactionResult { txConfig ->

                            val transactions = DSL.using(txConfig)
                                .select(
                                    TRANSACTION.HASH,
                                    TRANSACTION.INDEX,
                                    TRANSACTION.FROM,
                                    TRANSACTION.TO,
                                    TRANSACTION.CONTRACT_ADDRESS
                                )
                                .from(TRANSACTION)
                                .where(TRANSACTION.BLOCK_HASH.eq(header.hash))
                                .orderBy(TRANSACTION.INDEX.asc())
                                .fetchInto(TRANSACTION)

                            require(transactions.size == receipts.size) { "Transactions & receipts size mismatch" }

                            transactions.zip(receipts)
                                .map { (transaction, receipt) ->

                                    // Calculate gasUsed in this transaction and increment totalGasUsed
                                    val gasUsed = receipt.cumulativeGasUsed - totalGasUsed
                                    totalGasUsed += gasUsed

                                    val receiptRecord =
                                        receipt.toTransactionReceiptRecord(objectMapper, header, transaction, gasUsed)

                                    // important that this occurs last to allow relations to be inserted first
                                    val eventRecords = receipt.toEventRecords(header, transaction)

                                    listOf(receiptRecord) + eventRecords
                                }.flatten()
                        }

                        Pair(header, records)
                    }
                    .sequential()
                    .buffer(10, TimeUnit.SECONDS, 64)
                    .doOnNext { items ->

                        var updateCount = 0

                        val elapsedMs = measureTimeMillis {

                            val blockHashes = items.map { it.first.hash }
                            val records = items.map { it.second }.flatten()

                            dbContext.transaction { txConfig ->

                                val txCtx = DSL.using(txConfig)

                                txCtx.batchInsert(records).execute()

                                val recordsUpdated = txCtx.update(Tables.IMPORT_QUEUE)
                                    .set(Tables.IMPORT_QUEUE.STAGE, 2)
                                    .where(Tables.IMPORT_QUEUE.HASH.`in`(blockHashes))
                                    .execute()

                                updateCount = records.size + recordsUpdated
                            }
                        }

                        blockCount += items.size

                        log.debug("Written $blockCount blocks, $updateCount updates in $elapsedMs ms")
                    }
                    .doOnComplete { log.debug("Receipts import pass complete") }
                    .takeUntil { !running }
                    .blockingSubscribe()

                if (blockCount == 0) {
                    // empty pass so we wait a bit before making another improt pass
                    log.debug("Waiting ${pollInterval.toSeconds()} sec(s) before starting another import pass")
                    Thread.sleep(pollInterval.toMillis())
                }
            } catch (t: Throwable) {
                // TODO handle any transient errors in the Flowable pipeline so that an exception isn't thrown
                log.error("Critical failure", t)
                throw t // re-throw
            }
        }

        log.info("Stopped")
    }

    private val initialState = {
        dbContext
            .select(Tables.BLOCK_HEADER.HASH, Tables.BLOCK_HEADER.NUMBER, Tables.BLOCK_HEADER.TIMESTAMP)
            .from(Tables.IMPORT_QUEUE)
            .leftJoin(Tables.BLOCK_HEADER).on(Tables.IMPORT_QUEUE.HASH.eq(Tables.BLOCK_HEADER.HASH))
            .where(Tables.IMPORT_QUEUE.STAGE.eq(1))
            .orderBy(Tables.IMPORT_QUEUE.TIMESTAMP.asc())
            .limit(1024 * 10)
            .fetchLazy()
    }

    private val generator = { cursor: Cursor<Record3<String, Long, Timestamp>>, emitter: Emitter<BlockHeaderRecord> ->

        try {

            // conveniently closes the cursor if there is no more records available

            when (cursor.hasNext()) {
                true -> emitter.onNext(cursor.fetchNextInto(Tables.BLOCK_HEADER))
                false -> emitter.onComplete()
            }
        } catch (t: Throwable) {
            emitter.onError(t)
        }

        cursor
    }

    private val disposeState = { cursor: Cursor<Record3<String, Long, Timestamp>> -> cursor.close() }
}
