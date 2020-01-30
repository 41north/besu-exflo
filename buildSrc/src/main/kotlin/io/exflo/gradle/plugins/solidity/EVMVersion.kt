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

package io.exflo.gradle.plugins.solidity

/**
 * Ethereum Virtual Machines available to use for compilation.
 */
@Suppress("unused")
enum class EVMVersion {
    HOMESTEAD {
        override val value = "homestead"
    },
    TANGERINE_WHISTLE {
        override val value = "tangerineWhistle"
    },
    SPURIOUS_DRAGON {
        override val value = "spuriousDragon"
    },
    BYZANTIUM {
        override val value = "byzantium"
    },
    CONSTANTINOPLE {
        override val value = "constantinople"
    },
    PETERSBURG {
        override val value = "petersburg"
    },
    ISTANBUL {
        override val value = "istanbul"
    },
    BERLIN {
        override val value = "berlin"
    };

    abstract val value: String
}