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

import io.exflo.ingestion.extensions.bytesValue
import io.exflo.ingestion.extensions.indexOf
import org.apache.tuweni.bytes.Bytes
import org.hyperledger.besu.crypto.Hash.keccak256
import org.hyperledger.besu.ethereum.core.Address
import org.hyperledger.besu.ethereum.core.Hash
import org.hyperledger.besu.ethereum.transaction.TransactionSimulator

/**
 * Detects ERC20 contracts (or at least, it tries to do so).
 *
 * ERC20 is one of the first proposed standard, previous to the creation of ERC165 that allows to interrogate directly if a given contract implements or not
 * a series of interfaces.
 *
 * For that reason, some of the methods to detect if a given smart contract complies to ERC20 or not is to interrogate directly if the function signatures are present
 * on the raw source code of the contract.
 *
 * This detector apart from checking if a token is a compliant ERC20, tries to detect as well some common implementations offered by OpenZeppelin implementation
 * variants (burnable, mintable, pausable).
 *
 * See https://eips.ethereum.org/EIPS/eip-20 for more information about this ERC.
 */
class ERC20Detector(
    transactionSimulator: TransactionSimulator,
    precompiledAddress: Address,
    contractAddress: Address,
    blockHash: Hash,
    contractCode: Bytes
) : AbstractERC20Detector(
    transactionSimulator,
    precompiledAddress,
    contractAddress,
    blockHash
) {

    private val code = contractCode.toArray()

    fun hasERC20Interface(): Boolean = ERC20Signatures.erc20.find { code.indexOf(it) == -1 }?.let { false } ?: true

    fun hasERC20BurnableInterface(): Boolean = ERC20Signatures.erc20Burnable.find { code.indexOf(it) == -1 }?.let { false } ?: true

    fun hasERC20MintableInterface(): Boolean = ERC20Signatures.erc20Mintable.find { code.indexOf(it) == -1 }?.let { false } ?: true

    fun hasERC20PausableInterface(): Boolean = ERC20Signatures.erc20Pausable.find { code.indexOf(it) == -1 }?.let { false } ?: true
}

object ERC20Signatures {

    val erc20 = listOf(
        `4bytes`("totalSupply()"),
        `4bytes`("balanceOf(address)"),
        `4bytes`("transfer(address,uint256)"),
        `4bytes`("allowance(address,address)"),
        `4bytes`("approve(address,uint256)"),
        `4bytes`("transferFrom(address,address,uint256)")
    )

    val erc20Burnable = listOf(
        `4bytes`("burn(uint256)"),
        `4bytes`("burnFrom(address,uint256)")
    )

    val erc20Mintable = listOf(
        `4bytes`("mint(address,uint256)")
    )

    val erc20Pausable = listOf(
        `4bytes`("pause()"),
        `4bytes`("unpause()")
    )

    @Suppress("FunctionName")
    private fun `4bytes`(method: String) = keccak256(method.toByteArray().bytesValue).slice(0, 4).toArray()
}
