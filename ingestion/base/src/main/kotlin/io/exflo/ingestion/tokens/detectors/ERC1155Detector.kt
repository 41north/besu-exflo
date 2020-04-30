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

import org.apache.tuweni.bytes.Bytes
import org.hyperledger.besu.ethereum.core.Address
import org.hyperledger.besu.ethereum.core.Hash
import org.hyperledger.besu.ethereum.transaction.CallParameter
import org.hyperledger.besu.ethereum.transaction.TransactionSimulator
import org.hyperledger.besu.ethereum.transaction.TransactionSimulatorResult
import org.hyperledger.besu.ethereum.vm.Code
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Bool
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Address as Web3Address

/**
 * Auto generated code with SolidityContractWrapperGen. Do not modify manually!
 */
class ERC1155Detector(
    private val transactionSimulator: TransactionSimulator,
    private val precompiledAddress: Address,
    private val contractAddress: Address,
    private val blockHash: Hash
) {
    @Suppress("UNCHECKED_CAST")
    fun hasERC1155Interface(): Boolean? {
        val fn = Function(
            "hasERC1155Interface", listOf(Web3Address(contractAddress.toHexString())),
            listOf(TypeReference.create(Bool::class.java))
        )
        val fnEncoded = Bytes.fromHexString(FunctionEncoder.encode(fn))
        return execute(fnEncoded, precompiledAddress, blockHash)
            ?.output
            ?.let {
                val rawInput = it.toUnprefixedHexString()
                FunctionReturnDecoder.decode(rawInput, fn.outputParameters) as List<Bool>
            }
            ?.firstOrNull()
            ?.value
    }

    @Suppress("UNCHECKED_CAST")
    fun hasERC1155TokenReceiverInterface(): Boolean? {
        val fn = Function(
            "hasERC1155TokenReceiverInterface",
            listOf(Web3Address(contractAddress.toHexString())),
            listOf(TypeReference.create(Bool::class.java))
        )
        val fnEncoded = Bytes.fromHexString(FunctionEncoder.encode(fn))
        return execute(fnEncoded, precompiledAddress, blockHash)
            ?.output
            ?.let {
                val rawInput = it.toUnprefixedHexString()
                FunctionReturnDecoder.decode(rawInput, fn.outputParameters) as List<Bool>
            }
            ?.firstOrNull()
            ?.value
    }

    private fun execute(
        method: Bytes,
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
            Code(Bytes.fromHexString("608060405234801561001057600080fd5b50600436106100365760003560e01c806328c0b7e21461003b578063e6426ed214610075575b600080fd5b6100616004803603602081101561005157600080fd5b50356001600160a01b031661009b565b604080519115158252519081900360200190f35b6100616004803603602081101561008b57600080fd5b50356001600160a01b031661011e565b604080516301ffc9a760e01b8152630271189760e51b600482015290516000916001600160a01b038416916301ffc9a791602480820192602092909190829003018186803b1580156100ec57600080fd5b505afa158015610100573d6000803e3d6000fd5b505050506040513d602081101561011657600080fd5b505192915050565b604080516301ffc9a760e01b8152636cdb3d1360e11b600482015290516000916001600160a01b038416916301ffc9a791602480820192602092909190829003018186803b1580156100ec57600080fdfea265627a7a72315820bf55f25c4e5b742646ca7d332161dd2fe53270d08bd2e66156d0b80fe163658864736f6c634300050d0032"))
    }
}
