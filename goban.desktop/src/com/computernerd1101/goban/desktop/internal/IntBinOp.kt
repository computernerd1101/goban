package com.computernerd1101.goban.desktop.internal

import java.util.function.IntBinaryOperator

enum class IntBinOp: IntBinaryOperator {

    AND {
        override fun applyAsInt(a: Int, b: Int) = a and b
    },
    OR {
        override fun applyAsInt(a: Int, b: Int) = a or b
    }

}