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

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.units.bigints.UInt256
import org.hyperledger.besu.ethereum.core.Address
import org.hyperledger.besu.ethereum.core.Block
import org.hyperledger.besu.ethereum.core.Hash
import org.hyperledger.besu.ethereum.core.Transaction
import org.hyperledger.besu.ethereum.core.Wei
import org.hyperledger.besu.ethereum.mainnet.TransactionProcessor

data class BlockTrace(
  val block: Block,
  val rewards: Map<Hash, Wei>,
  val transactionTraces: List<TransactionTrace>,
  val feesByTransaction: Map<Transaction, Wei>,
  val totalTransactionsFees: Wei,
  val jsonTrace: String
)

data class TransactionTrace(
  val transaction: Transaction,
  val result: TransactionProcessor.Result,
  val contractsCreated: List<ContractCreated>,
  val contractsDestroyed: List<ContractDestroyed>,
  val internalTransactions: List<InternalTransaction>,
  val touchedAccounts: Set<Address>
)

data class ContractCreated(
  val transactionHash: Hash? = null,
  val originatorAddress: Address,
  val contractAddress: Address,
  val code: Bytes,
  val amount: Wei,
  val type: ContractType? = null,
  val capabilities: Set<ContractCapability>? = null,
  val metadata: ContractMetadata? = null,
  val pc: Int
)

data class ContractDestroyed(
  val transactionHash: Hash? = null,
  val contractAddress: Address,
  val refundAddress: Address,
  val refundAmount: Wei,
  val pc: Int
)

data class InternalTransaction(
  val transactionHash: Hash? = null,
  val fromAddress: Address,
  val toAddress: Address,
  val amount: Wei,
  val pc: Int
)

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
)
