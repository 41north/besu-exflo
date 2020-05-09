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

import io.exflo.testutil.ExfloTestCase
import io.exflo.testutil.ExfloTestSuite

/**
 * Mapped tests from truffle/ folder.
 */
object TruffleSpecs {

    object Transfers {

        object EtherTransfers : ExfloTestSuite() {

            val shouldTransferRegularEther = object : ExfloTestCase(this) {
                override val description = "should transfer regular ether"
            }
        }
    }

    object Tokens {

        object ERC20 {
            object InvalidERC20 : ExfloTestSuite() {

                val shouldDeployTheContract = object : ExfloTestCase(this) {
                    override val description = "should deploy the contract"
                }
            }

            object MinimalERC20 : ExfloTestSuite() {

                val shouldDeployTheContract = object : ExfloTestCase(this) {
                    override val description = "should deploy the contract"
                }
            }

            object DetailedERC20 : ExfloTestSuite() {

                val shouldDeployTheContract = object : ExfloTestCase(this) {
                    override val description = "should deploy the contract"
                }
            }

            object WeirdNameCharsERC20 : ExfloTestSuite() {

                val shouldDeployTheContract = object : ExfloTestCase(this) {
                    override val description = "should deploy the contract"
                }
            }

            object CappedERC20 : ExfloTestSuite() {

                val shouldDeployTheContract = object : ExfloTestCase(this) {
                    override val description = "should deploy the contract"
                }
            }
        }

        object ERC165 {

            object ERC165Contract : ExfloTestSuite() {

                val shouldDeployTheContract = object : ExfloTestCase(this) {
                    override val description = "should deploy the contract"
                }
            }

            object NonERC165 : ExfloTestSuite() {

                val shouldDeployTheContract = object : ExfloTestCase(this) {
                    override val description = "should deploy the contract"
                }
            }
        }

        object ERC721 {

            object MinimalERC721 : ExfloTestSuite() {

                val shouldDeployTheContract = object : ExfloTestCase(this) {
                    override val description = "should deploy the contract"
                }
            }

            object FullERC721 : ExfloTestSuite() {

                val shouldDeployTheContract = object : ExfloTestCase(this) {
                    override val description = "should deploy the contract"
                }
            }

            object WeirdNameCharsERC721 : ExfloTestSuite() {

                val shouldDeployTheContract = object : ExfloTestCase(this) {
                    override val description = "should deploy the contract"
                }
            }

            object InvalidERC721 : ExfloTestSuite() {

                val shouldDeployTheContract = object : ExfloTestCase(this) {
                    override val description = "should deploy the contract"
                }
            }
        }

        object ERC777 {

            object InvalidERC777 : ExfloTestSuite() {

                val shouldDeployTheContract = object : ExfloTestCase(this) {
                    override val description = "should deploy the contract"
                }
            }

            object MinimalERC777 : ExfloTestSuite() {

                val shouldDeployTheContract = object : ExfloTestCase(this) {
                    override val description = "should deploy the contract"
                }
            }
        }

        object TokenEvents {

            object ERC20Events : ExfloTestSuite() {

                val shouldDeployTheContract = object : ExfloTestCase(this) {
                    override val description = "should deploy the contract"
                }
            }

            object ERC721Events : ExfloTestSuite() {

                val shouldDeployTheContract = object : ExfloTestCase(this) {
                    override val description = "should deploy the contract"
                }
            }

            object ERC777Events : ExfloTestSuite() {

                val shouldDeployTheContract = object : ExfloTestCase(this) {
                    override val description = "should deploy the contract"
                }
            }

            object ERC1155Events : ExfloTestSuite() {

                val shouldDeployTheContract = object : ExfloTestCase(this) {
                    override val description = "should deploy the contract"
                }
            }
        }
    }

    object SelfDestructs {

        object SelfDestruct : ExfloTestSuite() {

            val shouldDestroyTheContractAndRefundTheSender = object : ExfloTestCase(this) {
                override val description = "should destroy the contract and refund the sender"
            }

            val shouldDestroyAndRefundSelf = object : ExfloTestCase(this) {
                override val description = "should destroy and refund self (which triggers the destroy of ether)"
            }
        }

        object SelfDestructDelegatingCalls : ExfloTestSuite() {

            val shouldSendEtherToContractAfterSelfReferencingDestroy = object : ExfloTestCase(this) {
                override val description = "should send ether to contract after self referencing destroy"
            }

            val shouldProduceACascadingDestroyAndRefundSender = object : ExfloTestCase(this) {
                override val description = "should produce a cascading destroy and refund sender"
            }

            val shouldCreateSelfDestroyingContractsAndSelfDestructItself = object : ExfloTestCase(this) {
                override val description = "should create self destroying contracts and self destruct itself"
            }
        }

        object SelfDestructInConstructor : ExfloTestSuite() {

            val shouldCreateAndDestroyItselfOnContractDeploy = object : ExfloTestCase(this) {
                override val description = "should create and destroy itself on contract deploy"
            }
        }
    }
}
