@file:Suppress("ClassName")

package com.computernerd1101.goban.resources

import com.computernerd1101.goban.*

internal fun interface GoPointFormatter {

    fun format(point: GoPoint?, width: Int, height: Int): String

    // GTP = Go Text Protocol
    companion object GTP: GoPointFormatter {

        override fun format(point: GoPoint?, width: Int, height: Int): String {
            if (point == null) return "Pass"
            val y = height - point.y
            return when {
                y !in 1..52 -> "${point.x.gtpFormatX()}$y"
                height <= 52 -> point.gtpFormat(height)
                else -> GoPoint(point.x, 52 - y).gtpFormat(52)
            }
        }

    }

}

internal object GoPointFormatter_ja: GoPointFormatter {

    private val cache: Array<String> = Array(52 * 52) { index ->
        (GobanDimensionFormatter_ja.Y.format(index / 52, 52) +
                '\u306e' + // hiragana "no" (possessive particle)
                GobanDimensionFormatter_ja.X.format(index % 52, 52)).intern()
    }

    @Suppress("SpellCheckingInspection")
    override fun format(point: GoPoint?, width: Int, height: Int): String =
        if (point == null) "\u30d1\u30b9" // katakana "pasu" (pass)
        else cache[point.x + point.y*52]

}