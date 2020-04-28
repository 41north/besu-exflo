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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    kotlin("jvm")
    `maven-publish`
    id("com.github.johnrengelman.shadow")
    id("org.jlleitschuh.gradle.ktlint")
}

dependencies {

    implementation(project(":ingestion:base"))

    implementation("io.kcache:kcache")
    implementation("org.apache.kafka:kafka-clients")

    runtimeOnly("org.apache.logging.log4j:log4j-core")

    testImplementation(project(":testutil"))
    testImplementation("org.springframework.kafka:spring-kafka-test")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {

            groupId = "${rootProject.group}"
            version = "${project.version}"

            project.shadow.component(this)

            pom {
                name.set("Exflo - ${project.name}")
                url.set("https://github.com/41North/exflo")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/41North/exflo.git")
                    developerConnection.set("scm:git:ssh://github.com/41North/exflo.git")
                    url.set("https://github.com/41North/exflo")
                }
            }
        }
    }
}

val build: DefaultTask by project.tasks
build.dependsOn(tasks.shadowJar)

tasks {
    withType<ShadowJar> {
        archiveBaseName.set(project.name)
        archiveClassifier.set("")
        minimize()
    }

    withType<Test> {
        useJUnitPlatform()
    }
}
