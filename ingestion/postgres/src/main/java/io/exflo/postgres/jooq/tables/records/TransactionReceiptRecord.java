/*
 * This file is generated by jOOQ.
 */
package io.exflo.postgres.jooq.tables.records;


import io.exflo.postgres.jooq.tables.TransactionReceipt;

import java.sql.Timestamp;

import javax.annotation.processing.Generated;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record15;
import org.jooq.Row15;
import org.jooq.impl.UpdatableRecordImpl;


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
public class TransactionReceiptRecord extends UpdatableRecordImpl<TransactionReceiptRecord> implements Record15<String, Integer, String, Long, String, String, String, Long, Long, String, String, Short, String, Timestamp, byte[]> {

    private static final long serialVersionUID = -2039293558;

    /**
     * Setter for <code>public.transaction_receipt.transaction_hash</code>.
     */
    public TransactionReceiptRecord setTransactionHash(String value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>public.transaction_receipt.transaction_hash</code>.
     */
    public String getTransactionHash() {
        return (String) get(0);
    }

    /**
     * Setter for <code>public.transaction_receipt.transaction_index</code>.
     */
    public TransactionReceiptRecord setTransactionIndex(Integer value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>public.transaction_receipt.transaction_index</code>.
     */
    public Integer getTransactionIndex() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>public.transaction_receipt.block_hash</code>.
     */
    public TransactionReceiptRecord setBlockHash(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>public.transaction_receipt.block_hash</code>.
     */
    public String getBlockHash() {
        return (String) get(2);
    }

    /**
     * Setter for <code>public.transaction_receipt.block_number</code>.
     */
    public TransactionReceiptRecord setBlockNumber(Long value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>public.transaction_receipt.block_number</code>.
     */
    public Long getBlockNumber() {
        return (Long) get(3);
    }

    /**
     * Setter for <code>public.transaction_receipt.from</code>.
     */
    public TransactionReceiptRecord setFrom(String value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>public.transaction_receipt.from</code>.
     */
    public String getFrom() {
        return (String) get(4);
    }

    /**
     * Setter for <code>public.transaction_receipt.to</code>.
     */
    public TransactionReceiptRecord setTo(String value) {
        set(5, value);
        return this;
    }

    /**
     * Getter for <code>public.transaction_receipt.to</code>.
     */
    public String getTo() {
        return (String) get(5);
    }

    /**
     * Setter for <code>public.transaction_receipt.contract_address</code>.
     */
    public TransactionReceiptRecord setContractAddress(String value) {
        set(6, value);
        return this;
    }

    /**
     * Getter for <code>public.transaction_receipt.contract_address</code>.
     */
    public String getContractAddress() {
        return (String) get(6);
    }

    /**
     * Setter for <code>public.transaction_receipt.cumulative_gas_used</code>.
     */
    public TransactionReceiptRecord setCumulativeGasUsed(Long value) {
        set(7, value);
        return this;
    }

    /**
     * Getter for <code>public.transaction_receipt.cumulative_gas_used</code>.
     */
    public Long getCumulativeGasUsed() {
        return (Long) get(7);
    }

    /**
     * Setter for <code>public.transaction_receipt.gas_used</code>.
     */
    public TransactionReceiptRecord setGasUsed(Long value) {
        set(8, value);
        return this;
    }

    /**
     * Getter for <code>public.transaction_receipt.gas_used</code>.
     */
    public Long getGasUsed() {
        return (Long) get(8);
    }

    /**
     * Setter for <code>public.transaction_receipt.logs</code>.
     */
    public TransactionReceiptRecord setLogs(String value) {
        set(9, value);
        return this;
    }

    /**
     * Getter for <code>public.transaction_receipt.logs</code>.
     */
    public String getLogs() {
        return (String) get(9);
    }

    /**
     * Setter for <code>public.transaction_receipt.state_root</code>.
     */
    public TransactionReceiptRecord setStateRoot(String value) {
        set(10, value);
        return this;
    }

    /**
     * Getter for <code>public.transaction_receipt.state_root</code>.
     */
    public String getStateRoot() {
        return (String) get(10);
    }

    /**
     * Setter for <code>public.transaction_receipt.status</code>.
     */
    public TransactionReceiptRecord setStatus(Short value) {
        set(11, value);
        return this;
    }

    /**
     * Getter for <code>public.transaction_receipt.status</code>.
     */
    public Short getStatus() {
        return (Short) get(11);
    }

    /**
     * Setter for <code>public.transaction_receipt.bloom_filter</code>.
     */
    public TransactionReceiptRecord setBloomFilter(String value) {
        set(12, value);
        return this;
    }

    /**
     * Getter for <code>public.transaction_receipt.bloom_filter</code>.
     */
    public String getBloomFilter() {
        return (String) get(12);
    }

    /**
     * Setter for <code>public.transaction_receipt.timestamp</code>.
     */
    public TransactionReceiptRecord setTimestamp(Timestamp value) {
        set(13, value);
        return this;
    }

    /**
     * Getter for <code>public.transaction_receipt.timestamp</code>.
     */
    public Timestamp getTimestamp() {
        return (Timestamp) get(13);
    }

    /**
     * Setter for <code>public.transaction_receipt.revert_reason</code>.
     */
    public TransactionReceiptRecord setRevertReason(byte... value) {
        set(14, value);
        return this;
    }

    /**
     * Getter for <code>public.transaction_receipt.revert_reason</code>.
     */
    public byte[] getRevertReason() {
        return (byte[]) get(14);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<String> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record15 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row15<String, Integer, String, Long, String, String, String, Long, Long, String, String, Short, String, Timestamp, byte[]> fieldsRow() {
        return (Row15) super.fieldsRow();
    }

    @Override
    public Row15<String, Integer, String, Long, String, String, String, Long, Long, String, String, Short, String, Timestamp, byte[]> valuesRow() {
        return (Row15) super.valuesRow();
    }

    @Override
    public Field<String> field1() {
        return TransactionReceipt.TRANSACTION_RECEIPT.TRANSACTION_HASH;
    }

    @Override
    public Field<Integer> field2() {
        return TransactionReceipt.TRANSACTION_RECEIPT.TRANSACTION_INDEX;
    }

    @Override
    public Field<String> field3() {
        return TransactionReceipt.TRANSACTION_RECEIPT.BLOCK_HASH;
    }

    @Override
    public Field<Long> field4() {
        return TransactionReceipt.TRANSACTION_RECEIPT.BLOCK_NUMBER;
    }

    @Override
    public Field<String> field5() {
        return TransactionReceipt.TRANSACTION_RECEIPT.FROM;
    }

    @Override
    public Field<String> field6() {
        return TransactionReceipt.TRANSACTION_RECEIPT.TO;
    }

    @Override
    public Field<String> field7() {
        return TransactionReceipt.TRANSACTION_RECEIPT.CONTRACT_ADDRESS;
    }

    @Override
    public Field<Long> field8() {
        return TransactionReceipt.TRANSACTION_RECEIPT.CUMULATIVE_GAS_USED;
    }

    @Override
    public Field<Long> field9() {
        return TransactionReceipt.TRANSACTION_RECEIPT.GAS_USED;
    }

    @Override
    public Field<String> field10() {
        return TransactionReceipt.TRANSACTION_RECEIPT.LOGS;
    }

    @Override
    public Field<String> field11() {
        return TransactionReceipt.TRANSACTION_RECEIPT.STATE_ROOT;
    }

    @Override
    public Field<Short> field12() {
        return TransactionReceipt.TRANSACTION_RECEIPT.STATUS;
    }

    @Override
    public Field<String> field13() {
        return TransactionReceipt.TRANSACTION_RECEIPT.BLOOM_FILTER;
    }

    @Override
    public Field<Timestamp> field14() {
        return TransactionReceipt.TRANSACTION_RECEIPT.TIMESTAMP;
    }

    @Override
    public Field<byte[]> field15() {
        return TransactionReceipt.TRANSACTION_RECEIPT.REVERT_REASON;
    }

    @Override
    public String component1() {
        return getTransactionHash();
    }

    @Override
    public Integer component2() {
        return getTransactionIndex();
    }

    @Override
    public String component3() {
        return getBlockHash();
    }

    @Override
    public Long component4() {
        return getBlockNumber();
    }

    @Override
    public String component5() {
        return getFrom();
    }

    @Override
    public String component6() {
        return getTo();
    }

    @Override
    public String component7() {
        return getContractAddress();
    }

    @Override
    public Long component8() {
        return getCumulativeGasUsed();
    }

    @Override
    public Long component9() {
        return getGasUsed();
    }

    @Override
    public String component10() {
        return getLogs();
    }

    @Override
    public String component11() {
        return getStateRoot();
    }

    @Override
    public Short component12() {
        return getStatus();
    }

    @Override
    public String component13() {
        return getBloomFilter();
    }

    @Override
    public Timestamp component14() {
        return getTimestamp();
    }

    @Override
    public byte[] component15() {
        return getRevertReason();
    }

    @Override
    public String value1() {
        return getTransactionHash();
    }

    @Override
    public Integer value2() {
        return getTransactionIndex();
    }

    @Override
    public String value3() {
        return getBlockHash();
    }

    @Override
    public Long value4() {
        return getBlockNumber();
    }

    @Override
    public String value5() {
        return getFrom();
    }

    @Override
    public String value6() {
        return getTo();
    }

    @Override
    public String value7() {
        return getContractAddress();
    }

    @Override
    public Long value8() {
        return getCumulativeGasUsed();
    }

    @Override
    public Long value9() {
        return getGasUsed();
    }

    @Override
    public String value10() {
        return getLogs();
    }

    @Override
    public String value11() {
        return getStateRoot();
    }

    @Override
    public Short value12() {
        return getStatus();
    }

    @Override
    public String value13() {
        return getBloomFilter();
    }

    @Override
    public Timestamp value14() {
        return getTimestamp();
    }

    @Override
    public byte[] value15() {
        return getRevertReason();
    }

    @Override
    public TransactionReceiptRecord value1(String value) {
        setTransactionHash(value);
        return this;
    }

    @Override
    public TransactionReceiptRecord value2(Integer value) {
        setTransactionIndex(value);
        return this;
    }

    @Override
    public TransactionReceiptRecord value3(String value) {
        setBlockHash(value);
        return this;
    }

    @Override
    public TransactionReceiptRecord value4(Long value) {
        setBlockNumber(value);
        return this;
    }

    @Override
    public TransactionReceiptRecord value5(String value) {
        setFrom(value);
        return this;
    }

    @Override
    public TransactionReceiptRecord value6(String value) {
        setTo(value);
        return this;
    }

    @Override
    public TransactionReceiptRecord value7(String value) {
        setContractAddress(value);
        return this;
    }

    @Override
    public TransactionReceiptRecord value8(Long value) {
        setCumulativeGasUsed(value);
        return this;
    }

    @Override
    public TransactionReceiptRecord value9(Long value) {
        setGasUsed(value);
        return this;
    }

    @Override
    public TransactionReceiptRecord value10(String value) {
        setLogs(value);
        return this;
    }

    @Override
    public TransactionReceiptRecord value11(String value) {
        setStateRoot(value);
        return this;
    }

    @Override
    public TransactionReceiptRecord value12(Short value) {
        setStatus(value);
        return this;
    }

    @Override
    public TransactionReceiptRecord value13(String value) {
        setBloomFilter(value);
        return this;
    }

    @Override
    public TransactionReceiptRecord value14(Timestamp value) {
        setTimestamp(value);
        return this;
    }

    @Override
    public TransactionReceiptRecord value15(byte... value) {
        setRevertReason(value);
        return this;
    }

    @Override
    public TransactionReceiptRecord values(String value1, Integer value2, String value3, Long value4, String value5, String value6, String value7, Long value8, Long value9, String value10, String value11, Short value12, String value13, Timestamp value14, byte[] value15) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        value10(value10);
        value11(value11);
        value12(value12);
        value13(value13);
        value14(value14);
        value15(value15);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached TransactionReceiptRecord
     */
    public TransactionReceiptRecord() {
        super(TransactionReceipt.TRANSACTION_RECEIPT);
    }

    /**
     * Create a detached, initialised TransactionReceiptRecord
     */
    public TransactionReceiptRecord(String transactionHash, Integer transactionIndex, String blockHash, Long blockNumber, String from, String to, String contractAddress, Long cumulativeGasUsed, Long gasUsed, String logs, String stateRoot, Short status, String bloomFilter, Timestamp timestamp, byte[] revertReason) {
        super(TransactionReceipt.TRANSACTION_RECEIPT);

        set(0, transactionHash);
        set(1, transactionIndex);
        set(2, blockHash);
        set(3, blockNumber);
        set(4, from);
        set(5, to);
        set(6, contractAddress);
        set(7, cumulativeGasUsed);
        set(8, gasUsed);
        set(9, logs);
        set(10, stateRoot);
        set(11, status);
        set(12, bloomFilter);
        set(13, timestamp);
        set(14, revertReason);
    }
}
