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
class ERC165Detector(
    private val transactionSimulator: TransactionSimulator,
    private val precompiledAddress: Address,
    private val contractAddress: Address,
    private val blockHash: Hash
) {
    @Suppress("UNCHECKED_CAST")
    fun hasERC165Interface(): Boolean? {
        val fn = Function(
            "hasERC165Interface", listOf(Web3Address(contractAddress.toHexString())),
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
            Code(Bytes.fromHexString("6080604052348015600f57600080fd5b506004361060285760003560e01c8063b3b70a2c14602d575b600080fd5b605060048036036020811015604157600080fd5b50356001600160a01b03166064565b604080519115158252519081900360200190f35b604080516301ffc9a760e01b808252600482015290516000916001600160a01b038416916301ffc9a791602480820192602092909190829003018186803b15801560ad57600080fd5b505afa15801560c0573d6000803e3d6000fd5b505050506040513d602081101560d557600080fd5b50519291505056fea265627a7a72315820ae9a14a3d8806fc809ecc6e6729e87f5ad1e937bdfb5c0598a086b575e76db4564736f6c634300050d0032"))
    }
}
