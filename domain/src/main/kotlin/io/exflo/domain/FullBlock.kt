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

package io.exflo.domain

import io.exflo.domain.fb.Block
import org.apache.tuweni.units.bigints.UInt256
import org.hyperledger.besu.ethereum.core.Account
import org.hyperledger.besu.ethereum.core.BlockBody
import org.hyperledger.besu.ethereum.core.BlockHeader
import org.hyperledger.besu.ethereum.core.TransactionReceipt

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
)
