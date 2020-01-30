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

package io.exflo.gradle.plugins.solidity

import io.exflo.gradle.extensions.listProperty
import io.exflo.gradle.extensions.property
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Extension class for configuring the [SolidityPlugin].
 */
@Suppress("UnstableApiUsage")
open class SolidityExtension internal constructor(
    objectFactory: ObjectFactory
) {

    /**
     * The executable command to use.
     */
    val command: Property<String> = objectFactory.property { set("docker run --rm") }

    /**
     * The solidity docker image to use.
     */
    val solidityImage: Property<String> = objectFactory.property { set("ethereum/solc:stable") }

    /**
     * Allows to overwrite compiled files.
     */
    val overwrite: Property<Boolean> = objectFactory.property { set(true) }

    /**
     * Enables the optimizer to optimize the code n number of times defined by [optimizeRuns] parameter.
     */
    val optimize: Property<Boolean> = objectFactory.property { set(true) }

    /**
     * Optimize for how many times you intend to run the code.
     * Lower values will optimize more for initial deployment cost, higher values will optimize more for high-frequency usage.
     */
    val optimizeRuns: Property<Int> = objectFactory.property { set(0) }

    /**
     * Enables JSON prettifier.
     */
    val prettyJson: Property<Boolean> = objectFactory.property { set(false) }

    val ignoreMissing: Property<Boolean> = objectFactory.property { set(false) }

    /**
     * The compiler has restrictions what directories it can access.
     * Paths (and their subdirectories) of source files specified on the commandline and paths defined by remappings are allowed for import statements,
     * but everything else is rejected. Additional paths (and their subdirectories) can be allowed by this property.
     */
    val allowPaths: ListProperty<String> = objectFactory.listProperty { set(emptyList()) }

    /**
     * Compile your contract for a particular EVM version in order to enable new features or behaviours
     */
    val evmVersion: Property<EVMVersion> = objectFactory.property { set(EVMVersion.ISTANBUL) }

    /**
     * The outputs that the compilation will produce.
     * For a complete reference see: https://solidity.readthedocs.io/en/v0.4.24/using-the-compiler.html#compiler-input-and-output-json-description
     */
    val outputComponents: ListProperty<OutputComponent> = objectFactory.listProperty { set(listOf(OutputComponent.BIN, OutputComponent.ABI)) }
}