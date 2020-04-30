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

package io.exflo.ingestion.tracer

import io.exflo.domain.ContractCreated
import io.exflo.domain.ContractDestroyed
import io.exflo.domain.InternalTransaction
import org.apache.logging.log4j.LogManager
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.units.bigints.UInt256
import org.hyperledger.besu.ethereum.core.Gas
import org.hyperledger.besu.ethereum.core.ModificationNotAllowedException
import org.hyperledger.besu.ethereum.core.Wei
import org.hyperledger.besu.ethereum.debug.TraceFrame
import org.hyperledger.besu.ethereum.vm.Code
import org.hyperledger.besu.ethereum.vm.MessageFrame
import org.hyperledger.besu.ethereum.vm.OperationTracer
import org.hyperledger.besu.ethereum.vm.Words
import java.util.ArrayDeque
import java.util.Deque
import java.util.EnumSet
import java.util.Optional
import java.util.TreeMap

/**
 * An implementation of an [OperationTracer] in charge of finding the following in a contract:
 *
 *   - Internal Ether transactions
 *   - Possible contract creations
 *   - Possible contract destructions
 *
 */
class ExfloOperationTracer(private val options: TraceOptions = TraceOptions()) : OperationTracer {

    val traceFrames = mutableListOf<TraceFrame>()
    val contractsCreated = mutableListOf<TraceSnapshot<ContractCreated>>()
    val contractsDestroyed = mutableListOf<TraceSnapshot<ContractDestroyed>>()
    val internalTransactions = mutableListOf<TraceSnapshot<InternalTransaction>>()

    val depthFrames = mutableMapOf<Int, Deque<MessageFrame>>()

    private var prevDepth = 0
    private var prevFrame: MessageFrame? = null

    private var lastFrame: TraceFrame? = null

    private val log = LogManager.getLogger()

    init {
        log.debug("${ExfloOperationTracer::class.simpleName} enabled with following trace options -> $options")
    }

    override fun traceExecution(
        frame: MessageFrame,
        currentGasCost: Optional<Gas>,
        executeOperation: OperationTracer.ExecuteOperation
    ) {

        val currentOperation = frame.currentOperation
        val depth = frame.messageStackDepth
        val opcode = frame.currentOperation.name
        val pc = frame.pc
        val gasRemaining = frame.remainingGas
        val exceptionalHaltReason = EnumSet.copyOf(frame.exceptionalHaltReasons)
        val inputData = frame.inputData
        val stack = captureStack(frame)
        val worldUpdater = frame.worldState
        val type = frame.type

        log.trace("Tracing execution -> Type: $type | Depth: $depth | Opcode: $opcode | PC: $pc | Gas remaining: $gasRemaining | ExceptionalHaltReason: $exceptionalHaltReason")

        // If we are creating a new contract, add it to the list of contracts created
        if (type == MessageFrame.Type.CONTRACT_CREATION && pc == 0 && depth == 0) {

            val originatorAddress = frame.originatorAddress
            val contractAddress = frame.contractAddress

            val code = frame.code.bytes
            val amount = frame.value

            val event = ContractCreated(
                originatorAddress = originatorAddress,
                contractAddress = contractAddress,
                code = code,
                amount = amount,
                pc = pc
            )

            log.trace("Contract Created -> $event | PC: $pc | Depth: $depth")

            contractsCreated.add(TraceSnapshot(frame, pc, event))
        }

        // capture refund amount before self destruct
        val refundAmount =
            if (opcode == "SELFDESTRUCT") {
                val balance = frame.worldState.getAccount(frame.contractAddress).balance
                log.trace("Refund amount: $balance")
                balance
            } else
                null

        try {
            executeOperation.execute()
        } finally {

            val outputData = frame.outputData
            val memory = captureMemory(frame)
            val stackPostExecution = captureStack(frame)
            lastFrame?.let { it.gasRemainingPostExecution = gasRemaining }
            val storage = captureStorage(frame)
            val maybeRefunds = when {
                frame.refunds.isEmpty() -> Optional.empty()
                else -> Optional.of(frame.refunds)
            }

            // store traces if enabled
            if (options.traceFrames) {
                lastFrame = TraceFrame(
                    pc,
                    opcode,
                    gasRemaining,
                    currentGasCost,
                    frame.gasRefund,
                    depth,
                    exceptionalHaltReason,
                    frame.recipientAddress,
                    frame.apparentValue,
                    inputData,
                    outputData,
                    if (options.traceStack) Optional.of(stack) else Optional.empty(),
                    memory,
                    storage,
                    worldUpdater,
                    frame.revertReason,
                    maybeRefunds,
                    Optional.ofNullable(frame.messageFrameStack.peek()).map { it.code },
                    frame.currentOperation.stackItemsProduced,
                    if (options.traceStack) Optional.of(stackPostExecution) else Optional.empty(),
                    currentOperation.isVirtualOperation,
                    Optional.empty(), // frame.maybeUpdatedMemory, TODO: This is not public by default ????
                    frame.maybeUpdatedStorage
                )

                log.trace("Adding trace frame: $lastFrame")

                traceFrames.add(lastFrame!!)
            }

            // If there's an issue, we don't process or store invalid information
            if (!exceptionalHaltReason.isNullOrEmpty()) {
                log.trace("Tracing execution -> Exceptional halt reason found: $exceptionalHaltReason")
                return
            }

            // inspect opcodes to extract meaningful events
            when (opcode) {
                "CREATE", "CREATE2" -> {
                    val childFrame = frame.messageFrameStack.first

                    val originatorAddress = frame.originatorAddress
                    val contractAddress = childFrame.contractAddress

                    val inputOffset = UInt256.fromBytes(stack[1])
                    val inputSize = UInt256.fromBytes(stack[2])
                    val inputData = frame.readMemory(inputOffset, inputSize)
                    val code = Code(inputData).bytes

                    val amount = Wei.wrap(stack[0])

                    val event = ContractCreated(
                        originatorAddress = originatorAddress,
                        contractAddress = contractAddress,
                        code = code,
                        amount = amount,
                        pc = pc
                    )

                    log.trace("Contract created -> $event | PC: $pc | Depth: $depth")

                    contractsCreated.add(TraceSnapshot(frame, pc, event))
                }
                "SELFDESTRUCT" -> {
                    val address = frame.contractAddress
                    val refundAddress = Words.toAddress(stack[0])

                    val event = ContractDestroyed(
                        contractAddress = address,
                        refundAddress = refundAddress,
                        refundAmount = refundAmount!!,
                        pc = pc
                    )

                    log.trace("Contract destroyed -> $event | PC: $pc | Depth: $depth")

                    contractsDestroyed.add(TraceSnapshot(frame, pc, event))
                }
                "CALL", "CALLCODE" -> {
                    val fromAddress = frame.recipientAddress
                    val toAddress = Words.toAddress(stack[1])
                    val amount = Wei.wrap(stack[2])

                    val event = InternalTransaction(
                        fromAddress = fromAddress,
                        toAddress = toAddress,
                        amount = amount,
                        pc = pc
                    )

                    log.trace("Internal transaction -> $event | PC: $pc | Depth: $depth")

                    internalTransactions.add(TraceSnapshot(frame, pc, event))
                }
            }

            // keep track of current vs previous depth in order to store last frame executed at previous depth
            // this determines if the create call succeeded or not
            when {
                depth > prevDepth -> prevDepth = depth
                depth < prevDepth -> {
                    depthFrames
                        .getOrElse(prevDepth, { ArrayDeque() })
                        .apply { add(prevFrame) }
                        .also { depthFrames[prevDepth] = it }
                    prevDepth = depth
                }
            }

            // store previous message frame
            prevFrame = frame
        }

        frame.reset()
    }

