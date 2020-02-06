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

package io.exflo.gradle.plugins.jooq.tasks

import io.exflo.gradle.plugins.jooq.JooqConfiguration
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecResult
import org.gradle.process.JavaExecSpec
import org.jooq.Constants
import org.jooq.codegen.GenerationTool
import org.jooq.meta.jaxb.Configuration
import java.io.File
import javax.xml.XMLConstants
import javax.xml.bind.JAXBContext
import javax.xml.validation.SchemaFactory

open class JooqCodeGenerationTask : DefaultTask() {

    @Internal
    lateinit var jooqConfiguration: JooqConfiguration

    @Classpath
    @InputFiles
    lateinit var taskClasspath: FileCollection

    @Internal
    var javaExecAction: Action<in JavaExecSpec>? = null

    @Internal
    var execResultHandler: Action<in ExecResult>? = null

    @get:Input
    lateinit var databaseSourceLocations: List<Any>

    @TaskAction
    fun generateSources() {
        val configFile = File(project.buildDir, "tmp/jooq/config-${jooqConfiguration.configName}.xml")
        writeConfigFile(configFile)

        val execResult = executeJooq(configFile)
        execResultHandler?.execute(execResult)
    }

    fun configureAdditionalInputs() {
        databaseSourceLocations = jooqConfiguration.databaseSources
        jooqConfiguration.databaseSources.forEach { inputs.dir(it) }
    }

    @Input
    fun getConfigHash(): Int = jooqConfiguration.configuration.hashCode()

    @OutputDirectory
    fun getOutputDirectory(): File =
        project.file(jooqConfiguration.configuration.generator.target.directory)

    private fun writeConfigFile(file: File) {
        val schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
            .newSchema(GenerationTool::class.java.getResource("/xsd/${Constants.XSD_CODEGEN}"))

        val marshaller = JAXBContext.newInstance(Configuration::class.java).let {
            it.createMarshaller().apply { setSchema(schema) }
        }

        file.parentFile.mkdirs()
        marshaller.marshal(jooqConfiguration.configuration, file)
    }

    private fun executeJooq(file: File): ExecResult =
        project.javaexec {
            println(taskClasspath.files.map { it.name })

            main = GenerationTool::class.qualifiedName
            classpath = taskClasspath
            args = listOf(file.absolutePath)

            javaExecAction?.execute(this)
        }
}