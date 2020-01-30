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

const HEX_CHARACTERS = 'abcdef0123456789'

const randomAddress = () => {
  let result = '0x'
  for (let i = 0; i < 40; i++) {
    const idx = Math.floor(Math.random() * HEX_CHARACTERS.length)
    result += HEX_CHARACTERS.charAt(idx)
  }
  return result
}

const randomValue = () => Math.floor(Math.random() * 10000000000000)

module.exports = {
  randomAddress,
  randomValue,
}
