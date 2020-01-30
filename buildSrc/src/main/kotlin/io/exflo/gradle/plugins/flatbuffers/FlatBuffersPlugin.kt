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

import io.exflo.gradle.extensions.registerTask
import io.exflo.gradle.plugins.flatbuffers.tasks.FlatBuffersCompileTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.create
import java.io.File
import javax.inject.Inject

@Suppress("UnstableApiUsage", "MemberVisibilityCanBePrivate")
class FlatBuffersPlugin @Inject constructor(
    val objectFactory: ObjectFactory
) : Plugin<Project> {

    companion object {
        const val NAME = "flatbuffers"
    }

    override fun apply(target: Project) {
        target.pluginManager.apply(JavaPlugin::class.java)

        val extension: FlatBuffersExtension = target.extensions.create(NAME)

        target
            .convention
            .getPlugin(JavaPluginConvention::class.java)
            .sourceSets
            .forEach { sourceSet -> configure(target, extension, sourceSet) }
    }

    private fun configure(project: Project, extension: FlatBuffersExtension, sourceSet: SourceSet) {

        // Configure FlatBuffersSourceSets
        val srcSetName = if (sourceSet.name == "main") "" else sourceSet.name.capitalize()
        val flatBuffersSourceSet = DefaultFlatBuffersSourceSet(sourceSet.name.capitalize(), objectFactory)

        val src = File(project.projectDir, "src/${sourceSet.name}/${NAME}")
        val output = File(project.projectDir, "src/generated/${extension.language.get().value}/")

        flatBuffersSourceSet.flatbuffers.apply {
            srcDir(src)
            outputDir = output
        }

        // Add FlatBuffers source sets to parent
        sourceSet.allJava.source(flatBuffersSourceSet.flatbuffers)
        sourceSet.allSource.source(flatBuffersSourceSet.flatbuffers)

        // Include also generated source
        val generatedSrcDir =
            File(project.projectDir, "src/generated/${extension.language.get().value}/")
        val generatedSourceSet = sourceSet.java.srcDir(generatedSrcDir)
        sourceSet.allJava.source(generatedSourceSet)

        // Register compile task
        val compileTask = project.registerTask<FlatBuffersCompileTask>("compile${srcSetName}FlatBuffers") {
            description = "Compiles ${sourceSet.name} FlatBuffers source."

            dockerCommand.set(extension.dockerCommand)
            extraDockerOptions.set(extension.extraDockerOptions)
            flatcDockerImage.set(extension.flatcDockerImage)
            extraFlatcArgs.set(extension.extraFlatcArgs)
            inputSources.set(extension.inputSources)

            inputDir.set(src)
            outputDir.set(output)

            source = flatBuffersSourceSet.flatbuffers
            outputs.dir(flatBuffersSourceSet.flatbuffers.outputDir)
        }

        // Make buildFlatBuffers task dependant of generic build
        project.tasks.getByName("build").dependsOn(compileTask)
    }
}