@file:JvmMultifileClass
@file:JvmName("GobanKt")

package com.computernerd1101.goban

import com.computernerd1101.goban.internal.*
import com.computernerd1101.sgf.*
import java.util.concurrent.atomic.*

inline val emptyGoPointSet get() = GoPointSet.EMPTY

open class GoPointSet protected constructor(
    private val rows: AtomicLongArray,
    @Volatile private var sizeAndHash: Long
): Set<GoPoint> {

    companion object {

        init {
            InternalGoPointSet.secrets = object: InternalGoPointSet.Secrets {

                override fun init(rows: AtomicLongArray): GoPointSet {
                    val words = InternalGoPointSet.sizeAndHash(rows)
                    return if (words == 0L) EMPTY
                    else GoPointSet(rows, words)
                }

                override fun rows(set: GoPointSet) = set.rows

            }
            InternalGoPointSet.sizeAndHash = atomicLongUpdater("sizeAndHash")
        }

        @JvmField
        val EMPTY = GoPointSet(AtomicLongArray(52), 0)

        private val compressBuffer by threadLocal { LongArray(53) }

        @JvmStatic
        @JvmName("readOnly")
        operator fun invoke(vararg points: Iterable<GoPoint>): GoPointSet {
            val rows = InternalGoPointSet.toLongArray(points)
            val words = InternalGoPointSet.sizeAndHash(rows)
            return if (words == 0L) EMPTY
            else GoPointSet(rows, words)
        }

    }

    init {
        @Suppress("SpellCheckingInspection")
        val klass = javaClass
        if (klass != GoPointSet::class.java && klass != MutableGoPointSet::class.java)
            throw IllegalAccessError(
                "${klass.name} does not have permission to inherit from com.computernerd1101.goban.GoPointSet"
            )
    }

    override val size: Int
        get() = sizeAndHash.toInt()

    override fun isEmpty() = size == 0

    override fun iterator(): Iterator<GoPoint> = GoPointItr(rows)

    private var compressed: Array<GoRectangle>? = null

    private fun compressed(): Array<GoRectangle> {
        var compressed = if (this is MutableGoPointSet) null else this.compressed
        if (compressed == null) {
            val rows = compressBuffer
            for (i in 0..51)
                rows[i] = this.rows[i]
            var bit = 0L
            var x = 51
            var yMin = 0
            var xMax = 0
            var yMax = 51
            while (rows[yMax] == 0L)
                if (--yMax < 0) return emptyArray()
            while (rows[yMin] == 0L) yMin++
            for (y in yMin..yMax) {
                val row = rows[y]
                if (row != 0L) {
                    val b1 = row and -row
                    val x1 = trailingZerosPow2(b1)
                    val x2 = highestBitLength(row)
                    if (x > x1) {
                        x = x1
                        bit = b1
                    }
                    if (xMax < x2) xMax = x2
                }
            }
            val list = mutableListOf<GoRectangle>()
            while (x <= xMax) {
                for (y in yMin..yMax) {
                    val row = rows[y]
                    if (row and bit == 0L)
                        continue
                    // starting point found
                    var rowMask = bit
                    var bit2 = bit
                    var x2 = x
                    var y2 = y // upper-left corner
                    var expandX = true
                    var expandY = true
                    while (expandX || expandY) { // still expanding area?
                        if (expandX && row and (bit2 shl 1) != 0L) {
                            var m = y
                            while (m <= y2) {
                                if (rows[m] and (bit2 shl 1) == 0L) break
                                m++
                            }
                            if (m > y2) {
                                // new column ok?
                                x2++
                                bit2 = bit2 shl 1
                                rowMask += bit2
                            } else // x limit reached
                                expandX = false
                        } else
                            expandX = false
                        if (expandY && rows[y2 + 1] and rowMask == rowMask) // new row ok?
                            y2++
                        else // y limit reached
                            expandY = false
                    }
                    list.add(GoPoint(x, y) rect GoPoint(x2, y2))
                    while (y2 >= y) {
                        rows[y2] = rows[y2] and rowMask.inv()
                        y2--
                    }
                }
                x++
                bit = bit shl 1
            }
            compressed = list.toTypedArray()
            compressed.sort()
            if (this !is MutableGoPointSet)
                this.compressed = compressed
        }
        return compressed
    }

    @Suppress("unused")
    fun compress(): Array<GoRectangle> {
        val compressed = compressed()
        return  if (this is MutableGoPointSet) compressed
        else compressed.clone()
    }

    fun toSGFProperty(compress: Boolean): SGFProperty? {
        if (size == 0) return null
        var prop: SGFProperty? = null
        if (compress) {
            val compressed = compressed()
            if (compressed.isEmpty()) return null
            for((first, second) in compressed) {
                val value = SGFValue(first.toSGFBytes())
                if (first != second)
                    value.list.add(second.toSGFBytes())
                prop?.list?.add(value) ?: SGFProperty(value).let { prop = it }
            }
        } else for(point in this) {
            val value = SGFValue(point.toSGFBytes())
            prop?.list?.add(value) ?: SGFProperty(value).let { prop = it }
        }
        return prop
    }

    fun copy(): MutableGoPointSet {
        val rows = AtomicLongArray(52)
        for(i in 0..51) rows[i] = this.rows[i]
        return InternalGoPointSet.mutableSecrets.init(rows)
    }

    open fun readOnly() = this

    override fun contains(element: GoPoint): Boolean {
        return rows[element.y] and (1L shl element.x) != 0L
    }

    override fun containsAll(elements: Collection<GoPoint>): Boolean {
        when(elements) {
            is GoPointSet -> for(y in 0..51)
                if (rows[y].inv() and elements.rows[y] != 0L) return false
            is GoRectangle -> {
                val bits = 1L.shl(elements.end.x + 1) - 1L.shl(elements.start.x)
                for(y in elements.start.y..elements.end.y)
                    if (rows[y].inv() and bits != 0L) return false
            }
            is GoPointKeys<*> -> {
                elements.expungeStaleRows()
                for(y in 0..51)
                    if (rows[y].inv() and elements.rowBits(y) != 0L) return false
            }
            is GoPointEntries<*, *> -> {
                elements.expungeStaleRows()
                return elements.isEmpty()
            }
            else -> return (elements as Iterable<*>).all {
                it is GoPoint && this.contains(it)
            }
        }
        return true
    }

    override fun equals(other: Any?): Boolean {
        return this === other || when(other) {
            is GoPointSet -> {
                if (size != other.size) return false
                for(i in 0..51)
                    if (rows[i] != other.rows[i]) return false
                true
            }
            is GoPointKeys<*> -> {
                // automatically calls other.expungeStaleRows()
                if (size != other.size) return false
                for(i in 0..51) {
                    if (rows[i] != other.rowBits(i)) return false
                }
                true
            }
            is GoRectangle -> {
                val bits: Long = 1L.shl(other.end.x + 1) - 1L.shl(other.start.x)
                for(y in 0..51) {
                    if (rows[y] != if (y in other.start.y..other.end.y) bits else 0)
                        return false
                }
                true
            }
            is Set<*> -> size == other.size && other.all {
                it is GoPoint && contains(it)
            }
            else -> false
        }
    }

    override fun hashCode(): Int {
        return (sizeAndHash shr 32).toInt()
    }

    private var string: String? = null

    override fun toString(): String {
        var s = if (this is MutableGoPointSet) null else string
        if (s == null) {
            val buffer = StringBuilder()
            s = compressed().joinTo(buffer, ", ", "[", "]") {
                val start = it.start.toString()
                val size = it.size
                if (size == 1) start
                else {
                    buffer.append(start).append(if (size == 2) ", " else ", ..., ")
                    it.end.toString()
                }
            }.toString()
            if (this !is MutableGoPointSet)
                string = s
        }
        return s
    }

}

