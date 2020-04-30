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
import io.exflo.domain.fb.events.ApprovalForAll.createApprovalForAll
import io.exflo.domain.fb.events.AuthorizedOperator.createAuthorizedOperator
import io.exflo.domain.fb.events.Burned.createBurned
import io.exflo.domain.fb.events.ContractEvent.ApprovalForAll
import io.exflo.domain.fb.events.ContractEvent.AuthorizedOperator
import io.exflo.domain.fb.events.ContractEvent.Burned
import io.exflo.domain.fb.events.ContractEvent.FungibleApproval
import io.exflo.domain.fb.events.ContractEvent.FungibleTransfer
import io.exflo.domain.fb.events.ContractEvent.Minted
import io.exflo.domain.fb.events.ContractEvent.NonFungibleApproval
import io.exflo.domain.fb.events.ContractEvent.NonFungibleTransfer
import io.exflo.domain.fb.events.ContractEvent.RevokedOperator
import io.exflo.domain.fb.events.ContractEvent.Sent
import io.exflo.domain.fb.events.ContractEvent.TransferBatch
import io.exflo.domain.fb.events.ContractEvent.TransferSingle
import io.exflo.domain.fb.events.ContractEvent.URI
import io.exflo.domain.fb.events.FungibleApproval.createFungibleApproval
import io.exflo.domain.fb.events.FungibleTransfer.createFungibleTransfer
import io.exflo.domain.fb.events.Minted.createMinted
import io.exflo.domain.fb.events.NonFungibleApproval.createNonFungibleApproval
import io.exflo.domain.fb.events.NonFungibleTransfer.createNonFungibleTransfer
import io.exflo.domain.fb.events.RevokedOperator.createRevokedOperator
import io.exflo.domain.fb.events.Sent.createSent
import io.exflo.domain.fb.events.TransferBatch.createTransferBatch
import io.exflo.domain.fb.events.TransferSingle.createTransferSingle
import io.exflo.domain.fb.events.URI.createURI
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.units.bigints.UInt256
import org.hyperledger.besu.ethereum.core.Address

interface ContractEvent {
    val contract: Address

    fun toFlatBuffer(bb: FlatBufferBuilder): Pair<Byte, Int>
}

object ContractEvents {

    data class FungibleApproval(
        override val contract: Address,
        val owner: Address,
        val spender: Address,
        val value: UInt256
    ) : ContractEvent {

        override fun toFlatBuffer(bb: FlatBufferBuilder): Pair<Byte, Int> {
            val contractOffset = contract.toFlatBuffer(bb)
            val ownerOffset = owner.toFlatBuffer(bb)
            val spenderOffset = spender.toFlatBuffer(bb)
            val valueOffset = value.toFlatBuffer(bb)
            return Pair(
                FungibleApproval,
                createFungibleApproval(bb, contractOffset, ownerOffset, spenderOffset, valueOffset)
            )
        }
    }

    data class FungibleTransfer(
        override val contract: Address,
        val from: Address,
        val to: Address,
        val value: UInt256
    ) : ContractEvent {

        override fun toFlatBuffer(bb: FlatBufferBuilder): Pair<Byte, Int> {
            val contractOffset = contract.toFlatBuffer(bb)
            val fromOffset = from.toFlatBuffer(bb)
            val toOffset = to.toFlatBuffer(bb)
            val valueOffset = value.toFlatBuffer(bb)
            return Pair(
                FungibleTransfer,
                createFungibleTransfer(bb, contractOffset, fromOffset, toOffset, valueOffset)
            )
        }
    }

    data class NonFungibleApproval(
        override val contract: Address,
        val owner: Address,
        val approved: Address,
        val tokenId: UInt256
    ) : ContractEvent {

        override fun toFlatBuffer(bb: FlatBufferBuilder): Pair<Byte, Int> {
            val contractOffset = contract.toFlatBuffer(bb)
            val ownerOffset = owner.toFlatBuffer(bb)
            val approvedOffset = approved.toFlatBuffer(bb)
            val tokenIdOffset = tokenId.toFlatBuffer(bb)
            return Pair(
                NonFungibleApproval,
                createNonFungibleApproval(
                    bb,
                    contractOffset,
                    ownerOffset,
                    approvedOffset,
                    tokenIdOffset
                )
            )
        }
    }

    data class ApprovalForAll(
        override val contract: Address,
        val owner: Address,
        val operator: Address,
        val approved: Boolean
    ) : ContractEvent {

        override fun toFlatBuffer(bb: FlatBufferBuilder): Pair<Byte, Int> {
            val contractOffset = contract.toFlatBuffer(bb)
            val ownerOffset = owner.toFlatBuffer(bb)
            val operatorOffset = operator.toFlatBuffer(bb)
            return Pair(
                ApprovalForAll,
                createApprovalForAll(bb, contractOffset, ownerOffset, operatorOffset, approved)
            )
        }
    }

    data class NonFungibleTransfer(
        override val contract: Address,
        val from: Address,
        val to: Address,
        val tokenId: UInt256
    ) : ContractEvent {

        override fun toFlatBuffer(bb: FlatBufferBuilder): Pair<Byte, Int> {
            val contractOffset = contract.toFlatBuffer(bb)
            val fromOffset = from.toFlatBuffer(bb)
            val toOffset = to.toFlatBuffer(bb)
            val tokenIdOffset = tokenId.toFlatBuffer(bb)
            return Pair(
                NonFungibleTransfer,
                createNonFungibleTransfer(bb, contractOffset, fromOffset, toOffset, tokenIdOffset)
            )
        }
    }

