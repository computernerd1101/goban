@file:JvmName("Util")
@file:JvmMultifileClass

package com.computernerd1101.sgf

import com.computernerd1101.goban.internal.InternalMarker
import com.computernerd1101.sgf.internal.*
import java.io.*
import kotlin.ConcurrentModificationException
import kotlin.NoSuchElementException
import kotlin.reflect.jvm.jvmName

sealed class SGFTreeElement: RowColumn()

class SGFTree: SGFTreeElement, Serializable {

    private var nodeList: SGFNodeList
    private var subTreeList: SGFSubTreeList

    val nodes: MutableList<SGFNode>
        @JvmName("nodes") get() = nodeList
    val subTrees: MutableList<SGFTree>
        @JvmName("subTrees") get() = subTreeList

    fun subTree(node1: SGFNode, vararg elements: SGFTreeElement): SGFTree {
        val subTree = SGFTree(node1, *elements)
        subTreeList.addPrivileged(subTree)
        return subTree
    }

    fun copy(level: SGFCopyLevel? = null): SGFTree = (level ?: SGFCopyLevel.NODE).copy(this)

    internal constructor(other: SGFTree, level: SGFCopyLevel, marker: InternalMarker) {
        marker.ignore()
        row = other.row
        column = other.column
        nodeList = SGFNodeList(other.nodeList, level)
        subTreeList = SGFSubTreeList(other.subTreeList, level)
    }

    constructor(firstNode: SGFNode) {
        nodeList = SGFNodeList(firstNode)
        subTreeList = SGFSubTreeList()
    }

    constructor(node1: SGFNode, vararg elements: SGFTreeElement) {
        nodeList = SGFNodeList(node1)
        subTreeList = SGFSubTreeList()
        for(element in elements) when(element) {
            is SGFNode -> nodeList.add(element)
            is SGFTree -> subTreeList.addPrivileged(element.copy())
        }
    }

    constructor(nodes: Collection<SGFNode>) {
        nodeList = SGFNodeList(nodes)
        subTreeList = SGFSubTreeList()
    }

    constructor(nodes: Collection<SGFNode>, subTrees: Collection<SGFTree>) {
        nodeList = SGFNodeList(nodes)
        subTreeList = SGFSubTreeList(subTrees)
    }

    @Throws(SGFException::class)
    @JvmOverloads
    constructor(toParse: String, warnings: SGFWarningList = SGFWarningList()):
            this(SGFReader.StringReader(toParse, warnings).startReading())

    @Throws(IOException::class, SGFException::class)
    @JvmOverloads
    constructor(input: InputStream, warnings: SGFWarningList = SGFWarningList()):
            this(SGFReader.IOReader(input, warnings).startReading())

    @OptIn(ExperimentalStdlibApi::class)
    private constructor(reader: SGFReader) {
        row = reader.row
        column = reader.column - 1
        val subTrees = SGFSubTreeList()
        nodeList = reader.readRecursive(subTrees)
        subTreeList = subTrees
    }

    internal constructor(nodes: SGFNodeList, subTrees: SGFSubTreeList) {
        nodeList = nodes
        subTreeList = subTrees
    }

    override fun toString() = buildString {
        SGFWriter.StringWriter(this).writeTree(this@SGFTree)
    }

    @Throws(IOException::class)
    fun write(os: OutputStream) {
        SGFWriter.IOWriter(os).writeTree(this)
    }

    private fun writeObject(oos: ObjectOutputStream) {
        val fields: ObjectOutputStream.PutField = oos.putFields()
        fields.put("row", row)
        fields.put("column", column)
        val nodes = nodeList.size
        fields.put("nodes", nodes)
        val subTrees = subTreeList.size
        fields.put("subTrees", subTrees)
        oos.writeFields()
        nodeList.write(oos, nodes)
        subTreeList.write(oos, subTrees)
    }

