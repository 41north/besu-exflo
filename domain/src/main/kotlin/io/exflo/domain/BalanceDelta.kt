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

import org.apache.tuweni.units.bigints.UInt256
import org.hyperledger.besu.ethereum.core.Address
import org.hyperledger.besu.ethereum.core.Hash
import org.hyperledger.besu.ethereum.core.Wei

data class BalanceDelta(
  val deltaType: DeltaType,
  val pc: Int,
  val transactionHash: Hash? = null,
  val transactionIndex: Int? = null,
  val contractAddress: Address? = null,
  val from: Address? = null,
  val to: Address? = null,
  val amount: Wei? = null,
  val tokenId: UInt256? = null
)

enum class DeltaType {
  BLOCK_REWARD,
  OMMER_REWARD,
  TX,
  TX_FEE,
  INTERNAL_TX,
  TOKEN_TRANSFER,
  CONTRACT_CREATION,
  CONTRACT_DESTRUCTION
}
