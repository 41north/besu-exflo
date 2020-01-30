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

import io.exflo.domain.ContractCapability
import io.exflo.domain.ContractType
import io.exflo.ingestion.TruffleSpecs.Tokens.ERC20.DetailedERC20
import io.exflo.ingestion.TruffleSpecs.Tokens.ERC721.FullERC721
import io.exflo.ingestion.TruffleSpecs.Tokens.ERC777.MinimalERC777
import io.exflo.ingestion.tokens.TokenDetector
import io.exflo.ingestion.tokens.precompiled.PrecompiledContractsFactory
import io.exflo.ingestion.tracer.BlockTracer
import io.exflo.testutil.ExfloTestCaseHelper
import io.exflo.testutil.TestChainLoader
import io.kotlintest.Spec
import io.kotlintest.TestCase
import io.kotlintest.TestResult
import io.kotlintest.extensions.TopLevelTest
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.FunSpec
import org.hyperledger.besu.cli.config.EthNetworkConfig
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule
import org.hyperledger.besu.ethereum.transaction.TransactionSimulator
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject

class TokenDetectorSpec : FunSpec(), KoinTest {

    private val testChainLoader: TestChainLoader by inject()
    private val testHelper: ExfloTestCaseHelper by inject()
    private val networkConfig: EthNetworkConfig by inject()
    private val protocolSchedule: ProtocolSchedule<*> by inject()
    private val transactionSimulator: TransactionSimulator by inject()
    private val tracer: BlockTracer by inject()

    override fun beforeSpecClass(spec: Spec, tests: List<TopLevelTest>) {
        startKoin {
            modules(KoinTestIngestionModules.defaultModuleList)
        }

        // import test blocks
        testChainLoader.load()

        // register precompiled contracts
        PrecompiledContractsFactory.register(protocolSchedule, networkConfig.networkId)
    }

    override fun afterSpecClass(spec: Spec, results: Map<TestCase, TestResult>) {
        stopKoin()
    }

    init {

        context("with a Detailed ERC20") {

            // Gather data
            val block = testHelper.blocksFor(DetailedERC20.shouldDeployTheContract).first()

            // Gather transaction
            val tx = block.body.transactions.first()
            tx shouldNotBe null

            // Gather contract address
            val contractAddress = tx.contractAddress().orElse(null)
            contractAddress shouldNotBe null

            // Process it
            val traces = tracer.trace(block, emptyList())!!
            traces shouldNotBe null

            val transactionTrace = traces.transactionTraces.first()

            // Create ContractCreatedEvent
            val event = transactionTrace.contractsCreated.first()
            event shouldNotBe null

            // Create TokenDetector
            val tokenDetector =
                TokenDetector(transactionSimulator, block.header.hash, event.contractAddress, event.code)

            test("we should detect and retrieve ERC20 and ERC20_DETAILED capabilities") {

                // Recover type and capabilities
                val (contractType, capabilities) = tokenDetector.detect()

                // Assert
                contractType shouldBe ContractType.ERC20

                capabilities shouldBe setOf(
                    ContractCapability.ERC20,
                    ContractCapability.ERC20_DETAILED
                )
            }
        }

        context("with an ERC721 contract that implements all ERC721 capabilities") {

            // Gather data
            val block = testHelper.blocksFor(FullERC721.shouldDeployTheContract).first()

            // Gather transaction
            val tx = block.body.transactions.first()
            tx shouldNotBe null

            // Gather contract address
            val contractAddress = tx.contractAddress().orElse(null)
            contractAddress shouldNotBe null

            // Process it
            val traces = tracer.trace(block, emptyList())!!
            traces shouldNotBe null

            val transactionTrace = traces.transactionTraces.first()

            // Create ContractCreatedEvent
            val event = transactionTrace.contractsCreated.first()
            event shouldNotBe null

            // Create TokenDetector
            val tokenDetector =
                TokenDetector(transactionSimulator, block.header.hash, event.contractAddress, event.code)

            test("we should detect and retrieve all ERC721 capabilities") {

                // Recover type and capabilities
                val (contractType, capabilities) = tokenDetector.detect()

                // Assert
                contractType shouldBe ContractType.ERC721
                capabilities shouldBe setOf(
                    ContractCapability.ERC165,
                    ContractCapability.ERC721,
                    ContractCapability.ERC721_ENUMERABLE,
                    ContractCapability.ERC721_METADATA
                )
            }
        }

        context("with an ERC777 contract that implements all ERC777 capabilities") {

            test("we should detect and retrieve all ERC777 capabilities") {

                // Gather data
                val block = testHelper.blocksFor(MinimalERC777.shouldDeployTheContract).first()

                // Gather transaction
                val tx = block.body.transactions.first()
                tx shouldNotBe null

                // Gather contract address
                val contractAddress = tx.contractAddress().orElse(null)
                contractAddress shouldNotBe null

                // Process it
                val traces = tracer.trace(block, emptyList())!!
                traces shouldNotBe null

                val transactionTrace = traces.transactionTraces.first()

                // Create ContractCreatedEvent
                val event = transactionTrace.contractsCreated.first()
                event shouldNotBe null

                // Create TokenDetector
                val tokenDetector =
                    TokenDetector(transactionSimulator, block.header.hash, event.contractAddress, event.code)

                // Recover type and capabilities
                val (contractType, capabilities) = tokenDetector.detect()

                // Assert
                contractType shouldBe ContractType.ERC777
                capabilities shouldBe setOf(
                    ContractCapability.ERC777
                )
            }
        }
    }
}