class MutableGoPointSet: GoPointSet, MutableSet<GoPoint> {

    private constructor(rows: AtomicLongArray): super(rows, InternalGoPointSet.sizeAndHash(rows))

    constructor() : super(AtomicLongArray(52), 0L)

    @Suppress("unused")
    constructor(vararg points: Iterable<GoPoint>) : this(InternalGoPointSet.toLongArray(points))

    override fun readOnly(): GoPointSet {
        if (isEmpty()) return emptyGoPointSet
        val secrets = InternalGoPointSet.secrets
        val rows = secrets.rows(this)
        val newRows = AtomicLongArray(52)
        for(i in 0..51) newRows[i] = rows[i]
        return secrets.init(newRows)
    }

    override fun iterator(): MutableIterator<GoPoint> = object :
        GoPointItr(InternalGoPointSet.secrets.rows(this)), MutableIterator<GoPoint> {
        override fun remove() {
            val (x, y) = lastReturned ?: throw IllegalStateException()
            val mask = (1L shl x).inv()
            val row = rows.getAndAccumulate(y, mask, LongBinOp.AND)
            if (row and mask != row) {
                InternalGoPointSet.sizeAndHash.addAndGet(this@MutableGoPointSet,
                    -(x + y*52L).shl(32) - 1L)
            }
            lastReturned = null
        }
    }

