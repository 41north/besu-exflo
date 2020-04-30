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

import io.exflo.ingestion.TruffleSpecs.Tracing.Integration.CreateDestroyIntegration
import io.exflo.ingestion.TruffleSpecs.Tracing.Integration.CreateIntegration
import io.exflo.ingestion.TruffleSpecs.Tracing.Integration.SelfDestructDelegatingCallsIntegration
import io.exflo.ingestion.TruffleSpecs.Tracing.Integration.SelfDestructIntegration
import io.exflo.ingestion.TruffleSpecs.Tracing.Unit.Create2OpCodeUnit
import io.exflo.ingestion.TruffleSpecs.Tracing.Unit.CreateOpCodeUnit
import io.exflo.ingestion.TruffleSpecs.Tracing.Unit.SelfDestructOpCodeUnit
import io.exflo.ingestion.tracer.BlockTracer
import io.exflo.testutil.ExfloTestCaseHelper
import io.exflo.testutil.TestChainLoader
import io.exflo.testutil.TestPremineAddresses
import io.kotlintest.Spec
import io.kotlintest.TestCase
import io.kotlintest.TestResult
import io.kotlintest.extensions.TopLevelTest
import io.kotlintest.matchers.numerics.shouldBeGreaterThan
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.FunSpec
import org.apache.tuweni.bytes.Bytes
import org.hyperledger.besu.ethereum.core.Wei
import org.hyperledger.besu.ethereum.vm.Code
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject

class TracerSpec : KoinTest, FunSpec() {

    private val testChainLoader: TestChainLoader by inject()
    private val testHelper: ExfloTestCaseHelper by inject()
    private val tracer: BlockTracer by inject()

    override fun beforeSpecClass(spec: Spec, tests: List<TopLevelTest>) {
        startKoin {
            modules(KoinTestIngestionModules.defaultModuleList)
        }
        // import test blocks
        testChainLoader.load()
    }

    override fun afterSpecClass(spec: Spec, results: Map<TestCase, TestResult>) {
        stopKoin()
    }

