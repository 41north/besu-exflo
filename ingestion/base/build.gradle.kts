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
import dev.north.fortyone.gradle.solidity.EVMVersion.ISTANBUL
import dev.north.fortyone.gradle.solidity.OutputComponent

plugins {
    `java-library`
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    id("org.jlleitschuh.gradle.ktlint")
    id("dev.north.fortyone.solidity") version "0.1.0"
}

dependencies {

    api(kotlin("stdlib"))
    api(kotlin("reflect"))

    api(project(":domain"))

    api("org.hyperledger.besu.internal:besu")
    api("org.hyperledger.besu.internal:config")
    api("org.hyperledger.besu.internal:metrics-core")
    api("org.hyperledger.besu.internal:plugins-rocksdb")
    api("org.hyperledger.besu.internal:kvstore")

    api("org.apache.tuweni:tuweni-bytes")
    api("org.apache.tuweni:tuweni-units")

    api("info.picocli:picocli")

    api("org.koin:koin-core")

    api("org.web3j:core")
    api("org.web3j:abi")
    api("org.web3j:utils")

    api("io.reactivex.rxjava3:rxjava")

    implementation("com.google.guava:guava")

    implementation("com.tinder.statemachine:statemachine")

    runtimeOnly("org.apache.logging.log4j:log4j-core")

    testApi(project(":testutil"))
}

ktlint {
    filter {
        exclude("**/tokens/detectors/**")
    }
}

solidity {
    dockerSolidityImage.set("ethereum/solc:0.5.13")
    evmVersion.set(ISTANBUL)
    outputComponents.set(listOf(OutputComponent.BIN_RUNTIME, OutputComponent.ABI))
}

val build: DefaultTask by tasks
build.dependsOn(tasks.shadowJar)

tasks {
    withType<ShadowJar> {
        archiveBaseName.set(project.name)
        archiveClassifier.set("")
        minimize()
    }

    withType<Jar> {
        enabled = false
    }

    withType<Test> {
        useJUnitPlatform()
    }
}
