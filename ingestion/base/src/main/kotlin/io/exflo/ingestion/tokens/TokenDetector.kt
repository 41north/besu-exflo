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

package io.exflo.ingestion.tokens

import com.tinder.StateMachine
import io.exflo.domain.ContractCapability
import io.exflo.domain.ContractMetadata
import io.exflo.domain.ContractType
import io.exflo.ingestion.extensions.sanitize
import io.exflo.ingestion.tokens.detectors.ERC1155Detector
import io.exflo.ingestion.tokens.detectors.ERC165Detector
import io.exflo.ingestion.tokens.detectors.ERC20Detector
import io.exflo.ingestion.tokens.detectors.ERC721Detector
import io.exflo.ingestion.tokens.detectors.ERC777Detector
import io.exflo.ingestion.tokens.precompiled.ERC1155DetectorPrecompiledContract
import io.exflo.ingestion.tokens.precompiled.ERC165DetectorPrecompiledContract
import io.exflo.ingestion.tokens.precompiled.ERC20DetectorPrecompiledContract
import io.exflo.ingestion.tokens.precompiled.ERC721DetectorPrecompiledContract
import io.exflo.ingestion.tokens.precompiled.ERC777DetectorPrecompiledContract
import org.apache.logging.log4j.LogManager
import org.hyperledger.besu.ethereum.core.Address
import org.hyperledger.besu.ethereum.core.Hash
import org.hyperledger.besu.ethereum.transaction.TransactionSimulator
import org.hyperledger.besu.util.bytes.BytesValue

/**
 * Class that tries to detect if a contract implements any of the following ERC20, ERC165, ERC721, ERC777 or ERC_1155 standards.
 *
 * For doing so, it uses a State Machine, that will perform on each step checks to extract meaningful information.
 *
 * Below there's a diagram of the State Machine:
 *
 * ```
 *                                                                       ---------------------------------------------------------------------------------------
 *                                                                      |                                                                                      |
 *                                                                      |                                                                                      |
 *                                                                      |                                                                                      V
 *  ---------------------        ---------------------        ---------------------        ---------------------        ---------------------        ---------------------
 *  |                   |        |                   |        |                   |        |                   |        |                   |        |                   |
 *  |      INITIAL      | -----> |      ERC777       | -----> |      ERC_165      | -----> |      ERC_1155     | -----> |      ERC_721      | -----> |      ERC_20       |
 *  |                   |        |                   |        |                   |        |                   |        |                   |        |                   |
 *  ---------------------        ---------------------        ---------------------        ---------------------        ---------------------        ---------------------
 *                                   |            |                                           |              |             |              |               |          |
 *                                   |            |---------------------                      |              |             |              |               |          |
 *                                   |                                 |                      |              |             |              |               |          |
 *                                   | -------------------------------------------------------------------------------------------------------------------           |
 *                                   |                                 |                                     |                            |                          |
 *                                   v                                 v                                     |                            |                          |
 *                            ---------------------         ---------------------                            |                            |                          |
 *                            |                   |        |                    |                            |                            |                          |
 *                            |  TOKEN_DETECTED   |        | TOKEN_NOT_DETECTED | <-----------------------------------------------------------------------------------
 *                            |                   |        |                    |
 *                            ---------------------        ---------------------
 * ```
 *
 * For detecting the Tokens we are trying to use as much as possible Solidity (specially on those cases where the token implements ERC165 to interrogate
 * the smart contract directly).
 *
 */
