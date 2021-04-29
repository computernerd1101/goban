@file:JvmMultifileClass
@file:JvmName("GobanKt")

package com.computernerd1101.goban

import com.computernerd1101.goban.internal.*

fun <V> GoPointMap() = GoPointMap.empty<V>()

fun <V> GoPointMap(vararg entries: Map.Entry<GoPoint, V>) = GoPointMap.readOnly(*entries)

fun <V> GoPointMap(vararg entries: Pair<GoPoint, V>) = GoPointMap.readOnly(*entries)

@Suppress("LeakingThis")
open class GoPointMap<out V> internal constructor(entries: Array<out Any>?, marker: InternalMarker):
    Map<GoPoint, V> {

    companion object {

        init {
            InternalGoPointMap.size = atomicIntUpdater("_size")
        }

        @JvmField
        val EMPTY: GoPointMap<*> = GoPointMap<Any>(null, InternalMarker)

        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <V> empty(): GoPointMap<V> = EMPTY as GoPointMap<V>

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

    @Volatile
    private var _size: Int

    override val size: Int get() = _size

    override fun isEmpty() = size == 0

    internal val secrets: GoPointMapSecrets<V>

    init {
        marker.ignore()
        secrets = GoPointMapSecrets(this)
        if (entries == null) {
            _size = 0
        } else {
            _size = entries.size
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
                @Suppress("UNCHECKED_CAST")
                var row = secrets.rows[y]
                if (row == null) {
                    row = arrayOfNulls(52)
                    secrets.rows[y] = row
                } else if (row[x] != null)
                    throw IllegalArgumentException("Duplicate key $key")
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
        return secrets.rows[key.y]?.get(key.x) != null
    }

    override fun containsValue(value: @UnsafeVariance V) = secrets.entries.any { it.value == value }

    override fun get(key: GoPoint): V? {
        return secrets.rows[key.y]?.get(key.x)?.value
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
        if (other !is Map<*, *> || _size != other.size) return false
        val entries: Set<*> = other.entries
        for(e2 in entries) {
            if (e2 !is Map.Entry<*, *>) return false
            val key = e2.key as? GoPoint ?: return false
            val row = secrets.rows[key.y] ?: return false
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
        this.entries = MutableGoPointEntries(this, secrets.entries)
        keys = MutableGoPointKeys(this, secrets.keys)
        values = MutableGoPointValues(this, secrets.values)
        if (entries != null) {
            val weakRows = weak.rows
            val rows = secrets.rows
            for(y in 0..51) if (rows[y] != null)
                weakRows[y] = WeakGoPointMap.Row(this, y)
            // now safe to leak this
            for(y in 0..51) {
                val row = rows[y]
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
            InternalGoPointMap.size.getAndIncrement(this)
            InternalGoPointMap.rowSize.getAndIncrement(weak.rows[y])
        } else {
            oldValue = entry.setValue(value)
        }
        return oldValue
    }

    private fun getOrCreateRow(y: Int): Array<Map.Entry<GoPoint, V>?> {
        val rows = secrets.rows
        var row = rows[y]
        if (row != null) return row
        val weakRow: WeakGoPointMap.Row<V>? = weak.rows[y]
        if (weakRow != null) {
            row = weakRow.get()
            if (row != null) {
                rows[y] = row
                return row
            }
        }
        row = arrayOfNulls(52)
        rows[y] = row
        weak.rows[y] = WeakGoPointMap.Row(this, y)
        return row
    }

    final override fun remove(key: GoPoint) = try {
        fastRemove(key)
    } finally {
        weak.expungeStaleRows()
    }

    private fun fastRemove(key: GoPoint): V? {
        val (x, y) = key
        var value: V? = null
        val rows = secrets.rows
        rows[y]?.let { row ->
            (row[x] as MutableGoPointEntry<V>?)?.let { entry ->
                value = entry.value
                entry.map = null
                row[x] = null
                val weakRow = weak.rows[y]
                if (weakRow == null || InternalGoPointMap.rowSize.decrementAndGet(weakRow) <= 0)
                    rows[y] = null
                InternalGoPointMap.size.decrementAndGet(this)
            }
        }
        return value
    }

    final override fun putAll(from: Map<out GoPoint, V>) {
        weak.expungeStaleRows()
        if (from is GoPointMap<V>) {
            val rows2 = from.secrets.rows
            for (y in 0..51) rows2[y]?.let { row2 ->
                val row1 = getOrCreateRow(y)
                val weakRow = weak.rows[y]
                for(x in 0..51) row2[x]?.let { entry2 ->
                    val value = entry2.value
                    filterValue(value, InternalMarker)
                    var entry1 = row1[x] as MutableGoPointEntry<V>?
                    if (entry1 == null) {
                        entry1 = MutableGoPointEntry(this, entry2.key, value)
                        row1[x] = entry1
                        InternalGoPointMap.size.getAndIncrement(this)
                        InternalGoPointMap.rowSize.getAndIncrement(weakRow)
                    }
                }
            }
        } else for(e in from) {
            val (key, value) = e
            filterValue(value, InternalMarker)
            fastPut(key, value)
        }
    }

    final override fun clear() {
        try {
            InternalGoPointMap.size[this] = 0
            val rows = secrets.rows
            for(y in 0..51) {
                val row = rows[y]
                if (row != null) for(x in 0..51)
                    (row[x] as MutableGoPointEntry<V>?)?.map = null
                val weakRow = weak.rows[y]
                if (weakRow != null) {
                    weakRow.size = 0
                }
                rows[y] = null
            }
        } finally {
            weak.expungeStaleRows()
        }
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
