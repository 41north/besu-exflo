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

package io.exflo.domain.extensions

import com.google.flatbuffers.FlatBufferBuilder
import io.exflo.domain.BlockTrace
import io.exflo.domain.ContractEvent
import io.exflo.domain.TransactionTrace
import io.exflo.domain.fb.Account
import io.exflo.domain.fb.Account.addAddress
import io.exflo.domain.fb.Account.addBalance
import io.exflo.domain.fb.Account.addCode
import io.exflo.domain.fb.Account.addCodeHash
import io.exflo.domain.fb.Account.endAccount
import io.exflo.domain.fb.Account.startAccount
import io.exflo.domain.fb.BlockBody.addOmmers
import io.exflo.domain.fb.BlockBody.addTransactions
import io.exflo.domain.fb.BlockBody.createOmmersVector
import io.exflo.domain.fb.BlockBody.createTransactionsVector
import io.exflo.domain.fb.BlockBody.endBlockBody
import io.exflo.domain.fb.BlockBody.startBlockBody
import io.exflo.domain.fb.BlockHeader.addCoinbase
import io.exflo.domain.fb.BlockHeader.addDifficulty
import io.exflo.domain.fb.BlockHeader.addExtraData
import io.exflo.domain.fb.BlockHeader.addGasLimit
import io.exflo.domain.fb.BlockHeader.addGasUsed
import io.exflo.domain.fb.BlockHeader.addHash
import io.exflo.domain.fb.BlockHeader.addLogsBloom
import io.exflo.domain.fb.BlockHeader.addMixHash
import io.exflo.domain.fb.BlockHeader.addNonce
import io.exflo.domain.fb.BlockHeader.addNumber
import io.exflo.domain.fb.BlockHeader.addOmmersHash
import io.exflo.domain.fb.BlockHeader.addParentHash
import io.exflo.domain.fb.BlockHeader.addReceiptsRoot
import io.exflo.domain.fb.BlockHeader.addStateRoot
import io.exflo.domain.fb.BlockHeader.addTimestamp
import io.exflo.domain.fb.BlockHeader.addTotalDifficulty
import io.exflo.domain.fb.BlockHeader.addTransactionsRoot
import io.exflo.domain.fb.BlockHeader.endBlockHeader
import io.exflo.domain.fb.BlockHeader.startBlockHeader
import io.exflo.domain.fb.Bytes20
import io.exflo.domain.fb.Bytes256
import io.exflo.domain.fb.Bytes32
import io.exflo.domain.fb.Log
import io.exflo.domain.fb.LogTopic
import io.exflo.domain.fb.Signature.addR
import io.exflo.domain.fb.Signature.addRecId
import io.exflo.domain.fb.Signature.addS
import io.exflo.domain.fb.Signature.endSignature
import io.exflo.domain.fb.Signature.startSignature
import io.exflo.domain.fb.Transaction
import io.exflo.domain.fb.Transaction.addChainId
import io.exflo.domain.fb.Transaction.addContractAddress
import io.exflo.domain.fb.Transaction.addFee
import io.exflo.domain.fb.Transaction.addFrom
import io.exflo.domain.fb.Transaction.addGasPrice
import io.exflo.domain.fb.Transaction.addPayload
import io.exflo.domain.fb.Transaction.addReceipt
import io.exflo.domain.fb.Transaction.addSignature
import io.exflo.domain.fb.Transaction.addTo
import io.exflo.domain.fb.Transaction.addTrace
import io.exflo.domain.fb.Transaction.addValue
import io.exflo.domain.fb.Transaction.endTransaction
import io.exflo.domain.fb.Transaction.startTransaction
import io.exflo.domain.fb.TransactionReceipt
import io.exflo.domain.fb.TransactionReceipt.addBloomFilter
import io.exflo.domain.fb.TransactionReceipt.addCumulativeGasUsed
import io.exflo.domain.fb.TransactionReceipt.addEvents
import io.exflo.domain.fb.TransactionReceipt.addEventsType
import io.exflo.domain.fb.TransactionReceipt.addLogs
import io.exflo.domain.fb.TransactionReceipt.addRevertReason
import io.exflo.domain.fb.TransactionReceipt.addStatus
import io.exflo.domain.fb.TransactionReceipt.createEventsTypeVector
import io.exflo.domain.fb.TransactionReceipt.createEventsVector
import io.exflo.domain.fb.TransactionReceipt.createLogsVector
import io.exflo.domain.fb.TransactionReceipt.endTransactionReceipt
import io.exflo.domain.fb.TransactionReceipt.startTransactionReceipt
import io.exflo.domain.fb.UInt256
import org.hyperledger.besu.crypto.SECP256K1.Signature as BesuSignature
import org.hyperledger.besu.ethereum.core.Account as BesuAccount
import org.hyperledger.besu.ethereum.core.Address
import org.hyperledger.besu.ethereum.core.BlockBody as BesuBlockBody
import org.hyperledger.besu.ethereum.core.BlockHeader as BesuBlockHeader
import org.hyperledger.besu.ethereum.core.Hash
import org.hyperledger.besu.ethereum.core.Log as BesuLog
import org.hyperledger.besu.ethereum.core.LogTopic as BesuLogTopic
import org.hyperledger.besu.ethereum.core.LogsBloomFilter as BesuLogsBloomFilter
import org.hyperledger.besu.ethereum.core.Transaction as BesuTransaction
import org.hyperledger.besu.ethereum.core.TransactionReceipt as BesuTransactionReceipt
import org.hyperledger.besu.ethereum.core.Wei
import org.apache.tuweni.units.bigints.UInt256 as BesuUInt256

