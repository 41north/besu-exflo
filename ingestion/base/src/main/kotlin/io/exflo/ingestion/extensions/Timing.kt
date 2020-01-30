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

package io.exflo.ingestion.extensions

/**
 * Executes the given [block] and returns the result and elapsed time in milliseconds.
 */
inline fun <T> measureTimeMillis(block: () -> T): Pair<T, Long> {
    val start = System.currentTimeMillis()
    val output = block()
    return Pair(output, System.currentTimeMillis() - start)
}

/**
 * Executes the given [block] and returns elapsed time in nanoseconds.
 */
inline fun <T> measureNanoTime(block: () -> T): Pair<T, Long> {
    val start = System.nanoTime()
    val output = block()
    return Pair(output, System.nanoTime() - start)
}
