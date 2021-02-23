package com.computernerd1101.goban.internal;

import org.jetbrains.annotations.NotNull;

import java.util.function.LongBinaryOperator;

public final class GobanThreadLocals extends ThreadLocal<long[][]> implements LongBinaryOperator {

    private GobanThreadLocals() { }

    @Override
    // 7 arrays of 52 longs each
    protected long[][] initialValue() {
        // I could have done this in Kotlin with Array(7) { LongArray(52) },
        // but then the compiler would have generated more opcodes than multianewarray.
        // Since this method is called once per thread, or possibly more depending
        // on garbage collection, I wanted it to return as quickly as possible each time.
        // The implementation with more opcodes probably wouldn't take that much more time,
        // but there's a faster option available (which also takes up less space in compiled code),
        // so I'll take it. Two birds, one stone.
        return new long[7][52];
    }

    public static final @NotNull GobanThreadLocals INSTANCE = new GobanThreadLocals();
    
    public static final int GROUP = 0;
    public static final int CHAIN = 1;
    public static final int PENDING = 2;
    public static final int BLACK = 3;
    public static final int WHITE = 4;
    public static final int BLACK_SCORE = 5;
    public static final int WHITE_SCORE = 6;

    @Override
    public long applyAsLong(long row, long pos) {
        long[][] arrays = get();
        int y = (int)(pos >> 32);
        int shift = (int)pos;
        long mask = (arrays[GROUP][y] >>> shift) & 0xFFFF_FFFFL;
        return (row & ~(mask * 0x1_0000_0001L)) |
                ((arrays[BLACK][y] >>> shift) & mask) |
                ((arrays[WHITE][y] & (mask << shift)) << (32 - shift));
    }
}