fun Wei.toFlatBuffer(bb: FlatBufferBuilder): Int {
    val offset = bb.createByteVector(toBytes().toArray())
    UInt256.startUInt256(bb)
    UInt256.addBytes(bb, offset)
    return UInt256.endUInt256(bb)
}

fun Address.toFlatBuffer(bb: FlatBufferBuilder): Int {
    val offset = bb.createByteVector(toArray())
    Bytes20.startBytes20(bb)
    Bytes20.addBytes(bb, offset)
    return Bytes20.endBytes20(bb)
}

fun BesuSignature.toFlatBuffer(bb: FlatBufferBuilder): Int {

    val rOffset = r.toBesuUInt256().toFlatBuffer(bb)
    val sOffset = s.toBesuUInt256().toFlatBuffer(bb)

    startSignature(bb)

    addR(bb, rOffset)
    addS(bb, sOffset)
    addRecId(bb, recId)

    return endSignature(bb)
}

fun BesuLogTopic.toFlatBuffer(bb: FlatBufferBuilder): Int {
    val offset = bb.createByteVector(toArray())
    LogTopic.startLogTopic(bb)
    LogTopic.addBytes(bb, offset)
    return LogTopic.endLogTopic(bb)
}

fun BesuLog.toFlatBuffer(bb: FlatBufferBuilder): Int {

    val loggerOffset = logger.toFlatBuffer(bb)
    val dataOffset = bb.createByteVector(data.toArray())

    val topicOffsets = topics.map { it.toFlatBuffer(bb) }
    val topicVectorOffset = Log.createTopicsVector(bb, topicOffsets.toIntArray())

    Log.startLog(bb)
    Log.addLogger(bb, loggerOffset)
    Log.addData(bb, dataOffset)
    Log.addTopics(bb, topicVectorOffset)

    return Log.endLog(bb)
}

fun Hash.toFlatBuffer(bb: FlatBufferBuilder): Int {
    val offset = bb.createByteVector(toArray())
    Bytes32.startBytes32(bb)
    Bytes32.addBytes(bb, offset)
    return Bytes32.endBytes32(bb)
}

fun BesuLogsBloomFilter.toFlatBuffer(bb: FlatBufferBuilder): Int {
    val offset = bb.createByteVector(toArray())
    Bytes256.startBytes256(bb)
    Bytes256.addBytes(bb, offset)
    return Bytes256.endBytes256(bb)
}

