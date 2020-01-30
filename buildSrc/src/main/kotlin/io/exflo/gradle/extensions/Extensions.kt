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

@file:Suppress("UnstableApiUsage")

package io.exflo.gradle.extensions

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskProvider

inline fun <reified T> ObjectFactory.property(configuration: Property<T>.() -> Unit = {}) =
    property(T::class.java).apply(configuration)

inline fun <reified T> ObjectFactory.listProperty(configuration: ListProperty<T>.() -> Unit = {}) =
    listProperty(T::class.java).apply(configuration)

inline fun <reified T : Task> Project.registerTask(
    name: String,
    noinline configuration: T.() -> Unit
): TaskProvider<T> = this.tasks.register(name, T::class.java, configuration)