    private fun readObject(ois: ObjectInputStream) {
        val fields: ObjectInputStream.GetField = ois.readFields()
        row = fields["row", 0]
        column = fields["column", 0]
        val nodes = fields["nodes", -1]
        if (nodes < 0) throw InvalidObjectException("nodes require non-negative size")
        val subTrees = fields["subTrees", -1]
        if (subTrees < 0) throw InvalidObjectException("subTrees require non-negative size")
        nodeList = SGFNodeList(ois, nodes)
        subTreeList = SGFSubTreeList(ois, nodes)
    }

    companion object {

        private const val serialVersionUID = 1L
        private val serialPersistentFields = arrayOf(
            ObjectStreamField("row", Int::class.java),
            ObjectStreamField("column", Int::class.java),
            ObjectStreamField("nodes", Int::class.java),
            ObjectStreamField("subTrees", Int::class.java)
        )

    }


}

class SGFNode: SGFTreeElement, Serializable {

    private var map: PropertyMap

    val properties: MutableMap<String, SGFProperty>
        @JvmName("properties") get() = map

    fun copy(level: SGFCopyLevel? = null) = SGFNode(this, level ?: SGFCopyLevel.NODE)

    private constructor(other: SGFNode, level: SGFCopyLevel) {
        row = other.row
        column = other.column
        map = PropertyMap(other.map, level)
    }

    constructor() {
        map = PropertyMap()
    }

    constructor(expectedMaxSize: Int) {
        map = PropertyMap(expectedMaxSize)
    }

    constructor(map: Map<String, SGFProperty>) {
        this.map = PropertyMap(map, SGFCopyLevel.NODE)
    }

    override fun toString() = buildString {
        SGFWriter.StringWriter(this).writeNode(this@SGFNode)
    }

    @Throws(IOException::class)
    fun write(os: OutputStream) {
        SGFWriter.IOWriter(os).writeNode(this)
    }

    private object Private {

        @JvmStatic fun checkKey(name: String) {
            if (!isPropertyName(name)) throw IllegalArgumentException("Invalid property name: $name")
        }

        @JvmStatic fun nextKeyIndex(i: Int, size: Int): Int {
            val next = i + 1
            return if (next >= size) 0 else next
        }

        const val DEFAULT_CAPACITY = 32
        const val MINIMUM_CAPACITY = 4
        const val MAXIMUM_CAPACITY = 1 shl 30

        @JvmStatic fun capacity(size: Int): Int = when {
            size > MAXIMUM_CAPACITY/3 -> MAXIMUM_CAPACITY
            size <= 2*MINIMUM_CAPACITY/3 -> MINIMUM_CAPACITY
            else -> {
                // 2^floor(log2(size * 3))
                var x = size + (size shr 1) // x = size * 3/2
                x = x or (x ushr 16)
                x = x or (x ushr 8)
                x = x or (x ushr 4)
                x = x or (x ushr 2)
                x = x or (x ushr 1)
                x + 1 // cap = 2^(1 + floor(log2(x))
            }
        }

    }

    companion object {

        @JvmStatic
        fun isPropertyName(name: String) = name.isNotEmpty() && name.all { it in 'A'..'Z' }

        private const val serialVersionUID = 1L

        private val serialPersistentFields = arrayOf(
            ObjectStreamField("row", Int::class.java),
            ObjectStreamField("column", Int::class.java),
            ObjectStreamField("size", Int::class.java)
        )

    }

    private fun writeObject(oos: ObjectOutputStream) {
        val fields: ObjectOutputStream.PutField = oos.putFields()
        fields.put("row", row)
        fields.put("column", column)
        val size = map.size
        fields.put("size", size)
        oos.writeFields()
        map.write(oos, size)
    }

    private fun readObject(ois: ObjectInputStream) {
        val fields: ObjectInputStream.GetField = ois.readFields()
        row = fields["row", 0]
        column = fields["column", 0]
        val size = fields["size", -1]
        if (size < 0) throw InvalidObjectException("requires non-negative size")
        map = PropertyMap(ois, size)
    }

    private class PropertyMap: AbstractMutableMap<String, SGFProperty> {

