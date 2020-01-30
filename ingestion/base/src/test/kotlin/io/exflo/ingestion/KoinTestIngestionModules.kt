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

import io.exflo.ingestion.tracer.BlockReplay
import io.exflo.ingestion.tracer.BlockTracer
import io.exflo.testutil.KoinTestModules
import org.hyperledger.besu.ethereum.transaction.TransactionSimulator
import org.koin.dsl.module

@Suppress("MemberVisibilityCanBePrivate")
object KoinTestIngestionModules {

    val blockReplay = module {
        single { BlockReplay(get(), get(), get()) }
    }

    val tracer = module {
        single { BlockTracer(get(), get()) }
    }

    val simulator = module {
        single { TransactionSimulator(get(), get(), get()) }
    }

    val defaultModuleList = KoinTestModules.defaultModuleList + listOf(
        blockReplay,
        tracer,
        simulator
    )
}
