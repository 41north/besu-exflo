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

package io.exflo.ingestion.tokens.detectors

import io.exflo.ingestion.TruffleSpecs.Tokens.ERC20.CappedERC20
import io.exflo.ingestion.TruffleSpecs.Tokens.ERC20.DetailedERC20
import io.exflo.ingestion.TruffleSpecs.Tokens.ERC20.InvalidERC20
import io.exflo.ingestion.TruffleSpecs.Tokens.ERC20.MinimalERC20
import io.exflo.ingestion.tokens.precompiled.ERC20DetectorPrecompiledContract
import io.exflo.testutil.ExfloTestCase
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe

class ERC20DetectorSpec : AbstractTokenDetectorSpec() {

    init {

        context("with a generic contract") {

            val detector = detectorFor(InvalidERC20.shouldDeployTheContract)

            test("we should not detect any interfaces being implemented") {
                detector.hasERC20Interface() shouldBe false
                detector.hasERC20DetailedInterface() ?: false shouldBe false
                detector.hasERC20CappedInterface() ?: false shouldBe false
                detector.hasERC20BurnableInterface() shouldBe false
                detector.hasERC20MintableInterface() shouldBe false
                detector.hasERC20PausableInterface() shouldBe false
            }
        }

        context("with a minimal ERC20 contract") {

            val detector = detectorFor(MinimalERC20.shouldDeployTheContract)

            test("we should only detect a minimal ERC20 interface") {
                detector.hasERC20Interface() shouldBe true
                detector.hasERC20DetailedInterface() ?: false shouldBe false
                detector.hasERC20CappedInterface() ?: false shouldBe false
                detector.hasERC20BurnableInterface() shouldBe false
                detector.hasERC20MintableInterface() shouldBe false
                detector.hasERC20PausableInterface() shouldBe false
            }
        }

        context("with a detailed ERC20 contract") {

            val detector = detectorFor(DetailedERC20.shouldDeployTheContract)

            test("we should detected a minimal ERC20 and a detailed ERC20 interface") {
                detector.hasERC20Interface() shouldBe true
                detector.hasERC20DetailedInterface() ?: false shouldBe true
                detector.hasERC20CappedInterface() ?: false shouldBe false
                detector.hasERC20BurnableInterface() shouldBe false
                detector.hasERC20MintableInterface() shouldBe false
                detector.hasERC20PausableInterface() shouldBe false
            }
        }

        context("with a capped ERC20 contract") {

            val detector = detectorFor(CappedERC20.shouldDeployTheContract)

            test("we should detect ERC20, mintable and capped interfaces") {
                detector.hasERC20Interface() shouldBe true
                detector.hasERC20DetailedInterface() ?: false shouldBe false
                detector.hasERC20CappedInterface() ?: false shouldBe true
                detector.hasERC20BurnableInterface() shouldBe false
                detector.hasERC20MintableInterface() shouldBe true
                detector.hasERC20PausableInterface() shouldBe false
            }
        }
    }

    private fun detectorFor(testCase: ExfloTestCase): ERC20Detector {
        val block = testHelper.blocksFor(testCase).first()

        // Gather transaction
        val tx = block.body.transactions.first()
        tx shouldNotBe null

        // Gather contract address
        val contractAddress = tx.contractAddress().orElse(null)
        contractAddress shouldNotBe null

        // create detector
        return ERC20Detector(
            transactionSimulator,
            ERC20DetectorPrecompiledContract.ADDRESS,
            contractAddress,
            block.hash,
            tx.payload
        )
    }
}