        fun write(oos: ObjectOutputStream, size: Int) {
            var count = 0
            var link = root.next
            while(link != null && link !== root) {
                val (key, value) = link as PropertyEntry
                oos.writeUTF(key)
                oos.writeObject(value)
                count++
                link = link.next
            }
            if (count != size)
                throw InvalidObjectException("Expected size $size but found $count elements")
        }

        constructor(ois: ObjectInputStream, size: Int) {
            val cap = Private.capacity(size)
            table = arrayOfNulls(cap)
            for(n in 0 until size) {
                val key = ois.readUTF()
                if (!isPropertyName(key))
                    throw InvalidObjectException("\"$key\" is not a valid SGF property name")
                val value = ois.readObject() ?: throw InvalidObjectException("property cannot be null")
                if (value !is SGFProperty) throw InvalidObjectException(
                    "${value.javaClass.name} cannot be cast to ${SGFProperty::class.jvmName}")
                val pe = PropertyEntry(key, value, root)
                val i = pe.hash and (cap - 1)
                pe.index = i
                table[i] = pe
            }
            this.size = size
        }

        var modCount: Int = 0

        override var size: Int

        var table: Array<PropertyEntry?>
        val root = PropertyLink(this)

        constructor() {
            size = 0
            table = arrayOfNulls(Private.DEFAULT_CAPACITY)
        }

        constructor(expectedMaxSize: Int) {
            size = 0
            table = arrayOfNulls(Private.capacity(expectedMaxSize))
        }

        constructor(other: Map<out String, SGFProperty>, level: SGFCopyLevel) {
            val copy = level != SGFCopyLevel.NODE && level != SGFCopyLevel.VALUE
            var n = 0
            val cap = if (other is PropertyMap) other.table.size
            else Private.capacity(((1 + other.size)*1.1).toInt())
            val tab = arrayOfNulls<PropertyEntry>(cap)
            for(entry in other) {
                val key = entry.key
                if (!isPropertyName(key)) continue
                var value = entry.value
                if (copy) value = value.copy(level)
                val pe = PropertyEntry(key, value, root)
                val i = pe.hash and (cap - 1)
                pe.index = i
                tab[i] = pe
                n++
            }
            size = n
            table = tab
        }

        override fun get(key: String): SGFProperty? {
            return getProperty(key)?.value
        }

        fun getProperty(key: String): PropertyEntry? {
            if (!isPropertyName(key)) return null
            val tab = table
            val len = tab.size
            var i = key.hashCode() and (len - 1)
            while(true) {
                val entry = tab[i] ?: return null
                if (key == entry.key) return entry
                i = Private.nextKeyIndex(i, len)
            }
        }

        override fun put(key: String, value: SGFProperty): SGFProperty? {
            Private.checkKey(key)
            val oldSize = size
            val newSize = size + 1
            val hash = key.hashCode()
            var tab = table
            var i: Int
            var e: PropertyEntry?
            while(true) {
                val len = tab.size
                i = hash and (len - 1)
                e = tab[i]
                while(e != null) {
                    if (key == e.key) return e.setValue(value)
                    i = Private.nextKeyIndex(i, len)
                    e = tab[i]
                }
                if (newSize + (newSize shl 1) <= len) break
                val newTable = resize(tab, oldSize, len shl 1)
                if (newTable === tab) break
                tab = newTable
            }
            modCount++
            e = PropertyEntry(key, value, root)
            e.index = i
            tab[i] = e
            size = newSize
            return null
        }

        private fun resize(oldTable: Array<PropertyEntry?>, oldSize: Int, newCapacity: Int): Array<PropertyEntry?> {
            val oldCapacity = oldTable.size
            if (oldCapacity == Private.MAXIMUM_CAPACITY) {
                if (oldSize == Private.MAXIMUM_CAPACITY - 1)
                    throw IllegalStateException("Capacity exhausted")
                return oldTable
            }
            if (oldCapacity >= newCapacity) return oldTable
            val newTable = arrayOfNulls<PropertyEntry>(newCapacity)
            var entry = root.next
            while(entry !== root) {
                entry as PropertyEntry
                var i = entry.index
                entry.index = -1
                oldTable[i] = null
                i = entry.hash and (newCapacity - 1)
                while(newTable[i] != null)
                    i = Private.nextKeyIndex(i, newCapacity)
                entry.index = i
                newTable[i] = entry
                entry = entry.next
            }
            table = newTable
            return table
        }

