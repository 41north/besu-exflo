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

package io.exflo.ingestion.tracker

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import io.exflo.domain.BlockTrace
import io.exflo.domain.TransactionTrace as ExfloTransactionTrace
import io.exflo.ingestion.core.FullBlock
import io.exflo.ingestion.extensions.toBalanceDeltas
import io.exflo.ingestion.extensions.touchedAccounts
import io.exflo.ingestion.tracer.TransactionTraceParser
import org.apache.logging.log4j.LogManager
import org.apache.tuweni.units.bigints.UInt256
import org.hyperledger.besu.cli.config.EthNetworkConfig
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.processor.TransactionTrace
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.results.tracing.Trace
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.results.tracing.flat.FlatTrace
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.results.tracing.flat.FlatTraceGenerator
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.results.tracing.flat.RewardTraceGenerator
import org.hyperledger.besu.ethereum.chain.BlockchainStorage
import org.hyperledger.besu.ethereum.core.Account
import org.hyperledger.besu.ethereum.core.Block
import org.hyperledger.besu.ethereum.core.BlockBody
import org.hyperledger.besu.ethereum.core.BlockHeader
import org.hyperledger.besu.ethereum.core.Hash
import org.hyperledger.besu.ethereum.core.Transaction
import org.hyperledger.besu.ethereum.core.TransactionReceipt
import org.hyperledger.besu.ethereum.core.Wei
import org.hyperledger.besu.ethereum.debug.TraceOptions
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule
import org.hyperledger.besu.ethereum.transaction.TransactionSimulator
import org.hyperledger.besu.ethereum.vm.DebugOperationTracer
import org.hyperledger.besu.ethereum.worldstate.WorldStateArchive
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.processor.BlockTracer as BesuBlockTracer
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.processor.BlockReplay as BesuBlockReplay

class BlockReader : KoinComponent {

    private val blockchainStorage: BlockchainStorage by inject()

    private val networkConfig: EthNetworkConfig by inject()
    private val worldStateArchive: WorldStateArchive by inject()
    private val protocolSchedule: ProtocolSchedule<*> by inject()
    private val objectMapper: ObjectMapper by inject()

    private val transactionSimulator: TransactionSimulator by inject()

    private val besuBlockReplay: BesuBlockReplay by inject()

    private val log = LogManager.getLogger()

    fun chainHead(): Hash? = blockchainStorage.chainHead.orElse(null)

    fun fullBlock(
        hash: Hash,
        withHeader: Boolean = true,
        withBody: Boolean = true,
        withReceipts: Boolean = true,
        withTrace: Boolean = true
    ): FullBlock? = block(hash)?.let { fullBlock(it, withHeader, withBody, withReceipts, withTrace) }

    fun fullBlock(
        block: Block,
        withHeader: Boolean = true,
        withBody: Boolean = true,
        withReceipts: Boolean = true,
        withTrace: Boolean = true
    ): FullBlock? =
        block.let {
            val header = if (withHeader) it.header else null
            val totalDifficulty =
                if (withHeader) requireNotNull(totalDifficulty(it.hash)) { "totalDifficulty not found" } else null

            val body = if (withBody) it.body else null
            val receipts =
                if (withBody && withReceipts) requireNotNull(receipts(it.hash)) { "receipts not found" } else emptyList()

            val trace = if (withTrace) requireNotNull(trace(it.hash)) { "trace not found" } else null
            val touchedAccounts = trace?.let { t -> touchedAccounts(t) }
            val balanceDeltas = trace?.toBalanceDeltas(
                it.hash,
                it.header.coinbase,
                it.body.ommers.map { h -> Pair(h.hash, h.coinbase) }.toMap()
            )

            FullBlock(
                header,
                body,
                receipts,
                totalDifficulty,
                trace,
                touchedAccounts,
                balanceDeltas
            )
        }

    fun headersFrom(head: Hash, count: Int): List<BlockHeader> {

        var hash = head
        var headers = listOf<BlockHeader>()

        do {
            val header = this.header(hash)
            header?.let { h ->
                headers = headers + h
                hash = h.parentHash
            }
        } while (header != null && header.number >= BlockHeader.GENESIS_BLOCK_NUMBER && headers.size < count)

        return headers.toList()
    }

    fun header(hash: Hash): BlockHeader? =
        blockchainStorage.getBlockHeader(hash).orElse(null)

    fun header(number: Long): BlockHeader? =
        blockchainStorage.getBlockHash(number)
            .flatMap { hash -> blockchainStorage.getBlockHeader(hash) }
            .orElse(null)

    fun body(hash: Hash): BlockBody? =
        blockchainStorage.getBlockBody(hash).orElse(null)

    fun block(hash: Hash): Block? =
        header(hash)?.let { header -> Block(header, requireNotNull(body(hash)) { "body not found" }) }

    fun receipts(hash: Hash): List<TransactionReceipt>? =
        blockchainStorage.getTransactionReceipts(hash).orElse(null)

