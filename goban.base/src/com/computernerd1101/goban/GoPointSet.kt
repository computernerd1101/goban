@file:JvmMultifileClass
@file:JvmName("GobanKt")

package com.computernerd1101.goban

import com.computernerd1101.goban.internal.*
import com.computernerd1101.sgf.*

fun GoPointSet(vararg points: Iterable<GoPoint>) = GoPointSet.readOnly(*points)

open class GoPointSet internal constructor(intern: InternalGoPointSet): Set<GoPoint> {

    companion object {

        init {
            InternalGoPointSet.sizeAndHash = atomicLongUpdater("sizeAndHash")
            val rowUpdaters = InternalGoPointSet.rowUpdaters
            val buffer = CharArray(5)
            buffer[0] = 'r'
            buffer[1] = 'o'
            buffer[2] = 'w'
            for(i in 0..9) {
                buffer[3] = '0' + i
                rowUpdaters[i] = atomicLongUpdater(String(buffer, 0, 4))
            }
            for(i in 10..51) {
                buffer[3] = '0' + i / 10
                buffer[4] = '0' + i % 10
                rowUpdaters[i] = atomicLongUpdater(String(buffer))
            }
        }

        @JvmField
        val EMPTY = GoPointSet(InternalGoPointSet)

        @JvmStatic
        fun readOnly(vararg points: Iterable<GoPoint>): GoPointSet {
            val set = GoPointSet(InternalGoPointSet)
            InternalGoPointSet.init(set, points)
            return if (set.sizeAndHash == 0L) EMPTY
            else set
        }

    }

    init {
        intern.checkType(javaClass)
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
                    value.parts.add(second.toSGFBytes())
                if (prop != null) prop.values.add(value)
                else prop = SGFProperty(value)
            }
        } else for(point in this) {
            val value = SGFValue(point.toSGFBytes())
            if (prop != null) prop.values.add(value)
            else prop = SGFProperty(value)
        }
        return prop
    }

    fun copy(): MutableGoPointSet {
        val copy = MutableGoPointSet()
        copyInto(copy, InternalGoPointSet)
        return copy
    }

    internal fun copyInto(dst: GoPointSet, intern: InternalGoPointSet) {
        dst.row0 = row0
        dst.row1 = row1
        dst.row2 = row2
        dst.row3 = row3
        dst.row4 = row4
        dst.row5 = row5
        dst.row6 = row6
        dst.row7 = row7
        dst.row8 = row8
        dst.row9 = row9
        dst.row10 = row10
        dst.row11 = row11
        dst.row12 = row12
        dst.row13 = row13
        dst.row14 = row14
        dst.row15 = row15
        dst.row16 = row16
        dst.row17 = row17
        dst.row18 = row18
        dst.row19 = row19
        dst.row20 = row20
        dst.row21 = row21
        dst.row22 = row22
        dst.row23 = row23
        dst.row24 = row24
        dst.row25 = row25
        dst.row26 = row26
        dst.row27 = row27
        dst.row28 = row28
        dst.row29 = row29
        dst.row30 = row30
        dst.row31 = row31
        dst.row32 = row32
        dst.row33 = row33
        dst.row34 = row34
        dst.row35 = row35
        dst.row36 = row36
        dst.row37 = row37
        dst.row38 = row38
        dst.row39 = row39
        dst.row40 = row40
        dst.row41 = row41
        dst.row42 = row42
        dst.row43 = row43
        dst.row44 = row44
        dst.row45 = row45
        dst.row46 = row46
        dst.row47 = row47
        dst.row48 = row48
        dst.row49 = row49
        dst.row50 = row50
        dst.row51 = row51
        dst.sizeAndHash = intern.sizeAndHash(dst)
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
                val bits = InternalGoRectangle.rowBits(elements)
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
                val bits: Long = InternalGoRectangle.rowBits(other)
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

class MutableGoPointSet: GoPointSet, MutableSet<GoPoint> {

//    internal constructor(rows: AtomicLongArray, marker: InternalMarker):
//            super(rows, InternalGoPointSet.sizeAndHash(rows), marker)

    constructor(): super(InternalGoPointSet)

    @Suppress("unused")
    constructor(vararg points: Iterable<GoPoint>): super(InternalGoPointSet) {
        InternalGoPointSet.init(this, points)
    }

    override fun readOnly(): GoPointSet {
        if (isEmpty()) return EMPTY
        val copy = GoPointSet(InternalGoPointSet)
        copyInto(copy, InternalGoPointSet)
        return if (copy.isEmpty()) EMPTY else copy
    }

    override fun iterator(): MutableIterator<GoPoint> = object :
        GoPointItr(this), MutableIterator<GoPoint> {

        private var lastReturned: GoPoint? = null

        override fun next(): GoPoint = super.next().also { lastReturned = it }

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

    fun copyFrom(elements: Collection<GoPoint>) {
        if (elements is GoPointSet) {
            elements.copyInto(this, InternalGoPointSet)
        } else {
            clear()
            addAll(elements)
        }
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
                val mask = InternalGoRectangle.rowBits(elements)
                for(y in elements.start.y..elements.end.y) {
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
                val mask = InternalGoRectangle.rowBits(elements)
                for(y in elements.start.y..elements.end.y) {
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
                val mask = InternalGoRectangle.rowBits(elements)
                for(y in elements.start.y..elements.end.y) {
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

    private object ThreadLocalRows: ThreadLocal<LongArray>() {

        override fun initialValue() = LongArray(52)

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
                val mask = InternalGoRectangle.rowBits(elements)
                for(y in elements.start.y..elements.end.y)
                    if (invertRow(y, mask)) modified = true
            }
            is GoPointSet -> {
                for (y in 0..51)
                    if (invertRow(y, InternalGoPointSet.rowUpdaters[y][elements])) modified = true
            }
            else -> {
                val rows: LongArray = ThreadLocalRows.get()
                @Suppress("USELESS_CAST")
                for((x, y) in elements)
                    rows[y] = rows[y] xor (1L shl x)
                for(y in 0..51)
                    if (invertRow(y, rows[y])) modified = true
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

fun removeClashingGoPoints(vararg sets: MutableGoPointSet?) = MutableGoPointSet.removeClashingPoints(*sets)