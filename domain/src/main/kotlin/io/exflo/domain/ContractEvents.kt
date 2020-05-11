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

interface ContractEvent {
    val contract: Address
}

object ContractEvents {

    data class FungibleApproval(
      override val contract: Address,
      val owner: Address,
      val spender: Address,
      val value: UInt256
    ) : ContractEvent

    data class FungibleTransfer(
      override val contract: Address,
      val from: Address,
      val to: Address,
      val value: UInt256
    ) : ContractEvent

    data class NonFungibleApproval(
      override val contract: Address,
      val owner: Address,
      val approved: Address,
      val tokenId: UInt256
    ) : ContractEvent

    data class ApprovalForAll(
      override val contract: Address,
      val owner: Address,
      val operator: Address,
      val approved: Boolean
    ) : ContractEvent

    data class NonFungibleTransfer(
      override val contract: Address,
      val from: Address,
      val to: Address,
      val tokenId: UInt256
    ) : ContractEvent

    data class Sent(
      override val contract: Address,
      val operator: Address,
      val from: Address,
      val to: Address,
      val amount: UInt256,
      val data: Bytes,
      val operatorData: Bytes
    ) : ContractEvent

    data class Minted(
      override val contract: Address,
      val operator: Address,
      val to: Address,
      val amount: UInt256,
      val data: Bytes,
      val operatorData: Bytes
    ) : ContractEvent

    data class Burned(
      override val contract: Address,
      val operator: Address,
      val to: Address,
      val amount: UInt256,
      val data: Bytes,
      val operatorData: Bytes
    ) : ContractEvent

    data class AuthorizedOperator(
      override val contract: Address,
      val operator: Address,
      val holder: Address
    ) : ContractEvent

    data class RevokedOperator(
      override val contract: Address,
      val operator: Address,
      val holder: Address
    ) : ContractEvent

    data class TransferSingle(
      override val contract: Address,
      val operator: Address,
      val from: Address,
      val to: Address,
      val id: UInt256,
      val value: UInt256
    ) : ContractEvent

    data class TransferBatch(
      override val contract: Address,
      val operator: Address,
      val from: Address,
      val to: Address,
      val ids: List<UInt256>,
      val values: List<UInt256>
    ) : ContractEvent

    data class URI(
      override val contract: Address,
      val id: UInt256,
      val value: String
    ) : ContractEvent
}
