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
  `java-library`
  kotlin("jvm")
}

dependencies {
  api(kotlin("stdlib"))

  implementation("org.hyperledger.besu.internal:besu")
  implementation("org.hyperledger.besu.internal:core")
  implementation("org.hyperledger.besu.internal:config")
  implementation("org.hyperledger.besu.internal:metrics-core")
  implementation("org.hyperledger.besu.internal:rlp")
  implementation("org.hyperledger.besu.internal:kvstore")

  implementation("com.beust:klaxon")

  api("io.kotlintest:kotlintest-runner-junit5")
  api("org.hyperledger.besu.internal:testutil")
  api("org.koin:koin-test")
  api("io.mockk:mockk")
  api("com.splunk.logging:splunk-library-javalogging")
}
