@file:JvmMultifileClass
@file:JvmName("GobanKt")

package com.computernerd1101.goban

import com.computernerd1101.goban.internal.*

@Suppress("unused")
fun <V> GoPointMap() = GoPointMap.empty<V>()

@Suppress("unused")
fun <V> GoPointMap(vararg entries: Map.Entry<GoPoint, V>) = GoPointMap.readOnly(*entries)

@Suppress("unused")
fun <V> GoPointMap(vararg entries: Pair<GoPoint, V>) = GoPointMap.readOnly(*entries)

@Suppress("LeakingThis")
open class GoPointMap<out V> internal constructor(entries: Array<out Any>?, marker: InternalMarker):
    Map<GoPoint, V> {

    companion object {

        @JvmField
        val EMPTY = GoPointMap<Nothing>(null, InternalMarker)

        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <V> empty(): GoPointMap<V> = EMPTY

        @JvmStatic
        fun <V> readOnly(vararg entries: Map.Entry<GoPoint, V>): GoPointMap<V> {
            return if (entries.isEmpty()) empty()
            else GoPointMap(entries, InternalMarker)
        }

        fun <V> readOnly(vararg entries: Pair<GoPoint, V>): GoPointMap<V> {
            return if (entries.isEmpty()) empty()
            else GoPointMap(entries, InternalMarker)
        }

    }

    override val size: Int get() = secrets.size

    override fun isEmpty() = size == 0

    internal val secrets: GoPointMapSecrets<V>

    init {
        marker.ignore()
        secrets = GoPointMapSecrets(this)
        if (entries == null) {
            secrets.size = 0
        } else {
            secrets.size = entries.size
            for (entry in entries) {
                val key: GoPoint
                val value: V
                if (entry is Map.Entry<*, *>) {
                    key = entry.key as GoPoint
                    @Suppress("UNCHECKED_CAST")
                    value = entry.value as V
                } else {
                    @Suppress("UNCHECKED_CAST")
                    key = (entry as Pair<GoPoint, V>).first
                    value = entry.second
                }
                val (x, y) = key
                var row = secrets[y]
                if (row == null) {
                    row = AtomicArray52()
                    secrets[y] = row
                } else if (row[x] != null)
                    throw IllegalArgumentException("Duplicate key $key")
                row.size++
                @Suppress("UNCHECKED_CAST")
                row[x] = when {
                    this is MutableGoPointMap<V> -> MutableGoPointEntry(this, key, value)
                    entry is ImmutableEntry<*, *> -> entry as ImmutableEntry<GoPoint, V>
                    else -> ImmutableEntry(key, value)
                }
            }
        }
    }

    override fun containsKey(key: GoPoint): Boolean {
        return secrets[key.y]?.get(key.x) != null
    }

    override fun containsValue(value: @UnsafeVariance V) = secrets.entries.any { it.value == value }

    override fun get(key: GoPoint): V? {
        return secrets[key.y]?.get(key.x)?.value
    }

    override val entries: Set<Map.Entry<GoPoint, V>> get() = secrets.entries

    override val keys: Set<GoPoint> get() = secrets.keys

    override val values: Collection<V> get() = secrets.values

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other is MutableGoPointMap<*>)
            other.weak.expungeStaleRows()
        if (other is GoPointMap<*>)
            return InternalGoPointMap.fastEquals(this, other)
        if (other !is Map<*, *> || secrets.size != other.size) return false
        val entries: Set<*> = other.entries
        for(e2 in entries) {
            if (e2 !is Map.Entry<*, *>) return false
            val key = e2.key as? GoPoint ?: return false
            val row = secrets[key.y] ?: return false
            val e1 = row[key.x] ?: return false
            if (e1.value != e2.value) return false
        }
        return true
    }

    override fun hashCode() = InternalGoPointMap.hashCode(secrets.entries)

    override fun toString() = secrets.entries.toString("{", "}")

}

