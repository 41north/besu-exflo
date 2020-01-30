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

package io.exflo.ingestion.tokens.detectors

import org.hyperledger.besu.ethereum.core.Address
import org.hyperledger.besu.ethereum.core.Hash
import org.hyperledger.besu.ethereum.transaction.CallParameter
import org.hyperledger.besu.ethereum.transaction.TransactionSimulator
import org.hyperledger.besu.ethereum.transaction.TransactionSimulatorResult
import org.hyperledger.besu.ethereum.vm.Code
import org.hyperledger.besu.util.bytes.BytesValue
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Bool
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Address as Web3Address

/**
 * Auto generated code with SolidityContractWrapperGen. Do not modify manually!
 */
class ERC721Detector(
    private val transactionSimulator: TransactionSimulator,
    private val precompiledAddress: Address,
    private val contractAddress: Address,
    private val blockHash: Hash
) {
    @Suppress("UNCHECKED_CAST")
    fun hasERC721EnumerableInterface(): Boolean? {
        val fn = Function(
            "hasERC721EnumerableInterface",
            listOf(Web3Address(contractAddress.hexString)),
            listOf(TypeReference.create(Bool::class.java))
        )
        val fnEncoded = BytesValue.fromHexString(FunctionEncoder.encode(fn))
        return execute(fnEncoded, precompiledAddress, blockHash)
            ?.output
            ?.let {
                val rawInput = it.toUnprefixedString()
                FunctionReturnDecoder.decode(rawInput, fn.outputParameters) as List<Bool>
            }
            ?.firstOrNull()
            ?.value
    }

    @Suppress("UNCHECKED_CAST")
    fun hasERC721Interface(): Boolean? {
        val fn = Function(
            "hasERC721Interface", listOf(Web3Address(contractAddress.hexString)),
            listOf(TypeReference.create(Bool::class.java))
        )
        val fnEncoded = BytesValue.fromHexString(FunctionEncoder.encode(fn))
        return execute(fnEncoded, precompiledAddress, blockHash)
            ?.output
            ?.let {
                val rawInput = it.toUnprefixedString()
                FunctionReturnDecoder.decode(rawInput, fn.outputParameters) as List<Bool>
            }
            ?.firstOrNull()
            ?.value
    }

    @Suppress("UNCHECKED_CAST")
    fun hasERC721MetadataInterface(): Boolean? {
        val fn = Function(
            "hasERC721MetadataInterface", listOf(Web3Address(contractAddress.hexString)),
            listOf(TypeReference.create(Bool::class.java))
        )
        val fnEncoded = BytesValue.fromHexString(FunctionEncoder.encode(fn))
        return execute(fnEncoded, precompiledAddress, blockHash)
            ?.output
            ?.let {
                val rawInput = it.toUnprefixedString()
                FunctionReturnDecoder.decode(rawInput, fn.outputParameters) as List<Bool>
            }
            ?.firstOrNull()
            ?.value
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

    companion object {
        val CODE: Code =
            Code(
                BytesValue.fromHexString(
                    "608060405234801561001057600080fd5b50600436106100415760003560e01c8063ae3384c714610046578063b04814d214610080578063cb97755c146100a6575b600080fd5b61006c6004803603602081101561005c57600080fd5b50356001600160a01b03166100cc565b604080519115158252519081900360200190f35b61006c6004803603602081101561009657600080fd5b50356001600160a01b031661014f565b61006c600480360360208110156100bc57600080fd5b50356001600160a01b03166101a0565b604080516301ffc9a760e01b8152635b5e139f60e01b600482015290516000916001600160a01b038416916301ffc9a791602480820192602092909190829003018186803b15801561011d57600080fd5b505afa158015610131573d6000803e3d6000fd5b505050506040513d602081101561014757600080fd5b505192915050565b604080516301ffc9a760e01b81526380ac58cd60e01b600482015290516000916001600160a01b038416916301ffc9a791602480820192602092909190829003018186803b15801561011d57600080fd5b604080516301ffc9a760e01b815263780e9d6360e01b600482015290516000916001600160a01b038416916301ffc9a791602480820192602092909190829003018186803b15801561011d57600080fdfea265627a7a723158206f3f19f979bf934ed5895d574e7d67175de656aa23d2099eac354b287dc66f7e64736f6c634300050d0032"
                )
            )
    }
}
