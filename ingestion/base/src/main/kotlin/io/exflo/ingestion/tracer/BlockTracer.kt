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
import io.exflo.ingestion.extensions.measureTimeMillis
import io.exflo.ingestion.tokens.TokenDetector
import org.apache.logging.log4j.LogManager
import org.hyperledger.besu.ethereum.chain.Blockchain
import org.hyperledger.besu.ethereum.core.Block
import org.hyperledger.besu.ethereum.core.BlockBody
import org.hyperledger.besu.ethereum.core.BlockHeader
import org.hyperledger.besu.ethereum.core.MutableWorldState
import org.hyperledger.besu.ethereum.core.Transaction
import org.hyperledger.besu.ethereum.core.TransactionReceipt
import org.hyperledger.besu.ethereum.mainnet.TransactionProcessor
import org.hyperledger.besu.ethereum.transaction.TransactionSimulator
import org.hyperledger.besu.ethereum.vm.BlockHashLookup
import org.hyperledger.besu.ethereum.vm.MessageFrame

/**
 * Used to produce debug traces of blocks.
 */
class BlockTracer(
    private val blockReplay: BlockReplay,
    private val transactionSimulator: TransactionSimulator
) {

    private val log = LogManager.getLogger()

    fun trace(block: Block, receipts: List<TransactionReceipt>): BlockTrace? =
        blockReplay
            .block(
                block,
                receipts
            ) { txIndex, transaction, header, blockchain, mutableWorldState, transactionProcessor ->

                val updater = mutableWorldState.updater()

                val tracer = ExfloOperationTracer()

                val (result, elapsedMs) = measureTimeMillis {
                    transactionProcessor.processTransaction(
                        blockchain,
                        updater,
                        header,
                        transaction,
                        header.coinbase,
                        tracer,
                        BlockHashLookup(header, blockchain),
                        false
                    )
                }

                val touchedAccounts = updater.touchedAccounts.toSet()

                // required to keep our temporary mutable world state consistent with transaction execution
                updater.commit()

                val trace =
                    TransactionTrace(
                        transaction,
                        result,
                        tracer.traceFrames,
                        tracer.contractsCreated
                            .map { elem -> Triple(elem.frame, elem.pc, elem.event) }
                            .filter { (frame, pc, _) ->
                                if (result.isInvalid) return@filter false

                                val state = frame.state
                                if (state != MessageFrame.State.COMPLETED_SUCCESS) return@filter false

                                val depth = frame.messageStackDepth
                                val nextDepth = depth + 1

                                val type = frame.type

                                val depthStatusCheckSuccess = when {
                                    type == MessageFrame.Type.CONTRACT_CREATION && pc == 0 && depth == 0 -> true
                                    else -> tracer.depthFrames[nextDepth]?.pollFirst()?.state == MessageFrame.State.COMPLETED_SUCCESS
                                }

                                depthStatusCheckSuccess
                            }
                            .map { (_, _, ev) ->
                                val (contractType, capabilities, metadata) = TokenDetector(
                                    transactionSimulator,
                                    block.hash,
                                    ev.contractAddress,
                                    ev.code
                                ).detect()
                                ev.copy(
                                    transactionHash = transaction.hash,
                                    type = contractType,
                                    capabilities = capabilities,
                                    metadata = metadata
                                )
                            },
                        tracer.contractsDestroyed
                            .filter { result.isSuccessful && it.frame.state == MessageFrame.State.COMPLETED_SUCCESS }
                            .map { it.event.copy(transactionHash = transaction.hash) },
                        tracer.internalTransactions
                            .filter { result.isSuccessful && it.frame.state == MessageFrame.State.COMPLETED_SUCCESS && !it.event.amount.isZero }
                            .map { it.event.copy(transactionHash = transaction.hash) },
                        touchedAccounts
                    )

                log.trace("Tracing -> Block: ${block.header.number} | Tx Hash: ${transaction.hash} | Tx Index: $txIndex | Contracts created: ${trace.contractsCreated.size} | Contracts destroyed: ${trace.contractsDestroyed.size} | Internal Txs: ${trace.internalTransactions.size} | Traced in $elapsedMs ms")

                trace
            }
}

typealias BlockAction<T> = (
    body: BlockBody,
    blockHeader: BlockHeader,
    blockchain: Blockchain,
    worldState: MutableWorldState,
    transactionProcessor: TransactionProcessor
) -> T?

typealias ReplayAction<T> = (
    transactionIndex: Int,
    transaction: Transaction,
    blockHeader: BlockHeader,
    blockchain: Blockchain,
    worldState: MutableWorldState,
    transactionProcessor: TransactionProcessor
) -> T
