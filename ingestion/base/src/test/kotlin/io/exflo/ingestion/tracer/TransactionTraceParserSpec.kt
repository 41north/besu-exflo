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

package io.exflo.ingestion.tracer

import io.exflo.domain.ContractCapability
import io.exflo.domain.ContractType
import io.exflo.ingestion.KoinTestIngestionModules
import io.exflo.ingestion.TruffleSpecs.SelfDestructs.SelfDestruct
import io.exflo.ingestion.TruffleSpecs.SelfDestructs.SelfDestructDelegatingCalls
import io.exflo.ingestion.TruffleSpecs.Tokens.ERC20.CappedERC20
import io.exflo.ingestion.TruffleSpecs.Tokens.ERC20.DetailedERC20
import io.exflo.ingestion.TruffleSpecs.Tokens.ERC20.InvalidERC20
import io.exflo.ingestion.TruffleSpecs.Tokens.ERC20.WeirdNameCharsERC20
import io.exflo.ingestion.TruffleSpecs.Tokens.ERC721.FullERC721
import io.exflo.ingestion.TruffleSpecs.Tokens.ERC721.MinimalERC721
import io.exflo.ingestion.TruffleSpecs.Tokens.ERC721.WeirdNameCharsERC721
import io.exflo.ingestion.TruffleSpecs.Tokens.ERC777.MinimalERC777
import io.exflo.ingestion.tokens.precompiled.PrecompiledContractsFactory
import io.exflo.ingestion.tracker.BlockReader
import io.exflo.testutil.ExfloTestCaseHelper
import io.exflo.testutil.TestChainLoader
import io.exflo.testutil.TestPremineAddresses
import io.kotlintest.Spec
import io.kotlintest.TestCase
import io.kotlintest.TestResult
import io.kotlintest.extensions.TopLevelTest
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.FunSpec
import org.apache.tuweni.units.bigints.UInt256
import org.hyperledger.besu.cli.config.EthNetworkConfig
import org.hyperledger.besu.ethereum.core.Wei
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject

class TransactionTraceParserSpec : FunSpec(), KoinTest {

    private val testChainLoader: TestChainLoader by inject()
    private val testHelper: ExfloTestCaseHelper by inject()
    private val networkConfig: EthNetworkConfig by inject()
    private val protocolSchedule: ProtocolSchedule<*> by inject()
    private val blockReader: BlockReader by inject()

