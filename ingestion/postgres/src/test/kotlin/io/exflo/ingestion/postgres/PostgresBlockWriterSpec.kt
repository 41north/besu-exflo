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

package io.exflo.ingestion.postgres

import com.fasterxml.jackson.databind.ObjectMapper
import io.exflo.ingestion.tracker.BlockReader
import io.exflo.postgres.jooq.Tables
import io.exflo.testutil.TestChainLoader
import io.kotlintest.Spec
import io.kotlintest.TestCase
import io.kotlintest.TestResult
import io.kotlintest.extensions.TopLevelTest
import io.kotlintest.specs.FunSpec
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject
import java.io.PrintWriter
import java.sql.Connection
import java.sql.SQLFeatureNotSupportedException
import java.util.logging.Logger
import javax.sql.DataSource
import kotlin.time.ExperimentalTime

@ExperimentalTime
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PostgresBlockWriterSpec : FunSpec(), KoinTest {

  private val mapper: ObjectMapper by inject()
  private val blockReader: BlockReader by inject()
  private val testChainLoader: TestChainLoader by inject()

  private val scope = TestCoroutineScope()

  override fun beforeSpecClass(spec: Spec, tests: List<TopLevelTest>) {
    startKoin { modules(KoinPostgresIngestionModules()) }

    // import test blocks
    testChainLoader.load()
  }

  override fun afterSpecClass(spec: Spec, results: Map<TestCase, TestResult>) {
    stopKoin()
  }

  init {

    context("with a PostgresBlockWriter entity") {

      test("it should convert and store correctly a batch of blocks") {
        val dataSource = MockDataSources.shouldConvertAndStoreABatchOfBlocks
        val blockReader = blockReader
        val cliOptions = ExfloPostgresCliOptions()

        val writer = PostgresBlockWriter(mapper, dataSource, blockReader, cliOptions)

        scope.runBlockingTest {
          writer.run()
        }
      }
    }
  }
}

object MockDataSources {

  val shouldConvertAndStoreABatchOfBlocks: DataSource by lazy {
    val mockDataProvider = MockDataProvider { ctx ->
      val create = DSL.using(SQLDialect.POSTGRES)

      val sql = ctx.sql()
      when {
        sql.startsWith("select") && sql.contains(Tables.BLOCK_HEADER.toString()) -> {
          arrayOf(MockResult(0, create.newResult(Tables.BLOCK_HEADER.NUMBER, Tables.BLOCK_HEADER.HASH)))
        }
        sql.startsWith("insert") || sql.startsWith("update") -> {
          // ignore updates to db
          arrayOf()
        }
        else -> throw IllegalStateException("SQL statement not mocked: $sql")
      }
    }

    MockDataSource(mockDataProvider)
  }

  private class MockDataSource(
    private val provider: MockDataProvider
  ) : DataSource {

    override fun setLogWriter(out: PrintWriter?): Unit = throw SQLFeatureNotSupportedException()

    override fun setLoginTimeout(seconds: Int): Unit = throw SQLFeatureNotSupportedException()

    override fun isWrapperFor(iface: Class<*>?): Boolean = throw SQLFeatureNotSupportedException()

    override fun <T : Any?> unwrap(iface: Class<T>?): T = throw SQLFeatureNotSupportedException()

    override fun getConnection(): Connection = MockConnection(provider)

    override fun getConnection(username: String?, password: String?): Connection =
      throw SQLFeatureNotSupportedException()

    override fun getParentLogger(): Logger = throw SQLFeatureNotSupportedException()

    override fun getLogWriter(): PrintWriter = throw SQLFeatureNotSupportedException()

    override fun getLoginTimeout(): Int = throw SQLFeatureNotSupportedException()
  }
}
