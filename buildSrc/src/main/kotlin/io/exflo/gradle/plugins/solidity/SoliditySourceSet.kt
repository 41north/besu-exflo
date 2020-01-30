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

import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.reflect.HasPublicType
import org.gradle.api.reflect.TypeOf

/**
 * Source set for Solidity classes in a Gradle project.
 */
interface SoliditySourceSet {

    companion object {
        const val NAME = "solidity"
    }

    /**
     * Returns the source to be compiled by the Solidity compiler for this source set.
     *
     * @return The Solidity source. Never returns null.
     */
    val solidity: SourceDirectorySet
}

@Suppress("UnstableApiUsage")
internal class DefaultSoliditySourceSet(
    displayName: String,
    objectFactory: ObjectFactory
) : SoliditySourceSet, HasPublicType {

    override val solidity: SourceDirectorySet =
        objectFactory
            .sourceDirectorySet(
                SoliditySourceSet.NAME,
                "$displayName Solidity Sources"
            )
            .apply { filter.include("**/*.sol") }

    override fun getPublicType(): TypeOf<*> = TypeOf.typeOf(SoliditySourceSet::class.java)
}