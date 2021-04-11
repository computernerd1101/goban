@file:JvmMultifileClass
@file:JvmName("GobanKt")

package com.computernerd1101.goban

import com.computernerd1101.goban.internal.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed class AbstractGoban(
    @JvmField val width: Int,
    @JvmField val height: Int,
    internal val rows: GobanRows1,
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

    constructor() : this(19, 19, InternalGoban.newRows(false, 19), 0L)

    constructor(width: Int, height: Int): this(
        width, height, InternalGoban.newRows(width > 32, height), 0L
    )

    constructor(other: AbstractGoban, rows: GobanRows1): this(
        other.width, other.height,
        rows, InternalGoban.copyRows(other.rows, rows)
    )

    constructor(other: AbstractGoban): this(other, other.rows.newInstance())

    val blackCount: Int
        @JvmName("blackCount")
        get() = count.toInt()

    val whiteCount: Int
        @JvmName("whiteCount")
        get() = (count shr 32).toInt()

    val emptyCount: Int
        @JvmName("emptyCount")
        get() {
            // count might not be atomic on 32-bit architecture
            val count = InternalGoban.count[this]
            return width*height - (count + (count shr 32)).toInt()
        }

    fun count(color: GoColor?): Int {
        val count = InternalGoban.count[this]
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

    @JvmOverloads
    fun getChain(p: GoPoint, chain: MutableGoPointSet? = null): GoPointSet =
        getChain(p.x, p.y, chain)

    @JvmOverloads
    fun getChain(x: Int, y: Int, chain: MutableGoPointSet? = null): GoPointSet =
        getChainOrGroup(x, y, chain, isChain = true)

    @JvmOverloads
    fun  getGroup(p: GoPoint, group: MutableGoPointSet? = null): GoPointSet =
        getGroup(p.x, p.y, group)

    @JvmOverloads
    fun getGroup(x: Int, y: Int, chain: MutableGoPointSet? = null): GoPointSet =
        getChainOrGroup(x, y, chain, isChain = false)

    private fun getChainOrGroup(x: Int, y: Int, chainOrGroup: MutableGoPointSet?, isChain: Boolean): GoPointSet {
        if (x !in 0 until width)
            throw InternalGoban.indexOutOfBoundsException(x, width)
        if (y !in 0 until height)
            throw InternalGoban.indexOutOfBoundsException(y, height)
        GobanBulk.threadLocalGoban(width, height, rows)
        val arrays: Array<LongArray> = GobanThreadLocals.INSTANCE.get()
        val black = arrays[GobanThreadLocals.BLACK]
        val white = arrays[GobanThreadLocals.WHITE]
        val xBit = 1L shl x
        val playerRows: LongArray
        val player: Int
        val opponent: Int
        when {
            black[y] and xBit != 0L -> {
                playerRows = black
                player = GobanThreadLocals.BLACK
                opponent = GobanThreadLocals.WHITE
            }
            white[y] and xBit != 0L -> {
                playerRows = white
                player = GobanThreadLocals.WHITE
                opponent = GobanThreadLocals.BLACK
            }
            chainOrGroup == null -> return GoPointSet.EMPTY
            else -> {
                chainOrGroup.clear()
                return chainOrGroup
            }
        }
        GobanBulk.isAlive(width, height, xBit, y, player, opponent, onlyChain = isChain, shortCircuit = false)
        val set = chainOrGroup ?: GoPointSet(InternalGoPointSet)
        val rows = arrays[GobanThreadLocals.CHAIN]
        for(i in 0..51) InternalGoPointSet.copyRowFrom(set, i, rows[i] and playerRows[i])
        // If the parameter chainOrGroup == null, then set will be an immutable GoPointSet
        // that is not empty because it is guaranteed to contain the starting parameter p,
        // unless this[p] == null, in which case this method would have returned already.
        return set
    }

    fun toPointSet(color: GoColor?): GoPointSet {
        val points = GoPointSet(InternalGoPointSet) // creates a separate instance from GoPointSet.EMPTY
        initPointSet(points, color)
        val words = InternalGoPointSet.sizeAndHash(points)
        if (words == 0L) return GoPointSet.EMPTY
        InternalGoPointSet.sizeAndHash[points] = words
        return points
    }

    fun toMutablePointSet(color: GoColor?): MutableGoPointSet {
        val points = MutableGoPointSet()
        initPointSet(points, color)
        InternalGoPointSet.sizeAndHash[points] = InternalGoPointSet.sizeAndHash(points)
        return points
    }

    private fun initPointSet(points: GoPointSet, color: GoColor?) {
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
            InternalGoPointSet.rowUpdaters[y][points] = setRow
        }
    }

    infix fun contentEquals(other: AbstractGoban): Boolean {
        if (width != other.width || height != other.height) return false
        for(i in 0 until rows.size) if (rows[i] != other.rows[i]) return false
        return true
    }

    override fun toString(): String {
        return "${javaClass.name}[${width}x$height]"
    }

}

