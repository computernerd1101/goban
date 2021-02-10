package com.computernerd1101.goban.desktop.internal

import com.computernerd1101.goban.desktop.resources.gobanDesktopResources
import java.util.*
import java.util.function.IntBinaryOperator

internal object InternalMarker {

    fun ignore() = Unit

}

inline fun <reified K: Enum<K>, V> enumMap() = EnumMap<K, V>(K::class.java)

inline fun localeToString(crossinline block: Any.(ResourceBundle) -> String): Any = object {

    override fun toString(): String {
        return block(gobanDesktopResources())
    }

}

enum class IntBinOp: IntBinaryOperator {

    AND {
        override fun applyAsInt(a: Int, b: Int) = a and b
    },
    OR {
        override fun applyAsInt(a: Int, b: Int) = a or b
    }

}
