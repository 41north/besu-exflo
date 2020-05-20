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

package io.exflo.domain.extensions

/**
 * Returns a list of triple built from the elements of `this` collection and the [one] and [two] lists with the same index.
 * The returned list has length of the shortest collection.
 */
fun <T, R, V> List<T>.zip(one: List<R>, two: List<V>): List<Triple<T, R, V>> =
  zip(one, two) { t1, t2, t3 -> Triple(t1, t2, t3) }

/**
 * Returns a list of values built from the elements of `this` collection, the [one] and [two] lists with the same index
 * using the provided [transform] function applied to each group of elements.
 * The returned list has length of the shortest collection.
 */
inline fun <T, R, P, V> List<T>.zip(one: List<R>, two: List<P>, transform: (a: T, b: R, c: P) -> V): List<V> {
  val arraySize = minOf(one.size, two.size)
  val list = ArrayList<V>(minOf(size, arraySize))
  var i = 0
  for (element in this) {
    if (i >= arraySize) break
    list.add(transform(element, one[i], two[i]))
    i++
  }
  return list
}
