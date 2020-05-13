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

import io.exflo.ingestion.TruffleSpecs.Tokens.ERC777.InvalidERC777
import io.exflo.ingestion.TruffleSpecs.Tokens.ERC777.MinimalERC777
import io.exflo.ingestion.tokens.precompiled.ERC777DetectorPrecompiledContract
import io.exflo.testutil.ExfloTestCase
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe

class ERC777DetectorSpec : AbstractTokenDetectorSpec() {

  init {

    context("given a generic contract") {

      val detector = detectorFor(InvalidERC777.shouldDeployTheContract)

      test("we should not detect any interfaces") {
        detector.hasERC777Interface() ?: false shouldBe false
      }
    }

    context("given a minimal ERC777 contract") {

      val detector = detectorFor(MinimalERC777.shouldDeployTheContract)

      test("we should detect ERC777 interface") {
        detector.hasERC777Interface() ?: false shouldBe true
      }
    }
  }

  private fun detectorFor(testCase: ExfloTestCase): ERC777Detector {
    // Gather data
    val block = testHelper.blocksFor(testCase).first()

    // Gather transaction
    val tx = block.body.transactions.first()
    tx shouldNotBe null

    // Gather contract address
    val contractAddress = tx.contractAddress().orElse(null)
    contractAddress shouldNotBe null

    // create detector
    return ERC777Detector(
      transactionSimulator,
      ERC777DetectorPrecompiledContract.ADDRESS,
      contractAddress,
      block.hash
    )
  }
}
