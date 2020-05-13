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

package io.exflo.ingestion.tracker

import io.exflo.ingestion.ExfloCliOptions
import org.apache.logging.log4j.LogManager
import org.hyperledger.besu.cli.config.EthNetworkConfig
import org.hyperledger.besu.ethereum.chain.Blockchain
import org.hyperledger.besu.ethereum.core.BlockHeader
import org.hyperledger.besu.ethereum.core.Hash
import org.hyperledger.besu.plugin.data.SyncStatus
import org.hyperledger.besu.plugin.services.BesuEvents
import java.math.BigInteger
import java.util.Optional
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.max
import kotlin.math.min

class ChainTracker(
  private val blockchain: Blockchain,
  cliOptions: ExfloCliOptions,
  networkConfig: EthNetworkConfig,
  storeFactory: StoreFactory,
  private val besuEvents: BesuEvents
) {

  private val log = LogManager.getLogger()

  private val networkId = networkConfig.networkId

  private val maxForkSize = cliOptions.maxForkSize
  private val store = storeFactory.create(networkId)

  private val syncListener = SyncListener()
  private val syncListenerId: Long

  private val lock = ReentrantLock()

  private var numbersToDelete = emptyList<LongRange>()

  @Volatile
  var head: Long = 0L

  val tail: Long
    get() = store.getTail() ?: -1L

  init {

    val maxForkSize = cliOptions.maxForkSize

    // initialise head from local storage
    updateHead(blockchain.getBlockHeader(blockchain.chainHead.hash).get())

    // initialise tail from options override or cache
    var initialTail = cliOptions.startBlockOverride ?: store.getTail() ?: -1L

    if (initialTail > 0L && cliOptions.startBlockOverride == null) {
      // a fork may have happened when we were offline, replay to ensure we missed nothing
      initialTail -= maxForkSize
      if (initialTail < 0L) initialTail = -1L
    }

    store.setTail(initialTail)

    // register a sync listener
    syncListenerId = besuEvents.addSyncStatusListener(syncListener)

    log.info("Initialised. Tail = {}, head = {}", initialTail, head)
  }

  fun poll(): Pair<Long, LongRange?>? {

    lock.lock()
    try {
      return when (tail < head) {
        true -> {
          val number = tail + 1

          val blockHash = blockchain.getBlockHashByNumber(number).get()
          store.setBlockHash(number, blockHash)

          val numbersToDelete =
            when (this.numbersToDelete.isEmpty()) {
              true -> null
              false -> this.numbersToDelete
                .reduce { memo, next ->
                  LongRange(
                    min(memo.start, next.start),
                    max(memo.endInclusive, next.endInclusive)
                  )
                }
            }

          this.numbersToDelete = emptyList()

          Pair(number, numbersToDelete)
        }
        false -> null
      }
    } finally {
      lock.unlock()
    }
  }

  fun commit(number: Long): Boolean {

    lock.lock()

    try {

      require(number <= head) { "number must be less than or equal to the current head" }

      val successful = number == (tail + 1)
      if (successful) {
        store.setTail(number)
        store.removeBlockHashesBefore(number - maxForkSize)
      }

      return successful
    } finally {
      lock.unlock()
    }
  }

  fun stop() {
    blockchain.removeObserver(syncListenerId)
    store.stop()
  }

  private fun updateHead(newHead: BlockHeader) {
    lock.lock()
    try {
      head = newHead.number

      // check for divergence

      var count = 0
      var currentHeader = newHead
      var forkHeader = currentHeader

      do {
        val cachedHash = store.getBlockHash(currentHeader.number)

        if (cachedHash != null && cachedHash != currentHeader.hash) {
          forkHeader = currentHeader
        }

        if (currentHeader.number == BlockHeader.GENESIS_BLOCK_NUMBER) break

        currentHeader = blockchain.getBlockHeader(currentHeader.parentHash).get()
        count += 1
      } while (count < maxForkSize)

      if (forkHeader != newHead) {

        // reset tail and add to numbers to delete
        numbersToDelete = numbersToDelete + listOf(LongRange(forkHeader.number, newHead.number))
        store.setTail(forkHeader.number - 1L)
      }
    } finally {
      lock.unlock()
    }
  }

  private inner class SyncListener : BesuEvents.SyncStatusListener {
    override fun onSyncStatusChanged(syncStatus: Optional<SyncStatus>) {
      if (!syncStatus.isPresent) return
      val header = blockchain.getBlockHeader(syncStatus.get().currentBlock).get()
      updateHead(header)
    }
  }

  interface StoreFactory {
    fun create(networkId: BigInteger): Store
  }

  /**
   * An abstraction for the state store used by a chain tracker instance to record the latest block number it has returned in a poll
   */
  interface Store {

    fun setBlockHash(number: Long, hash: Hash)

    fun getBlockHash(number: Long): Hash?

    fun removeBlockHashesBefore(number: Long)

    fun setTail(number: Long)

    fun getTail(): Long?

    fun stop()
  }
}