        override fun containsKey(key: String) = getProperty(key) != null

        override fun containsValue(value: SGFProperty): Boolean {
            var link = root.next
            while(link != null && link !== root) {
                if ((link as PropertyEntry).value == value) return true
                link = link.next
            }
            return false
        }

        override fun putAll(from: Map<out String, SGFProperty>) {
            if (from === this) return // redundant
            val n = from.size
            if (n == 0) return
            val s = size
            if (n > s) resize(table, s, Private.capacity(n))
            for(e in from)
                put(e.key, e.value)
        }

        override fun remove(key: String): SGFProperty? {
            if (!isPropertyName(key)) return null
            val tab = table
            val len = tab.size
            var i = key.hashCode() and (len - 1)
            while(true) {
                val e = tab[i] ?: return null
                if (key == e.key) {
                    val value = e.value
                    modCount++
                    e.remove()
                    return value
                }
                i = Private.nextKeyIndex(i, len)
            }
        }

        fun remove(key: String, value: SGFProperty): Boolean {
            if (!isPropertyName(key)) return false
            val tab = table
            val len = tab.size
            var i = key.hashCode() and (len - 1)
            while(true) {
                val e = tab[i] ?: return false
                if (key == e.key) {
                    if (value != e.value) return false
                    modCount++
                    e.remove()
                    return true
                }
                i = Private.nextKeyIndex(i, len)
            }
        }

        override fun clear() {
            size = 0
            modCount++
            table.fill(null)
            val root = this.root
            var entry = root.next
            root.prev = root
            root.next = root
            while(entry !== root) {
                val next = (entry as PropertyEntry).next
                entry.next = null
                entry.prev = null
                entry.map = null
                entry.index = -1
                entry = next
            }
        }

        private var _entries: Entries? = null
        private var _keys: Keys? = null
        private var _values: Values? = null

        override val entries: MutableSet<MutableMap.MutableEntry<String, SGFProperty>> get() {
            var entries = _entries
            if (entries == null) {
                entries = Entries(this)
                _entries = entries
            }
            return entries
        }

        override val keys: MutableSet<String> get() {
            var keys = _keys
            if (keys == null) {
                keys = Keys(this)
                _keys = keys
            }
            return keys
        }

        override val values: MutableCollection<SGFProperty> get() {
            var values = _values
            if (values == null) {
                values = Values(this)
                _values = values
            }
            return values
        }

    }

    private abstract class Itr<E>(val map: PropertyMap): MutableIterator<E> {

        var expectedModCount = map.modCount
        var next: PropertyLink? = map.root.next
        var lastReturned: PropertyEntry? = null

        override fun hasNext(): Boolean {
            val link = next
            return link != null && link != map.root
        }

        fun nextEntry(): PropertyEntry {
            if (map.modCount != expectedModCount)
                throw ConcurrentModificationException()
            val link = next
            if (link == null || link == map.root)
                throw NoSuchElementException()
            next = link.next
            lastReturned = link as PropertyEntry
            return link
        }

        override fun remove() {
            val e = lastReturned ?: throw IllegalStateException()
            if (map.modCount != expectedModCount)
                throw ConcurrentModificationException()
            e.remove()
            lastReturned = null
        }

    }

    @Suppress("LeakingThis")
    private open class PropertyLink {

        var map: PropertyMap?
        var prev: PropertyLink?
        var next: PropertyLink?

        constructor(map: PropertyMap) {
            this.map = map
            prev = this
            next = this
        }

        constructor(next: PropertyLink) {
            map = next.map
            val prev = next.prev!!
            this.prev = prev
            this.next = next
            prev.next = this
            next.prev = this
        }

    }

