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

import io.exflo.domain.ContractEvent
import org.hyperledger.besu.ethereum.core.Log as BesuLog
import org.hyperledger.besu.ethereum.core.TransactionReceipt as BesuTransactionReceipt

/**
 * This class parses produced [org.hyperledger.besu.ethereum.core.Log] in [TransactionReceipt] to search for potential
 * token transfers.
 */
object LogParser {

    fun parse(log: BesuLog): ContractEvent? {
        val events = ContractEventParsers.values().mapNotNull { it.parse(log) }
        require(events.size <= 1) { "More than one event spec has matched with this log" }
        return events.firstOrNull()
    }

    fun parse(receipt: BesuTransactionReceipt): List<ContractEvent> =
        receipt.logs.mapNotNull { log -> parse(log) }
}