    override fun add(element: GoPoint): Boolean {
        val (x, y) = element
        val bit = 1L shl x
        val secrets = InternalGoPointSet.secrets
        val oldBits = secrets.rows(this).getAndAccumulate(y, bit, LongBinOp.OR)
        return if (oldBits and bit == 0L) {
            InternalGoPointSet.sizeAndHash.addAndGet(this,
                (x + y*52L).shl(32) + 1L)
            true
        } else false
    }

    override fun remove(element: GoPoint): Boolean {
        val (x, y) = element
        val bit = 1L shl x
        val secrets = InternalGoPointSet.secrets
        val oldBits = secrets.rows(this).getAndAccumulate(y, bit.inv(), LongBinOp.AND)
        return if (oldBits and bit != 0L) {
            InternalGoPointSet.sizeAndHash.addAndGet(this,
                -((x + y*52L) shl 32) - 1L)
            true
        } else false
    }

    override fun addAll(elements: Collection<GoPoint>): Boolean {
        val secrets = InternalGoPointSet.secrets
        val rows = secrets.rows(this)
        var modified = false
        when(elements) {
            is GoPointSet -> {
                val rows2 = secrets.rows(elements)
                for(y in 0..51) {
                    var newBits = rows2[y]
                    val oldBits = rows.getAndAccumulate(y, newBits, LongBinOp.OR)
                    newBits = newBits and oldBits.inv()
                    if (newBits != 0L) {
                        InternalGoPointSet.sizeAndHash.addAndGet(
                            this,
                            InternalGoPointSet.sizeAndHash(newBits, y)
                        )
                        modified = true
                    }
                }
            }
            is GoPointKeys<*> -> {
                elements.expungeStaleRows()
                for(y in 0..51) {
                    var newBits = elements.rowBits(y)
                    val oldBits = rows.getAndAccumulate(y, newBits, LongBinOp.OR)
                    newBits = newBits and oldBits.inv()
                    if (newBits != 0L) {
                        InternalGoPointSet.sizeAndHash.addAndGet(
                            this,
                            InternalGoPointSet.sizeAndHash(newBits, y)
                        )
                        modified = true
                    }
                }
            }
            is GoRectangle -> {
                val (x1, y1) = elements.start
                val (x2, y2) = elements.end
                val mask = (1L shl (x2 + 1)) - (1L shl x1)
                for(y in y1..y2) {
                    val oldBits = rows.accumulateAndGet(y, mask, LongBinOp.OR)
                    val newBits = mask and oldBits.inv()
                    if (newBits != 0L) {
                        InternalGoPointSet.sizeAndHash.addAndGet(
                            this,
                            InternalGoPointSet.sizeAndHash(newBits, y)
                        )
                        modified = true
                    }
                }
            }
            else -> {
                for(p in elements)
                    if (add(p)) modified = true
            }
        }
        return modified
    }