@Suppress("unused")
@OptIn(ExperimentalContracts::class)
fun AbstractGoban?.isNullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@isNullOrEmpty != null)
    }
    return this?.isEmpty() != false
}

@Suppress("unused")
infix fun AbstractGoban?.contentEquals(other: AbstractGoban?) = AbstractGoban.contentEquals(this, other)

fun FixedGoban(width: Int, height: Int) = FixedGoban.empty(width, height)
fun FixedGoban(size: Int) = FixedGoban.empty(size)
fun FixedGoban() = FixedGoban.EMPTY

fun FixedGoban(width: Int, height: Int, builder: Goban.() -> Unit): FixedGoban =
    Goban(width, height).apply(builder).readOnly()
fun FixedGoban(size: Int, builder: Goban.() -> Unit): FixedGoban = Goban(size).apply(builder).readOnly()
fun FixedGoban(builder: Goban.() -> Unit): FixedGoban = Goban().apply(builder).readOnly()

class FixedGoban: AbstractGoban {

    private val hash: Int
    private val isPlayable: Boolean

    internal constructor(width: Int, height: Int, rows: GobanRows1, count: Long):
            super(width, height, rows, count) {
        var hash = 0
        for(i in 0 until rows.size) {
            val row = rows[i]
            val black = row.toInt()
            val white = (row ushr 32).toInt()
            hash = 31*hash + (black + (white shl -width))
        }
        this.hash = hash
        GobanBulk.threadLocalGoban(width, height, rows)
        val arrays: Array<LongArray> = GobanThreadLocals.INSTANCE.get()
        val group = arrays[GobanThreadLocals.GROUP]
        GobanBulk.clear(group)
        val blackRows = arrays[GobanThreadLocals.BLACK]
        val whiteRows = arrays[GobanThreadLocals.WHITE]
        for(y in 0 until height) {
            val black = blackRows[y]
            var unseen = black or whiteRows[y]
            while(unseen != 0L) {
                val xBit = unseen and -unseen
                unseen -= xBit
                if (group[y] and xBit != 0L) continue
                val player: Int
                val opponent: Int
                if (black and xBit != 0L) {
                    player = GobanThreadLocals.BLACK
                    opponent = GobanThreadLocals.WHITE
                } else {
                    player = GobanThreadLocals.WHITE
                    opponent = GobanThreadLocals.BLACK
                }
                if (!GobanBulk.isAlive(width, height, xBit, y, player, opponent)) { // updates chain
                    isPlayable = false
                    return
                }
                GobanBulk.addChainToGroup(height)
            }
        }
        isPlayable = true
    }

    private constructor(width: Int, height: Int, cache: Cache):
            super(width, height, InternalGoban.emptyRows(width > 32, height), 0L) {
        hash = 0
        isPlayable = true
        cache.empty[width + 52*height - 53] = this // (width - 1) + 52*(height - 1)
    }

    private object Cache {

        @JvmField val empty = unsafeArrayOfNulls<FixedGoban>(52*52)

    }

