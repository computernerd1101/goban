@file:JvmMultifileClass
@file:JvmName("GobanKt")
@file:Suppress("unused")

package com.computernerd1101.goban

import com.computernerd1101.goban.internal.*
import com.computernerd1101.goban.resources.GoPointFormatter
import com.computernerd1101.goban.resources.GobanDimensionFormatter
import com.computernerd1101.goban.resources.gobanFormatResources
import com.computernerd1101.sgf.SGFBytes
import java.io.*
import java.util.*
import kotlin.NoSuchElementException

fun GoPoint(x: Int, y: Int) = GoPoint.pointAt(x, y)

fun String.toGoPoint() = GoPoint.parse(this) ?: throw IllegalArgumentException(this)
fun String.toGoPointOrNull() = GoPoint.parse(this)
fun Int.toGoPointChar() = GoPoint.toChar(this)
fun Char.toGoPointInt() = GoPoint.parseChar(this)
fun GoPoint?.formatOrPass(x: Int, y: Int, locale: Locale): String = GoPoint.format(this, x, y, locale)
fun GoPoint?.formatOrPass(x: Int, y: Int): String = GoPoint.format(this, x, y)
fun Int.gtpFormatX() = GoPoint.gtpFormatX(this)
fun Char.gtpParseXOrThrow(width: Int) = GoPoint.gtpParseXOrThrow(this, width)
fun Char.gtpParseX(width: Int) = GoPoint.gtpParseX(this, width)