fun BesuTransactionReceipt.toFlatBuffer(bb: FlatBufferBuilder, logParser: (receipt: BesuLog) -> ContractEvent?): Int {

    val stateRootOffset = stateRoot?.toFlatBuffer(bb)
    val logsVectorOffset = createLogsVector(bb, logs.map { it.toFlatBuffer(bb) }.toIntArray())
    val bloomFilterOffset = bloomFilter.toFlatBuffer(bb)

    val eventsWithType = logs
        .mapNotNull { log -> logParser(log) }
        .map { it.toFlatBuffer(bb) }
    val eventsTypeVectorOffset = createEventsTypeVector(bb, eventsWithType.map { (type, _) -> type }.toByteArray())
    val eventsVectorOffset = createEventsVector(bb, eventsWithType.map { (_, eventOffset) -> eventOffset }.toIntArray())

    val revertReasonOffset: Int? = revertReason
        .map { bb.createByteVector(it.toArray()) }
        .orElse(null)

    startTransactionReceipt(bb)
    stateRootOffset?.run { TransactionReceipt.addStateRoot(bb, this) }
    addCumulativeGasUsed(bb, cumulativeGasUsed)
    addLogs(bb, logsVectorOffset)
    addEventsType(bb, eventsTypeVectorOffset)
    addEvents(bb, eventsVectorOffset)
    addBloomFilter(bb, bloomFilterOffset)
    addStatus(bb, status.toByte())
    revertReasonOffset?.run { addRevertReason(bb, this) }

    return endTransactionReceipt(bb)
}

fun BesuTransaction.toFlatBuffer(
    bb: FlatBufferBuilder,
    receipt: BesuTransactionReceipt,
    fee: Wei,
    trace: TransactionTrace?,
    logParser: (receipt: BesuLog) -> ContractEvent?
): Int {

    val hashOffset = hash.toFlatBuffer(bb)
    val fromOffset = sender.toFlatBuffer(bb)
    val gasPriceOffset = gasPrice.toFlatBuffer(bb)
    val valueOffset = value.toFlatBuffer(bb)
    val payloadOffset = bb.createByteVector(payload.toArray())
    val signatureOffset = signature.toFlatBuffer(bb)
    val receiptOffset = receipt.toFlatBuffer(bb, logParser)
    val feeOffset = fee.toFlatBuffer(bb)
    val traceOffset = trace?.toFlatBuffer(bb)
    val toOffset: Int? = to.map { it.toFlatBuffer(bb) }.orElse(null)
    val chainIdOffset: Int? = chainId.map { it.toBesuUInt256().toFlatBuffer(bb) }.orElse(null)
    val contractAddressOffset: Int? = contractAddress().map { it.toFlatBuffer(bb) }.orElse(null)

    startTransaction(bb)

    Transaction.addHash(bb, hashOffset)
    addFrom(bb, fromOffset)
    Transaction.addNonce(bb, nonce)
    addGasPrice(bb, gasPriceOffset)
    Transaction.addGasLimit(bb, gasLimit)
    addValue(bb, valueOffset)
    addPayload(bb, payloadOffset)
    addSignature(bb, signatureOffset)
    addReceipt(bb, receiptOffset)
    addFee(bb, feeOffset)
    traceOffset?.let { addTrace(bb, it) }
    toOffset?.run { addTo(bb, this) }
    chainIdOffset?.run { addChainId(bb, this) }
    contractAddressOffset?.run { addContractAddress(bb, this) }

    return endTransaction(bb)
}

fun BesuAccount.toFlatBuffer(bb: FlatBufferBuilder, contractsCreated: List<Address>): Int {

    val contractCreated = contractsCreated.contains(address)

    val addressOffset = address.toFlatBuffer(bb)
    val balanceOffset = balance.toFlatBuffer(bb)

    val codeOffset: Int? = if (contractCreated) bb.createByteVector(code.toArray()) else null
    val codeHashOffset: Int? = if (contractCreated) codeHash.toFlatBuffer(bb) else null

    startAccount(bb)

    addAddress(bb, addressOffset)
    Account.addNonce(bb, nonce)
    addBalance(bb, balanceOffset)

    codeOffset?.run { addCode(bb, this) }
    codeHashOffset?.run { addCodeHash(bb, this) }

    return endAccount(bb)
}

