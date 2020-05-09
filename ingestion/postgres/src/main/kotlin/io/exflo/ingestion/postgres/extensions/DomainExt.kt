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

package io.exflo.ingestion.postgres.extensions

import com.fasterxml.jackson.databind.ObjectMapper
import io.exflo.domain.BalanceDelta
import io.exflo.domain.ContractCapability
import io.exflo.domain.ContractCreated
import io.exflo.domain.ContractDestroyed
import io.exflo.domain.ContractEvents
import io.exflo.domain.ContractType
import io.exflo.ingestion.extensions.toBigDecimal
import io.exflo.ingestion.extensions.contractEvents
import io.exflo.postgres.jooq.enums.ContractEventType
import io.exflo.postgres.jooq.tables.records.AccountRecord
import io.exflo.postgres.jooq.tables.records.BalanceDeltaRecord
import io.exflo.postgres.jooq.tables.records.BlockHeaderRecord
import io.exflo.postgres.jooq.tables.records.ContractCreatedRecord
import io.exflo.postgres.jooq.tables.records.ContractDestroyedRecord
import io.exflo.postgres.jooq.tables.records.ContractEventRecord
import io.exflo.postgres.jooq.tables.records.OmmerRecord
import io.exflo.postgres.jooq.tables.records.TransactionReceiptRecord
import io.exflo.postgres.jooq.tables.records.TransactionRecord
import org.apache.tuweni.units.bigints.UInt256
import org.hyperledger.besu.ethereum.core.Account
import org.hyperledger.besu.ethereum.core.BlockHeader
import org.hyperledger.besu.ethereum.core.Transaction
import org.hyperledger.besu.ethereum.core.TransactionReceipt
import org.jooq.TableRecord
import java.sql.Timestamp

fun BlockHeader.toBlockHeaderRecord(totalDifficulty: UInt256): BlockHeaderRecord {
    val header = this
    return BlockHeaderRecord()
        .apply {
            this.number = header.number
            this.hash = header.hash.toHexString()
            this.parentHash = header.parentHash.toHexString()
            this.nonce = header.nonce
            this.isCanonical = true
            this.stateRoot = header.stateRoot.toHexString()
            this.receiptsRoot = header.receiptsRoot.toHexString()
            this.transactionsRoot = header.transactionsRoot.toHexString()
            this.coinbase = header.coinbase.toHexString()
            this.difficulty = header.difficulty.toBigInteger().toBigDecimal()
            this.totalDifficulty = totalDifficulty.toBigDecimal()
            header.extraData?.let { this.setExtraData(*(it.toArray())) }
            this.gasLimit = header.gasLimit
            this.gasUsed = header.gasUsed
            this.timestamp = Timestamp(header.timestamp.times(1000))
            this.mixHash = header.mixHash.toHexString()
            this.ommersHash = header.ommersHash.toHexString()
            this.logsBloom = header.logsBloom.toHexString()
        }
}

fun BlockHeader.toOmmerRecord(nephew: BlockHeaderRecord, index: Int): OmmerRecord {
    val header = this
    return OmmerRecord()
        .apply {
            this.index = index
            this.number = header.number
            this.hash = header.hash.toHexString()
            this.parentHash = header.parentHash.toHexString()
            this.nephewHash = nephew.hash
            this.height = nephew.number
            this.nonce = header.nonce
            this.stateRoot = header.stateRoot.toHexString()
            this.receiptsRoot = header.receiptsRoot.toHexString()
            this.transactionsRoot = header.transactionsRoot.toHexString()
            this.coinbase = header.coinbase.toHexString()
            // TODO verify this difficulty to big decimal conversion and find a better way of doing it
            this.difficulty = header.difficulty.toBigInteger().toBigDecimal()
            header.extraData?.let { this.setExtraData(*(it.toArray())) }
            this.gasLimit = header.gasLimit
            this.gasUsed = header.gasUsed
            this.timestamp = Timestamp(header.timestamp.times(1000))
            this.mixHash = header.mixHash.toHexString()
            this.ommersHash = header.ommersHash.toHexString()
            this.logsBloom = header.logsBloom.toHexString()
        }
}

