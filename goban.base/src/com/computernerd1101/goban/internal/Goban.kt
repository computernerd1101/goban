@file:OptIn(ExperimentalStdlibApi::class)

package com.computernerd1101.goban.internal

import com.computernerd1101.goban.*
import java.util.concurrent.atomic.*
import java.util.function.LongBinaryOperator

internal object InternalGoban: LongBinaryOperator {

    lateinit var count: AtomicLongFieldUpdater<AbstractGoban>

    fun copyRows(src: AtomicLongArray, dst: AtomicLongArray): Long {
        var count = 0L
        for(i in 0 until src.length()) {
            val row = src[i]
            dst[i] = row
            count += countStonesInRow(row)
        }
        return count
    }

    /**
     * @param row The lowest 32 bits represent the positions of black stones,
     * while the highest 32 bits represent the positions of white stones.
     * @return The lowest 32 bits represent the number of black stones,
     * while the highest 32 bits represent the number of white stones.
     */
    fun countStonesInRow(row: Long): Long {
        var i = row
        i -= (i ushr 1) and 0x5555_5555_5555_5555L
        i = (i and 0x3333_3333_3333_3333L) + ((i ushr 2) and 0x3333_3333_3333_3333L)
        i = (i + (i ushr 4)) and 0x0f0f_0f0f_0f0f_0f0fL
        i += i ushr 8
        i += i ushr 16
        return i and 0x3f_0000_003fL
    }

    fun set(
        goban: AbstractMutableGoban,
        x: Int,
        y: Int,
        newColor: GoColor?,
        expected: GoColor?
    ): GoColor? {
        if (x !in 0 until goban.width)
            throw indexOutOfBoundsException(x, goban.width)
        if (y !in 0 until goban.height)
            throw indexOutOfBoundsException(y, goban.height)
        val x2 = x and 31
        val y2 = when {
            goban.width <= 32 -> y
            x < 32 -> y*2
            else -> y*2 + 1
        }
        val row = goban.getRows(InternalMarker).getAndAccumulate(y2,
            x2.toLong() or when(newColor) {
                null -> 0L
                GoColor.BLACK -> NEW_BLACK
                GoColor.WHITE -> NEW_WHITE
            } or when(expected) {
                newColor -> 0L
                null -> EXPECT_EMPTY
                GoColor.BLACK -> EXPECT_BLACK
                GoColor.WHITE -> EXPECT_WHITE
            }, this)
        var recount = 0L
        val actual: GoColor? = when((row ushr x2) and MASK) {
            BLACK -> {
                recount = -BLACK
                GoColor.BLACK
            }
            WHITE -> {
                recount = -WHITE
                GoColor.WHITE
            }
            else -> null
        }
        if (actual != newColor && (expected == newColor || expected == actual)) {
            when(newColor) {
                GoColor.BLACK -> recount += BLACK
                GoColor.WHITE -> recount += WHITE
            }
            count.addAndGet(goban, recount)
        }
        return actual
    }

    override fun applyAsLong(row: Long, flags: Long): Long {
        val x = flags.toInt()
        val expected = flags and EXPECT_EMPTY
        if (expected != 0L) {
            val mask = MASK shl x
            val rowFlags: Long = when(expected) {
                EXPECT_WHITE -> WHITE shl x
                EXPECT_BLACK -> BLACK shl x
                else -> 0L
            }
            if (row and mask != rowFlags) return row
        }
        val add: Long
        val remove: Long
        when(flags and NEW_MASK) {
            NEW_BLACK -> {
                add = BLACK shl x
                remove = WHITE shl x
            }
            NEW_WHITE -> {
                add = WHITE shl x
                remove = BLACK shl x
            }
            else -> {
                add = 0L
                remove = MASK shl x
            }
        }
        return (row or add) and remove.inv()
    }

    fun get(width: Int, rows: AtomicLongArray, x: Int, y: Int): GoColor? {
        val row = rows[when {
            width <= 32 -> y
            x < 32 -> 2*y
            else -> 2*y + 1
        }]
        return when((row ushr (x and 31)) and MASK) {
            BLACK -> GoColor.BLACK
            WHITE -> GoColor.WHITE
            else -> null
        }
    }