    init {

        context("with a set of solidity contracts") {

            context("with CreateOpCodeUnit contract") {

                test("we should detect a CREATE opcode inside our tracer with valid code and with ether associated") {

                    // Gather data
                    val block = testHelper.blocksFor(CreateOpCodeUnit.shouldCreateANewSmartContractWithValidCodeAndWithEtherAssociated).first()

                    // Process it
                    val traces = tracer.trace(block, emptyList())!!
                    traces shouldNotBe null

                    val transactionTrace = traces.transactionTraces.first()

                    // Assert
                    transactionTrace shouldNotBe null
                    transactionTrace.contractsCreated.size shouldBe 1
                    transactionTrace.contractsDestroyed.size shouldBe 0
                    transactionTrace.internalTransactions.size shouldBe 0

                    val createdEvent = transactionTrace.contractsCreated.first()

                    createdEvent.originatorAddress shouldBe TestPremineAddresses.one
                    createdEvent.contractAddress.size() shouldBe 20
                    createdEvent.amount shouldBe Wei.of(100000000000)
                    createdEvent.code shouldBe Code(Bytes.fromHexString("0x3000000000000000000000000000000000000000000000000000000000000000")).bytes
                }

                test("we should detect a CREATE opcode inside our tracer with valid code and without ether associated") {

                    // Gather data
                    val block = testHelper.blocksFor(CreateOpCodeUnit.shouldCreateANewSmartContractWithValidCodeAndWithoutEtherAssociated).first()

                    // Process it
                    val traces = tracer.trace(block, emptyList())!!
                    traces shouldNotBe null

                    val transactionTrace = traces.transactionTraces.first()

                    // Assert
                    transactionTrace shouldNotBe null
                    transactionTrace.contractsCreated.size shouldBe 1
                    transactionTrace.contractsDestroyed.size shouldBe 0
                    transactionTrace.internalTransactions.size shouldBe 0

                    val createdEvent = transactionTrace.contractsCreated.first()

                    createdEvent.originatorAddress shouldBe TestPremineAddresses.one
                    createdEvent.contractAddress.size() shouldBe 20
                    createdEvent.amount shouldBe Wei.of(0)
                    createdEvent.code shouldBe Code(Bytes.fromHexString("0x3000000000000000000000000000000000000000000000000000000000000000")).bytes
                }

                test("we should detect a CREATE opcode inside our tracer without code and without ether associated") {

                    // Gather data
                    val block = testHelper.blocksFor(CreateOpCodeUnit.shouldCreateANewSmartContractWithoutCodeAndWithoutEtherAssociated).first()

                    // Process it
                    val traces = tracer.trace(block, emptyList())!!
                    traces shouldNotBe null
                    val transactionTrace = traces.transactionTraces.first()

                    // Assert
                    transactionTrace shouldNotBe null
                    transactionTrace.contractsCreated.size shouldBe 1
                    transactionTrace.contractsDestroyed.size shouldBe 0
                    transactionTrace.internalTransactions.size shouldBe 0

                    val createdEvent = transactionTrace.contractsCreated.first()

                    createdEvent.originatorAddress shouldBe TestPremineAddresses.one
                    createdEvent.contractAddress.size() shouldBe 20
                    createdEvent.amount shouldBe Wei.of(0)
                    createdEvent.code shouldBe Code(Bytes.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000000")).bytes
                }

                test("we should NOT detect a CREATE opcode inside our tracer with invalid code and without ether associated") {

                    // Gather data
                    val block = testHelper.blocksFor(CreateOpCodeUnit.shouldCreateANewSmartContractWithInvalidCodeAndWithoutEtherAssociated).first()

                    // Process it
                    val traces = tracer.trace(block, emptyList())!!
                    traces shouldNotBe null
                    val transactionTrace = traces.transactionTraces.first()

                    // Assert
                    transactionTrace shouldNotBe null
                    transactionTrace.contractsCreated.size shouldBe 0
                    transactionTrace.contractsDestroyed.size shouldBe 0
                    transactionTrace.internalTransactions.size shouldBe 0
                }

                test("we should detect two CREATE opcodes inside our tracer with valid code and with ether associated") {

                    // Gather data
                    val block = testHelper.blocksFor(CreateOpCodeUnit.shouldCreateTwoNewSmartContractsWithValidCodeAndWithEtherAssociated).first()

                    // Process it
                    val traces = tracer.trace(block, emptyList())!!
                    traces shouldNotBe null
                    val transactionTrace = traces.transactionTraces.first()

                    // Assert
                    transactionTrace shouldNotBe null
                    transactionTrace.contractsCreated.size shouldBe 2
                    transactionTrace.contractsDestroyed.size shouldBe 0
                    transactionTrace.internalTransactions.size shouldBe 0

                    val createdEvent = transactionTrace.contractsCreated.first()

                    createdEvent.originatorAddress shouldBe TestPremineAddresses.one
                    createdEvent.contractAddress.size() shouldBe 20
                    createdEvent.amount shouldBe Wei.of(100000000000)
                    createdEvent.code shouldBe Code(Bytes.fromHexString("0x3000000000000000000000000000000000000000000000000000000000000000")).bytes

                    val createdEvent2 = transactionTrace.contractsCreated[1]

                    createdEvent2.originatorAddress shouldBe TestPremineAddresses.one
                    createdEvent2.contractAddress.size() shouldBe 20
                    createdEvent2.amount shouldBe Wei.of(100000000000)
                    createdEvent2.code shouldBe Code(Bytes.fromHexString("0x3200000000000000000000000000000000000000000000000000000000000000")).bytes
                }

                test("we should detect a CREATE opcode inside our tracer with valid code and with ether associated while the second one is discarded") {

                    // Gather data
                    val block = testHelper.blocksFor(CreateOpCodeUnit.shouldCreateTwoNewSmartContractsWithValidAndInvalidCodeAndWithEtherAssociated).first()

                    // Process it
                    val traces = tracer.trace(block, emptyList())!!
                    traces shouldNotBe null
                    val transactionTrace = traces.transactionTraces.first()

                    // Assert
                    transactionTrace shouldNotBe null
                    transactionTrace.contractsCreated.size shouldBe 1
                    transactionTrace.contractsDestroyed.size shouldBe 0
                    transactionTrace.internalTransactions.size shouldBe 0

                    val createdEvent = transactionTrace.contractsCreated.first()

                    createdEvent.originatorAddress shouldBe TestPremineAddresses.one
                    createdEvent.contractAddress.size() shouldBe 20
                    createdEvent.amount shouldBe Wei.of(100000000000)
                    createdEvent.code shouldBe Code(Bytes.fromHexString("0x3000000000000000000000000000000000000000000000000000000000000000")).bytes
                }
            }

            context("with CreateOpcode2Unit contract") {

                test("we should detect a CREATE2 opcode inside our tracer") {

                    // Gather data
                    val block = testHelper.blocksFor(Create2OpCodeUnit.shouldCreateANewSmartContractWithAnEmptySource).first()

                    // Process it
                    val traces = tracer.trace(block, emptyList())!!
                    traces shouldNotBe null
                    val transactionTrace = traces.transactionTraces.first()

                    // Assert traces
                    with(transactionTrace) {
                        this shouldNotBe null
                        contractsCreated.size shouldBe 1
                        contractsDestroyed.size shouldBe 0
                        internalTransactions.size shouldBe 0
                    }

                    // Assert events
                    val createdEvent = transactionTrace.contractsCreated.first()
                    with(createdEvent) {
                        originatorAddress shouldBe originatorAddress
                        contractAddress.size() shouldBe 20
                        amount shouldBe Wei.of(100000000000)
                        code shouldBe Code(Bytes.fromHexString("0x3000000000000000000000000000000000000000000000000000000000000000")).bytes
                    }
                }
            }

            context("with SelfDestructOpCode contract") {

                test("we should detect a SELFDESTRUCT opcode inside our tracer refunding sender") {

                    // Gather data
                    val block = testHelper.blocksFor(SelfDestructOpCodeUnit.shouldDestroyTheContractAndRefundTheSender).first()

                    // Process it
                    val traces = tracer.trace(block, emptyList())!!
                    traces shouldNotBe null
                    val transactionTrace = traces.transactionTraces.first()

                    // Assert
                    transactionTrace shouldNotBe null
                    transactionTrace.contractsCreated.size shouldBe 0
                    transactionTrace.contractsDestroyed.size shouldBe 1
                    transactionTrace.internalTransactions.size shouldBe 0

                    val destroyedEvent = transactionTrace.contractsDestroyed.first()

                    destroyedEvent.contractAddress.size() shouldBe 20
                    destroyedEvent.refundAddress shouldBe TestPremineAddresses.two
                    destroyedEvent.refundAmount shouldBe Wei.of(1000000000000000000)
                }

                test("we should detect a SELFDESTRUCT opcode inside our tracer destroying ether in the process") {

                    // Gather data
                    val block = testHelper.blocksFor(SelfDestructOpCodeUnit.shouldDestroyAndRefundSelfWhileDestroyingEther).first()

                    // Process it
                    val traces = tracer.trace(block, emptyList())!!
                    traces shouldNotBe null
                    val transactionTrace = traces.transactionTraces.first()

                    // Assert
                    transactionTrace shouldNotBe null
                    transactionTrace.contractsCreated.size shouldBe 0
                    transactionTrace.contractsDestroyed.size shouldBe 1
                    transactionTrace.internalTransactions.size shouldBe 0

                    val destroyedEvent = transactionTrace.contractsDestroyed.first()

                    destroyedEvent.contractAddress.size() shouldBe 20
                    destroyedEvent.refundAddress shouldBe destroyedEvent.refundAddress
                    destroyedEvent.refundAmount shouldBe Wei.of(1000000000000000000)
                }
            }

            context("with CreateIntegration contract") {

                test("should create a DummyContract when \"createDummyContract\" method is called") {

                    // Gather data
                    val block = testHelper.blocksFor(CreateIntegration.shouldCreateADummyContractWhenCreateDummyContractMethodIsCalled).first()

                    // Process it
                    val traces = tracer.trace(block, emptyList())!!
                    traces shouldNotBe null
                    val transactionTrace = traces.transactionTraces.first()

                    // Assert
                    transactionTrace shouldNotBe null
                    transactionTrace.contractsCreated.size shouldBe 1
                    transactionTrace.contractsDestroyed.size shouldBe 0
                    transactionTrace.internalTransactions.size shouldBe 0

                    val createdEvent = transactionTrace.contractsCreated.first()

                    createdEvent.originatorAddress shouldBe TestPremineAddresses.one
                    createdEvent.contractAddress.size() shouldBe 20
                    createdEvent.amount shouldBe Wei.of(0)
                    createdEvent.code.size() shouldBeGreaterThan 0
                }

                test("should NOT create DummyOutOfGasContract when \"createDummyOutOfGasContract\" method is called") {

                    // Gather data
                    val block =
                        testHelper.blocksFor(CreateIntegration.shouldNotCreateADummyOutOfGasContractWhenCreateDummyOutOfGasContractMethodIsCalled).first()

                    // Process it
                    val traces = tracer.trace(block, emptyList())!!
                    traces shouldNotBe null
                    val transactionTrace = traces.transactionTraces.first()

                    // Assert
                    transactionTrace shouldNotBe null
                    transactionTrace.contractsCreated.size shouldBe 0
                    transactionTrace.contractsDestroyed.size shouldBe 0
                    transactionTrace.internalTransactions.size shouldBe 0
                }

                test("should NOT create DummyBadOpCodeContract when \"createDummyBadOpCodeContract\" method is called") {

                    // Gather data
                    val block =
                        testHelper.blocksFor(CreateIntegration.shouldNotCreateDummyBadOpCodeContractWhenCreateDummyBadOpCodeContractMethodIsCalled).first()

                    // Process it
                    val traces = tracer.trace(block, emptyList())!!
                    traces shouldNotBe null
                    val transactionTrace = traces.transactionTraces.first()

                    // Assert
                    transactionTrace shouldNotBe null
                    transactionTrace.contractsCreated.size shouldBe 0
                    transactionTrace.contractsDestroyed.size shouldBe 0
                    transactionTrace.internalTransactions.size shouldBe 0
                }

                test("should NOT create a DummyRevertContract when \"createDummyRevertContract\" method is called") {

                    // Gather data
                    val block = testHelper.blocksFor(CreateIntegration.shouldNotCreateADummyRevertContractWhenCreateDummyRevertContractMethodIsCalled).first()

                    // Process it
                    val traces = tracer.trace(block, emptyList())!!
                    traces shouldNotBe null
                    val transactionTrace = traces.transactionTraces.first()

                    // Assert
                    transactionTrace shouldNotBe null
                    transactionTrace.contractsCreated.size shouldBe 0
                    transactionTrace.contractsDestroyed.size shouldBe 0
                    transactionTrace.internalTransactions.size shouldBe 0
                }

                test("should create a CreateNestedIntegration contract when \"createNestedDummyContracts\" method is called") {

                    // Gather data
                    val block =
                        testHelper.blocksFor(CreateIntegration.shouldCreateACreateNestedIntegrationContractWhenCreateNestedDummyContractsMethodIsCalled).first()

                    // Process it
                    val traces = tracer.trace(block, emptyList())!!
                    traces shouldNotBe null
                    val transactionTrace = traces.transactionTraces.first()

                    // Assert
                    transactionTrace shouldNotBe null
                    transactionTrace.contractsCreated.size shouldBe 5
                    transactionTrace.contractsDestroyed.size shouldBe 0
                    transactionTrace.internalTransactions.size shouldBe 0

                    val weiAmounts = listOf(
                        5000000000000000000L,
                        0L,
                        1000000000000000000L,
                        2000000000000000000L,
                        25L
                    )

                    transactionTrace.contractsCreated
                        .zip(weiAmounts)
                        .forEach { (createdEvent, weiAmount) ->

                            createdEvent.originatorAddress shouldBe TestPremineAddresses.one
                            createdEvent.contractAddress.size() shouldBe 20
                            createdEvent.amount shouldBe Wei.of(weiAmount)
                            createdEvent.code.size() shouldBeGreaterThan 0
                        }
                }
            }

            context("with SelfDestructIntegration contract") {

                test("should destroy the contract and refund the sender") {

                    // Gather data
                    val block = testHelper.blocksFor(SelfDestructIntegration.shouldDestroyTheContractAndRefundTheSender).first()

                    // Process it
                    val traces = tracer.trace(block, emptyList())!!
                    traces shouldNotBe null
                    val transactionTrace = traces.transactionTraces.first()

                    // Assert
                    transactionTrace shouldNotBe null
                    transactionTrace.contractsCreated.size shouldBe 0
                    transactionTrace.contractsDestroyed.size shouldBe 1
                    transactionTrace.internalTransactions.size shouldBe 0

                    val destroyedEvent = transactionTrace.contractsDestroyed.first()

                    destroyedEvent.contractAddress.size() shouldBe 20
                    destroyedEvent.refundAddress shouldNotBe TestPremineAddresses.one
                    destroyedEvent.refundAmount shouldBe Wei.of(1000000000000000000)
                }

                test("should destroy and refund self (which triggers the destroy of ether)") {

                    // Gather data
                    val block = testHelper.blocksFor(SelfDestructIntegration.shouldDestroyAndRefundSelfWhichTriggersTheDestroyOfEther).first()

                    // Process it
                    val traces = tracer.trace(block, emptyList())!!
                    traces shouldNotBe null
                    val transactionTrace = traces.transactionTraces.first()

                    // Assert
                    transactionTrace shouldNotBe null
                    transactionTrace.contractsCreated.size shouldBe 0
                    transactionTrace.contractsDestroyed.size shouldBe 1
                    transactionTrace.internalTransactions.size shouldBe 0

                    val destroyedEvent = transactionTrace.contractsDestroyed.first()

                    destroyedEvent.contractAddress.size() shouldBe 20
                    destroyedEvent.refundAddress shouldBe destroyedEvent.contractAddress
                    destroyedEvent.refundAmount shouldBe Wei.of(1000000000000000000)
                }
            }

            context("with SelfDestructDelegatingCallsIntegration contract") {

                test("should send ether to contract after self referencing destroy") {

                    // Gather data
                    val block = testHelper.blocksFor(SelfDestructDelegatingCallsIntegration.shouldSendEtherToContractAfterSelfReferencingDestroy).first()

                    // Process it
                    val traces = tracer.trace(block, emptyList())!!
                    traces shouldNotBe null
                    val transactionTrace = traces.transactionTraces.first()

                    // Assert
                    transactionTrace shouldNotBe null
                    transactionTrace.contractsCreated.size shouldBe 0
                    transactionTrace.contractsDestroyed.size shouldBe 1
                    transactionTrace.internalTransactions.size shouldBe 1

                    val destroyedEvent = transactionTrace.contractsDestroyed.first()

                    destroyedEvent.contractAddress.size() shouldBe 20
                    destroyedEvent.refundAddress shouldBe destroyedEvent.contractAddress
                    destroyedEvent.refundAmount shouldBe Wei.of(1000000000000000000)

                    val internalTransactionEvent = transactionTrace.internalTransactions.first()

                    internalTransactionEvent.amount shouldBe Wei.of(2000000000000000000)
                    internalTransactionEvent.fromAddress.size() shouldBe 20
                    internalTransactionEvent.toAddress.size() shouldBe 20
                }

                test("should produce a cascading destroy and refund sender") {

                    // Gather data
                    val block = testHelper.blocksFor(SelfDestructDelegatingCallsIntegration.shouldProduceACascadingDestroyAndRefundSender).first()

                    // Process it
                    val traces = tracer.trace(block, emptyList())!!
                    traces shouldNotBe null
                    val transactionTrace = traces.transactionTraces.first()

                    // Assert
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

                    // Gather data
                    val block = testHelper.blocksFor(SelfDestructDelegatingCallsIntegration.shouldCreateSelfDestroyingContractsAndSelfDestructItself).first()

                    // Process it
                    val traces = tracer.trace(block, emptyList())!!
                    traces shouldNotBe null
                    val transactionTrace = traces.transactionTraces.first()

                    // Assert
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

            context("with CreateDestroyIntegration contract") {

                test("should create and destroy itself on contract deploy") {

                    // Gather data
                    val block = testHelper.blocksFor(CreateDestroyIntegration.shouldCreateAndDestroyItselfOnContractDeploy).first()

                    // Process it
                    val traces = tracer.trace(block, emptyList())!!
                    traces shouldNotBe null
                    val transactionTrace = traces.transactionTraces.first()

                    // Assert
                    transactionTrace shouldNotBe null
                    transactionTrace.contractsCreated.size shouldBe 1
                    transactionTrace.contractsDestroyed.size shouldBe 1
                    transactionTrace.internalTransactions.size shouldBe 0

                    transactionTrace.contractsCreated
                        .zip(transactionTrace.contractsDestroyed)
                        .forEach { (createdEvent, destroyedEvent) ->
                            createdEvent.contractAddress shouldBe destroyedEvent.contractAddress
                            createdEvent.amount shouldBe destroyedEvent.refundAmount
                        }
                }
            }
        }
    }
}
