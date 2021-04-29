package com.computernerd1101.goban.internal

import java.util.function.*

enum class BinOp: IntBinaryOperator, LongBinaryOperator {

    AND {
        override fun applyAsInt(a: Int, b: Int): Int = a and b
        override fun applyAsLong(a: Long, b: Long) = a and b
    },
    OR {
        override fun applyAsInt(a: Int, b: Int): Int = a or b
        override fun applyAsLong(a: Long, b: Long) = a or b
    },
    XOR {
        override fun applyAsInt(a: Int, b: Int): Int = a xor b
        override fun applyAsLong(a: Long, b: Long) = a xor b
    }

}