    const val NEW_BLACK = 1L shl 32
    const val NEW_WHITE = 2L shl 32
    const val NEW_MASK  = 3L shl 32

    const val EXPECT_BLACK = 1L shl 34
    const val EXPECT_WHITE = 2L shl 34
    const val EXPECT_EMPTY = 3L shl 34

    const val BLACK = 1L
    const val WHITE = 1L shl 32
    const val MASK = BLACK or WHITE

    fun indexOutOfBoundsException(index: Int, size: Int) =
        IndexOutOfBoundsException("$index is not in the range [0-$size)")

    fun illegalSizeException(size: Int) = IllegalArgumentException("$size is not in the range [1-52]")

}

internal object GobanBulk: LongBinaryOperator {

    fun setAll(goban: AbstractMutableGoban, rows: Any, color: GoColor?): Boolean {
        val width = goban.width
        val height = goban.height
        var mask1 = 1.shl(width) - 1
        val mask2: Int
        if (width < 32) {
            mask2 = 0
        } else {
            // 1 shl (width - 32) == 1 shl width; assuming 32 bits
            mask2 = mask1 // 1.shl(width - 32) - 1
            mask1 = -1 // even if width == 32 exactly
        }
        var modified = false
        var i = 0
        for(y in 0 until height) {
            val row = when(rows) {
                is AtomicLongArray -> rows[y]
                else -> (rows as LongArray)[y]
            }
            if (setRow(goban, i++, row.toInt() and mask1, color))
                modified = true
            // If width == 32 exactly, then we don't need the following code,
            // but we still need mask1 to be -1 instead of 0.
            if (width > 32)
                if (setRow(goban, i++, (row ushr 32).toInt() and mask2, color))
                    modified = true
        }
        return modified
    }

    private fun setRow(goban: AbstractMutableGoban, i: Int, setMask: Int, color: GoColor?): Boolean {
        val rowMask = setMask.toLong() and (-1L ushr 32)
        val row = goban.getRows(InternalMarker).getAndAccumulate(i,
            rowMask or when(color) {
                null -> 0L
                GoColor.BLACK -> InternalGoban.NEW_BLACK
                GoColor.WHITE -> InternalGoban.NEW_WHITE
            }, this)
        val recount: Long = if (color == null)
            -InternalGoban.countStonesInRow(row and (rowMask * InternalGoban.MASK))
        else {
            val addMask: Long
            val removeMask: Long
            if (color == GoColor.BLACK) {
                addMask = rowMask
                removeMask = rowMask shl 32
            } else { // color == GoColor.WHITE
                addMask = rowMask shl 32
                removeMask = rowMask
            }
            InternalGoban.countStonesInRow(row.inv() and addMask) -
                    InternalGoban.countStonesInRow(row and removeMask)
        }
        return if (recount != 0L) {
            InternalGoban.count.addAndGet(goban, recount)
            true
        } else false
    }

    override fun applyAsLong(row: Long, flags: Long): Long {
        val setMask = flags and (-1L ushr 32)
        val add: Long
        val remove: Long
        when(flags and InternalGoban.NEW_MASK) {
            InternalGoban.NEW_BLACK -> {
                add = setMask
                remove = setMask shl 32
            }
            InternalGoban.NEW_WHITE -> {
                add = setMask shl 32
                remove = setMask
            }
            else -> {
                add = 0L
                remove = setMask * InternalGoban.MASK
            }
        }
        return (row or add) and remove.inv()
    }

