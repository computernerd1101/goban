@file:JvmMultifileClass
@file:JvmName("GobanKt")

package com.computernerd1101.goban

import com.computernerd1101.goban.internal.*
import com.computernerd1101.goban.resources.*
import com.computernerd1101.sgf.SGFBytes
import java.io.*
import java.util.*
import kotlin.NoSuchElementException

fun GoPoint(x: Int, y: Int) = GoPoint.pointAt(x, y)

fun String.toGoPoint(): GoPoint = GoPoint.parse(this) ?: throw IllegalArgumentException(this)
fun String.toGoPointOrNull(): GoPoint? = GoPoint.parse(this)
fun Int.toGoPointChar(): Char = GoPoint.toChar(this)
fun Char.toGoPointInt(): Int = GoPoint.parseChar(this)
fun GoPoint?.formatOrPass(width: Int, height: Int, locale: Locale): String =
    GoPoint.format(this, width, height, locale)
fun GoPoint?.formatOrPass(height: Int, width: Int): String = GoPoint.format(this, width, height)
fun String.gtpParse(width: Int, height: Int): GoPoint = GoPoint.gtpParse(this, width, height)
fun String?.gtpParseOrNull(width: Int, height: Int): GoPoint? = GoPoint.gtpParseOrNull(this, width, height)
fun Int.gtpFormatX(): Char = GoPoint.gtpFormatX(this)
fun Char.gtpParseXOrThrow(width: Int): Int = GoPoint.gtpParseXOrThrow(this, width)
fun Char.gtpParseX(width: Int): Int = GoPoint.gtpParseX(this, width)

