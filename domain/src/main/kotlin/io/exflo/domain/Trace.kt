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

package io.exflo.domain

import com.google.flatbuffers.FlatBufferBuilder
import io.exflo.domain.extensions.toFlatBuffer
import io.exflo.domain.fb.ContractCreated.addAddress
import io.exflo.domain.fb.ContractCreated.addAmount
import io.exflo.domain.fb.ContractCreated.addCapabilities
import io.exflo.domain.fb.ContractCreated.addCode
import io.exflo.domain.fb.ContractCreated.addCreator
import io.exflo.domain.fb.ContractCreated.addMetadata
import io.exflo.domain.fb.ContractCreated.addTransactionHash
import io.exflo.domain.fb.ContractCreated.addType
import io.exflo.domain.fb.ContractCreated.endContractCreated
import io.exflo.domain.fb.ContractCreated.startContractCreated
import io.exflo.domain.fb.ContractDestroyed.addRefundAddress
import io.exflo.domain.fb.ContractDestroyed.addRefundAmount
import io.exflo.domain.fb.ContractDestroyed.endContractDestroyed
import io.exflo.domain.fb.ContractDestroyed.startContractDestroyed
import io.exflo.domain.fb.ContractMetadata.addCap
import io.exflo.domain.fb.ContractMetadata.addDecimals
import io.exflo.domain.fb.ContractMetadata.addGranularity
import io.exflo.domain.fb.ContractMetadata.addName
import io.exflo.domain.fb.ContractMetadata.addSymbol
import io.exflo.domain.fb.ContractMetadata.addTotalSupply
import io.exflo.domain.fb.ContractMetadata.endContractMetadata
import io.exflo.domain.fb.ContractMetadata.startContractMetadata
import io.exflo.domain.fb.InternalTransaction.addFrom
import io.exflo.domain.fb.InternalTransaction.addTo
import io.exflo.domain.fb.InternalTransaction.endInternalTransaction
import io.exflo.domain.fb.InternalTransaction.startInternalTransaction
import io.exflo.domain.fb.Reward
import io.exflo.domain.fb.Rewards
import io.exflo.domain.fb.TransactionTrace.addContractsCreated
import io.exflo.domain.fb.TransactionTrace.addContractsDestroyed
import io.exflo.domain.fb.TransactionTrace.addInternalTransactions
import io.exflo.domain.fb.TransactionTrace.addRevertReason
import io.exflo.domain.fb.TransactionTrace.addStatus
import io.exflo.domain.fb.TransactionTrace.createContractsCreatedVector
import io.exflo.domain.fb.TransactionTrace.createContractsDestroyedVector
import io.exflo.domain.fb.TransactionTrace.createInternalTransactionsVector
import io.exflo.domain.fb.TransactionTrace.endTransactionTrace
import io.exflo.domain.fb.TransactionTrace.startTransactionTrace
import org.hyperledger.besu.ethereum.core.Account
import org.hyperledger.besu.ethereum.core.Address
import org.hyperledger.besu.ethereum.core.Block
import org.hyperledger.besu.ethereum.core.Hash
import org.hyperledger.besu.ethereum.core.Transaction
import org.hyperledger.besu.ethereum.core.Wei
import org.hyperledger.besu.ethereum.debug.TraceFrame
import org.hyperledger.besu.ethereum.mainnet.TransactionProcessor
import org.hyperledger.besu.util.bytes.BytesValue
import org.hyperledger.besu.util.uint.UInt256

data class BlockTrace(
    val block: Block,
    val rewards: Map<Hash, Wei>,
    val transactionTraces: List<TransactionTrace>,
    val feesByTransaction: Map<Transaction, Wei>,
    val totalTransactionsFees: Wei
) {

    fun toRewardsFlatBuffer(bb: FlatBufferBuilder): Int {

        val rewardsOffsets: List<Int> = rewards
            .entries
            .map { reward ->
                val hashOffset = reward.key.toFlatBuffer(bb)
                val amountOffset = reward.value.toFlatBuffer(bb)
                Reward.startReward(bb)
                Reward.addHash(bb, hashOffset)
                Reward.addAmount(bb, amountOffset)
                Reward.endReward(bb)
            }

        val rewardsVectorOffset = Rewards.createRewardsVector(bb, rewardsOffsets.toIntArray())

        Rewards.startRewards(bb)
        Rewards.addRewards(bb, rewardsVectorOffset)

        return Rewards.endRewards(bb)
    }
}

data class TransactionTrace(
    val transaction: Transaction,
    val result: TransactionProcessor.Result,
    val traceFrames: List<TraceFrame>,
    val contractsCreated: List<ContractCreated>,
    val contractsDestroyed: List<ContractDestroyed>,
    val internalTransactions: List<InternalTransaction>,
    val touchedAccounts: Set<Account>
) {

    fun toFlatBuffer(bb: FlatBufferBuilder): Int {

        val contractsCreatedVectorOffset: Int = contractsCreated
            .map { contractCreated -> contractCreated.toFlatBuffer(bb) }
            .let { offsetArray ->
                createContractsCreatedVector(
                    bb,
                    offsetArray.toIntArray()
                )
            }

        val contractsDestroyedVectorOffset: Int = contractsDestroyed
            .map { contractDestroyed -> contractDestroyed.toFlatBuffer(bb) }
            .let { offsetArray ->
                createContractsDestroyedVector(
                    bb,
                    offsetArray.toIntArray()
                )
            }

        val internalTransactionsVectorOffset: Int = internalTransactions
            .map { internalTransaction -> internalTransaction.toFlatBuffer(bb) }
            .let { offsetArray ->
                createInternalTransactionsVector(
                    bb,
                    offsetArray.toIntArray()
                )
            }

        val revertReasonOffset: Int? =
            result
                .revertReason
                .map { bb.createByteVector(it.extractArray()) }.orElse(null)

        startTransactionTrace(bb)

        addContractsCreated(bb, contractsCreatedVectorOffset)
        addContractsDestroyed(bb, contractsDestroyedVectorOffset)
        addInternalTransactions(bb, internalTransactionsVectorOffset)
        addStatus(bb, result.status.ordinal.toByte())

        revertReasonOffset?.run { addRevertReason(bb, this) }

        return endTransactionTrace(bb)
    }
}

