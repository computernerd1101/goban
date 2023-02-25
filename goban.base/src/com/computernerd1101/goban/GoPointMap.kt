@file:JvmMultifileClass
@file:JvmName("GobanKt")

package com.computernerd1101.goban

import com.computernerd1101.goban.internal.*
import java.io.*

@Suppress("unused")
fun <V> GoPointMap() = GoPointMap.empty<V>()

@Suppress("unused")
fun <V> GoPointMap(vararg entries: Map.Entry<GoPoint, V>) = GoPointMap.readOnly(*entries)

@Suppress("unused")
fun <V> GoPointMap(vararg entries: Pair<GoPoint, V>) = GoPointMap.readOnly(*entries)

@Suppress("LeakingThis")
open class GoPointMap<out V> internal constructor(entries: Array<out Any>?, marker: InternalMarker):
    Map<GoPoint, V>, Serializable {

    companion object {

        @JvmField
        val EMPTY = GoPointMap<Nothing>(null, InternalMarker)

        @JvmStatic
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

        private const val serialVersionUID = 1L

        private val serialPersistentFields: Array<ObjectStreamField> = ObjectStreamClass.NO_FIELDS

    }

    override val size: Int get() = secrets.size

    @Suppress("ReplaceSizeZeroCheckWithIsEmpty")
    override fun isEmpty() = size == 0

    private var _secrets: GoPointMapSecrets<V>
    internal val secrets: GoPointMapSecrets<V> get() = _secrets

    init {
        marker.ignore()
        _secrets = GoPointMapSecrets(this)
        if (entries == null) {
            _secrets.size = 0
        } else {
            _secrets.size = entries.size
            for (entry in entries) {
                val key: GoPoint
                val value: V
                @Suppress("UNCHECKED_CAST")
                if (entry is Map.Entry<*, *>) {
                    key = entry.key as GoPoint
                    value = entry.value as V
                } else {
                    key = (entry as Pair<GoPoint, V>).first
                    value = entry.second
                }
                val (x, y) = key
                var row = _secrets[y]
                if (row == null) {
                    row = AtomicArray52()
                    _secrets[y] = row
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
        return _secrets[key.y]?.get(key.x) != null
    }

    override fun containsValue(value: @UnsafeVariance V) = _secrets.entries.any { it.value == value }

    override fun get(key: GoPoint): V? {
        return _secrets[key.y]?.get(key.x)?.value
    }

    override val entries: Set<Map.Entry<GoPoint, V>> get() = _secrets.entries
    override val keys: Set<GoPoint> get() = _secrets.keys
    override val values: Collection<V> get() = _secrets.values

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other is MutableGoPointMap<*>)
            other.weak.expungeStaleRows()
        if (other is GoPointMap<*>)
            return InternalGoPointMap.fastEquals(this, other)
        if (other !is Map<*, *> || _secrets.size != other.size) return false
        val entries: Set<*> = other.entries
        for(e2 in entries) {
            if (e2 !is Map.Entry<*, *>) return false
            val key = e2.key as? GoPoint ?: return false
            val row = _secrets[key.y] ?: return false
            val e1 = row[key.x] ?: return false
            if (e1.value != e2.value) return false
        }
        return true
    }

    override fun hashCode() = InternalGoPointMap.hashCode(_secrets.entries)

    override fun toString() = _secrets.entries.toString("{", "}")

    private fun writeObject(oos: ObjectOutputStream) {
        // copy into lists that won't be affected by concurrent modification
        val size = this.size
        val keys = ArrayList<GoPoint>(size)
        val values = ArrayList<V>(size)
        for((k, v) in _secrets.entries) {
            keys.add(k)
            values.add(v)
        }
        oos.writeInt(keys.size)
        for(i in keys.indices) {
            oos.writeObject(keys[i])
            oos.writeObject(values[i])
        }
    }

    private fun readObject(ois: ObjectInputStream) {
        var count = ois.readInt()
        if (count < 0) throw InvalidObjectException("size cannot be negative")
        _secrets = GoPointMapSecrets(this)
        _secrets.size = count
        while(count-- > 0) {
            val key = ois.readObject() ?: throw InvalidObjectException(
                "Cannot cast null to com.computernerd1101.goban.GoPoint"
            )
            val (x, y) = try {
                key as GoPoint
            } catch(e: ClassCastException) {
                var msg = e.message
                if (msg.isNullOrEmpty())
                    msg = "Cannot cast ${key.javaClass.name} to com.computernerd1101.goban.GoPoint"
                throw InvalidObjectException(msg)
            }
            @Suppress("UNCHECKED_CAST")
            val value = ois.readObject() as V
            var row = _secrets[y]
            if (row == null) {
                row = AtomicArray52()
                _secrets[y] = row
            } else if (row[x] != null)
                throw InvalidObjectException("Duplicate key $key")
            row.size++
            row[x] = if (this is MutableGoPointMap<V>)
                MutableGoPointEntry(this, key, value)
            else ImmutableEntry(key, value)
        }
    }

    private fun writeReplace(): Any = if (isEmpty()) EmptyProxy else this
    private fun readResolve(): Any = if (isEmpty()) EMPTY else this

    private object EmptyProxy: Serializable {

        private fun readResolve(): Any = EMPTY

    }

}

@Suppress("LeakingThis")
open class MutableGoPointMap<V> private constructor(entries: Array<out Any>?):
    GoPointMap<V>(entries, InternalMarker), MutableMap<GoPoint, V> {

    private var _weak = WeakGoPointMap<V>()
    private var _entries: MutableGoPointEntries<V>
    private var _keys: MutableGoPointKeys<V>
    private var _values: MutableGoPointValues<V>

    internal val weak: WeakGoPointMap<V> get() = _weak

    @Suppress("UNCHECKED_CAST")
    final override val entries get() = _entries as MutableSet<MutableMap.MutableEntry<GoPoint, V>>
    final override val keys: MutableSet<GoPoint> get() = _keys
    final override val values: MutableCollection<V> get() = _values

    init {
        val secrets = this.secrets
        _entries = MutableGoPointEntries(this, secrets.entries)
        _keys = MutableGoPointKeys(this, secrets.keys)
        _values = MutableGoPointValues(this, secrets.values)
        if (entries != null) {
            val weakRows = _weak.rows
            val queue = _weak.queue
            for(y in 0..51) {
                val row = secrets[y]
                if (row != null)
                    weakRows[y] = WeakGoPointMap.Row(row, queue, y)
            }
            // now safe to leak this
            if (javaClass !== MutableGoPointMap::class.java)
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
            _weak.expungeStaleRows()
            return super.size
        }

    final override fun containsKey(key: GoPoint): Boolean {
        _weak.expungeStaleRows()
        return super.containsKey(key)
    }

    final override fun containsValue(value: V): Boolean {
        _weak.expungeStaleRows()
        return super.containsValue(value)
    }

    final override fun get(key: GoPoint): V? {
        _weak.expungeStaleRows()
        return super.get(key)
    }

    final override fun put(key: GoPoint, value: V): V? {
        _weak.expungeStaleRows()
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
            AtomicArray52.SIZE.getAndIncrement(row)
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
        val weakRow: WeakGoPointMap.Row<V>? = _weak.rows[y]
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
        _weak.rows[y] = WeakGoPointMap.Row(newRow, _weak.queue, y)
        return newRow
    }

    final override fun remove(key: GoPoint): V? = try {
        fastRemove(key)
    } finally {
        _weak.expungeStaleRows()
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
                if (AtomicArray52.SIZE.decrementAndGet(row) <= 0)
                    secrets[y] = null
                AtomicArray52.SIZE.decrementAndGet(secrets)
            }
        }
        return value
    }

    final override fun putAll(from: Map<out GoPoint, V>) {
        _weak.expungeStaleRows()
        if (from is GoPointMap<V>) {
            val secrets2 = from.secrets
            for (y in 0..51) {
                val row2 = secrets2[y] ?: continue
                var row1: AtomicArray52<Map.Entry<GoPoint, V>>? = null
                for(x in 0..51) {
                    val entry2 = row2[x] ?: continue
                    val value = entry2.value
                    filterValue(value, InternalMarker)
                    if (row1 == null) row1 = getOrCreateRow(y)
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

    final override fun clear() {
        keys.clear()
    }

    final override fun equals(other: Any?): Boolean {
        _weak.expungeStaleRows()
        return super.equals(other)
    }

    final override fun hashCode(): Int {
        _weak.expungeStaleRows()
        return super.hashCode()
    }

    final override fun toString(): String {
        _weak.expungeStaleRows()
        return super.toString()
    }

    companion object {

        private const val serialVersionUID = 1L

        private val serialPersistentFields: Array<ObjectStreamField> = ObjectStreamClass.NO_FIELDS

    }

    @Suppress("UNUSED_PARAMETER")
    private fun writeObject(oos: ObjectOutputStream) {
        // Do nothing
    }

    @Suppress("UNUSED_PARAMETER")
    private fun readObject(ois: ObjectInputStream) {
        _weak = WeakGoPointMap()
        val secrets = this.secrets
        _entries = MutableGoPointEntries(this, secrets.entries)
        _keys = MutableGoPointKeys(this, secrets.keys)
        _values = MutableGoPointValues(this, secrets.values)
        val weakRows = _weak.rows
        val queue = _weak.queue
        for(y in 0..51) {
            val row = secrets[y]
            if (row != null)
                weakRows[y] = WeakGoPointMap.Row(row, queue, y)
        }
        // Now safe to leak this
        if (javaClass !== MutableGoPointMap::class.java)
            for (y in 0..51) {
                val row = secrets[y]
                if (row != null) for (x in 0..51) {
                    val entry = row[x]
                    if (entry != null) {
                        val valid = try {
                            isValidValue(entry.value, true)
                        } catch(e: IOException) {
                            throw e
                        } catch(e: Throwable) {
                            throw InvalidObjectException(e.toString()).initCause(e)
                        }
                        if (!valid)
                            throw InvalidObjectException("value")
                    }
                }
            }
    }

}
