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

create table metadata
(
    key   varchar(128) primary key,
    value varchar(2048) not null
);

create table import_queue
(
    number       bigint    not null,
    hash         char(66)  not null unique,
    stage        smallint  not null default 0,
    timestamp    timestamp not null,
    primary key (number, hash)
);

create index idx_import_queue__hash on import_queue (hash);
create index idx_import_queue__stage_timestamp_asc on import_queue (stage asc, timestamp asc);

create table block_header
(
    hash              char(66) primary key references import_queue (hash),
    number            bigint,
    parent_hash       char(66)  not null,
    nonce             bigint    null,
    is_canonical      boolean   not null,
    state_root        char(66)  not null,
    receipts_root     char(66)  not null,
    transactions_root char(66)  not null,
    coinbase          char(42)  not null,
    difficulty        numeric   not null,
    total_difficulty  numeric   not null,
    extra_data        bytea     null,
    gas_limit         bigint    not null,
    gas_used          bigint    not null,
    timestamp         timestamp not null,
    mix_hash          char(66)  not null,
    ommers_hash       char(66)  not null,
    logs_bloom        char(514) not null
);

create index idx_block_header__number_desc on block_header (number desc);

create table transaction
(
    hash             char(66) primary key,
    block_number     bigint    not null,
    block_hash       char(66)  not null references block_header (hash) on update cascade on delete cascade,
    index            int       not null,
    nonce            bigint    not null,
    "from"           char(42)  not null,
    "to"             char(42)  null,
    value            numeric   not null,
    gas_price        numeric   not null,
    gas_limit        bigint    not null,
    payload          bytea     null,
    chain_id         numeric   null,
    fee              numeric   not null,
    rec_id           smallint  not null,
    r                numeric   not null,
    s                numeric   not null,
    contract_address char(42)  null,
    timestamp        timestamp not null
);

create index idx_transaction__block_hash on transaction (block_hash);
create index idx_transaction__number_desc on transaction (block_number desc);
create index idx_transaction__index_asc on transaction (index asc);

create table transaction_receipt
(
    transaction_hash    char(66) primary key,
    transaction_index   int       not null,
    block_hash          char(66)  not null references block_header (hash) on update cascade on delete cascade,
    block_number        bigint    not null,
    "from"              char(42)  not null,
    "to"                char(42)  null,
    contract_address    char(42)  null,
    cumulative_gas_used bigint    not null,
    gas_used            bigint    not null,
    logs                text      not null,
    state_root          char(66)  null,
    status              smallint  null,
    bloom_filter        char(514) not null,
    timestamp           timestamp not null,
    revert_reason       bytea     null
);

create index idx_transaction_receipt__number_desc on transaction_receipt (block_number desc);
create index idx_transaction_receipt__transaction_hash on transaction_receipt (transaction_hash);

create table ommer
(
    hash              char(66) primary key,
    number            bigint    not null,
    nephew_hash       char(66)  not null references block_header (hash) on update cascade on delete cascade,
    height            bigint    not null,
    parent_hash       char(66)  not null,
    index             int       not null,
    nonce             bigint    null,
    ommers_hash       char(66)  not null,
    state_root        char(66)  not null,
    receipts_root     char(66)  not null,
    transactions_root char(66)  not null,
    coinbase          char(42)  not null,
    difficulty        numeric   not null,
    extra_data        bytea     null,
    gas_limit         bigint    not null,
    gas_used          bigint    not null,
    timestamp         timestamp not null,
    mix_hash          char(66)  not null,
    logs_bloom        char(514) not null
);

create index idx_ommer__number_desc on ommer (number desc);
create index idx_ommer__hash on ommer (hash);
create index idx_ommer__nephew_hash on ommer (nephew_hash);
create index idx_ommer__index on ommer (index asc);
create index idx_ommer__height on ommer (height);

create table account
(
    address      char(42) not null,
    block_hash   char(66) not null references block_header (hash) on update cascade on delete cascade,
    block_number bigint   not null,
    nonce        bigint   not null,
    balance      numeric  not null,
    primary key (address, block_hash)
);

create index idx_account__address on account (address);
create index idx_account__block_hash on account (block_hash);
create index idx_account__block_number on account (block_number);

create type contract_type as enum (
    'ERC1155',
    'ERC777',
    'ERC721',
    'ERC20',
    'GENERIC'
    );

create type contract_capability as enum (
    'ERC1155',
    'ERC1155_TOKEN_RECEIVER',
    'ERC777',
    'ERC165',
    'ERC721',
    'ERC721_METADATA',
    'ERC721_ENUMERABLE',
    'ERC20',
    'ERC20_DETAILED',
    'ERC20_BURNABLE',
    'ERC20_MINTABLE',
    'ERC20_PAUSABLE',
    'ERC20_CAPPED'
    );

