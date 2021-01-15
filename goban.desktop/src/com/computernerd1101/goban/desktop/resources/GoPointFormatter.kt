package com.computernerd1101.goban.desktop.resources

import com.computernerd1101.goban.GoPoint

fun interface GoPointFormatter {

    fun format(point: GoPoint?, width: Int, height: Int): String

    companion object Default: GoPointFormatter {

        override fun format(point: GoPoint?, width: Int, height: Int): String {
            if (point == null) return "Pass"
            val ch = when(val x = point.x) {
                in 0..7 -> 'A' + x
                in 8..24 -> 'B' + x
                in 25..32 -> ('a' - 25) + x
                in 33..49 -> ('a' - 24) + x
                50 -> 'I'
                51 -> 'i'
                else -> '\u0000'
            }
            return "$ch${height - point.y}"
        }

    }

}