fun Transaction.toTransactionRecord(header: BlockHeaderRecord, index: Int): TransactionRecord {
    val tx = this
    return TransactionRecord()
        .apply {
            this.hash = tx.hash.toHexString()
            this.blockNumber = header.number
            this.blockHash = header.hash
            this.index = index
            this.nonce = tx.nonce
            this.from = tx.sender.toHexString()
            this.to = tx.to.orElse(null)?.toHexString()
            this.value = tx.value.toBigDecimal()
            this.gasPrice = tx.gasPrice.toBigDecimal()
            this.gasLimit = tx.gasLimit
            tx.payload?.let { this.setPayload(*(it.toArray())) }
            tx.chainId.orElse(null)?.let { setChainId(it.toBigDecimal()) }
            this.fee = tx.gasPrice.multiply(tx.gasLimit).toBigDecimal()
            this.recId = tx.signature.recId.toShort()
            this.r = tx.signature.r.toBigDecimal()
            this.s = tx.signature.s.toBigDecimal()
            tx.contractAddress().orElse(null)?.let { setContractAddress(it.toHexString()) }
            this.timestamp = header.timestamp
        }
}

fun TransactionReceipt.toTransactionReceiptRecord(
    objectMapper: ObjectMapper,
    blockHeader: BlockHeaderRecord,
    transaction: TransactionRecord,
    gasUsed: Long
): TransactionReceiptRecord {

    val receipt = this
    val logsAsJson = logs.map { objectMapper.writeValueAsString(it) }

    return TransactionReceiptRecord()
        .apply {
            this.transactionHash = transaction.hash
            this.transactionIndex = transaction.index
            this.blockHash = blockHeader.hash
            this.blockNumber = blockHeader.number
            this.from = transaction.from
            this.to = transaction.to
            this.contractAddress = transaction.contractAddress
            this.cumulativeGasUsed = receipt.cumulativeGasUsed
            this.gasUsed = gasUsed
            this.logs = "[" + logsAsJson.joinToString(",") + "]"
            receipt.stateRoot?.let { setStateRoot(it.toHexString()) }
            this.status = receipt.status.toShort()
            this.bloomFilter = receipt.bloomFilter.toHexString()
            this.timestamp = blockHeader.timestamp
            receipt.revertReason.orElse(null)?.let { setRevertReason(*(it.toArray())) }
        }
}

