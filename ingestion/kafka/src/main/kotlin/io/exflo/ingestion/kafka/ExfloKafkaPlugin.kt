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

import io.exflo.ingestion.ExfloCliDefaultOptions
import io.exflo.ingestion.ExfloCliOptions
import io.exflo.ingestion.ExfloCliOptions.ProcessableEntity
import io.exflo.ingestion.ExfloCliOptions.ProcessableEntity.BODY
import io.exflo.ingestion.ExfloCliOptions.ProcessableEntity.HEADER
import io.exflo.ingestion.ExfloCliOptions.ProcessableEntity.RECEIPTS
import io.exflo.ingestion.ExfloCliOptions.ProcessableEntity.TRACES
import io.exflo.ingestion.ExfloPlugin
import io.exflo.ingestion.tracker.BlockWriter
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.ProducerConfig
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.module
import picocli.CommandLine
import java.util.Properties
import kotlin.time.ExperimentalTime

@ExperimentalTime
class ExfloKafkaPlugin : ExfloPlugin<ExfloKafkaCliOptions>() {

  override val name = ExfloCliDefaultOptions.EXFLO_KAFKA_PLUGIN_ID

  override val options = ExfloKafkaCliOptions()

  override fun implKoinModules(): List<Module> = listOf(
    module {
      single { options }
      single<ExfloCliOptions> { options }
      factory<BlockWriter> { KafkaBlockWriter(get(), get(), get()) }
    }
  )

  override fun implStart(koinApp: KoinApplication) {

    val koin = koinApp.koin

    val options = koin.get<ExfloKafkaCliOptions>()

    options.disableTopicCreation
      .takeIf { !it }
      ?.run { createKafkaTopic() }
  }

  private fun createKafkaTopic() {

    val adminClient = AdminClient.create(
      Properties().apply {
        put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, options.bootstrapServers)
      }
    )

    val blocksTopic = NewTopic(
      options.blocksTopic,
      options.blocksTopicPartitions,
      options.blocksTopicReplicationFactor.toShort()
    ).configs(mapOf("cleanup.policy" to "compact"))

    adminClient.createTopics(listOf(blocksTopic))
    adminClient.close()
  }
}

class ExfloKafkaCliOptions : ExfloCliOptions {

  @CommandLine.Option(
    names = ["--plugin-${ExfloCliDefaultOptions.EXFLO_KAFKA_PLUGIN_ID}-enabled"],
    paramLabel = "<BOOLEAN>",
    defaultValue = "false",
    description = ["Enable this plugin"]
  )
  override var enabled: Boolean = false

  @CommandLine.Option(
    names = ["--plugin-${ExfloCliDefaultOptions.EXFLO_KAFKA_PLUGIN_ID}-start-block-override"],
    paramLabel = "<LONG>",
    description = ["Block number from which to start publishing"]
  )
  var startBlockOverride: Long? = null

  @CommandLine.Option(
    names = ["--plugin-${ExfloCliDefaultOptions.EXFLO_KAFKA_PLUGIN_ID}-processing-entities"],
    paramLabel = "<ENTITY>",
    description = ["Comma separated list of entities to include on import / ingest. Default is a predefined list"],
    split = ",",
    arity = "1..4"
  )
  var entities: List<ProcessableEntity> = listOf(HEADER, BODY, RECEIPTS, TRACES)

  @CommandLine.Option(
    names = ["--plugin-${ExfloCliDefaultOptions.EXFLO_KAFKA_PLUGIN_ID}-bootstrap-servers"],
    defaultValue = "localhost:9092",
    paramLabel = "<STRING>",
    description = ["Kafka cluster to publish into"]
  )
  var bootstrapServers: String = "localhost:9092"

  @CommandLine.Option(
    names = ["--plugin-${ExfloCliDefaultOptions.EXFLO_KAFKA_PLUGIN_ID}-client-id"],
    paramLabel = "<STRING>",
    defaultValue = "exflo",
    description = ["Client id to use with Kafka Publisher"]
  )
  var clientId: String = "exflo"

  @CommandLine.Option(
    names = ["--plugin-${ExfloCliDefaultOptions.EXFLO_KAFKA_PLUGIN_ID}-replication-factor"],
    defaultValue = "1",
    paramLabel = "<INTEGER>",
    description = ["Replication factor to use for topics"]
  )
  var replicationFactor: Int = 1

  @CommandLine.Option(
    names = ["--plugin-${ExfloCliDefaultOptions.EXFLO_KAFKA_PLUGIN_ID}-import-cache-topic"],
    defaultValue = "_exflo-import-cache",
    paramLabel = "<STRING>",
    description = ["Topic to use for import progress tracking"]
  )
  var importCacheTopic: String = "_exflo-import-cache"

  @CommandLine.Option(
    names = ["--plugin-${ExfloCliDefaultOptions.EXFLO_KAFKA_PLUGIN_ID}-blocks-topic"],
    defaultValue = "blocks",
    paramLabel = "<STRING>",
    description = ["Topic to use for chain tracker state store"]
  )
  var blocksTopic: String = "blocks"

  @CommandLine.Option(
    names = ["--plugin-${ExfloCliDefaultOptions.EXFLO_KAFKA_PLUGIN_ID}-blocks-topic-partitions"],
    defaultValue = "1",
    paramLabel = "<INTEGER>",
    description = ["Num of partitions related to blocks topic"]
  )
  var blocksTopicPartitions: Int = 1

  @CommandLine.Option(
    names = ["--plugin-${ExfloCliDefaultOptions.EXFLO_KAFKA_PLUGIN_ID}-blocks-topic-replication-factor"],
    defaultValue = "1",
    paramLabel = "<INTEGER>",
    description = ["Num of replication factor related to blocks topic"]
  )
  var blocksTopicReplicationFactor: Int = 1

  @CommandLine.Option(
    names = ["--plugin-${ExfloCliDefaultOptions.EXFLO_KAFKA_PLUGIN_ID}-ignore-kafka-topic-creation"],
    paramLabel = "<BOOLEAN>",
    description = ["Enables or disables the creation of the required Kafka topic"]
  )
  var disableTopicCreation: Boolean = false

  @CommandLine.Option(
    names = ["--plugin-${ExfloCliDefaultOptions.EXFLO_KAFKA_PLUGIN_ID}-safe-sync-block-amount"],
    defaultValue = "256",
    paramLabel = "<INTEGER>",
    description = ["Number of blocks to check during the initial safe sync check"]
  )
  var initialSafeSyncBlockAmount: Int = 256
}
