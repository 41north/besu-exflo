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
/*
 * Copyright (c) 2019 41North.
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

import io.exflo.ingestion.KoinTestIngestionModules
import io.exflo.ingestion.tokens.precompiled.PrecompiledContractsFactory
import io.exflo.testutil.ExfloTestCaseHelper
import io.exflo.testutil.TestChainLoader
import io.kotlintest.Spec
import io.kotlintest.TestCase
import io.kotlintest.TestResult
import io.kotlintest.extensions.TopLevelTest
import io.kotlintest.specs.FunSpec
import org.hyperledger.besu.cli.config.EthNetworkConfig
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule
import org.hyperledger.besu.ethereum.transaction.TransactionSimulator
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject

abstract class AbstractTokenDetectorSpec : FunSpec(), KoinTest {

    protected val testChainLoader: TestChainLoader by inject()
    protected val testHelper: ExfloTestCaseHelper by inject()
    protected val networkConfig: EthNetworkConfig by inject()
    protected val protocolSchedule: ProtocolSchedule<*> by inject()
    protected val transactionSimulator: TransactionSimulator by inject()

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
}