data class ContractCreated(
    val transactionHash: Hash? = null,
    val originatorAddress: Address,
    val contractAddress: Address,
    val code: BytesValue,
    val amount: Wei,
    val type: ContractType? = null,
    val capabilities: Set<ContractCapability>? = null,
    val metadata: ContractMetadata? = null,
    val pc: Int
) {

    fun toFlatBuffer(bb: FlatBufferBuilder): Int {

        val addressOffset = contractAddress.toFlatBuffer(bb)
        val creatorOffset = originatorAddress.toFlatBuffer(bb)
        val codeOffset = bb.createByteVector(code.extractArray())
        val amountOffset = amount.toFlatBuffer(bb)
        val transactionHashOffset = transactionHash?.toFlatBuffer(bb)
        val capabilitiesOffset = capabilities
            ?.map { it.ordinal.toByte() }
            ?.toByteArray()
            ?.let { elems -> bb.createByteVector(elems) }
        val metadataOffset = metadata?.toFlatBuffer(bb)

        startContractCreated(bb)

        addAddress(bb, addressOffset)
        addCreator(bb, creatorOffset)
        addCode(bb, codeOffset)
        addAmount(bb, amountOffset)
        io.exflo.domain.fb.ContractDestroyed.addPc(bb, pc)
        transactionHashOffset?.run { addTransactionHash(bb, this) }
        type?.run { addType(bb, ordinal.toByte()) }
        capabilitiesOffset?.run { addCapabilities(bb, this) }
        metadataOffset?.run { addMetadata(bb, this) }

        return endContractCreated(bb)
    }
}

data class ContractDestroyed(
    val transactionHash: Hash? = null,
    val contractAddress: Address,
    val refundAddress: Address,
    val refundAmount: Wei,
    val pc: Int
) {

    fun toFlatBuffer(bb: FlatBufferBuilder): Int {

        val addressOffset = contractAddress.toFlatBuffer(bb)
        val refundAddressOffset = refundAddress.toFlatBuffer(bb)
        val refundAmountOffset = refundAmount.toFlatBuffer(bb)

        val transactionHashOffset: Int? = transactionHash?.toFlatBuffer(bb)

        startContractDestroyed(bb)

        io.exflo.domain.fb.ContractDestroyed.addAddress(bb, addressOffset)
        addRefundAddress(bb, refundAddressOffset)
        addRefundAmount(bb, refundAmountOffset)
        io.exflo.domain.fb.ContractDestroyed.addPc(bb, pc)

        transactionHashOffset?.run { io.exflo.domain.fb.ContractDestroyed.addTransactionHash(bb, this) }

        return endContractDestroyed(bb)
    }
}

data class InternalTransaction(
    val transactionHash: Hash? = null,
    val fromAddress: Address,
    val toAddress: Address,
    val amount: Wei,
    val pc: Int
) {

    fun toFlatBuffer(bb: FlatBufferBuilder): Int {

        val fromAddressOffset = fromAddress.toFlatBuffer(bb)
        val toAddressOffset = toAddress.toFlatBuffer(bb)
        val amountOffset = amount.toFlatBuffer(bb)

        val transactionHashOffset: Int? = transactionHash?.toFlatBuffer(bb)

        startInternalTransaction(bb)

        addFrom(bb, fromAddressOffset)
        addTo(bb, toAddressOffset)
        addAmount(bb, amountOffset)
        io.exflo.domain.fb.InternalTransaction.addPc(bb, pc)
        transactionHashOffset?.run { addTransactionHash(bb, this) }

        return endInternalTransaction(bb)
    }
}

enum class ContractType {
    ERC1155,
    ERC777,
    ERC721,
    ERC20,
    GENERIC
}

enum class ContractCapability {
    ERC1155,
    ERC1155_TOKEN_RECEIVER,
    ERC777,
    ERC165,
    ERC721,
    ERC721_METADATA,
    ERC721_ENUMERABLE,
    ERC20,
    ERC20_DETAILED,
    ERC20_BURNABLE,
    ERC20_MINTABLE,
    ERC20_PAUSABLE,
    ERC20_CAPPED
}

data class ContractMetadata(
    var name: String? = null,
    var symbol: String? = null,
    var decimals: Byte? = null,
    var totalSupply: UInt256? = null,
    var granularity: UInt256? = null,
    var cap: UInt256? = null
) {

    fun toFlatBuffer(bb: FlatBufferBuilder): Int {

        val nameOffset = name?.let { bb.createString(it) }
        val symbolOffset = symbol?.let { bb.createString(it) }
        val totalSupplyOffset = totalSupply?.toFlatBuffer(bb)
        val granularityOffset = granularity?.toFlatBuffer(bb)
        val capOffset = cap?.toFlatBuffer(bb)

        startContractMetadata(bb)

        nameOffset?.let { addName(bb, nameOffset) }
        symbolOffset?.let { addSymbol(bb, symbolOffset) }
        decimals?.let { addDecimals(bb, it) }
        totalSupplyOffset?.let { addTotalSupply(bb, it) }
        granularityOffset?.let { addGranularity(bb, it) }
        capOffset?.let { addCap(bb, it) }

        return endContractMetadata(bb)
    }
}
