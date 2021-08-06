package com.computernerd1101.goban.internal

import com.computernerd1101.goban.*
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater

internal object InternalGoPointMap {

    /** Updates [GoPointMap._size] */
    lateinit var size: AtomicIntegerFieldUpdater<GoPointMap<*>>
    /** Updates [WeakGoPointMap.Row.size] */
    val rowSize = atomicIntUpdater<WeakGoPointMap.Row<*>>("size")

    fun fastEquals(m1: GoPointMap<*>, m2: GoPointMap<*>): Boolean {
        if (size[m1] != size[m2])
            return false
        val rows1 = m1.secrets.rows
        val rows2 = m2.secrets.rows
        val weak1: Array<out WeakGoPointMap.Row<*>?>? = (m1 as? MutableGoPointMap<*>)?.weak?.rows
        val weak2: Array<out WeakGoPointMap.Row<*>?>? = (m2 as? MutableGoPointMap<*>)?.weak?.rows
        for(y in 0..51) {
            val row1 = rows1[y]
            val row2 = rows2[y]
            if (row1 === row2) continue
            if (row1 == null || row2 == null) return false
            if (weak1 != null && weak2 != null) {
                val a = weak1[y]
                val b = weak2[y]
                if (a != null && b != null && a.size != b.size) return false
            }
            for(x in 0..51)
                if (row1[x]?.value != row2[x]?.value) return false
        }
        return true
    }

    fun hashCode(c: Collection<*>): Int {
        var hashCode = 0
        for(element in c)
            hashCode += element.hashCode()
        return hashCode
    }

}

internal fun GoPointMap<*>.valueToString(e: Map.Entry<*, *>): String {
    val value = e.value
    return when {
        value === this -> "(this Map)"
        value === e -> "(this Map.Entry)"
        value === entries -> "(this Map.entrySet)"
        value === keys -> "(this Map.keySet)"
        value === values -> "(this Map.values)"
        else -> value.toString()
    }
}

internal class GoPointMapSecrets<out V>(
    map: GoPointMap<V>
) {

    @JvmField val rows: Array<Array<Map.Entry<GoPoint, @UnsafeVariance V>?>?> = arrayOfNulls(52)
    @JvmField val entries: GoPointEntries<V, Map.Entry<GoPoint, V>> = GoPointEntries(map)
    @JvmField val keys = GoPointKeys(map)
    @JvmField val values = GoPointValues(map, entries)

}

internal class WeakGoPointMap<V> {

    internal class Row<V>(map: MutableGoPointMap<V>, val y: Int):
        WeakReference<Array<Map.Entry<GoPoint, V>?>?>(
            map.secrets.rows[y],
            map.weak.queue
        ) {

        @Volatile
        @JvmField
        var size = 0

    }

    @JvmField val rows = arrayOfNulls<Row<V>>(52)
    @JvmField val queue = ReferenceQueue<Array<Map.Entry<GoPoint, V>?>?>()

    fun expungeStaleRows() {
        try {
            while(true) {
                val ref = queue.poll() ?: break
                if (ref is Row<*>) {
                    val y = ref.y
                    if (rows[y] === ref) rows[y] = null
                }
            }
        } catch (ignored: Exception) { }
    }

}

internal abstract class GoPointMapCollection<out V, out E>(open val map: GoPointMap<V>):
    AbstractCollection<E>() {

    abstract fun elementFrom(entry: Map.Entry<GoPoint, @UnsafeVariance V>): E

    abstract val immutable: GoPointMapCollection<V, *>

    open fun expungeStaleRows() = Unit

    override val size: Int
        get() = InternalGoPointMap.size[map]

    override fun iterator(): Iterator<E> {
        return GoPointMapItr(this)
    }

    public override fun toArray() = super.toArray()

    public override fun <T> toArray(array: Array<T>) = super.toArray(array)

    fun <T> toImmutable(c: Collection<T>): Collection<T> {
        return if (c is GoPointMapCollection<*, *>) {
            if (c.map !== map) c.expungeStaleRows()
            @Suppress("UNCHECKED_CAST")
            c.immutable as Collection<T>
        } else c
    }

}

internal open class GoPointMapItr<out V, out E>(val collection: GoPointMapCollection<V, E>): Iterator<E> {

