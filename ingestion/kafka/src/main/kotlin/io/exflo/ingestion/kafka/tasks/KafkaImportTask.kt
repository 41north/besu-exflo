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

package io.exflo.ingestion.kafka.tasks

import com.google.flatbuffers.FlatBufferBuilder
import io.exflo.domain.serialization.toFlatBuffer
import io.exflo.ingestion.ExfloCliOptions.ProcessableEntity.BODY
import io.exflo.ingestion.ExfloCliOptions.ProcessableEntity.HEADER
import io.exflo.ingestion.ExfloCliOptions.ProcessableEntity.RECEIPTS
import io.exflo.ingestion.ExfloCliOptions.ProcessableEntity.TRACES
import io.exflo.ingestion.core.ImportTask
import io.exflo.ingestion.kafka.ExfloKafkaCliOptions
import io.exflo.ingestion.tokens.events.LogParser
import io.exflo.ingestion.tracker.BlockReader
import io.kcache.KafkaCache
import io.kcache.KafkaCacheConfig
import io.reactivex.rxjava3.core.Emitter
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.functions.BiFunction
import io.reactivex.rxjava3.functions.Supplier
import java.nio.ByteBuffer
import java.time.Duration
import java.util.Properties
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.ByteBufferSerializer
import org.apache.kafka.common.serialization.LongSerializer
import org.apache.kafka.common.serialization.Serdes
import org.apache.logging.log4j.LogManager
import org.hyperledger.besu.ethereum.core.BlockHeader
import org.hyperledger.besu.ethereum.core.Hash
import org.koin.core.KoinComponent
import org.koin.core.inject

enum class HeaderType {
    NEW, FORK
}

data class HeaderUpdate(
  val type: HeaderType,
  val header: BlockHeader,
  val byteBuffer: ByteBuffer? = null
)

class BlockImportTask : ImportTask, KoinComponent {

    private val log = LogManager.getLogger()

    private val cliOptions: ExfloKafkaCliOptions by inject()

    private val blockReader: BlockReader by inject()

    private val importCache: KafkaCache<Long, String>

