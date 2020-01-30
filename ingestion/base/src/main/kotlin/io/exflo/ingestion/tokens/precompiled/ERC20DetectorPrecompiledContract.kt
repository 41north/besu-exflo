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

import io.exflo.ingestion.tokens.detectors.AbstractERC20Detector
import io.exflo.ingestion.tokens.detectors.ERC20Detector
import org.hyperledger.besu.ethereum.core.Address
import org.hyperledger.besu.ethereum.vm.Code
import org.hyperledger.besu.ethereum.vm.EVM

class ERC20DetectorPrecompiledContract(evm: EVM) : AbstractDetectorPrecompiledContract(evm) {

    override val code: Code = AbstractERC20Detector.CODE

    override fun getName(): String = ERC20Detector::class.simpleName!!

    companion object {
        val ADDRESS: Address = Address.fromHexString("0xffffffffffffffffffffffffffffffffffffffff")
    }
}
