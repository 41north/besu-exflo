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

import com.beust.klaxon.Klaxon
import java.io.InputStream
import org.hyperledger.besu.ethereum.chain.Blockchain
import org.hyperledger.besu.ethereum.core.Block
import org.hyperledger.besu.ethereum.core.Hash
import org.hyperledger.besu.ethereum.core.TransactionReceipt

abstract class ExfloTestSuite {

    // take our id from the simple name of the implementing class
    open val title: String = this::class.java.simpleName
}

abstract class ExfloTestCase(val suite: ExfloTestSuite) {
    abstract val description: String
}

class ExfloTestCaseHelper(
    private val blockchain: Blockchain,
    testReportStream: InputStream
) {

    private val klaxon = Klaxon()

    private val report = klaxon.parse<TruffleReport>(testReportStream)!!

    private val suitesByTitle = report
        .results
        .map { result ->
            result.title to result.tests
                .map { test -> test.description to test }
                .toMap()
        }
        .toMap()

    fun web3TestResultFor(testCase: ExfloTestCase) =
        suitesByTitle[testCase.suite.title]
            ?.get(testCase.description)

    fun blockHashesFor(testCase: ExfloTestCase): List<String> =
        web3TestResultFor(testCase)
            ?.let { result -> result.web3.summaries.map { summary -> summary.blockHash } }
            .orEmpty()

    fun blocksFor(testCase: ExfloTestCase): List<Block> =
        blockHashesFor(testCase)
            .map { hash -> blockchain.getBlockByHash(Hash.fromHexString(hash)) }
            .filter { it.isPresent }
            .map { it.get() }

    fun blocksWithReceiptsFor(testCase: ExfloTestCase): List<Pair<Block, List<TransactionReceipt>>> =
        blocksFor(testCase)
            .map { block -> Pair(block, blockchain.getTxReceipts(block.hash).orElse(emptyList())) }
}

data class TruffleReport(val results: List<TestSuite>)

data class TestSuite(
    val uuid: String,
    val title: String,
    val file: String,
    val tests: List<TestResult>
)

data class TestResult(
    val uuid: String,
    val description: String,
    val web3: Web3TestResult
)

data class Web3TestResult(
    val summaries: List<Web3TestResultSummary>
)

data class Web3TestResultSummary(
    val blockNumber: Long,
    val blockHash: String,
    val txHash: String,
    val txStatus: Boolean
)
