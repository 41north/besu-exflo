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

import org.hyperledger.besu.util.bytes.BytesValue

/**
 * Computes the failure function using a boot-strapping process, where the pattern is matched against itself.
 */
private fun ByteArray.computeFailure(): IntArray {
    val failure = IntArray(size)
    var j = 0

    for (i in 1 until size) {
        while (j > 0 && this[j] != this[i]) {
            j = failure[j - 1]
        }
        if (this[j] == this[i]) {
            j++
        }
        failure[i] = j
    }

    return failure
}

/**
 * Search the fixed for the first occurrence of the byte array pattern.
 */
fun ByteArray.indexOf(pattern: ByteArray): Int {

    val failure = pattern.computeFailure()
    var j = 0

    for (i in this.indices) {
        while (j > 0 && pattern[j] != this[i]) {
            j = failure[j - 1]
        }
        if (pattern[j] == this[i]) {
            j++
        }
        if (j == pattern.size) {
            return i - pattern.size + 1
        }
    }

    return -1
}

val ByteArray.bytesValue: BytesValue
    get() = BytesValue.wrap(this)
