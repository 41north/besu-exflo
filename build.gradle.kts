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

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import io.exflo.gradle.tasks.IntellijRunConfiguratorTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `maven-publish`
    distribution
    id("org.jetbrains.kotlin.jvm") version "1.3.61"
    id("org.jlleitschuh.gradle.ktlint") version "9.1.1" apply false
    id("org.jlleitschuh.gradle.ktlint-idea") version "9.1.1" apply true
    id("com.github.johnrengelman.shadow") version "5.2.0" apply true
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
    id("com.github.ben-manes.versions") version "0.27.0"
}

if (!JavaVersion.current().isJava11Compatible) {
    throw GradleException("Java 11 or later is required to build Exflo. Detected version ${JavaVersion.current()}")
}

val distZip: Zip by project.tasks
distZip.apply {
    dependsOn(":ingestion:kafka:build", ":ingestion:postgres:build")
    doFirst { delete { fileTree(Pair("build/distributions", "*.zip")) } }
}

val distTar: Tar by project.tasks
distTar.apply {
    dependsOn(":ingestion:kafka:build", ":ingestion:postgres:build")
    doFirst { delete { fileTree(Pair("build/distributions", "*.tar.gz")) } }
    compression = Compression.GZIP
    archiveExtension.set("tar.gz")
}

allprojects {
    apply(plugin = "io.spring.dependency-management")
    apply(from = "${rootDir}/gradle/versions.gradle")

    group = "io.exflo"
    version = rootProject.version

    repositories {
        mavenLocal()
        jcenter()
        maven(url = "https://jitpack.io")
        maven(url = "https://packages.confluent.io/maven/")
        maven(url = "https://oss.sonatype.org/content/repositories/releases/")
        maven(url = "https://dl.bintray.com/ethereum/maven/")
        maven(url = "https://dl.bintray.com/hyperledger-org/besu-repo/")
        maven(url = "https://dl.bintray.com/consensys/pegasys-repo/")
        maven(url = "https://dl.bintray.com/tuweni/tuweni/")
    }

    tasks.withType<KotlinCompile>().all {
        sourceCompatibility = "${JavaVersion.VERSION_11}"
        targetCompatibility = "${JavaVersion.VERSION_11}"
        kotlinOptions.jvmTarget = "${JavaVersion.VERSION_11}"
    }

    tasks.withType<JavaCompile> {
        sourceCompatibility = "${JavaVersion.VERSION_11}"
        targetCompatibility = "${JavaVersion.VERSION_11}"
    }
}

distributions {
    main {
        contents {
            from("LICENSE") { into("") }
            from("README.md") { into("") }
            from("CHANGELOG.md") { into("") }
            from("ingestion/kafka/build/libs") { into("plugins") }
            from("ingestion/postgres/build/libs") { into("plugins") }
        }
    }
}

ktlint {
    debug.set(false)
    verbose.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(true)
    filter {
        exclude("**/generated/**")
        exclude("**/flatbuffers/**")
    }
}

tasks.jar {
    enabled = false
}

tasks.register<IntellijRunConfiguratorTask>("generateIntellijRunConfigs") {
    tasksDefinitions = File("intellij/run-configs.yaml")
}

tasks.withType<DependencyUpdatesTask> {
    fun isNonStable(version: String): Boolean {
        val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
        val regex = "^[0-9,.v-]+(-r)?$".toRegex()
        val isStable = stableKeyword || regex.matches(version)
        return isStable.not()
    }

    // Reject all non stable versions
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}
