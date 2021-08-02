package com.computernerd1101.goban.internal

import com.computernerd1101.goban.*
import java.util.concurrent.atomic.*
import java.util.function.LongBinaryOperator

internal object InternalGoban: LongBinaryOperator {

    lateinit var count: AtomicLongFieldUpdater<AbstractGoban>

    fun emptyRows(wide: Boolean, height: Int): GobanRows1 {
        val index = when {
            !wide -> height - 1
            height <= 26 -> height*2 - 1
            else -> height + 25 // height + 26 - 1
        }
        return GobanRows.empty[index]
    }

    fun newRows(wide: Boolean, height: Int): GobanRows1 = emptyRows(wide, height).newInstance()

    fun copyRows(src: GobanRows1, dst: GobanRows1): Long {
        var count = 0L
        for(i in 0 until src.size) {
            val row = src[i]
            val oldRow = GobanRows.updaters[i].getAndSet(dst, row)
            count += countStonesInRow(row) - countStonesInRow(oldRow)
        }
        return count
    }

    /**
     * @param row The lowest 32 bits represent the positions of black stones,
     * while the highest 32 bits represent the positions of white stones.
     * @return The lowest 6 bits represent the number of black stones, while
     * the lowest 6 bits among the highest 32 bits represent the number of white stones.
     */
    fun countStonesInRow(row: Long): Long {
        var i = row - ((row ushr 1) and 0x5555_5555_5555_5555L)
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
        val row = GobanRows.updaters[y2].getAndAccumulate(goban.rows,
            (1L shl x2) or when(newColor) {
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
        val bit = flags and (-1L ushr 32)
        val expected = flags and EXPECT_EMPTY
        if (expected != 0L) {
            val mask = bit * MASK
            val rowFlags: Long = when(expected) {
                EXPECT_WHITE -> bit shl 32
                EXPECT_BLACK -> bit
                else -> 0L
            }
            if (row and mask != rowFlags) return row
        }
        val add: Long
        val remove: Long
        when(flags and NEW_MASK) {
            NEW_BLACK -> {
                add = bit
                remove = bit shl 32
            }
            NEW_WHITE -> {
                add = bit shl 32
                remove = bit
            }
            else -> {
                add = 0L
                remove = bit * MASK
            }
        }
        return (row or add) and remove.inv()
    }

    fun get(width: Int, rows: GobanRows1, x: Int, y: Int): GoColor? {
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
        val y1: Int
        val y2: Int
        val rectRow: Long
        var i: Int
        if (rows is GoRectangle) {
            y1 = rows.start.y
            if (y1 >= height || rows.start.x >= width) return false
            y2 = minOf(rows.end.y, height - 1)
            rectRow = InternalGoRectangle.rowBits(rows) // Guaranteed non-zero
            i = if (width <= 32) y1 else y1*2
        } else {
            y1 = 0
            y2 = height - 1
            rectRow = 0L
            i = 0
        }
        for(y in y1..y2) {
            val row = when {
                rectRow != 0L -> rectRow
                rows is GoPointSet -> InternalGoPointSet.rowUpdaters[y][rows]
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

    private fun setRow(goban: AbstractMutableGoban, i: Int, mask: Int, color: GoColor?): Boolean {
        if (mask == 0) return false
        var removeBits = mask.toLong() and (-1L ushr 32)
        if (color == null) removeBits *= InternalGoban.MASK // remove both black and white
        else if (color == GoColor.BLACK) removeBits = removeBits shl 32 // remove white
        // else remove black
        val row = GobanRows.updaters[i].getAndAccumulate(goban.rows, removeBits, this)
        val recount: Long = if (color == null)
            -InternalGoban.countStonesInRow(row and removeBits)
        else {
            val addBits = if (color == GoColor.BLACK)
                removeBits ushr 32
            else // color == GoColor.WHITE
                removeBits shl 32
            InternalGoban.countStonesInRow(row.inv() and addBits) -
                    InternalGoban.countStonesInRow(row and removeBits)
        }
        return if (recount != 0L) {
            InternalGoban.count.addAndGet(goban, recount)
            true
        } else false
    }

    override fun applyAsLong(row: Long, remove: Long): Long {
        val add: Long = when {
            remove and (-1L ushr 32) == 0L -> remove ushr 32
            remove and (-1L shl 32) == 0L -> remove shl 32
            else -> 0L
        }
        return (row or add) and remove.inv()
    }

    fun  isAlive(width: Int, height: Int,
                xBit: Long, y: Int,
                player: Int, opponent: Int,
                falseEyeScore: Int = -1,
                onlyChain: Boolean = true,
                shortCircuit: Boolean = true
    ): Boolean {
        val arrays: Array<LongArray> = GobanThreadLocals.INSTANCE.get()
        val chain = arrays[GobanThreadLocals.CHAIN]
        clear(chain)
        chain[y] = xBit
        val playerRows = arrays[player]
        val opponentRows = arrays[opponent]
        // I tried making this a recursive function, but sometimes got a StackOverflowError.
        // Fortunately, an array of 52 Longs occupies 52*8 = 416 bytes, plus the obligatory
        // heap allocation data, which still consumes way less memory than every local variable
        // in this function times a large number of recursive steps in the worse-case scenario.
        val pending = arrays[GobanThreadLocals.PENDING]
        clear(pending)
        val maxBit = 1L shl (width - 1)
        var bit = xBit
        var y1 = y
        var pendingY = y
        var isAlive = false
        val falseEyes: LongArray?
        if (onlyChain && !shortCircuit && falseEyeScore >= 0) {
            falseEyes = arrays[GobanThreadLocals.FALSE_EYES]
            clear(falseEyes)
        } else falseEyes = null
        pop@ while(true) {
            // check the points above, below, and to the left and right of the current point
            var y2 = y1 - 1 // above
            var chainRow: Long
            if (y2 >= 0 && opponentRows[y2] and bit == 0L) {
                if (onlyChain && playerRows[y2] and bit == 0L) {
                    if (shortCircuit) return true
                    if (falseEyes != null) falseEyes[y2] = falseEyes[y2] or bit
                    else isAlive = true
                } else {
                    chainRow = chain[y2]
                    if (chainRow and bit == 0L) {
                        chain[y2] = chainRow or bit
                        // pendingY is the first (top to bottom) non-zero row in pending.
                        // If the row before (above) it gained a bit, then that
                        // must be the new pendingY.
                        pendingY = y2
                        pending[y2] = pending[y2] or bit
                    }
                }
            }
            var bits = 0L
            // Ironically, use binary right-shift to move to the left on the goban.
            // This is because x progresses to the right, but significant bits
            // in an integer progress to the left. The computer may or may not
            // see it that way (big endian vs little endian), but enough human mathematicians
            // have agreed to name the operations "left shift" and "right shift" accordingly.
            var bit2 = bit shr 1 // left
            chainRow = chain[y1]
            if (bit2 > 0 && opponentRows[y1] and bit2 == 0L) {
                if (onlyChain && playerRows[y1] and bit2 == 0L) {
                    if (shortCircuit) return true
                    if (falseEyes != null) falseEyes[y1] = falseEyes[y1] or bit2
                    else isAlive = true
                } else if (chainRow and bit2 == 0L)
                    bits = bit2
            }
            bit2 = bit shl 1 // right
            if (bit2 <= maxBit && opponentRows[y1] and bit2 == 0L) {
                if (onlyChain && playerRows[y1] and bit2 == 0L) {
                    if (shortCircuit) return true
                    if (falseEyes != null) falseEyes[y1] = falseEyes[y1] or bit2
                    else isAlive = true
                } else if (chainRow and bit2 == 0L)
                    bits = bits or bit2
            }
            if (bits != 0L) {
                chain[y1] = chainRow or bits
                pending[y1] = pending[y1] or bits
            }
            y2 = y1 + 1 // below
            if (y2 < height && opponentRows[y2] and bit == 0L) {
                if (onlyChain && playerRows[y2] and bit == 0L) {
                    if (shortCircuit) return true
                    if (falseEyes != null) falseEyes[y2] = falseEyes[y2] or bit
                    else isAlive = true
                } else {
                    chainRow = chain[y2]
                    if (chainRow and bit == 0L) {
                        chain[y2] = chainRow or bit
                        pending[y2] = pending[y2] or bit
                    }
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
            if (falseEyes == null) return isAlive
            val score = arrays[falseEyeScore]
            y1 = -1
            // Return true unless score and falseEyes have exactly one bit in common.
            for(i in 0 until height) {
                val row = score[i] and falseEyes[i]
                if (row == 0L) continue
                if (y1 >= 0 || row and -row != row) return true
                y1 = i
                bit = row
            }
            if (y1 < 0) return true
            score[y1] = score[y1] and bit.inv()
            return false
        }
    }

    fun threadLocalGoban(width: Int, height: Int, rows: GobanRows1) {
        val arrays: Array<LongArray> = GobanThreadLocals.INSTANCE.get()
        val blackRows = arrays[GobanThreadLocals.BLACK]
        val whiteRows = arrays[GobanThreadLocals.WHITE]
        var i = 0
        for(y in 0 until height) {
            var row = rows[i++]
            var blackRow = row and (-1L ushr 32) // blackRow.lo = row.lo
            var whiteRow = row ushr 32          // whiteRow.lo = row.hi
            if (width > 32) {
                row = rows[i++]
                blackRow = blackRow or (row shl 32)             // blackRow.hi = row.lo
                whiteRow = whiteRow or row.and(-1L shl 32) // whiteRow.hi = row.hi
            }
            blackRows[y] = blackRow
            whiteRows[y] = whiteRow
        }
    }

    fun clear(array: LongArray) {
        for(i in array.indices) array[i] = 0L
    }

    fun addChainToGroup(height: Int) {
        val arrays: Array<LongArray> = GobanThreadLocals.INSTANCE.get()
        val group = arrays[GobanThreadLocals.GROUP]
        val chain = arrays[GobanThreadLocals.CHAIN]
        for(y in 0 until height) {
            group[y] = group[y] or chain[y]
        }
    }

}
