package com.computernerd1101.goban

import com.computernerd1101.goban.internal.*
import java.io.Serializable

class GoRectangle private constructor(
    @JvmField val start: GoPoint,
    @JvmField val end: GoPoint,
    @Transient private var string: String
): Set<GoPoint>, Comparable<GoRectangle>, Serializable {

    companion object {

        init {
            internalSelfRect = { p, buffer ->
                GoRectangle(p, p, String(buffer).intern())
            }
        }

        @JvmStatic fun rect(from: GoPoint, to: GoPoint): GoRectangle {
            val (x1, y1) = from
            val (x2, y2) = to
            val start: GoPoint
            val end: GoPoint
            when {
                x1 == x2 && y1 == y2 -> return from.selfRect
                y1 <= y2 -> {
                    if (x1 <= x2) {
                        start = from
                        end = to
                    } else {
                        start = GoPoint(x2, y1)
                        end   = GoPoint(x1, y2)
                    }
                }
                x1 < x2 -> {
                    start = GoPoint(x1, y2)
                    end   = GoPoint(x2, y1)
                }
                else -> {
                    start = to
                    end = from
                }
            }
            return GoRectangle(start, end, toString(start, end))
        }

        @JvmStatic
        fun rect(x1: Int, y1: Int, x2: Int, y2: Int): GoRectangle {
            if (x1 == x2 && y1 == y2) return GoPoint(x1, y1).selfRect
            val startX: Int
            val startY: Int
            val endX: Int
            val endY: Int
            if (x1 <= x2) {
                startX = x1
                endX = x2
            } else {
                startX = x2
                endX = x1
            }
            if (y1 <= y2) {
                startY = y1
                endY = y2
            } else {
                startY = y2
                endY = y1
            }
            val start = GoPoint(startX, startY)
            val end = GoPoint(endX, endY)
            return GoRectangle(start, end, toString(start, end))
        }

        private fun toString(start: GoPoint, end: GoPoint): String {
            val buffer: CharArray
            val offset: Int
            val (x1, y1) = start
            val (x2, y2) = end
            if ((x1 == x2 && y1 + 1 == y2) ||
                (x1 + 1 == x2 && y1 == y2)) {
                buffer = buffer2
                offset = 5
            } else {
                buffer = bufferTruncate
                offset = 10
            }
            buffer[1] = x1.toGoPointChar()
            buffer[2] = y1.toGoPointChar()
            buffer[offset] = x2.toGoPointChar()
            buffer[offset + 1] = y2.toGoPointChar()
            return String(buffer)
        }

        private val buffer2: CharArray by threadLocal {
            val buffer = CharArray(8)
            buffer[0] = '['
            // buffer[1] = x1
            // buffer[2] = y1
            buffer[3] = ','
            buffer[4] = ' '
            // buffer[5] = x2
            // buffer[6] = y2
            buffer[7] = ']'
            buffer
        }

        private val bufferTruncate: CharArray by threadLocal {
            val buffer = CharArray(13)
            buffer[0] = '['
            // buffer[1] = x1
            // buffer[2] = y1
            // ", ..., "
            buffer[3] = ','
            buffer[4] = ' '
            buffer[5] = '.'
            buffer[6] = '.'
            buffer[7] = '.'
            buffer[8] = ','
            buffer[9] = ' '
            // buffer[10] = x2
            // buffer[11] = y2
            buffer[12] = ']'
            buffer
        }

        private const val serialVersionUID = 1L

    }

    override val size: Int
        get() = (end.x - start.x + 1)*(end.y - start.y + 1)

    override fun isEmpty() = false

    override fun contains(element: GoPoint) = element.x in start.x..end.x && element.y in start.y..end.y

    override fun containsAll(elements: Collection<GoPoint>): Boolean {
        when(elements) {
            is GoRectangle -> return contains(elements.start) && contains(elements.end)
            is GoPointSet -> {
                val rows = InternalGoPointSet.secrets.rows(elements)
                val mask: Long = ((1L shl (end.x + 1)) - (1L shl start.x)).inv()
                for(y in 0..51) {
                    var row = rows[y]
                    if (y in start.y..end.y) row = row and mask
                    if (row != 0L) return false
                }
            }
            is GoPointKeys<*> -> {
                elements.expungeStaleRows()
                val rows = InternalGoPointMap.secrets.rows(elements.map)
                for (y in 0..51) {
                    val row = rows[y] ?: continue
                    if (y !in start.y..end.y)
                        return false
                    for(x in 0 until start.x) if (row[x] != null) return false
                    for(x in (end.x + 1)..51) if (row[x] != null) return false
                }
            }
            is GoPointEntries<*, *> -> return elements.isEmpty()
            else -> return defaultContainsAll(elements)
        }
        return true
    }

    private fun defaultContainsAll(elements: Collection<*>): Boolean {
        for(element in elements)
            if (element !is GoPoint || !contains(element)) return false
        return true
    }

    override fun iterator(): Iterator<GoPoint> = Itr(start.x, start.y, end.x, end.y)

    private class Itr(
        // Every time the iterator starts a new row, x will reset to x1
        private val x1: Int,
        // y1 is never needed after it is assigned to y
        y1: Int,
        // When to move on to the next row
        private val x2: Int,
        // When to stop iterating entirely
        private val y2: Int
    ): Iterator<GoPoint> {

        private var x = x1
        private var y = y1

        override fun hasNext() = y <= y2

        override fun next(): GoPoint {
            val y = this.y
            if (y > y2) throw NoSuchElementException()
            val x = this.x
            this.x = if (x < x2)
                x + 1
            else {
                this.y = y + 1
                x1
            }
            return GoPoint(x, y)
        }

    }

    override fun compareTo(other: GoRectangle): Int {
        return start.compareTo(other.start)*52*52 + end.compareTo(other.end)
    }

    override fun equals(other: Any?): Boolean {
        return this === other || when (other) {
            is GoRectangle -> start == other.start && end == other.end
            is GoPointSet, is GoPointKeys<*> -> other == this
            is GoPointEntries<*, *> -> {
                other.expungeStaleRows()
                false
            }
            is Set<*> -> size == other.size && defaultContainsAll(other)
            else -> false
        }
    }

    override fun hashCode(): Int {
        // The hash code of a Set is the sum of the hash codes of its elements.
        // start.hashCode() = x1 + 52*y1
        val x = end.x - start.x // x2 - x1
        val y = end.y - start.y // y2 - y1
        // sum[y0=y1..y2]sum[x0=x1..x2](x0+52*y0) = (y+1)*(x+1)*(x/2 + y*26 + start.hashCode())
        // Proof: sum[y0=y1..y2]sum[x0=x1..x2](x0+52*y0)
        // = sum[y0=0..y]sum[x0=0..x](x0 + x1 + 52*(y0 + y1))
        // = sum[y0=0..y]sum[x0=0..x](x0 + 52*y0)             + sum[y0=0..y]sum[x0=0..x](x1 + 52*y1)
        // = sum[y0=0..y](sum[x0=0..x]x0 + 52*sum[x0=0..x]y0) + (y+1)*(x+1)*(x1 + 52*y1)
        // = sum[y0=0..y](x*(x+1)/2 + 52*(x+1)*y0)            + (y+1)*(x+1)*start.hashCode()
        // = (y+1)*(x+1)*x/2 + 52*(x+1)*sum[y0=0..y]y0        + (y+1)*(x+1)*start.hashCode()
        // = (y+1)*(x+1)*x/2 + 52*(x+1)*y*(y+1)/2             + (y+1)*(x+1)*start.hashCode()
        // = (y+1)*(x+1)*x/2 + (y+1)*(x+1)*y*26               + (y+1)*(x+1)*start.hashCode()
        // = (y+1)*(x+1)*(x/2 + y*26 + start.hashCode())          QED
        // = (y+1)*(x*(x+1)/2 + (x+1)*(y*26 + start.hashCode()))  to avoid rounding errors
        return (y+1)*((x*(x+1)).shr(1) + (x+1)*(y*26 + start.hashCode()))
        // x might be odd, but x*(x+1) is guaranteed to be even.
    }

    override fun toString() = string

    operator fun component1() = start
    operator fun component2() = end

    fun copy(
        x1: Int = this.start.x,
        y1: Int = this.start.y,
        x2: Int = this.end.x,
        y2: Int = this.end.y,
        from: GoPoint? = null,
        to: GoPoint? = null
    ): GoRectangle {
        var startX: Int
        var startY: Int
        if (from == null) {
            startX = x1
            startY = y1
        } else {
            startX = from.x
            startY = from.y
        }
        var endX: Int
        var endY: Int
        if (to == null) {
            endX = x2
            endY = y2
        } else {
            endX = to.x
            endY = to.y
        }
        if (startX > endX) {
            val tmp = startX
            startX = endX
            endX = tmp
        }
        if (startY > endY) {
            val tmp = startY
            startY = endY
            endY = tmp
        }
        val start = GoPoint(startX, startY)
        val end = GoPoint(endX, endY)
        return when {
            start == end -> start.selfRect
            start == this.start && end == this.end -> this
            else -> GoRectangle(start, end, toString(start, end))
        }
    }

    private fun readResolve(): Any {
        val (x1, y1) = this.start
        val (x2, y2) = this.end
        if (x1 == x2 && y1 == y2) return this.start.selfRect
        val start: GoPoint
        val end: GoPoint
        when {
            y1 <= y2 -> {
                if (x1 <= x2) {
                    string = toString(this.start, this.end)
                    return this
                }
                start = GoPoint(x2, y1)
                end   = GoPoint(x1, y2)
            }
            x1 < x2 -> {
                start = GoPoint(x1, y2)
                end   = GoPoint(x2, y1)
            }
            else -> {
                start = this.end
                end = this.start
            }
        }
        return GoRectangle(start, end, toString(start, end))
    }

}