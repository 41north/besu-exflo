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

package io.exflo.ingestion.tokens.events

import io.exflo.domain.ContractEvent
import io.exflo.domain.ContractEvents
import io.exflo.ingestion.extensions.toBytesValue
import java.math.BigInteger
import org.hyperledger.besu.ethereum.core.Address
import org.hyperledger.besu.ethereum.core.Log
import org.hyperledger.besu.util.bytes.BytesValue
import org.hyperledger.besu.util.uint.UInt256
import org.web3j.abi.EventEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address as AbiAddress
import org.web3j.abi.datatypes.Bool as AbiBool
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.DynamicBytes as AbiDynamicBytes
import org.web3j.abi.datatypes.Event as AbiEvent
import org.web3j.abi.datatypes.Type as AbiType
import org.web3j.abi.datatypes.Utf8String as AbiString
import org.web3j.abi.datatypes.generated.Uint256 as AbiUint256

@Suppress("UNCHECKED_CAST")
enum class ContractEventParsers(
    private val web3Event: AbiEvent,
    private val parser: (Address, List<AbiType<*>>) -> ContractEvent?
) {

    FungibleApproval(
        AbiEvent(
            "Approval",
            listOf(
                TypeReference.create(AbiAddress::class.java, true),
                TypeReference.create(AbiAddress::class.java, true),
                TypeReference.create(AbiUint256::class.java, false)
            )
        ),
        { contractAddress, values ->
            val (owner, spender, value) = values
            ContractEvents.FungibleApproval(
                contractAddress,
                Address.fromHexString(owner.value as String),
                Address.fromHexString(spender.value as String),
                UInt256.of(value.value as BigInteger)
            )
        }
    ),

    FungibleTransfer(
        AbiEvent(
            "Transfer",
            listOf(
                TypeReference.create(AbiAddress::class.java, true),
                TypeReference.create(AbiAddress::class.java, true),
                TypeReference.create(AbiUint256::class.java, false)
            )
        ),
        { contractAddress, values ->
            val (from, to, value) = values
            ContractEvents.FungibleTransfer(
                contractAddress,
                Address.fromHexString(from.value as String),
                Address.fromHexString(to.value as String),
                UInt256.of(value.value as BigInteger)
            )
        }
    ),

    NonFungibleApproval(
        AbiEvent(
            "Approval",
            listOf(
                TypeReference.create(AbiAddress::class.java, true),
                TypeReference.create(AbiAddress::class.java, true),
                TypeReference.create(AbiUint256::class.java, true)
            )
        ),
        { contractAddress, values ->
            val (owner, approved, tokenId) = values
            ContractEvents.NonFungibleApproval(
                contractAddress,
                Address.fromHexString(owner.value as String),
                Address.fromHexString(approved.value as String),
                UInt256.of(tokenId.value as BigInteger)
            )
        }
    ),

    ApprovalForAll(
        AbiEvent(
            "ApprovalForAll",
            listOf(
                TypeReference.create(AbiAddress::class.java, true),
                TypeReference.create(AbiAddress::class.java, true),
                TypeReference.create(AbiBool::class.java, false)
            )
        ),
        { contractAddress, values ->
            val (owner, operator, approved) = values
            ContractEvents.ApprovalForAll(
                contractAddress,
                Address.fromHexString(owner.value as String),
                Address.fromHexString(operator.value as String),
                approved.value as Boolean
            )
        }
    ),

    NonFungibleTransfer(
        AbiEvent(
            "Transfer",
            listOf(
                TypeReference.create(AbiAddress::class.java, true),
                TypeReference.create(AbiAddress::class.java, true),
                TypeReference.create(AbiUint256::class.java, true)
            )
        ),
        { contractAddress, values ->
            val (from, to, tokenId) = values
            ContractEvents.NonFungibleTransfer(
                contractAddress,
                Address.fromHexString(from.value as String),
                Address.fromHexString(to.value as String),
                UInt256.of(tokenId.value as BigInteger)
            )
        }
    ),

    Sent(
        AbiEvent(
            "Sent",
            listOf(
                TypeReference.create(AbiAddress::class.java, true),
                TypeReference.create(AbiAddress::class.java, true),
                TypeReference.create(AbiAddress::class.java, true),
                TypeReference.create(AbiUint256::class.java, false),
                TypeReference.create(AbiDynamicBytes::class.java, false),
                TypeReference.create(AbiDynamicBytes::class.java, false)
            )
        ),
        { contractAddress, values ->
            val (operator, from, to, amount, data) = values

            // list can only be destructured with up to 5 items
            val operatorData = values[5]

            ContractEvents.Sent(
                contractAddress,
                Address.fromHexString(operator.value as String),
                Address.fromHexString(from.value as String),
                Address.fromHexString(to.value as String),
                UInt256.of(amount.value as BigInteger),
                BytesValue.wrap(data.value as ByteArray),
                BytesValue.wrap(operatorData.value as ByteArray)
            )
        }
    ),

    Minted(
        AbiEvent(
            "Minted",
            listOf(
                TypeReference.create(AbiAddress::class.java, true),
                TypeReference.create(AbiAddress::class.java, true),
                TypeReference.create(AbiUint256::class.java, false),
                TypeReference.create(AbiDynamicBytes::class.java, false),
                TypeReference.create(AbiDynamicBytes::class.java, false)
            )
        ),
        { contractAddress, values ->
            val (operator, to, amount, data, operatorData) = values
            ContractEvents.Minted(
                contractAddress,
                Address.fromHexString(operator.value as String),
                Address.fromHexString(to.value as String),
                UInt256.of(amount.value as BigInteger),
                BytesValue.wrap(data.value as ByteArray),
                BytesValue.wrap(operatorData.value as ByteArray)
            )
        }
    ),

    Burned(
        AbiEvent(
            "Burned",
            listOf(
                TypeReference.create(AbiAddress::class.java, true),
                TypeReference.create(AbiAddress::class.java, true),
                TypeReference.create(AbiUint256::class.java, false),
                TypeReference.create(AbiDynamicBytes::class.java, false),
                TypeReference.create(AbiDynamicBytes::class.java, false)
            )
        ),
        { contractAddress, values ->
            val (operator, to, amount, data, operatorData) = values
            ContractEvents.Burned(
                contractAddress,
                Address.fromHexString(operator.value as String),
                Address.fromHexString(to.value as String),
                UInt256.of(amount.value as BigInteger),
                BytesValue.wrap(data.value as ByteArray),
                BytesValue.wrap(operatorData.value as ByteArray)
            )
        }
    ),

    AuthorizedOperator(
        AbiEvent(
            "AuthorizedOperator",
            listOf(
                TypeReference.create(AbiAddress::class.java, true),
                TypeReference.create(AbiAddress::class.java, true)
            )
        ),
        { contractAddress, values ->
            val (operator, holder) = values
            ContractEvents.AuthorizedOperator(
                contractAddress,
                Address.fromHexString(operator.value as String),
                Address.fromHexString(holder.value as String)
            )
        }
    ),

    RevokedOperator(
        AbiEvent(
            "RevokedOperator",
            listOf(
                TypeReference.create(AbiAddress::class.java, true),
                TypeReference.create(AbiAddress::class.java, true)
            )
        ),
        { contractAddress, values ->
            val (operator, holder) = values
            ContractEvents.RevokedOperator(
                contractAddress,
                Address.fromHexString(operator.value as String),
                Address.fromHexString(holder.value as String)
            )
        }
    ),

    TransferSingle(
        AbiEvent(
            "TransferSingle",
            listOf(
                TypeReference.create(AbiAddress::class.java, true),
                TypeReference.create(AbiAddress::class.java, true),
                TypeReference.create(AbiAddress::class.java, true),
                TypeReference.create(AbiUint256::class.java, false),
                TypeReference.create(AbiUint256::class.java, false)
            )
        ),
        { contractAddress, values ->
            val (operator, from, to, id, value) = values
            ContractEvents.TransferSingle(
                contractAddress,
                Address.fromHexString(operator.value as String),
                Address.fromHexString(from.value as String),
                Address.fromHexString(to.value as String),
                UInt256.of(id.value as BigInteger),
                UInt256.of(value.value as BigInteger)
            )
        }
    ),

    TransferBatch(
        AbiEvent(
            "TransferBatch",
            listOf(
                TypeReference.create(AbiAddress::class.java, true),
                TypeReference.create(AbiAddress::class.java, true),
                TypeReference.create(AbiAddress::class.java, true),
                object : TypeReference<DynamicArray<AbiUint256>>(false) {},
                object : TypeReference<DynamicArray<AbiUint256>>(false) {}
            )
        ),
        { contractAddress, v ->
            val (operator, from, to, ids, values) = v
            ContractEvents.TransferBatch(
                contractAddress,
                Address.fromHexString(operator.value as String),
                Address.fromHexString(from.value as String),
                Address.fromHexString(to.value as String),
                (ids.value as List<AbiUint256>).map { UInt256.of(it.value) },
                (values.value as List<AbiUint256>).map { UInt256.of(it.value) }
            )
        }
    ),

    URI(
        AbiEvent(
            "URI",
            listOf(
                // the abi specifies the non indexed parameter first for some reason. TODO check this
                TypeReference.create(AbiString::class.java, false),
                TypeReference.create(AbiUint256::class.java, true)
            )
        ),
        { contractAddress, values ->

            val (id, value) = values
            ContractEvents.URI(
                contractAddress,
                UInt256.of(id.value as BigInteger),
                value.value as String
            )
        }
    );

    private val numIndexedParameters = web3Event.indexedParameters.size

    private val signature: BytesValue = EventEncoder.encode(web3Event).toBytesValue()

    fun parse(log: Log): ContractEvent? =
        // look for signature and indexed topics match
        if (signature == log.topics.firstOrNull() && log.topics.size == (numIndexedParameters + 1)) {
            // TODO we are converting into hex to re-use web3 utilities. We should replace this with some utilities which operate against BytesValue
            val values =
                log.topics
                    // drop first entry as that contains the method signature
                    .drop(1)
                    .zip(web3Event.indexedParameters)
                    // zip each topic with corresponding parameter definition and extract each value
                    .map { (topic, parameter) -> FunctionReturnDecoder.decodeIndexedValue(topic.toString(), parameter) } +
                    // append the non indexed parameters
                    FunctionReturnDecoder.decode(log.data.hexString, web3Event.nonIndexedParameters)

            parser(log.logger, values)
        } else {
            null
        }
}