    override fun removeAll(elements: Collection<GoPoint>): Boolean {
        val secrets = InternalGoPointSet.secrets
        val rows = secrets.rows(this)
        var modified = false
        when {
            elements is GoPointSet -> {
                val rows2 = secrets.rows(elements)
                for(y in 0..51) {
                    var modBits = rows2[y]
                    val oldBits = rows.getAndAccumulate(y, modBits.inv(), LongBinOp.AND)
                    modBits = modBits and oldBits
                    if (modBits != 0L) {
                        InternalGoPointSet.sizeAndHash.addAndGet(
                            this,
                            -InternalGoPointSet.sizeAndHash(modBits, y)
                        )
                        modified = true
                    }
                }
            }
            elements is GoPointKeys<*> -> {
                elements.expungeStaleRows()
                for(y in 0..51) {
                    var modBits = elements.rowBits(y)
                    val oldBits = rows.getAndAccumulate(y, modBits.inv(), LongBinOp.AND)
                    modBits = modBits and oldBits
                    if (modBits != 0L) {
                        InternalGoPointSet.sizeAndHash.addAndGet(
                            this,
                            -InternalGoPointSet.sizeAndHash(modBits, y)
                        )
                        modified = true
                    }
                }
            }
            elements is GoPointEntries<*, *> -> {
                elements.expungeStaleRows()
                return false
            }
            elements is GoRectangle -> {
                val (x1, y1) = elements.start
                val (x2, y2) = elements.end
                val mask = (1L shl (x2 + 1)) - (1L shl x1)
                for(y in y1..y2) {
                    val oldBits = rows.accumulateAndGet(y, mask.inv(), LongBinOp.AND)
                    val modBits = mask and oldBits
                    if (modBits != 0L) {
                        InternalGoPointSet.sizeAndHash.addAndGet(
                            this,
                            -InternalGoPointSet.sizeAndHash(modBits, y)
                        )
                        modified = true
                    }
                }
            }
            size > elements.size ->
                @Suppress("USELESS_CAST")
                for(e in elements as Collection<*>)
                    if (e is GoPoint && remove(e)) modified = true
            else -> {
                val itr = iterator()
                while(itr.hasNext())
                    if (elements.contains(itr.next())) {
                        itr.remove()
                        modified = true
                    }
            }
        }
        return modified
    }

    override fun retainAll(elements: Collection<GoPoint>): Boolean {
        val secrets = InternalGoPointSet.secrets
        val rows = secrets.rows(this)
        var modified = false
        when(elements) {
            is GoPointSet -> {
                val rows2 = secrets.rows(elements)
                for(y in 0..51) {
                    var modBits = rows2[y]
                    val oldBits = rows.getAndAccumulate(y, modBits, LongBinOp.AND)
                    modBits = modBits.inv() and oldBits
                    if (modBits != 0L) {
                        InternalGoPointSet.sizeAndHash.addAndGet(
                            this,
                            -InternalGoPointSet.sizeAndHash(modBits, y)
                        )
                        modified = true
                    }
                }
            }
            is GoPointKeys<*> -> {
                elements.expungeStaleRows()
                for(y in 0..51) {
                    var modBits = elements.rowBits(y)
                    val oldBits = rows.getAndAccumulate(y, modBits, LongBinOp.AND)
                    modBits = modBits.inv() and oldBits
                    if (modBits != 0L) {
                        InternalGoPointSet.sizeAndHash.addAndGet(
                            this,
                            -InternalGoPointSet.sizeAndHash(modBits, y)
                        )
                        modified = true
                    }
                }
            }
            is GoPointEntries<*, *> -> {
                elements.expungeStaleRows()
                if (isEmpty()) return false
                clear()
                return true
            }
            is GoRectangle -> {
                val (x1, y1) = elements.start
                val (x2, y2) = elements.end
                val mask = (1L shl (x2 + 1)) - (1L shl x1)
                for(y in y1..y2) {
                    val oldBits = rows.accumulateAndGet(y, mask, LongBinOp.AND)
                    val modBits = mask.inv() and oldBits
                    if (modBits != 0L) {
                        InternalGoPointSet.sizeAndHash.addAndGet(
                            this,
                            -InternalGoPointSet.sizeAndHash(modBits, y)
                        )
                        modified = true
                    }
                }
            }
            else -> {
                val itr = iterator()
                while(itr.hasNext())
                    if (!elements.contains(itr.next())) {
                        itr.remove()
                        modified = true
                    }
            }
        }
        return modified
    }

