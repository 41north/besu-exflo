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

package io.exflo.testutil

import java.nio.file.Path
import org.hyperledger.besu.cli.config.EthNetworkConfig
import org.hyperledger.besu.cli.config.NetworkName
import org.hyperledger.besu.config.GenesisConfigFile
import org.hyperledger.besu.ethereum.ProtocolContext
import org.hyperledger.besu.ethereum.chain.Blockchain
import org.hyperledger.besu.ethereum.chain.BlockchainStorage
import org.hyperledger.besu.ethereum.chain.DefaultBlockchain
import org.hyperledger.besu.ethereum.chain.GenesisState
import org.hyperledger.besu.ethereum.chain.MutableBlockchain
import org.hyperledger.besu.ethereum.core.BlockHeader
import org.hyperledger.besu.ethereum.core.BlockHeaderFunctions
import org.hyperledger.besu.ethereum.mainnet.MainnetProtocolSchedule
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule
import org.hyperledger.besu.ethereum.mainnet.ScheduleBasedBlockHeaderFunctions
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueStoragePrefixedKeyBlockchainStorage
import org.hyperledger.besu.ethereum.storage.keyvalue.WorldStateKeyValueStorage
import org.hyperledger.besu.ethereum.storage.keyvalue.WorldStatePreimageKeyValueStorage
import org.hyperledger.besu.ethereum.util.RawBlockIterator
import org.hyperledger.besu.ethereum.worldstate.WorldStateArchive
import org.hyperledger.besu.ethereum.worldstate.WorldStatePreimageStorage
import org.hyperledger.besu.ethereum.worldstate.WorldStateStorage
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem
import org.hyperledger.besu.services.kvstore.InMemoryKeyValueStorage
import org.hyperledger.besu.testutil.BlockTestUtil
import org.koin.dsl.module

@Suppress("MemberVisibilityCanBePrivate")
object KoinTestModules {

    val defaultTestChainResources = module {
        single { ExfloBlockTestUtil.getTestChainResources() }
    }

    val initialState = module {

        single { EthNetworkConfig.getNetworkConfig(NetworkName.DEV) }

        // genesis config file
        single {
            val chainResources = get<BlockTestUtil.ChainResources>()
            val genesisJson = chainResources.genesisURL.readText()
            GenesisConfigFile.fromConfig(genesisJson)
        }

        // genesis state
        single {
            val chainResources = get<BlockTestUtil.ChainResources>()
            val protocolSchedule = get<ProtocolSchedule<Void>>()
            val genesisJson = chainResources.genesisURL.readText()
            GenesisState.fromJson(genesisJson, protocolSchedule)
        }

        // used to read test blocks when importing
        factory {

            val chainResources = get<BlockTestUtil.ChainResources>()
            val blockHeaderFunctions = get<BlockHeaderFunctions>()
            val blocksPath = Path.of(chainResources.blocksURL.toURI())

            RawBlockIterator(blocksPath) { rlp -> BlockHeader.readFrom(rlp, blockHeaderFunctions) }
        }

        single {

            val iterator = get<RawBlockIterator>()

            var head = 0L

            iterator.forEach { block -> head = block.header.number }

            val genesisState = get<GenesisState>()
            TestChainSummary(genesisState, head)
        }
    }

    val headerFunctions = module {

        // protocol schedule
        single {
            val genesisConfigFile = get<GenesisConfigFile>()
            MainnetProtocolSchedule.fromConfig(genesisConfigFile.configOptions)
        }

        // header functions
        single {
            val protocolSchedule = get<ProtocolSchedule<Void>>()
            ScheduleBasedBlockHeaderFunctions.create(protocolSchedule)
        }
    }

    val storage = module {

        single<BlockchainStorage> {
            val keyValueStorage = InMemoryKeyValueStorage()
            val headerFunctions = get<BlockHeaderFunctions>()
            KeyValueStoragePrefixedKeyBlockchainStorage(keyValueStorage, headerFunctions)
        }

        single<WorldStateStorage> { WorldStateKeyValueStorage(InMemoryKeyValueStorage()) }

        single<WorldStatePreimageStorage> { WorldStatePreimageKeyValueStorage(InMemoryKeyValueStorage()) }
    }

    val chainState = module {

        // mutable blockchain initialised with genesis block
        single<MutableBlockchain> {

            val genesisState = get<GenesisState>()
            val genesisBlock = genesisState.block

            val blockchainStorage = get<BlockchainStorage>()

            DefaultBlockchain.createMutable(
                genesisBlock,
                blockchainStorage,
                NoOpMetricsSystem()
            )
        }

        // re-export under interface
        single<Blockchain> { get<MutableBlockchain>() }

        single {

            val worldStateArchive = WorldStateArchive(get(), get())

            // initialise world state from genesis block
            val genesisState = get<GenesisState>()
            genesisState.writeStateTo(worldStateArchive.mutable)

            worldStateArchive
        }

        // protocol context
        single {
            val blockchain = get<MutableBlockchain>()
            val worldStateArchive = get<WorldStateArchive>()
            ProtocolContext(blockchain, worldStateArchive, null)
        }
    }

    val testHelpers = module {

        // created at start ensures initialisation before tests
        single(createdAtStart = true) {
            ExfloTestCaseHelper(get(), ExfloBlockTestUtil.getTestReportAsInputStream())
        }

        single { TestChainLoader(get(), get(), get()) }
    }

    val defaultModuleList = listOf(
        defaultTestChainResources,
        initialState,
        headerFunctions,
        storage,
        chainState,
        testHelpers
    )
}
