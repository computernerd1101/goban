@file:JvmMultifileClass
@file:JvmName("GobanKt")

package com.computernerd1101.goban

import com.computernerd1101.goban.internal.*
import java.lang.ref.*
import java.util.AbstractMap.SimpleImmutableEntry

@Suppress("unused")
inline val emptyGoPointMap: GoPointMap<*> get() = GoPointMap.EMPTY

@Suppress("LeakingThis")
open class GoPointMap<out V> protected constructor(entries: Array<out Any>?):
    Map<GoPoint, V> {

    companion object {

        init {
            InternalGoPointMap.size = atomicIntUpdater("_size")
            InternalGoPointMap.secrets = object: InternalGoPointMap.Secrets {
                @Suppress("UNCHECKED_CAST")
                override fun <V> rows(map: GoPointMap<V>): Array<Array<Map.Entry<GoPoint, V>?>?> {
                    return map.rows as Array<Array<Map.Entry<GoPoint, V>?>?>
                }
            }
        }

        @JvmField
        val EMPTY: GoPointMap<*> = GoPointMap<Any>(null)

        @JvmStatic
        @JvmName("of")
        @Suppress("unused", "UNCHECKED_CAST")
        operator fun <V> invoke(): GoPointMap<V> = EMPTY as GoPointMap<V>

        @JvmStatic
        @JvmName("of")
        @Suppress("unused")
        operator fun <V> invoke(vararg entries: Map.Entry<GoPoint, V>): GoPointMap<V> {
            return if (entries.isEmpty()) GoPointMap()
            else GoPointMap(entries)
        }

        operator fun <V> invoke(vararg entries: Pair<GoPoint, V>): GoPointMap<V> {
            return if (entries.isEmpty()) GoPointMap()
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
        if (this !is MutableGoPointMap<*>) {
            @Suppress("SpellCheckingInspection")
            val klass = javaClass
            if (klass != GoPointMap::class.java)
                @Suppress("SpellCheckingInspection")
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

    @JvmField
    protected val immutableEntries: GoPointEntries<V, Map.Entry<GoPoint, @UnsafeVariance V>> =
        GoPointEntries(this)
    override val entries: Set<Map.Entry<GoPoint, V>>
        get() = immutableEntries

    @JvmField
    protected val immutableKeys = GoPointKeys(this)
    override val keys: Set<GoPoint>
        get() = immutableKeys

    @JvmField
    protected val immutableValues = GoPointValues(this, immutableEntries)
    override val values: Collection<V>
        get() = immutableValues

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        (other as? MutableGoPointMap<*>)?.expungeStaleRows()
        if (other is GoPointMap<*>) {
            return InternalGoPointMap.fastEquals(this, other)
        }
        return super.equals(other)
    }

    override fun hashCode() = InternalGoPointMap.hashCode(immutableEntries)

    override fun toString() = immutableEntries.toString("{", "}")

}

@Suppress("LeakingThis")
open class MutableGoPointMap<V>: GoPointMap<V>,
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

    constructor(): super(null)

    constructor(vararg entries: Pair<GoPoint, V>): super(entries) {
        initRows()
    }

    constructor(vararg entries: Map.Entry<GoPoint, V>): super(entries) {
        initRows()
    }

    protected open fun isValidValue(value: Any?, throwIfInvalid: Boolean) = true

    private fun filterValue(value: Any?) {
        if (!isValidValue(value, true))
            throw IllegalArgumentException("value")
    }

    private val weakRows = arrayOfNulls<WeakRow<V>>(52)
    private val rowQueue = ReferenceQueue<Array<Map.Entry<GoPoint, V>?>?>()

    private fun initRows() {
        val weak = weakRows
        val rows = InternalGoPointMap.secrets.rows(this)
        for(y in 0..51) if (rows[y] != null)
            weak[y] = WeakRow(this, y)
        // now safe to leak this
        for(y in 0..51) {
            val row = rows[y]
            if (row != null) for(entry in row)
                if (entry != null) filterValue(entry.value)
        }
    }

    final override val entries: MutableSet<MutableMap.MutableEntry<GoPoint, V>> =
        MutableGoPointEntries(this, immutableEntries)

    final override val keys: MutableSet<GoPoint> = MutableGoPointKeys(this, immutableKeys)

    final override val values: MutableCollection<V> = MutableGoPointValues(this, immutableValues)

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
        return immutableValues.contains(value)
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
            for (y in 0..51) {
                rows2[y]?.let { row2 ->
                    val row1 = getOrCreateRow(y)
                    val weak = weakRows[y]
                    for(x in 0..51) {
                        row2[x]?.let { entry2 ->
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
                }
            }
        } else
            for(e in from.entries) fastPut(e.key, e.value)
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
        return immutableEntries.hashCode()
    }

    final override fun toString(): String {
        expungeStaleRows()
        return super.toString()
    }

}
