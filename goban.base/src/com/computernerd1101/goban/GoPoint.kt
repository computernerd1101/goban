package com.computernerd1101.goban

import com.computernerd1101.goban.internal.*
import com.computernerd1101.sgf.SGFBytes
import java.io.*

// Implement all the same methods as a data class, with the same behavior,
// except that all instances are cached.
class GoPoint private constructor(
    @JvmField val x: Int,
    @JvmField val y: Int,
    buffer2: CharArray,
    buffer4: CharArray
): Iterable<GoPoint>, Comparable<GoPoint>, Serializable {

    @Transient private val string: String

    @Transient
    @get:JvmName("selfRect")
    val selfRect: GoRectangle

    infix fun rect(other: GoPoint) = rect(other, other.x, other.y)

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
        return InternalGoRectangle.init(start, end, InternalGoRectangle.toString(start, end))
    }

    init {
        val cx = toChar(x)
        val cy = toChar(y)
        buffer2[0] = cx
        buffer2[1] = cy
        buffer4[1] = cx
        buffer4[2] = cy
        string = String(buffer2).intern()
        selfRect = InternalGoRectangle.init(this, this, String(buffer4).intern())
    }

    companion object {

        @JvmStatic
        fun pointAt(x: Int, y: Int): GoPoint {
            if (x !in 0..51) throw IndexOutOfBoundsException("x=$x is not in the range [0,52)")
            if (y !in 0..51) throw IndexOutOfBoundsException("y=$y is not in the range [0,52)")
            return CACHE[x + y*52]
        }

        @JvmStatic
        fun nullable(x: Int, y: Int): GoPoint? {
            return if (x in 0..51 && y in 0..51) CACHE[x + y*52] else null
        }

        private val CACHE: Array<GoPoint>

        init {
            GoRectangle // initialize companion
            val buffer2 = CharArray(2)
            val buffer4 = CharArray(4)
            buffer4[0] = '['
            buffer4[3] = ']'
            CACHE = Array(52*52) {
                GoPoint(it % 52, it / 52, buffer2, buffer4)
            }
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

        private const val serialVersionUID = 1L

    }

    override fun toString() = string

    override fun equals(other: Any?) = this === other

    override fun hashCode() = x + 52*y

    operator fun component1() = x
    operator fun component2() = y

    @Suppress("unused")
    fun copy(x: Int = this.x, y: Int = this.y): GoPoint {
        return GoPoint(x, y)
    }

    override fun compareTo(other: GoPoint): Int {
        return (x - other.x) + (y - other.y)*52
    }

    override fun iterator(): Iterator<GoPoint> = object: Iterator<GoPoint> {

        private var hasNext = true

        override fun hasNext() = hasNext

        override fun next(): GoPoint {
            if (!hasNext) throw NoSuchElementException()
            hasNext = false
            return this@GoPoint
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
        return CACHE[x + y*52]
    }

}