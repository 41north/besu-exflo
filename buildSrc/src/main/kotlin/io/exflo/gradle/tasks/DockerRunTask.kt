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

import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

open class DockerRunTask : Exec() {

    @Input
    lateinit var image: String

    @Input
    var clean: Boolean = true

    @Input
    var uid: String? = null

    @Input
    var volumes: List<String> = emptyList()

    @Input
    var mounts: List<String> = emptyList()

    @Input
    var commands: List<String> = emptyList()

    @TaskAction
    override fun exec() {
        val args = mutableListOf("docker", "run")

        if (clean) {
            args.add("--rm")
        }

        if (uid != null) {
            args.add("--user=$uid")
        }

        if (volumes.isNotEmpty()) {
            volumes.forEach { volume ->
                args.add("-v")
                args.add(volume)
            }
        }

        if (mounts.isNotEmpty()) {
            mounts.forEach { mount ->
                args.add("--mount")
                args.add(mount)
            }
        }

        args.add(image)

        if (commands.isNotEmpty()) {
            args.addAll(commands)
        }

        commandLine = args.toList()

        super.exec()
    }
}