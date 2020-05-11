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

package io.exflo.ingestion.tokens.events

import io.exflo.domain.ContractEvents
import io.exflo.ingestion.extensions.hexToAddress
import io.exflo.ingestion.KoinTestIngestionModules
import io.exflo.ingestion.TruffleSpecs.Tokens.TokenEvents.ERC1155Events
import io.exflo.ingestion.TruffleSpecs.Tokens.TokenEvents.ERC20Events
import io.exflo.ingestion.TruffleSpecs.Tokens.TokenEvents.ERC721Events
import io.exflo.ingestion.TruffleSpecs.Tokens.TokenEvents.ERC777Events
import io.exflo.ingestion.extensions.contractEvents
import io.exflo.testutil.ExfloTestCaseHelper
import io.exflo.testutil.TestChainLoader
import io.kotlintest.Spec
import io.kotlintest.TestCase
import io.kotlintest.TestResult
import io.kotlintest.extensions.TopLevelTest
import io.kotlintest.matchers.beInstanceOf
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.units.bigints.UInt256
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject

class LogParserSpec : FunSpec(), KoinTest {

    private val testChainLoader: TestChainLoader by inject()
    private val testHelper: ExfloTestCaseHelper by inject()

    override fun beforeSpecClass(spec: Spec, tests: List<TopLevelTest>) {
        startKoin {
            modules(KoinTestIngestionModules())
        }

        // import test blocks
        testChainLoader.load()
    }

    override fun afterSpecClass(spec: Spec, results: Map<TestCase, TestResult>) {
        stopKoin()
    }

