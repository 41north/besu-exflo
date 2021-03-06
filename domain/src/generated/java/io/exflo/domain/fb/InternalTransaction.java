// automatically generated by the FlatBuffers compiler, do not modify

package io.exflo.domain.fb;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class InternalTransaction extends Table {
  public static InternalTransaction getRootAsInternalTransaction(ByteBuffer _bb) { return getRootAsInternalTransaction(_bb, new InternalTransaction()); }
  public static InternalTransaction getRootAsInternalTransaction(ByteBuffer _bb, InternalTransaction obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; vtable_start = bb_pos - bb.getInt(bb_pos); vtable_size = bb.getShort(vtable_start); }
  public InternalTransaction __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public int pc() { int o = __offset(4); return o != 0 ? bb.getInt(o + bb_pos) : 0; }
  public Bytes20 from() { return from(new Bytes20()); }
  public Bytes20 from(Bytes20 obj) { int o = __offset(6); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }
  public Bytes20 to() { return to(new Bytes20()); }
  public Bytes20 to(Bytes20 obj) { int o = __offset(8); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }
  public UInt256 amount() { return amount(new UInt256()); }
  public UInt256 amount(UInt256 obj) { int o = __offset(10); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }
  public Bytes32 transactionHash() { return transactionHash(new Bytes32()); }
  public Bytes32 transactionHash(Bytes32 obj) { int o = __offset(12); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }

  public static int createInternalTransaction(FlatBufferBuilder builder,
      int pc,
      int fromOffset,
      int toOffset,
      int amountOffset,
      int transactionHashOffset) {
    builder.startObject(5);
    InternalTransaction.addTransactionHash(builder, transactionHashOffset);
    InternalTransaction.addAmount(builder, amountOffset);
    InternalTransaction.addTo(builder, toOffset);
    InternalTransaction.addFrom(builder, fromOffset);
    InternalTransaction.addPc(builder, pc);
    return InternalTransaction.endInternalTransaction(builder);
  }

  public static void startInternalTransaction(FlatBufferBuilder builder) { builder.startObject(5); }
  public static void addPc(FlatBufferBuilder builder, int pc) { builder.addInt(0, pc, 0); }
  public static void addFrom(FlatBufferBuilder builder, int fromOffset) { builder.addOffset(1, fromOffset, 0); }
  public static void addTo(FlatBufferBuilder builder, int toOffset) { builder.addOffset(2, toOffset, 0); }
  public static void addAmount(FlatBufferBuilder builder, int amountOffset) { builder.addOffset(3, amountOffset, 0); }
  public static void addTransactionHash(FlatBufferBuilder builder, int transactionHashOffset) { builder.addOffset(4, transactionHashOffset, 0); }
  public static int endInternalTransaction(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
}