    override fun tracePrecompileCall(frame: MessageFrame?, gasRequirement: Gas?, output: Bytes?) {
        traceFrames[traceFrames.size - 1].precompiledGasCost = Optional.of(gasRequirement!!)
    }

    private fun captureStorage(frame: MessageFrame): Optional<Map<UInt256, UInt256>> =
        when {
            options.traceStorage -> {
                try {
                    val storageContents: Map<UInt256, UInt256> =
                        TreeMap(
                            frame
                                .worldState
                                .getAccount(frame.recipientAddress)
                                .mutable
                                .updatedStorage
                        )
                    Optional.of(storageContents)
                } catch (e: ModificationNotAllowedException) {
                    val empty: Map<UInt256, UInt256> = TreeMap<UInt256, UInt256>()
                    Optional.of(empty)
                }
            }
            else -> Optional.empty()
        }

    private fun captureMemory(frame: MessageFrame): Optional<Array<Bytes>> =
        when {
            options.traceMemory -> {
                val memoryContents = Array<Bytes>(frame.memoryWordSize().intValue()) { Bytes32.ZERO }
                memoryContents.indices.forEach { i ->
                    memoryContents[i] = frame.readMemory(UInt256.valueOf(i * 32L), UINT256_32)
                }
                Optional.of(memoryContents)
            }
            else -> Optional.empty()
        }

    private fun captureStack(frame: MessageFrame): Array<Bytes32> {
        val stackContents = Array<Bytes32>(frame.stackSize()) { Bytes32.ZERO }
        stackContents.indices.forEach { i -> stackContents[i] = frame.getStackItem(i) }
        return stackContents
    }

    companion object {
        private val UINT256_32 = UInt256.valueOf(32)
    }
}

data class TraceOptions(
    val traceFrames: Boolean = false,
    val traceStorage: Boolean = false,
    val traceMemory: Boolean = false,
    val traceStack: Boolean = false
)

data class TraceSnapshot<T>(
    val frame: MessageFrame,
    val pc: Int,
    val event: T
)
