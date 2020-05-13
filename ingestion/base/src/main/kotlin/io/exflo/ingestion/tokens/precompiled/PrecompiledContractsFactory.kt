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

package io.exflo.ingestion.tokens.precompiled

import io.exflo.ingestion.extensions.reflektField
import io.exflo.ingestion.tokens.EVMFactory
import org.hyperledger.besu.ethereum.core.Account
import org.hyperledger.besu.ethereum.mainnet.MutableProtocolSchedule
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule
import org.hyperledger.besu.ethereum.mainnet.ScheduledProtocolSpec
import java.math.BigInteger
import java.util.NavigableSet

/**
 * In order to use directly Solidity to detect several Token types (instead of relying directly with low level stuff)
 * we need to register special custom **precompiled** contracts with **special addresses**.
 *
 * This class registers [ERC20DetectorPrecompiledContract], [ERC165DetectorPrecompiledContract],
 * [ERC721DetectorPrecompiledContract], [ERC777DetectorPrecompiledContract], [ERC1155DetectorPrecompiledContract]
 * for each [ScheduledProtocolSpec].
 */
object PrecompiledContractsFactory {

  fun register(protocolSchedule: ProtocolSchedule<*>, chainId: BigInteger) {
    check(protocolSchedule is MutableProtocolSchedule<*>) { "protocolSchedule must be of MutableProtocolSchedule" }

    // TODO: Review with Besu devs if there's a better way to avoid having reflection here
    val protocolSpecs =
      reflektField<NavigableSet<ScheduledProtocolSpec<*>>>(protocolSchedule, "protocolSpecs")

    // TODO: MainnetEvmRegistries is also another useful class that shouldn't be private
    val evm = EVMFactory.istanbul(chainId)

    val erc20Detector = ERC20DetectorPrecompiledContract(evm)
    val erc165Detector = ERC165DetectorPrecompiledContract(evm)
    val erc721Detector = ERC721DetectorPrecompiledContract(evm)
    val erc777Detector = ERC777DetectorPrecompiledContract(evm)
    val erc1155Detector = ERC1155DetectorPrecompiledContract(evm)

    protocolSpecs
      .map { it.spec.precompileContractRegistry }
      .forEach { precompileContractRegistry ->
        with(precompileContractRegistry) {
          put(
            ERC20DetectorPrecompiledContract.ADDRESS,
            Account.DEFAULT_VERSION,
            erc20Detector
          )

          put(
            ERC165DetectorPrecompiledContract.ADDRESS,
            Account.DEFAULT_VERSION,
            erc165Detector
          )

          put(
            ERC721DetectorPrecompiledContract.ADDRESS,
            Account.DEFAULT_VERSION,
            erc721Detector
          )

          put(
            ERC777DetectorPrecompiledContract.ADDRESS,
            Account.DEFAULT_VERSION,
            erc777Detector
          )

          put(
            ERC1155DetectorPrecompiledContract.ADDRESS,
            Account.DEFAULT_VERSION,
            erc1155Detector
          )
        }
      }
  }
}