    protected var nextX = 0
    protected var nextY = 0

    protected var lastReturnedX = 0
    protected var lastReturnedY = -1

    private fun nextEntry(): Map.Entry<GoPoint, V>? {
        expungeStaleRows()
        var x: Int
        var y = nextY
        if (y >= 52) return null
        x = nextX
        val rows = collection.map.secrets.rows
        while (y < 52) {
            rows[y]?.let { row: Array<Map.Entry<GoPoint, V>?> ->
                while (x < 52) {
                    val e = row[x]
                    if (e != null) {
                        nextX = x
                        nextY = y
                        return e
                    }
                    x++
                }
            }
            x = 0
            y++
        }
        nextX = 0
        nextY = 52
        return null
    }

    protected open fun expungeStaleRows() = Unit

    override fun hasNext(): Boolean {
        return nextEntry() != null
    }

    override fun next(): E {
        val entry = nextEntry() ?: throw NoSuchElementException()
        val x = nextX
        val y = nextY
        lastReturnedX = x
        lastReturnedY = y
        val next = x + 1
        if (next < 52) {
            nextX = next
        } else {
            nextX = 0
            nextY = y + 1
        }
        return collection.elementFrom(entry)
    }

}

internal open class MutableGoPointMapItr<V, E>(collection: GoPointMapCollection<V, E>):
    GoPointMapItr<V, E>(collection), MutableIterator<E> {

    override fun remove() {
        val y = lastReturnedY
        if (y < 0) throw IllegalStateException()
        val map = collection.map as MutableGoPointMap<V>
        val rows = map.secrets.rows
        val row = rows[y] ?: throw IllegalStateException()
        val x = lastReturnedX
        val entry = row[x] as MutableGoPointEntry<V>? ?: throw IllegalStateException()
        entry.map = null
        row[x] = null
        InternalGoPointMap.size.getAndDecrement(map)
        map.weak.rows[y]?.let { weak ->
            if (InternalGoPointMap.rowSize.getAndDecrement(weak) <= 0) rows[y] = null
        }
        lastReturnedX = 0
        lastReturnedY = -1
    }

    fun removeAll(c: Collection<E>, retain: Boolean = false): Boolean {
        var modified = false
        while(hasNext()) {
            if (c.contains(next()) xor retain) {
                remove()
                modified = true
            }
        }
        return modified
    }

}

internal class ExpungeGoPointMapItr<V, E>(collection: GoPointMapCollection<V, E>):
    MutableGoPointMapItr<V, E>(collection) {

    override fun expungeStaleRows() {
        (collection.map as MutableGoPointMap<V>).weak.expungeStaleRows()
    }

    override fun remove() {
        try {
            super.remove()
        } finally {
            expungeStaleRows()
        }
    }

}