    fun isAlive(width: Int, height: Int, xBit: Long, y: Int, player: Int, opponent: Int): Boolean {
        val arrays: Array<LongArray> = GobanThreadLocals.arrays()
        val cluster = arrays[THREAD_CLUSTER]
        clear(cluster)
        cluster[y] = xBit
        val playerRows = arrays[player]
        val opponentRows = arrays[opponent]
        // I tried making this a recursive function, but sometimes got a StackOverflowError.
        // Fortunately, an array of 52 Longs occupies 52*8 = 416 bytes, plus the obligatory
        // heap allocation data, which still consumes way less memory than every local variable
        // in this function times a large number of recursive steps in the worse-case scenario.
        val pending = arrays[THREAD_PENDING]
        clear(pending)
        val maxBit = 1L shl (width - 1)
        var bit = xBit
        var y1 = y
        var pendingY = y
        pop@ while(true) {
            // check the points above, below, and to the left and right of the current point
            var y2 = y1 - 1 // above
            if (y2 >= 0 && opponentRows[y2] and bit == 0L) {
                if (playerRows[y2] and bit == 0L) return true
                val clusterRow = cluster[y2]
                if (clusterRow and bit == 0L) {
                    cluster[y2] = clusterRow or bit
                    // pendingY is the first (top to bottom) non-zero row in pending.
                    // If the row before (above) it gained a bit, then that
                    // must be the new pendingY.
                    pendingY = y2
                    pending[y2] = pending[y2] or bit
                }
            }
            var bits = 0L
            // Ironically, use binary right-shift to move to the left on the goban.
            // This is because x progresses to the right, but significant bits
            // in an integer progress to the left. The computer may or may not
            // see it that way (big endian vs little endian), but enough human mathematicians
            // have agreed to name the operations "left shift" and "right shift" accordingly.
            var bit2 = bit shr 1 // left
            if (bit2 > 0 && opponentRows[y1] and bit2 == 0L) {
                if (playerRows[y1] and bit2 == 0L) return true
                val clusterRow = cluster[y1]
                if (clusterRow and bit2 == 0L) {
                    cluster[y1] = clusterRow or bit2
                    bits = bit2
                }
            }
            bit2 = bit shl 1 // right
            if (bit2 <= maxBit && opponentRows[y1] and bit2 == 0L) {
                if (playerRows[y1] and bit2 == 0L) return true
                val clusterRow = cluster[y1]
                if (clusterRow and bit2 == 0L) {
                    cluster[y1] = clusterRow or bit2
                    bits = bits or bit2
                }
            }
            if (bits != 0L) pending[y1] = pending[y1] or bits
            y2 = y1 + 1 // down
            if (y2 < height && opponentRows[y2] and bit == 0L) {
                if (playerRows[y2] and bit == 0L) return true
                val clusterRow = cluster[y2]
                if (clusterRow and bit == 0L) {
                    cluster[y2] = clusterRow or bit
                    pending[y2] = pending[y2] or bit
                }
            }
            // pop next point
            while(pendingY < height) {
                val row = pending[pendingY]
                if (row != 0L) {
                    bit = row and -row
                    pending[pendingY] = row - bit
                    y1 = pendingY
                    // found next point
                    continue@pop
                }
                pendingY++
            }
            // no more points
            return false
        }
    }

    const val THREAD_META_CLUSTER = 0
    const val THREAD_CLUSTER = 1
    const val THREAD_BLACK = 2
    const val THREAD_WHITE = 3
    const val THREAD_PENDING = 4

    fun threadLocalGoban(width: Int, height: Int, rows: AtomicLongArray) {
        // 5 arrays of 52 longs each
        val arrays: Array<LongArray> = GobanThreadLocals.arrays()
        val blackRows = arrays[THREAD_BLACK]
        val whiteRows = arrays[THREAD_WHITE]
        var i = 0
        for(y in 0 until height) {
            var row = rows[i++]
            var blackRow = row and (-1 ushr 32) // blackRow.lo = row.lo
            var whiteRow = row ushr 32          // whiteRow.lo = row.hi
            if (width > 32) {
                row = rows[i++]
                blackRow = blackRow or (row shl 32)             // blackRow.hi = row.lo
                whiteRow = whiteRow or row.and(-1 shl 32) // whiteRow.hi = row.hi
            }
            blackRows[y] = blackRow
            whiteRows[y] = whiteRow
        }
    }

    fun clear(array: LongArray) {
        for(i in array.indices) array[i] = 0L
    }

    fun updateMetaCluster(height: Int) {
        val arrays: Array<LongArray> = GobanThreadLocals.arrays()
        val metaCluster = arrays[THREAD_META_CLUSTER]
        val cluster = arrays[THREAD_CLUSTER]
        for(y in 0 until height) {
            metaCluster[y] = metaCluster[y] or cluster[y]
        }
    }

}