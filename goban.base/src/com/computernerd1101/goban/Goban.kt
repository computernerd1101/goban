@file:Suppress("NOTHING_TO_INLINE")
@file:JvmMultifileClass
@file:JvmName("GobanKt")

package com.computernerd1101.goban

import com.computernerd1101.goban.internal.*
import java.util.concurrent.atomic.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed class AbstractGoban(
    @JvmField val width: Int,
    @JvmField val height: Int,
    private val rows: AtomicLongArray,
    /** Updated by [InternalGoban.count] */
    @Volatile private var count: Long
) {

    companion object {

        init {
            InternalGoban.count = atomicLongUpdater("count")
        }

        @JvmStatic
        fun contentEquals(a: AbstractGoban?, b: AbstractGoban?) = when {
            a == null -> b == null
            b == null -> false
            else -> a contentEquals b
        }

    }

    constructor() : this(19, 19, AtomicLongArray(19), 0L)

    constructor(width: Int, height: Int): this(
        width, height, AtomicLongArray(if (width > 32) height*2 else height), 0L
    )

    constructor(other: AbstractGoban, rows: AtomicLongArray): this(
        other.width, other.height,
        rows, InternalGoban.copyRows(other.rows, rows)
    )

    constructor(other: AbstractGoban): this(other, AtomicLongArray(other.rows.length()))

    internal fun getRows(marker: InternalMarker) = marker.access { rows }

    val blackCount: Int
        @JvmName("blackCount")
        get() = count.toInt()

    val whiteCount: Int
        @JvmName("whiteCount")
        get() = (count shr 32).toInt()

    val emptyCount: Int
        @JvmName("emptyCount")
        get() {
            val count = this.count
            return width*height - (count + (count shr 32)).toInt()
        }

    fun count(color: GoColor?): Int {
        val count = this.count
        return when (color) {
            null -> width*height - (count + (count shr 32)).toInt()
            GoColor.BLACK -> count.toInt()
            GoColor.WHITE -> (count shr 32).toInt()
        }
    }

    fun isEmpty() = count == 0L

    fun isNotEmpty() = !isEmpty()

    operator fun get(x: Int, y: Int): GoColor? {
        if (x !in 0 until width)
            throw InternalGoban.indexOutOfBoundsException(x, width)
        if (y !in 0 until height)
            throw InternalGoban.indexOutOfBoundsException(y, height)
        return InternalGoban.get(width, rows, x, y)
    }

    operator fun get(p: GoPoint) = this[p.x, p.y]

    operator fun contains(p: GoPoint) = this[p] != null

    /**
     * Returns this instance if it is already a [FixedGoban], otherwise
     * copies the contents of this instance into a new [FixedGoban] instance,
     * or a pre-instantiated [FixedGoban] instance if it is an empty square.
     */
    abstract fun readOnly(): FixedGoban

    /**
     * Returns this instance if it is already a [Goban], otherwise
     * copies the contents of this instance into a new [Goban] instance.
     */
    abstract fun playable(): Goban

    /**
     * Returns this instance if it is already a [MutableGoban], otherwise
     * copies the contents of this instance into a new [MutableGoban] instance.
     */
    abstract fun edit(): MutableGoban

    /**
     * Returns this instance if it is already an [AbstractMutableGoban], otherwise
     * copies the contents of this instance into a new [Goban] instance
     * if the stones are in a legal board position, or a new [MutableGoban] instance
     * if the board contains any stones that should have been captured.
     */
    abstract fun editable(): AbstractMutableGoban

    fun toPointSet(color: GoColor?): GoPointSet {
        val rows = toPointSetBits(color)
        val words = InternalGoPointSet.sizeAndHash(rows)
        return if (words == 0L) GoPointSet.EMPTY else GoPointSet(rows, words, InternalMarker)
    }

    fun toMutablePointSet(color: GoColor?): MutableGoPointSet {
        return MutableGoPointSet(toPointSetBits(color), InternalMarker)
    }

    private fun toPointSetBits(color: GoColor?): AtomicLongArray {
        val rows = AtomicLongArray(52)
        var i = 0
        for(y in 0 until height) {
            var row = this.rows[i++]
            var setRow: Long = when(color) {
                GoColor.BLACK -> row and (-1L ushr 32) // setRow.lo = row.lo
                GoColor.WHITE -> row ushr 32 // setRow.lo = row.hi
                else -> (row or (row ushr 32)).inv() and (-1L ushr 32)
            }
            if (width > 32) {
                row = this.rows[i++]
                setRow = setRow or when(color) {
                    GoColor.BLACK -> row shl 32 // setRow.hi = row.lo
                    GoColor.WHITE -> row and (-1L shl 32) // setRow.hi = row.hi
                    else -> (row or (row shl 32)).inv() and (-1L shl 32)
                }
            }
            rows[y] = setRow
        }
        return rows
    }

    infix fun contentEquals(other: AbstractGoban): Boolean {
        if (width != other.width || height != other.height) return false
        for(i in 0 until rows.length()) if (rows[i] != other.rows[i]) return false
        return true
    }

    override fun toString(): String {
        return "${javaClass.name}[${width}x$height]"
    }

}

