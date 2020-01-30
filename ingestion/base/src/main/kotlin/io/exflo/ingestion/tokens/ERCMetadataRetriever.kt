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

package io.exflo.ingestion.tokens

import org.hyperledger.besu.ethereum.core.Address
import org.hyperledger.besu.ethereum.core.Hash
import org.hyperledger.besu.ethereum.transaction.CallParameter
import org.hyperledger.besu.ethereum.transaction.TransactionSimulator
import org.hyperledger.besu.ethereum.transaction.TransactionSimulatorResult
import org.hyperledger.besu.util.bytes.BytesValue
import org.hyperledger.besu.util.uint.UInt256
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Utf8String
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.abi.datatypes.generated.Uint8

/**
 * This class in charge of extracting common metadata fields founds on Token standards like ERC20, ERC721, ERC777.
 */
class ERCMetadataRetriever(
    private val transactionSimulator: TransactionSimulator,
    private val contractAddress: Address,
    private val blockHash: Hash
) {

    @Suppress("UNCHECKED_CAST")
    fun name(): String? {
        val fn = Function(
            "name",
            listOf(),
            listOf(TypeReference.create(Utf8String::class.java))
        )
        val fnEncoded = BytesValue.fromHexString(FunctionEncoder.encode(fn))
        return execute(fnEncoded, contractAddress, blockHash)
            ?.output
            ?.let {
                val rawInput = it.toUnprefixedString()
                FunctionReturnDecoder.decode(rawInput, fn.outputParameters) as List<Utf8String>
            }
            ?.firstOrNull()
            ?.value
    }

    @Suppress("UNCHECKED_CAST")
    fun symbol(): String? {
        val fn = Function(
            "symbol",
            listOf(),
            listOf(TypeReference.create(Utf8String::class.java))
        )
        val fnEncoded = BytesValue.fromHexString(FunctionEncoder.encode(fn))
        return execute(fnEncoded, contractAddress, blockHash)
            ?.output
            ?.let {
                val rawInput = it.toUnprefixedString()
                FunctionReturnDecoder.decode(rawInput, fn.outputParameters) as List<Utf8String>
            }
            ?.firstOrNull()
            ?.value
    }

    @Suppress("UNCHECKED_CAST")
    fun totalSupply(): UInt256? {
        val fn = Function(
            "totalSupply",
            listOf(),
            listOf(TypeReference.create(Uint256::class.java))
        )
        val fnEncoded = BytesValue.fromHexString(FunctionEncoder.encode(fn))
        return execute(fnEncoded, contractAddress, blockHash)
            ?.output
            ?.let {
                val rawInput = it.toUnprefixedString()
                FunctionReturnDecoder.decode(rawInput, fn.outputParameters) as List<Uint256>
            }
            ?.firstOrNull()
            ?.value?.let { UInt256.of(it) }
    }

    @Suppress("UNCHECKED_CAST")
    fun decimals(): Byte? {
        val fn = Function(
            "decimals",
            listOf(),
            listOf(TypeReference.create(Uint8::class.java))
        )
        val fnEncoded = BytesValue.fromHexString(FunctionEncoder.encode(fn))
        return execute(fnEncoded, contractAddress, blockHash)
            ?.output
            ?.let {
                val rawInput = it.toUnprefixedString()
                FunctionReturnDecoder.decode(rawInput, fn.outputParameters) as List<Uint8>
            }
            ?.firstOrNull()
            ?.value
            ?.toByte()
    }

    @Suppress("UNCHECKED_CAST")
    fun cap(): UInt256? {
        val fn = Function(
            "cap",
            listOf(),
            listOf(TypeReference.create(Uint256::class.java))
        )
        val fnEncoded = BytesValue.fromHexString(FunctionEncoder.encode(fn))
        return execute(fnEncoded, contractAddress, blockHash)
            ?.output
            ?.let {
                val rawInput = it.toUnprefixedString()
                FunctionReturnDecoder.decode(rawInput, fn.outputParameters) as List<Uint256>
            }
            ?.firstOrNull()
            ?.value?.let { UInt256.of(it) }
    }

    @Suppress("UNCHECKED_CAST")
    fun granularity(): UInt256? {
        val fn = Function(
            "granularity",
            listOf(),
            listOf(TypeReference.create(Uint256::class.java))
        )
        val fnEncoded = BytesValue.fromHexString(FunctionEncoder.encode(fn))
        return execute(fnEncoded, contractAddress, blockHash)
            ?.output
            ?.let {
                val rawInput = it.toUnprefixedString()
                FunctionReturnDecoder.decode(rawInput, fn.outputParameters) as List<Uint256>
            }
            ?.firstOrNull()
            ?.value?.let { UInt256.of(it) }
    }

    private fun execute(
        method: BytesValue,
        address: Address,
        blockHash: Hash
    ): TransactionSimulatorResult? = transactionSimulator.process(
        CallParameter(
            null,
            address,
            100_000,
            null,
            null,
            method
        ),
        blockHash
    )
        .orElseGet(null)
        ?.takeIf { it.isSuccessful }
}