@Suppress("LeakingThis")
open class MutableGoPointMap<V> private constructor(entries: Array<out Any>?):
    GoPointMap<V>(entries, InternalMarker), MutableMap<GoPoint, V> {

    companion object;

    internal val weak = WeakGoPointMap<V>()

    final override val entries: MutableSet<MutableMap.MutableEntry<GoPoint, V>>
    final override val keys: MutableSet<GoPoint>
    final override val values: MutableCollection<V>

    init {
        val secrets = this.secrets
        this.entries = mutableEntries()
        keys = MutableGoPointKeys(this, secrets.keys)
        values = MutableGoPointValues(this, secrets.values)
        if (entries != null) {
            val weakRows = weak.rows
            val queue = weak.queue
            for(y in 0..51) {
                val row = secrets[y]
                if (row != null)
                    weakRows[y] = WeakGoPointMap.Row(row, queue, y)

            }
            // now safe to leak this
            for(y in 0..51) {
                val row = secrets[y]
                if (row != null) for(x in 0..51) {
                    val entry = row[x]
                    if (entry != null) filterValue(entry.value, InternalMarker)
                }
            }
        }
    }

    constructor(): this(null)

    constructor(vararg entries: Pair<GoPoint, V>): this(entries)

    constructor(vararg entries: Map.Entry<GoPoint, V>): this(entries)

    protected open fun isValidValue(value: Any?, throwIfInvalid: Boolean) = true

    internal fun isValidValue(value: Any?, marker: InternalMarker): Boolean {
        marker.ignore()
        return try {
            isValidValue(value, false)
        } catch (e: Exception) {
            false
        }
    }

    internal fun filterValue(value: Any?, marker: InternalMarker) {
        marker.ignore()
        if (!isValidValue(value, true))
            throw IllegalArgumentException("value")
    }

    final override val size: Int
        get() {
            weak.expungeStaleRows()
            return super.size
        }

    final override fun containsKey(key: GoPoint): Boolean {
        weak.expungeStaleRows()
        return super.containsKey(key)
    }

    final override fun containsValue(value: V): Boolean {
        weak.expungeStaleRows()
        return super.containsValue(value)
    }

    final override fun get(key: GoPoint): V? {
        weak.expungeStaleRows()
        return super.get(key)
    }

    final override fun put(key: GoPoint, value: V): V? {
        weak.expungeStaleRows()
        filterValue(value, InternalMarker)
        return fastPut(key, value)
    }

    private fun fastPut(key: GoPoint, value: V): V? {
        val (x, y) = key
        val row = getOrCreateRow(y)
        var entry = row[x] as MutableGoPointEntry<V>?
        val oldValue: V?
        if (entry == null) {
            entry = MutableGoPointEntry(this, key, value)
            row[x] = entry
            oldValue = null
            AtomicArray52.SIZE.getAndIncrement(secrets)
            AtomicArray52.SIZE.getAndIncrement(secrets[y])
        } else {
            oldValue = entry.setValue(value)
        }
        return oldValue
    }

    private fun getOrCreateRow(y: Int): AtomicArray52<Map.Entry<GoPoint, V>> {
        val update = AtomicArray52.update<AtomicArray52<Map.Entry<GoPoint, V>>>(y)
        val secrets = this.secrets
        var row = update[secrets]
        if (row != null) return row
        val weakRow: WeakGoPointMap.Row<V>? = weak.rows[y]
        if (weakRow != null) {
            row = weakRow.get()
            if (row != null && update.compareAndSet(secrets, null, row))
                return row
        }
        val newRow = AtomicArray52<Map.Entry<GoPoint, V>>()
        while(!update.compareAndSet(secrets, null, newRow)) {
             row = update[secrets]
            if (row != null) return row
        }
        weak.rows[y] = WeakGoPointMap.Row(newRow, weak.queue, y)
        return newRow
    }

    final override fun remove(key: GoPoint) = try {
        fastRemove(key)
    } finally {
        weak.expungeStaleRows()
    }

    private fun fastRemove(key: GoPoint): V? {
        val (x, y) = key
        var value: V? = null
        val secrets = this.secrets
        secrets[y]?.let { row ->
            (row[x] as MutableGoPointEntry<V>?)?.let { entry ->
                value = entry.value
                entry.map = null
                row[x] = null
                val weakRow = weak.rows[y]
                if (weakRow == null || AtomicArray52.SIZE.decrementAndGet(row) <= 0)
                    secrets[y] = null
                AtomicArray52.SIZE.decrementAndGet(secrets)
            }
        }
        return value
    }

    final override fun putAll(from: Map<out GoPoint, V>) {
        weak.expungeStaleRows()
        if (from is GoPointMap<V>) {
            val secrets2 = from.secrets
            for (y in 0..51) secrets2[y]?.let { row2 ->
                val row1 = getOrCreateRow(y)
                for(x in 0..51) row2[x]?.let { entry2 ->
                    val value = entry2.value
                    filterValue(value, InternalMarker)
                    var entry1 = row1[x] as MutableGoPointEntry<V>?
                    if (entry1 == null) {
                        entry1 = MutableGoPointEntry(this, entry2.key, value)
                        row1[x] = entry1
                        AtomicArray52.SIZE.getAndIncrement(secrets)
                        AtomicArray52.SIZE.getAndIncrement(row1)
                    }
                }
            }
        } else for(e in from) {
            val (key, value) = e
            filterValue(value, InternalMarker)
            fastPut(key, value)
        }
    }

    @Suppress("UNCHECKED_CAST")
    final override fun clear() {
        keys.clear()
    }

    final override fun equals(other: Any?): Boolean {
        weak.expungeStaleRows()
        return super.equals(other)
    }

    final override fun hashCode(): Int {
        weak.expungeStaleRows()
        return super.hashCode()
    }

    final override fun toString(): String {
        weak.expungeStaleRows()
        return super.toString()
    }

}
