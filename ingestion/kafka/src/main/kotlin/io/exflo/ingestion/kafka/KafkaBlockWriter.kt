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

package io.exflo.ingestion.kafka

import com.google.flatbuffers.FlatBufferBuilder
import io.exflo.domain.serialization.toFlatBuffer
import io.exflo.ingestion.ExfloCliOptions
import io.exflo.ingestion.extensions.format
import io.exflo.ingestion.tokens.events.LogParser
import io.exflo.ingestion.tracker.BlockReader
import io.exflo.ingestion.tracker.BlockWriter
import io.kcache.KafkaCache
import io.kcache.KafkaCacheConfig
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.ByteBufferSerializer
import org.apache.kafka.common.serialization.LongSerializer
import org.apache.kafka.common.serialization.Serdes
import org.apache.logging.log4j.LogManager
import org.hyperledger.besu.ethereum.core.BlockHeader
import org.hyperledger.besu.ethereum.core.BlockHeader.GENESIS_BLOCK_NUMBER
import org.hyperledger.besu.ethereum.core.Hash
import java.nio.ByteBuffer
import java.util.Properties
import java.util.concurrent.Future
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue
import kotlin.time.seconds

@ExperimentalTime
@OptIn(InternalCoroutinesApi::class)
class KafkaBlockWriter(
  private val blockReader: BlockReader,
  private val cliOptions: ExfloKafkaCliOptions
) : BlockWriter {

  private val log = LogManager.getLogger()

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

  private val blocksTopic: String = cliOptions.blocksTopic

  private val pollInterval = 1.seconds

  private val startBlockNumber = cliOptions.earliestBlockNumber ?: BlockHeader.GENESIS_BLOCK_NUMBER

  private val withBody: Boolean = cliOptions.entities.find { it == ExfloCliOptions.ProcessingEntity.BODY } != null

  private val withReceipts: Boolean =
    cliOptions.entities.find { it == ExfloCliOptions.ProcessingEntity.RECEIPTS } != null

  private val withTraces: Boolean = cliOptions.entities.find { it == ExfloCliOptions.ProcessingEntity.TRACES } != null

  private lateinit var importCache: KafkaCache<Long, String>

  private lateinit var producer: KafkaProducer<Long, ByteBuffer?>

  val batchSize = 512

  override suspend fun run() {

    producer = KafkaProducer<Long, ByteBuffer?>(kafkaProps)

    // used to track ingestion progress
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

    coroutineScope {

      // first ensure we have the genesis block

      tryImportStartBlock()

      // start import loop

      while (isActive) {

        // add one the latest header's block number
        val startNumber = requireNotNull(latestHeaderInTopic()) { "No previous blocks found in kafka state" }
          .first + 1L

        // generate a range with the required batch size
        val range = startNumber.until(startNumber + batchSize)

        // attempt an import
        val timedImport = measureTimedValue { tryImport(range) }

        val (retryImmediately, headers) = timedImport.value
        val elapsed = timedImport.duration

        val importRate = headers
          ?.let { (it.size / elapsed.inSeconds) }
          ?: 0.0

        val first = headers?.first()
        val last = headers?.last()

        val importRangeStr =
          if (!(first == null || last == null)) {
            "${first.number}..${last.number}"
          } else {
            ""
          }

        val headerCount = headers?.size ?: 0

        log.info("Imported $headerCount headers in $elapsed: [$importRangeStr]. Rate = ${importRate.format(0)} / s ")

        if (isActive && !retryImmediately && headerCount <= 1) {
          // typically we will import 1 block every 15 seconds
          // during sync we will import more than 1 block per attempt so it makes sense to no wait and retry immediately
          log.debug("Waiting $pollInterval before retrying import")
          delay(pollInterval)
        }
      }
    }
  }

  private fun latestHeaderInTopic(): Pair<Long, Hash>? =
    importCache.descendingCache()
      .takeIf { cache -> cache.isNotEmpty() }
      ?.firstKey()
      ?.let { number -> Pair(number, Hash.fromHexString(importCache[number])) }

  private suspend fun tryImportStartBlock() {
    if (latestHeaderInTopic() == null) {
      val (_, headers) = tryImport(LongRange(startBlockNumber, startBlockNumber))
      check(headers != null && headers.size == 1) { "Failed to import start block" }
    }
  }

  private suspend fun tryImport(range: LongRange): Pair<Boolean, List<BlockHeader>?> {

    log.debug("Attempting to import $range")

    val headers = blockReader.headers(range)

    if (headers.isEmpty()) {
      return Pair(false, null)
    }

    val firstHeader = headers.first()

    // first check if a fork has occurred which affects any blocks we have already import

    val forkDetected = latestHeaderInTopic()
      ?.let { (_, hash) -> hash != firstHeader.parentHash }
      ?: false

    if (forkDetected) {
      handleFork()

      // short circuit and allow a fresh import pass with forked state removed
      return Pair(true, null)
    }

    // next we check if we integrity check the headers we did read in case a fork occurred in the middle

    if (!checkBatchIntegrity(headers.asReversed().drop(1), headers.last())) {
      // best to simply stop this import pass and allow for a retry after the poll interval
      return Pair(false, null)
    }

    return coroutineScope {

      val blocksChannel = Channel<Triple<Long, Hash, ByteBuffer>>(headers.size)

      launch { readBlocks(headers, blocksChannel) }
      launch { publishBlocks(blocksChannel) }.join()

      Pair(false, headers)
    }
  }

  private fun checkBatchIntegrity(headers: List<BlockHeader>, child: BlockHeader): Boolean =
    if (headers.isEmpty()) {
      true
    } else {
      val parent = headers.first()
      parent.hash == child.parentHash && checkBatchIntegrity(headers.drop(1), parent)
    }

  private fun findForkHeader(): BlockHeader? {

    val descendingCache = importCache.descendingCache()

    var forkHeader: BlockHeader? = null

    for (entry in descendingCache) {

      val (number, hash) = entry

      val header =
        requireNotNull(blockReader.header(number)) {
          "Could not read header from block chain with number = $number"
        }

      val isDifferent = header.hash != Hash.fromHexString(hash)

      if (isDifferent) {
        forkHeader = header
      } else {
        break
      }
    }

    return forkHeader
  }

  private fun handleFork() {

    val forkHeader = requireNotNull(findForkHeader()) { "Fork header not found" }
    val forkNumber = forkHeader.number

    val descendingCache = importCache.descendingCache()
    val latestNumber = descendingCache.firstKey()

    val futures = (latestNumber downTo forkNumber)
      .map { number ->
        // publish a tombstone to allow reversing any state in the correct reverse order of how the state was accumulated
        Pair(number, producer.send(ProducerRecord(blocksTopic, number, null)))
      }

    // wait for publishing to be confirmed and then remove from import cache
    futures
      .map { (number, future) ->
        future.get()
        number
      }
      .forEach { number -> importCache.remove(number) }

    importCache.flush()

    log.info("Fork detected. State reset to ${forkHeader.number - 1}")
  }

  private suspend fun readBlocks(headers: List<BlockHeader>, blocksChannel: Channel<Triple<Long, Hash, ByteBuffer>>) {

    for (header in headers) {

      try {

        val update = header.hash
          .let { blockReader.fullBlock(header, withBody, withReceipts, withTraces) }
          ?.let { block ->
            FlatBufferBuilder(1024)
              .let { bb ->
                val root = block.toFlatBuffer(LogParser::parse, bb)
                bb.finish(root)
                bb.dataBuffer()
              }
          }
          ?.let { buffer -> Triple(header.number, header.hash, buffer) }
          ?: error("block could not be read, hash = ${header.hash}")

        // forward
        blocksChannel.send(update)
      } catch (t: Throwable) {
        log.error("Failed to read block. Number = ${header.number}, hash = ${header.hash}", t)
      }

      if (!isActive) break
    }

    blocksChannel.close()
  }

  private suspend fun publishBlocks(channel: Channel<Triple<Long, Hash, ByteBuffer>>) {

    val futures = mutableListOf<Triple<Long, Hash, Future<RecordMetadata>>>()

    for (update in channel) {

      val (number, hash, buffer) = update
      val record = ProducerRecord(blocksTopic, number, buffer)

      val future = producer.send(record)

      futures.add(Triple(number, hash, future))
    }

    futures
      .forEach { (number, hash, future) ->
        // await confirmation
        future.get()
        // update import cache
        importCache[number] = hash.toString()
      }

    // flush cache
    importCache.flush()
  }
}
