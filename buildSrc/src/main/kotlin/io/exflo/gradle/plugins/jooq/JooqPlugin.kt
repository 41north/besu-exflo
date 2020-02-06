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

import io.exflo.gradle.extensions.registerTask
import io.exflo.gradle.plugins.jooq.tasks.JooqCodeGenerationTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.SourceSet

/**
 * Code mostly based on: https://github.com/rohanprabhu/kotlin-dsl-gradle-jooq-plugin
 *
 * Several minor differences:
 *  - Tasks are configured lazily
 *  - Allows to enable / disable attachment to compileKotlin task (this allows manual execution of the task)
 *  - Removed unused classes and methods
 */
open class JooqPlugin : Plugin<Project> {

    private lateinit var project: Project
    private lateinit var jooqGeneratorRuntime: Configuration
    private lateinit var extension: JooqPluginExtension

    companion object {
        const val ExtensionName = "jooqGenerator"
        const val BuildPhaseGradleConfigurationName = "jooqGeneratorRuntime"
    }

    override fun apply(project: Project) {
        this.project = project

        bootstrap()

        extension = project.extensions.create(
            ExtensionName, JooqPluginExtension::class.java,
            this.project,
            jooqGeneratorRuntime
        )

        manageJooqEditionAndVersion()
    }

    private fun bootstrap() {
        project.plugins.apply(JavaBasePlugin::class.java)
        addBuildPhaseConfiguration()
    }

    private fun addBuildPhaseConfiguration() {
        jooqGeneratorRuntime = project.configurations.create(BuildPhaseGradleConfigurationName)

        jooqGeneratorRuntime.description =
            "The classpath used to run the jooq generator. Your JDBC classes, generator extensions etc.," +
                "are to be added in this configuration to keep them separate from your build"

        jooqGeneratorRuntime.let {
            project.dependencies.add(it.name, "org.jooq:jooq-codegen")
        }
    }

    private fun manageJooqEditionAndVersion() {
        val groupIds = JooqEdition.values().map { it.editionArtifactGroup }.toSet()

        project.configurations.all {
            resolutionStrategy.eachDependency {
                if (groupIds.contains(requested.group) && requested.name.startsWith("jooq")) {
                    useTarget("${extension.jooqEdition.editionArtifactGroup}:${requested.name}:${extension.jooqVersion}")
                }
            }
        }
    }
}

open class JooqPluginExtension(
    private val project: Project,
    private val jooqGeneratorRuntime: Configuration
) {
    companion object {
        const val DefaultJooqVersion = "3.12.3"
    }

    var jooqEdition: JooqEdition = JooqEdition.OpenSource
    var jooqVersion: String = DefaultJooqVersion
    var attachToCompileJava = true

    fun configuration(name: String, sourceSet: SourceSet, configure: JooqConfiguration.() -> Unit) {
        val configuration = JooqConfiguration(name, sourceSet).apply(configure)

        project.registerTask<JooqCodeGenerationTask>(configuration.taskName) {
            description = "Generate jooq sources for config $name"
            group = "jooq-codegen"
            jooqConfiguration = configuration
            taskClasspath = jooqGeneratorRuntime

            cleanGeneratedSources(project, this)
            configureSourceSet(project, this@JooqPluginExtension, configuration)

            configureAdditionalInputs()
        }
    }

    private fun cleanGeneratedSources(project: Project, task: Task) {
        val cleanJooqSourcesTaskName = "clean" + task.name.capitalize()
        project.tasks.getByName(BasePlugin.CLEAN_TASK_NAME).dependsOn(cleanJooqSourcesTaskName)
        task.mustRunAfter(cleanJooqSourcesTaskName)
    }

    private fun configureSourceSet(
        project: Project,
        extension: JooqPluginExtension,
        jooqConfiguration: JooqConfiguration
    ) {
        val sourceSet = jooqConfiguration.sourceSet

        if (extension.attachToCompileJava) {
            sourceSet.java.srcDir(jooqConfiguration.configuration.generator.target.directory)
            project.tasks.getByName(sourceSet.compileJavaTaskName).dependsOn(jooqConfiguration.taskName)
            project.tasks.findByName("compileKotlin")?.dependsOn(jooqConfiguration.taskName)
        }
    }
}