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

import com.google.common.base.Suppliers
import io.exflo.ingestion.storage.InterceptingKeyValueStorageFactory
import io.exflo.ingestion.storage.InterceptingPrivacyKeyValueStorageFactory
import java.io.IOException
import org.apache.logging.log4j.LogManager
import org.hyperledger.besu.plugin.BesuContext
import org.hyperledger.besu.plugin.BesuPlugin
import org.hyperledger.besu.plugin.services.PicoCLIOptions
import org.hyperledger.besu.plugin.services.StorageService
import org.hyperledger.besu.plugin.services.storage.rocksdb.RocksDBKeyValuePrivacyStorageFactory
import org.hyperledger.besu.plugin.services.storage.rocksdb.RocksDBKeyValueStorageFactory
import org.hyperledger.besu.plugin.services.storage.rocksdb.RocksDBMetricsFactory
import org.hyperledger.besu.plugin.services.storage.rocksdb.configuration.RocksDBCLIOptions

/**
 * This plugin intercepts calls made to [RocksDB] storage.
 *
 * The main difference between the original [RocksDbPlugin] and this one is on the construction of the factory where
 * we pass our custom [InterceptingKeyValueStorageFactory].
 */
internal class RocksDBPlugin : BesuPlugin {

    private val options: RocksDBCLIOptions = RocksDBCLIOptions.create()

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
        private const val NAME = "rocksdb"
    }
}
