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
import org.apache.kafka.common.serialization.ByteBufferSerializer
import org.apache.kafka.common.serialization.LongSerializer
import org.apache.kafka.common.serialization.Serdes
import org.apache.logging.log4j.LogManager
import org.hyperledger.besu.ethereum.core.BlockHeader
import java.nio.ByteBuffer
import java.util.Properties
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

enum class HeaderType {
  NEW, FORK
}

data class HeaderUpdate(
  val type: HeaderType,
  val header: BlockHeader,
  val byteBuffer: ByteBuffer? = null
)

@ExperimentalTime
@OptIn(InternalCoroutinesApi::class)
class KafkaBlockWriter(
  classLoader: ClassLoader,
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

  private val withHeader: Boolean = cliOptions.entities.find { it == ExfloCliOptions.ProcessingLevel.HEADER } != null
  private val withBody: Boolean = cliOptions.entities.find { it == ExfloCliOptions.ProcessingLevel.BODY } != null
  private val withReceipts: Boolean =
    cliOptions.entities.find { it == ExfloCliOptions.ProcessingLevel.RECEIPTS } != null
  private val withTraces: Boolean = cliOptions.entities.find { it == ExfloCliOptions.ProcessingLevel.TRACES } != null

  private lateinit var importCache: KafkaCache<Long, String>

  private lateinit var producer: KafkaProducer<Long, ByteBuffer?>

  override suspend fun run() {

    producer = KafkaProducer<Long, ByteBuffer?>(kafkaProps)

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

    coroutineScope {

      val head = blockReader.chainHead()!!
      val header = requireNotNull(blockReader.header(head)) { "chain header cannot be null" }

      val initialSafeSyncRange = (header.number - cliOptions.initialSafeSyncBlockAmount)
        .let { if (it < BlockHeader.GENESIS_BLOCK_NUMBER) BlockHeader.GENESIS_BLOCK_NUMBER else it }
        .let { LongRange(0L, it) }

      val initialSyncChannel = Channel<HeaderUpdate>(128)

      launch { initialSyncHeaders(initialSafeSyncRange, initialSyncChannel) }

      // publish initial sync blocks and wait for completiong
      launch { publishBlocks(initialSyncChannel) }.join()

      // start live publish loop

      while (isActive) {

        val headersChannel = Channel<HeaderUpdate>()
        val publishChannel = Channel<HeaderUpdate>()

        launch { readBlockHeaders(headersChannel) }
        launch { readBlocks(headersChannel, publishChannel) }

        // wait for publishing to finish
        launch { publishBlocks(publishChannel) }.join()

        // finished an import pass, either reaching the genesis block from an initial sync, or reaching an ancestor
        // that has already been imported

        if (isActive) {
          pollInterval
            .apply {
              log.debug("Import pass complete. Waiting $this before another attempt")
              delay(this)
            }
        }
      }

    }
  }

  private suspend fun readBlocks(headerUpdates: Channel<HeaderUpdate>, blocksChannel: Channel<HeaderUpdate>) {

    for (update in headerUpdates) {

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

      // forward
      blocksChannel.send(update)

      if (!isActive) break
    }

    blocksChannel.close()
  }

  private suspend fun publishBlocks(channel: Channel<HeaderUpdate>) {
    for (update in channel) {

      val number = update.header.number

      val records = mutableListOf(
        ProducerRecord<Long, ByteBuffer?>(blocksTopic, number, update.byteBuffer)
      )

      if (update.type == HeaderType.FORK) {
        // prepend a tombstone to clear the previous value
        records.add(0, ProducerRecord<Long, ByteBuffer?>(blocksTopic, number, null))
      }

      // send
      val futures = records.map { producer.send(it) }

      // wait for confirmation
      futures.forEach { it.get() }

      // update cache
      importCache[number] = update.header.hash.toString()
    }
  }

  private suspend fun initialSyncHeaders(range: LongRange, channel: Channel<HeaderUpdate>) {
    for (number in range) {
      blockReader.header(number)
        ?.takeIf { header ->
          val cacheEntry = importCache[header.number]
          header.hash.toHexString() != cacheEntry
        }
        ?.let { HeaderUpdate(HeaderType.NEW, it) }
        ?.apply { channel.send(this) }
    }
  }

  private suspend fun readBlockHeaders(channel: Channel<HeaderUpdate>) {

    var nextHash = blockReader.chainHead()!!

    do {

      val header = blockReader.header(nextHash)!!
      val cacheEntry = importCache[header.number]

      val isNew = cacheEntry == null
      val isFork = !isNew && header.hash.toHexString() != cacheEntry

      val update = when {
        isNew -> HeaderUpdate(HeaderType.NEW, header)
        isFork -> HeaderUpdate(HeaderType.FORK, header)
        else -> null
      }

      update?.also { channel.send(it) }

      nextHash = header.parentHash

      // if not new or a fork block then we stop
    } while (isActive && (isNew || isFork))

    channel.close()
  }
}
