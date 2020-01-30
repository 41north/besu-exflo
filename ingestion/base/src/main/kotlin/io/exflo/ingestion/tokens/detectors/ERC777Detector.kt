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
class ERC777Detector(
    private val transactionSimulator: TransactionSimulator,
    private val precompiledAddress: Address,
    private val contractAddress: Address,
    private val blockHash: Hash
) {
    @Suppress("UNCHECKED_CAST")
    fun hasERC777Interface(): Boolean? {
        val fn = Function(
            "hasERC777Interface", listOf(Web3Address(contractAddress.hexString)),
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
                    "608060405234801561001057600080fd5b506004361061002b5760003560e01c8063b74adced14610030575b600080fd5b6100566004803603602081101561004657600080fd5b50356001600160a01b031661006a565b604080519115158252519081900360200190f35b600080829050806001600160a01b03166306fdde036040518163ffffffff1660e01b815260040160006040518083038186803b1580156100a957600080fd5b505afa1580156100bd573d6000803e3d6000fd5b505050506040513d6000823e601f3d908101601f1916820160405260208110156100e657600080fd5b810190808051604051939291908464010000000082111561010657600080fd5b90830190602082018581111561011b57600080fd5b825164010000000081118282018810171561013557600080fd5b82525081516020918201929091019080838360005b8381101561016257818101518382015260200161014a565b50505050905090810190601f16801561018f5780820380516001836020036101000a031916815260200191505b5060405250505050806001600160a01b03166395d89b416040518163ffffffff1660e01b815260040160006040518083038186803b1580156101d057600080fd5b505afa1580156101e4573d6000803e3d6000fd5b505050506040513d6000823e601f3d908101601f19168201604052602081101561020d57600080fd5b810190808051604051939291908464010000000082111561022d57600080fd5b90830190602082018581111561024257600080fd5b825164010000000081118282018810171561025c57600080fd5b82525081516020918201929091019080838360005b83811015610289578181015183820152602001610271565b50505050905090810190601f1680156102b65780820380516001836020036101000a031916815260200191505b5060405250505050806001600160a01b03166318160ddd6040518163ffffffff1660e01b815260040160206040518083038186803b1580156102f757600080fd5b505afa15801561030b573d6000803e3d6000fd5b505050506040513d602081101561032157600080fd5b50506040805163556f0dc760e01b815290516001600160a01b0383169163556f0dc7916004808301926020929190829003018186803b15801561036357600080fd5b505afa158015610377573d6000803e3d6000fd5b505050506040513d602081101561038d57600080fd5b50506040805162dc90a760e31b815290516001600160a01b038316916306e48538916004808301926000929190829003018186803b1580156103ce57600080fd5b505afa1580156103e2573d6000803e3d6000fd5b505050506040513d6000823e601f3d908101601f19168201604052602081101561040b57600080fd5b810190808051604051939291908464010000000082111561042b57600080fd5b90830190602082018581111561044057600080fd5b825186602082028301116401000000008211171561045d57600080fd5b82525081516020918201928201910280838360005b8381101561048a578181015183820152602001610472565b5050505090500160405250505050600191505091905056fea265627a7a723158203caf417af0262784c6cdfbd9854988c421df48617043e6cfba4b3bd02f02f1c764736f6c634300050d0032"
                )
            )
    }
}
