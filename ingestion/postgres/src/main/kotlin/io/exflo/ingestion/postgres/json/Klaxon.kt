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

package io.exflo.ingestion.postgres.json

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import com.beust.klaxon.Klaxon
import org.hyperledger.besu.ethereum.core.Log

object Klaxon {

    private val logConverter = object : Converter {
        override fun canConvert(cls: Class<*>): Boolean =
            cls == Log::class.java

        override fun fromJson(jv: JsonValue): Any? = {
        }

        override fun toJson(value: Any): String {
            var res = """{
                |"logger":"${(value as Log).logger.toHexString()}",
                |"data":"${value.data.toHexString()}",
            """.trimMargin()

            val log = value

            // Serialize topics
            if (log.topics.isNotEmpty()) {
                res += "\"topics\":["
                val topicsAsStrings = log.topics.map { topic -> "\"${topic.toHexString()}\"" }
                res += "${topicsAsStrings.joinToString(",")}]"
            } else {
                res += "\"topics\":[]"
            }

            res += "}"
            return res
        }
    }

    private val klaxon = Klaxon()
        .converter(logConverter)

    operator fun invoke(): Klaxon = klaxon
}
