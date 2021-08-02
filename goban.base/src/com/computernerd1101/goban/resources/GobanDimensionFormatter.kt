@file:Suppress("ClassName")

package com.computernerd1101.goban.resources

import com.computernerd1101.goban.gtpFormatX

internal interface GobanDimensionFormatter {

    fun format(index: Int, size: Int): String

    companion object {
        @JvmField val X = Default.X
        @JvmField val Y = Default.Y

        @JvmField val LETTERS: Array<String> = Array(52) { x -> "${x.gtpFormatX()}".intern() }
        @JvmField val NUMBERS: Array<String> = Array(52) { y -> "${y + 1}".intern() }

    }

    enum class Default: GobanDimensionFormatter {
        X {
            override fun format(index: Int, size: Int): String =
                if (index in 0..51) LETTERS[index] else ""
        },
        Y {
            override fun format(index: Int, size: Int): String {
                val y = size - index
                return if (y in 1..52) NUMBERS[y - 1] else "$y"
            }
        }
    }

}

@Suppress("SpellCheckingInspection")
//                                1     2     3     4     5     6     7     8     9
//                                ichi  ni    san   yon   go    roku  nana  hachi kyuu
private const val KANJI_DIGITS = "\u4e00\u4e8c\u4e09\u56db\u4e94\u516d\u4e03\u516b\u4e5d"

internal enum class GobanDimensionFormatter_ja: GobanDimensionFormatter {

    X {
        override fun format(index: Int, size: Int): String =
            if (index in 0..51) GobanDimensionFormatter.NUMBERS[index] else "${index + 1}"
    },
    Y {

        private val kanji: Array<String> = CharArray(3).let { buffer ->
            buffer[1] = '\u5341' // juu = 10
            Array(99) { y ->
                val start = when {
                    y >= 19 -> { // y + 1 >= 20
                        buffer[0] = KANJI_DIGITS[(y - 9) / 10] // (y + 1)/10 - 1
                        0
                    }
                    y >= 9 -> 1 // y + 1 >= 10
                    else -> 2
                }
                val end = when(val digit = (y + 1) % 10) {
                    0 -> 2
                    else -> {
                        buffer[2] = KANJI_DIGITS[digit - 1]
                        3
                    }
                }
                buffer.concatToString(start, end).intern()
            }
        }

        override fun format(index: Int, size: Int): String =
            if (index in 0..98) kanji[index] else "${index + 1}"
    };

}