    private class PropertyEntry(
        override val key: String,
        value: SGFProperty,
        next: PropertyLink
    ): PropertyLink(next), MutableMap.MutableEntry<String, SGFProperty> {

        override var value: SGFProperty = value; private set

        var index: Int = 0
        val hash: Int = key.hashCode()

        override fun setValue(newValue: SGFProperty): SGFProperty {
            val old = this.value
            this.value = newValue
            return old
        }

        override fun equals(other: Any?): Boolean {
            return this === other || other is Map.Entry<*, *> && key == other.key && value == other.value
        }

        override fun hashCode() = hash xor value.hashCode()

        override fun toString() = "$key=$value"

        fun remove() {
            val m = map
            var d = index
            if (m == null || d < 0) throw ConcurrentModificationException()
            val s = m.size
            val p = prev
            val n = next
            val tab: Array<PropertyEntry?> = m.table
            val len = tab.size
            tab[d] = null
            var i = Private.nextKeyIndex(d, len)
            var e = tab[i]
            while (e != null) {
                val r: Int = e.hash and (len - 1)
                if (i < r && (r <= d || d <= i) || d in r..i) {
                    tab[d] = e
                    tab[i] = null
                    d = i
                }
                i = Private.nextKeyIndex(i, len)
                e = tab[i]
            }
            m.size = s - 1
            val root: PropertyLink = m.root
            if (root.next === this) root.next = n
            if (root.prev === this) root.prev = p
            p?.next = n
            n?.prev = p
            prev = null
            next = null
            map = null
            index = -1
        }

    }

    private class EntryItr(map: PropertyMap): Itr<MutableMap.MutableEntry<String, SGFProperty>>(map) {
        override fun next(): MutableMap.MutableEntry<String, SGFProperty> = nextEntry()
    }

    private class KeyItr(map: PropertyMap): Itr<String>(map) {
        override fun next() = nextEntry().key
    }

    private class ValueItr(map: PropertyMap): Itr<SGFProperty>(map) {
        override fun next() = nextEntry().value
    }

    private class Entries(val map: PropertyMap): AbstractMutableSet<MutableMap.MutableEntry<String, SGFProperty>>() {

        override val size: Int get() = map.size

        override fun iterator(): MutableIterator<MutableMap.MutableEntry<String, SGFProperty>> {
            return EntryItr(map)
        }

        override fun contains(element: MutableMap.MutableEntry<String, SGFProperty>): Boolean {
            return map.getProperty(element.key)?.value == element.value
        }

        override fun add(element: MutableMap.MutableEntry<String, SGFProperty>): Boolean {
            throw UnsupportedOperationException()
        }

        override fun remove(element: MutableMap.MutableEntry<String, SGFProperty>): Boolean {
            return map.remove(element.key, element.value)
        }

        override fun clear() {
            map.clear()
        }

    }

    private class Keys(val map: PropertyMap): AbstractMutableSet<String>() {

        override val size: Int get() = map.size

        override fun iterator(): MutableIterator<String> = KeyItr(map)

        override fun contains(element: String) = map.containsKey(element)

        override fun add(element: String): Boolean {
            throw UnsupportedOperationException()
        }

        override fun remove(element: String) = map.remove(element) != null

        override fun clear() {
            map.clear()
        }

    }

    private class Values(val map: PropertyMap): AbstractMutableCollection<SGFProperty>() {

        override val size: Int get() = map.size

        override fun iterator(): MutableIterator<SGFProperty> = ValueItr(map)

        override fun contains(element: SGFProperty) = map.containsValue(element)

        override fun add(element: SGFProperty): Boolean {
            throw UnsupportedOperationException()
        }

        override fun remove(element: SGFProperty): Boolean {
            var entry = map.root.next
            while(entry !== map.root) {
                if ((entry as PropertyEntry).value == element) {
                    entry.remove()
                    return true
                }
                entry = entry.next
            }
            return true
        }

    }

}