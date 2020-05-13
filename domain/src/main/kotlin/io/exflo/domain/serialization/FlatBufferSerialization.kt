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

package io.exflo.domain.serialization

import com.google.flatbuffers.FlatBufferBuilder
import io.exflo.domain.extensions.zip
import io.exflo.domain.fb.Account
import io.exflo.domain.fb.Account.addAddress
import io.exflo.domain.fb.Account.addBalance
import io.exflo.domain.fb.Account.addCode
import io.exflo.domain.fb.Account.addCodeHash
import io.exflo.domain.fb.Account.endAccount
import io.exflo.domain.fb.Account.startAccount
import io.exflo.domain.fb.BalanceDelta
import io.exflo.domain.fb.Block
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
import io.exflo.domain.fb.ContractCreated
import io.exflo.domain.fb.ContractDestroyed
import io.exflo.domain.fb.ContractMetadata
import io.exflo.domain.fb.InternalTransaction
import io.exflo.domain.fb.Log
import io.exflo.domain.fb.LogTopic
import io.exflo.domain.fb.Reward
import io.exflo.domain.fb.Rewards
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
import io.exflo.domain.fb.TransactionTrace
import io.exflo.domain.fb.UInt256
import io.exflo.domain.fb.events.ApprovalForAll
import io.exflo.domain.fb.events.AuthorizedOperator
import io.exflo.domain.fb.events.Burned
import io.exflo.domain.fb.events.ContractEvent
import io.exflo.domain.fb.events.FungibleApproval
import io.exflo.domain.fb.events.FungibleTransfer
import io.exflo.domain.fb.events.Minted
import io.exflo.domain.fb.events.NonFungibleApproval
import io.exflo.domain.fb.events.NonFungibleTransfer
import io.exflo.domain.fb.events.RevokedOperator
import io.exflo.domain.fb.events.Sent
import io.exflo.domain.fb.events.TransferBatch
import io.exflo.domain.fb.events.TransferSingle
import io.exflo.domain.fb.events.URI
import io.exflo.domain.BalanceDelta as ExfloBalanceDelta
import io.exflo.domain.BlockTrace as ExfloBlockTrace
import io.exflo.domain.ContractCreated as ExfloContractCreated
import io.exflo.domain.ContractDestroyed as ExfloContractDestroyed
import io.exflo.domain.ContractEvent as ExfloContractEvent
import io.exflo.domain.ContractEvents as ExfloContractEvents
import io.exflo.domain.ContractMetadata as ExfloContractMetadata
import io.exflo.domain.FullBlock as ExfloFullBlock
import io.exflo.domain.InternalTransaction as ExfloInternalTransaction
import io.exflo.domain.TransactionTrace as ExfloTransactionTrace
import org.apache.tuweni.units.bigints.UInt256 as BesuUInt256
import org.hyperledger.besu.crypto.SECP256K1.Signature as BesuSignature
import org.hyperledger.besu.ethereum.core.Account as BesuAccount
import org.hyperledger.besu.ethereum.core.Address as BesuAddress
import org.hyperledger.besu.ethereum.core.BlockBody as BesuBlockBody
import org.hyperledger.besu.ethereum.core.BlockHeader as BesuBlockHeader
import org.hyperledger.besu.ethereum.core.Hash as BesuHash
import org.hyperledger.besu.ethereum.core.Log as BesuLog
import org.hyperledger.besu.ethereum.core.LogTopic as BesuLogTopic
import org.hyperledger.besu.ethereum.core.LogsBloomFilter as BesuLogsBloomFilter
import org.hyperledger.besu.ethereum.core.Transaction as BesuTransaction
import org.hyperledger.besu.ethereum.core.TransactionReceipt as BesuTransactionReceipt
import org.hyperledger.besu.ethereum.core.Wei as BesuWei

// Contains all entities from Besu and Exflo to be serialized to FlatBuffer
//
// NOTE. We are using import aliases in order to better clarify fromm where the entities are coming:
//      - Besu for entities related to Besu
//      - Exflo for entities related to Exflo
//      - And nothing for FlatBuffer converters

// --------------------------------------------------------------------------
// Besu Entities
// --------------------------------------------------------------------------

fun BesuWei.toFlatBuffer(bb: FlatBufferBuilder): Int {
  val offset = bb.createByteVector(toBytes().toArray())
  UInt256.startUInt256(bb)
  UInt256.addBytes(bb, offset)
  return UInt256.endUInt256(bb)
}

