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

package io.exflo.gradle.plugins.jooq

import org.gradle.api.tasks.SourceSet
import org.jooq.meta.jaxb.Configuration

class JooqConfiguration(
    val configName: String,
    val sourceSet: SourceSet
) {
    var databaseSources: List<Any> = emptyList()

    val taskName = "jooq-codegen-$configName"
    lateinit var configuration: Configuration

    override fun toString(): String {
        return "JooqConfiguration(configuration=$configuration, sourceSet=$sourceSet)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JooqConfiguration

        if (configName != other.configName) return false
        if (configuration != other.configuration) return false
        if (sourceSet != other.sourceSet) return false

        return true
    }

    override fun hashCode(): Int {
        var result = configName.hashCode()
        result = 31 * result + configuration.hashCode()
        result = 31 * result + sourceSet.hashCode()
        return result
    }
}