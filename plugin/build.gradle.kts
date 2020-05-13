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
  `maven-publish`
  id("com.github.johnrengelman.shadow")
}

dependencies {
  implementation(project(":ingestion:kafka"))
  implementation(project(":ingestion:postgres"))
}

val build: DefaultTask by project.tasks
build.dependsOn(tasks.shadowJar)

tasks {
  withType<ShadowJar> {
    archiveBaseName.set(project.name)
    archiveClassifier.set("")
  }
}