fun BesuAddress.toFlatBuffer(bb: FlatBufferBuilder): Int {
  val offset = bb.createByteVector(toArray())
  Bytes20.startBytes20(bb)
  Bytes20.addBytes(bb, offset)
  return Bytes20.endBytes20(bb)
}

fun BesuSignature.toFlatBuffer(bb: FlatBufferBuilder): Int {

  val rOffset = BesuUInt256.valueOf(r).toFlatBuffer(bb)
  val sOffset = BesuUInt256.valueOf(s).toFlatBuffer(bb)

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

fun BesuHash.toFlatBuffer(bb: FlatBufferBuilder): Int {
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

fun BesuTransactionReceipt.toFlatBuffer(
  bb: FlatBufferBuilder,
  logParser: (receipt: BesuLog) -> ExfloContractEvent?
): Int {

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
  fee: BesuWei,
  trace: ExfloTransactionTrace?,
  logParser: (receipt: BesuLog) -> ExfloContractEvent?
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
  val chainIdOffset: Int? = chainId.map { BesuUInt256.valueOf(it).toFlatBuffer(bb) }.orElse(null)
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

fun BesuAccount.toFlatBuffer(bb: FlatBufferBuilder, contractsCreated: List<BesuAddress>): Int {

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
  val difficultyOffset = (difficulty.toUInt256()).toFlatBuffer(bb)
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
  trace: ExfloBlockTrace?,
  logParser: (receipt: BesuLog) -> ExfloContractEvent?
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

  val jsonTraceOffset = trace?.let { bb.createString(it.jsonTrace) }

  startBlockBody(bb)

  addOmmers(bb, ommersVectorOffset)
  addTransactions(bb, transactionsVectorOffset)
  jsonTraceOffset?.apply { addTrace(bb, this) }

  return endBlockBody(bb)
}

// --------------------------------------------------------------------------
// Exflo Entities
// --------------------------------------------------------------------------

fun ExfloBlockTrace.toRewardsFlatBuffer(bb: FlatBufferBuilder): Int {

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

fun ExfloTransactionTrace.toFlatBuffer(bb: FlatBufferBuilder): Int {

  val contractsCreatedVectorOffset: Int = contractsCreated
    .map { contractCreated -> contractCreated.toFlatBuffer(bb) }
    .let { offsetArray ->
      TransactionTrace.createContractsCreatedVector(
        bb,
        offsetArray.toIntArray()
      )
    }

  val contractsDestroyedVectorOffset: Int = contractsDestroyed
    .map { contractDestroyed -> contractDestroyed.toFlatBuffer(bb) }
    .let { offsetArray ->
      TransactionTrace.createContractsDestroyedVector(
        bb,
        offsetArray.toIntArray()
      )
    }

  val internalTransactionsVectorOffset: Int = internalTransactions
    .map { internalTransaction -> internalTransaction.toFlatBuffer(bb) }
    .let { offsetArray ->
      TransactionTrace.createInternalTransactionsVector(
        bb,
        offsetArray.toIntArray()
      )
    }

  val revertReasonOffset: Int? =
    result
      .revertReason
      .map { bb.createByteVector(it.toArray()) }.orElse(null)

  TransactionTrace.startTransactionTrace(bb)

  TransactionTrace.addContractsCreated(bb, contractsCreatedVectorOffset)
  TransactionTrace.addContractsDestroyed(bb, contractsDestroyedVectorOffset)
  TransactionTrace.addInternalTransactions(bb, internalTransactionsVectorOffset)
  TransactionTrace.addStatus(bb, result.status.ordinal.toByte())

  revertReasonOffset?.run { TransactionTrace.addRevertReason(bb, this) }

  return TransactionTrace.endTransactionTrace(bb)
}

fun ExfloInternalTransaction.toFlatBuffer(bb: FlatBufferBuilder): Int {
  val fromAddressOffset = fromAddress.toFlatBuffer(bb)
  val toAddressOffset = toAddress.toFlatBuffer(bb)
  val amountOffset = amount.toFlatBuffer(bb)

  val transactionHashOffset: Int? = transactionHash?.toFlatBuffer(bb)

  InternalTransaction.startInternalTransaction(bb)

  InternalTransaction.addFrom(bb, fromAddressOffset)
  InternalTransaction.addTo(bb, toAddressOffset)
  ContractCreated.addAmount(bb, amountOffset)
  InternalTransaction.addPc(bb, pc)
  transactionHashOffset?.run { ContractCreated.addTransactionHash(bb, this) }

  return InternalTransaction.endInternalTransaction(bb)
}

fun ExfloContractCreated.toFlatBuffer(bb: FlatBufferBuilder): Int {
  val addressOffset = contractAddress.toFlatBuffer(bb)
  val creatorOffset = originatorAddress.toFlatBuffer(bb)
  val codeOffset = bb.createByteVector(code.toArray())
  val amountOffset = amount.toFlatBuffer(bb)
  val transactionHashOffset = transactionHash?.toFlatBuffer(bb)
  val capabilitiesOffset = capabilities
    ?.map { it.ordinal.toByte() }
    ?.toByteArray()
    ?.let { elems -> bb.createByteVector(elems) }
  val metadataOffset = metadata?.toFlatBuffer(bb)

  ContractCreated.startContractCreated(bb)

  ContractCreated.addAddress(bb, addressOffset)
  ContractCreated.addCreator(bb, creatorOffset)
  ContractCreated.addCode(bb, codeOffset)
  ContractCreated.addAmount(bb, amountOffset)
  ContractDestroyed.addPc(bb, pc)
  transactionHashOffset?.run { ContractCreated.addTransactionHash(bb, this) }
  type?.run { ContractCreated.addType(bb, ordinal.toByte()) }
  capabilitiesOffset?.run { ContractCreated.addCapabilities(bb, this) }
  metadataOffset?.run { ContractCreated.addMetadata(bb, this) }

  return ContractCreated.endContractCreated(bb)
}

fun ExfloContractDestroyed.toFlatBuffer(bb: FlatBufferBuilder): Int {

  val addressOffset = contractAddress.toFlatBuffer(bb)
  val refundAddressOffset = refundAddress.toFlatBuffer(bb)
  val refundAmountOffset = refundAmount.toFlatBuffer(bb)

  val transactionHashOffset: Int? = transactionHash?.toFlatBuffer(bb)

  ContractDestroyed.startContractDestroyed(bb)

  ContractDestroyed.addAddress(bb, addressOffset)
  ContractDestroyed.addRefundAddress(bb, refundAddressOffset)
  ContractDestroyed.addRefundAmount(bb, refundAmountOffset)
  ContractDestroyed.addPc(bb, pc)

  transactionHashOffset?.run { ContractDestroyed.addTransactionHash(bb, this) }

  return ContractDestroyed.endContractDestroyed(bb)
}

fun ExfloContractMetadata.toFlatBuffer(bb: FlatBufferBuilder): Int {
  val nameOffset = name?.let { bb.createString(it) }
  val symbolOffset = symbol?.let { bb.createString(it) }
  val totalSupplyOffset = totalSupply?.toFlatBuffer(bb)
  val granularityOffset = granularity?.toFlatBuffer(bb)
  val capOffset = cap?.toFlatBuffer(bb)

  ContractMetadata.startContractMetadata(bb)

  nameOffset?.let { ContractMetadata.addName(bb, nameOffset) }
  symbolOffset?.let { ContractMetadata.addSymbol(bb, symbolOffset) }
  decimals?.let { ContractMetadata.addDecimals(bb, it) }
  totalSupplyOffset?.let { ContractMetadata.addTotalSupply(bb, it) }
  granularityOffset?.let { ContractMetadata.addGranularity(bb, it) }
  capOffset?.let { ContractMetadata.addCap(bb, it) }

  return ContractMetadata.endContractMetadata(bb)
}

fun ExfloBalanceDelta.toFlatBuffer(bb: FlatBufferBuilder): Int {
  val transactionHashOffset = transactionHash?.toFlatBuffer(bb)
  val contractAddressOffset = contractAddress?.toFlatBuffer(bb)
  val fromOffset = from?.toFlatBuffer(bb)
  val toOffset = to?.toFlatBuffer(bb)
  val amountOffset = amount?.toFlatBuffer(bb)
  val tokenIdOffset = tokenId?.toFlatBuffer(bb)

  BalanceDelta.startBalanceDelta(bb)

  BalanceDelta.addDeltaType(bb, deltaType.ordinal.toByte())
  BalanceDelta.addPc(bb, pc)
  transactionIndex?.let { BalanceDelta.addTransactionIndex(bb, it) }
  transactionHashOffset?.let { BalanceDelta.addTransactionHash(bb, it) }
  contractAddressOffset?.let { BalanceDelta.addContractAddress(bb, it) }
  fromOffset?.let { BalanceDelta.addFrom(bb, it) }
  toOffset?.let { BalanceDelta.addTo(bb, it) }
  amountOffset?.let { BalanceDelta.addAmount(bb, it) }
  tokenIdOffset?.let { BalanceDelta.addTokenId(bb, it) }

  return BalanceDelta.endBalanceDelta(bb)
}

inline fun <reified T : ExfloContractEvent> T.toFlatBuffer(bb: FlatBufferBuilder): Pair<Byte, Int> {
  when (this) {
    is ExfloContractEvents.FungibleApproval -> {
      val contractOffset = contract.toFlatBuffer(bb)
      val ownerOffset = owner.toFlatBuffer(bb)
      val spenderOffset = spender.toFlatBuffer(bb)
      val valueOffset = value.toFlatBuffer(bb)
      return Pair(
        ContractEvent.FungibleApproval,
        FungibleApproval.createFungibleApproval(bb, contractOffset, ownerOffset, spenderOffset, valueOffset)
      )
    }

    is ExfloContractEvents.FungibleTransfer -> {
      val contractOffset = contract.toFlatBuffer(bb)
      val fromOffset = from.toFlatBuffer(bb)
      val toOffset = to.toFlatBuffer(bb)
      val valueOffset = value.toFlatBuffer(bb)
      return Pair(
        ContractEvent.FungibleTransfer,
        FungibleTransfer.createFungibleTransfer(bb, contractOffset, fromOffset, toOffset, valueOffset)
      )
    }

    is ExfloContractEvents.NonFungibleApproval -> {
      val contractOffset = contract.toFlatBuffer(bb)
      val ownerOffset = owner.toFlatBuffer(bb)
      val approvedOffset = approved.toFlatBuffer(bb)
      val tokenIdOffset = tokenId.toFlatBuffer(bb)
      return Pair(
        ContractEvent.NonFungibleApproval,
        NonFungibleApproval.createNonFungibleApproval(
          bb,
          contractOffset,
          ownerOffset,
          approvedOffset,
          tokenIdOffset
        )
      )
    }

    is ExfloContractEvents.ApprovalForAll -> {
      val contractOffset = contract.toFlatBuffer(bb)
      val ownerOffset = owner.toFlatBuffer(bb)
      val operatorOffset = operator.toFlatBuffer(bb)
      return Pair(
        ContractEvent.ApprovalForAll,
        ApprovalForAll.createApprovalForAll(bb, contractOffset, ownerOffset, operatorOffset, approved)
      )
    }

    is ExfloContractEvents.NonFungibleTransfer -> {
      val contractOffset = contract.toFlatBuffer(bb)
      val fromOffset = from.toFlatBuffer(bb)
      val toOffset = to.toFlatBuffer(bb)
      val tokenIdOffset = tokenId.toFlatBuffer(bb)
      return Pair(
        ContractEvent.NonFungibleTransfer,
        NonFungibleTransfer.createNonFungibleTransfer(bb, contractOffset, fromOffset, toOffset, tokenIdOffset)
      )
    }

    is ExfloContractEvents.Sent -> {
      val contractOffset = contract.toFlatBuffer(bb)
      val operatorOffset = operator.toFlatBuffer(bb)
      val fromOffset = from.toFlatBuffer(bb)
      val toOffset = to.toFlatBuffer(bb)
      val amountOffset = amount.toFlatBuffer(bb)
      val dataOffset = bb.createByteVector(data.toArray())
      val operatorDataOffset = bb.createByteVector(operatorData.toArray())
      return Pair(
        ContractEvent.Sent,
        Sent.createSent(
          bb,
          contractOffset,
          operatorOffset,
          fromOffset,
          toOffset,
          amountOffset,
          dataOffset,
          operatorDataOffset
        )
      )
    }

    is ExfloContractEvents.Minted -> {
      val contractOffset = contract.toFlatBuffer(bb)
      val operatorOffset = operator.toFlatBuffer(bb)
      val toOffset = to.toFlatBuffer(bb)
      val amountOffset = amount.toFlatBuffer(bb)
      val dataOffset = bb.createByteVector(data.toArray())
      val operatorDataOffset = bb.createByteVector(operatorData.toArray())
      return Pair(
        ContractEvent.Minted,
        Minted.createMinted(
          bb,
          contractOffset,
          operatorOffset,
          toOffset,
          amountOffset,
          dataOffset,
          operatorDataOffset
        )
      )
    }

    is ExfloContractEvents.Burned -> {
      val contractOffset = contract.toFlatBuffer(bb)
      val operatorOffset = operator.toFlatBuffer(bb)
      val toOffset = to.toFlatBuffer(bb)
      val amountOffset = amount.toFlatBuffer(bb)
      val dataOffset = bb.createByteVector(data.toArray())
      val operatorDataOffset = bb.createByteVector(operatorData.toArray())
      return Pair(
        ContractEvent.Burned,
        Burned.createBurned(
          bb,
          contractOffset,
          operatorOffset,
          toOffset,
          amountOffset,
          dataOffset,
          operatorDataOffset
        )
      )
    }

    is ExfloContractEvents.AuthorizedOperator -> {
      val contractOffset = contract.toFlatBuffer(bb)
      val operatorOffset = operator.toFlatBuffer(bb)
      val holderOffset = holder.toFlatBuffer(bb)
      return Pair(
        ContractEvent.AuthorizedOperator,
        AuthorizedOperator.createAuthorizedOperator(bb, contractOffset, operatorOffset, holderOffset)
      )
    }

    is ExfloContractEvents.RevokedOperator -> {
      val contractOffset = contract.toFlatBuffer(bb)
      val operatorOffset = operator.toFlatBuffer(bb)
      val holderOffset = holder.toFlatBuffer(bb)
      return Pair(
        ContractEvent.RevokedOperator,
        RevokedOperator.createRevokedOperator(bb, contractOffset, operatorOffset, holderOffset)
      )
    }

    is ExfloContractEvents.TransferSingle -> {
      val contractOffset = contract.toFlatBuffer(bb)
      val operatorOffset = operator.toFlatBuffer(bb)
      val fromOffset = from.toFlatBuffer(bb)
      val toOffset = to.toFlatBuffer(bb)
      val idOffset = id.toFlatBuffer(bb)
      val valueOffset = value.toFlatBuffer(bb)
      return Pair(
        ContractEvent.TransferSingle,
        TransferSingle.createTransferSingle(
          bb,
          contractOffset,
          operatorOffset,
          fromOffset,
          toOffset,
          idOffset,
          valueOffset
        )
      )
    }

    is ExfloContractEvents.TransferBatch -> {
      val contractOffset = contract.toFlatBuffer(bb)
      val operatorOffset = operator.toFlatBuffer(bb)
      val fromOffset = from.toFlatBuffer(bb)
      val toOffset = to.toFlatBuffer(bb)

      val idsOffset = ids
        .map { it.toFlatBuffer(bb) }
        .let { bb.createVectorOfTables(it.toIntArray()) }

      val valuesOffset = values
        .map { it.toFlatBuffer(bb) }
        .let { bb.createVectorOfTables(it.toIntArray()) }

      return Pair(
        ContractEvent.TransferBatch,
        TransferBatch.createTransferBatch(
          bb,
          contractOffset,
          operatorOffset,
          fromOffset,
          toOffset,
          idsOffset,
          valuesOffset
        )
      )
    }

    is ExfloContractEvents.URI -> {
      val contractOffset = contract.toFlatBuffer(bb)
      val idOffset = id.toFlatBuffer(bb)
      val valueOffset = bb.createString(value)
      return Pair(
        ContractEvent.URI,
        URI.createURI(bb, contractOffset, valueOffset, idOffset)
      )
    }

    else -> throw IllegalArgumentException("Unknown entity: $this")
  }
}

fun ExfloFullBlock.toFlatBuffer(logParser: (receipt: BesuLog) -> ExfloContractEvent?, bb: FlatBufferBuilder): Int {
  val headerOffset = header?.toFlatBuffer(bb, totalDifficulty)
  val bodyOffset = body?.toFlatBuffer(bb, receipts, trace, logParser)

  val contractsCreatedAddresses = trace?.transactionTraces
    ?.map { it.contractsCreated }
    ?.flatten()
    ?.map { it.contractAddress }

  val touchedAccountsVectorOffset = touchedAccounts
    ?.map { account -> account.toFlatBuffer(bb, contractsCreatedAddresses!!) }
    ?.let { offsetArray -> Block.createTouchedAccountsVector(bb, offsetArray.toIntArray()) }

  // Only create rewards offset where rewards exist for this block
  val rewardsOffset = if (trace?.rewards?.isNotEmpty() == true) trace.toRewardsFlatBuffer(bb) else null

  val balanceDeltasVectorOffset = balanceDeltas
    ?.map { delta -> delta.toFlatBuffer(bb) }
    ?.let { offsetArray -> Block.createBalanceDeltasVector(bb, offsetArray.toIntArray()) }

  Block.startBlock(bb)

  headerOffset?.let { Block.addHeader(bb, headerOffset) }
  bodyOffset?.let { Block.addBody(bb, bodyOffset) }
  rewardsOffset?.let { Block.addRewards(bb, it) }
  touchedAccountsVectorOffset?.let { Block.addTouchedAccounts(bb, it) }
  balanceDeltasVectorOffset?.let { Block.addBalanceDeltas(bb, it) }

  return Block.endBlock(bb)
}
