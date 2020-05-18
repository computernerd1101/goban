@file:OptIn(ExperimentalStdlibApi::class)

package com.computernerd1101.goban.internal

import com.computernerd1101.goban.*
import java.util.concurrent.atomic.*
import java.util.function.LongBinaryOperator
import java.util.function.Supplier

object InternalGoban: LongBinaryOperator {

    interface AbstractSecrets {
        fun rows(goban: AbstractGoban): AtomicLongArray
    }
    var abstractSecrets: AbstractSecrets by SecretKeeper { AbstractGoban }

    lateinit var count: AtomicLongFieldUpdater<AbstractGoban>

    interface FixedSecrets {
        fun copy(goban: AbstractGoban): FixedGoban
    }
    var fixedSecrets: FixedSecrets by SecretKeeper { FixedGoban }

    interface EditableSecrets<out G: AbstractMutableGoban> {
        fun goban(width: Int, height: Int, rows: AtomicLongArray, count: Long): G
    }
    var mutableSecrets: EditableSecrets<MutableGoban> by SecretKeeper { MutableGoban }
    var playableSecrets: EditableSecrets<Goban> by SecretKeeper { Goban }

    fun copyRows(src: AtomicLongArray, dst: AtomicLongArray): Long {
        var count = 0L
        for(i in 0 until src.length()) {
            val row = src[i]
            dst[i] = row
            val blackStones = row.ushr(32).toInt()
            val whiteStones = row.toInt() and blackStones.inv()
            count += blackStones.countOneBits().toLong() +
                    whiteStones.countOneBits().toLong().shl(32)
        }
        return count
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
        val row = abstractSecrets.rows(goban).getAndAccumulate(y2,
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
        val bit = 1L shl x2
        val recount: Long
        val wasEmpty = row and bit == 0L
        val wasBlack = row and (bit shl 32) != 0L
        val actual: GoColor?
        if (newColor == null) {
            if (wasEmpty) return null
            actual = wasBlack.goBlackOrWhite()
            if (expected != null && expected != actual)
                return actual
            recount = if (wasBlack) -1L
            else -1L shl 32
        } else {
            if (wasEmpty) {
                if (expected != newColor && expected != null)
                    return null
                actual = null
            } else {
                actual = wasBlack.goBlackOrWhite()
                if (actual == newColor || (expected != newColor && expected != actual))
                    return actual
            }
            recount = when {
                newColor == GoColor.BLACK ->
                    if (wasEmpty) 1L
                    else 1L - (1L shl 32)
                wasEmpty -> 1L shl 32
                else -> (1L shl 32) - 1L
            }
        }
        count.addAndGet(goban, recount)
        return actual
    }

    override fun applyAsLong(row: Long, flags: Long): Long {
        val x = flags.toInt()
        val oldFlags = flags and EXPECT_BLACK
        if (oldFlags != 0L) {
            val mask = BLACK shl x
            val rowFlags: Long = when(oldFlags) {
                EXPECT_WHITE -> WHITE shl x
                EXPECT_BLACK -> mask
                else -> 0L
            }
            if (row and mask != rowFlags) return row
        }
        var o = 0L
        val a: Long = when(flags and NEW_BLACK) {
            NEW_BLACK -> {
                o = BLACK shl x
                -1L
            }
            NEW_WHITE -> {
                o = WHITE shl x
                ((1L shl 32) shl x).inv()
            }
            else -> (BLACK shl x).inv()
        }
        return (row or o) and a
    }

    fun get(width: Int, rows: AtomicLongArray, x: Int, y: Int): GoColor? {
        val row = rows[when {
            width <= 32 -> y
            x < 32 -> 2*y
            else -> 2*y + 1
        }]
        val bit = 1L shl (x and 31)
        return if (row and bit == 0L) null
        else (row and bit.shl(32) != 0L).goBlackOrWhite()
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

    const val NEW_BLACK = 3L shl 32
    const val NEW_WHITE = 1L shl 32

    const val EXPECT_EMPTY = 2L shl 34
    const val EXPECT_BLACK = 3L shl 34
    const val EXPECT_WHITE = 1L shl 34

    const val BLACK = 1L + (1L shl 32)
    const val WHITE = 1L

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
                if (setRow(goban, i++, row.ushr(32).toInt() and mask2, color))
                    modified = true
        }
        return modified
    }

    private fun setRow(goban: AbstractMutableGoban, i: Int, setMask: Int, color: GoColor?): Boolean {
        val row = InternalGoban.abstractSecrets.rows(goban).getAndAccumulate(i,
            setMask.toLong().and(-1L ushr 32) or when(color) {
                null -> 0L
                GoColor.BLACK -> InternalGoban.NEW_BLACK
                GoColor.WHITE -> InternalGoban.NEW_WHITE
            }, this)
        val blackStones = row.ushr(32).toInt()
        val whiteStones = row.toInt() and blackStones.inv()
        val recount: Long
        when(color) {
            null -> {
                if (row.toInt() and setMask == 0) return false
                recount = -blackStones.and(setMask).countOneBits().toLong() -
                        whiteStones.and(setMask).countOneBits().toLong().shl(32)
            }
            GoColor.BLACK -> {
                if (blackStones or setMask == blackStones) return false
                recount = blackStones.inv().and(setMask).countOneBits().toLong() -
                        whiteStones.and(setMask).countOneBits().toLong().shl(32)
            }
            GoColor.WHITE -> {
                if (whiteStones or setMask == whiteStones) return false
                recount = whiteStones.inv().and(setMask).countOneBits().toLong().shl(32) -
                        blackStones.and(setMask).countOneBits().toLong()
            }
        }
        InternalGoban.count.addAndGet(goban, recount)
        return true
    }

    override fun applyAsLong(row: Long, flags: Long): Long {
        val setMask = flags and (-1L ushr 32)
        return when {
            flags and (1L shl 32) == 0L -> // empty
                row and (setMask or setMask.shl(32)).inv()
            flags and (1L shl 33) != 0L -> // black
                row or (setMask or setMask.shl(32))
            else -> (row or setMask) and setMask.shl(32).inv() // white
        }
    }

}