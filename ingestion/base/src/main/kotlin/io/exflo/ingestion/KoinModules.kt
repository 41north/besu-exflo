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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.exflo.ingestion.storage.KeyValueStores
import io.exflo.ingestion.tracker.BlockReader
import org.hyperledger.besu.ethereum.chain.Blockchain
import org.hyperledger.besu.ethereum.chain.BlockchainStorage
import org.hyperledger.besu.ethereum.chain.DefaultBlockchain
import org.hyperledger.besu.ethereum.chain.GenesisState
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule
import org.hyperledger.besu.ethereum.mainnet.ScheduleBasedBlockHeaderFunctions
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueStoragePrefixedKeyBlockchainStorage
import org.hyperledger.besu.ethereum.storage.keyvalue.WorldStateKeyValueStorage
import org.hyperledger.besu.ethereum.storage.keyvalue.WorldStatePreimageKeyValueStorage
import org.hyperledger.besu.ethereum.transaction.TransactionSimulator
import org.hyperledger.besu.ethereum.worldstate.WorldStateArchive
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem
import org.hyperledger.besu.plugin.BesuContext
import org.hyperledger.besu.plugin.services.BesuConfiguration
import org.hyperledger.besu.plugin.services.BesuEvents
import org.hyperledger.besu.plugin.services.MetricsSystem
import org.hyperledger.besu.plugin.services.StorageService
import org.hyperledger.besu.services.kvstore.LimitedInMemoryKeyValueStorage
import org.koin.dsl.module
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.processor.BlockReplay as BesuBlockReplay

object KoinModules {
    val eventsModule = module {

        single {
            get<BesuContext>().getService(BesuEvents::class.java).get()
        }
    }

    val storageModule = module {

        single {
            val classLoader = get<ClassLoader>()
            ObjectMapper()
                .registerModule(KotlinModule())
                .apply {
                    this.typeFactory = TypeFactory
                        .defaultInstance()
                        .withClassLoader(classLoader)
                }
        }

        single {
            val context = get<BesuContext>()
            context.getService(StorageService::class.java).get()
        }

        single {
            get<StorageService>().getByName("rocksdb")
        }

        single {
            get<BesuContext>().getService(BesuConfiguration::class.java).get()
        }

        single<BlockchainStorage> {

            val protocolSchedule: ProtocolSchedule<Void> = get()
            val segmentedStorage = KeyValueStores[KeyValueSegmentIdentifier.BLOCKCHAIN]

            KeyValueStoragePrefixedKeyBlockchainStorage(
                segmentedStorage,
                ScheduleBasedBlockHeaderFunctions.create(protocolSchedule)
            )
        }

        single<MetricsSystem> { NoOpMetricsSystem() }

        single<Blockchain> {
            DefaultBlockchain.createMutable(
                get<GenesisState>().block,
                get<BlockchainStorage>(),
                get<MetricsSystem>()
            )
        }

        single {
            val segmentedStorage = KeyValueStores[KeyValueSegmentIdentifier.WORLD_STATE]
            WorldStateKeyValueStorage(segmentedStorage)
        }

        single {
            val worldStateStorage = get<WorldStateKeyValueStorage>()
            val worldStatePreImageStorage = WorldStatePreimageKeyValueStorage(LimitedInMemoryKeyValueStorage(5000L))
            WorldStateArchive(worldStateStorage, worldStatePreImageStorage)
        }
    }

    val stateModule = module {

        single { BesuBlockReplay(get(), get(), get()) }

        single { TransactionSimulator(get(), get(), get()) }

        single { BlockReader() }
    }
}
