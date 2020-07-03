@file:OptIn(ExperimentalStdlibApi::class)

package com.computernerd1101.goban.internal

import com.computernerd1101.goban.*
import java.util.concurrent.atomic.*
import java.util.function.LongBinaryOperator
import java.util.function.Supplier

object InternalGoban: LongBinaryOperator {

    lateinit var rows: (AbstractGoban) -> AtomicLongArray
    lateinit var count: AtomicLongFieldUpdater<AbstractGoban>

    interface Factory<out G: AbstractGoban> {
        operator fun invoke(width: Int, height: Int, rows: AtomicLongArray, count: Long): G
    }
    var fixedFactory: Factory<FixedGoban> by SecretKeeper { FixedGoban }
    var mutableFactory: Factory<MutableGoban> by SecretKeeper { MutableGoban }
    var playableFactory: Factory<Goban> by SecretKeeper { Goban }

    fun copyRows(src: AtomicLongArray, dst: AtomicLongArray): Long {
        var count = 0L
        for(i in 0 until src.length()) {
            val row = src[i]
            dst[i] = row
            count += countStonesInRow(row)
        }
        return count
    }

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
        val row = rows(goban).getAndAccumulate(y2,
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
                recount = -1L
                GoColor.BLACK
            }
            WHITE -> {
                recount = -1L shl 32
                GoColor.WHITE
            }
            else -> null
        }
        if (actual != newColor && (expected == newColor || expected == actual)) {
            if (newColor == GoColor.BLACK)
                recount++
            else if (newColor == GoColor.WHITE)
                recount += 1L shl 32
            count.addAndGet(goban, recount)
        }
        return actual
    }

    override fun applyAsLong(row: Long, flags: Long): Long {
        val x = flags.toInt()
        val expected = flags and EXPECT_EMPTY
        if (expected != 0L) {
            val mask = BLACK shl x
            val rowFlags: Long = when(expected) {
                EXPECT_WHITE -> WHITE shl x
                EXPECT_BLACK -> mask
                else -> 0L
            }
            if (row and mask != rowFlags) return row
        }
        var o = 0L
        val a: Long = when(flags and NEW_MASK) {
            NEW_BLACK -> {
                o = BLACK shl x
                (WHITE shl x).inv()
            }
            NEW_WHITE -> {
                o = WHITE shl x
                (BLACK shl x).inv()
            }
            else -> (MASK shl x).inv()
        }
        return (row or o) and a
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

    @JvmField
    val NEIGHBOR_OFFSETS = intArrayOf(
        0, -1,
        1,  0,
        0,  1,
        -1, 0
    )

    fun isAlive(width: Int, height: Int, rows: AtomicLongArray, p: GoPoint, color: GoColor): Boolean {
        val cluster: MutableGoPointSet = threadCluster
        cluster.clear()
        val stack: MutableList<GoPoint> = libertyStack
        stack.clear()
        stack.add(p)
        cluster.add(p)
        do {
            val point = stack.removeAt(stack.size - 1)
            for(i in 0..6 step 2) {
                val x = point.x + NEIGHBOR_OFFSETS[i]
                val y = point.y + NEIGHBOR_OFFSETS[i + 1]
                if (x in 0 until width && y in 0 until height) {
                    val stone = get(width, rows, x, y) ?: return true
                    if (stone == color) {
                        val neighbor = GoPoint(x, y)
                        if (cluster.add(neighbor)) stack.add(neighbor)
                    }
                }
            }
        } while(stack.isNotEmpty())
        return false
    }

    const val NEW_BLACK = 1L shl 32
    const val NEW_WHITE = 2L shl 32
    const val NEW_MASK = NEW_BLACK or NEW_WHITE

    const val EXPECT_BLACK = 1L shl 34
    const val EXPECT_WHITE = 2L shl 34
    const val EXPECT_EMPTY = 3L shl 34

    const val BLACK = 1L
    const val WHITE = 1L shl 32
    const val MASK = BLACK or WHITE

    private object MutableSetSupplier: Supplier<MutableGoPointSet> {

        override fun get() = MutableGoPointSet()

    }

    val threadMetaCluster by threadLocal(MutableSetSupplier)
    val threadCluster by threadLocal(MutableSetSupplier)
    val libertyStack by threadLocal { mutableListOf<GoPoint>() }

    fun indexOutOfBoundsException(index: Int, size: Int) =
        IndexOutOfBoundsException("$index is not in the range [0-$size)")

    fun illegalSizeException(size: Int) = IllegalArgumentException("$size is not in the range [1-52]")

}

object GobanSetAllOp: LongBinaryOperator {

    fun setAll(goban: AbstractMutableGoban, points: GoPointSet, color: GoColor?): Boolean {
        val width = goban.width
        val height = goban.height
        val rows = InternalGoPointSet.secrets.rows(points)
        var mask1 = 1.shl(width) - 1
        var mask2 = 0
        if (width >= 32) {
            // 1 shl (width - 32) == 1 shl width; assuming 32 bits
            mask2 = mask1 // 1.shl(width - 32) - 1
            mask1 = -1
        }
        var modified = false
        var i = 0
        for(y in 0 until height) {
            val row = rows[y]
            if (setRow(goban, i++, row.toInt() and mask1, color))
                modified = true
            if (width > 32)
                if (setRow(goban, i++, (row ushr 32).toInt() and mask2, color))
                    modified = true
        }
        return modified
    }

    private fun setRow(goban: AbstractMutableGoban, i: Int, setMask: Int, color: GoColor?): Boolean {
        val row = InternalGoban.rows(goban).getAndAccumulate(i,
            setMask.toLong().and(-1L ushr 32) or when(color) {
                null -> 0L
                GoColor.BLACK -> InternalGoban.NEW_BLACK
                GoColor.WHITE -> InternalGoban.NEW_WHITE
            }, this)
        val rowMask = setMask.toLong() and (-1L ushr 32)
        val recount: Long
        recount = if (color == null)
            -InternalGoban.countStonesInRow(row and (rowMask * InternalGoban.MASK))
        else {
            val addMask: Long
            val removeMask: Long
            if (color == GoColor.BLACK) {
                addMask = rowMask
                removeMask = rowMask shl 32
            } else {
                addMask = rowMask shl 32
                removeMask = rowMask
            }
            InternalGoban.countStonesInRow(row.inv() and addMask) -
                    InternalGoban.countStonesInRow(row and removeMask)
        }
        InternalGoban.count.addAndGet(goban, recount)
        return true
    }

    override fun applyAsLong(row: Long, flags: Long): Long {
        val setMask = flags and (-1L ushr 32)
        val o: Long
        val a: Long
        when(flags and InternalGoban.NEW_MASK) {
            InternalGoban.NEW_BLACK -> {
                o = setMask
                a = (setMask shl 32).inv()
            }
            InternalGoban.NEW_WHITE -> {
                o = setMask shl 32
                a = setMask.inv()
            }
            else -> {
                o = 0L
                a = (setMask * InternalGoban.MASK).inv()
            }
        }
        return (row or o) and a
    }

}