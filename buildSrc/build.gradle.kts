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

plugins {
    `kotlin-dsl`
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
}

apply(plugin = "io.spring.dependency-management")
apply(from = "${project.rootDir}/../gradle/versions.gradle")

repositories {
    mavenLocal()
    jcenter()
    maven(url = "https://dl.bintray.com/hyperledger-org/besu-repo/")
}

dependencies {
    implementation("org.redundent:kotlin-xml-builder")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")

    implementation("javax.activation:activation:1.1.1")
    implementation("javax.xml.bind:jaxb-api:2.3.0")
    implementation("com.sun.xml.bind:jaxb-core:2.3.0.1")
    runtimeOnly("com.sun.xml.bind:jaxb-impl:2.3.0.1")
}