@Suppress("unused")
@OptIn(ExperimentalContracts::class)
inline fun AbstractGoban?.isNullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@isNullOrEmpty != null)
    }
    return this?.isEmpty() != false
}

@Suppress("unused")
inline infix fun AbstractGoban?.contentEquals(other: AbstractGoban?) = AbstractGoban.contentEquals(this, other)
@Suppress("FunctionName")
inline fun FixedGoban(width: Int, height: Int) = FixedGoban.empty(width, height)
@Suppress("FunctionName")
inline fun FixedGoban(size: Int) = FixedGoban.empty(size)
@Suppress("FunctionName")
inline fun FixedGoban() = FixedGoban.EMPTY

class FixedGoban: AbstractGoban {

    private val hash: Int

    private constructor(width: Int, height: Int, marker: InternalMarker): super(width, height) {
        marker.ignore()
        hash = 0
    }

    private constructor(size: Int, marker: InternalMarker): this(size, size, marker)

    internal constructor(width: Int, height: Int, rows: AtomicLongArray, count: Long, marker: InternalMarker):
            super(width, height, rows, count) {
        marker.ignore()
        var hash = 0
        for(i in 0 until rows.length()) {
            val row = rows[i]
            val black = row.toInt()
            val white = (row ushr 32).toInt()
            hash = 31*hash + (black + (white shl -width))
        }
        this.hash = hash
    }

    private object Cache {

        @JvmField val squares = unsafeArrayOfNulls<FixedGoban>(52)

    }

    companion object {

        init {
            for(size in 1..52) {
                Cache.squares[size - 1] = FixedGoban(size, InternalMarker)
            }
        }

        @JvmStatic
        fun empty(width: Int, height: Int): FixedGoban {
            return when {
                width !in 1..52 -> throw InternalGoban.illegalSizeException(width)
                width == height -> Cache.squares[width - 1]
                height !in 1..52 -> throw InternalGoban.illegalSizeException(height)
                else -> FixedGoban(width, height, InternalMarker)
            }
        }

        @JvmStatic
        fun empty(size: Int = 19): FixedGoban {
            if (size !in 1..52) throw InternalGoban.illegalSizeException(size)
            return Cache.squares[size - 1]
        }

        @JvmField
        val EMPTY = Cache.squares[18] // size=19

    }

    /**
     * Returns this instance.
     * @return this instance
     */
    override fun readOnly() = this

    /**
     * Returns a new [Goban] instance with the same contents
     * as this instance.
     * @return a new [Goban] instance with the same contents
     * as this instance
     */
    override fun playable() = Goban(this)

    /**
     * Returns a new [MutableGoban] instance with the same contents
     * as this instance.
     * @return a new [MutableGoban] instance with the same contents
     * as this instance
     */
    override fun edit() = MutableGoban(this)

