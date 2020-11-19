@file:Suppress("FunctionName", "NOTHING_TO_INLINE")
@file:JvmMultifileClass
@file:JvmName("GobanKt")

package com.computernerd1101.goban

import com.computernerd1101.goban.internal.*
import com.computernerd1101.sgf.*

inline val emptyGoPointSet get() = GoPointSet.EMPTY

inline fun GoPointSet(vararg points: Iterable<GoPoint>) = GoPointSet.readOnly(*points)

open class GoPointSet internal constructor(marker: InternalMarker): Set<GoPoint> {

    companion object {

        init {
            InternalGoPointSet.sizeAndHash = atomicLongUpdater("sizeAndHash")
            val rowUpdaters = InternalGoPointSet.rowUpdaters
            for(i in 0..51)
                rowUpdaters[i] = atomicLongUpdater("row$i")
        }

        @JvmField
        val EMPTY = GoPointSet(InternalMarker)

        @JvmStatic
        fun readOnly(vararg points: Iterable<GoPoint>): GoPointSet {
            val set = GoPointSet(InternalMarker)
            InternalGoPointSet.init(set, points)
            return if (set.sizeAndHash == 0L) EMPTY
            else set
        }

    }

    init {
        marker.ignore()
        @Suppress("SpellCheckingInspection")
        val klass = javaClass
        if (klass != GoPointSet::class.java && klass != MutableGoPointSet::class.java)
            throw IllegalAccessError(
                "${klass.name} does not have permission to inherit from com.computernerd1101.goban.GoPointSet"
            )
    }

    @Volatile private var sizeAndHash: Long = 0L
    @Volatile private var row0: Long = 0L
    @Volatile private var row1: Long = 0L
    @Volatile private var row2: Long = 0L
    @Volatile private var row3: Long = 0L
    @Volatile private var row4: Long = 0L
    @Volatile private var row5: Long = 0L
    @Volatile private var row6: Long = 0L
    @Volatile private var row7: Long = 0L
    @Volatile private var row8: Long = 0L
    @Volatile private var row9: Long = 0L
    @Volatile private var row10: Long = 0L
    @Volatile private var row11: Long = 0L
    @Volatile private var row12: Long = 0L
    @Volatile private var row13: Long = 0L
    @Volatile private var row14: Long = 0L
    @Volatile private var row15: Long = 0L
    @Volatile private var row16: Long = 0L
    @Volatile private var row17: Long = 0L
    @Volatile private var row18: Long = 0L
    @Volatile private var row19: Long = 0L
    @Volatile private var row20: Long = 0L
    @Volatile private var row21: Long = 0L
    @Volatile private var row22: Long = 0L
    @Volatile private var row23: Long = 0L
    @Volatile private var row24: Long = 0L
    @Volatile private var row25: Long = 0L
    @Volatile private var row26: Long = 0L
    @Volatile private var row27: Long = 0L
    @Volatile private var row28: Long = 0L
    @Volatile private var row29: Long = 0L
    @Volatile private var row30: Long = 0L
    @Volatile private var row31: Long = 0L
    @Volatile private var row32: Long = 0L
    @Volatile private var row33: Long = 0L
    @Volatile private var row34: Long = 0L
    @Volatile private var row35: Long = 0L
    @Volatile private var row36: Long = 0L
    @Volatile private var row37: Long = 0L
    @Volatile private var row38: Long = 0L
    @Volatile private var row39: Long = 0L
    @Volatile private var row40: Long = 0L
    @Volatile private var row41: Long = 0L
    @Volatile private var row42: Long = 0L
    @Volatile private var row43: Long = 0L
    @Volatile private var row44: Long = 0L
    @Volatile private var row45: Long = 0L
    @Volatile private var row46: Long = 0L
    @Volatile private var row47: Long = 0L
    @Volatile private var row48: Long = 0L
    @Volatile private var row49: Long = 0L
    @Volatile private var row50: Long = 0L
    @Volatile private var row51: Long = 0L

    override val size: Int
        get() = sizeAndHash.toInt()

