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

package io.exflo.ingestion

import io.exflo.ingestion.KoinModules.eventsModule
import io.exflo.ingestion.KoinModules.stateModule
import io.exflo.ingestion.KoinModules.storageModule
import io.exflo.ingestion.extensions.reflektField
import io.exflo.ingestion.storage.InterceptingKeyValueStorageFactory
import io.exflo.ingestion.storage.InterceptingPrivacyKeyValueStorageFactory
import io.exflo.ingestion.tokens.precompiled.PrecompiledContractsFactory
import io.exflo.ingestion.tracker.BlockWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.hyperledger.besu.cli.BesuCommand
import org.hyperledger.besu.cli.config.EthNetworkConfig
import org.hyperledger.besu.config.GenesisConfigFile
import org.hyperledger.besu.ethereum.chain.GenesisState
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule
import org.hyperledger.besu.ethereum.mainnet.ScheduleBasedBlockHeaderFunctions
import org.hyperledger.besu.plugin.BesuContext
import org.hyperledger.besu.plugin.BesuPlugin
import org.hyperledger.besu.plugin.services.PicoCLIOptions
import org.hyperledger.besu.plugin.services.StorageService
import org.hyperledger.besu.plugin.services.storage.PrivacyKeyValueStorageFactory
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import picocli.CommandLine

@Suppress("MemberVisibilityCanBePrivate")
abstract class ExfloPlugin<T : ExfloCliOptions> : BesuPlugin {

  protected abstract val name: String

  protected abstract val options: T

  private lateinit var log: Logger

  protected lateinit var context: BesuContext

  private lateinit var blockWriter: BlockWriter

  private lateinit var commandLine: CommandLine

  private lateinit var besuCommand: BesuCommand

  private lateinit var blockWriterJob: Job

  override fun register(context: BesuContext) {

    log = LogManager.getLogger(name)

    this.context = context

    val cmdlineOptions = context.getService(PicoCLIOptions::class.java)
    check(!cmdlineOptions.isEmpty) { "Expecting a PicoCLIO options to register CLI options with, but none found." }

    val cliOptions = cmdlineOptions.get()
    cliOptions.addPicoCLIOptions(name, options)

    // TODO: Review with BESU devs how we can improve obtaining this info without resorting to reflection
    commandLine = reflektField(cliOptions, "commandLine")
    besuCommand = commandLine.commandSpec.userObject() as BesuCommand

    registerStorageInterceptors()

    log.info("Plugin registered")
  }

  private fun registerStorageInterceptors() {

    val storageService = context
      .getService(StorageService::class.java)
      .get()

    // get the original factories registered by the rocks db plugin

    val keyValueStorageFactory = storageService
      .getByName("rocksdb")
      .get()

    val privacyKeyValueStorageFactory = storageService
      .getByName("rocksdb-privacy")
      .get()

    // determine if another exflo plugin has already decorated the base storage factories and capture their
    // references, otherwise register our intercepting factories

    if (
      keyValueStorageFactory is InterceptingKeyValueStorageFactory &&
      privacyKeyValueStorageFactory is InterceptingPrivacyKeyValueStorageFactory
    ) {
      // we have already setup the interceptors in another exflo plugin
      return
    }

    val interceptingKeyValueStorageFactory =
      InterceptingKeyValueStorageFactory(keyValueStorageFactory)

    val interceptingPrivacyKeyValueStorageFactory =
      InterceptingPrivacyKeyValueStorageFactory(privacyKeyValueStorageFactory as PrivacyKeyValueStorageFactory)

    storageService.registerKeyValueStorage(interceptingKeyValueStorageFactory)
    storageService.registerKeyValueStorage(interceptingPrivacyKeyValueStorageFactory)
  }

  protected abstract fun implKoinModules(): List<Module>

  protected open fun implStart(koinApp: KoinApplication) {}

  override fun start() {

    if (!options.enabled) {
      return
    }

    log.debug("Starting plugin")

    try {

      val networkConfig = reflektField<EthNetworkConfig>(besuCommand, "ethNetworkConfig")
      val genesisConfigFile = GenesisConfigFile.fromConfig(networkConfig.genesisConfig)

      log.debug("Network id: ${networkConfig.networkId} | Network Config: $networkConfig")

      val controller = besuCommand.controllerBuilder.build()
      val protocolSchedule = controller.protocolSchedule

      // Register custom precompiled contracts
      PrecompiledContractsFactory.register(protocolSchedule, networkConfig.networkId)

      val genesisState = GenesisState.fromConfig(genesisConfigFile, protocolSchedule)

      // create a module for injecting various basic context objects
      val contextModule = module {
        single { context }
        // we capture the classloader as plugins are executed under a custom classloader and we need to
        // specify this in some places to ensure behaviour
        single<ClassLoader> { ExfloPlugin::class.java.classLoader }
        single { networkConfig }
        single { protocolSchedule }
        single { genesisState }
        single { ScheduleBasedBlockHeaderFunctions.create(get<ProtocolSchedule<Void>>()) }
      }

      // implementation specific DI modules which we combine with other standard modules
      val implKoinModules = implKoinModules()

      // start the DI system
      val koinApp = startKoin {
        if (log.isDebugEnabled) printLogger()
        modules(
          listOf(
            contextModule,
            eventsModule,
            storageModule,
            stateModule
          ) + implKoinModules
        )
      }

      // allow derived plugins to execute some start logic before we start publishing
      implStart(koinApp)

      blockWriter = koinApp.koin.get()

      this.blockWriterJob = GlobalScope
        .launch(Dispatchers.IO) {
          blockWriter.run()
        }
    } catch (ex: Exception) {
      log.error("Failed to start", ex)
    }
  }

  override fun stop() {

    if (!options.enabled) {
      return
    }

    log.debug("Stopping plugin")

    runBlocking { blockWriterJob.cancelAndJoin() }

    stopKoin()
  }
}

/**
 * Defines common cli options shared among implementors.
 */
interface ExfloCliOptions {

  var enabled: Boolean

  enum class ProcessingLevel(val level: Int) {
    HEADER(1),
    BODY(2),
    RECEIPTS(3),
    TRACES(4);
    fun isActive(other: ProcessingLevel) = other.level <= this.level
  }
}

object ExfloCliDefaultOptions {
  const val EXFLO_POSTGRES_PLUGIN_ID: String = "exflo-postgres"
  const val EXFLO_KAFKA_PLUGIN_ID: String = "exflo-kafka"
}