// Implement all the same methods as a data class, with the same behavior,
// except that all instances are cached.
class GoPoint private constructor(
    @JvmField val x: Int,
    @JvmField val y: Int,
    buffer: CharArray,
    cache: Cache
): Iterable<GoPoint>, Comparable<GoPoint>, Serializable {

    @Transient private val string: String

    @Transient
    @get:JvmName("selfRect")
    val selfRect: GoRectangle

    infix fun rect(other: GoPoint) = rect(other, other.x, other.y)

    /**
     * Makes the Java syntax as pretty as I could manage:
     * `GoRectangle rect = pointAt(x1, y1).rect(x2, y2);`
     */
    fun rect(x: Int, y: Int) = rect(null, x, y)

    private fun rect(other: GoPoint?, x2: Int, y2: Int): GoRectangle {
        val (x1, y1) = this
        val start: GoPoint
        val end: GoPoint
        when {
            x1 == x2 && y1 == y2 -> return selfRect
            y1 <= y2 -> if (x1 <= x2) {
                start = this
                end   = other ?: pointAt(x2, y2)
            } else {
                start = pointAt(x2, y1)
                end   = pointAt(x1, y2)
            }
            x1 < x2 -> {
                start = pointAt(x1, y2)
                end   = pointAt(x2, y1)
            }
            else -> {
                start = other ?: pointAt(x2, y2)
                end   = this
            }
        }
        return GoRectangle(start, end, null, InternalGoRectangle)
    }

    init {
        val cx = toChar(x)
        val cy = toChar(y)
        buffer[1] = cx
        buffer[2] = cy
        string = String(buffer, 1, 2).intern()
        selfRect = GoRectangle(this, this, String(buffer).intern(), InternalGoRectangle)
        cache.points[x + y*52] = this
    }

    private object Cache {

        @JvmField val points = unsafeArrayOfNulls<GoPoint>(52*52)

    }

    companion object {

        init {
            val buffer = CharArray(4)
            buffer[0] = '['
            buffer[3] = ']'
            for(y in 0..51) {
                for(x in 0..51) {
                    GoPoint(x, y, buffer, Cache)
                }
            }
        }

        @JvmStatic
        fun pointAt(x: Int, y: Int): GoPoint {
            if (x !in 0..51) throw IndexOutOfBoundsException("x=$x is not in the range [0,52)")
            if (y !in 0..51) throw IndexOutOfBoundsException("y=$y is not in the range [0,52)")
            return Cache.points[x + y*52]
        }

        @JvmStatic
        fun nullable(x: Int, y: Int): GoPoint? {
            return if (x in 0..51 && y in 0..51) Cache.points[x + y*52] else null
        }

        @JvmStatic
        fun parse(s: String): GoPoint? = if (s.length == 2)
            nullable(parseChar(s[0]), parseChar(s[1]))
        else null

        @JvmStatic
        fun toChar(i: Int): Char {
            val offset = when(i) {
                in 0..25 -> 'a'
                in 26..51 -> 'A' - 26
                else -> return '\u0000'
            }
            return offset + i
        }

        @JvmStatic
        fun parseChar(ch: Char): Int {
            val offset = when(ch) {
                in 'a'..'z' -> 'a'
                in 'A'..'Z' ->  'A' - 26
                else -> return -1
            }
            return ch - offset
        }

        @JvmStatic
        @JvmOverloads
        fun formatX(x: Int, width: Int, locale: Locale = Locale.getDefault(Locale.Category.FORMAT)): String =
            formatDimension(x, width, locale, horizontal = true)

        @JvmStatic
        @JvmOverloads
        fun formatY(y: Int, height: Int, locale: Locale = Locale.getDefault(Locale.Category.FORMAT)): String =
            formatDimension(y, height, locale, horizontal = false)

        private fun formatDimension(index: Int, size: Int, locale: Locale, horizontal: Boolean): String {
            if (size !in 1..52) throw IllegalArgumentException(
                "${if (horizontal) "width=" else "height="}$size is not in the range [1,52]"
            )
            if (index !in 0 until size) throw IndexOutOfBoundsException(
                "${if (horizontal) "x=" else "y="}$index is not in the range [0,$size)"
            )
            return (gobanFormatResources(locale).getObject(
                if (horizontal) "GobanDimensionFormatter.X" else "GobanDimensionFormatter.Y"
            ) as GobanDimensionFormatter)
                .format(index, size)
        }

        @JvmStatic
        @JvmOverloads
        fun format(
            point: GoPoint?,
            width: Int,
            height: Int,
            locale: Locale = Locale.getDefault(Locale.Category.FORMAT)
        ): String {
            if (width !in 1..52) throw IllegalArgumentException("width=$width is not in the range [1,52]")
            if (height !in 1..52) throw IllegalArgumentException("height=$height is not in the range [1,52]")
            if (point != null) {
                val (x, y) = point
                if (x >= width) throw IndexOutOfBoundsException("x=$x is not in the range [0,$width)")
                if (y >= height) throw IndexOutOfBoundsException("y=$y is not in the range [0,$height)")
            }
            return (gobanFormatResources(locale).getObject("GoPointFormatter") as GoPointFormatter)
                .format(point, width, height)
        }

        @JvmStatic
        fun gtpFormatX(x: Int): Char = GobanDimensionFormatter.formatX(x)

        @JvmStatic
        fun gtpParse(string: String, width: Int, height: Int): GoPoint =
            gtpParse(string, width, height, throws = true)!!

        @JvmStatic
        fun gtpParseOrNull(string: String, width: Int, height: Int): GoPoint? =
            gtpParse(string, width, height, throws = false)

        @JvmStatic
        fun gtpParseXOrThrow(ch: Char, width: Int): Int = gtpParseX(ch, width, throws = true)

        @JvmStatic
        fun gtpParseX(ch: Char, width: Int): Int = gtpParseX(ch, width, throws = false)

        private fun gtpParse(string: String, width: Int, height: Int, throws: Boolean): GoPoint? {
            if (width !in 1..52) {
                if (throws) throw IllegalArgumentException("width=$width is not in the range [1,52]")
                return null
            }
            if (height !in 1..52) {
                if (throws) throw IllegalArgumentException("height=$height is not in the range [1,52]")
                return null
            }
            if (string.isEmpty()) {
                if (throws) throw IllegalArgumentException("cannot parse empty string")
                return null
            }
            val x = gtpParseX(string[0], width, throws)
            if (x < 0) return null // x will never be < 0 if throws == true
            val y: Int
            try {
                y = string.substring(1).toInt()
            } catch(e: Exception) {
                if (throws) throw e
                return null
            }
            if (y !in 1..height) {
                if (throws) throw IllegalArgumentException("y=$y is not in the range [1,$height]")
                return null
            }
            return pointAt(x, height - y)
        }

        private fun gtpParseX(ch: Char, width: Int, throws: Boolean): Int {
            val base: Char = when(ch) {
                'I', 'i' -> return when (width) {
                    51 -> 50
                    // Minimize opcodes: (ch + 1527) / 32
                    // 50 + (ch - 'I') / ('i' - 'I') = (ch - 'I' + 50*'i' - 50*'I') / ('i' - 'I')
                    52 -> (ch.toInt() + (50*'i'.toInt() - 51*'I'.toInt())) / ('i' - 'I')
                    else -> {
                        if (throws) throw IllegalArgumentException("x-coordinate 'i' illegal for width < 51")
                        -1
                    }
                }
                in 'A'..'H' -> 'A'
                in 'J'..'Z' -> 'B'
                in 'a'..'h' -> 'a' - 25
                in 'j'..'z' -> 'a' - 24
                else -> {
                    if (throws) throw IllegalArgumentException("'$ch' is not a letter")
                    return -1
                }
            }
            var x = ch - base
            if (width in 26..x)
                x -= 25
            if (x >= width) {
                if (throws) throw IllegalArgumentException(
                    "'$ch' is the ${x + 1}${
                        when(x) {         // x + 1
                            0, 20 -> "st" // 1st, 21st
                            1, 21 -> "nd" // 2nd, 22nd
                            2, 22 -> "rd" // 3rd, 23rd
                            else -> "th" // 10th, 11th, 12th, 13th, 20th, or anything else
                        }
                    } letter of the alphabet, which is out of range for width = $width"
                )
                x = -1
            }
            return x
        }

        private const val serialVersionUID = 1L

    }

    fun gtpFormat(height: Int): String {
        if (height !in 1..52) throw IllegalArgumentException("height=$height is not in the range [1,52]")
        if (y >= height) throw IndexOutOfBoundsException("y=$y is not in the range [0,$height)")
        return GoPointFormatter.format(this, 52, height)
    }

    fun format(width: Int, height: Int, locale: Locale): String = format(this, width, height, locale)
    fun format(width: Int, height: Int): String = format(this, width, height)

    override fun toString() = string

    override fun equals(other: Any?) = this === other

    override fun hashCode() = x + 52*y

    operator fun component1() = x
    operator fun component2() = y

    fun copy(x: Int = this.x, y: Int = this.y): GoPoint {
        return GoPoint(x, y)
    }

    override fun compareTo(other: GoPoint): Int {
        return (x - other.x) + (y - other.y)*52
    }

    override fun iterator(): Iterator<GoPoint> = Itr(this)

    private class Itr(point: GoPoint): Iterator<GoPoint> {

        private var next: GoPoint? = point

        override fun hasNext() = next != null

        override fun next(): GoPoint {
            val point = next ?: throw NoSuchElementException()
            next = null
            return point
        }

    }

    fun toSGFBytes(): SGFBytes {
        return SGFBytes(2)
            .append(toChar(x).toByte())
            .append(toChar(y).toByte())
    }

    private fun readResolve(): Any {
        if (x !in 0..51) throw InvalidObjectException("x=$x is not in the range [0,52)")
        if (y !in 0..51) throw InvalidObjectException("y=$y is not in the range [0,52)")
        return Cache.points[x + y*52]
    }

}