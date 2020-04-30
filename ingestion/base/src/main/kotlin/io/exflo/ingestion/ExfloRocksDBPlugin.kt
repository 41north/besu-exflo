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

import com.google.common.base.MoreObjects
import com.google.common.base.Suppliers
import io.exflo.ingestion.storage.InterceptingKeyValueStorageFactory
import io.exflo.ingestion.storage.InterceptingPrivacyKeyValueStorageFactory
import org.apache.logging.log4j.LogManager
import org.hyperledger.besu.plugin.BesuContext
import org.hyperledger.besu.plugin.BesuPlugin
import org.hyperledger.besu.plugin.services.PicoCLIOptions
import org.hyperledger.besu.plugin.services.StorageService
import org.hyperledger.besu.plugin.services.storage.rocksdb.RocksDBKeyValuePrivacyStorageFactory
import org.hyperledger.besu.plugin.services.storage.rocksdb.RocksDBKeyValueStorageFactory
import org.hyperledger.besu.plugin.services.storage.rocksdb.RocksDBMetricsFactory
import org.hyperledger.besu.plugin.services.storage.rocksdb.configuration.RocksDBCLIOptions
import org.hyperledger.besu.plugin.services.storage.rocksdb.configuration.RocksDBConfiguration
import org.hyperledger.besu.plugin.services.storage.rocksdb.configuration.RocksDBFactoryConfiguration
import picocli.CommandLine
import java.io.IOException

internal class ExfloRocksDBCliOptions {

    companion object {

        private const val MAX_OPEN_FILES_FLAG = "--Xplugin-exflo-rocksdb-max-open-files"
        private const val CACHE_CAPACITY_FLAG = "--Xplugin-exflo-rocksdb-cache-capacity"
        private const val MAX_BACKGROUND_COMPACTIONS_FLAG = "--Xplugin-exflo-rocksdb-max-background-compactions"
        private const val BACKGROUND_THREAD_COUNT_FLAG = "--Xplugin-exflo-rocksdb-background-thread-count"

        fun fromConfig(config: RocksDBConfiguration): ExfloRocksDBCliOptions =
            ExfloRocksDBCliOptions().apply {
                this.maxOpenFiles = config.maxOpenFiles
                this.cacheCapacity = config.cacheCapacity
                this.maxBackgroundCompactions = config.maxBackgroundCompactions
                this.backgroundThreadCount = config.backgroundThreadCount
            }
    }

    @CommandLine.Option(
        names = [MAX_OPEN_FILES_FLAG],
        hidden = true,
        defaultValue = "1024",
        paramLabel = "<INTEGER>",
        description = ["Max number of files RocksDB will open (default: \${DEFAULT-VALUE})"]
    )
    var maxOpenFiles = 0

    @CommandLine.Option(
        names = [CACHE_CAPACITY_FLAG],
        hidden = true,
        defaultValue = "8388608",
        paramLabel = "<LONG>",
        description = ["Cache capacity of RocksDB (default: \${DEFAULT-VALUE})"]
    )
    var cacheCapacity: Long = 0

    @CommandLine.Option(
        names = [MAX_BACKGROUND_COMPACTIONS_FLAG],
        hidden = true,
        defaultValue = "4",
        paramLabel = "<INTEGER>",
        description = ["Maximum number of RocksDB background compactions (default: \${DEFAULT-VALUE})"]
    )
    var maxBackgroundCompactions = 0

    @CommandLine.Option(
        names = [BACKGROUND_THREAD_COUNT_FLAG],
        hidden = true,
        defaultValue = "4",
        paramLabel = "<INTEGER>",
        description = ["Number of RocksDB background threads (default: \${DEFAULT-VALUE})"]
    )
    var backgroundThreadCount = 0

    fun toDomainObject(): RocksDBFactoryConfiguration = RocksDBFactoryConfiguration(
        maxOpenFiles, maxBackgroundCompactions, backgroundThreadCount, cacheCapacity
    )

    override fun toString(): String =
        MoreObjects.toStringHelper(this)
            .add("maxOpenFiles", maxOpenFiles)
            .add("cacheCapacity", cacheCapacity)
            .add("maxBackgroundCompactions", maxBackgroundCompactions)
            .add("backgroundThreadCount", backgroundThreadCount)
            .toString()
}

/**
 * This plugin intercepts calls made to [RocksDB] storage.
 *
 * The main difference between the original [RocksDbPlugin] and this one is on the construction of the factory where
 * we pass our custom [InterceptingKeyValueStorageFactory].
 */
internal class ExfloRocksDBPlugin : BesuPlugin {

    private val options: ExfloRocksDBCliOptions = ExfloRocksDBCliOptions()

    private var context: BesuContext? = null
    private var factory: InterceptingKeyValueStorageFactory? = null
    private var privacyFactory: InterceptingPrivacyKeyValueStorageFactory? = null

    private val log = LogManager.getLogger()

    override fun register(context: BesuContext) {
        log.debug("Registering plugin")
        this.context = context

        val cmdlineOptions = context.getService(PicoCLIOptions::class.java)
        check(!cmdlineOptions.isEmpty) { "Expecting a PicoCLIO options to register CLI options with, but none found." }

        cmdlineOptions.get().addPicoCLIOptions(NAME, options)
        createFactoriesAndRegisterWithStorageService()

        log.debug("Plugin registered")
    }

    override fun start() {
        log.debug("Starting plugin")
        if (factory == null) {
            log.trace("Applied configuration: {}", options.toString())
            createFactoriesAndRegisterWithStorageService()
        }
    }

    override fun stop() {
        log.debug("Stopping plugin")
        try {
            if (factory != null) {
                factory!!.close()
                factory = null
            }
        } catch (e: IOException) {
            log.error("Failed to stop plugin: {}", e.message, e)
        }
        try {
            if (privacyFactory != null) {
                privacyFactory!!.close()
                privacyFactory = null
            }
        } catch (e: IOException) {
            log.error("Failed to stop plugin: {}", e.message, e)
        }
    }

    private fun createAndRegister(service: StorageService) {
        val segments = service.allSegmentIdentifiers

        val configuration = Suppliers.memoize { options.toDomainObject() }

        val rocksDBKeyValueStorageFactory = RocksDBKeyValueStorageFactory(
            configuration,
            segments,
            RocksDBMetricsFactory.PUBLIC_ROCKS_DB_METRICS
        )

        factory = InterceptingKeyValueStorageFactory(rocksDBKeyValueStorageFactory)
        privacyFactory =
            InterceptingPrivacyKeyValueStorageFactory(RocksDBKeyValuePrivacyStorageFactory(rocksDBKeyValueStorageFactory))

        service.registerKeyValueStorage(factory)
        service.registerKeyValueStorage(privacyFactory)
    }

    private fun createFactoriesAndRegisterWithStorageService() {
        context
            ?.getService(StorageService::class.java)
            ?.ifPresentOrElse(
                { service: StorageService ->
                    createAndRegister(
                        service
                    )
                }
            ) { log.error("Failed to register KeyValueFactory due to missing StorageService.") }
    }

    companion object {
        private const val NAME = "exflo-rocksdb"
    }
}
