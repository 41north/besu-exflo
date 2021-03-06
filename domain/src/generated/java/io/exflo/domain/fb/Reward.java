// automatically generated by the FlatBuffers compiler, do not modify

package io.exflo.domain.fb;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class Reward extends Table {
  public static Reward getRootAsReward(ByteBuffer _bb) { return getRootAsReward(_bb, new Reward()); }
  public static Reward getRootAsReward(ByteBuffer _bb, Reward obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; vtable_start = bb_pos - bb.getInt(bb_pos); vtable_size = bb.getShort(vtable_start); }
  public Reward __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public Bytes32 hash() { return hash(new Bytes32()); }
  public Bytes32 hash(Bytes32 obj) { int o = __offset(4); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }
  public UInt256 amount() { return amount(new UInt256()); }
  public UInt256 amount(UInt256 obj) { int o = __offset(6); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }

  public static int createReward(FlatBufferBuilder builder,
      int hashOffset,
      int amountOffset) {
    builder.startObject(2);
    Reward.addAmount(builder, amountOffset);
    Reward.addHash(builder, hashOffset);
    return Reward.endReward(builder);
  }

  public static void startReward(FlatBufferBuilder builder) { builder.startObject(2); }
  public static void addHash(FlatBufferBuilder builder, int hashOffset) { builder.addOffset(0, hashOffset, 0); }
  public static void addAmount(FlatBufferBuilder builder, int amountOffset) { builder.addOffset(1, amountOffset, 0); }
  public static int endReward(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
}

