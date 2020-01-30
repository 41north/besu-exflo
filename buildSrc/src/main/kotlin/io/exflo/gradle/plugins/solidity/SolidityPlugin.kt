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

import io.exflo.gradle.extensions.registerTask
import io.exflo.gradle.plugins.solidity.tasks.SolidityCompileTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.create
import java.io.File
import javax.inject.Inject

/**
 * Gradle plugin for Solidity compile automation.
 *
 * Inspired by the work done in https://github.com/web3j/solidity-gradle-plugin/
 */
@Suppress("UnstableApiUsage", "MemberVisibilityCanBePrivate")
open class SolidityPlugin @Inject constructor(
    val objectFactory: ObjectFactory
) : Plugin<Project> {

    companion object {
        const val NAME = "solidity"
    }

    private lateinit var extension: SolidityExtension

    override fun apply(target: Project) {
        target.pluginManager.apply(JavaPlugin::class.java)

        this.extension = target.extensions.create(NAME)

        target
            .convention
            .getPlugin(JavaPluginConvention::class.java)
            .sourceSets
            .forEach { sourceSet -> configureTask(target, sourceSet) }
    }

    /**
     * Configures code compilation tasks for the Solidity source sets defined in the project
     * (e.g. main, test).
     *
     * By default the generated task name for the <code>main</code> source set
     * is <code>compileSolidity</code> and for <code>test</code>
     * <code>compileTestSolidity</code>.
     */
    private fun configureTask(project: Project, sourceSet: SourceSet) {

        // Configure first SoliditySourceSets
        val srcSetName = if (sourceSet.name == "main") "" else sourceSet.name.capitalize()
        val soliditySourceSet = DefaultSoliditySourceSet(sourceSet.name.capitalize(), objectFactory)

        soliditySourceSet.solidity.apply {
            val defaultSrcDir = File(project.projectDir, "src/${sourceSet.name}/${SoliditySourceSet.NAME}")
            val defaultOutputDir = File(project.buildDir, "resources/${sourceSet.name}/${SoliditySourceSet.NAME}")

            srcDir(defaultSrcDir)
            outputDir = defaultOutputDir
        }

        sourceSet.allJava.source(soliditySourceSet.solidity)
        sourceSet.allSource.source(soliditySourceSet.solidity)

        // Configure lazily tasks
        val compileTask = project.registerTask<SolidityCompileTask>("compile${srcSetName}Solidity") {
            description = "Compiles ${sourceSet.name} Solidity source."

            command.set(extension.command)
            solidityImage.set(extension.solidityImage)
            overwrite.set(extension.overwrite)
            optimize.set(extension.optimize)
            optimizeRuns.set(extension.optimizeRuns)
            prettyJson.set(extension.prettyJson)
            ignoreMissing.set(extension.ignoreMissing)
            allowPaths.set(extension.allowPaths)
            evmVersion.set(extension.evmVersion)
            outputComponents.set(extension.outputComponents)

            source = soliditySourceSet.solidity
            outputs.dir(soliditySourceSet.solidity.outputDir)
        }

        // Make buildSolidity task dependant of generic build
        project.tasks.getByName("build").dependsOn(compileTask)
    }
}