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

package io.exflo.ingestion.postgres

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.exflo.ingestion.tracker.BlockReader
import io.exflo.testutil.KoinTestModules
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.processor.BlockReplay
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.processor.BlockTracer
import org.hyperledger.besu.ethereum.transaction.TransactionSimulator
import org.koin.dsl.module

object KoinPostgresIngestionModules {

  private val ingestion = module {
    single { BlockReplay(get(), get(), get()) }

    single { BlockTracer(get()) }

    single { TransactionSimulator(get(), get(), get()) }

    single { BlockReader() }

    single { ObjectMapper().registerModule(KotlinModule()) }
  }

  operator fun invoke() = KoinTestModules() + ingestion
}
