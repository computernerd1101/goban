@file:Suppress("FunctionName", "NOTHING_TO_INLINE")
@file:JvmMultifileClass
@file:JvmName("GobanKt")

package com.computernerd1101.goban

import com.computernerd1101.goban.internal.*
import java.lang.ref.*
import java.util.AbstractMap.SimpleImmutableEntry

inline fun <V> GoPointMap() = GoPointMap.empty<V>()

inline fun <V> GoPointMap(vararg entries: Map.Entry<GoPoint, V>) = GoPointMap.readOnly(*entries)

inline fun <V> GoPointMap(vararg entries: Pair<GoPoint, V>) = GoPointMap.readOnly(*entries)

@Suppress("LeakingThis")
open class GoPointMap<out V> protected constructor(entries: Array<out Any>?):
    Map<GoPoint, V> {

    companion object {

        init {
            InternalGoPointMap.size = atomicIntUpdater("_size")
            InternalGoPointMap.secrets = object: InternalGoPointMap.Secrets {
                @Suppress("UNCHECKED_CAST")
                override fun <V> rows(map: GoPointMap<V>) = map.rows as Array<Array<Map.Entry<GoPoint, V>?>?>
                override fun <V> entries(map: GoPointMap<V>) = map.immutableEntries
                override fun <V> keys(map: GoPointMap<V>) = map.immutableKeys
                override fun <V> values(map: GoPointMap<V>) = map.immutableValues
            }
        }

        @JvmField
        val EMPTY: GoPointMap<*> = GoPointMap<Any>(null)

        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <V> empty(): GoPointMap<V> = EMPTY as GoPointMap<V>

        @JvmStatic
        fun <V> readOnly(vararg entries: Map.Entry<GoPoint, V>): GoPointMap<V> {
            return if (entries.isEmpty()) empty()
            else GoPointMap(entries)
        }

        fun <V> readOnly(vararg entries: Pair<GoPoint, V>): GoPointMap<V> {
            return if (entries.isEmpty()) empty()
            else GoPointMap(entries)
        }

    }

    @Volatile
    private var _size: Int

    override val size: Int
        get() = _size

    override fun isEmpty() = size == 0

    private val rows: Array<out Array<out Map.Entry<GoPoint, V>?>?>

    init {
        @Suppress("SpellCheckingInspection")
        if (this !is MutableGoPointMap<*>) {
            val klass = javaClass
            if (klass != GoPointMap::class.java)
                throw IllegalAccessError("${klass.name} does not have permission to inherit from " +
                        "com.computernerd1101.goban.GoPointMap unless it also inherits from" +
                        "com.computernerd1101.goban.MutableGoPointMap")
        }
        rows = arrayOfNulls(52)
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
                var row = rows[y] as Array<Map.Entry<GoPoint, V>?>?
                if (row == null) {
                    row = arrayOfNulls(52)
                    rows[y] = row
                } else if (rows[x] != null)
                    throw IllegalArgumentException("Duplicate key $key")
                @Suppress("UNCHECKED_CAST")
                row[x] = when {
                    this is MutableGoPointMap<V> -> MutableGoPointEntry(this, key, value)
                    entry is SimpleImmutableEntry<*, *> -> entry as SimpleImmutableEntry<GoPoint, V>
                    else -> SimpleImmutableEntry(key, value)
                }
            }
        }
    }

    override fun containsKey(key: GoPoint): Boolean {
        return rows[key.y]?.get(key.x) != null
    }

    override fun containsValue(value: @UnsafeVariance V) = immutableEntries.any { it.value == value }

    override fun get(key: GoPoint): V? {
        return rows[key.y]?.get(key.x)?.value
    }

    private val immutableEntries: GoPointEntries<V, Map.Entry<GoPoint, V>> =
        GoPointEntries(this)
    override val entries: Set<Map.Entry<GoPoint, V>>
        get() = immutableEntries

    private val immutableKeys = GoPointKeys(this)
    override val keys: Set<GoPoint>
        get() = immutableKeys

    private val immutableValues = GoPointValues(this, immutableEntries)
    override val values: Collection<V>
        get() = immutableValues

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        (other as? MutableGoPointMap<*>)?.expungeStaleRows()
        if (other is GoPointMap<*>)
            return InternalGoPointMap.fastEquals(this, other)
        if (other !is Map<*, *> || _size != other.size) return false
        val entries: Set<*> = other.entries
        for(e2 in entries) {
            if (e2 !is Map.Entry<*, *>) return false
            val key = e2.key as? GoPoint ?: return false
            val row = rows[key.y] ?: return false
            val e1 = row[key.x] ?: return false
            if (e1.value != e2.value) return false
        }
        return true
    }

    override fun hashCode() = InternalGoPointMap.hashCode(immutableEntries)

    override fun toString() = immutableEntries.toString("{", "}")

}

