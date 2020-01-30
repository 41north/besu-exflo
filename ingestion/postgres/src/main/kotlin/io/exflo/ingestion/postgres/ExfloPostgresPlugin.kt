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

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.exflo.ingestion.ExfloCliDefaultOptions
import io.exflo.ingestion.ExfloCliOptions
import io.exflo.ingestion.ExfloCliOptions.ProcessableEntity
import io.exflo.ingestion.ExfloCliOptions.ProcessableEntity.RECEIPTS
import io.exflo.ingestion.ExfloPlugin
import io.exflo.ingestion.tracker.BlockWriter
import io.exflo.postgres.jooq.Tables.METADATA
import io.exflo.postgres.jooq.tables.records.MetadataRecord
import javax.sql.DataSource
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.hyperledger.besu.cli.config.EthNetworkConfig
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.module
import picocli.CommandLine

class ExfloPostgresPlugin : ExfloPlugin<ExfloPostgresCliOptions>() {

    override val name = ExfloCliDefaultOptions.EXFLO_POSTGRES_PLUGIN_ID

    override val options = ExfloPostgresCliOptions()

    override fun implKoinModules(): List<Module> = listOf(
        module {
            single { options }
            single<ExfloCliOptions> { options }

            single<DataSource> {
                val dataSourceConfig = HikariConfig()
                    .apply {
                        jdbcUrl = options.jdbcUrl
                        isAutoCommit = false
                        maximumPoolSize = 30
                        addDataSourceProperty("cachePrepStmts", "true")
                        addDataSourceProperty("prepStmtCacheSize", "250")
                        addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
                    }

                HikariDataSource(dataSourceConfig)
            }

            single<BlockWriter> {
                PostgresBlockWriter(get(), get(), options)
            }
        }
    )

    override fun implStart(koinApp: KoinApplication) {

        val koin = koinApp.koin

        val dataSource = koin.get<DataSource>()
        val netConfig = koin.get<EthNetworkConfig>()

        options.disableMigrations
            .takeIf { !it }
            ?.run { migrateDatabase(dataSource) }

        checkDatabaseNetworkId(dataSource, netConfig)
    }

    private fun migrateDatabase(dataSource: DataSource) {

        val config = FluentConfiguration()
            .dataSource(dataSource)
            .locations("classpath:/db/migration")

        Flyway(config).migrate()
    }

    private fun checkDatabaseNetworkId(dataSource: DataSource, config: EthNetworkConfig) {

        val dbCtx = DSL.using(dataSource, SQLDialect.POSTGRES)

        dbCtx.transaction { txConfig ->

            val txCtx = DSL.using(txConfig)

            val networkKey = "network_id"

            val dbNetworkId = txCtx
                .select(METADATA.VALUE)
                .from(METADATA)
                .where(METADATA.KEY.eq(networkKey))
                .fetchOne()
                ?.value1()
                ?.toBigInteger()

            when (dbNetworkId) {
                // do nothing, db matches the configured network
                config.networkId -> {
                }
                // first run, we need to set the network id in the database
                null -> {
                    MetadataRecord()
                        .apply {
                            key = networkKey
                            value = config.networkId.toString()
                            txCtx.executeInsert(this)
                        }
                }
                else -> throw IllegalStateException(
                    "Configured network id '${config.networkId}' does not match database network id '$dbNetworkId"
                )
            }
        }
    }
}

class ExfloPostgresCliOptions : ExfloCliOptions {

    @CommandLine.Option(
        names = ["--plugin-${ExfloCliDefaultOptions.EXFLO_POSTGRES_PLUGIN_ID}-start-block-override"],
        paramLabel = "<LONG>",
        description = ["Block number from which to start publishing"]
    )
    override var startBlockOverride: Long? = null

    @CommandLine.Option(
        names = ["--plugin-${ExfloCliDefaultOptions.EXFLO_POSTGRES_PLUGIN_ID}-max-fork-size"],
        paramLabel = "<INTEGER>",
        defaultValue = "${ExfloCliDefaultOptions.MAX_FORK_SIZE}",
        description = ["Max no. of blocks that a fork can be comprised of. Used for resetting chain tracker's tail on restart"]
    )
    override var maxForkSize: Int = ExfloCliDefaultOptions.MAX_FORK_SIZE

    @CommandLine.Option(
        names = ["--plugin-${ExfloCliDefaultOptions.EXFLO_POSTGRES_PLUGIN_ID}-processing-level"],
        paramLabel = "<ENTITY>",
        description = ["Level of which this plugin will process entities. Each one relies on the previous one"]
    )
    var processableEntity: ProcessableEntity = RECEIPTS

    @CommandLine.Option(
        names = ["--plugin-${ExfloCliDefaultOptions.EXFLO_POSTGRES_PLUGIN_ID}-jdbc-url"],
        defaultValue = "jdbc:postgresql://localhost/exflo_dev?user=exflo_dev&password=exflo_dev",
        paramLabel = "<STRING>",
        description = ["JDBC connection url for postgres database"]
    )
    var jdbcUrl: String? = null

    @CommandLine.Option(
        names = ["--plugin-${ExfloCliDefaultOptions.EXFLO_POSTGRES_PLUGIN_ID}-ignore-migrations-check"],
        paramLabel = "<BOOLEAN>",
        description = ["Enables or disables checking migrations on the selected DB"]
    )
    var disableMigrations: Boolean = false
}