internal open class GoPointEntries<out V, out E>(
    map: GoPointMap<V>
): GoPointMapCollection<V, E>(map), Set<E>
        where E: Map.Entry<GoPoint, V> {

    override val immutable: GoPointEntries<V, Map.Entry<GoPoint, V>>
        get() = this

    @Suppress("UNCHECKED_CAST")
    override fun elementFrom(entry: Map.Entry<GoPoint, @UnsafeVariance V>): E {
        return (if (entry !is ImmutableEntry<*, *>)
            ImmutableEntry(entry.key, entry.value)
        else entry) as E
    }

    override fun contains(element: @UnsafeVariance E): Boolean {
        val map = this.map
        val (x, y) = element.key
        val foundValue: Boolean
        val value: Any?
        if (map is MutableGoPointMap<*>) {
            foundValue = true
            value = element.value
            if (!map.isValidValue(value, InternalMarker)) return false
        } else {
            foundValue = false
            value = null
        }
        val entry = map.secrets.rows[y]?.get(x) ?: return false
        return entry.value == if (foundValue) value else element.value
    }

    override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean {
        if (this === elements)
            return true
        @Suppress("USELESS_CAST")
        if ((elements as Collection<*>) is GoRectangle) return false
        if (elements is GoPointKeys<*>)
            return elements.isEmpty()
        if (elements is GoPointValues<E>) {
            elements.expungeStaleRows()
            return super.containsAll(elements.immutable)
        }
        if (elements !is GoPointEntries<*, *>)
            return super.containsAll(elements)
        elements.expungeStaleRows()
        val rows1 = map.secrets.rows
        val rows2 = elements.map.secrets.rows
        for(y in 0..51) {
            val row2 = rows2[y] ?: continue
            val row1 = rows1[y] ?: return false
            for(x in 0..51)
                row2[x]?.let { entry2 ->
                    val entry1 = row1[x]
                    if (entry1 == null || entry1.value != entry2.value)
                        return false
                }
        }
        return true
    }

    override fun equals(other: Any?): Boolean {
        expungeStaleRows()
        if (this === other)
            return true
        if (other is GoPointSet) return InternalGoPointMap.size[map] == 0 && other.isEmpty()
        if (other is GoRectangle || other !is Set<*>) return false
        var set: Collection<*> = other
        if (other is GoPointMapCollection<*, *>) {
            if (other is GoPointEntries<*, *>) {
                if (map === other.map)
                    return true
                other.expungeStaleRows()
                return InternalGoPointMap.fastEquals(map, other.map)
            }
            if (map !== other.map)
                other.expungeStaleRows()
            if (other is GoPointKeys<*>)
                return InternalGoPointMap.size[map] == 0 && InternalGoPointMap.size[other.map] == 0
            set = other.immutable
        }
        return InternalGoPointMap.size[map] == other.size && immutable.containsAll(set)
    }

    override fun hashCode(): Int {
        expungeStaleRows()
        return InternalGoPointMap.hashCode(immutable)
    }

    override fun toString(): String {
        expungeStaleRows()
        return immutable.toString("[", "]")
    }

    fun toString(prefix: String, postfix: String): String {
        return joinToString(prefix=prefix, postfix=postfix) { entry: Map.Entry<GoPoint, V> ->
            "${entry.key}=${map.valueToString(entry)}"
        }
    }

}

internal class MutableGoPointEntry<V>(
    map: MutableGoPointMap<V>,
    override val key: GoPoint,
    value: V
): MutableMap.MutableEntry<GoPoint, V> {

    override var value: V = value; private set

    var map: MutableGoPointMap<V>? = map

    override fun setValue(newValue: V): V {
        map?.filterValue(newValue, InternalMarker)
        val oldValue = value
        value = newValue
        return oldValue
    }

    override fun equals(other: Any?): Boolean {
        return this === other || (other is Map.Entry<*, *> &&
                key == other.key && value == other.value)
    }

    override fun hashCode(): Int {
        return key.hashCode() xor value.hashCode()
    }

    override fun toString(): String {
        val value = this.value
        return "$key=${if (value === this) "(this Map.Entry)" else value.toString()}"
    }

}

