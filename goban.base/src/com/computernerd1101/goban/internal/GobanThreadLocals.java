package com.computernerd1101.goban.internal;

public final class GobanThreadLocals extends ThreadLocal<long[][]> {

    private GobanThreadLocals() { }

    private static final GobanThreadLocals arrays = new GobanThreadLocals();

    public static long[][] arrays() {
        return arrays.get();
    }

    // 5 arrays of 52 longs each
    @Override
    protected long[][] initialValue() {
        // I could have done this in Kotlin with Array(5) { LongArray(52) },
        // but then the compiler would have generated more opcodes than multianewarray.
        // Since this method is called once per thread, or possibly more depending
        // on garbage collection, I wanted it to return as quickly as possible each time.
        // The implementation with more opcodes probably wouldn't take that much more time,
        // but there's a faster option available (which also takes up less space in compiled code),
        // so I'll take it.
        return new long[5][52];
    }
}