@Suppress("LeakingThis")
open class MutableGoPointMap<V> private constructor(entries: Array<out Any>?): GoPointMap<V>(entries),
    MutableMap<GoPoint, V> {

    companion object {

        init {
            InternalGoPointMap.mutableSecrets = object: InternalGoPointMap.MutableSecrets {

                override fun <V> weakRows(map: MutableGoPointMap<V>) = map.weakRows

                override fun <V> rowQueue(map: MutableGoPointMap<V>) = map.rowQueue

                override fun isValidValue(map: MutableGoPointMap<*>, value: Any?) = try {
                    map.isValidValue(value, false)
                } catch(e: Exception) {
                    false
                }

                override fun filterValue(map: MutableGoPointMap<*>?, value: Any?) {
                    map?.filterValue(value)
                }

            }
        }

    }

    private val weakRows = arrayOfNulls<WeakRow<V>>(52)
    private val rowQueue = ReferenceQueue<Array<Map.Entry<GoPoint, V>?>?>()

    final override val entries: MutableSet<MutableMap.MutableEntry<GoPoint, V>>
    final override val keys: MutableSet<GoPoint>
    final override val values: MutableCollection<V>

    init {
        val secrets = InternalGoPointMap.secrets
        this.entries = MutableGoPointEntries(this, secrets.entries(this))
        keys = MutableGoPointKeys(this, secrets.keys(this))
        values = MutableGoPointValues(this, secrets.values(this))
        if (entries != null) {
            val weak = weakRows
            val rows = InternalGoPointMap.secrets.rows(this)
            for(y in 0..51) if (rows[y] != null)
                weak[y] = WeakRow(this, y)
            // now safe to leak this
            for(y in 0..51) {
                val row = rows[y]
                if (row != null) for(x in 0..51) {
                    val entry = row[x]
                    if (entry != null) filterValue(entry.value)
                }
            }
        }
    }

    constructor(): this(null)

    constructor(vararg entries: Pair<GoPoint, V>): this(entries)

    constructor(vararg entries: Map.Entry<GoPoint, V>): this(entries)

    protected open fun isValidValue(value: Any?, throwIfInvalid: Boolean) = true

    private fun filterValue(value: Any?) {
        if (!isValidValue(value, true))
            throw IllegalArgumentException("value")
    }

    final override val size: Int
        get() {
            expungeStaleRows()
            return super.size
        }

    final override fun containsKey(key: GoPoint): Boolean {
        expungeStaleRows()
        return super.containsKey(key)
    }

    final override fun containsValue(value: V): Boolean {
        expungeStaleRows()
        return super.containsValue(value)
    }

    final override fun get(key: GoPoint): V? {
        expungeStaleRows()
        return super.get(key)
    }

    final override fun put(key: GoPoint, value: V): V? {
        expungeStaleRows()
        filterValue(value)
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
            InternalGoPointMap.rowSize.getAndIncrement(weakRows[y])
        } else {
            oldValue = entry.setValue(value)
        }
        return oldValue
    }

    private fun getOrCreateRow(y: Int): Array<Map.Entry<GoPoint, V>?> {
        val rows = InternalGoPointMap.secrets.rows(this)
        var row = rows[y]
        if (row != null) return row
        val weak: WeakRow<V>? = weakRows[y]
        if (weak != null) {
            row = weak.get()
            if (row != null) {
                rows[y] = row
                return row
            }
        }
        row = arrayOfNulls(52)
        rows[y] = row
        weakRows[y] = WeakRow(this, y)
        return row
    }

    final override fun remove(key: GoPoint) = try {
        fastRemove(key)
    } finally {
        this.expungeStaleRows()
    }

    private fun fastRemove(key: GoPoint): V? {
        val (x, y) = key
        var value: V? = null
        val rows = InternalGoPointMap.secrets.rows(this)
        rows[y]?.let { row ->
            (row[x] as MutableGoPointEntry<V>?)?.let { entry ->
                value = entry.value
                entry.map = null
                row[x] = null
                val weak = weakRows[y]
                if (weak == null || InternalGoPointMap.rowSize.decrementAndGet(weak) <= 0)
                    rows[y] = null
                InternalGoPointMap.size.decrementAndGet(this)
            }
        }
        return value
    }

    final override fun putAll(from: Map<out GoPoint, V>) {
        this.expungeStaleRows()
        if (from is GoPointMap<V>) {
            val rows2 = InternalGoPointMap.secrets.rows(from)
            for (y in 0..51) rows2[y]?.let { row2 ->
                val row1 = getOrCreateRow(y)
                val weak = weakRows[y]
                for(x in 0..51) row2[x]?.let { entry2 ->
                    val value = entry2.value
                    filterValue(value)
                    var entry1 = row1[x] as MutableGoPointEntry<V>?
                    if (entry1 == null) {
                        entry1 = MutableGoPointEntry(this, entry2.key, value)
                        row1[x] = entry1
                        InternalGoPointMap.size.getAndIncrement(this)
                        InternalGoPointMap.rowSize.getAndIncrement(weak)
                    }
                }
            }
        } else for(e in from) {
            val (key, value) = e
            filterValue(value)
            fastPut(key, value)
        }
    }

    final override fun clear() {
        try {
            InternalGoPointMap.size[this] = 0
            val rows = InternalGoPointMap.secrets.rows(this)
            for(y in 0..51) {
                val row = rows[y]
                if (row != null) for(x in 0..51)
                    (row[x] as MutableGoPointEntry<V>?)?.map = null
                val weak = weakRows[y]
                if (weak != null) {
                    weak.size = 0
                }
                rows[y] = null
            }
        } finally {
            this.expungeStaleRows()
        }
    }

    final override fun equals(other: Any?): Boolean {
        expungeStaleRows()
        return super.equals(other)
    }

    final override fun hashCode(): Int {
        expungeStaleRows()
        return super.hashCode()
    }

    final override fun toString(): String {
        expungeStaleRows()
        return super.toString()
    }

}