fun TransactionReceipt.toEventRecords(
    blockHeader: BlockHeaderRecord,
    transaction: TransactionRecord
): List<TableRecord<*>> {

    val blockNumber = blockHeader.number
    val blockHash = blockHeader.hash
    val transactionHash = transaction.hash

    return contractEvents()
        .map { event ->

            when (event) {

                is ContractEvents.FungibleApproval -> {
                    ContractEventRecord()
                        .apply {
                            this.blockNumber = blockNumber
                            this.blockHash = blockHash
                            this.transactionHash = transactionHash
                            this.contractAddress = event.contract.toHexString()
                            this.type = ContractEventType.fungible_approval
                            this.ownerAddress = event.owner.toHexString()
                            this.spenderAddress = event.spender.toHexString()
                            this.value = event.value.toBigDecimal()
                        }
                }

                is ContractEvents.FungibleTransfer -> {
                    ContractEventRecord()
                        .apply {
                            this.blockNumber = blockNumber
                            this.blockHash = blockHash
                            this.transactionHash = transactionHash
                            this.contractAddress = event.contract.toHexString()
                            this.type = ContractEventType.fungible_transfer
                            this.fromAddress = event.from.toHexString()
                            this.toAddress = event.to.toHexString()
                            this.value = event.value.toBigDecimal()
                        }
                }

                is ContractEvents.NonFungibleApproval -> {
                    ContractEventRecord()
                        .apply {
                            this.blockNumber = blockNumber
                            this.blockHash = blockHash
                            this.transactionHash = transactionHash
                            this.contractAddress = event.contract.toHexString()
                            this.type = ContractEventType.non_fungible_approval
                            this.ownerAddress = event.owner.toHexString()
                            this.approvedAddress = event.approved.toHexString()
                            this.tokenId = event.tokenId.toBigDecimal()
                        }
                }

                is ContractEvents.ApprovalForAll -> {
                    ContractEventRecord()
                        .apply {
                            this.blockNumber = blockNumber
                            this.blockHash = blockHash
                            this.transactionHash = transactionHash
                            this.contractAddress = event.contract.toHexString()
                            this.type = ContractEventType.approval_for_all
                            this.ownerAddress = event.owner.toHexString()
                            this.operatorAddress = event.operator.toHexString()
                            this.approved = approved
                        }
                }

                is ContractEvents.NonFungibleTransfer -> {
                    ContractEventRecord()
                        .apply {
                            this.blockNumber = blockNumber
                            this.blockHash = blockHash
                            this.transactionHash = transactionHash
                            this.contractAddress = event.contract.toHexString()
                            this.type = ContractEventType.non_fungible_transfer
                            this.fromAddress = event.from.toHexString()
                            this.toAddress = event.to.toHexString()
                            this.tokenId = event.tokenId.toBigDecimal()
                        }
                }

                is ContractEvents.Sent -> {
                    ContractEventRecord()
                        .apply {
                            this.blockNumber = blockNumber
                            this.blockHash = blockHash
                            this.transactionHash = transactionHash
                            this.contractAddress = event.contract.toHexString()
                            this.type = ContractEventType.sent
                            this.fromAddress = event.from.toHexString()
                            this.toAddress = event.to.toHexString()
                            this.amount = event.amount.toBigDecimal()
                            this.setData(*event.data.toArray())
                            this.setOperatorData(*event.operatorData.toArray())
                        }
                }

                is ContractEvents.Minted -> {
                    ContractEventRecord()
                        .apply {
                            this.blockNumber = blockNumber
                            this.blockHash = blockHash
                            this.transactionHash = transactionHash
                            this.contractAddress = event.contract.toHexString()
                            this.type = ContractEventType.minted
                            this.toAddress = event.to.toHexString()
                            this.amount = event.amount.toBigDecimal()
                            this.setData(*event.data.toArray())
                            this.setOperatorData(*event.operatorData.toArray())
                        }
                }

                is ContractEvents.Burned -> {
                    ContractEventRecord()
                        .apply {
                            this.blockNumber = blockNumber
                            this.blockHash = blockHash
                            this.transactionHash = transactionHash
                            this.contractAddress = event.contract.toHexString()
                            this.type = ContractEventType.burned
                            this.toAddress = event.to.toHexString()
                            this.amount = event.amount.toBigDecimal()
                            this.setData(*event.data.toArray())
                            this.setOperatorData(*event.operatorData.toArray())
                        }
                }

                is ContractEvents.AuthorizedOperator -> {
                    ContractEventRecord()
                        .apply {
                            this.blockNumber = blockNumber
                            this.blockHash = blockHash
                            this.transactionHash = transactionHash
                            this.contractAddress = event.contract.toHexString()
                            this.type = ContractEventType.authorized_operator
                            this.operatorAddress = event.operator.toHexString()
                            this.holderAddress = event.holder.toHexString()
                        }
                }

                is ContractEvents.RevokedOperator -> {
                    ContractEventRecord()
                        .apply {
                            this.blockNumber = blockNumber
                            this.blockHash = blockHash
                            this.transactionHash = transactionHash
                            this.contractAddress = event.contract.toHexString()
                            this.type = ContractEventType.revoked_operator
                            this.operatorAddress = event.operator.toHexString()
                            this.holderAddress = event.holder.toHexString()
                        }
                }

                is ContractEvents.TransferSingle -> {
                    ContractEventRecord()
                        .apply {
                            this.blockNumber = blockNumber
                            this.blockHash = blockHash
                            this.transactionHash = transactionHash
                            this.contractAddress = event.contract.toHexString()
                            this.type = ContractEventType.transfer_single
                            this.operatorAddress = event.operator.toHexString()
                            this.fromAddress = event.from.toHexString()
                            this.toAddress = event.to.toHexString()
                            this.id = event.id.toBigDecimal()
                            this.value = event.value.toBigDecimal()
                        }
                }

                is ContractEvents.TransferBatch -> {
                    ContractEventRecord()
                        .apply {
                            this.blockNumber = blockNumber
                            this.blockHash = blockHash
                            this.transactionHash = transactionHash
                            this.contractAddress = event.contract.toHexString()
                            this.type = ContractEventType.transfer_batch
                            this.operatorAddress = event.operator.toHexString()
                            this.fromAddress = event.from.toHexString()
                            this.toAddress = event.to.toHexString()
                            event.ids.forEach { id -> setIds(id.toBigInteger().toBigDecimal()) }
                            event.values.forEach { value -> setValues(value.toBigInteger().toBigDecimal()) }
                        }
                }

                is ContractEvents.URI -> {
                    ContractEventRecord()
                        .apply {
                            this.blockNumber = blockNumber
                            this.blockHash = blockHash
                            this.transactionHash = transactionHash
                            this.contractAddress = event.contract.toHexString()
                            this.type = ContractEventType.uri
                            this.valueStr = event.value
                            this.id = event.id.toBigDecimal()
                        }
                }

                else -> throw IllegalArgumentException("Unexpected event type: $event")
            }
        }
}

