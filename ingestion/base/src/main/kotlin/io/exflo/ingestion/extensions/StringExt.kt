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

import org.hyperledger.besu.ethereum.core.Wei
import java.math.BigInteger
import java.util.Locale

fun String.hexToLong(): Long {
    var lowercase = this.toLowerCase(Locale.US)
    if (lowercase.startsWith("0x")) {
        lowercase = lowercase.substring(2)
    }
    return lowercase.toLong(16)
}

fun String.toWei(): Wei =
    if (this.startsWith("0x")) {
        Wei.fromHexString(this)
    } else {
        Wei.of(BigInteger(this))
    }