    companion object {

        init {
            for(height in 1..52) for(width in 1..52)
                FixedGoban(width, height, Cache)
        }

        @JvmStatic
        fun empty(width: Int, height: Int): FixedGoban {
            return when {
                width !in 1..52 -> throw InternalGoban.illegalSizeException(width)
                height !in 1..52 -> throw InternalGoban.illegalSizeException(height)
                else -> Cache.empty[width + 52*height - 53] // (width - 1) + 52*(height - 1)
            }
        }

        @JvmStatic
        fun empty(size: Int = 19): FixedGoban {
            if (size !in 1..52) throw InternalGoban.illegalSizeException(size)
            return Cache.empty[(size - 1)*53] // size + 52*size - 53
        }

        @JvmField
        val EMPTY = Cache.empty[18*53] // size=19

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
    override fun editable(): AbstractMutableGoban =
        if (isPlayable) playable() else edit()

    override fun equals(other: Any?): Boolean {
        return other is FixedGoban && this contentEquals other
    }

    override fun hashCode() = hash

}

sealed class AbstractMutableGoban: AbstractGoban {

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
        if (points is GoPointSet || points is GoRectangle)
            return GobanBulk.setAll(this, points, color)
        var changed = false
        for(p in points)
            if (p.x < width && p.y < height && InternalGoban.set(this, p.x, p.y, color, color) != color)
                changed = true
        return changed
    }

    @JvmOverloads
    fun copyFrom(goban: AbstractGoban, mask: Set<GoPoint>? = null): Boolean {
        if (width != goban.width || height != goban.height) return false
        val rows = this.rows
        var diff: Long
        if (mask == null) {
            diff = InternalGoban.copyRows(goban.rows, rows)
        } else {
            diff = 0
            GobanBulk.threadLocalGoban(width, height, goban.rows)
            val copyRows = GobanThreadLocals.INSTANCE
            val group: LongArray = copyRows.get()[GobanThreadLocals.GROUP]
            when(val set: Set<*> = mask) {
                is GoPointSet -> for(y in 0 until height)
                    group[y] = InternalGoPointSet.rowUpdaters[y][set]
                is GoRectangle -> {
                    val rowBits = InternalGoRectangle.rowBits(set)
                    val y1 = set.start.y
                    val y2 = set.end.y
                    for(y in 0 until y1) group[y] = 0L
                    for(y in y1..minOf(y2, height - 1)) group[y] = rowBits
                    for(y in y2 + 1 until height) group[y] = 0L
                }
                else -> {
                    for(y in 0 until height) group[y] = 0L
                    for (element in set) if (element is GoPoint) {
                        val y = element.y
                        if (y < height) group[y] = group[y] or (1L shl element.x)
                    }
                }
            }
            val wide = width > 32
            for(y in 0 until rows.size) {
                var pos = y.toLong()
                pos = if (wide) pos.and(-2L).shl(31) or pos.and(1L).shl(5)
                else pos shl 32
                val oldRow = GobanRows.updaters[y].getAndAccumulate(rows, pos, copyRows)
                val newRow = copyRows.applyAsLong(oldRow, pos)
                diff += InternalGoban.countStonesInRow(newRow) - InternalGoban.countStonesInRow(oldRow)
            }
        }
        InternalGoban.count.addAndGet(this, diff)
        return true
    }

    @JvmOverloads
    fun clear(color: GoColor? = null) {
        val rows = this.rows
        var count = 0L
        val mask: Long = when(color) {
            null -> -1L
            GoColor.BLACK -> -1L ushr 32
            else -> -1L shl 32
        }
        for (i in 0 until rows.size) {
            val updater = GobanRows.updaters[i]
            val row: Long = if (color == null) updater.getAndSet(rows, 0L)
            else updater.getAndAccumulate(rows, mask.inv(), LongBinOp.AND)
            count -= InternalGoban.countStonesInRow(row) and mask
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
        if (isEmpty()) return FixedGoban(width, height)
        val oldRows = this.rows
        val rows = InternalGoban.newRows(width > 32, height)
        val count = InternalGoban.copyRows(oldRows, rows)
        if (count == 0L) return FixedGoban(width, height)
        return FixedGoban(width, height, rows, count)
    }

    /**
     * Returns this instance.
     * @return this instance
     */
    override fun editable() = this

}

class MutableGoban: AbstractMutableGoban {

    companion object;

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

    constructor(width: Int, height: Int): super(width, height)

    constructor(size: Int): super(size)

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

    fun play(p: GoPoint?, stone: GoColor): Boolean = p != null && play(p.x, p.y, stone)

