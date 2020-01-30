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

import io.exflo.domain.BalanceDelta
import io.exflo.domain.ContractCapability
import io.exflo.domain.ContractCreated
import io.exflo.domain.ContractDestroyed
import io.exflo.domain.ContractEvents
import io.exflo.domain.ContractType
import io.exflo.ingestion.extensions.bigDecimal
import io.exflo.ingestion.extensions.contractEvents
import io.exflo.ingestion.postgres.json.Klaxon
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
import java.sql.Timestamp
import org.hyperledger.besu.ethereum.core.Account
import org.hyperledger.besu.ethereum.core.BlockHeader
import org.hyperledger.besu.ethereum.core.Transaction
import org.hyperledger.besu.ethereum.core.TransactionReceipt
import org.hyperledger.besu.util.uint.UInt256
import org.jooq.TableRecord

fun BlockHeader.toBlockHeaderRecord(totalDifficulty: UInt256): BlockHeaderRecord {
    val header = this
    return BlockHeaderRecord()
        .apply {
            this.number = header.number
            this.hash = header.hash.hexString
            this.parentHash = header.parentHash.hexString
            this.nonce = header.nonce
            this.isCanonical = true
            this.stateRoot = header.stateRoot.hexString
            this.receiptsRoot = header.receiptsRoot.hexString
            this.transactionsRoot = header.transactionsRoot.hexString
            this.coinbase = header.coinbase.hexString
            this.difficulty = header.difficulty.bigDecimal()
            this.totalDifficulty = totalDifficulty.bigDecimal()
            header.extraData?.let { this.setExtraData(*(it.byteArray)) }
            this.gasLimit = header.gasLimit
            this.gasUsed = header.gasUsed
            this.timestamp = Timestamp(header.timestamp.times(1000))
            this.mixHash = header.mixHash.hexString
            this.ommersHash = header.ommersHash.hexString
            this.logsBloom = header.logsBloom.hexString
        }
}

fun BlockHeader.toOmmerRecord(nephew: BlockHeaderRecord, index: Int): OmmerRecord {
    val header = this
    return OmmerRecord()
        .apply {
            this.index = index
            this.number = header.number
            this.hash = header.hash.hexString
            this.parentHash = header.parentHash.hexString
            this.nephewHash = nephew.hash
            this.height = nephew.number
            this.nonce = header.nonce
            this.stateRoot = header.stateRoot.hexString
            this.receiptsRoot = header.receiptsRoot.hexString
            this.transactionsRoot = header.transactionsRoot.hexString
            this.coinbase = header.coinbase.hexString
            // TODO verify this difficulty to big decimal conversion and find a better way of doing it
            this.difficulty = header.difficulty.bigDecimal()
            header.extraData?.let { this.setExtraData(*(it.byteArray)) }
            this.gasLimit = header.gasLimit
            this.gasUsed = header.gasUsed
            this.timestamp = Timestamp(header.timestamp.times(1000))
            this.mixHash = header.mixHash.hexString
            this.ommersHash = header.ommersHash.hexString
            this.logsBloom = header.logsBloom.hexString
        }
}

fun Transaction.toTransactionRecord(header: BlockHeaderRecord, index: Int): TransactionRecord {
    val tx = this
    return TransactionRecord()
        .apply {
            this.hash = tx.hash.hexString
            this.blockNumber = header.number
            this.blockHash = header.hash
            this.index = index
            this.nonce = tx.nonce
            this.from = tx.sender.hexString
            this.to = tx.to.orElse(null)?.hexString
            this.value = tx.value.bigDecimal()
            this.gasPrice = tx.gasPrice.bigDecimal()
            this.gasLimit = tx.gasLimit
            tx.payload?.let { this.setPayload(*(it.byteArray)) }
            tx.chainId.orElse(null)?.let { setChainId(it.toBigDecimal()) }
            this.fee = tx.gasPrice.times(tx.gasLimit).bigDecimal()
            this.recId = tx.signature.recId.toShort()
            this.r = tx.signature.r.toBigDecimal()
            this.s = tx.signature.s.toBigDecimal()
            tx.contractAddress().orElse(null)?.let { setContractAddress(it.hexString) }
            this.timestamp = header.timestamp
        }
}

fun TransactionReceipt.toTransactionReceiptRecord(blockHeader: BlockHeaderRecord, transaction: TransactionRecord, gasUsed: Long): TransactionReceiptRecord {

    val receipt = this
    val logsAsJson = logs.map { Klaxon().toJsonString(it) }

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
            receipt.stateRoot?.let { setStateRoot(it.hexString) }
            this.status = receipt.status.toShort()
            this.bloomFilter = receipt.bloomFilter.hexString
            this.timestamp = blockHeader.timestamp
            receipt.revertReason.orElse(null)?.let { setRevertReason(*(it.byteArray)) }
        }
}

