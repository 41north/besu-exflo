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

package io.exflo.ingestion

import io.exflo.ingestion.tracker.ChainTracker
import io.exflo.testutil.KoinTestModules
import io.exflo.testutil.TestChainLoader
import io.exflo.testutil.TestChainSummary
import io.kotlintest.Spec
import io.kotlintest.TestCase
import io.kotlintest.TestResult
import io.kotlintest.extensions.TopLevelTest
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotThrowAny
import io.kotlintest.specs.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.math.BigInteger
import org.hyperledger.besu.ethereum.core.Hash
import org.hyperledger.besu.plugin.services.BesuEvents
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import org.koin.test.inject

class ChainTrackerSpec : FunSpec(), KoinTest {

    private val besuEvents = mockk<BesuEvents>(relaxUnitFun = true)
    private val syncStatusListener = slot<BesuEvents.SyncStatusListener>()
    private val cliOptions = mockk<ExfloCliOptions>()

    private val testChainLoader: TestChainLoader by inject()
    private val testChainSummary: TestChainSummary by inject()

    override fun beforeSpecClass(spec: Spec, tests: List<TopLevelTest>) {

        // we want to capture the status listener so we can call it directly
        every { besuEvents.addSyncStatusListener(capture(syncStatusListener)) } returns 0L

        // cli options
        every { cliOptions.maxForkSize } returns 12
        every { cliOptions.startBlockOverride } returns null

        val configModule = module {

            single { besuEvents }
            // we configure a small maxForkSize for ease of testing
            single { cliOptions }

            single { InMemoryStore.factory }

            single { ChainTracker(get(), get(), get(), get(), get()) }

            factory(named("withHistory")) {
                val factory = InMemoryStore.factoryWithHistory(50)
                ChainTracker(get(), get(), get(), factory, get())
            }
        }

        startKoin { modules(KoinTestModules() + configModule) }

        // import test blocks
        testChainLoader.load()
    }

    override fun afterSpecClass(spec: Spec, results: Map<TestCase, TestResult>) {
        stopKoin()
    }

    init {

        context("Given an empty store") {

            val chainTracker = get<ChainTracker>()

            test("tail should initially be -1") { chainTracker.tail shouldBe -1L }
            test("head should initially equal the test chain summary head") { chainTracker.head shouldBe testChainSummary.head }
            test("calling poll() for the first time should return 0L") { chainTracker.poll() shouldBe Pair(0L, null) }
            test("subsequently calling commit(0L) should succeed") { shouldNotThrowAny { chainTracker.commit(0L) } }

            test("successive calls to poll() should return numbers in sequence until the current head") {

                val expectedHead = testChainSummary.head

                for (number in LongRange(1L, expectedHead)) {
                    chainTracker.poll() shouldBe Pair(number, null)
                    chainTracker.commit(number)
                }
                for (i in 1..10) {
                    chainTracker.poll() shouldBe null
                }
            }

            test("successive calls to commit() should succeed until the current head") {

                val expectedHead = testChainSummary.head

                for (number in LongRange(1L, expectedHead)) {
                    shouldNotThrowAny { chainTracker.commit(number) }
                }
            }
        }

        context("Given a non-empty store") {

            val chainTracker = get<ChainTracker>(named("withHistory"))
            val expectedHead = testChainSummary.head

            test("head should be $expectedHead") { chainTracker.head shouldBe expectedHead }
            test("tail should be equal to 50 minus the maxForkSize") { chainTracker.tail shouldBe 50 - cliOptions.maxForkSize }
        }
    }
}

private class InMemoryStore : ChainTracker.Store {

    companion object {

        val factory = object : ChainTracker.StoreFactory {
            override fun create(networkId: BigInteger): ChainTracker.Store = InMemoryStore()
        }

        fun factoryWithHistory(tail: Long) = object : ChainTracker.StoreFactory {
            override fun create(networkId: BigInteger): ChainTracker.Store {
                val store = InMemoryStore()
                store.tail = tail
                return store
            }
        }
    }

    private var hashMap = mapOf<Long, Hash>()
    private var tail: Long? = null

    override fun setBlockHash(number: Long, hash: Hash) {
        hashMap = hashMap + (number to hash)
    }

    override fun getBlockHash(number: Long): Hash? =
        hashMap[number]

    override fun removeBlockHashesBefore(number: Long) {
        var next = number
        do {
            hashMap = hashMap - number
            next -= 1
        } while (next >= 0L)
    }

    override fun setTail(number: Long) {
        tail = number
    }

    override fun getTail(): Long? = tail

    override fun stop() {}
}
