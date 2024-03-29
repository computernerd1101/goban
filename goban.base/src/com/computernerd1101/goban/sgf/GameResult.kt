package com.computernerd1101.goban.sgf

import com.computernerd1101.goban.GoColor
import com.computernerd1101.goban.internal.InternalMarker
import java.io.*

fun GameResult(winner: GoColor?, score: Float): GameResult {
    return GameResult.result(winner, score)
}


@Suppress("unused")
fun GameResult(s: String): GameResult {
    return GameResult.parse(s)
}

class GameResult private constructor(
    @Transient private var code: Int,
    @Transient private var string: String
): Comparable<GameResult>, Serializable {

    companion object {

        private const val BLACK_WINS =  1 shl 24
        private const val WHITE_WINS = -1 shl 24

        @JvmField val WHITE_UNKNOWN = GameResult(WHITE_WINS, "W+")
        @JvmField val WHITE_TIME = GameResult(WHITE_WINS or ('T'.code shl 16), "W+T")
        @JvmField val WHITE_RESIGN = GameResult(WHITE_WINS or ('R'.code shl 16), "W+R")
        @JvmField val WHITE_FORFEIT = GameResult(WHITE_WINS or ('F'.code shl 16), "W+F")
        @JvmField val DRAW = GameResult(0, "0")
        @JvmField val UNKNOWN = GameResult('?'.code shl 16, "?")
        @JvmField val VOID = GameResult('V'.code shl 16, "Void")
        @JvmField val BLACK_FORFEIT = GameResult(BLACK_WINS or ('F'.code shl 16), "B+F")
        @JvmField val BLACK_RESIGN = GameResult(BLACK_WINS or ('R'.code shl 16), "B+R")
        @JvmField val BLACK_TIME = GameResult(BLACK_WINS or ('T'.code shl 16), "B+T")
        @JvmField val BLACK_UNKNOWN = GameResult(BLACK_WINS, "B+")

        @JvmStatic fun time(winner: GoColor): GameResult =
            if (winner == GoColor.BLACK) BLACK_TIME else WHITE_TIME
        @JvmStatic fun resign(winner: GoColor): GameResult =
            if (winner == GoColor.BLACK) BLACK_RESIGN else WHITE_RESIGN
        @JvmStatic fun forfeit(winner: GoColor): GameResult =
            if (winner == GoColor.BLACK) BLACK_FORFEIT else WHITE_FORFEIT

        @JvmStatic
        fun result(winner: GoColor?, score: Float): GameResult {
            if (winner == null) return DRAW
            val realWinner: GoColor
            val abs: Float
            if (score < 0f) {
                realWinner = winner.opponent
                abs = -score
            } else {
                realWinner = winner
                abs = score
            }
            var code = 0
            var s = ""
            if (abs > 0f && abs < 0x8000) {
                val i = abs.toInt()
                code = i shl 1
                s = if (i.toFloat() == abs)
                    i.toString()
                else {
                    code++
                    (code*0.5f).toString()
                }
            }
            if (code == 0)
                return (if (realWinner == GoColor.BLACK) BLACK_UNKNOWN else WHITE_UNKNOWN)
            if (realWinner == GoColor.BLACK) {
                code = code or BLACK_WINS
                s = "B+$s"
            } else {
                code = code or WHITE_WINS
                s = "W+$s"
            }
            return GameResult(code, s, InternalMarker)
        }

        @JvmStatic
        fun parse(s: String): GameResult = nullable(s) ?: UNKNOWN

        @JvmStatic
        @JvmName("parseNullable")
        fun nullable(s: String): GameResult? {
            if (s.isEmpty()) return null
            var ch = s[0]
            val prefix: String
            val table: Array<GameResult?>
            val unknown: GameResult?
            val color: Int
            when(ch) {
                'B', 'b' -> {
                    prefix = "B+"
                    table = Tables.blackWins
                    unknown = BLACK_UNKNOWN
                    color = BLACK_WINS
                }
                'W', 'w' -> {
                    prefix = "W+"
                    table = Tables.whiteWins
                    unknown = WHITE_UNKNOWN
                    color = WHITE_WINS
                }
                // even though noWinnerTable[0] == DRAW,
                // only accept the following characters for DRAW:
                // '0' == '\u0030'
                // 'D' == '\u0044'
                // 'd' == '\u0064'
                '\u0000' -> return null
                else -> {
                    prefix = ""
                    table = Tables.noWinner
                    unknown = null
                    color = 0
                }
            }
            var i = 0
            if (color != 0) {
                if (s.length == 1) return unknown
                ch = s[1]
                if (ch == '+') {
                    if (s.length == 2) return unknown
                    i = 2
                    ch = s[2]
                } else i = 1
            }
            table[ch.code]?.let { return it }
            if (color != 0) {
                var score = 0
                loop@ while (i < s.length) {
                    ch = s[i]
                    if (ch == '.') while (true) {
                        if (++i >= s.length) break@loop
                        ch = s[i]
                        if (ch in '1'..'9') {
                            score++
                            break@loop
                        }
                        if (ch != '0') break@loop
                    }
                    if (ch !in '0'..'9') break
                    score = score * 10 + ((ch - '0') shl 1)
                    if (score >= 0x10000) {
                        score = 0
                        break
                    }
                    i++
                }
                if (score != 0)
                    return GameResult(color or score,
                        prefix + if (score and 1 == 0) (score shr 1).toString() else (score*0.5f).toString(),
                        InternalMarker)
            }
            return unknown
        }

        private const val serialVersionUID: Long = 1L

    }

    internal constructor(code: Int, string: String, marker: InternalMarker): this(code, string) {
        marker.ignore()
    }

    private object Tables {

        @JvmField val noWinner = makeTable(DRAW, UNKNOWN, VOID)
        @JvmField val blackWins = makeTable(BLACK_FORFEIT, BLACK_RESIGN, BLACK_TIME)
        @JvmField val whiteWins = makeTable(WHITE_FORFEIT, WHITE_RESIGN, WHITE_TIME)

        @JvmStatic
        private fun makeTable(vararg results: GameResult): Array<GameResult?> {
            val table = arrayOfNulls<GameResult>(0x100)
            for (result in results) {
                val ch = (result.code shr 16) and 0xFF
                table[ch] = result
                when (ch) {
                    0 -> { // draw
                        table['0'.code] = result
                        table['D'.code] = result
                        table['d'.code] = result
                    }
                    in 'A'.code..'Z'.code -> table[ch + ('a' - 'A')] = result
                    in 'a'.code..'z'.code -> table[ch - ('a' - 'A')] = result
                }
            }
            return table
        }

    }

    val opposite: GameResult
        @JvmName("opposite")
        get() {
            var code = this.code
            var color = code and WHITE_WINS
            if (color == 0) return this
            color = -color
            code = color or (code and 0xFFFFFF)
            val table: Array<GameResult?>
            val unknown: GameResult
            val prefix: String
            if (color > 0) {
                table = Tables.blackWins
                unknown = BLACK_UNKNOWN
                prefix = "B+"
            } else {
                table = Tables.whiteWins
                unknown = WHITE_UNKNOWN
                prefix = "W+"
            }
            return if (code and 0xFFFF != 0) GameResult(code, prefix + string.substring(2))
            else table[(code shr 16) and 0xFF] ?: unknown
        }

    val winner: GoColor?
        @JvmName("winner")
        get() {
            val color = code shr 24 // intentionally signed
            return when {
                color == 0 -> null
                color > 0 -> GoColor.BLACK
                else -> GoColor.WHITE
            }
        }

    val score: Float
        @JvmName("score")
        get() = if ((code shr 16) and 0xFF == 0) (code and 0xFFFF)*0.5f
        else Float.NaN

    val charCode: Char
        @JvmName("charCode")
        get() = when {
            code == 0 -> '0'
            code and 0xFFFF != 0 -> '#'
            code and 0xFFFFFF == 0 -> '+'
            else -> ((code shr 16) and 0xFF).toChar()
        }

    override fun compareTo(other: GameResult): Int {
        if (this === other) return 0
        val code1: Int = code
        val code2: Int = other.code
        if (code1 == code2) return 0
        val color1 = code1 shr 24
        val color2 = code2 shr 24
        // All no-winner results are greater than
        // all white victory results, but less than
        // all black victory results.
        if (color1 > color2) return 1
        if (color1 < color2) return -1
        if (color1 == 0) return if (code1 and 0xFFFFFF > code2 and 0xFFFFFF) 1 else -1
        // Sort white victory results in reverse order
        // from their black victory equivalents.
        // I said "bigger" in the sortableCode() comments
        // instead of "greater" for this reason.
        return if (sortableCode() > other.sortableCode()) color1 else -color1
    }

    private fun sortableCode(): Int {
        val code = this.code and 0xFFFFFF
        // Numerically scored winner results are
        // "bigger" than character-coded winner results.
        // Rearrange significant figures accordingly.
        //     numeric score              1 iff the whole thing is 0     character code
        return (code and 0xFFFF shl 9) or (code - 1 shr 31 and 0x100) or (code ushr 16)
    }

    override fun equals(other: Any?) = this === other || (other is GameResult && code == other.code)

    override fun hashCode() = code

    override fun toString() = string

    private fun writeObject(oos: ObjectOutputStream) {
        oos.writeShort(code shr 16)
        if (code shr 24 != 0 && (code shr 16) and 0xFF == 0)
            oos.writeFloat((code and 0xFFFF) * 0.5f)
    }

    private fun readObject(ois: ObjectInputStream) {
        var code = ois.readShort().toInt()
        var color = code shr 8
        val ch = code and 0xFF
        if (color > 1) {
            code = 0x100 or ch
            color = 1
        } else if (color < -1) {
            code = -0x100 or ch
            color = -1
        }
        code = code shl 16
        if (color != 0 && ch == 0) {
            var score = ois.readFloat()
            if (score < 0f) {
                code = -code
                color = -color
                score = -score
            }
            var i2 = 0
            if (score < 0x8000) {
                val i1 = score.toInt()
                i2 = i1 shl 1
                if (i1.toFloat() != score) i2++
            }
            if (i2 != 0) {
                code = code or i2
                string = (if (color > 0) "B+" else "W+") +
                        (if (i2 and 1 == 0) (i2 shr 1).toString() else (i2 * 0.5f).toString())
            } // else readResolve() will return a pre-instantiated object with a pre-initialized string
        }
        this.code = code
    }

    private fun readResolve(): Any {
        val color = code shr 24
        val ch = (code shr 16) and 0xFF
        val table: Array<GameResult?>
        val unknown: GameResult
        when {
            color == 0 -> {
                table = Tables.noWinner
                unknown = UNKNOWN
            }
            ch == 0 && code and 0xFFFF != 0 -> return this
            color > 0 -> {
                table = Tables.blackWins
                unknown = BLACK_UNKNOWN
            }
            else -> {
                table = Tables.whiteWins
                unknown = WHITE_UNKNOWN
            }
        }
        return table[ch] ?: unknown
    }

}