    fun trace(hash: Hash): BlockTrace? =
        block(hash)
            ?.let { block -> Pair(block, requireNotNull(receipts(hash)) { "receipts not found" }) }
            ?.let { (block, receipts) ->

                val resultArrayNode = objectMapper.createArrayNode()

                val exfloTxTraces = BesuBlockTracer(besuBlockReplay)
                    .trace(hash, DebugOperationTracer(TraceOptions.DEFAULT))
                    .orElse(null)
                    ?.let { blockTrace ->

                        generateTracesFromTransactionTraceAndBlock(
                            blockTrace.transactionTraces, block, resultArrayNode
                        )
                    } ?: emptyList()

                val rewardsMap = rewards(block)

                val feesByTransaction = feesByTransaction(block, receipts)
                val transactionFees = totalTransactionFees(feesByTransaction)

                BlockTrace(block, rewardsMap, exfloTxTraces, feesByTransaction, transactionFees, resultArrayNode.toString())
            }

    private fun generateTracesFromTransactionTraceAndBlock(
        transactionTraces: List<TransactionTrace>,
        block: Block,
        resultArrayNode: ArrayNode
    ): List<ExfloTransactionTrace> =
        transactionTraces.map { txTrace ->

            val txTraceParser = TransactionTraceParser(transactionSimulator)

            val traceStream = FlatTraceGenerator.generateFromTransactionTraceAndBlock(
                protocolSchedule, txTrace, block
            )

            traceStream.forEachOrdered { trace ->
                txTraceParser.apply(trace as FlatTrace)
                resultArrayNode.addPOJO(trace)
            }

            ExfloTransactionTrace(
                txTrace.transaction,
                txTrace.result,
                txTraceParser.contractsCreated.toList(),
                txTraceParser.contractsDestroyed.toList(),
                txTraceParser.internalTransactions.toList(),
                txTraceParser.touchedAccounts.toSet()
            )
        }

    private fun rewards(block: Block): Map<Hash, Wei> {
        val number = block.header.number
        val blockHash = block.header.hash
        val protocolSpec = protocolSchedule.getByBlockNumber(number)
        val blockReward = protocolSpec.blockReward
        val ommersSize = block.body.ommers.size.toLong()

        if (number == BlockHeader.GENESIS_BLOCK_NUMBER || (blockReward.isZero && protocolSpec.isSkipZeroBlockRewards))
            return emptyMap()

        var rewards = mapOf<Hash, Wei>()

        // Include "uncle inclusion rewards" if there are ommers
        val coinbaseReward = blockReward.plus(blockReward.multiply(ommersSize).divide(32))

        rewards = rewards + (blockHash to coinbaseReward)

        block.body.ommers
            .forEach { ommer ->

                val ommerHash = ommer.hash
                val distance = number - ommer.number

                val ommerRewardDelta = blockReward.subtract(blockReward.multiply(distance).divide(8))

                rewards = rewards + (ommerHash to ommerRewardDelta)
            }

        return rewards
    }

    private fun generateRewardsFromBlock(
        block: Block,
        resultArrayNode: ArrayNode
    ): Map<Hash, Wei> {
        var rewardsMap = mapOf<Hash, Wei>()

        RewardTraceGenerator.generateFromBlock(protocolSchedule, block)
            .forEachOrdered { trace: Trace ->
                resultArrayNode.addPOJO(trace)

                val flatTrace = trace as FlatTrace
                require(flatTrace.type == "reward")
                require(flatTrace.action.rewardType == "uncle" || flatTrace.action.rewardType == "block")

                val author = Hash.fromHexString(flatTrace.action.author)
                val reward = Wei.fromHexString(requireNotNull(flatTrace.action.value))

                rewardsMap = rewardsMap + (author to (rewardsMap.getOrDefault(author, Wei.ZERO).plus(reward)))
            }

        return rewardsMap
    }

    private fun feesByTransaction(block: Block, receipts: List<TransactionReceipt>): Map<Transaction, Wei> {
        var cumulativeGasUsed = 0L

        return block.body
            .transactions
            .zip(receipts)
            .map { (tx, receipt) ->

                val gasUsed = receipt.cumulativeGasUsed - cumulativeGasUsed
                cumulativeGasUsed = receipt.cumulativeGasUsed

                tx to Wei.of(gasUsed).multiply(tx.gasPrice)
            }
            .toMap()
    }

    private fun totalTransactionFees(feesByTransaction: Map<Transaction, Wei>): Wei =
        feesByTransaction
            .map { it.value }
            .fold(Wei.ZERO) { total, next -> total.plus(next) }

    fun totalDifficulty(hash: Hash): UInt256? =
        blockchainStorage.getTotalDifficulty(hash).map { it.toUInt256() }.orElse(null)

    fun touchedAccounts(trace: BlockTrace): List<Account> = trace.touchedAccounts(networkConfig, worldStateArchive)
}