    data class Sent(
        override val contract: Address,
        val operator: Address,
        val from: Address,
        val to: Address,
        val amount: UInt256,
        val data: Bytes,
        val operatorData: Bytes
    ) : ContractEvent {

        override fun toFlatBuffer(bb: FlatBufferBuilder): Pair<Byte, Int> {
            val contractOffset = contract.toFlatBuffer(bb)
            val operatorOffset = operator.toFlatBuffer(bb)
            val fromOffset = from.toFlatBuffer(bb)
            val toOffset = to.toFlatBuffer(bb)
            val amountOffset = amount.toFlatBuffer(bb)
            val dataOffset = bb.createByteVector(data.toArray())
            val operatorDataOffset = bb.createByteVector(operatorData.toArray())
            return Pair(
                Sent,
                createSent(
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
    }

    data class Minted(
        override val contract: Address,
        val operator: Address,
        val to: Address,
        val amount: UInt256,
        val data: Bytes,
        val operatorData: Bytes
    ) : ContractEvent {

        override fun toFlatBuffer(bb: FlatBufferBuilder): Pair<Byte, Int> {
            val contractOffset = contract.toFlatBuffer(bb)
            val operatorOffset = operator.toFlatBuffer(bb)
            val toOffset = to.toFlatBuffer(bb)
            val amountOffset = amount.toFlatBuffer(bb)
            val dataOffset = bb.createByteVector(data.toArray())
            val operatorDataOffset = bb.createByteVector(operatorData.toArray())
            return Pair(
                Minted,
                createMinted(
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
    }

    data class Burned(
        override val contract: Address,
        val operator: Address,
        val to: Address,
        val amount: UInt256,
        val data: Bytes,
        val operatorData: Bytes
    ) : ContractEvent {

        override fun toFlatBuffer(bb: FlatBufferBuilder): Pair<Byte, Int> {
            val contractOffset = contract.toFlatBuffer(bb)
            val operatorOffset = operator.toFlatBuffer(bb)
            val toOffset = to.toFlatBuffer(bb)
            val amountOffset = amount.toFlatBuffer(bb)
            val dataOffset = bb.createByteVector(data.toArray())
            val operatorDataOffset = bb.createByteVector(operatorData.toArray())
            return Pair(
                Burned,
                createBurned(
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
    }

    data class AuthorizedOperator(
        override val contract: Address,
        val operator: Address,
        val holder: Address
    ) : ContractEvent {

        override fun toFlatBuffer(bb: FlatBufferBuilder): Pair<Byte, Int> {
            val contractOffset = contract.toFlatBuffer(bb)
            val operatorOffset = operator.toFlatBuffer(bb)
            val holderOffset = holder.toFlatBuffer(bb)
            return Pair(
                AuthorizedOperator,
                createAuthorizedOperator(bb, contractOffset, operatorOffset, holderOffset)
            )
        }
    }

    data class RevokedOperator(
        override val contract: Address,
        val operator: Address,
        val holder: Address
    ) : ContractEvent {

        override fun toFlatBuffer(bb: FlatBufferBuilder): Pair<Byte, Int> {
            val contractOffset = contract.toFlatBuffer(bb)
            val operatorOffset = operator.toFlatBuffer(bb)
            val holderOffset = holder.toFlatBuffer(bb)
            return Pair(
                RevokedOperator,
                createRevokedOperator(bb, contractOffset, operatorOffset, holderOffset)
            )
        }
    }

    data class TransferSingle(
        override val contract: Address,
        val operator: Address,
        val from: Address,
        val to: Address,
        val id: UInt256,
        val value: UInt256
    ) : ContractEvent {

        override fun toFlatBuffer(bb: FlatBufferBuilder): Pair<Byte, Int> {
            val contractOffset = contract.toFlatBuffer(bb)
            val operatorOffset = operator.toFlatBuffer(bb)
            val fromOffset = from.toFlatBuffer(bb)
            val toOffset = to.toFlatBuffer(bb)
            val idOffset = id.toFlatBuffer(bb)
            val valueOffset = value.toFlatBuffer(bb)
            return Pair(
                TransferSingle,
                createTransferSingle(
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
    }

    data class TransferBatch(
        override val contract: Address,
        val operator: Address,
        val from: Address,
        val to: Address,
        val ids: List<UInt256>,
        val values: List<UInt256>
    ) : ContractEvent {

        override fun toFlatBuffer(bb: FlatBufferBuilder): Pair<Byte, Int> {
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
                TransferBatch,
                createTransferBatch(
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
    }

    data class URI(
        override val contract: Address,
        val id: UInt256,
        val value: String
    ) : ContractEvent {

        override fun toFlatBuffer(bb: FlatBufferBuilder): Pair<Byte, Int> {
            val contractOffset = contract.toFlatBuffer(bb)
            val idOffset = id.toFlatBuffer(bb)
            val valueOffset = bb.createString(value)
            return Pair(
                URI,
                createURI(bb, contractOffset, valueOffset, idOffset)
            )
        }
    }
}
