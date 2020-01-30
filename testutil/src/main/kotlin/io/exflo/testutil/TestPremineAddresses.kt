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

package io.exflo.testutil

import org.hyperledger.besu.ethereum.core.Address

/**
 * Hardcoded addreses that are being used in Truffle as well to generate concrete tests.
 */
object TestPremineAddresses {

    val one: Address = Address.fromHexString("fe3b557e8fb62b89f4916b721be55ceb828dbd73")!!
    val two: Address = Address.fromHexString("627306090abaB3A6e1400e9345bC60c78a8BEf57")!!
    val three: Address = Address.fromHexString("f17f52151EbEF6C7334FAD080c5704D77216b732")!!

    val all = listOf(one, two, three)
}