internal class MutableGoPointEntries<V>(
    map: MutableGoPointMap<V>,
    override val immutable: GoPointEntries<V, Map.Entry<GoPoint, V>>
): GoPointEntries<V, MutableMap.MutableEntry<GoPoint, V>>(map),
    MutableSet<MutableMap.MutableEntry<GoPoint, V>> {

    override val map: MutableGoPointMap<V>
        get() = super.map as MutableGoPointMap<V>

    override fun elementFrom(entry: Map.Entry<GoPoint, V>): MutableMap.MutableEntry<GoPoint, V> {
        return when(entry) {
            is MutableGoPointEntry<V> -> entry
            else -> MutableGoPointEntry(map, entry.key, entry.value)
        }
    }

    override fun expungeStaleRows() {
        map.weak.expungeStaleRows()
    }

    override val size: Int get() = map.size

    override fun iterator(): MutableIterator<MutableMap.MutableEntry<GoPoint, V>> {
        return ExpungeGoPointMapItr(this)
    }

    override fun contains(element: MutableMap.MutableEntry<GoPoint, V>): Boolean {
        expungeStaleRows()
        return immutable.contains(element)
    }

    override fun add(element: MutableMap.MutableEntry<GoPoint, V>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun remove(element: MutableMap.MutableEntry<GoPoint, V>) = try {
        fastRemove(element)
    } finally {
        expungeStaleRows()
    }

    private fun fastRemove(element: Map.Entry<GoPoint, V>): Boolean {
        val (x, y) = element.key
        val value = element.value
        val map = this.map
        if (!map.isValidValue(value, InternalMarker)) return false
        val rows = map.secrets.rows
        rows[y]?.let { row ->
            (row[x] as MutableGoPointEntry<V>?)?.let { entry ->
                if (element.value == entry.value) {
                    entry.map = null
                    row[x] = null
                    InternalGoPointMap.size.decrementAndGet(map)
                    map.weak.rows[y]?.let { weak ->
                        if (InternalGoPointMap.rowSize.decrementAndGet(weak) <= 0)
                            rows[y] = null
                    }
                    return true
                }
            }
        }
        return false
    }

    override fun containsAll(elements: Collection<MutableMap.MutableEntry<GoPoint, V>>): Boolean {
        expungeStaleRows()
        return immutable.containsAll(elements)
    }

    override fun addAll(elements: Collection<MutableMap.MutableEntry<GoPoint, V>>): Boolean {
        expungeStaleRows()
        throw UnsupportedOperationException()
    }

    override fun removeAll(elements: Collection<MutableMap.MutableEntry<GoPoint, V>>): Boolean {
        if (this === elements) {
            if (InternalGoPointMap.size[map] == 0) {
                expungeStaleRows()
                return false
            }
            clear()
            return true
        }
        if (elements is GoPointKeys<*>) {
            if (map !== elements.map) elements.expungeStaleRows()
            expungeStaleRows()
            return false
        }
        var modified = false
        try {
            if (elements is GoPointEntries<*, *>) {
                if (map !== elements.map) elements.expungeStaleRows()
                val rows1 = map.secrets.rows
                val rows2 = elements.map.secrets.rows
                val weakRows = map.weak.rows
                for (y in 0..51) {
                    val row1 = rows1[y]
                    val row2 = rows2[y]
                    if (row1 == null || row2 == null) continue
                    for (x in 0..51) {
                        val e1 = row1[x] as MutableGoPointEntry<V>?
                        val e2 = row2[x]
                        if (e1 != null && e2 != null && e1.value == e2.value) {
                            modified = true
                            e1.map = null
                            row1[x] = null
                            InternalGoPointMap.size.decrementAndGet(map)
                            if (InternalGoPointMap.rowSize.decrementAndGet(weakRows[y]) <= 0) {
                                rows1[y] = null
                                break
                            }
                        }
                    }
                }
            } else {
                val c = toImmutable(elements)
                if (InternalGoPointMap.size[map] > c.size) {
                    for(e in c)
                        if (fastRemove(e)) modified = true
                } else modified = MutableGoPointMapItr(this).removeAll(c)
            }
        } finally {
            expungeStaleRows()
        }
        return modified
    }

    override fun retainAll(elements: Collection<MutableMap.MutableEntry<GoPoint, V>>): Boolean {
        if (this === elements) {
            expungeStaleRows()
            return false
        }
        if (elements is GoPointKeys<*>) {
            if (map !== elements.map) elements.expungeStaleRows()
            if (InternalGoPointMap.size[map] == 0) {
                expungeStaleRows()
                return false
            }
            clear()
            return true
        }
        var modified = false
        try {
            if (elements is GoPointEntries<*, *>) {
                if (map !== elements.map) elements.expungeStaleRows()
                val rows1 = map.secrets.rows
                val rows2 = elements.map.secrets.rows
                val weakRows = map.weak.rows
                for (y in 0..51) {
                    val row1 = rows1[y]
                    val weak = weakRows[y]
                    if (row1 == null || weak == null) continue
                    val row2 = rows2[y]
                    if (row2 == null) {
                        InternalGoPointMap.size.getAndAdd(map, -weak.size)
                        weak.size = 0
                        rows1[y] = null
                        modified = true
                    } else {
                        for (x in 0..51) {
                            val e1 = row1[x] as MutableGoPointEntry<V>?
                            if (e1 != null) {
                                val e2 = row2[x]
                                if (e2 == null || e1.value != e2.value) {
                                    modified = true
                                    e1.map = null
                                    row1[x] = null
                                    InternalGoPointMap.size.decrementAndGet(map)
                                    if (InternalGoPointMap.rowSize.decrementAndGet(weak) <= 0) {
                                        rows1[y] = null
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                modified = MutableGoPointMapItr(this).removeAll(toImmutable(elements), retain=true)
            }
        } finally {
            expungeStaleRows()
        }
        return modified
    }

    override fun clear() {
        map.clear()
    }

    override fun toArray(): Array<Any?> {
        expungeStaleRows()
        return immutable.toArray()
    }

    override fun <T> toArray(array: Array<T>): Array<T> {
        expungeStaleRows()
        return immutable.toArray(array)
    }

}

internal open class GoPointKeys<out V>(map: GoPointMap<V>):
    GoPointMapCollection<V, GoPoint>(map), Set<GoPoint> {

    override val immutable: GoPointKeys<V>
        get() = this

    override fun elementFrom(entry: Map.Entry<GoPoint, @UnsafeVariance V>) = entry.key

    override fun contains(element: GoPoint): Boolean {
        return map.secrets.rows[element.y]?.get(element.x) != null
    }

    override fun containsAll(elements: Collection<GoPoint>): Boolean {
        if (this === elements)
            return true
        val rows1 = map.secrets.rows
        return when(elements) {
            is GoRectangle -> {
                for(y in elements.start.y..elements.end.y) {
                    val row = rows1[y] ?: return false
                    for(x in elements.start.x..elements.end.x)
                        if (row[x] == null) return false
                }
                true
            }
            is GoPointSet -> {
                for(y in 0..51)
                    if (rowBits(y).inv() and InternalGoPointSet.rowUpdaters[y][elements] != 0L)
                        return false
                true
            }
            is GoPointEntries<*, *> -> elements.isEmpty()
            is GoPointValues<GoPoint> -> {
                elements.expungeStaleRows()
                super.containsAll(elements.immutable)
            }
            is GoPointKeys<*> -> {
                elements.expungeStaleRows()
                val rows2 = elements.map.secrets.rows
                for(y in 0..51) {
                    val row2 = rows2[y] ?: continue
                    val row1 = rows1[y] ?: return false
                    for(x in 0..51)
                        if (row1[x] == null && row2[x] != null) return false
                }
                true
            }
            else -> super.containsAll(elements)
        }
    }

    fun rowBits(y: Int): Long {
        val row = map.secrets.rows[y] ?: return 0L
        var bits = 0L
        for(x in 0..51) {
            if (row[x] != null) bits = bits or (1L shl x)
        }
        return bits
    }

    override fun equals(other: Any?): Boolean {
        expungeStaleRows()
        return this === other || when(other) {
            is GoRectangle -> {
                val rows = map.secrets.rows
                for(y in 0..51) {
                    val row = rows[y]
                    if (y !in other.start.y..other.end.y) {
                        if (row == null) continue
                        return false
                    }
                    if (row == null) return false
                    for(x in 0..51)
                        if ((x in other.start.x..other.end.x) == (row[x] == null)) return false
                }
                true
            }
            is GoPointSet -> {
                for(y in 0..51)
                    if (rowBits(y) != InternalGoPointSet.rowUpdaters[y][other]) return false
                true
            }
            is GoPointKeys<*> -> {
                if (map === other.map)
                    return true
                other.expungeStaleRows()
                val rows1 = map.secrets.rows
                val rows2 = other.map.secrets.rows
                for(y in 0..51) {
                    val row1 = rows1[y]
                    val row2 = rows2[y]
                    if (row1 == null && row2 == null) continue
                    if (row1 == null || row2 == null) return false
                    for(x in 0..51)
                        if ((row1[x] == null) != (row2[x] == null)) return false
                }
                true
            }
            is Set<*> -> {
                var c: Collection<*> = other
                if (other is GoPointMapCollection<*, *>) {
                    if (map !== other.map)
                        other.expungeStaleRows()
                    if (other is GoPointEntries<*, *>)
                        return InternalGoPointMap.size[map] == 0 &&
                                InternalGoPointMap.size[other.map] == 0
                    c = other.immutable
                }
                InternalGoPointMap.size[map] == c.size && immutable.containsAll(c)
            }
            else -> false
        }
    }

    override fun hashCode(): Int {
        expungeStaleRows()
        return InternalGoPointMap.hashCode(immutable)
    }

    override fun toString(): String {
        expungeStaleRows()
        return immutable.joinToString(prefix="[", postfix="]")
    }

}

internal class MutableGoPointKeys<V>(
    map: MutableGoPointMap<V>,
    override val immutable: GoPointKeys<V>
): GoPointKeys<V>(map), MutableSet<GoPoint> {

    override val map: MutableGoPointMap<V>
        get() = super.map as MutableGoPointMap<V>

    override fun expungeStaleRows() {
        map.weak.expungeStaleRows()
    }

    override val size: Int get() = map.size

    override fun iterator(): MutableIterator<GoPoint> = ExpungeGoPointMapItr(this)

    override fun contains(element: GoPoint): Boolean {
        expungeStaleRows()
        return immutable.contains(element)
    }

    override fun add(element: GoPoint): Boolean {
        throw UnsupportedOperationException()
    }

    override fun remove(element: GoPoint) = try {
        fastRemove(element)
    } finally {
        expungeStaleRows()
    }

    private fun fastRemove(element: GoPoint): Boolean {
        val (x, y) = element
        val map = this.map
        val rows = map.secrets.rows
        val row = rows[y]
        if (row != null) {
            val entry = row[x] as MutableGoPointEntry<V>?
            if (entry != null) {
                entry.map = null
                row[x] = null
                InternalGoPointMap.size.decrementAndGet(map)
                map.weak.rows[y]?.let { weak ->
                    if (InternalGoPointMap.rowSize.decrementAndGet(weak) <= 0)
                        rows[y] = null
                }
                return true
            }
        }
        return false
    }

    override fun containsAll(elements: Collection<GoPoint>): Boolean {
        expungeStaleRows()
        return immutable.containsAll(elements)
    }

    override fun addAll(elements: Collection<GoPoint>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun removeAll(elements: Collection<GoPoint>): Boolean {
        if (this === elements) {
            if (InternalGoPointMap.size[map] == 0) {
                expungeStaleRows()
                return false
            }
            clear()
            return true
        }
        if (elements is GoPointEntries<*, *>) {
            if (map !== elements.map) elements.expungeStaleRows()
            expungeStaleRows()
            return false
        }
        val rows1 = map.secrets.rows
        val weakRows = map.weak.rows
        var modified = false
        try {
            when (elements) {
                is GoPointKeys<*> -> {
                    if (map !== elements.map) elements.expungeStaleRows()
                    val rows2 = elements.map.secrets.rows
                    for (y in 0..51) {
                        val row1 = rows1[y]
                        val row2 = rows2[y]
                        if (row1 == null || row2 == null) continue
                        for (x in 0..51) {
                            val entry = row1[x] as MutableGoPointEntry<V>?
                            if (entry != null && row2[x] != null) {
                                modified = true
                                entry.map = null
                                row1[x] = null
                                InternalGoPointMap.size.decrementAndGet(map)
                                if (InternalGoPointMap.rowSize.decrementAndGet(weakRows[y]) <= 0) {
                                    rows1[y] = null
                                    break
                                }
                            }
                        }
                    }
                }
                is GoPointSet -> for(y in 0..51) {
                    val row = rows1[y]
                    if (row != null && removeBitsFromRow(y, rows1, weakRows, row,
                            InternalGoPointSet.rowUpdaters[y][elements]))
                        modified = true
                }
                is GoRectangle -> for(y in elements.start.y..elements.end.y) {
                    val row = rows1[y]
                    if (row != null && removeRangeFromRow(elements.start.x, elements.end.x,
                            y, rows1, weakRows, row, retain=false))
                        modified = true
                }
                else -> {
                    val c = toImmutable(elements)
                    if (InternalGoPointMap.size[map] > c.size) {
                        for(e in c)
                            if (fastRemove(e)) modified = true
                    } else {
                        modified = MutableGoPointMapItr(this).removeAll(c)
                    }
                }
            }
        } finally {
            expungeStaleRows()
        }
        return modified
    }

    override fun retainAll(elements: Collection<GoPoint>): Boolean {
        if (this === elements) {
            expungeStaleRows()
            return false
        }
        if (elements is GoPointEntries<*, *>) {
            if (map !== elements.map) elements.expungeStaleRows()
            if (InternalGoPointMap.size[map] == 0) {
                expungeStaleRows()
                return false
            }
            clear()
            return true
        }
        var modified = false
        try {
            val rows1 = map.secrets.rows
            val weakRows = map.weak.rows
            when (elements) {
                is GoPointKeys<*> -> {
                    if (map !== elements.map) elements.expungeStaleRows()
                    val rows2 = elements.map.secrets.rows
                    for(y in 0..51) {
                        val row1 = rows1[y]
                        val weak = weakRows[y]
                        if (row1 == null || weak == null) continue
                        val row2 = rows2[y]
                        if (row2 == null) {
                            InternalGoPointMap.size.getAndAdd(map, -weak.size)
                            weak.size = 0
                            rows1[y] = null
                            modified = true
                        } else {
                            for(x in 0..51) {
                                val entry = row1[x] as MutableGoPointEntry<V>?
                                if (entry != null && row2[x] == null) {
                                    modified = true
                                    entry.map = null
                                    row1[x] = null
                                    InternalGoPointMap.size.decrementAndGet(map)
                                    if (InternalGoPointMap.rowSize.decrementAndGet(weak) <= 0) {
                                        rows1[y] = null
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
                is GoPointSet -> for(y in 0..51) {
                    val row = rows1[y]
                    if (row != null && removeBitsFromRow(y, rows1, weakRows, row,
                            InternalGoPointSet.rowUpdaters[y][elements].inv()))
                        modified = true
                }
                is GoRectangle -> {
                    modified = removeRows(0, elements.start.y - 1, rows1, weakRows)
                    for(y in elements.start.y..elements.end.y) {
                        val row = rows1[y]
                        if (row != null && removeRangeFromRow(
                                elements.start.x, elements.end.x,
                                y, rows1, weakRows, row, retain=true))
                            modified = true
                    }
                    if (removeRows(elements.end.y + 1, 51, rows1, weakRows))
                        modified = true
                }
                else -> modified = MutableGoPointMapItr(this).removeAll(toImmutable(elements), retain=true)
            }
        } finally {
            expungeStaleRows()
        }
        return modified
    }

    private fun removeBitsFromRow(
        y: Int,
        rows: Array<Array<Map.Entry<GoPoint, V>?>?>,
        weakRows: Array<WeakGoPointMap.Row<V>?>,
        row: Array<Map.Entry<GoPoint, V>?>,
        bits: Long
    ): Boolean {
        var remainingBits = bits
        var modified = false
        while (remainingBits != 0L) {
            val bit = remainingBits and -remainingBits
            remainingBits -= bit
            val x = trailingZerosPow2(bit)
            val entry = row[x] as MutableGoPointEntry<V>?
            if (entry != null) {
                modified = true
                entry.map = null
                row[x] = null
                InternalGoPointMap.size.decrementAndGet(map)
                if (InternalGoPointMap.rowSize.decrementAndGet(weakRows[y]) <= 0) {
                    rows[y] = null
                    return true
                }
            }
        }
        return modified
    }

    private fun removeRangeFromRow(
        x1: Int,
        x2: Int,
        y: Int,
        rows: Array<Array<Map.Entry<GoPoint, V>?>?>,
        weakRows: Array<WeakGoPointMap.Row<V>?>,
        row: Array<Map.Entry<GoPoint, V>?>,
        retain: Boolean
    ): Boolean {
        var startX: Int
        var endX: Int
        var repeat: Boolean
        when {
            !retain -> {
                startX = x1
                endX = x2
                repeat = false
            }
            x1 == 0 -> {
                if (x2 >= 51) return false
                startX = x2 + 1
                endX = 51
                repeat = false
            }
            else -> {
                startX = 0
                endX = x1 - 1
                repeat = x2 < 51
            }
        }
        var modified = false
        while(true) {
            for (x in startX..endX) {
                val entry = row[x] as MutableGoPointEntry<V>?
                if (entry != null) {
                    modified = true
                    entry.map = null
                    row[x] = null
                    InternalGoPointMap.size.decrementAndGet(map)
                    if (InternalGoPointMap.rowSize.decrementAndGet(weakRows[y]) <= 0) {
                        rows[y] = null
                        return true
                    }
                }
            }
            if (!repeat) return modified
            startX = x2 + 1
            endX = 51
            repeat = false
        }
    }

    private fun removeRows(
        y1: Int,
        y2: Int,
        rows: Array<Array<Map.Entry<GoPoint, V>?>?>,
        weakRows: Array<WeakGoPointMap.Row<V>?>
    ): Boolean {
        var modified = false
        for(y in y1..y2) {
            val row = rows[y]
            if (row != null) for(x in 0..51) {
                (row[x] as MutableGoPointEntry<V>?)?.map = null
                row[x] = null
            }
            rows[y] = null
            val weak = weakRows[y]
            if (weak != null) {
                val size = InternalGoPointMap.rowSize.getAndSet(weak, 0)
                if (size != 0) {
                    modified = true
                    InternalGoPointMap.size.addAndGet(map, -size)
                }
            }
        }
        return modified

    }

    override fun clear() {
        map.clear()
    }

    override fun toArray(): Array<Any?> {
        expungeStaleRows()
        return immutable.toArray()
    }

    override fun <T> toArray(array: Array<T>): Array<T> {
        expungeStaleRows()
        return immutable.toArray(array)
    }

}

internal open class GoPointValues<out V>(
    map: GoPointMap<V>,
    val entries: GoPointEntries<V, Map.Entry<GoPoint, @UnsafeVariance V>>
): GoPointMapCollection<V, @UnsafeVariance V>(map) {

    override val immutable: GoPointValues<V>
        get() = this

    override fun elementFrom(entry: Map.Entry<GoPoint, @UnsafeVariance V>) = entry.value

    override fun toString(): String {
        expungeStaleRows()
        return entries.joinToString(prefix="[", postfix="]", transform=map::valueToString)
    }

}

internal class MutableGoPointValues<V>(
    map: MutableGoPointMap<V>,
    override val immutable: GoPointValues<V>
): GoPointValues<V>(map, immutable.entries), MutableCollection<V> {

    override val map: MutableGoPointMap<V>
        get() = super.map as MutableGoPointMap<V>

    override fun expungeStaleRows() {
        map.weak.expungeStaleRows()
    }

    override val size: Int get() = map.size

    override fun iterator(): MutableIterator<V> = ExpungeGoPointMapItr(this)

    override fun contains(element: V): Boolean {
        expungeStaleRows()
        return map.isValidValue(element, InternalMarker) &&
                immutable.contains(element)
    }

    override fun add(element: V): Boolean {
        throw UnsupportedOperationException()
    }

    override fun remove(element: V) = try {
        fastRemove(element)
    } finally {
        expungeStaleRows()
    }

    private fun fastRemove(element: V): Boolean {
        val map = this.map
        if (!map.isValidValue(element, InternalMarker)) return false
        val rows = map.secrets.rows
        val weakRows = map.weak.rows
        for(y in 0..51) {
            val row = rows[y]
            if (row != null) for(x in 0..51) {
                val entry = row[x] as MutableGoPointEntry<V>?
                if (entry != null && element == entry.value) {
                    entry.map = null
                    row[x] = null
                    InternalGoPointMap.size.decrementAndGet(map)
                    weakRows[y]?.let { weak ->
                        if (InternalGoPointMap.rowSize.decrementAndGet(weak) <= 0)
                            rows[y] = null
                    }
                    return true
                }
            }
        }
        return false
    }

    override fun containsAll(elements: Collection<V>): Boolean {
        expungeStaleRows()
        return this === elements || immutable.containsAll(elements)
    }

    override fun addAll(elements: Collection<V>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun removeAll(elements: Collection<V>): Boolean {
        if (this === elements) {
            if (InternalGoPointMap.size[map] == 0) {
                expungeStaleRows()
                return false
            }
            clear()
            return true
        }
        return try {
            MutableGoPointMapItr(this).removeAll(toImmutable(elements))
        } finally {
            expungeStaleRows()
        }
    }

    override fun retainAll(elements: Collection<V>): Boolean {
        if (this === elements) {
            expungeStaleRows()
            return false
        }
        return try {
            MutableGoPointMapItr(this).removeAll(toImmutable(elements), retain=true)
        } finally {
            expungeStaleRows()
        }
    }

    override fun clear() {
        map.clear()
    }

    override fun toArray(): Array<Any?> {
        expungeStaleRows()
        return immutable.toArray()
    }

    override fun <T> toArray(array: Array<T>): Array<T> {
        expungeStaleRows()
        return immutable.toArray(array)
    }

}

