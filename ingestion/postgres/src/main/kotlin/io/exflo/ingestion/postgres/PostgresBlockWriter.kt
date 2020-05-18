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
import kotlin.time.seconds

@ExperimentalTime
class PostgresBlockWriter(
  private val objectMapper: ObjectMapper,
  private val dataSource: DataSource,
  private val blockReader: BlockReader,
  private val cliOptions: ExfloPostgresCliOptions
) : BlockWriter {

  private val dbContext = DSL.using(dataSource, SQLDialect.POSTGRES)

  private val log = LogManager.getLogger()

  private val pollInterval = 1.seconds

  private val processingLevel = cliOptions.processingLevel

  override suspend fun run() {
    coroutineScope {

      while (isActive) {

        var head = latestHead()

        do {

          val headers = readHeadersFrom(head, 128)

          val elapsed = measureTime {

            val writeJobs = headers
              .chunked(16)
              .map { chunk ->

                launch {

                  val connection = dataSource.connection

                  val txCtx = DSL.using(connection)

                  try {

                    // update any previous headers with the same numbers and set them as no longer canonical

                    txCtx
                      .update(Tables.BLOCK_HEADER)
                      .set(Tables.BLOCK_HEADER.IS_CANONICAL, false)
                      .where(Tables.BLOCK_HEADER.NUMBER.`in`(chunk.map{ it.number }))
                      .execute()

                    //

                    for (header in chunk) {

                      // insert the header

                      val headerRecord = header.hash
                        .let { hash -> blockReader.totalDifficulty(hash) }
                        .let { totalDifficulty ->
                          header.toBlockHeaderRecord(
                            requireNotNull(totalDifficulty) { "Total difficulty cannot be null, hash = ${header.hash}" }
                          )
                        }

                      txCtx.insertInto(Tables.BLOCK_HEADER).set(headerRecord).execute()

                      // insert the rest based on the processing level

                      val bodyRecords = Channel<TableRecord<*>>(512)
                      val receiptRecords = Channel<TableRecord<*>>(512)
                      val traceRecords = Channel<TableRecord<*>>(512)

                      val ommerChannel = Channel<OmmerRecord>(512)
                      val transactionChannel = Channel<TransactionRecord>(512)

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

                  } catch (e: Exception) {
                    connection.rollback()
                  } finally {
                    connection.close()
                  }

                }
              }

            writeJobs.forEach { it.join() }
          }

          val headNumber = headers.firstOrNull()?.number
          val tailNumber = headers.lastOrNull()?.number

          val importRate = if (headers.isNotEmpty()) headers.size / elapsed.inSeconds else 0.0

          log.info("Imported ${headers.size} blocks in $elapsed, rate = ${importRate.format(2)} / second. Start = $headNumber, finish = $tailNumber")

          // grab the tail for the next read
          if (headers.isNotEmpty()) {
            head = headers.last().hash
          }

        } while (isActive && headers.isNotEmpty())

        // finished an import pass, either reaching the genesis block from an initial sync, or reaching an ancestor
        // that has already been imported

        if (isActive) {
          pollInterval
            .apply {
              log.debug("Import pass complete. Waiting $this before another attempt")
              delay(this)
            }
        }
      }

    }
  }

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

  private fun latestHead(): Hash {

    // the latest head from besu
    var head = blockReader.chainHead()!!

    // determine if ever made it back to the genesis block
    // if not we resume from there until we reach the genesis block, otherwise resume from besu's head

    val earliestEntry = dbContext
      .select(Tables.BLOCK_HEADER.NUMBER, Tables.BLOCK_HEADER.HASH)
      .from(Tables.BLOCK_HEADER)
      .orderBy(Tables.BLOCK_HEADER.NUMBER.asc())
      .limit(1)
      .fetch()
      .firstOrNull()

    val earliestNumber = earliestEntry?.value1()
    val earliestHash = earliestEntry?.let { Hash.fromHexString(it.value2()) }

    if (!(earliestNumber == null || earliestNumber == BlockHeader.GENESIS_BLOCK_NUMBER)) {
      // we did not complete the initial first pass back to genesis
      // lets restart from the earliest point we did reach
      head = blockReader.header(earliestHash!!)!!.parentHash
      log.info("Restarting header import from block number = $earliestNumber")
    }

    return head
  }

  private fun readHeadersFrom(hash: Hash, count: Int = 128): List<BlockHeader> {

    // read the next series of headers starting from hash and propagating backwards via parent hashes
    val headers = blockReader.headersFrom(hash, count + 1)

    val hashStrings = headers
      .map { it.hash.toHexString() }
      .toSet()

    // check the import queue for existing entries for the headers we just read, indicating we are reaching a common ancestor
    // TODO make this logic more robust by checking where in the sequence the common ancestor was found and ensuring it was at the end

    // TODO need to mark old headers as no longer canonical in the db

    val existingHashStrings = dbContext
      .select(Tables.BLOCK_HEADER.HASH)
      .from(Tables.BLOCK_HEADER)
      .where(Tables.BLOCK_HEADER.HASH.`in`(hashStrings))
      .fetch()
      .map { it.value1() }

    // subtract any existing hashes found in the import queue from the set of hashes read from besu and filtered the headers list

    val hashStringsToInsert =
      existingHashStrings
        .takeIf { it.isNotEmpty() }
        ?.let { hashStrings.minus(it) }
        ?: hashStrings

    return headers
      .filter { hashStringsToInsert.contains(it.hash.toHexString()) }
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