    fun play(x: Int, y: Int, stone: GoColor): Boolean {
        if (x < 0 || y < 0 || x >= width || y >= height ||
            InternalGoban.set(this, x, y, stone, null) != null)
            return false
        val arrays: Array<LongArray> = GobanThreadLocals.INSTANCE.get()
        val group = arrays[GobanThreadLocals.GROUP]
        GobanBulk.clear(group)
        val chain = arrays[GobanThreadLocals.CHAIN]
        val rows = this.rows
        GobanBulk.threadLocalGoban(width, height, rows)
        val player: Int
        val opponent: Int
        if (stone == GoColor.BLACK) {
            player = GobanThreadLocals.BLACK
            opponent = GobanThreadLocals.WHITE
        } else {
            player = GobanThreadLocals.WHITE
            opponent = GobanThreadLocals.BLACK
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
            GobanBulk.setAll(this, chain, null)
        return true
    }

    private fun checkNeighbor(xBit: Long, y: Int, player: Int, opponent: Int): Int {
        val arrays: Array<LongArray> = GobanThreadLocals.INSTANCE.get()
        val group = arrays[GobanThreadLocals.GROUP]
        val chain = arrays[GobanThreadLocals.CHAIN]
        val playerRows = arrays[player]
        val opponentRows = arrays[opponent]
        if (playerRows[y] and xBit != 0L) return 2 // multi-stone
        if (opponentRows[y] and xBit == 0L) return 1 // alive
        if (group[y] and xBit != 0L) return 0
        if (GobanBulk.isAlive(width, height, xBit, y, opponent, player)) {
            GobanBulk.addChainToGroup(height)
            return 0
        }
        GobanBulk.setAll(this, chain, null)
        for(i in 0 until height)
            opponentRows[i] = opponentRows[i] and chain[i].inv()
        return 1 // alive
    }

    val scoreGoban: MutableGoban get() = getScoreGoban()

    fun getScoreGoban(territory: Boolean) = getScoreGoban(territory, null)

    @Suppress("unused")
    fun getScoreGoban(score: MutableGoban?) = getScoreGoban(false, score)

    fun getScoreGoban(territory: Boolean = false, score: MutableGoban? = null): MutableGoban {
        GobanBulk.threadLocalGoban(width, height, rows)
        val arrays: Array<LongArray> = GobanThreadLocals.INSTANCE.get()
        val group = arrays[GobanThreadLocals.GROUP]
        val black = arrays[GobanThreadLocals.BLACK]
        val white = arrays[GobanThreadLocals.WHITE]
        val blackScore = arrays[GobanThreadLocals.BLACK_SCORE]
        val whiteScore = arrays[GobanThreadLocals.WHITE_SCORE]
        GobanBulk.clear(blackScore)
        GobanBulk.clear(whiteScore)
        val mask = (1L shl width) - 1L
        for(y in 0 until height)
            group[y] = mask xor (black[y] or white[y])
        for(y in height..51)
            group[y] = 0L
        for(y in 0 until height)
            while(group[y] != 0L) nextScore(y)
        if (territory) {
            do {
                for (y in 0 until height)
                    group[y] = black[y] or white[y]
                var hasFalseEyes = false
                for (y in 0 until height) while (group[y] != 0L)
                    if (nextFalseEye(y)) hasFalseEyes = true
            } while(hasFalseEyes)
        } else for(y in 0 until height) {
            blackScore[y] = blackScore[y] or black[y]
            whiteScore[y] = whiteScore[y] or white[y]
        }
        val scoreGoban = if (score != null && width == score.width && height == score.height) {
            score.clear()
            score
        } else MutableGoban(width, height)
        GobanBulk.setAll(scoreGoban, blackScore, GoColor.BLACK)
        GobanBulk.setAll(scoreGoban, whiteScore, GoColor.WHITE)
        return scoreGoban
    }

    private fun nextScore(y: Int) {
        val maxBit = 1L shl (width - 1)
        val arrays: Array<LongArray> = GobanThreadLocals.INSTANCE.get()
        val group = arrays[GobanThreadLocals.GROUP]
        val chain = arrays[GobanThreadLocals.CHAIN]
        val pending = arrays[GobanThreadLocals.PENDING]
        val black = arrays[GobanThreadLocals.BLACK]
        val white = arrays[GobanThreadLocals.WHITE]
        val blackScore = arrays[GobanThreadLocals.BLACK_SCORE]
        val whiteScore = arrays[GobanThreadLocals.WHITE_SCORE]
        GobanBulk.clear(chain)
        GobanBulk.clear(pending)
        var groupRow = group[y]
        var xBit = groupRow and -groupRow
        group[y] = groupRow - xBit
        chain[y] = xBit
        var y1 = y
        var pendingY = y
        var hasBlack = false
        var hasWhite = false
        pop@ while(true) {
            var y2 = y1 - 1 // above
            if (y2 >= 0) {
                groupRow = group[y2]
                when {
                    black[y2] and xBit != 0L -> hasBlack = true
                    white[y2] and xBit != 0L -> hasWhite = true
                    groupRow and xBit != 0L -> {
                        group[y2] = groupRow - xBit
                        chain[y2] = chain[y2] or xBit
                        pendingY = y2
                        pending[y2] = pending[y2] or xBit
                    }
                }
            }
            var bits = 0L
            var xBit2 = xBit shr 1 // left
            groupRow = group[y1]
            if (xBit2 > 0) when {
                black[y1] and xBit2 != 0L -> hasBlack = true
                white[y1] and xBit2 != 0L -> hasWhite = true
                groupRow and xBit2 != 0L -> {
                    group[y1] = groupRow - xBit2
                    chain[y1] = chain[y1] or xBit2
                    bits = xBit2
                }
            }
            xBit2 = xBit shl 1 // right
            if (xBit2 <= maxBit) when {
                black[y1] and xBit2 != 0L -> hasBlack = true
                white[y1] and xBit2 != 0L -> hasWhite = true
                groupRow and xBit2 != 0L -> {
                    group[y1] = groupRow - xBit2
                    chain[y1] = chain[y1] or xBit2
                    bits = bits or xBit2
                }
            }
            if (bits != 0L) pending[y1] = pending[y1] or bits
            y2 = y1 + 1 // below
            if (y2 < height) {
                groupRow = group[y2]
                when {
                    black[y2] and xBit != 0L -> hasBlack = true
                    white[y2] and xBit != 0L -> hasWhite = true
                    groupRow and xBit != 0L -> {
                        group[y2] = groupRow - xBit
                        chain[y2] = chain[y2] or xBit
                        pending[y2] = pending[y2] or xBit
                    }
                }
            }
            // pop next point
            while(pendingY < height) {
                val row = pending[pendingY]
                if (row != 0L) {
                    xBit = row and -row
                    pending[pendingY] = row - xBit
                    y1 = pendingY
                    // found next point
                    continue@pop
                }
                pendingY++
            }
            // no more points
            if (hasBlack xor hasWhite) for(i in y..y1) {
                if (hasBlack) blackScore[i] = blackScore[i] or chain[i]
                else whiteScore[i] = whiteScore[i] or chain[i]
            }
            return
        }
    }

    private fun nextFalseEye(y: Int): Boolean {
        val arrays: Array<LongArray> = GobanThreadLocals.INSTANCE.get()
        val group = arrays[GobanThreadLocals.GROUP]
        val chain = arrays[GobanThreadLocals.CHAIN]
        val black = arrays[GobanThreadLocals.BLACK]
        var row = group[y]
        val xBit = row and -row
        val player: Int
        val opponent: Int
        val score: Int
        if (black[y] and xBit != 0L) {
            player = GobanThreadLocals.BLACK
            opponent = GobanThreadLocals.WHITE
            score = GobanThreadLocals.BLACK_SCORE
        } else {
            player = GobanThreadLocals.WHITE
            opponent = GobanThreadLocals.BLACK
            score = GobanThreadLocals.WHITE_SCORE
        }
        val hasFalseEye = !GobanBulk.isAlive(width, height, xBit, y, player, opponent,
            falseEyeScore = score, shortCircuit = false)
        for(y1 in y until height) {
            row = chain[y1]
            if (row == 0L) break
            group[y1] = group[y1] and row.inv()
        }
        return hasFalseEye
    }

}

