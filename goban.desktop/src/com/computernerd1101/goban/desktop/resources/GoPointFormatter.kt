@file:Suppress("ClassName")

package com.computernerd1101.goban.desktop.resources

import com.computernerd1101.goban.GoPoint

fun interface GoPointFormatter {

    fun format(point: GoPoint?, width: Int, height: Int): String

    companion object Default: GoPointFormatter {

        override fun format(point: GoPoint?, width: Int, height: Int): String {
            return if (point == null) "Pass"
            else GobanDimensionFormatter.X.format(point.x, width) +
                    GobanDimensionFormatter.Y.format(point.y, height)
        }

    }

}

object GoPointFormatter_ja: GoPointFormatter {

    override fun format(point: GoPoint?, width: Int, height: Int): String {
        return if (point == null) "\u30d1\u30b9" // katakana "pasu" (pass)
        else GobanDimensionFormatter_ja.Y.format(point.y, height) +
                "\u306e" + // hiragana "no" (possessive particle)
                GobanDimensionFormatter_ja.X.format(point.x, width)
    }

}