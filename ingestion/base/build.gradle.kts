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

import dev.north.fortyone.gradle.solidity.EVMVersion.ISTANBUL
import dev.north.fortyone.gradle.solidity.OutputComponent
import io.exflo.gradle.tasks.ClassOutput
import io.exflo.gradle.tasks.ClassVisibility
import io.exflo.gradle.tasks.Web3KtCodegenTask

plugins {
  `java-library`
  kotlin("jvm")
  id("dev.north.fortyone.solidity") version "0.1.1"
}

dependencies {

  api(kotlin("stdlib"))
  api(kotlin("reflect"))

  api(project(":domain"))

  api("org.hyperledger.besu.internal:besu")
  api("org.hyperledger.besu.internal:api")
  api("org.hyperledger.besu.internal:config")
  api("org.hyperledger.besu.internal:metrics-core")
  api("org.hyperledger.besu.internal:kvstore")

  api("org.apache.tuweni:tuweni-bytes")
  api("org.apache.tuweni:tuweni-units")

  api("info.picocli:picocli")

  api("org.koin:koin-core")

  api("org.web3j:core")
  api("org.web3j:abi")
  api("org.web3j:utils")

  api("io.reactivex.rxjava3:rxjava")

  api("org.jetbrains.kotlinx:kotlinx-coroutines-core")
  api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")

  implementation("com.fasterxml.jackson.core:jackson-databind")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

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
  attachToBuild.set(false)
  dockerSolidityImage.set("ethereum/solc:0.5.13")
  evmVersion.set(ISTANBUL)
  outputComponents.set(listOf(OutputComponent.BIN_RUNTIME, OutputComponent.ABI))
}

tasks {
  register<Web3KtCodegenTask>("generateContractWrappers") {
    dependsOn(project.tasks["compileSolidity"])

    group = "web3"

    solidityDir = "${project.buildDir.path}/resources/main/solidity"
    basePackageName = "${project.group}.ingestion.tokens.detectors"
    destinationDir = project.sourceSets.main.get().allJava.sourceDirectories.first { it.name.contains("kotlin") }.path

    contracts = listOf(
      ClassOutput("ERC20Detector", ClassVisibility.ABSTRACT),
      ClassOutput("ERC165Detector"),
      ClassOutput("ERC721Detector"),
      ClassOutput("ERC777Detector"),
      ClassOutput("ERC1155Detector")
    )
  }

  withType<Test> {
    useJUnitPlatform()
  }
}
