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

import io.exflo.ingestion.TruffleSpecs.Tokens.ERC165.ERC165Contract
import io.exflo.ingestion.TruffleSpecs.Tokens.ERC165.NonERC165
import io.exflo.ingestion.tokens.detectors.ERC165Detector
import io.exflo.ingestion.tokens.precompiled.ERC165DetectorPrecompiledContract
import io.exflo.testutil.ExfloTestCase
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe

class ERC165DetectorSpec : AbstractDetectorSpec() {

    init {

        context("given a generic contract") {

            val detector = detectorFor(NonERC165.shouldDeployTheContract)

            test("we should not detect an ERC165 interface") {
                detector.hasERC165Interface() ?: false shouldBe false
            }
        }

        context("given a contract that implements ERC165") {

            val detector = detectorFor(ERC165Contract.shouldDeployTheContract)

            test("we should detect an ERC165 interface") {
                detector.hasERC165Interface() shouldBe true
            }
        }
    }

    private fun detectorFor(testCase: ExfloTestCase): ERC165Detector {
        // Gather data
        val block = testHelper.blocksFor(testCase).first()

        // Gather transaction
        val tx = block.body.transactions.first()
        tx shouldNotBe null

        // Gather contract address
        val contractAddress = tx.contractAddress().orElse(null)
        contractAddress shouldNotBe null

        // create detector
        return ERC165Detector(
            transactionSimulator,
            ERC165DetectorPrecompiledContract.ADDRESS,
            contractAddress,
            block.hash
        )
    }
}
