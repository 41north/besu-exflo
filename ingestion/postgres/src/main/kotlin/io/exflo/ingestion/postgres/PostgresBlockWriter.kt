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
import io.exflo.ingestion.ExfloCliOptions.ProcessingLevel.BODY
import io.exflo.ingestion.ExfloCliOptions.ProcessingLevel.RECEIPTS
import io.exflo.ingestion.ExfloCliOptions.ProcessingLevel.TRACES
import io.exflo.ingestion.extensions.format
import io.exflo.ingestion.extensions.toBalanceDeltas
import io.exflo.ingestion.postgres.extensions.toAccountRecord
import io.exflo.ingestion.postgres.extensions.toBalanceDeltaRecord
import io.exflo.ingestion.postgres.extensions.toBlockHeaderRecord
import io.exflo.ingestion.postgres.extensions.toContractCreatedRecord
import io.exflo.ingestion.postgres.extensions.toContractDestroyedRecord
import io.exflo.ingestion.postgres.extensions.toEventRecords
import io.exflo.ingestion.postgres.extensions.toOmmerRecord
import io.exflo.ingestion.postgres.extensions.toTransactionReceiptRecord
import io.exflo.ingestion.postgres.extensions.toTransactionRecord
import io.exflo.ingestion.tracker.BlockReader
import io.exflo.ingestion.tracker.BlockWriter
import io.exflo.postgres.jooq.Tables
import io.exflo.postgres.jooq.tables.records.BlockHeaderRecord
import io.exflo.postgres.jooq.tables.records.BlockTraceRecord
import io.exflo.postgres.jooq.tables.records.OmmerRecord
import io.exflo.postgres.jooq.tables.records.TransactionRecord
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.logging.log4j.LogManager
import org.hyperledger.besu.ethereum.core.Address
import org.hyperledger.besu.ethereum.core.BlockHeader
import org.hyperledger.besu.ethereum.core.Hash
import org.jooq.DSLContext
import org.jooq.JSONB
import org.jooq.SQLDialect
import org.jooq.TableRecord
import org.jooq.impl.DSL
import javax.sql.DataSource
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlin.time.measureTimedValue
import kotlin.time.seconds

