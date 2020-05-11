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

package io.exflo.ingestion.core

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.units.bigints.UInt256
import org.hyperledger.besu.config.GenesisAllocation
import org.hyperledger.besu.ethereum.core.Account
import org.hyperledger.besu.ethereum.core.AccountStorageEntry
import org.hyperledger.besu.ethereum.core.Address
import org.hyperledger.besu.ethereum.core.Hash
import org.hyperledger.besu.ethereum.core.Wei
import java.math.BigInteger
import java.util.Locale
import java.util.NavigableMap

/**
 * Implementation of [Account] that holds its data as properties.
 *
 * It's used for generating synthetic [GenesisAllocation] accounts for genesis block.
 */
class InMemoryAccount(
    private val address: Address,
    private val balance: Wei,
    private val nonce: Long,
    private val code: Bytes?,
    private val codeHash: Hash?
) : Account {

    override fun getBalance(): Wei = balance

    override fun getStorageValue(key: UInt256?): UInt256 {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getAddressHash(): Hash = Hash.fromHexString(address.toHexString())

    override fun getOriginalStorageValue(key: UInt256?): UInt256 {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getAddress(): Address = address

    override fun getVersion(): Int {
        throw UnsupportedOperationException("not implemented")
    }

    override fun storageEntriesFrom(startKeyHash: Bytes32?, limit: Int): NavigableMap<Bytes32, AccountStorageEntry> {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getCode(): Bytes = code ?: Bytes.EMPTY

    override fun getNonce(): Long = nonce

    override fun getCodeHash(): Hash = codeHash ?: Hash.EMPTY

    companion object {

        fun fromGenesisAllocation(allocation: GenesisAllocation): InMemoryAccount {

            val nonce = toUnsignedLong(allocation.nonce)
            val address = Address.fromHexString(allocation.address)
            val balance = toWei(allocation.balance)
            val code = allocation.code?.let { Bytes.fromHexString(it) }

            return InMemoryAccount(address, balance, nonce, code, null)
        }

        private fun toUnsignedLong(s: String): Long {
            var value = s.toLowerCase(Locale.US)
            if (value.startsWith("0x")) {
                value = value.substring(2)
            }
            return java.lang.Long.parseUnsignedLong(value, 16)
        }

        private fun toWei(s: String): Wei =
            if (s.startsWith("0x")) {
                Wei.fromHexString(s)
            } else {
                Wei.of(BigInteger(s))
            }
    }
}
