package com.computernerd1101.goban.internal

import java.util.function.*

enum class IntBinOp: IntBinaryOperator {

    AND {
        override fun applyAsInt(a: Int, b: Int) = a and b
    },
    OR {
        override fun applyAsInt(a: Int, b: Int) = a or b
    }

}

enum class LongBinOp: LongBinaryOperator {

    AND {
        override fun applyAsLong(a: Long, b: Long) = a and b
    },
    OR {
        override fun applyAsLong(a: Long, b: Long) = a or b
    },
    XOR {
        override fun applyAsLong(a: Long, b: Long) = a xor b
    }

}