class TokenDetector(
    private val transactionSimulator: TransactionSimulator,
    private val blockHash: Hash,
    private val contractAddress: Address,
    private val contractCode: BytesValue
) {

    private val logger = LogManager.getLogger()

    private var type = ContractType.GENERIC
    private val capabilities = mutableSetOf<ContractCapability>()
    private var metadata = ContractMetadata()

    private val metadataRetriever = ERCMetadataRetriever(transactionSimulator, contractAddress, blockHash)

    private val stateMachine = StateMachine.create<State, Event, NoSideEffect> {

        initialState(State.Initial)

        state<State.Initial> {
            on<Event.OnCheckERC777> {
                logger.debug("Starting token detection -> Block Hash: $blockHash | Contract Address: $contractAddress")
                transitionTo(State.ERC777)
            }
        }

        state<State.ERC777> {
            onEnter {
                logger.debug("Performing checks for ERC777")
                checkERC777()
            }

            on<Event.OnCheckERC165> { transitionTo(State.ERC165) }

            on<Event.OnTokenDetected> { transitionTo(State.TokenDetected) }
        }

        state<State.ERC165> {
            onEnter {
                logger.debug("Performing checks for ERC165")
                checkERC165()
            }

            on<Event.OnCheckERC1155> { transitionTo(State.ERC1155) }

            on<Event.OnCheckERC20> { transitionTo(State.ERC20) }
        }

        state<State.ERC1155> {
            onEnter {
                logger.debug("Performing checks for ERC1155")
                checkERC1155()
            }

            on<Event.OnCheckERC721> { transitionTo(State.ERC721) }

            on<Event.OnTokenDetected> { transitionTo(State.TokenDetected) }
        }

        state<State.ERC721> {
            onEnter {
                logger.debug("Performing checks for ERC721")
                checkERC721()
            }

            on<Event.OnCheckERC20> { transitionTo(State.ERC20) }

            on<Event.OnTokenDetected> { transitionTo(State.TokenDetected) }
        }

        state<State.ERC20> {
            onEnter {
                logger.debug("Performing checks for ERC20")
                checkERC20()
            }

            on<Event.OnTokenDetected> { transitionTo(State.TokenDetected) }

            on<Event.OnTokenNotDetected> { transitionTo(State.TokenNotDetected) }
        }

        state<State.TokenDetected> {
            onEnter {
                logger.debug("Token detected! -> Type: $type | Capabilities: $capabilities | Metadata: $metadata")
            }
        }

        state<State.TokenNotDetected> {
            onEnter {
                logger.debug("Token NOT detected!")
            }
        }
    }

    private fun checkERC777() {
        val detector = ERC777Detector(
            transactionSimulator,
            ERC777DetectorPrecompiledContract.ADDRESS,
            contractAddress,
            blockHash
        )

        when (detector.hasERC777Interface()) {
            true -> {
                type = ContractType.ERC777
                capabilities.add(ContractCapability.ERC777)

                metadata = metadata.copy(
                    name = metadataRetriever.name().sanitize(),
                    symbol = metadataRetriever.symbol().sanitize(),
                    totalSupply = metadataRetriever.totalSupply(),
                    granularity = metadataRetriever.granularity()
                )

                stateMachine.transition(Event.OnTokenDetected)
            }
            else -> stateMachine.transition(Event.OnCheckERC165)
        }
    }

    private fun checkERC165() {
        val detector = ERC165Detector(
            transactionSimulator,
            ERC165DetectorPrecompiledContract.ADDRESS,
            contractAddress,
            blockHash
        )

        when (detector.hasERC165Interface()) {
            true -> {
                capabilities.add(ContractCapability.ERC165)
                stateMachine.transition(Event.OnCheckERC1155)
            }
            else -> stateMachine.transition(Event.OnCheckERC20)
        }
    }

    private fun checkERC1155() {
        val detector = ERC1155Detector(
            transactionSimulator,
            ERC1155DetectorPrecompiledContract.ADDRESS,
            contractAddress,
            blockHash
        )

        when (detector.hasERC1155Interface()) {
            true -> {
                type = ContractType.ERC1155
                capabilities.add(ContractCapability.ERC1155)
                stateMachine.transition(Event.OnTokenDetected)
            }
            else -> stateMachine.transition(Event.OnCheckERC721)
        }
    }

    private fun checkERC721() {
        val detector = ERC721Detector(
            transactionSimulator,
            ERC721DetectorPrecompiledContract.ADDRESS,
            contractAddress,
            blockHash
        )

        when (detector.hasERC721Interface()) {
            true -> {
                type = ContractType.ERC721
                capabilities.add(ContractCapability.ERC721)

                detector
                    .hasERC721MetadataInterface()
                    ?.takeIf { it }
                    ?.apply {
                        capabilities.add(ContractCapability.ERC721_METADATA)
                        metadata = metadata.copy(
                            name = metadataRetriever.name().sanitize(),
                            symbol = metadataRetriever.symbol().sanitize()
                        )
                    }

                detector
                    .hasERC721EnumerableInterface()
                    ?.takeIf { it }
                    ?.apply {
                        capabilities.add(ContractCapability.ERC721_ENUMERABLE)
                        metadata = metadata.copy(
                            totalSupply = metadataRetriever.totalSupply()
                        )
                    }

                stateMachine.transition(Event.OnTokenDetected)
            }
            else -> stateMachine.transition(Event.OnCheckERC20)
        }
    }

    private fun checkERC20() {
        val detector = ERC20Detector(
            transactionSimulator,
            ERC20DetectorPrecompiledContract.ADDRESS,
            contractAddress,
            blockHash,
            contractCode
        )

        when (detector.hasERC20Interface()) {
            true -> {
                type = ContractType.ERC20
                capabilities.add(ContractCapability.ERC20)

                metadata = metadata.copy(
                    totalSupply = metadataRetriever.totalSupply()
                )

                detector
                    .hasERC20DetailedInterface()
                    ?.takeIf { it }
                    ?.apply {
                        capabilities.add(ContractCapability.ERC20_DETAILED)
                        metadata = metadata.copy(
                            name = metadataRetriever.name().sanitize(),
                            symbol = metadataRetriever.symbol().sanitize(),
                            decimals = metadataRetriever.decimals()
                        )
                    }

                detector
                    .hasERC20BurnableInterface()
                    .takeIf { it }
                    ?.apply {
                        capabilities.add(ContractCapability.ERC20_BURNABLE)
                    }

                detector
                    .hasERC20MintableInterface()
                    .takeIf { it }
                    ?.apply {
                        capabilities.add(ContractCapability.ERC20_MINTABLE)
                    }

                detector
                    .hasERC20PausableInterface()
                    .takeIf { it }
                    ?.apply {
                        capabilities.add(ContractCapability.ERC20_PAUSABLE)
                    }

                detector
                    .hasERC20CappedInterface()
                    ?.takeIf { it }
                    ?.apply {
                        capabilities.add(ContractCapability.ERC20_CAPPED)
                        metadata = metadata.copy(
                            cap = metadataRetriever.cap()
                        )
                    }

                stateMachine.transition(Event.OnTokenDetected)
            }
            false -> stateMachine.transition(Event.OnTokenNotDetected)
        }
    }

    /**
     * Performs a detection on a contract.
     * @return a [Triple] of [ContractType], a set of [ContractCapability] and [ContractMetadata].
     */
    fun detect(): Triple<ContractType, Set<ContractCapability>, ContractMetadata> {
        stateMachine.transition(Event.OnCheckERC777)
        return Triple(type, capabilities.toSet(), metadata)
    }

    private sealed class State {
        object Initial : State()
        object ERC1155 : State()
        object ERC777 : State()
        object ERC165 : State()
        object ERC721 : State()
        object ERC20 : State()
        object TokenDetected : State()
        object TokenNotDetected : State()
    }

    private sealed class Event {
        object OnCheckERC1155 : Event()
        object OnCheckERC777 : Event()
        object OnCheckERC165 : Event()
        object OnCheckERC721 : Event()
        object OnCheckERC20 : Event()
        object OnTokenDetected : Event()
        object OnTokenNotDetected : Event()
    }

    private sealed class NoSideEffect
}