@ExperimentalTime
class PostgresBlockWriter(
  private val objectMapper: ObjectMapper,
  private val dataSource: DataSource,
  private val blockReader: BlockReader,
  cliOptions: ExfloPostgresCliOptions
) : BlockWriter {

  private val dbContext = DSL.using(dataSource, SQLDialect.POSTGRES)

  private val pollInterval = cliOptions.pollInterval?.seconds ?: 1.seconds

  private val processingLevel = cliOptions.processingLevel

  private val startBlockNumber = cliOptions.earliestBlockNumber ?: BlockHeader.GENESIS_BLOCK_NUMBER

  private val log = LogManager.getLogger()

  private val batchSize = 512

  private val chunkSize = 64

  override suspend fun run() =
    coroutineScope {

      // first ensure we have the genesis block

      tryImportStartBlock()

      // start import loop

      while (isActive) {

        val startNumber = requireNotNull(latestHeaderInDb(dbContext)) { "We should already have the genesis block" }
          .number + 1L

        val range = startNumber.until(startNumber + batchSize)

        val importCount = measureTimedValue { tryImport(range) }

        val importRate = (importCount.value / importCount.duration.inSeconds).format(2)

        log.info("Import rate = $importRate: ${importCount.value} blocks in ${importCount.duration}")

        if (isActive && importCount.value <= 1) {
          delay(pollInterval)
        }
      }
    }

  private suspend fun tryImportStartBlock() {
    if (latestHeaderInDb() == null) {
      require(tryImport(LongRange(startBlockNumber, startBlockNumber)) == 1) { "Failed to import start block" }
    }
  }

  private fun handleFork() {
    dbContext.transaction { txConfig ->
      val ctx = DSL.using(txConfig)

      val forkHeader = requireNotNull(findForkHeader(ctx)) { "Fork header not found" }

      // delete everything from the fork header onwards (inclusive)
      // TODO preserve forked block data

      ctx
        .deleteFrom(Tables.BLOCK_HEADER)
        .where(Tables.BLOCK_HEADER.NUMBER.ge(forkHeader.number))
        .execute()

      log.info("Fork detected. State reset to ${forkHeader.number - 1}")
    }
  }

  private suspend fun tryImport(range: LongRange): Int {

    log.info("Attempting to import $range")

    val headers = blockReader.headers(range)

    if (headers.isEmpty()) {
      return 0
    }

    val firstHeader = headers.first()

    // first check if a fork has occurred which affects any blocks we have already import

    val forkDetected = latestHeaderInDb()
      ?.let { Hash.fromHexString(it.hash) != firstHeader.parentHash }
      ?: false

    if (forkDetected) {
      handleFork()

      // short circuit and allow a fresh import pass with forked state removed
      // specify 1 as the return value so that as immediate retry is scheduled instead of waiting for the poll interval
      return 2
    }

    // next we check if we integrity check the headers we did read in case a fork occurred in the middle

    if (!checkBatchIntegrity(headers.asReversed().drop(1), headers.last())) {
      // best to simply stop this import pass and allow for a retry after the poll interval
      return 0
    }

    return coroutineScope {

      val jobs = headers
        .chunked(chunkSize)
        .map { chunk ->

          async {

            val connection = dataSource.connection

            val txCtx = DSL.using(connection)

            try {

              for (header in chunk) {

                val headerRecord = header.hash
                  .let { hash -> blockReader.totalDifficulty(hash) }
                  .let { totalDifficulty ->
                    header.toBlockHeaderRecord(
                      requireNotNull(totalDifficulty) { "Total difficulty cannot be null, hash = ${header.hash}" }
                    )
                  }

                txCtx.insertInto(Tables.BLOCK_HEADER).set(headerRecord).execute()

                // insert the rest based on the processing level

                val bodyRecords = Channel<TableRecord<*>>(1024 * 10)
                val receiptRecords = Channel<TableRecord<*>>(1024 * 10)
                val traceRecords = Channel<TableRecord<*>>(1024 * 10)

                val ommerChannel = Channel<OmmerRecord>(1024 * 10)
                val transactionChannel = Channel<TransactionRecord>(1024 * 10)

                launch { readBody(headerRecord, bodyRecords, ommerChannel, transactionChannel) }
                launch { readReceipts(headerRecord, transactionChannel, receiptRecords) }
                launch { trace(headerRecord, ommerChannel, traceRecords) }

                launch {
                  writeToDb(txCtx, bodyRecords)
                  writeToDb(txCtx, receiptRecords)
                  writeToDb(txCtx, traceRecords)
                }.join()
              }

              connection.commit()

              chunk.size
            } finally {
              connection.close()
            }

          }
        }

      jobs
        .map{ it.await() }
        .sum()

    }
  }

  private fun checkBatchIntegrity(headers: List<BlockHeader>, child: BlockHeader): Boolean =
    if (headers.isEmpty()) {
      true
    } else {
      val parent = headers.first()
      parent.hash == child.parentHash && checkBatchIntegrity(headers.drop(1), parent)
    }

  private fun findForkHeader(ctx: DSLContext): BlockHeader? {

    val cursor = ctx
      .select(Tables.BLOCK_HEADER.NUMBER, Tables.BLOCK_HEADER.HASH)
      .from(Tables.BLOCK_HEADER)
      .orderBy(Tables.BLOCK_HEADER.NUMBER.desc())
      .fetchLazy()

    var forkHeader: BlockHeader? = null

    try {

      for (record in cursor) {

        val (number, hash) = record

        val header =
          requireNotNull(blockReader.header(number)) {
            "Could not read header from block chain with number = $number"
          }

        val isDifferent = header.hash != Hash.fromHexString(hash)

        if (isDifferent) {
          forkHeader = header
        } else {
          break
        }
      }
    } finally {
      cursor.close()
    }

    return forkHeader
  }

  private fun latestHeaderInDb(ctx: DSLContext = dbContext): BlockHeaderRecord? =
    ctx
      .select(Tables.BLOCK_HEADER.NUMBER, Tables.BLOCK_HEADER.HASH)
      .from(Tables.BLOCK_HEADER)
      .orderBy(Tables.BLOCK_HEADER.NUMBER.desc())
      .limit(1)
      .fetchInto(Tables.BLOCK_HEADER)
      .firstOrNull()

  private suspend fun writeToDb(ctx: DSLContext, channel: Channel<TableRecord<*>>, batchSize: Int = 256) {

    var writeBuffer = emptyList<TableRecord<*>>()

    for (record in channel) {
      // add to buffer
      writeBuffer = writeBuffer + record

      // flush if buffer is full
      if (writeBuffer.size == batchSize) {
        ctx.batchInsert(writeBuffer).execute()
      }
    }

    // flush remainder of buffer
    ctx.batchInsert(writeBuffer).execute()
  }

  private suspend fun readBody(
    header: BlockHeaderRecord,
    recordsChannel: Channel<TableRecord<*>>,
    ommerChannel: Channel<OmmerRecord>,
    transactionChannel: Channel<TransactionRecord>
  ) {

    if (!processingLevel.isActive(BODY)) {
      // close the channels and return early
      recordsChannel.close()
      ommerChannel.close()
      transactionChannel.close()
      return
    }

    Hash.fromHexString(header.hash)
      .let { hash -> blockReader.body(hash) }
      ?.let { body ->

        body.ommers.forEachIndexed { idx, ommer ->
          ommer
            .toOmmerRecord(header, idx)
            .also {
              recordsChannel.send(it)
              ommerChannel.send(it)
            }
        }

        body.transactions.forEachIndexed { idx, transaction ->
          transaction
            .toTransactionRecord(header, idx)
            .also {
              recordsChannel.send(it)
              transactionChannel.send(it)
            }
        }

        recordsChannel.close()
        transactionChannel.close()
        ommerChannel.close()
      }
  }

  private suspend fun readReceipts(
    header: BlockHeaderRecord,
    transactionChannel: Channel<TransactionRecord>,
    recordsChannel: Channel<TableRecord<*>>
  ) {

    if (!processingLevel.isActive(RECEIPTS)) {
      // close the channels and return early
      recordsChannel.close()
      return
    }

    val hash = Hash.fromHexString(header.hash)
    val receipts =
      requireNotNull(blockReader.receipts(hash)) { "Receipts cannot be null, hash = $hash" }.toTypedArray()

    var totalGasUsed = 0L
    var index = 0

    for (transaction in transactionChannel) {

      val receipt = receipts[index]

      // Calculate gasUsed in this transaction and increment totalGasUsed
      val gasUsed = receipt.cumulativeGasUsed - totalGasUsed
      totalGasUsed += gasUsed

      val receiptRecord =
        receipt.toTransactionReceiptRecord(objectMapper, header, transaction, gasUsed)

      recordsChannel.send(receiptRecord)

      // important that this occurs last to allow relations to be inserted first
      receipt
        .toEventRecords(header, transaction)
        .forEach { recordsChannel.send(it) }

      index += 1
    }

    recordsChannel.close()
  }

  private suspend fun trace(
    header: BlockHeaderRecord,
    ommerChannel: Channel<OmmerRecord>,
    recordsChannel: Channel<TableRecord<*>>
  ) {

    if (!processingLevel.isActive(TRACES)) {
      // close the channels and return early
      recordsChannel.close()
      return
    }

    val hash = Hash.fromHexString(header.hash)
    val coinbase = Address.fromHexString(header.coinbase)

    val trace = requireNotNull(blockReader.trace(hash)) { "Trace cannot be null, hash = $hash" }

    recordsChannel.send(
      BlockTraceRecord()
        .apply {
          this.blockHash = header.hash
          this.trace = JSONB.valueOf(trace.jsonTrace)
        }
    )

    blockReader
      .touchedAccounts(trace)
      .map { it.toAccountRecord(header) }
      .forEach { recordsChannel.send(it) }

    var ommerCoinbaseMap = emptyMap<Hash, Address>()

    for (ommer in ommerChannel) {
      ommerCoinbaseMap = ommerCoinbaseMap + (Hash.fromHexString(ommer.hash) to Address.fromHexString(ommer.coinbase))
    }

    trace
      .toBalanceDeltas(hash, coinbase, ommerCoinbaseMap)
      .map { it.toBalanceDeltaRecord(header) }
      .forEach { recordsChannel.send(it) }

    trace.transactionTraces
      .map { txTrace ->

        txTrace.contractsCreated
          .map { it.toContractCreatedRecord(header) }
          .forEach { recordsChannel.send(it) }

        txTrace.contractsDestroyed
          .map { it.toContractDestroyedRecord(header) }
          .forEach { recordsChannel.send(it) }
      }

    recordsChannel.close()
  }
}
