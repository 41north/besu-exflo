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
abstract class AbstractERC20Detector(
  private val transactionSimulator: TransactionSimulator,
  private val precompiledAddress: Address,
  private val contractAddress: Address,
  private val blockHash: Hash
) {
  @Suppress("UNCHECKED_CAST")
  fun hasERC20CappedInterface(): Boolean? {
    val fn = Function(
      "hasERC20CappedInterface", listOf(Web3Address(contractAddress.toHexString())),
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
  fun hasERC20DetailedInterface(): Boolean? {
    val fn = Function(
      "hasERC20DetailedInterface", listOf(Web3Address(contractAddress.toHexString())),
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
      Code(
        Bytes.fromHexString(
          "608060405234801561001057600080fd5b50600436106100365760003560e01c8063586e699a1461003b5780637273538c14610075575b600080fd5b6100616004803603602081101561005157600080fd5b50356001600160a01b031661009b565b604080519115158252519081900360200190f35b6100616004803603602081101561008b57600080fd5b50356001600160a01b031661010f565b600080829050806001600160a01b031663355274ea6040518163ffffffff1660e01b815260040160206040518083038186803b1580156100da57600080fd5b505afa1580156100ee573d6000803e3d6000fd5b505050506040513d602081101561010457600080fd5b506001949350505050565b600080829050806001600160a01b03166306fdde036040518163ffffffff1660e01b815260040160006040518083038186803b15801561014e57600080fd5b505afa158015610162573d6000803e3d6000fd5b505050506040513d6000823e601f3d908101601f19168201604052602081101561018b57600080fd5b81019080805160405193929190846401000000008211156101ab57600080fd5b9083019060208201858111156101c057600080fd5b82516401000000008111828201881017156101da57600080fd5b82525081516020918201929091019080838360005b838110156102075781810151838201526020016101ef565b50505050905090810190601f1680156102345780820380516001836020036101000a031916815260200191505b5060405250505050806001600160a01b03166395d89b416040518163ffffffff1660e01b815260040160006040518083038186803b15801561027557600080fd5b505afa158015610289573d6000803e3d6000fd5b505050506040513d6000823e601f3d908101601f1916820160405260208110156102b257600080fd5b81019080805160405193929190846401000000008211156102d257600080fd5b9083019060208201858111156102e757600080fd5b825164010000000081118282018810171561030157600080fd5b82525081516020918201929091019080838360005b8381101561032e578181015183820152602001610316565b50505050905090810190601f16801561035b5780820380516001836020036101000a031916815260200191505b5060405250505050806001600160a01b031663313ce5676040518163ffffffff1660e01b815260040160206040518083038186803b1580156100da57600080fdfea265627a7a72315820568d87734204e465c9e409dca32b9ff9d83551827f8c17303998aac9750d615764736f6c634300050d0032"
        )
      )
  }
}