    fun invertAll(elements: Collection<GoPoint>): Boolean {
        val secrets = InternalGoPointSet.secrets
        val rows = secrets.rows(this)
        var modified = false
        when(elements) {
            is GoPointKeys<*> -> {
                elements.expungeStaleRows()
                for(y in 0..51)
                    if (invertRow(rows, y, elements.rowBits(y))) modified = true
            }
            is GoPointEntries<*, *> -> {
                elements.expungeStaleRows()
                return false
            }
            is GoRectangle -> {
                val (x1, y1) = elements.start
                val (x2, y2) = elements.end
                val mask = (1L shl (x2 + 1)) - (1L shl x1)
                for(y in y1..y2)
                    if (invertRow(rows, y, mask)) modified = true
            }
            is GoPointSet -> {
                val rows2 = secrets.rows(elements)
                for(y in 0..51)
                    if (invertRow(rows, y, rows2[y])) modified = true
            }
            else -> {
                val rows2 = LongArray(52)
                @Suppress("USELESS_CAST")
                for(element in (elements as Collection<*>)) if (element is GoPoint) {
                    val (x, y) = element
                    val row = rows2[y]
                    val flag = 1L shl x
                    rows2[y] = if (elements is Set<*>) row or flag
                    else row xor flag
                }
                for(y in 0..51)
                    if (invertRow(rows, y, rows2[y])) modified = true
            }
        }
        return modified
    }

    private fun invertRow(rows: AtomicLongArray, y: Int, invBits: Long): Boolean {
        var modified = false
        var words = 0L
        val oldBits = rows.getAndAccumulate(y, invBits, LongBinOp.XOR)
        var modBits = invBits and oldBits.inv()
        if (modBits != 0L) {
            words += InternalGoPointSet.sizeAndHash(modBits, y)
            modified = true
        }
        modBits = invBits and oldBits
        if (modBits != 0L) {
            words -= InternalGoPointSet.sizeAndHash(modBits, y)
            modified = true
        }
        if (modified && words != 0L)
            InternalGoPointSet.sizeAndHash.addAndGet(this, words)
        return modified
    }

    override fun clear() {
        val secrets = InternalGoPointSet.secrets
        val rows = secrets.rows(this)
        var words = 0L
        for(y in 0..51)
            words -= InternalGoPointSet.sizeAndHash(rows.getAndSet(y, 0), y)
        if (words != 0L) {
            InternalGoPointSet.sizeAndHash.addAndGet(this, words)
        }
    }

    companion object {

        init {
            InternalGoPointSet.mutableSecrets = object: InternalGoPointSet.MutableSecrets {
                override fun init(rows: AtomicLongArray) = MutableGoPointSet(rows)
            }
        }

        @JvmStatic
        fun removeClashingPoints(vararg sets: MutableGoPointSet?): MutableGoPointSet? {
            if (sets.isEmpty()) return null
            val sets3: Array<MutableGoPointSet> = clashingPoints
            val all = sets3[0]
            all.clear()
            val tmp = sets3[1]
            val clashes = sets3[2]
            clashes.clear()
            val secrets = InternalGoPointSet.secrets
            val tmpRows = secrets.rows(tmp)
            for(set in sets) if (set != null) {
                val setRows = secrets.rows(set)
                for(i in 0..51)
                    tmpRows[i] = setRows[i]
                InternalGoPointSet.sizeAndHash[tmp] = InternalGoPointSet.sizeAndHash(tmpRows)
                tmp.retainAll(all)
                clashes.addAll(tmp)
                all.addAll(set)
            }
            if (clashes.size == 0) return null
            sets3[2] = MutableGoPointSet()
            for(set in sets)
                set?.removeAll(clashes)
            return clashes
        }

        private val clashingPoints by threadLocal { Array(3) { MutableGoPointSet() } }

    }

}

@Suppress("NOTHING_TO_INLINE")
inline fun removeClashingGoPoints(vararg sets: MutableGoPointSet?) = MutableGoPointSet.removeClashingPoints(*sets)