    /**
     * Returns a new [AbstractMutableGoban] with the same contents
     * as this instance. The new instance will be a [Goban] if the
     * board position is legal, or a [MutableGoban] if it contains
     * any stones that should have been captured.
     * @return a new [AbstractMutableGoban] with the same contents
     * as this instance
     */
    override fun editable(): AbstractMutableGoban {
        val width = this.width
        val height = this.height
        val oldRows = getRows(InternalMarker)
        val rows = AtomicLongArray(oldRows.length())
        val count = InternalGoban.copyRows(oldRows, rows)
        GobanBulk.threadLocalGoban(width, height, rows)
        val arrays: Array<LongArray> = GobanThreadLocals.arrays()
        val metaCluster = arrays[GobanBulk.THREAD_META_CLUSTER]
        GobanBulk.clear(metaCluster)
        val blackRows = arrays[GobanBulk.THREAD_BLACK]
        val whiteRows = arrays[GobanBulk.THREAD_WHITE]
        rows@ for(y in 0 until height) {
            val black = blackRows[y]
            var unseen = black or whiteRows[y]
            while(unseen != 0L) {
                val xBit = unseen and -unseen
                unseen -= xBit
                if (metaCluster[y] and xBit != 0L) continue
                val player: Int
                val opponent: Int
                if (black and xBit != 0L) {
                    player = GobanBulk.THREAD_BLACK
                    opponent = GobanBulk.THREAD_WHITE
                } else {
                    player = GobanBulk.THREAD_WHITE
                    opponent = GobanBulk.THREAD_BLACK
                }
                if (!GobanBulk.isAlive(width, height, xBit, y, player, opponent)) // updates cluster
                    return MutableGoban(width, height, rows, count, InternalMarker)
                GobanBulk.updateMetaCluster(height)
            }
        }
        return Goban(width, height, rows, count, InternalMarker)
    }

    override fun equals(other: Any?): Boolean {
        return other is FixedGoban && this contentEquals other
    }

    override fun hashCode() = hash

}

sealed class AbstractMutableGoban: AbstractGoban {

    constructor(width: Int, height: Int, rows: AtomicLongArray, count: Long):
            super(width, height, rows, count)

    constructor(width: Int, height: Int): super(
        if (width in 1..52) width else throw InternalGoban.illegalSizeException(width),
        if (height in 1..52) height else throw InternalGoban.illegalSizeException(height)
    )

    constructor(size: Int): super(
        if (size in 1..52) size else throw InternalGoban.illegalSizeException(size),
        size
    )

    constructor(): super()

    constructor(other: AbstractGoban): super(other)

    operator fun set(x: Int, y: Int, stone: GoColor?) {
        InternalGoban.set(this, x, y, stone, stone)
    }

    operator fun set(p: GoPoint, stone: GoColor?) {
        this[p.x, p.y] = stone
    }

    fun setAll(points: Set<GoPoint>, color: GoColor?): Boolean {
        if (points is GoPointSet)
            return GobanBulk.setAll(this, points.getRows(InternalMarker), color)
        var changed = false
        for(p in points)
            if (InternalGoban.set(this, p.x, p.y, color, color) != color)
                changed = true
        return changed
    }

    @Suppress("unused")
    fun clear() {
        val rows = getRows(InternalMarker)
        var count = 0L
        for (i in 0 until rows.length()) {
            val row = rows.getAndSet(i, 0L)
            count -= InternalGoban.countStonesInRow(row)
        }
        InternalGoban.count.addAndGet(this, count)
    }

    /**
     * Returns a [FixedGoban] with the same contents as this instance.
     * The returned instance will be newly created unless it is an empty square,
     * in which case it will be a preexisting instance.
     * @return a [FixedGoban] with the same contents as this instance
     */
    override fun readOnly(): FixedGoban {
        val width = this.width
        val height = this.height
        if (width == height && isEmpty()) return FixedGoban(width)
        val oldRows = getRows(InternalMarker)
        val rows = AtomicLongArray(oldRows.length())
        val count = InternalGoban.copyRows(oldRows, rows)
        if (width == height && count == 0L) return FixedGoban(width)
        return FixedGoban(width, height, rows, InternalGoban.copyRows(oldRows, rows), InternalMarker)
    }

    /**
     * Returns this instance.
     * @return this instance
     */
    override fun editable() = this

}

class MutableGoban: AbstractMutableGoban {

    companion object;

