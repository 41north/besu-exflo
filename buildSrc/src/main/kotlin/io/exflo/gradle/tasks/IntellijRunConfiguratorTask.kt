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

package io.exflo.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.redundent.kotlin.xml.PrintOptions
import org.redundent.kotlin.xml.xml
import java.io.File
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.security.InvalidParameterException

/**
 * Task that allows to generate XML Run configurations for IntelliJ.
 */
open class IntellijRunConfiguratorTask : DefaultTask() {

    private val jackson = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())

    @InputFile
    lateinit var tasksDefinitions: File

    init {
        group = "Intellij"
        description = "Generates XML Run configurations files for Intellij"
    }

    @TaskAction
    fun run() {
        val runConfigurationDir = File(".idea/runConfigurations")
        runConfigurationDir.mkdirs()

        val td = jackson.readValue(tasksDefinitions, TasksDefinitions::class.java)
            ?: throw InvalidParameterException("Task definition file not valid!")

        val configs: List<IntellijRunConfig> = td.application + td.docker + td.gradle

        configs
            .forEach { config ->
                val xml = config.toXml()
                File(runConfigurationDir, config.filename.replace("\\s", "_")).writeText(xml)
            }
    }
}

data class TasksDefinitions(
    val application: List<ApplicationIntellijRunConfig>,
    val docker: List<DockerIntellijRunConfig>,
    val gradle: List<GradleIntellijRunConfig>
)

interface IntellijRunConfig {
    val name: String
    val filename: String
    val default: Boolean

    fun toXml(): String
}

data class ApplicationIntellijRunConfig(
    override val name: String,
    override val filename: String,
    override val default: Boolean,
    private val envs: Map<String, String>,
    private val mainClassName: String,
    private val module: String
) : IntellijRunConfig {

    override fun toXml(): String =
        xml("component") {
            attribute("name", "ProjectRunConfigurationManager")

            "configuration" {

                attribute("default", default)
                attribute("name", name)
                attribute("type", "Application")
                attribute("factoryName", "Application")

                "envs" {

                    for (entry in envs) {
                        "env" {
                            attribute("name", entry.key)
                            attribute("value", entry.value)
                        }
                    }
                }

                "option" {
                    attribute("name", "INCLUDE_PROVIDED_SCOPE")
                    attribute("value", "true")
                }

                "option" {
                    attribute("name", "MAIN_CLASS_NAME")
                    attribute("value", mainClassName)
                }

                "option" {
                    attribute("name", "WORKING_DIRECTORY")
                    attribute("value", "\$MODULE_WORKING_DIR\$")
                }

                "module" {
                    attribute("name", module)
                }

                "extension" {
                    attribute("name", "net.ashald.envfile")

                    "option" {
                        attribute("name", "IS_ENABLED")
                        attribute("value", "false")
                    }

                    "option" {
                        attribute("name", "IS_SUBST")
                        attribute("value", "false")
                    }

                    "option" {
                        attribute("name", "IS_PATH_MACRO_SUPPORTED")
                        attribute("value", "false")
                    }

                    "option" {
                        attribute("name", "IS_IGNORE_MISSING_FILES")
                        attribute("value", "false")
                    }

                    "option" {
                        attribute("name", "IS_ENABLE_EXPERIMENTAL_INTEGRATIONS")
                        attribute("value", "false")
                    }

                    "ENTRIES" {
                        "ENTRY" {
                            attribute("IS_ENABLED", "true")
                            attribute("PARSER", "runconfig")
                        }
                    }
                }

                "method" {
                    attribute("v", "2")

                    "option" {
                        attribute("name", "Make")
                        attribute("enabled", "true")
                    }
                }
            }
        }.toString(PrintOptions(pretty = false, singleLineTextElements = true, useSelfClosingTags = true))
}

data class DockerIntellijRunConfig(
    override val name: String,
    override val filename: String,
    override val default: Boolean = false,
    private val dockerComposeFileName: String
) : IntellijRunConfig {

    override fun toXml(): String =
        xml("component") {
            attribute("name", "ProjectRunConfigurationManager")

            "configuration" {

                attribute("default", default)
                attribute("name", name)
                attribute("type", "docker-deploy")
                attribute("factoryName", "docker-compose.yml")
                attribute("server-name", "Docker")

                "deployment" {
                    attribute("type", "docker-compose.yml")

                    "settings" {

                        "option" {
                            attribute("name", "sourceFilePath")
                            attribute("value", dockerComposeFileName)
                        }
                    }
                }

                "method" {
                    attribute("v", "2")
                }
            }
        }.toString(PrintOptions(pretty = false, singleLineTextElements = true, useSelfClosingTags = true))
}

data class GradleIntellijRunConfig(
    override val name: String,
    override val filename: String,
    override val default: Boolean = false,
    val tasks: List<String> = emptyList()
) : IntellijRunConfig {

    override fun toXml(): String =
        xml("component") {
            attribute("name", "ProjectRunConfigurationManager")

            "configuration" {

                attribute("default", default)
                attribute("name", name)
                attribute("type", "GradleRunConfiguration")
                attribute("factoryName", "Gradle")

                "ExternalSystemSettings" {

                    "option" {
                        attribute("name", "executionName")
                    }

                    "option" {
                        attribute("name", "externalProjectPath")
                        attribute("value", "\$PROJECT_DIR\$")
                    }

                    "option" {
                        attribute("name", "externalSystemIdString")
                        attribute("value", "GRADLE")
                    }

                    "option" {
                        attribute("name", "scriptParameters")
                        attribute("value", "")
                    }

                    "option" {
                        attribute("name", "taskDescriptions")

                        "list" {}
                    }

                    "option" {
                        attribute("name", "taskNames")

                        "list" {

                            for (task in tasks) {
                                "option" {
                                    attribute("name", task)
                                }
                            }
                        }
                    }
                }

                "extension" {
                    attribute("name", "net.ashald.envfile")

                    "option" {
                        attribute("name", "IS_ENABLED")
                        attribute("value", "false")
                    }

                    "option" {
                        attribute("name", "IS_SUBST")
                        attribute("value", "false")
                    }

                    "option" {
                        attribute("name", "IS_PATH_MACRO_SUPPORTED")
                        attribute("value", "false")
                    }

                    "option" {
                        attribute("name", "IS_IGNORE_MISSING_FILES")
                        attribute("value", "false")
                    }

                    "option" {
                        attribute("name", "IS_ENABLE_EXPERIMENTAL_INTEGRATIONS")
                        attribute("value", "false")
                    }

                    "ENTRIES" {
                        "ENTRY" {
                            attribute("IS_ENABLED", "true")
                            attribute("PARSER", "runconfig")
                        }
                    }
                }

                "GradleScriptDebugEnabled" {
                    -"true"
                }

                "method" {
                    attribute("v", "2")

                    "option" {
                        attribute("name", "Make")
                        attribute("enabled", "true")
                    }
                }
            }
        }.toString(PrintOptions(pretty = false, singleLineTextElements = true, useSelfClosingTags = true))
}