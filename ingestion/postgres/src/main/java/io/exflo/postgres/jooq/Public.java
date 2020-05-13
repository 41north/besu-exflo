/*
 * This file is generated by jOOQ.
 */
package io.exflo.postgres.jooq;


import io.exflo.postgres.jooq.tables.Account;
import io.exflo.postgres.jooq.tables.BalanceDelta;
import io.exflo.postgres.jooq.tables.BlockHeader;
import io.exflo.postgres.jooq.tables.BlockTrace;
import io.exflo.postgres.jooq.tables.ContractCreated;
import io.exflo.postgres.jooq.tables.ContractDestroyed;
import io.exflo.postgres.jooq.tables.ContractEvent;
import io.exflo.postgres.jooq.tables.FlywaySchemaHistory;
import io.exflo.postgres.jooq.tables.FungibleTokenTransfer;
import io.exflo.postgres.jooq.tables.ImportQueue;
import io.exflo.postgres.jooq.tables.InternalTransaction;
import io.exflo.postgres.jooq.tables.Metadata;
import io.exflo.postgres.jooq.tables.NonFungibleTokenTransfer;
import io.exflo.postgres.jooq.tables.Ommer;
import io.exflo.postgres.jooq.tables.Reward;
import io.exflo.postgres.jooq.tables.Transaction;
import io.exflo.postgres.jooq.tables.TransactionReceipt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.processing.Generated;

import org.jooq.Catalog;
import org.jooq.Sequence;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.12.3"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Public extends SchemaImpl {

    private static final long serialVersionUID = 49552753;

    /**
     * The reference instance of <code>public</code>
     */
    public static final Public PUBLIC = new Public();

    /**
     * The table <code>public.account</code>.
     */
    public final Account ACCOUNT = io.exflo.postgres.jooq.tables.Account.ACCOUNT;

    /**
     * The table <code>public.balance_delta</code>.
     */
    public final BalanceDelta BALANCE_DELTA = io.exflo.postgres.jooq.tables.BalanceDelta.BALANCE_DELTA;

    /**
     * The table <code>public.block_header</code>.
     */
    public final BlockHeader BLOCK_HEADER = io.exflo.postgres.jooq.tables.BlockHeader.BLOCK_HEADER;

    /**
     * The table <code>public.block_trace</code>.
     */
    public final BlockTrace BLOCK_TRACE = io.exflo.postgres.jooq.tables.BlockTrace.BLOCK_TRACE;

    /**
     * The table <code>public.contract_created</code>.
     */
    public final ContractCreated CONTRACT_CREATED = io.exflo.postgres.jooq.tables.ContractCreated.CONTRACT_CREATED;

    /**
     * The table <code>public.contract_destroyed</code>.
     */
    public final ContractDestroyed CONTRACT_DESTROYED = io.exflo.postgres.jooq.tables.ContractDestroyed.CONTRACT_DESTROYED;

    /**
     * The table <code>public.contract_event</code>.
     */
    public final ContractEvent CONTRACT_EVENT = io.exflo.postgres.jooq.tables.ContractEvent.CONTRACT_EVENT;

    /**
     * The table <code>public.flyway_schema_history</code>.
     */
    public final FlywaySchemaHistory FLYWAY_SCHEMA_HISTORY = io.exflo.postgres.jooq.tables.FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY;

    /**
     * The table <code>public.fungible_token_transfer</code>.
     */
    public final FungibleTokenTransfer FUNGIBLE_TOKEN_TRANSFER = io.exflo.postgres.jooq.tables.FungibleTokenTransfer.FUNGIBLE_TOKEN_TRANSFER;

    /**
     * The table <code>public.import_queue</code>.
     */
    public final ImportQueue IMPORT_QUEUE = io.exflo.postgres.jooq.tables.ImportQueue.IMPORT_QUEUE;

    /**
     * The table <code>public.internal_transaction</code>.
     */
    public final InternalTransaction INTERNAL_TRANSACTION = io.exflo.postgres.jooq.tables.InternalTransaction.INTERNAL_TRANSACTION;

    /**
     * The table <code>public.metadata</code>.
     */
    public final Metadata METADATA = io.exflo.postgres.jooq.tables.Metadata.METADATA;

    /**
     * The table <code>public.non_fungible_token_transfer</code>.
     */
    public final NonFungibleTokenTransfer NON_FUNGIBLE_TOKEN_TRANSFER = io.exflo.postgres.jooq.tables.NonFungibleTokenTransfer.NON_FUNGIBLE_TOKEN_TRANSFER;

    /**
     * The table <code>public.ommer</code>.
     */
    public final Ommer OMMER = io.exflo.postgres.jooq.tables.Ommer.OMMER;

    /**
     * The table <code>public.reward</code>.
     */
    public final Reward REWARD = io.exflo.postgres.jooq.tables.Reward.REWARD;

    /**
     * The table <code>public.transaction</code>.
     */
    public final Transaction TRANSACTION = io.exflo.postgres.jooq.tables.Transaction.TRANSACTION;

    /**
     * The table <code>public.transaction_receipt</code>.
     */
    public final TransactionReceipt TRANSACTION_RECEIPT = io.exflo.postgres.jooq.tables.TransactionReceipt.TRANSACTION_RECEIPT;

    /**
     * No further instances allowed
     */
    private Public() {
        super("public", null);
    }


    @Override
    public Catalog getCatalog() {
        return DefaultCatalog.DEFAULT_CATALOG;
    }

    @Override
    public final List<Sequence<?>> getSequences() {
        List result = new ArrayList();
        result.addAll(getSequences0());
        return result;
    }

    private final List<Sequence<?>> getSequences0() {
        return Arrays.<Sequence<?>>asList(
            Sequences.BALANCE_DELTA_ID_SEQ);
    }

    @Override
    public final List<Table<?>> getTables() {
        List result = new ArrayList();
        result.addAll(getTables0());
        return result;
    }

    private final List<Table<?>> getTables0() {
        return Arrays.<Table<?>>asList(
            Account.ACCOUNT,
            BalanceDelta.BALANCE_DELTA,
            BlockHeader.BLOCK_HEADER,
            BlockTrace.BLOCK_TRACE,
            ContractCreated.CONTRACT_CREATED,
            ContractDestroyed.CONTRACT_DESTROYED,
            ContractEvent.CONTRACT_EVENT,
            FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY,
            FungibleTokenTransfer.FUNGIBLE_TOKEN_TRANSFER,
            ImportQueue.IMPORT_QUEUE,
            InternalTransaction.INTERNAL_TRANSACTION,
            Metadata.METADATA,
            NonFungibleTokenTransfer.NON_FUNGIBLE_TOKEN_TRANSFER,
            Ommer.OMMER,
            Reward.REWARD,
            Transaction.TRANSACTION,
            TransactionReceipt.TRANSACTION_RECEIPT);
    }
}
