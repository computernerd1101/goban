@file:JvmMultifileClass
@file:JvmName("GobanKt")

package com.computernerd1101.goban

import com.computernerd1101.goban.internal.*
import java.io.Serializable

fun GoRectangle(x1: Int, y1: Int, x2: Int, y2: Int) = GoRectangle.rect(x1, y1, x2, y2)

class GoRectangle internal constructor(
    @JvmField val start: GoPoint,
    @JvmField val end: GoPoint,
    string: String?,
    intern: InternalGoRectangle
): Set<GoPoint>, Comparable<GoRectangle>, Serializable {

    @Transient private var string: String = string ?: intern.toString(start, end)

    companion object {

        @JvmStatic
        fun rect(x1: Int, y1: Int, x2: Int, y2: Int): GoRectangle {
            if (x1 == x2 && y1 == y2) return GoPoint(x1, y1).selfRect
            if ((x1 == 0 && y1 == 0) || (x2 == 0 && y2 == 0))
                return GoPoint(x1, y1).rect(x2, y2)
            if ((x1 == 0 && y2 == 0) || (x2 == 0 && y1 == 0))
                return GoPoint(x1, y2).rect(x2, y1)
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
            return GoRectangle(GoPoint(startX, startY), GoPoint(endX, endY), null, InternalGoRectangle)
        }

        private const val serialVersionUID = 1L

    }

    val x1: Int @JvmName("x1") get() = start.x
    val y1: Int @JvmName("y1") get() = start.y
    val x2: Int @JvmName("x2") get() = end.x
    val y2: Int @JvmName("y2") get() = end.y

    override val size: Int
        get() = (x2 - x1 + 1)*(y2 - y1 + 1)

    override fun isEmpty() = false

    override fun contains(element: GoPoint) = element.x in x1..x2 && element.y in y1..y2

    override fun containsAll(elements: Collection<GoPoint>): Boolean {
        when(elements) {
            is GoRectangle -> return contains(elements.start) && contains(elements.end)
            is GoPointSet -> {
                val mask: Long = InternalGoRectangle.rowBits(this).inv()
                for(y in 0..51) {
                    var row = InternalGoPointSet.ROWS[y][elements]
                    if (y in start.y..end.y) row = row and mask
                    if (row != 0L) return false
                }
            }
            is GoPointKeys<*> -> {
                elements.expungeStaleRows()
                val secrets = elements.map.secrets
                for (y in 0..51) {
                    val row = secrets[y] ?: continue
                    if (y !in start.y..end.y)
                        return false
                    for(x in 0 until start.x) if (row[x] != null) return false
                    for(x in (end.x + 1)..51) if (row[x] != null) return false
                }
            }
            is GoPointEntries<*> -> return elements.isEmpty()
            else -> return defaultContainsAll(elements)
        }
        return true
    }

    private fun defaultContainsAll(elements: Collection<*>): Boolean {
        for(element in elements)
            if (element !is GoPoint || !contains(element)) return false
        return true
    }

    override fun iterator(): Iterator<GoPoint> = Itr(x1, y1, x2, y2)

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
        return start.compareTo(other.start)*(52*52) + end.compareTo(other.end)
    }

    override fun equals(other: Any?): Boolean {
        return this === other || when (other) {
            is GoRectangle -> start == other.start && end == other.end
            is GoPointSet, is GoPointKeys<*> -> other == this
            is GoPointEntries<*> -> {
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
        val x = x2 - x1
        val y = y2 - y1
        // sum[y0=y1..y2]sum[x0=x1..x2](x0+52*y0) = (y+1)*(x+1)*(x/2 + y*26 + start.hashCode())
        // Proof: sum[y0=y1..y2]sum[x0=x1..x2](x0 + 52*y0)
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
        // x could be even or odd, but x*(x+1) is guaranteed to be even.
    }

    override fun toString() = string

    operator fun component1() = start
    operator fun component2() = end

    fun copy(
        x1: Int = this.x1,
        y1: Int = this.y1,
        x2: Int = this.x2,
        y2: Int = this.y2,
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
            startX == 0 && startY == 0 -> start rect end
            start == this.start && end == this.end -> this
            else -> GoRectangle(start, end, null, InternalGoRectangle)
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
                if (x1 > x2) {
                    start = GoPoint(x2, y1)
                    end   = GoPoint(x1, y2)
                } else {
                    start = this.start
                    end = this.end
                    if (x1 != 0 || y1 != 0) {
                        string = InternalGoRectangle.toString(start, end)
                        return this
                    }
                }
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
        return if (start.x == 0 && start.y == 0) start rect end
        else GoRectangle(start, end, null, InternalGoRectangle)
    }

}