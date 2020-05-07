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
import io.exflo.ingestion.extensions.toBalanceDeltas
import io.exflo.ingestion.postgres.extensions.toAccountRecord
import io.exflo.ingestion.postgres.extensions.toBalanceDeltaRecord
import io.exflo.ingestion.postgres.extensions.toContractCreatedRecord
import io.exflo.ingestion.postgres.extensions.toContractDestroyedRecord
import io.exflo.ingestion.tracker.BlockReader
import io.exflo.postgres.jooq.Tables
import io.exflo.postgres.jooq.Tables.OMMER
import io.exflo.postgres.jooq.tables.records.BlockHeaderRecord
import io.exflo.postgres.jooq.tables.records.BlockTraceRecord
import io.reactivex.rxjava3.core.Emitter
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.sql.Timestamp
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.sql.DataSource
import kotlin.system.measureTimeMillis
import org.apache.logging.log4j.LogManager
import org.hyperledger.besu.ethereum.core.Address
import org.hyperledger.besu.ethereum.core.Hash
import org.jooq.Cursor
import org.jooq.JSONB
import org.jooq.Record4
import org.jooq.SQLDialect
import org.jooq.impl.DSL

class TraceImportTask(
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

                var blockCount = 0

                Flowable
                    .generate(initialState, generator, disposeState)
                    .parallel()
                    .runOn(Schedulers.io())
                    .map { header ->

                        val hash = Hash.fromHexString(header.hash)
                        val coinbase = Address.fromHexString(header.coinbase)

                        log.info("Processing traces -> Block Number: ${header.number} | Block Hash: ${header.hash}")

                        val trace = requireNotNull(blockReader.trace(hash)) { "Trace cannot be null, hash = $hash" }

                        val records = dbContext.transactionResult { txConfig ->

                            val txCtx = DSL.using(txConfig)

                            val accountRecords = blockReader
                                .touchedAccounts(trace)
                                .map { it.toAccountRecord(header) }

                            val ommerCoinbaseMap = txCtx
                                .select(OMMER.HASH, OMMER.COINBASE)
                                .from(OMMER)
                                .where(OMMER.NEPHEW_HASH.eq(header.hash))
                                .orderBy(OMMER.INDEX.asc())
                                .fetchInto(OMMER)
                                .map { Pair(Hash.fromHexString(it.hash), Address.fromHexString(it.coinbase)) }
                                .toMap()

                            val deltaRecords = trace
                                .toBalanceDeltas(hash, coinbase, ommerCoinbaseMap)
                                .map { it.toBalanceDeltaRecord(header) }

                            val contractRecords = trace.transactionTraces
                                .map { trace ->
                                    trace.contractsCreated.map { it.toContractCreatedRecord(header) } +
                                        trace.contractsDestroyed.map { it.toContractDestroyedRecord(header) }
                                }.flatten()

                            val blockTraceRecord = BlockTraceRecord()
                                .apply {
                                    this.blockHash = header.hash
                                    this.trace = JSONB.valueOf(trace.jsonTrace)
                                }

                            accountRecords + contractRecords + deltaRecords + blockTraceRecord
                        }

                        Pair(header, records)
                    }
                    .sequential()
                    .buffer(1, TimeUnit.SECONDS, 64)
                    .doOnNext { items ->

                        val blockHashes = items.map { it.first.hash }
                        val records = items.map { it.second }.flatten()

                        var updateCount = 0

                        val elapsedMs = measureTimeMillis {

                            dbContext.transaction { txConfig ->

                                val txCtx = DSL.using(txConfig)

                                txCtx.batchInsert(records).execute()

                                val recordsUpdated = txCtx.update(Tables.IMPORT_QUEUE)
                                    .set(Tables.IMPORT_QUEUE.STAGE, 3)
                                    .where(Tables.IMPORT_QUEUE.HASH.`in`(blockHashes))
                                    .execute()

                                updateCount = records.size + recordsUpdated
                            }
                        }

                        log.debug("Written $updateCount records in $elapsedMs ms")

                        blockCount += items.size
                    }
                    .doOnComplete { log.debug("Trace import pass complete") }
                    .takeUntil { !running }
                    .blockingSubscribe()

                if (blockCount == 0) {
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
            .select(
                Tables.BLOCK_HEADER.HASH,
                Tables.BLOCK_HEADER.NUMBER,
                Tables.BLOCK_HEADER.COINBASE,
                Tables.BLOCK_HEADER.TIMESTAMP
            )
            .from(Tables.IMPORT_QUEUE)
            .leftJoin(Tables.BLOCK_HEADER).on(Tables.IMPORT_QUEUE.HASH.eq(Tables.BLOCK_HEADER.HASH))
            .where(Tables.IMPORT_QUEUE.STAGE.eq(2))
            .orderBy(Tables.IMPORT_QUEUE.TIMESTAMP.asc())
            .limit(1024 * 10)
            .fetchLazy()
    }

    private val generator =
        { cursor: Cursor<Record4<String, Long, String, Timestamp>>, emitter: Emitter<BlockHeaderRecord> ->

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

    private val disposeState = { cursor: Cursor<Record4<String, Long, String, Timestamp>> -> cursor.close() }
}