    private val kafkaProps = Properties()
        .apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, cliOptions.bootstrapServers)
            put(ProducerConfig.CLIENT_ID_CONFIG, cliOptions.clientId)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer::class.java)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteBufferSerializer::class.java)
            put(ProducerConfig.ACKS_CONFIG, "all")
            put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1024)
            put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 1024 * 1024 * 20)
        }

    private val producer = KafkaProducer<Long, ByteBuffer?>(kafkaProps)

    private val blocksTopic: String = cliOptions.blocksTopic

    @Volatile
    private var running = true

    private val batchSize = 64

    private val pollInterval = Duration.ofSeconds(1)

    private val withHeader: Boolean = cliOptions.entities.find { it == HEADER } != null
    private val withBody: Boolean = cliOptions.entities.find { it == BODY } != null
    private val withReceipts: Boolean = cliOptions.entities.find { it == RECEIPTS } != null
    private val withTraces: Boolean = cliOptions.entities.find { it == TRACES } != null

    init {

        // used to track progress ingestion progress
        importCache = Properties()
            .let { props ->

                props["kafkacache.bootstrap.servers"] = cliOptions.bootstrapServers
                props["kafkacache.topic.replication.factor"] = cliOptions.replicationFactor
                props["kafkacache.topic"] = cliOptions.importCacheTopic

                // TODO get rocksdb local cache working, currently there seems to be a version conflict

                KafkaCache(KafkaCacheConfig(props), Serdes.Long(), Serdes.String())
            }.also { it.init() }

        log.info("Syncing import cache")
        importCache.sync()
    }

    override fun run() {

        try {

            val head = blockReader.chainHead()!!
            val header = requireNotNull(blockReader.header(head)) { "chain header cannot be null" }

            val initialSafeSyncRange = (header.number - cliOptions.initialSafeSyncBlockAmount)
                .let { if (it < BlockHeader.GENESIS_BLOCK_NUMBER) BlockHeader.GENESIS_BLOCK_NUMBER else it }
                .let { LongRange(0L, it) }

            initialSyncHeaderSource(initialSafeSyncRange)
                .map(this::readBlock)
                .buffer(batchSize)
                .doOnNext(this::publishRecords)
                .doOnComplete { log.debug("Initial sync import pass complete") }
                .takeUntil { !running }
                .blockingSubscribe()

            while (running) {

                liveSyncHeaderSource
                    .flatMapIterable { it }
                    .map(this::readBlock)
                    .buffer(batchSize)
                    .doOnNext(this::publishRecords)
                    .doOnComplete { log.debug("Live sync import pass complete") }
                    .takeUntil { !running }
                    .blockingSubscribe()

                log.debug("Waiting $pollInterval before starting another import pass")
                Thread.sleep(pollInterval.toMillis())
            }
        } catch (t: Throwable) {
            // TODO handle any transient errors in the Flowable pipeline so that an exception isn't thrown
            log.error("Critical failure", t)
            throw t // re-throw
        }
    }

    override fun stop() {
        running = false
    }

    private fun readBlock(update: HeaderUpdate): HeaderUpdate =
        update.header.hash
            .let { blockReader.fullBlock(it, withHeader, withBody, withReceipts, withTraces) }
            ?.let { block ->
                FlatBufferBuilder(1024)
                    .let { bb ->
                        val root = block.toFlatBuffer(LogParser::parse, bb)
                        bb.finish(root)
                        bb.dataBuffer()
                    }
            }
            ?.let { update.copy(byteBuffer = it) } ?: error("block could not be read, hash = ${update.header.hash}")

    private fun publishRecords(records: List<HeaderUpdate>) {

        val elapsedMs = measureTimeMillis {
            // fork records should be in one contiguous block at the beginning
            val forkRecords = records.filter { (type) -> type == HeaderType.FORK }

            // send tombstones for the forked records in reverse order first
            val tombstoneProducerRecords =
                forkRecords
                    .reversed()
                    .map { (_, header) -> ProducerRecord<Long, ByteBuffer?>(blocksTopic, header.number, null) }

            val newProducerRecords =
                records
                    .map { (_, header, bytes) -> ProducerRecord<Long, ByteBuffer?>(blocksTopic, header.number, bytes) }

            // send to topic
            val futures = (tombstoneProducerRecords + newProducerRecords)
                .map { producer.send(it) }

            // wait for acks
            futures
                .forEach { it.get(60, TimeUnit.SECONDS) }

            // update cache
            records
                .forEach { (_, header, _) ->
                    importCache[header.number] = header.hash.toHexString()
                }

            importCache.flush()
        }

        val firstNumber = records.first().header.number
        val lastNumber = records.last().header.number

        log.info("Written ${records.size} records in $elapsedMs ms. First = $firstNumber, last = $lastNumber")
    }

    private fun initialSyncHeaderSource(syncRange: LongRange) = Flowable.generate(
        Supplier { syncRange },
        BiFunction { range: LongRange, emitter: Emitter<HeaderUpdate> ->

            if (range.isEmpty()) {

                emitter.onComplete()
                range
            } else {

                val number = range.first

                blockReader.header(range.first)
                    ?.takeIf {
                        val cacheEntry = importCache[it.number]
                        it.hash.toHexString() != cacheEntry
                    }
                    ?.let { HeaderUpdate(HeaderType.NEW, it) }
                    ?.apply { emitter.onNext(this) }

                LongRange(number + 1L, range.last)
            }
        }
    )

    private val liveSyncHeaderSource = Flowable.generate(
        Supplier { blockReader.chainHead()!! },
        BiFunction { hash: Hash, emitter: Emitter<List<HeaderUpdate>> ->

            var currentHash = hash
            val updates = mutableListOf<HeaderUpdate>()

            do {

                val header = blockReader.header(currentHash)!!
                val cacheEntry = importCache[header.number]

                val isNew = cacheEntry == null
                val isFork = !isNew && header.hash.toHexString() != cacheEntry

                when {
                    isNew -> updates += HeaderUpdate(HeaderType.NEW, header)
                    isFork -> updates += HeaderUpdate(HeaderType.FORK, header)
                }

                currentHash = header.parentHash
            } while (isNew || isFork)

            val sorted = updates.sortedWith(Comparator { a, b -> (a.header.number - b.header.number).toInt() })

            emitter.onNext(sorted)
            emitter.onComplete()

            hash
        }
    )
}