fun BesuUInt256.toFlatBuffer(bb: FlatBufferBuilder): Int {
    val offset = bb.createByteVector(toBytes().toArray())
    UInt256.startUInt256(bb)
    UInt256.addBytes(bb, offset)
    return UInt256.endUInt256(bb)
}

fun BesuBlockHeader.toFlatBuffer(bb: FlatBufferBuilder, totalDifficulty: BesuUInt256?): Int {

    val hashOffset = hash.toFlatBuffer(bb)
    val parentHashOffset = parentHash.toFlatBuffer(bb)
    val ommersHashOffset = ommersHash.toFlatBuffer(bb)
    val coinbaseOffset = coinbase.toFlatBuffer(bb)
    val stateRootOffset = stateRoot.toFlatBuffer(bb)
    val transactionsRootOffset = transactionsRoot.toFlatBuffer(bb)
    val receiptsRootOffset = receiptsRoot.toFlatBuffer(bb)
    val logsBloomOffset = logsBloom.toFlatBuffer(bb)
    val difficultyOffset = difficulty.toFlatBuffer(bb)
    val extraDataOffset = bb.createByteVector(extraData.toArray())
    val mixHashOffset = mixHash.toFlatBuffer(bb)
    // Total difficulty is not set on ommers
    val totalDifficultyOffset: Int? = totalDifficulty?.toFlatBuffer(bb)

    startBlockHeader(bb)

    addHash(bb, hashOffset)
    addParentHash(bb, parentHashOffset)
    addOmmersHash(bb, ommersHashOffset)
    addCoinbase(bb, coinbaseOffset)
    addStateRoot(bb, stateRootOffset)
    addTransactionsRoot(bb, transactionsRootOffset)
    addReceiptsRoot(bb, receiptsRootOffset)
    addLogsBloom(bb, logsBloomOffset)
    addDifficulty(bb, difficultyOffset)
    addNumber(bb, number)
    addGasLimit(bb, gasLimit)
    addGasUsed(bb, gasUsed)
    addTimestamp(bb, timestamp)
    addExtraData(bb, extraDataOffset)
    addMixHash(bb, mixHashOffset)
    addNonce(bb, nonce)
    totalDifficultyOffset?.run { addTotalDifficulty(bb, this) }

    return endBlockHeader(bb)
}

fun BesuBlockBody.toFlatBuffer(
    bb: FlatBufferBuilder,
    receipts: List<BesuTransactionReceipt>,
    trace: BlockTrace?,
    logParser: (receipt: BesuLog) -> ContractEvent?
): Int {

    val ommersVectorOffset = ommers
        .map { it.toFlatBuffer(bb, null) }
        .let { offsetArray -> createOmmersVector(bb, offsetArray.toIntArray()) }

    require(receipts.size == transactions.size) { "Receipts and transactions lists must be the same size" }

    trace?.apply { require(this.transactionTraces.size == transactions.size) { "Transaction traces and transactions lists must be the same size" } }

    var totalGasUsed = 0L

    val transactionsVectorOffset = transactions
        .zip(receipts, trace?.transactionTraces ?: receipts.map { null })
        .map { (tx, receipt, trace) ->
            val gasUsed = receipt.cumulativeGasUsed.minus(totalGasUsed)
            totalGasUsed += gasUsed
            val fee = tx.gasPrice.multiply(gasUsed)
            tx.toFlatBuffer(bb, receipt, fee, trace, logParser)
        }
        .let { offsetArray -> createTransactionsVector(bb, offsetArray.toIntArray()) }

    startBlockBody(bb)

    addOmmers(bb, ommersVectorOffset)
    addTransactions(bb, transactionsVectorOffset)

    return endBlockBody(bb)
}
