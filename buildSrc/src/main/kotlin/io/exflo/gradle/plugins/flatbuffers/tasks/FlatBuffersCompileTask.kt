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

package io.exflo.gradle.plugins.flatbuffers.tasks

import com.sun.security.auth.module.UnixSystem
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import java.io.File
import javax.inject.Inject

/**
 * Task in charge of compiling actual FlatBuffer entities.
 */
@Suppress("UnstableApiUsage", "MemberVisibilityCanBePrivate")
open class FlatBuffersCompileTask @Inject constructor(
    objectFactory: ObjectFactory
) : SourceTask() {

    @get:Input
    val dockerCommand: Property<String> = objectFactory.property()

    @get:Input
    val extraDockerOptions: Property<String> = objectFactory.property()

    @get:Input
    val flatcDockerImage: Property<String> = objectFactory.property()

    @get:Input
    val extraFlatcArgs: Property<String> = objectFactory.property()

    @get:Input
    val inputSources: ListProperty<String> = objectFactory.listProperty()

    @get:Input
    val inputDir: Property<File> = objectFactory.property()

    @get:Input
    val outputDir: Property<File> = objectFactory.property()

    @TaskAction
    fun run() {

        source
            .filter { fbs -> inputSources.get().contains(fbs.name) }
            .forEach { _ ->

                // Extract extra docker arguments

                val options = mutableListOf<String>()

                val extraDockerExecutableOptions = extraDockerOptions.get().split(" ")
                options.addAll(extraDockerExecutableOptions)

                // Add uid
                options.add("--user=${UnixSystem().uid}")

                // Add docker volumes
                val volumes = "-v ${inputDir.get().absolutePath}:/input -v ${outputDir.get().absolutePath}:/output"
                options.addAll(volumes.split(" "))

                // Add docker image
                options.add(flatcDockerImage.get())

                // Prepare extra flatc arguments
                options.addAll(extraFlatcArgs.get().split(" "))

                logger.debug("Executable: ${dockerCommand.get()}")
                logger.debug("Args: $options")

                project.exec {
                    executable = dockerCommand.get()
                    args = options
                }
            }
    }
}