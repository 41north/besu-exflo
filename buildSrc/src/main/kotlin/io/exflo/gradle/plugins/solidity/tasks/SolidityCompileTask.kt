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

package io.exflo.gradle.plugins.solidity.tasks

import io.exflo.gradle.plugins.solidity.EVMVersion
import io.exflo.gradle.plugins.solidity.OutputComponent
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

/**
 * Task in charge of compiling actual Solidity code.
 */
@Suppress("UnstableApiUsage", "MemberVisibilityCanBePrivate")
open class SolidityCompileTask @Inject constructor(
    objectFactory: ObjectFactory
) : SourceTask() {

    @get:Input
    val command: Property<String> = objectFactory.property()

    @get:Input
    val solidityImage: Property<String> = objectFactory.property()

    @get:Input
    val overwrite: Property<Boolean> = objectFactory.property()

    @get:Input
    val optimize: Property<Boolean> = objectFactory.property()

    @get:Input
    val optimizeRuns: Property<Int> = objectFactory.property()

    @get:Input
    val prettyJson: Property<Boolean> = objectFactory.property()

    @get:Input
    val ignoreMissing: Property<Boolean> = objectFactory.property()

    @get:Input
    val allowPaths: ListProperty<String> = objectFactory.listProperty()

    @get:Input
    val evmVersion: Property<EVMVersion> = objectFactory.property()

    @get:Input
    val outputComponents: ListProperty<OutputComponent> = objectFactory.listProperty()

    @TaskAction
    fun compileSolidity() {
        source.forEach { contract ->

            val options = mutableListOf<String>()

            // Prepare main command
            val executableParts = command.get().split(" ")

            val dockerCommand = executableParts.first()
            val extraDockerExecutableOptions = executableParts.drop(1)

            options.addAll(extraDockerExecutableOptions)
            options.addAll("-v ${project.projectDir}/src:/src".split(" "))
            options.addAll("-v ${project.buildDir}:/build".split(" "))
            options.add(solidityImage.get())

            // Prepare solc options

            outputComponents.get().forEach { output -> options.add("--$output") }

            optimize.get().let {
                options.add("--optimize")
                optimizeRuns.get()
                    .takeIf { it > 0 }
                    ?.let { runs ->
                        options.add("--optimize-runs")
                        options.add(runs.toString())
                    }
            }

            overwrite.get().takeIf { it }?.let { options.add("--overwrite") }

            prettyJson.get().takeIf { it }?.let { options.add("--pretty-json") }

            ignoreMissing.get().takeIf { it }?.let { options.add("--ignore-missing") }

            allowPaths.get()
                .takeIf { it.isNotEmpty() }
                ?.let { paths ->
                    options.add("--allow-paths")
                    options.add(paths.joinToString(separator = ",") { it })
                }

            evmVersion.get().let {
                options.add("--evm-version")
                options.add(it.value)
            }

            options.add("--output-dir")
            options.add("/${outputs.files.singleFile.toRelativeString(project.projectDir)}")

            options.add("/${contract.toRelativeString(project.projectDir)}")

            project.exec {
                executable = dockerCommand
                args = options
            }
        }
    }
}