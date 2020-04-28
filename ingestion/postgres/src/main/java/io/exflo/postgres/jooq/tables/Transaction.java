/*
 * This file is generated by jOOQ.
 */
package io.exflo.postgres.jooq.tables;


import io.exflo.postgres.jooq.Indexes;
import io.exflo.postgres.jooq.Keys;
import io.exflo.postgres.jooq.Public;
import io.exflo.postgres.jooq.tables.records.TransactionRecord;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import javax.annotation.processing.Generated;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row18;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;


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
public class Transaction extends TableImpl<TransactionRecord> {

    private static final long serialVersionUID = 115608777;

    /**
     * The reference instance of <code>public.transaction</code>
     */
    public static final Transaction TRANSACTION = new Transaction();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<TransactionRecord> getRecordType() {
        return TransactionRecord.class;
    }

    /**
     * The column <code>public.transaction.hash</code>.
     */
    public final TableField<TransactionRecord, String> HASH = createField(DSL.name("hash"), org.jooq.impl.SQLDataType.CHAR(66).nullable(false), this, "");

    /**
     * The column <code>public.transaction.block_number</code>.
     */
    public final TableField<TransactionRecord, Long> BLOCK_NUMBER = createField(DSL.name("block_number"), org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>public.transaction.block_hash</code>.
     */
    public final TableField<TransactionRecord, String> BLOCK_HASH = createField(DSL.name("block_hash"), org.jooq.impl.SQLDataType.CHAR(66).nullable(false), this, "");

    /**
     * The column <code>public.transaction.index</code>.
     */
    public final TableField<TransactionRecord, Integer> INDEX = createField(DSL.name("index"), org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>public.transaction.nonce</code>.
     */
    public final TableField<TransactionRecord, Long> NONCE = createField(DSL.name("nonce"), org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>public.transaction.from</code>.
     */
    public final TableField<TransactionRecord, String> FROM = createField(DSL.name("from"), org.jooq.impl.SQLDataType.CHAR(42).nullable(false), this, "");

    /**
     * The column <code>public.transaction.to</code>.
     */
    public final TableField<TransactionRecord, String> TO = createField(DSL.name("to"), org.jooq.impl.SQLDataType.CHAR(42), this, "");

    /**
     * The column <code>public.transaction.value</code>.
     */
    public final TableField<TransactionRecord, BigDecimal> VALUE = createField(DSL.name("value"), org.jooq.impl.SQLDataType.NUMERIC.nullable(false), this, "");

    /**
     * The column <code>public.transaction.gas_price</code>.
     */
    public final TableField<TransactionRecord, BigDecimal> GAS_PRICE = createField(DSL.name("gas_price"), org.jooq.impl.SQLDataType.NUMERIC.nullable(false), this, "");

    /**
     * The column <code>public.transaction.gas_limit</code>.
     */
    public final TableField<TransactionRecord, Long> GAS_LIMIT = createField(DSL.name("gas_limit"), org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>public.transaction.payload</code>.
     */
    public final TableField<TransactionRecord, byte[]> PAYLOAD = createField(DSL.name("payload"), org.jooq.impl.SQLDataType.BLOB, this, "");

    /**
     * The column <code>public.transaction.chain_id</code>.
     */
    public final TableField<TransactionRecord, BigDecimal> CHAIN_ID = createField(DSL.name("chain_id"), org.jooq.impl.SQLDataType.NUMERIC, this, "");

    /**
     * The column <code>public.transaction.fee</code>.
     */
    public final TableField<TransactionRecord, BigDecimal> FEE = createField(DSL.name("fee"), org.jooq.impl.SQLDataType.NUMERIC.nullable(false), this, "");

    /**
     * The column <code>public.transaction.rec_id</code>.
     */
    public final TableField<TransactionRecord, Short> REC_ID = createField(DSL.name("rec_id"), org.jooq.impl.SQLDataType.SMALLINT.nullable(false), this, "");

    /**
     * The column <code>public.transaction.r</code>.
     */
    public final TableField<TransactionRecord, BigDecimal> R = createField(DSL.name("r"), org.jooq.impl.SQLDataType.NUMERIC.nullable(false), this, "");

    /**
     * The column <code>public.transaction.s</code>.
     */
    public final TableField<TransactionRecord, BigDecimal> S = createField(DSL.name("s"), org.jooq.impl.SQLDataType.NUMERIC.nullable(false), this, "");

    /**
     * The column <code>public.transaction.contract_address</code>.
     */
    public final TableField<TransactionRecord, String> CONTRACT_ADDRESS = createField(DSL.name("contract_address"), org.jooq.impl.SQLDataType.CHAR(42), this, "");

    /**
     * The column <code>public.transaction.timestamp</code>.
     */
    public final TableField<TransactionRecord, Timestamp> TIMESTAMP = createField(DSL.name("timestamp"), org.jooq.impl.SQLDataType.TIMESTAMP.nullable(false), this, "");

    /**
     * Create a <code>public.transaction</code> table reference
     */
    public Transaction() {
        this(DSL.name("transaction"), null);
    }

    /**
     * Create an aliased <code>public.transaction</code> table reference
     */
    public Transaction(String alias) {
        this(DSL.name(alias), TRANSACTION);
    }

    /**
     * Create an aliased <code>public.transaction</code> table reference
     */
    public Transaction(Name alias) {
        this(alias, TRANSACTION);
    }

    private Transaction(Name alias, Table<TransactionRecord> aliased) {
        this(alias, aliased, null);
    }

    private Transaction(Name alias, Table<TransactionRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""));
    }

    public <O extends Record> Transaction(Table<O> child, ForeignKey<O, TransactionRecord> key) {
        super(child, key, TRANSACTION);
    }

    @Override
    public Schema getSchema() {
        return Public.PUBLIC;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.IDX_TRANSACTION__BLOCK_HASH, Indexes.IDX_TRANSACTION__INDEX_ASC, Indexes.IDX_TRANSACTION__NUMBER_DESC, Indexes.TRANSACTION_PKEY);
    }

    @Override
    public UniqueKey<TransactionRecord> getPrimaryKey() {
        return Keys.TRANSACTION_PKEY;
    }

    @Override
    public List<UniqueKey<TransactionRecord>> getKeys() {
        return Arrays.<UniqueKey<TransactionRecord>>asList(Keys.TRANSACTION_PKEY);
    }

    @Override
    public List<ForeignKey<TransactionRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<TransactionRecord, ?>>asList(Keys.TRANSACTION__TRANSACTION_BLOCK_HASH_FKEY);
    }

    public BlockHeader blockHeader() {
        return new BlockHeader(this, Keys.TRANSACTION__TRANSACTION_BLOCK_HASH_FKEY);
    }

    @Override
    public Transaction as(String alias) {
        return new Transaction(DSL.name(alias), this);
    }

    @Override
    public Transaction as(Name alias) {
        return new Transaction(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Transaction rename(String name) {
        return new Transaction(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Transaction rename(Name name) {
        return new Transaction(name, null);
    }

    // -------------------------------------------------------------------------
    // Row18 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row18<String, Long, String, Integer, Long, String, String, BigDecimal, BigDecimal, Long, byte[], BigDecimal, BigDecimal, Short, BigDecimal, BigDecimal, String, Timestamp> fieldsRow() {
        return (Row18) super.fieldsRow();
    }
}