fun TransactionReceipt.toEventRecords(blockHeader: BlockHeaderRecord, transaction: TransactionRecord): List<TableRecord<*>> {

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
                            this.contractAddress = event.contract.hexString
                            this.type = ContractEventType.fungible_approval
                            this.ownerAddress = event.owner.hexString
                            this.spenderAddress = event.spender.hexString
                            this.value = event.value.bigDecimal()
                        }
                }

                is ContractEvents.FungibleTransfer -> {
                    ContractEventRecord()
                        .apply {
                            this.blockNumber = blockNumber
                            this.blockHash = blockHash
                            this.transactionHash = transactionHash
                            this.contractAddress = event.contract.hexString
                            this.type = ContractEventType.fungible_transfer
                            this.fromAddress = event.from.hexString
                            this.toAddress = event.to.hexString
                            this.value = event.value.bigDecimal()
                        }
                }

                is ContractEvents.NonFungibleApproval -> {
                    ContractEventRecord()
                        .apply {
                            this.blockNumber = blockNumber
                            this.blockHash = blockHash
                            this.transactionHash = transactionHash
                            this.contractAddress = event.contract.hexString
                            this.type = ContractEventType.non_fungible_approval
                            this.ownerAddress = event.owner.hexString
                            this.approvedAddress = event.approved.hexString
                            this.tokenId = event.tokenId.bigDecimal()
                        }
                }

                is ContractEvents.ApprovalForAll -> {
                    ContractEventRecord()
                        .apply {
                            this.blockNumber = blockNumber
                            this.blockHash = blockHash
                            this.transactionHash = transactionHash
                            this.contractAddress = event.contract.hexString
                            this.type = ContractEventType.approval_for_all
                            this.ownerAddress = event.owner.hexString
                            this.operatorAddress = event.operator.hexString
                            this.approved = approved
                        }
                }

                is ContractEvents.NonFungibleTransfer -> {
                    ContractEventRecord()
                        .apply {
                            this.blockNumber = blockNumber
                            this.blockHash = blockHash
                            this.transactionHash = transactionHash
                            this.contractAddress = event.contract.hexString
                            this.type = ContractEventType.non_fungible_transfer
                            this.fromAddress = event.from.hexString
                            this.toAddress = event.to.hexString
                            this.tokenId = event.tokenId.bigDecimal()
                        }
                }

                is ContractEvents.Sent -> {
                    ContractEventRecord()
                        .apply {
                            this.blockNumber = blockNumber
                            this.blockHash = blockHash
                            this.transactionHash = transactionHash
                            this.contractAddress = event.contract.hexString
                            this.type = ContractEventType.sent
                            this.fromAddress = event.from.hexString
                            this.toAddress = event.to.hexString
                            this.amount = event.amount.bigDecimal()
                            this.setData(*event.data.extractArray())
                            this.setOperatorData(*event.operatorData.extractArray())
                        }
                }

                is ContractEvents.Minted -> {
                    ContractEventRecord()
                        .apply {
                            this.blockNumber = blockNumber
                            this.blockHash = blockHash
                            this.transactionHash = transactionHash
                            this.contractAddress = event.contract.hexString
                            this.type = ContractEventType.minted
                            this.toAddress = event.to.hexString
                            this.amount = event.amount.bigDecimal()
                            this.setData(*event.data.extractArray())
                            this.setOperatorData(*event.operatorData.extractArray())
                        }
                }

                is ContractEvents.Burned -> {
                    ContractEventRecord()
                        .apply {
                            this.blockNumber = blockNumber
                            this.blockHash = blockHash
                            this.transactionHash = transactionHash
                            this.contractAddress = event.contract.hexString
                            this.type = ContractEventType.burned
                            this.toAddress = event.to.hexString
                            this.amount = event.amount.bigDecimal()
                            this.setData(*event.data.extractArray())
                            this.setOperatorData(*event.operatorData.extractArray())
                        }
                }

                is ContractEvents.AuthorizedOperator -> {
                    ContractEventRecord()
                        .apply {
                            this.blockNumber = blockNumber
                            this.blockHash = blockHash
                            this.transactionHash = transactionHash
                            this.contractAddress = event.contract.hexString
                            this.type = ContractEventType.authorized_operator
                            this.operatorAddress = event.operator.hexString
                            this.holderAddress = event.holder.hexString
                        }
                }

                is ContractEvents.RevokedOperator -> {
                    ContractEventRecord()
                        .apply {
                            this.blockNumber = blockNumber
                            this.blockHash = blockHash
                            this.transactionHash = transactionHash
                            this.contractAddress = event.contract.hexString
                            this.type = ContractEventType.revoked_operator
                            this.operatorAddress = event.operator.hexString
                            this.holderAddress = event.holder.hexString
                        }
                }

                is ContractEvents.TransferSingle -> {
                    ContractEventRecord()
                        .apply {
                            this.blockNumber = blockNumber
                            this.blockHash = blockHash
                            this.transactionHash = transactionHash
                            this.contractAddress = event.contract.hexString
                            this.type = ContractEventType.transfer_single
                            this.operatorAddress = event.operator.hexString
                            this.fromAddress = event.from.hexString
                            this.toAddress = event.to.hexString
                            this.id = event.id.bigDecimal()
                            this.value = event.value.bigDecimal()
                        }
                }

                is ContractEvents.TransferBatch -> {
                    ContractEventRecord()
                        .apply {
                            this.blockNumber = blockNumber
                            this.blockHash = blockHash
                            this.transactionHash = transactionHash
                            this.contractAddress = event.contract.hexString
                            this.type = ContractEventType.transfer_batch
                            this.operatorAddress = event.operator.hexString
                            this.fromAddress = event.from.hexString
                            this.toAddress = event.to.hexString
                            event.ids.forEach { id -> setIds(id.bigDecimal) }
                            event.values.forEach { value -> setValues(value.bigDecimal) }
                        }
                }

                is ContractEvents.URI -> {
                    ContractEventRecord()
                        .apply {
                            this.blockNumber = blockNumber
                            this.blockHash = blockHash
                            this.transactionHash = transactionHash
                            this.contractAddress = event.contract.hexString
                            this.type = ContractEventType.uri
                            this.valueStr = event.value
                            this.id = event.id.bigDecimal()
                        }
                }

                else -> throw IllegalArgumentException("Unexpected event type: $event")
            }
        }
}

