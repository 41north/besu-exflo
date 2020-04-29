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

package io.exflo.ingestion.extensions

fun <T : Any> assertNotNull(
    actual: T?,
    message: String = "null found"
): T {
    assert(actual != null) { message }
    return actual!!
}

inline fun <reified T : Any> reflektField(entity: Any, fieldName: String): T {
    val field = assertNotNull(entity::class.java.declaredFields.find { it.name == fieldName })
    if (!field.isAccessible) field.isAccessible = true
    return field.get(entity) as T
}

inline fun <reified T : Any> reflektMethod(entity: Any, methodName: String, vararg args: Any?): T {
    val method = assertNotNull(entity::class.java.declaredMethods.find { it.name == methodName })
    if (!method.isAccessible) method.isAccessible = true
    return method.invoke(entity, args) as T
}

inline fun <reified T : Any> reflektMethod(entity: Any? = null, clazz: Class<*>, methodName: String, vararg args: Any?): T {
    val method = assertNotNull(clazz.declaredMethods.find { it.name == methodName })
    if (!method.isAccessible) method.isAccessible = true
    return method.invoke(entity, args) as T
}