    override fun beforeSpecClass(spec: Spec, tests: List<TopLevelTest>) {
        startKoin {
            modules(KoinTestIngestionModules())
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

        context("Tokens") {

            context("ERC20") {

                test("with InvalidERC20 we should not detect anything at all") {

                    val block = testHelper.blocksFor(InvalidERC20.shouldDeployTheContract).first()

                    val trace = blockReader.trace(block.hash)
                    trace shouldNotBe null

                    val txTrace = trace!!.transactionTraces.firstOrNull()
                    txTrace shouldNotBe null

                    val contractCreated = txTrace!!.contractsCreated.firstOrNull()
                    contractCreated shouldNotBe null

                    contractCreated!!.type shouldBe ContractType.GENERIC
                    contractCreated.capabilities!! shouldContainExactly emptyList()
                    contractCreated.metadata shouldNotBe null

                    val (name, symbol, decimals, totalSupply, granularity, cap) = contractCreated.metadata!!
                    name shouldBe null
                    symbol shouldBe null
                    decimals shouldBe null
                    totalSupply shouldBe null
                    granularity shouldBe null
                    cap shouldBe null
                }

                test("with DetailedERC20 we should detect ERC20 and ERC20_DETAILED capabilities") {

                    val block = testHelper.blocksFor(DetailedERC20.shouldDeployTheContract).first()

                    val trace = blockReader.trace(block.hash)
                    trace shouldNotBe null

                    val txTrace = trace!!.transactionTraces.firstOrNull()
                    txTrace shouldNotBe null

                    val contractCreated = txTrace!!.contractsCreated.firstOrNull()
                    contractCreated shouldNotBe null

                    contractCreated!!.type shouldBe ContractType.ERC20
                    contractCreated.capabilities!! shouldContainExactly listOf(
                        ContractCapability.ERC20,
                        ContractCapability.ERC20_DETAILED
                    )
                    contractCreated.metadata shouldNotBe null

                    val (name, symbol, decimals, totalSupply, granularity, cap) = contractCreated.metadata!!
                    name shouldBe "ERC20 Detailed"
                    symbol shouldBe "E2D"
                    decimals shouldBe 9.toByte()
                    totalSupply shouldBe UInt256.valueOf(10)
                    granularity shouldBe null
                    cap shouldBe null
                }

                test("with WeirdNameCharsERC20 we should detect ERC20 and ERC20_DETAILED capabilities") {

                    val block = testHelper.blocksFor(WeirdNameCharsERC20.shouldDeployTheContract).first()

                    val trace = blockReader.trace(block.hash)
                    trace shouldNotBe null

                    val txTrace = trace!!.transactionTraces.firstOrNull()
                    txTrace shouldNotBe null

                    val contractCreated = txTrace!!.contractsCreated.firstOrNull()
                    contractCreated shouldNotBe null

                    contractCreated!!.type shouldBe ContractType.ERC20
                    contractCreated.capabilities!! shouldContainExactly listOf(
                        ContractCapability.ERC20,
                        ContractCapability.ERC20_DETAILED
                    )
                    contractCreated.metadata shouldNotBe null

                    val (name, symbol, decimals, totalSupply, granularity, cap) = contractCreated.metadata!!
                    name shouldBe """��V��RvvvW�WR�����"""
                    symbol shouldBe "ygunnnnn"
                    decimals shouldBe 0.toByte()
                    totalSupply shouldBe UInt256.valueOf(10)
                    granularity shouldBe null
                    cap shouldBe null
                }

                test("with CappedERC20 we should detect ERC20 and ERC20_DETAILED capabilities") {

                    val block = testHelper.blocksFor(CappedERC20.shouldDeployTheContract).first()

                    val trace = blockReader.trace(block.hash)
                    trace shouldNotBe null

                    val txTrace = trace!!.transactionTraces.firstOrNull()
                    txTrace shouldNotBe null

                    val contractCreated = txTrace!!.contractsCreated.firstOrNull()
                    contractCreated shouldNotBe null

                    contractCreated!!.type shouldBe ContractType.ERC20
                    contractCreated.capabilities!! shouldContainExactly listOf(
                        ContractCapability.ERC20,
                        ContractCapability.ERC20_MINTABLE,
                        ContractCapability.ERC20_CAPPED
                    )
                    contractCreated.metadata shouldNotBe null

                    val (name, symbol, decimals, totalSupply, granularity, cap) = contractCreated.metadata!!
                    name shouldBe null
                    symbol shouldBe null
                    decimals shouldBe null
                    totalSupply shouldBe UInt256.valueOf(10)
                    granularity shouldBe null
                    cap shouldBe UInt256.valueOf(10)
                }
            }

            context("ERC721") {

                test("with MinimalERC721 contract that implements some of ERC721 capabilities") {

                    val block = testHelper.blocksFor(MinimalERC721.shouldDeployTheContract).first()

                    val trace = blockReader.trace(block.hash)
                    trace shouldNotBe null

                    val txTrace = trace!!.transactionTraces.firstOrNull()
                    txTrace shouldNotBe null

                    val contractCreated = txTrace!!.contractsCreated.firstOrNull()
                    contractCreated shouldNotBe null

                    contractCreated!!.type shouldBe ContractType.ERC721
                    contractCreated.capabilities!! shouldContainExactly listOf(
                        ContractCapability.ERC165,
                        ContractCapability.ERC721
                    )
                    contractCreated.metadata shouldNotBe null

                    val (name, symbol, decimals, totalSupply, granularity, cap) = contractCreated.metadata!!
                    name shouldBe null
                    symbol shouldBe null
                    decimals shouldBe null
                    totalSupply shouldBe null
                    granularity shouldBe null
                    cap shouldBe null
                }

                test("with FullERC721 contract that implements some of ERC721 capabilities") {

                    val block = testHelper.blocksFor(FullERC721.shouldDeployTheContract).first()

                    val trace = blockReader.trace(block.hash)
                    trace shouldNotBe null

                    val txTrace = trace!!.transactionTraces.firstOrNull()
                    txTrace shouldNotBe null

                    val contractCreated = txTrace!!.contractsCreated.firstOrNull()
                    contractCreated shouldNotBe null

                    contractCreated!!.type shouldBe ContractType.ERC721
                    contractCreated.capabilities!! shouldContainExactly listOf(
                        ContractCapability.ERC165,
                        ContractCapability.ERC721,
                        ContractCapability.ERC721_METADATA,
                        ContractCapability.ERC721_ENUMERABLE
                    )
                    contractCreated.metadata shouldNotBe null

                    val (name, symbol, decimals, totalSupply, granularity, cap) = contractCreated.metadata!!
                    name shouldBe "ERC721 Full"
                    symbol shouldBe "E721"
                    decimals shouldBe null
                    totalSupply shouldBe UInt256.ZERO
                    granularity shouldBe null
                    cap shouldBe null
                }

                test("with WeirdNameCharsERC721 contract that implements some of ERC721 capabilities") {

                    val block = testHelper.blocksFor(WeirdNameCharsERC721.shouldDeployTheContract).first()

                    val trace = blockReader.trace(block.hash)
                    trace shouldNotBe null

                    val txTrace = trace!!.transactionTraces.firstOrNull()
                    txTrace shouldNotBe null

                    val contractCreated = txTrace!!.contractsCreated.firstOrNull()
                    contractCreated shouldNotBe null

                    contractCreated!!.type shouldBe ContractType.ERC721
                    contractCreated.capabilities!! shouldContainExactly listOf(
                        ContractCapability.ERC165,
                        ContractCapability.ERC721,
                        ContractCapability.ERC721_METADATA,
                        ContractCapability.ERC721_ENUMERABLE
                    )
                    contractCreated.metadata shouldNotBe null

                    val (name, symbol, decimals, totalSupply, granularity, cap) = contractCreated.metadata!!
                    name shouldBe """��V��RvvvW�WR�����"""
                    symbol shouldBe """�WR"""
                    decimals shouldBe null
                    totalSupply shouldBe UInt256.ZERO
                    granularity shouldBe null
                    cap shouldBe null
                }
            }

            context("ERC777") {

                test("with MinimalERC777 contract that implements some of ERC777 capabilities") {

                    val block = testHelper.blocksFor(MinimalERC777.shouldDeployTheContract).first()

                    val trace = blockReader.trace(block.hash)
                    trace shouldNotBe null

                    val txTrace = trace!!.transactionTraces.firstOrNull()
                    txTrace shouldNotBe null

                    val contractCreated = txTrace!!.contractsCreated.firstOrNull()
                    contractCreated shouldNotBe null

                    contractCreated!!.type shouldBe ContractType.ERC777
                    contractCreated.capabilities!! shouldContainExactly listOf(ContractCapability.ERC777)
                    contractCreated.metadata shouldNotBe null

                    val (name, symbol, decimals, totalSupply, granularity, cap) = contractCreated.metadata!!
                    name shouldBe """RegularERC777"""
                    symbol shouldBe """E777"""
                    decimals shouldBe null
                    totalSupply shouldBe UInt256.ZERO
                    granularity shouldBe UInt256.ONE
                    cap shouldBe null
                }
            }
        }

        context("Self-Destructs") {

            context("with SelfDestruct contract") {

                test("should destroy the contract and refund the sender") {

                    val block = testHelper.blocksFor(SelfDestruct.shouldDestroyTheContractAndRefundTheSender).first()

                    val trace = blockReader.trace(block.hash)
                    trace shouldNotBe null

                    val txTrace = trace!!.transactionTraces.firstOrNull()

                    txTrace shouldNotBe null
                    txTrace!!.contractsCreated.size shouldBe 0
                    txTrace.contractsDestroyed.size shouldBe 1
                    txTrace.internalTransactions.size shouldBe 0

                    val destroyedEvent = txTrace.contractsDestroyed.first()

                    destroyedEvent.contractAddress.size() shouldBe 20
                    destroyedEvent.refundAddress shouldNotBe TestPremineAddresses.one
                    destroyedEvent.refundAmount shouldBe Wei.of(1000000000000000000)
                }

                test("should destroy and refund self (which triggers the destroy of ether)") {

                    val block = testHelper.blocksFor(SelfDestruct.shouldDestroyAndRefundSelf).first()

                    val trace = blockReader.trace(block.hash)
                    trace shouldNotBe null

                    val txTrace = trace!!.transactionTraces.firstOrNull()

                    txTrace shouldNotBe null
                    txTrace!!.contractsCreated.size shouldBe 0
                    txTrace.contractsDestroyed.size shouldBe 1
                    txTrace.internalTransactions.size shouldBe 0

                    val destroyedEvent = txTrace.contractsDestroyed.first()

                    destroyedEvent.contractAddress.size() shouldBe 20
                    destroyedEvent.refundAddress shouldBe destroyedEvent.contractAddress
                    destroyedEvent.refundAmount shouldBe Wei.of(1000000000000000000)
                }
            }

            context("with SelfDestructDelegatingCalls contract") {

                test("should send ether to contract after self referencing destroy") {

                    val block = testHelper.blocksFor(SelfDestructDelegatingCalls.shouldSendEtherToContractAfterSelfReferencingDestroy).first()

                    val trace = blockReader.trace(block.hash)
                    trace shouldNotBe null

                    val txTrace = trace!!.transactionTraces.first()

                    txTrace shouldNotBe null
                    txTrace.contractsCreated.size shouldBe 0
                    txTrace.contractsDestroyed.size shouldBe 1
                    txTrace.internalTransactions.size shouldBe 1

                    val destroyedEvent = txTrace.contractsDestroyed.first()

                    destroyedEvent.contractAddress.size() shouldBe 20
                    destroyedEvent.refundAddress shouldBe destroyedEvent.contractAddress
                    destroyedEvent.refundAmount shouldBe Wei.of(1000000000000000000)

                    val internalTransactionEvent = txTrace.internalTransactions.first()

                    internalTransactionEvent.amount shouldBe Wei.of(2000000000000000000)
                    internalTransactionEvent.fromAddress.size() shouldBe 20
                    internalTransactionEvent.toAddress.size() shouldBe 20
                }

                test("should produce a cascading destroy and refund sender") {

                    val block = testHelper.blocksFor(SelfDestructDelegatingCalls.shouldProduceACascadingDestroyAndRefundSender).first()

                    val traces = blockReader.trace(block.hash)
                    traces shouldNotBe null

                    val transactionTrace = traces!!.transactionTraces.first()

                    transactionTrace shouldNotBe null
                    transactionTrace.contractsCreated.size shouldBe 0
                    transactionTrace.contractsDestroyed.size shouldBe 4
                    transactionTrace.internalTransactions.size shouldBe 0

                    val refundAmounts = listOf(
                        "1000000000000000000".toBigInteger(),
                        "2000000000000000000".toBigInteger(),
                        "3000000000000000000".toBigInteger(),
                        "10000000000000000000".toBigInteger()
                    )

                    transactionTrace.contractsDestroyed
                        .zip(refundAmounts)
                        .forEach { (destroyedEvent, refundAmount) ->
                            destroyedEvent.refundAddress.size() shouldBe 20
                            destroyedEvent.contractAddress.size() shouldBe 20
                            destroyedEvent.refundAmount shouldBe Wei.of(refundAmount)
                        }
                }

                test("should create self destroying contracts and self destruct itself") {

                    val block = testHelper.blocksFor(SelfDestructDelegatingCalls.shouldCreateSelfDestroyingContractsAndSelfDestructItself).first()

                    val traces = blockReader.trace(block.hash)
                    traces shouldNotBe null

                    val transactionTrace = traces!!.transactionTraces.first()

                    transactionTrace shouldNotBe null
                    transactionTrace.contractsCreated.size shouldBe 3
                    transactionTrace.contractsDestroyed.size shouldBe 4
                    transactionTrace.internalTransactions.size shouldBe 0

                    val refundAmounts = listOf(
                        10,
                        20,
                        30,
                        3999999999999999980
                    )

                    transactionTrace.contractsDestroyed
                        .zip(refundAmounts)
                        .forEach { (destroyedEvent, refundAmount) ->
                            destroyedEvent.refundAddress.size() shouldBe 20
                            destroyedEvent.contractAddress.size() shouldBe 20
                            destroyedEvent.refundAmount shouldBe Wei.of(refundAmount)
                        }
                }
            }

            // TODO: Commented offending test until we review with Besu devs if is a bug in their FlatTraceGenerator
            // context("with SelfDestructInConstructor contract") {
            //
            //     test("should create and destroy itself on contract deploy") {
            //
            //         val block = testHelper.blocksFor(SelfDestructInConstructor.shouldCreateAndDestroyItselfOnContractDeploy).first()
            //
            //         val traces = blockReader.trace(block.hash)
            //         traces shouldNotBe null
            //
            //         val transactionTrace = traces!!.transactionTraces.first()
            //
            //         transactionTrace shouldNotBe null
            //         transactionTrace.contractsCreated.size shouldBe 1
            //         transactionTrace.contractsDestroyed.size shouldBe 1
            //         transactionTrace.internalTransactions.size shouldBe 0
            //
            //         transactionTrace.contractsCreated
            //             .zip(transactionTrace.contractsDestroyed)
            //             .forEach { (createdEvent, destroyedEvent) ->
            //                 createdEvent.contractAddress shouldBe destroyedEvent.contractAddress
            //                 createdEvent.amount shouldBe destroyedEvent.refundAmount
            //             }
            //     }
            // }
        }
    }
}
