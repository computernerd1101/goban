package com.computernerd1101.goban.sgf.internal

import com.computernerd1101.goban.*
import com.computernerd1101.goban.sgf.*
import com.computernerd1101.goban.time.*
import com.computernerd1101.sgf.*
import java.io.ObjectInputStream
import java.nio.charset.Charset

internal object InternalGoSGF {

    @JvmStatic fun playStoneAt(parent: GoSGFNode, point: GoPoint?, stone: GoColor): AbstractGoban {
        val parentGoban = parent.goban
        if (point == null) return parentGoban
        val oldStone = parentGoban[point]
        if (oldStone == stone) return parentGoban
        val goban = Goban(parentGoban)
        if (oldStone != null)
            goban[point] = stone
        else if (!goban.play(point, stone))
            return parentGoban
        return goban
    }

    @JvmStatic
    fun violatesSituationalSuperko(turnPlayer: GoColor, repeatedPosition: GoSGFMoveNode, natural: Boolean): Boolean =
        turnPlayer == repeatedPosition.turnPlayer && (!natural || repeatedPosition.playStoneAt != null)

    @JvmStatic fun parseSGFValue(value: SGFValue, charset: Charset?, warnings: SGFWarningList?): String =
        parseSGFBytesList(value.row, value.column, value.parts, charset, warnings)

    @JvmStatic fun parseSGFBytesList(
        row: Int, column: Int, list: List<SGFBytes>,
        charset: Charset?, warnings: SGFWarningList?
    ): String {
        val buf: SGFBytes = if (list.size == 1) list[0]
        else SGFBytes().apply {
            var colon = false
            for(bytes in list) {
                if (colon) append(':'.code.toByte())
                append(bytes)
                colon = true
            }
        }
        return if (charset == null) buf.toString()
        else try {
            buf.toString(charset)
        } catch(e: RuntimeException) {
            warnings?.addWarning(SGFWarning(row, column, e))
            buf.toString()
        }
    }

    @JvmStatic fun parsePoint(bytes: SGFBytes?, ignoreCase: Boolean): GoPoint? {
        if (bytes?.size != 2) return null
        var ch = bytes[0].toInt().toChar()
        var offset = when {
            ch in 'a'..'z' -> 'a'
            ch !in 'A'..'Z' -> return null
            ignoreCase -> 'A'
            else -> 'A' - 26
        }
        val x = ch - offset
        ch = bytes[1].toInt().toChar()
        offset = when {
            ch in 'a'..'z' -> 'a'
            ch !in 'A'..'Z' -> return null
            ignoreCase -> 'A'
            else -> 'A' - 26
        }
        val y = ch - offset
        return GoPoint(x, y)
    }

    @JvmStatic fun malformedPoint(s: String) = "[$s] does not represent a point on a Go board"

    @JvmStatic fun pointOutOfRange(s: String, x: Int, y: Int, width: Int, height: Int): String {
        return "[$s] ($x,$y) is outside the boundaries ${width}x$height"
    }

}

internal fun GoSGFMoveNode.PlayerTime.parseTimeRemaining(time: SGFProperty?, overtime: SGFProperty?) {
    val warnings = node.tree.warnings
    var bytes: SGFBytes
    var s: String
    if (time != null) {
        bytes = time.values[0].parts[0]
        s =  bytes.toString()
        try {
            this.time = s.secondsToMillis()
        } catch(e: NumberFormatException) {
            warnings += SGFWarning(
                bytes.row, bytes.column, "Unable to parse remaining time ${
                if (color == GoColor.BLACK) 'B' else 'W'
                }L[$s]: $e", e
            )
        }
    }
    if (overtime != null) {
        bytes = overtime.values[0].parts[0]
        s = bytes.toString()
        try {
            this.overtime = s.toInt()
        } catch(e: NumberFormatException) {
            warnings += SGFWarning(
                bytes.row, bytes.column, "Unable to parse overtime O${
                if (color == GoColor.BLACK) 'B' else 'W'
                }[$s]: $e", e
            )
        }
    }
}

internal fun GoSGFMoveNode.PlayerTime.writeSGFTime(
    map: MutableMap<String, SGFProperty>,
    timeProp: String,
    overtimeProp: String
) {
    if (hasTime)
        map[timeProp] = SGFProperty(SGFValue(SGFBytes(time.millisToStringSeconds())))
    if (hasOvertime)
        map[overtimeProp] = SGFProperty(SGFValue(SGFBytes(overtime.toString())))
}

internal fun ObjectInputStream.GetField.getString(name: String): String {
    return try {
        this[name, null]?.toString()
    } catch(e: Exception) {
        null
    } ?: ""
}

internal class ThreadLocalGoban(
    @JvmField val width: Int,
    @JvmField val height: Int
) {

    private inner class Local: ThreadLocal<Goban>() {

        override fun initialValue() = Goban(width, height)

    }

    private val local = Local()

    val goban: Goban get() = local.get()

}