create table contract_created
(
    address          char(42)              not null,
    creator          char(42)              not null,
    code             text                  null,
    type             contract_type         null,
    capabilities     contract_capability[] null,
    name             varchar(128)          null,
    symbol           varchar(128)          null,
    decimals         smallint              null,
    total_supply     numeric               null,
    granularity      numeric               null,
    cap              numeric               null,
    block_hash       char(66)              not null references block_header (hash) on update cascade on delete cascade,
    block_number     bigint                not null,
    transaction_hash char(66)              not null references transaction (hash) on update cascade on delete cascade,
    timestamp        timestamp             not null,
    primary key (address, transaction_hash)
);

create index idx_contract_created__address on contract_created (address);
create index idx_contract_created__block_number_desc on contract_created (block_number desc);

create table contract_destroyed
(
    address          char(42)  not null,
    refund_address   char(42)  not null,
    refund_amount    numeric   not null,
    block_hash       char(66)  not null references block_header (hash) on update cascade on delete cascade,
    block_number     bigint    not null,
    transaction_hash char(66)  not null references transaction (hash) on update cascade on delete cascade,
    timestamp        timestamp not null,
    primary key (address, transaction_hash)
);

create index idx_contract_destroyed__address on contract_destroyed (address);
create index idx_contract_destroyed__block_number_desc on contract_destroyed (block_number desc);
create index idx_contract_destroyed__block_hash on contract_destroyed (block_hash);
create index idx_contract_destroyed__transaction_hash on contract_destroyed (transaction_hash);

create type delta_type as enum (
    'BLOCK_REWARD',
    'OMMER_REWARD',
    'TX',
    'TX_FEE',
    'INTERNAL_TX',
    'TOKEN_TRANSFER',
    'CONTRACT_CREATION',
    'CONTRACT_DESTRUCTION'
    );

create table balance_delta
(
    id                bigserial primary key,
    delta_type        delta_type not null,
    contract_address  char(42)   null,
    "from"            char(42)   null,
    "to"              char(42)   not null,
    amount            numeric    null,
    token_id          numeric    null,
    block_number      bigint     not null,
    block_hash        char(66)   not null references block_header (hash) on update cascade on delete cascade,
    transaction_hash  char(66)   null references transaction (hash) on update cascade on delete cascade,
    transaction_index int        null,
    block_timestamp   timestamp  not null
);

create index idx_balance_delta__block_number_desc on balance_delta (block_number desc);
create index idx_balance_delta__transaction_hash on balance_delta (transaction_hash);

create type contract_event_type as enum (
    'fungible_approval',
    'fungible_transfer',
    'non_fungible_approval',
    'approval_for_all',
    'non_fungible_transfer',
    'sent',
    'minted',
    'burned',
    'authorized_operator',
    'revoked_operator',
    'transfer_single',
    'transfer_batch',
    'uri'
    );

create table contract_event
(
    block_number     bigint              not null,
    block_hash       char(66)            not null references block_header (hash) on update cascade on delete cascade,
    transaction_hash char(66)            not null references transaction (hash) on update cascade on delete cascade,
    contract_address char(42)            not null,
    type             contract_event_type not null,
    owner_address    char(42)            null,
    spender_address  char(42)            null,
    operator_address char(42)            null,
    holder_address   char(42)            null,
    approved_address char(42)            null,
    from_address     char(42)            null,
    to_address       char(42)            null,
    value            numeric             null,
    amount           numeric             null,
    token_id         numeric             null,
    id               numeric             null,
    ids              numeric[]           null,
    values           numeric[]           null,
    approved         bool                null,
    data             bytea               null,
    operator_data    bytea               null,
    value_str        varchar(2048)       null
);

create index idx_contract_event__block_number_desc on contract_event (block_number desc);
create index idx_contract_event__transaction_hash on contract_event (transaction_hash);
create index idx_event_type on contract_event (type);

create view reward AS
SELECT id,
       delta_type,
       "to",
       amount,
       block_number,
       block_hash,
       block_timestamp
FROM balance_delta
WHERE delta_type IN ('BLOCK_REWARD', 'OMMER_REWARD');

create view internal_transaction AS
SELECT id,
       delta_type,
       "from",
       "to",
       amount,
       block_number,
       block_hash,
       block_timestamp,
       transaction_hash,
       transaction_index
FROM balance_delta
WHERE delta_type IN ('INTERNAL_TX', 'CONTRACT_CREATION', 'CONTRACT_DESTRUCTION');

create view fungible_token_transfer AS
SELECT id,
       contract_address,
       "from",
       "to",
       amount,
       block_number,
       block_hash,
       block_timestamp,
       transaction_hash,
       transaction_index
FROM balance_delta
WHERE delta_type = 'TOKEN_TRANSFER'
  AND amount IS NOT NULL;

create view non_fungible_token_transfer AS
SELECT id,
       contract_address,
       "from",
       "to",
       token_id,
       block_number,
       block_hash,
       block_timestamp,
       transaction_hash,
       transaction_index
FROM balance_delta
WHERE delta_type = 'TOKEN_TRANSFER'
  AND token_id IS NOT NULL;
