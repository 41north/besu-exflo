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
import io.exflo.ingestion.postgres.extensions.blockHash
import io.exflo.ingestion.postgres.extensions.blockNumber
import io.exflo.ingestion.postgres.extensions.toBlockHeaderRecord
import io.exflo.ingestion.tracker.BlockReader
import io.exflo.postgres.jooq.Tables
import io.exflo.postgres.jooq.Tables.BLOCK_HEADER
import io.exflo.postgres.jooq.tables.records.ImportQueueRecord
import io.reactivex.rxjava3.core.Emitter
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.functions.BiFunction
import io.reactivex.rxjava3.functions.Supplier
import java.sql.Timestamp
import java.time.Duration
import javax.sql.DataSource
import kotlin.system.measureTimeMillis
import org.apache.logging.log4j.LogManager
import org.hyperledger.besu.ethereum.core.BlockHeader
import org.hyperledger.besu.ethereum.core.Hash
import org.jooq.SQLDialect
import org.jooq.impl.DSL

class HeaderImportTask(
    private val objectMapper: ObjectMapper,
    private val blockReader: BlockReader,
    dataSource: DataSource
) : ImportTask {

    private val log = LogManager.getLogger()

    private val dbContext = DSL.using(dataSource, SQLDialect.POSTGRES)

    private val bufferSize = 512

    private val pollInterval = Duration.ofSeconds(1)

    @Volatile
    private var running = true

    override fun stop() {
        running = false
    }

    override fun run() {

        while (running) {

            try {

                Flowable
                    .generate(initialState, generator)
                    // flatten into a stream of individual headers
                    .flatMapIterable { it }
                    .map { header ->
                        // convert to a db record
                        val hash = header.hash
                        val totalDifficulty =
                            requireNotNull(blockReader.totalDifficulty(hash)) { "Total difficulty cannot be null, hash = $hash" }
                        header.toBlockHeaderRecord(totalDifficulty)
                    }
                    // batch the records for better db throughput
                    .buffer(bufferSize)
                    .doOnNext { records ->

                        var updateCount = 0

                        // withing the same transaction we insert the header records and add corresponding entries to the import queue
                        val elapsedMs = measureTimeMillis {

                            dbContext.transaction { txConfig ->

                                val txCtx = DSL.using(txConfig)

                                // mark any existing header entries with matching numbers as no longer being canonical

                                updateCount += txCtx.update(BLOCK_HEADER)
                                    .set(BLOCK_HEADER.IS_CANONICAL, false)
                                    .where(BLOCK_HEADER.NUMBER.`in`(records.map { it.blockNumber }))
                                    .execute()

                                // add one entry per header to the import queue

                                val now = System.currentTimeMillis()

                                records
                                    .map { r ->
                                        ImportQueueRecord()
                                            .apply {
                                                hash = r.blockHash
                                                number = r.blockNumber
                                                stage = 0
                                                timestamp = Timestamp(now)
                                            }
                                    }.apply { txCtx.batchInsert(this).execute() }

                                updateCount += records.size * 2

                                // insert new canonical headers

                                txCtx.batchInsert(records).execute()
                            }
                        }

                        log.debug("Written $updateCount records in $elapsedMs ms")
                    }
                    .doOnComplete { log.debug("Import pass complete") }
                    .takeUntil { !running }
                    .blockingSubscribe()

                log.debug("Waiting ${pollInterval.toSeconds()} sec(s) before starting another import pass")
                Thread.sleep(pollInterval.toMillis())
            } catch (t: Throwable) {
                // TODO handle any transient errors in the Flowable pipeline so that an exception isn't thrown
                log.error("Critical failure", t)
                throw t // re-throw
            }
        }

        log.info("Stopped")
    }

    private val initialState = Supplier {

        // the latest head from besu
        var head = blockReader.chainHead()!!

        // determine if ever made it back to the genesis block
        // if not we resume from there until we reach the genesis block, otherwise resume from besu's head

        val earliestEntry = dbContext
            .select(Tables.IMPORT_QUEUE.NUMBER, Tables.IMPORT_QUEUE.HASH)
            .from(Tables.IMPORT_QUEUE)
            .orderBy(Tables.IMPORT_QUEUE.NUMBER.asc())
            .limit(1)
            .fetch()
            .firstOrNull()

        val earliestNumber = earliestEntry?.value1()
        val earliestHash = earliestEntry?.let { Hash.fromHexString(it.value2()) }

        if (!(earliestNumber == null || earliestNumber == BlockHeader.GENESIS_BLOCK_NUMBER)) {
            // we did not complete the initial first pass back to genesis
            // lets restart from the earliest point we did reach
            head = blockReader.header(earliestHash!!)!!.parentHash
            log.info("Restarting header import from block number = $earliestNumber")
        }

        head
    }

    private val generator = BiFunction { hash: Hash, emitter: Emitter<List<BlockHeader>> ->

        // read the next series of headers starting from hash and propagating backwards via parent hashes
        val headers = blockReader.headersFrom(hash, 512)

        val hashStrings = headers
            .map { it.hash.toHexString() }
            .toSet()

        // check the import queue for existing entries for the headers we just read, indicating we are reaching a common ancestor
        // TODO make this logic more robust by checking where in the sequence the common ancestor was found and ensuring it was at the end

        val existingHashStrings = dbContext
            .select(Tables.IMPORT_QUEUE.HASH)
            .from(Tables.IMPORT_QUEUE)
            .where(Tables.IMPORT_QUEUE.HASH.`in`(hashStrings))
            .fetch()
            .map { it.value1() }

        // subtract any existing hashes found in the import queue from the set of hashes read from besu and filtered the headers list

        val hashStringsToInsert =
            existingHashStrings
                .takeIf { it.isNotEmpty() }
                ?.let { hashStrings.minus(it) }
                ?: hashStrings

        val filteredHeaders = headers.filter { hashStringsToInsert.contains(it.hash.toHexString()) }

        when (filteredHeaders.isNotEmpty()) {

            // emit the filtered headers list and return the parent hash of the last entry of the headers superset for the next read attempt
            true -> {
                emitter.onNext(filteredHeaders)
                headers.last().parentHash
            }

            // as we have filtered all headers we assume there is nothing left to add to the import queue from this read pass
            false -> {
                emitter.onComplete()
                hash
            }
        }
    }
}