fun Account.toAccountRecord(header: BlockHeaderRecord): AccountRecord =
    AccountRecord()
        .apply {
            this.address = this@toAccountRecord.address.toHexString()
            this.blockNumber = header.number
            this.blockHash = header.hash
            this.nonce = this@toAccountRecord.nonce
            this.balance = this@toAccountRecord.balance.toBigDecimal()
        }

fun ContractType.toContractTypeRecord() = io.exflo.postgres.jooq.enums.ContractType.valueOf(name)

fun ContractCapability.toContractCapabilityRecord() = io.exflo.postgres.jooq.enums.ContractCapability.valueOf(name)

fun ContractCreated.toContractCreatedRecord(header: BlockHeaderRecord): ContractCreatedRecord =
    ContractCreatedRecord()
        .apply {
            this.address = contractAddress.toHexString()
            this.creator = originatorAddress.toHexString()
            this.code = this@toContractCreatedRecord.code.toHexString()
            this.type = this@toContractCreatedRecord.type?.toContractTypeRecord()
            this.setCapabilities(
                *this@toContractCreatedRecord.capabilities?.map { it.toContractCapabilityRecord() }!!.toTypedArray()
            )
            this.name = metadata?.name
            this.symbol = metadata?.symbol
            this.totalSupply = metadata?.totalSupply?.toBigDecimal()
            this.decimals = metadata?.decimals?.toShort()
            this.granularity = metadata?.granularity?.toBigDecimal()
            this.cap = metadata?.cap?.toBigDecimal()
            this.blockHash = header.hash
            this.blockNumber = header.number
            this.timestamp = header.timestamp
            this.transactionHash = this@toContractCreatedRecord.transactionHash?.toHexString()
        }

fun ContractDestroyed.toContractDestroyedRecord(header: BlockHeaderRecord): ContractDestroyedRecord =
    ContractDestroyedRecord()
        .apply {
            this.address = contractAddress.toHexString()
            this.refundAddress = this@toContractDestroyedRecord.refundAddress.toHexString()
            this.refundAmount = this@toContractDestroyedRecord.refundAmount.toBigDecimal()
            this.blockHash = header.hash
            this.blockNumber = header.number
            this.timestamp = header.timestamp
            this.transactionHash = this@toContractDestroyedRecord.transactionHash?.toHexString()
        }

fun BalanceDelta.toBalanceDeltaRecord(blockHeader: BlockHeaderRecord): BalanceDeltaRecord =
    BalanceDeltaRecord()
        .apply {
            this.deltaType = io.exflo.postgres.jooq.enums.DeltaType.valueOf(this@toBalanceDeltaRecord.deltaType.name)
            this.contractAddress = this@toBalanceDeltaRecord.contractAddress?.toHexString()
            this.from = this@toBalanceDeltaRecord.from?.toHexString()
            this.to = this@toBalanceDeltaRecord.to?.toHexString()
            this.amount = this@toBalanceDeltaRecord.amount?.toBigDecimal()
            this.tokenId = this@toBalanceDeltaRecord.tokenId?.toBigDecimal()
            this.blockNumber = blockHeader.number
            this.blockHash = blockHeader.hash
            this.blockTimestamp = blockHeader.timestamp
            this.transactionHash = this@toBalanceDeltaRecord.transactionHash?.toHexString()
            this.transactionIndex = this@toBalanceDeltaRecord.transactionIndex
        }
