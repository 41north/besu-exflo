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

import io.exflo.ingestion.TruffleSpecs.Tokens.ERC721.FullERC721
import io.exflo.ingestion.TruffleSpecs.Tokens.ERC721.InvalidERC721
import io.exflo.ingestion.TruffleSpecs.Tokens.ERC721.MinimalERC721
import io.exflo.ingestion.tokens.detectors.ERC721Detector
import io.exflo.ingestion.tokens.precompiled.ERC721DetectorPrecompiledContract
import io.exflo.testutil.ExfloTestCase
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe

class ERC721DetectorSpec : AbstractDetectorSpec() {

    init {

        context("given a generic contract") {

            val detector = detectorFor(InvalidERC721.shouldDeployTheContract)

            test("we should detect no ERC721 interfaces") {
                detector.hasERC721Interface() ?: false shouldBe false
                detector.hasERC721EnumerableInterface() ?: false shouldBe false
                detector.hasERC721MetadataInterface() ?: false shouldBe false
            }
        }

        context("given a minimal ERC721 contract") {

            val detector = detectorFor(MinimalERC721.shouldDeployTheContract)

            test("we should detect minimal interfaces") {
                detector.hasERC721Interface() ?: false shouldBe true
                detector.hasERC721EnumerableInterface() ?: false shouldBe false
                detector.hasERC721MetadataInterface() ?: false shouldBe false
            }
        }

        context("given a full ERC721 contract") {

            val detector = detectorFor(FullERC721.shouldDeployTheContract)

            test("we should detect all the ERC721 interfaces") {
                detector.hasERC721Interface() ?: false shouldBe true
                detector.hasERC721EnumerableInterface() ?: false shouldBe true
                detector.hasERC721MetadataInterface() ?: false shouldBe true
            }
        }
    }

    private fun detectorFor(testCase: ExfloTestCase): ERC721Detector {
        // Gather data
        val block = testHelper.blocksFor(testCase).first()

        // Gather transaction
        val tx = block.body.transactions.first()
        tx shouldNotBe null

        // Gather contract address
        val contractAddress = tx.contractAddress().orElse(null)
        contractAddress shouldNotBe null

        // create detector
        return ERC721Detector(
            transactionSimulator,
            ERC721DetectorPrecompiledContract.ADDRESS,
            contractAddress,
            block.hash
        )
    }
}
