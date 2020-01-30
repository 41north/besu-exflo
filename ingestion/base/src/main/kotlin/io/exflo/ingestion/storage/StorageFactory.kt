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

package io.exflo.ingestion.storage

import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import org.hyperledger.besu.plugin.services.BesuConfiguration
import org.hyperledger.besu.plugin.services.MetricsSystem
import org.hyperledger.besu.plugin.services.exception.StorageException
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage
import org.hyperledger.besu.plugin.services.storage.KeyValueStorageFactory
import org.hyperledger.besu.plugin.services.storage.PrivacyKeyValueStorageFactory
import org.hyperledger.besu.plugin.services.storage.SegmentIdentifier

/**
 * Object that stores different [KeyValueStorage] in a [ConcurrentHashMap].
 */
object KeyValueStores {
    private val stores = ConcurrentHashMap<SegmentIdentifier, KeyValueStorage>()

    operator fun get(identifier: SegmentIdentifier) = stores[identifier]

    operator fun set(identifier: SegmentIdentifier, storage: KeyValueStorage) {
        stores[identifier] = storage
    }
}

/**
 * Factory for creating intercepting key-value storage instances.
 */
class InterceptingKeyValueStorageFactory(
    private val factory: KeyValueStorageFactory
) : KeyValueStorageFactory {

    override fun getName(): String = factory.name

    override fun isSegmentIsolationSupported(): Boolean = factory.isSegmentIsolationSupported

    @Throws(StorageException::class)
    override fun create(
        segment: SegmentIdentifier,
        configuration: BesuConfiguration,
        metricsSystem: MetricsSystem
    ): KeyValueStorage {
        val storage = factory.create(segment, configuration, metricsSystem)
        KeyValueStores[segment] = storage
        return storage
    }

    @Throws(IOException::class)
    override fun close() = factory.close()
}

/**
 * Factory for creating intercepting key-value storage instances.
 */
class InterceptingPrivacyKeyValueStorageFactory(
    private val factory: PrivacyKeyValueStorageFactory
) : PrivacyKeyValueStorageFactory {

    override fun getVersion(): Int = factory.version

    override fun getName(): String = factory.name

    @Throws(StorageException::class)
    override fun create(
        segment: SegmentIdentifier,
        configuration: BesuConfiguration,
        metricsSystem: MetricsSystem
    ): KeyValueStorage {
        val storage = factory.create(segment, configuration, metricsSystem)
        KeyValueStores[segment] = storage
        return storage
    }

    override fun isSegmentIsolationSupported(): Boolean = factory.isSegmentIsolationSupported

    @Throws(IOException::class)
    override fun close() = factory.close()
}
