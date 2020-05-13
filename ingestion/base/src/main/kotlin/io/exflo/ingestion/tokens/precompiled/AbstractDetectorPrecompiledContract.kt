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

import org.apache.tuweni.bytes.Bytes
import org.hyperledger.besu.ethereum.core.Gas
import org.hyperledger.besu.ethereum.mainnet.MainnetMessageCallProcessor
import org.hyperledger.besu.ethereum.mainnet.PrecompileContractRegistry
import org.hyperledger.besu.ethereum.mainnet.PrecompiledContract
import org.hyperledger.besu.ethereum.vm.Code
import org.hyperledger.besu.ethereum.vm.EVM
import org.hyperledger.besu.ethereum.vm.MessageFrame
import org.hyperledger.besu.ethereum.vm.OperationTracer
import java.util.ArrayDeque

abstract class AbstractDetectorPrecompiledContract(
  private val evm: EVM
) : PrecompiledContract {

  abstract val code: Code

  override fun compute(input: Bytes, frame: MessageFrame): Bytes {
    // Create an empty MessageFrame to use in our disposable EVM
    val messageFrameStack = ArrayDeque<MessageFrame>()
    val updater = frame.worldState.updater()

    // Create the initial frame
    @Suppress("UNUSED_LAMBDA_EXPRESSION")
    val initialFrame = MessageFrame.builder()
      .type(MessageFrame.Type.MESSAGE_CALL)
      .messageFrameStack(messageFrameStack)
      .blockchain(frame.blockchain)
      .worldState(updater)
      .initialGas(frame.remainingGas)
      .address(frame.contractAddress)
      .originator(frame.senderAddress)
      .contract(frame.contractAddress)
      .contractAccountVersion(frame.contractAccountVersion)
      .gasPrice(frame.gasPrice)
      .inputData(frame.inputData)
      .sender(frame.senderAddress)
      .value(frame.value)
      .apparentValue(frame.apparentValue)
      .code(code)
      .blockHeader(frame.blockHeader)
      .depth(0)
      .completer { _ -> {} }
      .miningBeneficiary(frame.miningBeneficiary)
      .blockHashLookup(frame.blockHashLookup)
      .maxStackSize(frame.maxStackSize)
      .isPersistingPrivateState(false)
      .build()

    messageFrameStack.addFirst(initialFrame)

    val executor = MainnetMessageCallProcessor(evm, PrecompileContractRegistry())

    while (!messageFrameStack.isEmpty()) {
      executor.process(messageFrameStack.peekFirst(), OperationTracer.NO_TRACING)
    }

    return initialFrame.outputData
  }

  override fun gasRequirement(input: Bytes?): Gas = Gas.of(1L)
}
