package com.computernerd1101.goban.internal

import com.computernerd1101.goban.*
import java.io.Serializable
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference

internal object InternalGoPointMap {

    fun fastEquals(m1: GoPointMap<*>, m2: GoPointMap<*>): Boolean {
        if (m1.secrets.size != m2.secrets.size)
            return false
        val rows1 = m1.secrets
        val rows2 = m2.secrets
        for(y in 0..51) {
            val row1 = rows1[y]
            val row2 = rows2[y]
            if (row1 === row2) continue
            if (row1 == null || row2 == null || row1.size != row2.size) return false
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

internal fun GoPointMap<*>?.valueToString(e: Map.Entry<*, *>): String {
    val value = e.value
    return when {
        value === e -> "(this Map.Entry)"
        this == null -> value.toString()
        value === this -> "(this Map)"
        value === entries -> "(this Map.entrySet)"
        value === keys -> "(this Map.keySet)"
        value === values -> "(this Map.values)"
        else -> value.toString()
    }
}

internal class GoPointMapSecrets<out V>(
    map: GoPointMap<V>
): AtomicArray52<AtomicArray52<Map.Entry<GoPoint, @UnsafeVariance V>>>() {

    @JvmField val entries: GoPointEntries<V> = GoPointEntries(map)
    @JvmField val keys = GoPointKeys(map)
    @JvmField val values = GoPointValues(map, entries)

}

internal class WeakGoPointMap<V> {

    internal class Row<V>(
        ref: AtomicArray52<Map.Entry<GoPoint, V>>?,
        queue: ReferenceQueue<AtomicArray52<Map.Entry<GoPoint, V>>?>,
        val y: Int
    ): WeakReference<AtomicArray52<Map.Entry<GoPoint, V>>?>(ref, queue)

    @JvmField val rows = arrayOfNulls<Row<V>>(52)
    @JvmField val queue = ReferenceQueue<AtomicArray52<Map.Entry<GoPoint, V>>?>()

    fun expungeStaleRows() {
        try {
            while(true) {
                val ref = queue.poll() ?: break
                if (ref is Row<*>) {
                    val y = ref.y
                    if (rows[y] === ref) rows[y] = null
                }
            }
        } catch (_: Exception) { }
    }

}

internal abstract class GoPointMapCollection<out V, out E>(open val map: GoPointMap<V>):
    AbstractCollection<E>() {

    abstract fun elementFrom(entry: Map.Entry<GoPoint, @UnsafeVariance V>): E

    abstract val immutable: GoPointMapCollection<V, E>

    open fun expungeStaleRows() = Unit

    override val size: Int
        get() = map.secrets.size

    override fun iterator(): Iterator<E> {
        return GoPointMapItr(this)
    }

    public override fun toArray() = super.toArray()

    public override fun <T> toArray(array: Array<T>) = super.toArray(array)

    fun toImmutable(c: Collection<*>): Collection<*> {
        return if (c is GoPointMapCollection<*, *>) {
            if (c.map !== map) c.expungeStaleRows()
            c.immutable
        } else c
    }

}

internal open class GoPointMapItr<out V, out E>(val collection: GoPointMapCollection<V, E>): Iterator<E> {

    private var nextX = 0
    private var nextY = 0

    protected var lastReturnedX = 0
    protected var lastReturnedY = -1

    private fun nextEntry(): Map.Entry<GoPoint, V>? {
        expungeStaleRows()
        var x: Int
        var y = nextY
        if (y >= 52) return null
        x = nextX
        val rows = collection.map.secrets
        while (y < 52) {
            rows[y]?.let { row: AtomicArray52<Map.Entry<GoPoint, V>> ->
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
        val secrets = map.secrets
        val row = secrets[y] ?: throw IllegalStateException()
        val x = lastReturnedX
        val entry = row[x] as MutableGoPointEntry<V>? ?: throw IllegalStateException()
        entry.map = null
        row[x] = null
        AtomicArray52.SIZE.decrementAndGet(secrets)
        if (AtomicArray52.SIZE.decrementAndGet(row) <= 0) secrets[y] = null
        lastReturnedX = 0
        lastReturnedY = -1
    }

    fun removeAll(c: Collection<*>, retain: Boolean = false): Boolean {
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

internal open class GoPointEntries<out V>(
    map: GoPointMap<V>
): GoPointMapCollection<V, Map.Entry<GoPoint, V>>(map), Set<Map.Entry<GoPoint, V>> {

    override val immutable: GoPointEntries<V>
        get() = this

    @Suppress("UNCHECKED_CAST")
    override fun elementFrom(entry: Map.Entry<GoPoint, @UnsafeVariance V>): Map.Entry<GoPoint, V> =
        if (entry !is ImmutableEntry<*, *>)
            ImmutableEntry(entry.key, entry.value)
        else entry

    override fun contains(element: Map.Entry<GoPoint, @UnsafeVariance V>): Boolean {
        val map = this.map
        val (x, y) = (element as Map.Entry<*, *>).key as? GoPoint ?: return false
        val foundValue: Boolean
        val value: Any?
        if (map is MutableGoPointMap<*>) {
            foundValue = true
            value = element.value
            // TODO Do I really want this here?
            if (!map.isValidValue(value, InternalMarker)) return false
        } else {
            foundValue = false
            value = null
        }
        val entry = map.secrets[y]?.get(x) ?: return false
        return entry.value == if (foundValue) value else element.value
    }

    override fun containsAll(elements: Collection<Map.Entry<GoPoint, @UnsafeVariance V>>): Boolean {
        if (this === elements)
            return true
        @Suppress("USELESS_CAST")
        if ((elements as Collection<*>) is GoRectangle) return false
        if (elements is GoPointKeys<*>)
            return elements.isEmpty()
        if (elements is GoPointValues<Map.Entry<GoPoint, V>>) {
            elements.expungeStaleRows()
            return super.containsAll(elements.immutable)
        }
        if (elements !is GoPointEntries<*>)
            return super.containsAll(elements)
        elements.expungeStaleRows()
        val secrets1 = map.secrets
        val secrets2 = elements.map.secrets
        for(y in 0..51) {
            val row2 = secrets2[y] ?: continue
            val row1 = secrets1[y] ?: return false
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
        if (other is GoPointSet) return map.secrets.size == 0 && other.isEmpty()
        if (other is GoRectangle || other !is Set<*>) return false
        val set: Collection<*> = if (other is GoPointMapCollection<*, *>) {
            if (other is GoPointEntries<*>) {
                if (map === other.map)
                    return true
                other.expungeStaleRows()
                return InternalGoPointMap.fastEquals(map, other.map)
            }
            if (map !== other.map)
                other.expungeStaleRows()
            if (other is GoPointKeys<*>)
                return map.secrets.size == 0 && other.map.secrets.size == 0
            other.immutable
        } else other
        return map.secrets.size == set.size && immutable.containsAll(set)
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
): MutableMap.MutableEntry<GoPoint, V>, Serializable {

    override var value: V = value; private set

    @Transient var map: MutableGoPointMap<V>? = map

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
        return "$key=${map.valueToString(this)}"
    }

    companion object {
        private const val serialVersionUID = 1L
    }

}

internal class MutableGoPointEntries<V>(
    map: MutableGoPointMap<V>,
    override val immutable: GoPointEntries<V>
): GoPointEntries<V>(map), MutableSet<Map.Entry<GoPoint, V>> {

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

    override fun iterator(): MutableIterator<Map.Entry<GoPoint, V>> {
        return ExpungeGoPointMapItr(this)
    }

    override fun contains(element: Map.Entry<GoPoint, V>): Boolean {
        expungeStaleRows()
        return immutable.contains(element)
    }

    override fun add(element: Map.Entry<GoPoint, V>): Boolean =
        throw UnsupportedOperationException()

    override fun remove(element: Map.Entry<GoPoint, V>): Boolean = try {
        fastRemove(element)
    } finally {
        expungeStaleRows()
    }

    private fun fastRemove(element: Map.Entry<*, *>): Boolean {
        val (x, y) = element.key as? GoPoint ?: return false
        val value = element.value
        val map = this.map
        if (!map.isValidValue(value, InternalMarker)) return false
        val secrets = map.secrets
        secrets[y]?.let { row ->
            (row[x] as MutableGoPointEntry<V>?)?.let { entry ->
                if (element.value == entry.value) {
                    entry.map = null
                    row[x] = null
                    AtomicArray52.SIZE.decrementAndGet(secrets)
                    if (AtomicArray52.SIZE.decrementAndGet(row) <= 0)
                        secrets[y] = null
                    return true
                }
            }
        }
        return false
    }

    override fun containsAll(elements: Collection<Map.Entry<GoPoint, V>>): Boolean {
        expungeStaleRows()
        return immutable.containsAll(elements)
    }

    override fun addAll(elements: Collection<Map.Entry<GoPoint, V>>): Boolean {
        expungeStaleRows()
        throw UnsupportedOperationException()
    }

    override fun removeAll(elements: Collection<Map.Entry<GoPoint, V>>): Boolean {
        val secrets1 = map.secrets
        if (this === elements) {
            if (secrets1.size == 0) {
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
            if (elements is GoPointEntries<*>) {
                if (map !== elements.map) elements.expungeStaleRows()
                val secrets2 = elements.map.secrets
                for (y in 0..51) {
                    val row1 = secrets1[y]
                    val row2 = secrets2[y]
                    if (row1 == null || row2 == null) continue
                    for (x in 0..51) {
                        val e1 = row1[x] as MutableGoPointEntry<V>?
                        val e2 = row2[x]
                        if (e1 != null && e2 != null && e1.value == e2.value) {
                            modified = true
                            e1.map = null
                            row1[x] = null
                            AtomicArray52.SIZE.decrementAndGet(secrets1)
                            if (AtomicArray52.SIZE.decrementAndGet(row1) <= 0) {
                                secrets1[y] = null
                                break
                            }
                        }
                    }
                }
            } else {
                val c = toImmutable(elements)
                if (secrets1.size > c.size) {
                    for(e in c)
                        if (e is Map.Entry<*, *> && fastRemove(e)) modified = true
                } else modified = MutableGoPointMapItr(this).removeAll(c)
            }
        } finally {
            expungeStaleRows()
        }
        return modified
    }

    override fun retainAll(elements: Collection<Map.Entry<GoPoint, V>>): Boolean {
        if (this === elements) {
            expungeStaleRows()
            return false
        }
        val secrets1 = map.secrets
        if (elements is GoPointKeys<*>) {
            if (map !== elements.map) elements.expungeStaleRows()
            if (secrets1.size == 0) {
                expungeStaleRows()
                return false
            }
            clear()
            return true
        }
        var modified = false
        try {
            if (elements is GoPointEntries<*>) {
                if (map !== elements.map) elements.expungeStaleRows()
                val secrets2 = elements.map.secrets
                for (y in 0..51) {
                    val row1 = secrets1[y] ?: continue
                    val row2 = secrets2[y]
                    if (row2 == null) {
                        for(x in 0..51) {
                            val entry = row1[x] as MutableGoPointEntry<V>?
                            if (entry != null) {
                                entry.map = null
                                row1[x] = null
                            }
                        }
                        AtomicArray52.SIZE.getAndAdd(secrets1, -AtomicArray52.SIZE.getAndSet(row1, 0))
                        secrets1[y] = null
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
                                    AtomicArray52.SIZE.decrementAndGet(secrets1)
                                    if (AtomicArray52.SIZE.decrementAndGet(row1) <= 0) {
                                        secrets1[y] = null
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
        return map.secrets[element.y]?.get(element.x) != null
    }

    override fun containsAll(elements: Collection<GoPoint>): Boolean {
        if (this === elements)
            return true
        val secrets1 = map.secrets
        return when(elements) {
            is GoRectangle -> {
                for(y in elements.start.y..elements.end.y) {
                    val row = secrets1[y] ?: return false
                    for(x in elements.start.x..elements.end.x)
                        if (row[x] == null) return false
                }
                true
            }
            is GoPointSet -> {
                for(y in 0..51)
                    if (rowBits(y).inv() and InternalGoPointSet.ROWS[y][elements] != 0L)
                        return false
                true
            }
            is GoPointEntries<*> -> elements.isEmpty()
            is GoPointValues<GoPoint> -> {
                elements.expungeStaleRows()
                super.containsAll(elements.immutable)
            }
            is GoPointKeys<*> -> {
                elements.expungeStaleRows()
                val secrets2 = elements.map.secrets
                for(y in 0..51) {
                    val row2 = secrets2[y] ?: continue
                    val row1 = secrets1[y] ?: return false
                    for(x in 0..51)
                        if (row1[x] == null && row2[x] != null) return false
                }
                true
            }
            else -> super.containsAll(elements)
        }
    }

    fun rowBits(y: Int): Long {
        val row = map.secrets[y] ?: return 0L
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
                val secrets = map.secrets
                for(y in 0..51) {
                    val row = secrets[y]
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
                    if (rowBits(y) != InternalGoPointSet.ROWS[y][other]) return false
                true
            }
            is GoPointKeys<*> -> {
                if (map === other.map)
                    return true
                other.expungeStaleRows()
                val secrets1 = map.secrets
                val secrets2 = other.map.secrets
                for(y in 0..51) {
                    val row1 = secrets1[y]
                    val row2 = secrets2[y]
                    if (row1 == null && row2 == null) continue
                    if (row1 == null || row2 == null) return false
                    for(x in 0..51)
                        if ((row1[x] == null) != (row2[x] == null)) return false
                }
                true
            }
            is Set<*> -> {
                val c: Collection<*> = if (other is GoPointMapCollection<*, *>) {
                    if (map !== other.map)
                        other.expungeStaleRows()
                    if (other is GoPointEntries<*>)
                        return map.secrets.size == 0 && other.map.secrets.size == 0
                    other.immutable
                } else other
                map.secrets.size == c.size && immutable.containsAll(c)
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
        val secrets = map.secrets
        val row = secrets[y]
        if (row != null) {
            val entry = row[x] as MutableGoPointEntry<V>?
            if (entry != null) {
                entry.map = null
                row[x] = null
                AtomicArray52.SIZE.decrementAndGet(secrets)
                if (AtomicArray52.SIZE.decrementAndGet(row) <= 0)
                    secrets[y] = null
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

    @Suppress("UNCHECKED_CAST")
    override fun removeAll(elements: Collection<GoPoint>): Boolean {
        if (this === elements) {
            if (map.secrets.size == 0) {
                expungeStaleRows()
                return false
            }
            clear()
            return true
        }
        if (elements is GoPointEntries<*>) {
            if (map !== elements.map) elements.expungeStaleRows()
            expungeStaleRows()
            return false
        }
        val secrets1 = map.secrets
        var modified = false
        try {
            when (elements) {
                is GoPointKeys<*> -> {
                    if (map !== elements.map) elements.expungeStaleRows()
                    val secrets2 = elements.map.secrets
                    for (y in 0..51) {
                        val row1 = secrets1[y]
                        val row2 = secrets2[y]
                        if (row1 == null || row2 == null) continue
                        for (x in 0..51) {
                            val entry = row1[x] as MutableGoPointEntry<V>?
                            if (entry != null && row2[x] != null) {
                                modified = true
                                entry.map = null
                                row1[x] = null
                                AtomicArray52.SIZE.decrementAndGet(secrets1)
                                if (AtomicArray52.SIZE.decrementAndGet(row1) <= 0) {
                                    secrets1[y] = null
                                    break
                                }
                            }
                        }
                    }
                }
                is GoPointSet -> for(y in 0..51) {
                    val row = secrets1[y] as AtomicArray52<MutableGoPointEntry<V>>?
                    if (row != null && removeBitsFromRow(y, secrets1, row,
                            InternalGoPointSet.ROWS[y][elements]))
                        modified = true
                }
                is GoRectangle -> for(y in elements.start.y..elements.end.y) {
                    val row = secrets1[y] as AtomicArray52<MutableGoPointEntry<V>>?
                    if (row != null && removeRangeFromRow(elements.start.x, elements.end.x,
                            y, secrets1, row, retain = false))
                        modified = true
                }
                else -> {
                    val c = toImmutable(elements)
                    if (secrets1.size > c.size) {
                        for(e in c)
                            if (e is GoPoint && fastRemove(e)) modified = true
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

    @Suppress("UNCHECKED_CAST")
    override fun retainAll(elements: Collection<GoPoint>): Boolean {
        if (this === elements) {
            expungeStaleRows()
            return false
        }
        if (elements is GoPointEntries<*>) {
            if (map !== elements.map) elements.expungeStaleRows()
            if (map.secrets.size == 0) {
                expungeStaleRows()
                return false
            }
            clear()
            return true
        }
        var modified = false
        try {
            val secrets1 = map.secrets
            when (elements) {
                is GoPointKeys<*> -> {
                    if (map !== elements.map) elements.expungeStaleRows()
                    val secrets2 = elements.map.secrets
                    for(y in 0..51) {
                        val row1 = secrets1[y] ?: continue
                        val row2 = secrets2[y]
                        if (row2 == null) {
                            val row = row1 as AtomicArray52<MutableGoPointEntry<V>>
                            if (removeRangeFromRow(0, 51, y, secrets1, row, retain = false))
                                modified = true
                        } else {
                            for(x in 0..51) {
                                val entry = row1[x] as MutableGoPointEntry<V>?
                                if (entry != null && row2[x] == null) {
                                    modified = true
                                    entry.map = null
                                    row1[x] = null
                                    AtomicArray52.SIZE.decrementAndGet(secrets1)
                                    if (AtomicArray52.SIZE.decrementAndGet(row1) <= 0) {
                                        secrets1[y] = null
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
                is GoPointSet -> for(y in 0..51) {
                    val row = secrets1[y] as AtomicArray52<MutableGoPointEntry<V>>?
                    if (row != null && removeBitsFromRow(y, secrets1, row,
                            InternalGoPointSet.ROWS[y][elements].inv()))
                        modified = true
                }
                is GoRectangle -> {
                    modified = removeRows(0, elements.start.y - 1, secrets1)
                    for(y in elements.start.y..elements.end.y) {
                        val row = secrets1[y] as AtomicArray52<MutableGoPointEntry<V>>?
                        if (row != null && removeRangeFromRow(
                                elements.start.x, elements.end.x,
                                y, secrets1, row, retain = true))
                            modified = true
                    }
                    if (removeRows(elements.end.y + 1, 51, secrets1))
                        modified = true
                }
                else -> modified = MutableGoPointMapItr(this).removeAll(toImmutable(elements), retain = true)
            }
        } finally {
            expungeStaleRows()
        }
        return modified
    }

    private fun removeBitsFromRow(
        y: Int,
        secrets: GoPointMapSecrets<V>,
        row: AtomicArray52<MutableGoPointEntry<V>>,
        bits: Long
    ): Boolean {
        var remainingBits = bits and ((1L shl 52) - 1L)
        var modified = false
        while (remainingBits != 0L) {
            val bit = remainingBits and -remainingBits
            remainingBits -= bit
            val x = trailingZerosPow2(bit)
            val entry = AtomicArray52.update<MutableGoPointEntry<V>>(x).getAndSet(row, null)
            if (entry != null) {
                modified = true
                entry.map = null
                row[x] = null
                AtomicArray52.SIZE.decrementAndGet(secrets)
                if (AtomicArray52.SIZE.decrementAndGet(row) <= 0) {
                    secrets[y] = null
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
        secrets: GoPointMapSecrets<V>,
        row: AtomicArray52<MutableGoPointEntry<V>>,
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
                val entry = AtomicArray52.update<MutableGoPointEntry<V>>(x).getAndSet(row, null)
                if (entry != null) {
                    modified = true
                    entry.map = null
                    AtomicArray52.SIZE.decrementAndGet(secrets)
                    if (AtomicArray52.SIZE.decrementAndGet(row) <= 0) {
                        secrets[y] = null
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

    @Suppress("UNCHECKED_CAST")
    private fun removeRows(
        y1: Int,
        y2: Int,
        secrets: GoPointMapSecrets<V>
    ): Boolean {
        var modified = false
        for(y in y1..y2) {
            val row = secrets[y] as AtomicArray52<MutableGoPointEntry<V>>?
            if (row != null && removeRangeFromRow(0, 51, y, secrets, row, retain = false))
                modified = true
        }
        return modified
    }

    override fun clear() {
        try {
            removeRows(0, 51, map.secrets)
        } finally {
            expungeStaleRows()
        }
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
    val entries: GoPointEntries<V>
): GoPointMapCollection<V, V>(map) {

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
        val secrets = map.secrets
        for(y in 0..51) {
            val row = secrets[y]
            if (row != null) for(x in 0..51) {
                val entry = row[x] as MutableGoPointEntry<V>?
                if (entry != null && element == entry.value) {
                    entry.map = null
                    row[x] = null
                    AtomicArray52.SIZE.decrementAndGet(secrets)
                    if (AtomicArray52.SIZE.decrementAndGet(row) <= 0)
                        secrets[y] = null
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
            if (map.secrets.size == 0) {
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