    init {

        context("given a series of ERC20 event logs") {

            val (_, receipts) = testHelper.blocksWithReceiptsFor(ERC20Events.shouldDeployTheContract).first()

            val eventList = receipts.first().contractEvents()

            eventList.size shouldBe 2

            test("we should detect a fungible transfer event") {

                eventList[0] should beInstanceOf<ContractEvents.FungibleTransfer>()
                val transfer = eventList[0] as ContractEvents.FungibleTransfer

                transfer.from shouldBe "0x1".hexToAddress()
                transfer.to shouldBe "0x2".hexToAddress()
                transfer.value shouldBe UInt256.valueOf(3)
            }

            test("we should detect a fungible approval event") {

                eventList[1] should beInstanceOf<ContractEvents.FungibleApproval>()
                val approval = eventList[1] as ContractEvents.FungibleApproval

                approval.owner shouldBe "0x4".hexToAddress()
                approval.spender shouldBe "0x5".hexToAddress()
                approval.value shouldBe UInt256.valueOf(6)
            }
        }

        context("given a series of ERC721 event logs") {

            val (_, receipts) = testHelper.blocksWithReceiptsFor(ERC721Events.shouldDeployTheContract).first()

            val eventList = receipts.first().contractEvents()

            eventList.size shouldBe 4

            test("we should detect a non fungible transfer event") {

                eventList[0] should beInstanceOf<ContractEvents.NonFungibleTransfer>()

                val transfer = eventList[0] as ContractEvents.NonFungibleTransfer

                transfer.from shouldBe "0x1".hexToAddress()
                transfer.to shouldBe "0x2".hexToAddress()
                transfer.tokenId shouldBe UInt256.valueOf(3)
            }

            test("we should detect a non fungible approval event") {

                eventList[1] should beInstanceOf<ContractEvents.NonFungibleApproval>()

                val approval = eventList[1] as ContractEvents.NonFungibleApproval

                approval.owner shouldBe "0x4".hexToAddress()
                approval.approved shouldBe "0x5".hexToAddress()
                approval.tokenId shouldBe UInt256.valueOf(6)
            }

            test("we should detect an approved approval for all event") {

                eventList[2] should beInstanceOf<ContractEvents.ApprovalForAll>()

                val approval = eventList[2] as ContractEvents.ApprovalForAll

                approval.owner shouldBe "0x1".hexToAddress()
                approval.operator shouldBe "0x2".hexToAddress()
                approval.approved shouldBe true
            }

            test("we should detect a rejected approval for all event") {

                eventList[3] should beInstanceOf<ContractEvents.ApprovalForAll>()

                val approval = eventList[3] as ContractEvents.ApprovalForAll

                approval.owner shouldBe "0x3".hexToAddress()
                approval.operator shouldBe "0x4".hexToAddress()
                approval.approved shouldBe false
            }
        }

        context("given a series of ERC777 event logs") {

            val (_, receipts) = testHelper.blocksWithReceiptsFor(ERC777Events.shouldDeployTheContract).first()

            val eventList = receipts.first().contractEvents()

            eventList.size shouldBe 5

            test("we should detect a sent event") {

                eventList[0] should beInstanceOf<ContractEvents.Sent>()

                val sent = eventList[0] as ContractEvents.Sent

                sent.operator shouldBe "0x1".hexToAddress()
                sent.from shouldBe "0x2".hexToAddress()
                sent.to shouldBe "0x3".hexToAddress()
                sent.amount shouldBe UInt256.valueOf(4)
                sent.data shouldBe Bytes.wrap("foo".toByteArray())
                sent.operatorData shouldBe Bytes.wrap("bar".toByteArray())
            }

            test("we should detect a minted event") {

                eventList[1] should beInstanceOf<ContractEvents.Minted>()

                val minted = eventList[1] as ContractEvents.Minted

                minted.operator shouldBe "0x5".hexToAddress()
                minted.to shouldBe "0x6".hexToAddress()
                minted.amount shouldBe UInt256.valueOf(7)
                minted.data shouldBe Bytes.wrap("hello".toByteArray())
                minted.operatorData shouldBe Bytes.wrap("world".toByteArray())
            }

            test("we should detect a burned event") {

                eventList[2] should beInstanceOf<ContractEvents.Burned>()

                val burned = eventList[2] as ContractEvents.Burned

                burned.operator shouldBe "0x8".hexToAddress()
                burned.to shouldBe "0x9".hexToAddress()
                burned.amount shouldBe UInt256.valueOf(10)
                burned.data shouldBe Bytes.wrap("fizz".toByteArray())
                burned.operatorData shouldBe Bytes.wrap("buzz".toByteArray())
            }

            test("we should detect an authorized operator event") {

                eventList[3] should beInstanceOf<ContractEvents.AuthorizedOperator>()

                val authorized = eventList[3] as ContractEvents.AuthorizedOperator

                authorized.operator shouldBe "0xb".hexToAddress()
                authorized.holder shouldBe "0xc".hexToAddress()
            }

            test("we should detect a revoked operator event") {

                eventList[4] should beInstanceOf<ContractEvents.RevokedOperator>()

                val revoked = eventList[4] as ContractEvents.RevokedOperator

                revoked.operator shouldBe "0xd".hexToAddress()
                revoked.holder shouldBe "0xe".hexToAddress()
            }
        }

        context("given a series of ERC1155 event logs") {

            val (_, receipts) = testHelper.blocksWithReceiptsFor(ERC1155Events.shouldDeployTheContract).first()

            val eventList = receipts.first().contractEvents()

            eventList.size shouldBe 5

            test("we should detect a transfer single event") {

                eventList[0] should beInstanceOf<ContractEvents.TransferSingle>()

                val transfer = eventList[0] as ContractEvents.TransferSingle

                transfer.operator shouldBe "0x1".hexToAddress()
                transfer.from shouldBe "0x2".hexToAddress()
                transfer.to shouldBe "0x3".hexToAddress()
                transfer.id shouldBe UInt256.valueOf(4)
                transfer.value shouldBe UInt256.valueOf(5)
            }

            test("we should detect a transfer batch event") {

                eventList[1] should beInstanceOf<ContractEvents.TransferBatch>()

                val transfer = eventList[1] as ContractEvents.TransferBatch

                transfer.operator shouldBe "0x4".hexToAddress()
                transfer.from shouldBe "0x5".hexToAddress()
                transfer.to shouldBe "0x6".hexToAddress()

                transfer.ids shouldBe listOf(UInt256.valueOf(1), UInt256.valueOf(2), UInt256.valueOf(3))
                transfer.values shouldBe listOf(UInt256.valueOf(4), UInt256.valueOf(5), UInt256.valueOf(6))
            }

            test("we should detect an approved approval for all event") {

                eventList[2] should beInstanceOf<ContractEvents.ApprovalForAll>()

                val approval = eventList[2] as ContractEvents.ApprovalForAll

                approval.owner shouldBe "0x7".hexToAddress()
                approval.operator shouldBe "0x8".hexToAddress()
                approval.approved shouldBe true
            }

            test("we should detect a rejected approval for all event") {

                eventList[3] should beInstanceOf<ContractEvents.ApprovalForAll>()

                val approval = eventList[3] as ContractEvents.ApprovalForAll

                approval.owner shouldBe "0x9".hexToAddress()
                approval.operator shouldBe "0xa".hexToAddress()
                approval.approved shouldBe false
            }

            test("we should detect a URI event") {

                eventList[4] should beInstanceOf<ContractEvents.URI>()

                val uri = eventList[4] as ContractEvents.URI

                uri.value shouldBe "hello world"
                uri.id shouldBe UInt256.valueOf(1)
            }
        }
    }
}