fun Account.toAccountRecord(header: BlockHeaderRecord): AccountRecord =
    AccountRecord()
        .apply {
            this.address = this@toAccountRecord.address.hexString
            this.blockNumber = header.number
            this.blockHash = header.hash
            this.nonce = this@toAccountRecord.nonce
            this.balance = this@toAccountRecord.balance.bigDecimal()
        }

fun ContractType.toContractTypeRecord() = io.exflo.postgres.jooq.enums.ContractType.valueOf(name)

fun ContractCapability.toContractCapabilityRecord() = io.exflo.postgres.jooq.enums.ContractCapability.valueOf(name)

fun ContractCreated.toContractCreatedRecord(header: BlockHeaderRecord): ContractCreatedRecord =
    ContractCreatedRecord()
        .apply {
            this.address = contractAddress.hexString
            this.creator = originatorAddress.hexString
            this.code = this@toContractCreatedRecord.code.hexString
            this.type = this@toContractCreatedRecord.type?.toContractTypeRecord()
            this.setCapabilities(*this@toContractCreatedRecord.capabilities?.map { it.toContractCapabilityRecord() }!!.toTypedArray())
            this.name = metadata?.name
            this.symbol = metadata?.symbol
            this.totalSupply = metadata?.totalSupply?.bigDecimal()
            this.decimals = metadata?.decimals?.toShort()
            this.granularity = metadata?.granularity?.bigDecimal()
            this.cap = metadata?.cap?.bigDecimal()
            this.blockHash = header.hash
            this.blockNumber = header.number
            this.timestamp = header.timestamp
            this.transactionHash = this@toContractCreatedRecord.transactionHash?.hexString
        }

fun ContractDestroyed.toContractDestroyedRecord(header: BlockHeaderRecord): ContractDestroyedRecord =
    ContractDestroyedRecord()
        .apply {
            this.address = contractAddress.hexString
            this.refundAddress = this@toContractDestroyedRecord.refundAddress.hexString
            this.refundAmount = this@toContractDestroyedRecord.refundAmount.bigDecimal()
            this.blockHash = header.hash
            this.blockNumber = header.number
            this.timestamp = header.timestamp
            this.transactionHash = this@toContractDestroyedRecord.transactionHash?.hexString
        }

fun BalanceDelta.toBalanceDeltaRecord(blockHeader: BlockHeaderRecord): BalanceDeltaRecord =
    BalanceDeltaRecord()
        .apply {
            this.deltaType = io.exflo.postgres.jooq.enums.DeltaType.valueOf(this@toBalanceDeltaRecord.deltaType.name)
            this.contractAddress = this@toBalanceDeltaRecord.contractAddress?.hexString
            this.from = this@toBalanceDeltaRecord.from?.hexString
            this.to = this@toBalanceDeltaRecord.to?.hexString
            this.amount = this@toBalanceDeltaRecord.amount?.bigDecimal()
            this.tokenId = this@toBalanceDeltaRecord.tokenId?.bigDecimal()
            this.blockNumber = blockHeader.number
            this.blockHash = blockHeader.hash
            this.blockTimestamp = blockHeader.timestamp
            this.transactionHash = this@toBalanceDeltaRecord.transactionHash?.hexString
            this.transactionIndex = this@toBalanceDeltaRecord.transactionIndex
        }
