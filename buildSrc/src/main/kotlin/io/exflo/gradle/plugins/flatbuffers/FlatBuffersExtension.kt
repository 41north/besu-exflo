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

package io.exflo.gradle.plugins.flatbuffers

import io.exflo.gradle.extensions.listProperty
import io.exflo.gradle.extensions.property
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Extension class for configuring the [FlatBuffersPlugin].
 */
@Suppress("UnstableApiUsage")
open class FlatBuffersExtension internal constructor(
    objectFactory: ObjectFactory
) {

    /**
     * The executable docker command to use.
     */
    val dockerCommand: Property<String> = objectFactory.property { set("docker") }

    /**
     * Extra docker options to add.
     */
    val extraDockerOptions: Property<String> = objectFactory.property { set("run --rm") }

    /**
     * The flatc docker image to use.
     */
    val flatcDockerImage: Property<String> = objectFactory.property { set("41north/flatbuffers:1.11.0") }

    /**
     * The flatc docker image to use.
     */
    val extraFlatcArgs: Property<String> = objectFactory.property { set("flatc") }

    /**
     * The language output on which the compiler will generate.
     */
    val language: Property<Language> = objectFactory.property { set(Language.JAVA) }

    /**
     * List of input sources to which flatc will compile.
     */
    val inputSources: ListProperty<String> = objectFactory.listProperty { set(emptyList<String>()) }
}