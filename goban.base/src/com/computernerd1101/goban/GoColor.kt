@file:JvmMultifileClass
@file:JvmName("GobanKt")

package com.computernerd1101.goban

import com.computernerd1101.goban.resources.gobanResources
import java.util.*

fun Boolean.goBlackOrWhite() = if (this) GoColor.BLACK else GoColor.WHITE

enum class GoColor {

    BLACK, WHITE;

    val opponent : GoColor
        @JvmName("opponent")
        get() = if (this == BLACK) WHITE else BLACK

    inline val isBlack : Boolean
        get() = this == BLACK

    override fun toString() = toString(Locale.getDefault())

    fun toString(locale: Locale): String {
        val resources = gobanResources(locale)
        return resources.getStringArray("GoColor")[ordinal]
    }

    companion object {

        @JvmStatic
        fun valueOf(isBlack: Boolean?): GoColor? = isBlack?.goBlackOrWhite()

    }

}