    override fun isEmpty() = size == 0

    override fun iterator(): Iterator<GoPoint> = GoPointItr(this)

    private object Compressed: ThreadLocal<LongArray>() {
        override fun initialValue() = LongArray(53)
    }

    private var compressed: Array<GoRectangle>? = null

    private fun compressed(): Array<GoRectangle> {
        var compressed = if (this is MutableGoPointSet) null else this.compressed
        if (compressed == null) {
            val rows: LongArray = Compressed.get()
            for (i in 0..51)
                rows[i] = InternalGoPointSet.rowUpdaters[i][this]
            var bit = 1L shl 51
            var yMin = 0
            var xMax = 0
            var yMax = 51
            while (rows[yMax] == 0L)
                if (--yMax < 0) {
                    compressed = emptyArray()
                    if (this !is MutableGoPointSet)
                        this.compressed = compressed
                    return compressed
                }
            while (rows[yMin] == 0L) yMin++
            for (y in yMin..yMax) {
                val row = rows[y]
                if (row != 0L) {
                    val b1 = row and -row
                    if (bit > b1)
                        bit = b1
                    val x2 = highestBitLength(row)
                    if (xMax < x2) xMax = x2
                }
            }
            val list = mutableListOf<GoRectangle>()
            for(x in trailingZerosPow2(bit)..xMax) {
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
                    list.add(GoRectangle(x, y, x2, y2))
                    while (y2 >= y) {
                        rows[y2] = rows[y2] and rowMask.inv()
                        y2--
                    }
                }
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
        return if (this is MutableGoPointSet || compressed.isEmpty()) compressed
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
                if (prop != null) prop.list.add(value)
                else prop = SGFProperty(value)
            }
        } else for(point in this) {
            val value = SGFValue(point.toSGFBytes())
            if (prop != null) prop.list.add(value)
            else prop = SGFProperty(value)
        }
        return prop
    }

    fun copy(): MutableGoPointSet {
        val copy = MutableGoPointSet()
        val set: GoPointSet = copy
        set.row0 = row0
        set.row1 = row1
        set.row2 = row2
        set.row3 = row3
        set.row4 = row4
        set.row5 = row5
        set.row6 = row6
        set.row7 = row7
        set.row8 = row8
        set.row9 = row9
        set.row10 = row10
        set.row11 = row11
        set.row12 = row12
        set.row13 = row13
        set.row14 = row14
        set.row15 = row15
        set.row16 = row16
        set.row17 = row17
        set.row18 = row18
        set.row19 = row19
        set.row20 = row20
        set.row21 = row21
        set.row22 = row22
        set.row23 = row23
        set.row24 = row24
        set.row25 = row25
        set.row26 = row26
        set.row27 = row27
        set.row28 = row28
        set.row29 = row29
        set.row30 = row30
        set.row31 = row31
        set.row32 = row32
        set.row33 = row33
        set.row34 = row34
        set.row35 = row35
        set.row36 = row36
        set.row37 = row37
        set.row38 = row38
        set.row39 = row39
        set.row40 = row40
        set.row41 = row41
        set.row42 = row42
        set.row43 = row43
        set.row44 = row44
        set.row45 = row45
        set.row46 = row46
        set.row47 = row47
        set.row48 = row48
        set.row49 = row49
        set.row50 = row50
        set.row51 = row51
        set.sizeAndHash = InternalGoPointSet.sizeAndHash(copy)
        return copy
    }

    open fun readOnly() = this

    override fun contains(element: GoPoint): Boolean {
        return InternalGoPointSet.rowUpdaters[element.y][this] and (1L shl element.x) != 0L
    }

    override fun containsAll(elements: Collection<GoPoint>): Boolean {
        when(elements) {
            is GoPointSet -> for(updater in InternalGoPointSet.rowUpdaters)
                if (updater[this].inv() and updater[elements] != 0L) return false
            is GoRectangle -> {
                val bits = 1L.shl(elements.end.x + 1) - 1L.shl(elements.start.x)
                for (y in elements.start.y..elements.end.y) {
                    if (InternalGoPointSet.rowUpdaters[y][this].inv() and bits != 0L) return false
                }
            }
            is GoPointKeys<*> -> {
                elements.expungeStaleRows()
                for(y in 0..51)
                    if (InternalGoPointSet.rowUpdaters[y][this].inv() and elements.rowBits(y) != 0L) return false
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
                if (sizeAndHash != other.sizeAndHash) return false
                for(updater in InternalGoPointSet.rowUpdaters)
                    if (updater[this] != updater[other]) return false
                true
            }
            is GoPointKeys<*> -> {
                // automatically calls other.expungeStaleRows()
                if (size != other.size) return false
                for(i in 0..51) {
                    if (InternalGoPointSet.rowUpdaters[i][this] != other.rowBits(i)) return false
                }
                true
            }
            is GoRectangle -> {
                val bits: Long = 1L.shl(other.end.x + 1) - 1L.shl(other.start.x)
                for(y in 0..51) {
                    if (InternalGoPointSet.rowUpdaters[y][this] != if (y in other.start.y..other.end.y) bits else 0)
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
            val compressed = compressed()
            s = if (compressed.size == 1) compressed[0].toString()
            else {
                val buffer = StringBuilder()
                compressed.joinTo(buffer, ", ", "[", "]") {
                    val start = it.start.toString()
                    val size = it.size
                    if (size == 1) start
                    else {
                        buffer.append(start).append(if (size == 2) ", " else ", ..., ")
                        it.end.toString()
                    }
                }.toString()
            }
            if (this !is MutableGoPointSet)
                string = s
        }
        return s
    }

}

class MutableGoPointSet(): GoPointSet(InternalMarker), MutableSet<GoPoint> {

//    internal constructor(rows: AtomicLongArray, marker: InternalMarker):
//            super(rows, InternalGoPointSet.sizeAndHash(rows), marker)

    @Suppress("unused")
    constructor(vararg points: Iterable<GoPoint>) {
        InternalGoPointSet.init(this, points)
    }

    override fun readOnly(): GoPointSet {
        if (isEmpty()) return emptyGoPointSet
        val copy = GoPointSet()
        for(updater in InternalGoPointSet.rowUpdaters)
            updater[copy] = updater[this]
        val sizeAndHash = InternalGoPointSet.sizeAndHash(copy)
        if (sizeAndHash == 0L) return EMPTY
        InternalGoPointSet.sizeAndHash[copy] = InternalGoPointSet.sizeAndHash(copy)
        return copy
    }

    override fun iterator(): MutableIterator<GoPoint> = object :
        GoPointItr(this), MutableIterator<GoPoint> {
        override fun remove() {
            val (x, y) = lastReturned ?: throw IllegalStateException()
            val mask = (1L shl x).inv()
            val row = InternalGoPointSet.rowUpdaters[y].getAndAccumulate(set, mask, LongBinOp.AND)
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
        val oldBits = InternalGoPointSet.rowUpdaters[y].getAndAccumulate(this, bit, LongBinOp.OR)
        return if (oldBits and bit == 0L) {
            InternalGoPointSet.sizeAndHash.addAndGet(this,
                (x + y*52L).shl(32) + 1L)
            true
        } else false
    }

    override fun remove(element: GoPoint): Boolean {
        val (x, y) = element
        val bit = 1L shl x
        val oldBits = InternalGoPointSet.rowUpdaters[y].getAndAccumulate(this, bit.inv(), LongBinOp.AND)
        return if (oldBits and bit != 0L) {
            InternalGoPointSet.sizeAndHash.addAndGet(this,
                -((x + y*52L) shl 32) - 1L)
            true
        } else false
    }

    override fun addAll(elements: Collection<GoPoint>): Boolean {
        var modified = false
        when(elements) {
            is GoPointSet -> {
                for(y in 0..51) {
                    val updater = InternalGoPointSet.rowUpdaters[y]
                    var newBits = updater[elements]
                    val oldBits = updater.getAndAccumulate(this, newBits, LongBinOp.OR)
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
                    val oldBits = InternalGoPointSet.rowUpdaters[y].getAndAccumulate(this, newBits, LongBinOp.OR)
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
                    val oldBits = InternalGoPointSet.rowUpdaters[y].accumulateAndGet(this, mask, LongBinOp.OR)
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
        var modified = false
        when {
            elements is GoPointSet -> {
                for(y in 0..51) {
                    val updater = InternalGoPointSet.rowUpdaters[y]
                    var modBits = updater[elements]
                    val oldBits = updater.getAndAccumulate(this, modBits.inv(), LongBinOp.AND)
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
                    val oldBits = InternalGoPointSet.rowUpdaters[y].getAndAccumulate(
                        this, modBits.inv(), LongBinOp.AND)
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
                    val oldBits = InternalGoPointSet.rowUpdaters[y].accumulateAndGet(
                        this, mask.inv(), LongBinOp.AND)
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
        var modified = false
        when(elements) {
            is GoPointSet -> {
                for(y in 0..51) {
                    val updater = InternalGoPointSet.rowUpdaters[y]
                    var modBits = updater[elements]
                    val oldBits = updater.getAndAccumulate(this, modBits, LongBinOp.AND)
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
                    val oldBits = InternalGoPointSet.rowUpdaters[y].getAndAccumulate(
                        this, modBits, LongBinOp.AND)
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
                    val oldBits = InternalGoPointSet.rowUpdaters[y].accumulateAndGet(
                        this, mask, LongBinOp.AND)
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
        var modified = false
        when(elements) {
            is GoPointKeys<*> -> {
                elements.expungeStaleRows()
                for(y in 0..51)
                    if (invertRow(y, elements.rowBits(y))) modified = true
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
                    if (invertRow(y, mask)) modified = true
            }
            is GoPointSet -> {
                for (y in 0..51)
                    if (invertRow(y, InternalGoPointSet.rowUpdaters[y][elements])) modified = true
            }
            else -> {
                val rows2 = LongArray(52)
                @Suppress("USELESS_CAST")
                for(element in (elements as Collection<*>)) if (element is GoPoint) {
                    val (x, y) = element
                    rows2[y] = rows2[y] xor (1L shl x)
                }
                for(y in 0..51)
                    if (invertRow(y, rows2[y])) modified = true
            }
        }
        return modified
    }

    private fun invertRow(y: Int, invBits: Long): Boolean {
        var modified = false
        var words = 0L
        val oldBits = InternalGoPointSet.rowUpdaters[y].getAndAccumulate(this, invBits, LongBinOp.XOR)
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
        var words = 0L
        for(y in 0..51)
            words -= InternalGoPointSet.sizeAndHash(
                InternalGoPointSet.rowUpdaters[y].getAndSet(this, 0L), y)
        if (words != 0L)
            InternalGoPointSet.sizeAndHash.addAndGet(this, words)
    }

    private object ClashingPoints: ThreadLocal<Array<MutableGoPointSet>>() {

        override fun initialValue() = Array(3) { MutableGoPointSet() }

    }

    companion object {

        @JvmStatic
        fun removeClashingPoints(vararg sets: MutableGoPointSet?): MutableGoPointSet? {
            if (sets.isEmpty()) return null
            val sets3: Array<MutableGoPointSet> = ClashingPoints.get()
            val all = sets3[0]
            all.clear()
            val tmp = sets3[1]
            val clashes = sets3[2]
            clashes.clear()
            for(set in sets) if (set != null) {
                for(updater in InternalGoPointSet.rowUpdaters)
                    updater[tmp] = updater[set]
                InternalGoPointSet.sizeAndHash[tmp] = InternalGoPointSet.sizeAndHash(tmp)
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

    }

}

@Suppress("NOTHING_TO_INLINE")
inline fun removeClashingGoPoints(vararg sets: MutableGoPointSet?) = MutableGoPointSet.removeClashingPoints(*sets)