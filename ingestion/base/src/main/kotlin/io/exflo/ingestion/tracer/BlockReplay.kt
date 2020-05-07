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

import io.exflo.domain.BlockTrace
import io.exflo.domain.TransactionTrace
import org.hyperledger.besu.ethereum.chain.Blockchain
import org.hyperledger.besu.ethereum.core.Block
import org.hyperledger.besu.ethereum.core.BlockBody
import org.hyperledger.besu.ethereum.core.BlockHeader
import org.hyperledger.besu.ethereum.core.Hash
import org.hyperledger.besu.ethereum.core.Transaction
import org.hyperledger.besu.ethereum.core.TransactionReceipt
import org.hyperledger.besu.ethereum.core.Wei
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule
import org.hyperledger.besu.ethereum.mainnet.ProtocolSpec
import org.hyperledger.besu.ethereum.worldstate.WorldStateArchive

/**
 * Process a given [Block] and returns an optional [BlockTrace].
 */
class BlockReplay(
    private val protocolSchedule: ProtocolSchedule<*>,
    private val blockchain: Blockchain,
    private val worldStateArchive: WorldStateArchive
) {

    fun block(block: Block, receipts: List<TransactionReceipt>, action: ReplayAction<TransactionTrace>): BlockTrace? =
        performActionWithBlock(
            block.header,
            block.body
        ) { body, header, blockchain, mutableWorldState, transactionProcessor ->

            val transactionTraces =
                body.transactions
                    .mapIndexed { pos, tx ->
                        action(
                            pos,
                            tx,
                            header,
                            blockchain,
                            mutableWorldState,
                            transactionProcessor
                        )
                    }
            val rewards = rewards(block)
            val feesByTransaction = feesByTransaction(block, receipts)
            val transactionFees = totalTransactionFees(feesByTransaction)

            BlockTrace(
                block,
                rewards,
                transactionTraces,
                feesByTransaction,
                transactionFees,
                ""
            )
        }

    private fun <T> performActionWithBlock(header: BlockHeader?, body: BlockBody?, action: BlockAction<T>): T? =
        when {
            header == null || body == null -> null
            else -> {
                val protocolSpec: ProtocolSpec<*> = protocolSchedule.getByBlockNumber(header.number)
                val transactionProcessor = protocolSpec.transactionProcessor

                val previous =
                    if (header.number == BlockHeader.GENESIS_BLOCK_NUMBER)
                        blockchain.genesisBlock.header
                    else
                        blockchain.getBlockHeader(header.parentHash).get()

                val mutableWorldState = worldStateArchive.getMutable(previous.stateRoot).orElse(null)

                when {
                    previous == null || mutableWorldState == null -> null
                    else -> action(body, header, blockchain, mutableWorldState, transactionProcessor)
                }
            }
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
}
