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

package io.exflo.testutil

import org.hyperledger.besu.ethereum.ProtocolContext
import org.hyperledger.besu.ethereum.core.BlockHeader
import org.hyperledger.besu.ethereum.mainnet.HeaderValidationMode
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule
import org.hyperledger.besu.ethereum.util.RawBlockIterator

/**
 * Reads test blocks from file and imports them to the test Blockchain.
 */
class TestChainLoader(
  private val protocolSchedule: ProtocolSchedule<Void>,
  private val protocolContext: ProtocolContext<Void>,
  private val blockIterator: RawBlockIterator
) {

    fun load() {

        blockIterator.forEach { block ->
            if (block.header.number == BlockHeader.GENESIS_BLOCK_NUMBER) {
                return@forEach
            }

            val protocolSpec = protocolSchedule.getByBlockNumber(block.header.number)
            val blockImporter = protocolSpec.blockImporter
            val result = blockImporter.importBlock(protocolContext, block, HeaderValidationMode.FULL)
            check(result) { "Unable to import block " + block.header.number }
        }
    }
}
