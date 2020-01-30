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

import com.google.flatbuffers.FlatBufferBuilder
import io.exflo.domain.BalanceDelta
import io.exflo.domain.BlockTrace
import io.exflo.domain.extensions.toFlatBuffer
import io.exflo.domain.fb.Block
import io.exflo.ingestion.tokens.events.LogParser
import org.hyperledger.besu.ethereum.core.Account
import org.hyperledger.besu.ethereum.core.BlockBody
import org.hyperledger.besu.ethereum.core.BlockHeader
import org.hyperledger.besu.ethereum.core.TransactionReceipt
import org.hyperledger.besu.util.uint.UInt256

/**
 * Data class that stores different computed information related to a [Block].
 */
data class FullBlock(
    val header: BlockHeader?,
    val body: BlockBody?,
    val receipts: List<TransactionReceipt>,
    val totalDifficulty: UInt256?,
    val trace: BlockTrace?,
    val touchedAccounts: List<Account>?,
    val balanceDeltas: List<BalanceDelta>?
) {

    fun toFlatBuffer(): Block =
        FlatBufferBuilder(1024)
            .let { bb ->
                toFlatBuffer(bb)
                Block.getRootAsBlock(bb.dataBuffer())
            }

    fun toFlatBuffer(bb: FlatBufferBuilder): Int {

        val headerOffset = header?.toFlatBuffer(bb, totalDifficulty)
        val bodyOffset = body?.toFlatBuffer(bb, receipts, trace, LogParser::parse)

        val contractsCreatedAddresses = trace?.transactionTraces
            ?.map { it.contractsCreated }
            ?.flatten()
            ?.map { it.contractAddress }

        val touchedAccountsVectorOffset = touchedAccounts
            ?.map { account -> account.toFlatBuffer(bb, contractsCreatedAddresses!!) }
            ?.let { offsetArray -> Block.createTouchedAccountsVector(bb, offsetArray.toIntArray()) }

        // Only create rewards offset where rewards exist for this block
        val rewardsOffset = if (trace?.rewards?.isNotEmpty() == true) trace.toRewardsFlatBuffer(bb) else null

        val balanceDeltasVectorOffset = balanceDeltas
            ?.map { delta -> delta.toFlatBuffer(bb) }
            ?.let { offsetArray -> Block.createBalanceDeltasVector(bb, offsetArray.toIntArray()) }

        Block.startBlock(bb)

        headerOffset?.let { Block.addHeader(bb, headerOffset) }
        bodyOffset?.let { Block.addBody(bb, bodyOffset) }
        rewardsOffset?.let { Block.addRewards(bb, it) }
        touchedAccountsVectorOffset?.let { Block.addTouchedAccounts(bb, it) }
        balanceDeltasVectorOffset?.let { Block.addBalanceDeltas(bb, it) }

        return Block.endBlock(bb)
    }
}