// Implement all the same methods as a data class, with the same behavior,
// except that all instances are cached.
class GoPoint private constructor(
    @JvmField val x: Int,
    @JvmField val y: Int,
    buffer: CharArray,
    cache: Cache
): Iterable<GoPoint>, Comparable<GoPoint>, Serializable {

    @Transient private val string: String = buffer.concatToString(10, 12).intern()
    @Transient private val gtp: String = buffer.concatToString(13, if (y < 9) 15 else 16).intern()

    @Transient
    @get:JvmName("selfRect")
    val selfRect: GoRectangle

    @Transient
    private val fromZeroRect: GoRectangle

    infix fun rect(other: GoPoint): GoRectangle = rect(other, other.x, other.y)

    /**
     * Makes the Java syntax as pretty as I could manage:
     * `GoRectangle rect = GoPoint.pointAt(x1, y1).rect(x2, y2);`
     */
    fun rect(x: Int, y: Int): GoRectangle = rect(null, x, y)

    private fun rect(other: GoPoint?, x2: Int, y2: Int): GoRectangle {
        val (x1, y1) = this
        val start: GoPoint
        val end: GoPoint
        when {
            x1 == x2 && y1 == y2 -> return selfRect
            x1 == 0 && y1 == 0 -> return (other ?: pointAt(x2, y2)).fromZeroRect
            x2 == 0 && y2 == 0 -> return fromZeroRect
            x1 == 0 && y2 == 0 -> return pointAt(x2, y1).fromZeroRect
            x2 == 0 && y1 == 0 -> return pointAt(x1, y2).fromZeroRect
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
        buffer[9] = '['
        selfRect = GoRectangle(this, this,
            buffer.concatToString(9, 13).intern(), InternalGoRectangle)
        if (x == 0 && y == 0) {
            fromZeroRect = selfRect
        } else {
            val start: Int
            if ((x == 0 && y == 1) || (x == 1 && y == 0)) {
                buffer[5] = '['
                buffer[6] = 'a'
                buffer[7] = 'a'
                start = 5
            } else {
                buffer[5] = '.'
                buffer[6] = '.'
                buffer[7] = '.'
                start = 0
            }
            buffer[9] = ' '
            fromZeroRect = GoRectangle(cache.points[0], this,
                buffer.concatToString(start, 13).intern(), InternalGoRectangle)
        }
        cache.points[x + y*52] = this
    }

    private object Cache {

        @JvmField val points = arrayOfLateInit<GoPoint>(52*52)

    }

    companion object {

        init {
            val buffer = CharArray(16)
            buffer[0] = '['
            buffer[1] = 'a'
            buffer[2] = 'a'
            buffer[3] = ','
            buffer[4] = ' '
            // buffer[5] = '.' or '['
            // buffer[6] = '.' or 'a'
            // buffer[7] = '.' or 'a'
            buffer[8] = ','
            // buffer[9] = ' ' or '['
            // buffer[10] = toChar(x)
            // buffer[11] = toChar(y)
            buffer[12] = ']'
            for(y in 0..51) {
                buffer[11] = toChar(y)
                if (y < 9)
                    buffer[14] = '1' + y
                else {
                    // buffer[14] =  '0' + (1 + y) / 10
                    //            =  (48 + (1 + y) / 10).toChar()
                    //            = ((480 + 1 + y) / 10).toChar()
                    buffer[14] = ((481 + y) / 10).toChar()
                    buffer[15] = '0' + (1 + y) % 10
                }
                for(x in 0..51) {
                    buffer[10] = toChar(x)
                    buffer[13] = gtpFormatX(x)
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
                else -> return Char.MIN_VALUE
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
        fun gtpFormatX(x: Int): Char {
            val base: Char = when(x) {
                in 0..7 -> 'A'
                in 8..24 -> 'B'
                in 25..32 -> 'a' - 25
                in 33..49 -> 'a' - 24
                50 -> return 'I'
                51 -> return 'i'
                else -> return Char.MIN_VALUE
            }
            return base + x
        }

        @JvmStatic
        fun gtpParse(string: String, width: Int, height: Int): GoPoint =
            string.gtpParse(width, height, throws = true)!!

        @JvmStatic
        fun gtpParseOrNull(string: String?, width: Int, height: Int): GoPoint? =
            string?.gtpParse(width, height, throws = false)

        @JvmStatic
        fun gtpParseXOrThrow(ch: Char, width: Int): Int {
            if (width !in 1..52) throw IllegalArgumentException("width=$width is not in the range [1,52]")
            return gtpParseX(ch, width, throws = true)
        }

        @JvmStatic
        fun gtpParseX(ch: Char, width: Int): Int =
            if (width in 1..52) gtpParseX(ch, width, throws = false) else -1

        private fun String.gtpParse(width: Int, height: Int, throws: Boolean): GoPoint? {
            if (width !in 1..52) {
                if (throws) throw IllegalArgumentException("width=$width is not in the range [1,52]")
                return null
            }
            if (height !in 1..52) {
                if (throws) throw IllegalArgumentException("height=$height is not in the range [1,52]")
                return null
            }
            if (isEmpty()) {
                if (throws) throw IllegalArgumentException("cannot parse empty string")
                return null
            }
            val x = gtpParseX(this[0], width, throws)
            if (x < 0) return null // x will never be < 0 if throws == true
            val y: Int
            try {
                y = substring(1).toInt()
            } catch(e: Exception) {
                if (throws) throw e
                return null
            }
            if (y !in 1..height) {
                if (throws) throw IllegalArgumentException("Y=$y is not in the range [1,$height]")
                return null
            }
            return Cache.points[x + 52*(height - y)]
        }

        private fun gtpParseX(ch: Char, width: Int, throws: Boolean): Int {
            val base: Char = when(ch) {
                'I' -> return when {
                    width >= 51 -> 50
                    throws -> throw IllegalArgumentException("X=51 ('I') is not in the range [1,$width]")
                    else -> -1
                }
                'i' -> return when {
                    width >= 51 -> width - 1
                    throws -> throw IllegalArgumentException("X=52 ('i') is not in the range [1,$width]")
                    else -> -1
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
            val x1 = x + 1
            if (x >= width && x >= 25)
                x -= 25
            if (x >= width) {
                if (throws) throw IllegalArgumentException("X=$x1 ('$ch') is not in the range [1,$width]")
                x = -1
            }
            return x
        }

        private const val serialVersionUID = 1L

    }

    fun gtpFormat(height: Int): String {
        if (height !in 1..52) throw IllegalArgumentException("height=$height is not in the range [1,52]")
        if (y >= height) throw IndexOutOfBoundsException("y=$y is not in the range [0,$height)")
        val y2 = height - 1 - y
        return (if (y2 == y) this else Cache.points[x + y2*52]).gtp
    }

    fun format(width: Int, height: Int, locale: Locale): String = format(this, width, height, locale)
    fun format(width: Int, height: Int): String = format(this, width, height)

    override fun toString() = string

    override fun equals(other: Any?) = this === other

    override fun hashCode() = x + 52*y

    operator fun component1() = x
    operator fun component2() = y

    fun copy(x: Int = this.x, y: Int = this.y): GoPoint {
        return pointAt(x, y)
    }

    override fun compareTo(other: GoPoint): Int {
        return (x - other.x) + (y - other.y)*52
    }

    override fun iterator(): Iterator<GoPoint> = Itr(this)

    private class Itr(private var next: GoPoint?): Iterator<GoPoint> {

        override fun hasNext() = next != null

        override fun next(): GoPoint {
            val point = next ?: throw NoSuchElementException()
            next = null
            return point
        }

    }

    fun toSGFBytes(): SGFBytes {
        return SGFBytes(2)
            .append(toChar(x).code.toByte())
            .append(toChar(y).code.toByte())
    }

    private fun readResolve(): Any {
        if (x !in 0..51) throw InvalidObjectException("x=$x is not in the range [0,52)")
        if (y !in 0..51) throw InvalidObjectException("y=$y is not in the range [0,52)")
        return Cache.points[x + y*52]
    }

}