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

package io.exflo.ingestion.extensions

import io.exflo.domain.BalanceDelta
import io.exflo.domain.BlockTrace
import io.exflo.domain.ContractEvent
import io.exflo.domain.DeltaType
import io.exflo.domain.extensions.bigInteger
import io.exflo.ingestion.core.InMemoryAccount
import io.exflo.ingestion.tokens.events.LogParser
import java.util.stream.Collectors
import org.hyperledger.besu.cli.config.EthNetworkConfig
import org.hyperledger.besu.config.GenesisConfigFile
import org.hyperledger.besu.ethereum.core.Account
import org.hyperledger.besu.ethereum.core.Address
import org.hyperledger.besu.ethereum.core.BlockHeader
import org.hyperledger.besu.ethereum.core.Hash
import org.hyperledger.besu.ethereum.core.TransactionReceipt
import org.hyperledger.besu.ethereum.core.Wei
import org.hyperledger.besu.ethereum.worldstate.WorldStateArchive
import org.hyperledger.besu.util.uint.UInt256

fun UInt256.bigInteger() = byteArray.bigInteger()

fun UInt256.bigDecimal() = this.bigInteger().toBigDecimal()

fun Wei.bigInteger() = this.asUInt256().bigInteger()

fun Wei.bigDecimal() = this.bigInteger().toBigDecimal()

fun TransactionReceipt.contractEvents(): List<ContractEvent> =
    LogParser.parse(this)

fun BlockTrace.toBalanceDeltas(
    blockHash: Hash,
    coinbase: Address,
    // map of ommer header hash to coinbase
    ommerCoinbaseMap: Map<Hash, Address>
): List<BalanceDelta> {

    // Rewards

    val rewardBalanceDeltas = rewards
        .map { (hash, amount) ->

            val (deltaType, to) = when (blockHash == hash) {
                true -> Pair(DeltaType.BLOCK_REWARD, coinbase)
                false -> Pair(DeltaType.OMMER_REWARD, ommerCoinbaseMap[hash])
            }

            BalanceDelta(
                deltaType = deltaType,
                to = to,
                amount = amount,
                pc = -1
            )
        }

    // Transactions

    val txsBalanceDeltas = this
        .transactionTraces
        .mapIndexed { idx, transactionTrace ->

            val deltas = mutableListOf<BalanceDelta>()

            val tx = transactionTrace.transaction

            // For Tx

            deltas += BalanceDelta(
                DeltaType.TX,
                from = tx.sender,
                to = if (tx.isContractCreation) tx.contractAddress().get() else tx.to.get(),
                amount = tx.value,
                transactionHash = tx.hash,
                transactionIndex = idx,
                pc = -1
            )

            // For TraceEvents: contracts created

            deltas += transactionTrace
                .contractsCreated
                .map { ev ->

                    BalanceDelta(
                        DeltaType.CONTRACT_CREATION,
                        from = ev.originatorAddress,
                        to = ev.contractAddress,
                        amount = ev.amount,
                        transactionHash = ev.transactionHash,
                        transactionIndex = idx,
                        pc = ev.pc
                    )
                }

            // For TraceEvents: contracts destroyed

            deltas += transactionTrace
                .contractsDestroyed
                .map { ev ->

                    BalanceDelta(
                        DeltaType.CONTRACT_DESTRUCTION,
                        from = ev.contractAddress,
                        to = ev.refundAddress,
                        amount = ev.refundAmount,
                        transactionHash = ev.transactionHash,
                        transactionIndex = idx,
                        pc = ev.pc
                    )
                }

            // For TraceEvents: internal transactions

            deltas += transactionTrace
                .internalTransactions
                .map { ev ->

                    BalanceDelta(
                        DeltaType.INTERNAL_TX,
                        from = ev.fromAddress,
                        to = ev.toAddress,
                        amount = ev.amount,
                        transactionHash = ev.transactionHash,
                        transactionIndex = idx,
                        pc = ev.pc
                    )
                }

            deltas
        }
        .flatten()

    // Transaction fees

    val txFeesBalanceDeltas = this
        .feesByTransaction
        .entries
        .mapIndexed { idx, (tx, fee) ->

            BalanceDelta(
                DeltaType.TX_FEE,
                from = tx.sender,
                to = coinbase,
                amount = fee,
                transactionHash = tx.hash,
                transactionIndex = idx,
                pc = -1
            )
        }

    return rewardBalanceDeltas + txsBalanceDeltas + txFeesBalanceDeltas
}

fun BlockTrace.touchedAccounts(
    networkConfig: EthNetworkConfig,
    worldStateArchive: WorldStateArchive
): List<Account> {

    val block = this.block
    val header = block.header
    val body = block.body
    val transactionTraces = this.transactionTraces

    val txAccounts = when {
        // TODO handle hard forks
        block.header.number > BlockHeader.GENESIS_BLOCK_NUMBER -> transactionTraces
            .flatMap { txTrace -> txTrace.touchedAccounts.map { account -> account.address to account } }
            .toMap()

        // For genesis block we need to pull the pre allocations
        else -> GenesisConfigFile.fromConfig(networkConfig.genesisConfig)
            .streamAllocations()
            .map<InMemoryAccount> { InMemoryAccount.fromGenesisAllocation(it) }
            .collect(Collectors.toList())
            .map { inMemoryAccount -> inMemoryAccount.address to inMemoryAccount }
            .toMap()
    }

    val allAccounts = txAccounts.let { map ->
        // need to add reward accounts
        val worldState = worldStateArchive.get(header.stateRoot).get()

        // start with the coinbase
        val minersByHash = mutableMapOf(block.header.hash to block.header.coinbase)

        // add in the ommers
        body.ommers.forEach { ommer -> minersByHash += ommer.hash to ommer.coinbase }

        map + this.rewards
            .map { (hash, _) ->
                val address = minersByHash[hash]
                address to worldState[address]
            }.toMap()
    }

    return allAccounts
        // Even if EIP158 is not enabled, we avoid serializing any empty / dead / unnecessary accounts
        .filterNot { (_, acc) -> acc.isEmpty }
        .values
        .toList()
}
