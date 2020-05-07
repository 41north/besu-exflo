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

package io.exflo.ingestion.tracer

import io.exflo.domain.ContractCreated
import io.exflo.domain.ContractDestroyed
import io.exflo.domain.InternalTransaction
import io.exflo.ingestion.tokens.TokenDetector
import org.apache.tuweni.bytes.Bytes
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.results.tracing.flat.FlatTrace
import org.hyperledger.besu.ethereum.core.Address
import org.hyperledger.besu.ethereum.core.Hash
import org.hyperledger.besu.ethereum.core.Wei
import org.hyperledger.besu.ethereum.transaction.TransactionSimulator
import java.lang.IllegalStateException

private val creationMethods = setOf("create", "create2")

fun FlatTrace.toContractCreated(programCounter: Int): ContractCreated {
    require(type == "create" && creationMethods.contains(action.creationMethod))

    val result = this.result.get()

    return ContractCreated(
        Hash.fromHexString(transactionHash),
        Address.fromHexString(action.from),
        Address.fromHexString(result.address),
        Bytes.fromHexString(result.code),
        Wei.fromHexString(action.value),
        null,
        null,
        null,
        programCounter
    )
}

fun FlatTrace.toContractDestroyed(programCounter: Int): ContractDestroyed {
    require(type == "suicide")
    return ContractDestroyed(
        Hash.fromHexString(transactionHash),
        Address.fromHexString(action.address),
        Address.fromHexString(action.refundAddress),
        Wei.fromHexString(action.balance),
        programCounter
    )
}

fun FlatTrace.toInternalTransaction(programCounter: Int): InternalTransaction? {
    require(type == "call" && action.callType == "call")

    val value = Wei.fromHexString(action.value)

    return if (value === Wei.ZERO) {
        null
    } else {
        InternalTransaction(
            Hash.fromHexString(transactionHash),
            Address.fromHexString(action.from),
            Address.fromHexString(action.to),
            Wei.fromHexString(action.value),
            programCounter
        )
    }
}

class TransactionTraceParser(
    private val transactionSimulator: TransactionSimulator
) {

    var programCounter = 0

    private val errorAddresses = mutableSetOf<List<Int>>()

    val contractsCreated = mutableListOf<ContractCreated>()
    val contractsDestroyed = mutableListOf<ContractDestroyed>()
    val internalTransactions = mutableListOf<InternalTransaction>()

    val touchedAccounts = mutableSetOf<Address>()

    fun apply(trace: FlatTrace) {

        programCounter += 1

        val traceAddress = trace.traceAddress

        if (trace.error != null) {
            errorAddresses.add(trace.traceAddress)
        }

        var inErrorBranch = false

        for (n in 0..traceAddress.size) {
            inErrorBranch = errorAddresses.contains(traceAddress.take(n))
            if (inErrorBranch) break
        }

        if (inErrorBranch) {
            return
        }

        when {

            trace.type == "call" && trace.action.callType == "call" && traceAddress.isEmpty() -> {
                // ignore as this is a normal transaction
            }

            trace.type == "call" && trace.action.callType == "call" && traceAddress.isNotEmpty() ->
                trace.toInternalTransaction(programCounter)
                    ?.apply {
                        internalTransactions.add(this)
                        touchedAccounts.add(this.fromAddress)
                        touchedAccounts.add(this.toAddress)
                    }

            trace.type == "create" && creationMethods.contains(trace.action.creationMethod) ->
                trace.toContractCreated(programCounter)
                    .apply {

                        touchedAccounts.add(this.originatorAddress)
                        touchedAccounts.add(this.contractAddress)

                        val (type, capabilities, metadata) = TokenDetector(
                            transactionSimulator,
                            Hash.fromHexString(trace.blockHash),
                            this.contractAddress,
                            this.code
                        ).detect()

                        contractsCreated.add(
                            this.copy(
                                type = type,
                                capabilities = capabilities,
                                metadata = metadata
                            )
                        )
                    }

            trace.type == "suicide" ->
                trace.toContractDestroyed(programCounter)
                    .apply {
                        contractsDestroyed.add(this)
                        touchedAccounts.add(this.contractAddress)
                        touchedAccounts.add(this.refundAddress)
                    }

            else -> throw IllegalStateException("Unhandled trace. Block hash = ${trace.blockHash}, tx hash = ${trace.transactionHash}, trace type = ${trace.type}")
        }
    }
}