    @Suppress("UNUSED_PARAMETER")
    internal constructor(width: Int, height: Int, rows: AtomicLongArray, count: Long, marker: InternalMarker):
            super(width, height, rows, count)

    constructor(width: Int, height: Int): super(width, height)

    @Suppress("unused")
    constructor(size: Int): super(size)

    @Suppress("unused")
    constructor()

    constructor(other: AbstractGoban): super(other)

    /**
     * Returns a new [Goban] instance with the same contents
     * as this instance.
     * @return a new [Goban] instance with the same contents
     * as this instance
     */
    override fun playable() = Goban(this)

    /**
     * Returns this instance.
     * @return this instance
     */
    override fun edit() = this

}

class Goban: AbstractMutableGoban {

    companion object;

    internal constructor(width: Int, height: Int, rows: AtomicLongArray, count: Long, marker: InternalMarker):
            super(width, height, rows, count) {
        marker.ignore()
    }

    constructor(width: Int, height: Int): super(width, height)

    @Suppress("unused")
    constructor(size: Int): super(size)

    @Suppress("unused")
    constructor(): super()

    constructor(other: AbstractGoban): super(other)

    /**
     * Returns this instance.
     * @return this instance
     */
    override fun playable() = this

    /**
     * Returns a new [MutableGoban] instance with the same contents
     * as this instance.
     * @return a new [MutableGoban] instance with the same contents
     * as this instance
     */
    override fun edit() = MutableGoban(this)

    fun play(p: GoPoint?, stone: GoColor): Boolean {
        if (p == null) return false
        val (x, y) = p
        if (x >= width || y >= height || InternalGoban.set(this, x, y, stone, null) != null)
            return false
        val arrays: Array<LongArray> = GobanThreadLocals.arrays()
        val metaCluster = arrays[GobanBulk.THREAD_META_CLUSTER]
        GobanBulk.clear(metaCluster)
        val cluster = arrays[GobanBulk.THREAD_CLUSTER]
        val rows = getRows(InternalMarker)
        GobanBulk.threadLocalGoban(width, height, rows)
        val player: Int
        val opponent: Int
        if (stone == GoColor.BLACK) {
            player = GobanBulk.THREAD_BLACK
            opponent = GobanBulk.THREAD_WHITE
        } else {
            player = GobanBulk.THREAD_WHITE
            opponent = GobanBulk.THREAD_BLACK
        }
        val xBit = 1L shl x
        var flags = if (y > 0) checkNeighbor(xBit, y - 1, player, opponent)
        else 0
        if (x < width - 1) flags = flags or checkNeighbor(xBit shl 1, y, player, opponent)
        if (y < height - 1) flags = flags or checkNeighbor(xBit, y + 1, player, opponent)
        if (x > 0) flags = flags or checkNeighbor(xBit shr 1, y, player, opponent)
        if (flags and 1 != 0) return true // alive
        if (flags and 2 == 0) { // not multi-stone
            InternalGoban.set(this, x, y, null, null)
            return false
        }
        if (!GobanBulk.isAlive(width, height, xBit, y, player, opponent))
            GobanBulk.setAll(this, cluster, null)
        return true
    }

    private fun checkNeighbor(xBit: Long, y: Int, player: Int, opponent: Int): Int {
        val arrays: Array<LongArray> = GobanThreadLocals.arrays()
        val metaCluster = arrays[GobanBulk.THREAD_META_CLUSTER]
        val cluster = arrays[GobanBulk.THREAD_CLUSTER]
        val playerRows = arrays[player]
        val opponentRows = arrays[opponent]
        if (playerRows[y] and xBit != 0L) return 2 // multi-stone
        if (opponentRows[y] and xBit == 0L) return 1 // alive
        if (metaCluster[y] and xBit != 0L) return 0
        if (GobanBulk.isAlive(width, height, xBit, y, opponent, player)) {
            GobanBulk.updateMetaCluster(height)
            return 0
        }
        GobanBulk.setAll(this, cluster, null)
        for(i in 0 until height)
            opponentRows[i] = opponentRows[i] and cluster[i].inv()
        return 1 // alive
    }

}

