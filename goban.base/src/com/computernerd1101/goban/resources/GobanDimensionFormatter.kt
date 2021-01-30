@file:Suppress("ClassName")

package com.computernerd1101.goban.resources

interface GobanDimensionFormatter {

    fun format(index: Int, size: Int): String

    companion object {
        @JvmField val X: GobanDimensionFormatter = Default.X
        @JvmField val Y: GobanDimensionFormatter = Default.Y
    }

    private enum class Default: GobanDimensionFormatter {
        X {
            override fun format(index: Int, size: Int): String {
                val offset = when(index) {
                    in 0..7 -> 'A'
                    in 8..24 -> 'B'
                    in 25..32 -> 'a' - 25
                    in 33..49 -> 'a' - 24
                    50 -> return "I"
                    51 -> return "i"
                    else -> return ""
                }
                return "${offset + index}"
            }
        },
        Y {
            override fun format(index: Int, size: Int) = "${size - index}"
        }
    }

}

enum class GobanDimensionFormatter_ja: GobanDimensionFormatter {

    X {
        override fun format(index: Int, size: Int) = "${index + 1}"
    },
    Y {
        override fun format(index: Int, size: Int): String {
            if (index !in 0..98) return "" // index + 1 !in 1..99
            val buffer: CharArray = Kanji.get()
            val start = when {
                index >= 19 -> { // index + 1 >= 20
                    buffer[0] = Kanji.DIGITS[(index - 9) / 10] // (index + 1)/10 - 1
                    0
                }
                index >= 9 -> 1 // index + 1 >= 10
                else -> 2
            }
            val end = when(val digit = (index + 1) % 10) {
                0 -> 2
                else -> {
                    buffer[2] = Kanji.DIGITS[digit - 1]
                    3
                }
            }
            return String(buffer, start, end - start)
        }
    };
    private object Kanji: ThreadLocal<CharArray>() {

        //                   1     2     3     4     5     6     7     8     9
        const val DIGITS = "\u4e00\u4e8c\u4e09\u56db\u4e94\u516d\u4e03\u516b\u4e5d"

        override fun initialValue() = CharArray(3).apply {
            this[1] = '\u5341' // 10
        }

    }

}