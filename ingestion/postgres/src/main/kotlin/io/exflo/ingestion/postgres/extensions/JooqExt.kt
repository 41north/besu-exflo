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

package io.exflo.ingestion.postgres.extensions

import io.exflo.postgres.jooq.tables.records.AccountRecord
import io.exflo.postgres.jooq.tables.records.BalanceDeltaRecord
import io.exflo.postgres.jooq.tables.records.BlockHeaderRecord
import io.exflo.postgres.jooq.tables.records.ContractCreatedRecord
import io.exflo.postgres.jooq.tables.records.ContractDestroyedRecord
import io.exflo.postgres.jooq.tables.records.ContractEventRecord
import io.exflo.postgres.jooq.tables.records.OmmerRecord
import io.exflo.postgres.jooq.tables.records.TransactionReceiptRecord
import io.exflo.postgres.jooq.tables.records.TransactionRecord
import org.jooq.TableRecord

val TableRecord<*>.blockNumber: Long
  get() = when (this) {
    is BlockHeaderRecord -> number
    is OmmerRecord -> number
    is TransactionRecord -> blockNumber
    is TransactionReceiptRecord -> blockNumber
    is ContractEventRecord -> blockNumber
    is ContractCreatedRecord -> blockNumber
    is ContractDestroyedRecord -> blockNumber
    is BalanceDeltaRecord -> blockNumber
    is AccountRecord -> blockNumber
    else -> throw IllegalArgumentException()
  }

val TableRecord<*>.blockHash: String
  get() = when (this) {
    is BlockHeaderRecord -> hash
    is OmmerRecord -> nephewHash
    is TransactionRecord -> blockHash
    is TransactionReceiptRecord -> blockHash
    is ContractEventRecord -> blockHash
    is ContractCreatedRecord -> blockHash
    is ContractDestroyedRecord -> blockHash
    is BalanceDeltaRecord -> blockHash
    is AccountRecord -> blockHash
    else -> throw IllegalArgumentException()
  }
