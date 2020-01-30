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

package io.exflo.testutil

import java.io.IOException
import java.io.InputStream
import java.net.URISyntaxException
import java.net.URL
import java.nio.file.FileSystemNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import org.hyperledger.besu.testutil.BlockTestUtil
import org.hyperledger.besu.testutil.BlockTestUtil.ChainResources

object ExfloBlockTestUtil {

    private val exfloTestChainSupplier: ChainResources by lazy {
        supplyTestChainResources()
    }

    fun getTestChainResources(): ChainResources = exfloTestChainSupplier

    fun getTestReportAsInputStream(): InputStream =
        BlockTestUtil::class.java.getResource("/exflo/test-report.json").openStream()

    private fun supplyTestChainResources(): ChainResources {
        val genesisURL: URL = ensureFileUrl(BlockTestUtil::class.java.getResource("/exflo/testGenesis.json"))
        val blocksURL: URL = ensureFileUrl(BlockTestUtil::class.java.getResource("/exflo/test.blocks"))

        return ChainResources(genesisURL, blocksURL)
    }

    /** Take a resource URL and if needed copy it to a temp file and return that URL.  */
    private fun ensureFileUrl(resource: URL?): URL {
        checkNotNull(resource)
        try {
            try {
                Paths.get(resource.toURI())
            } catch (e: FileSystemNotFoundException) {
                val target: Path = Files.createTempFile("exflo", null)
                target.toFile().deleteOnExit()
                Files.copy(resource.openStream(), target, StandardCopyOption.REPLACE_EXISTING)
                return target.toUri().toURL()
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }
        return resource
    }
}
