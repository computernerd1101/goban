package com.computernerd1101.goban.desktop.internal

import java.util.*
import java.util.function.IntBinaryOperator

internal object InternalMarker {

    fun ignore() = Unit

}

inline fun <reified K: Enum<K>, V> enumMap() = EnumMap<K, V>(K::class.java)

enum class IntBinOp: IntBinaryOperator {

    AND {
        override fun applyAsInt(a: Int, b: Int) = a and b
    },
    OR {
        override fun applyAsInt(a: Int, b: Int) = a or b
    }

}

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
typealias BoxedDouble = java.lang.Double

object Zero {

    @JvmField val plus = 0.0 as BoxedDouble
    @JvmField val minus = -0.0 as BoxedDouble

}