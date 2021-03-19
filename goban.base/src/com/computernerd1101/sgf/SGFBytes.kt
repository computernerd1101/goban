@file:Suppress("unused")

package com.computernerd1101.sgf

import com.computernerd1101.sgf.internal.*
import java.io.*
import java.nio.charset.Charset

class SGFBytes: RowColumn, MutableIterable<Byte>, Cloneable, Serializable {

    var size: Int
        private set
    private var bytes: ByteArray

    constructor() {
        size = 0
        bytes = Private.DEFAULT_CAPACITY_EMPTY
    }

    constructor(initialCapacity: Int) {
        size = 0
        bytes = when {
            initialCapacity < 0 -> Private.DEFAULT_CAPACITY_EMPTY
            initialCapacity == 0 -> Private.EMPTY
            else -> ByteArray(initialCapacity)
        }
    }

    constructor(bytes: ByteArray) {
        val size = bytes.size
        this.size = size
        this.bytes = if (size == 0) Private.EMPTY else bytes.clone()
    }

    constructor(bytes: ByteArray, start: Int, end: Int) {
        checkBoundsIndexes(start, end, bytes.size)
        this.size = end - start
        this.bytes = bytes.copyOfRange(start, end)
    }

    constructor(other: SGFBytes) {
        row = other.row
        column = other.column
        val size = other.size
        val bytes = other.bytes
        this.size = size
        this.bytes = when {
            bytes.isEmpty() -> bytes
            size == 0 -> Private.EMPTY
            else -> bytes.copyOf(size)
        }
    }

    constructor(other: SGFBytes, start: Int, end: Int) {
        checkBoundsIndexes(start, end, other.size)
        if (start == 0) {
            row = other.row
            column = other.column
        }
        val size = end - start
        val bytes = other.bytes
        this.size = size
        this.bytes = when {
            bytes.isEmpty() -> bytes
            size == 0 -> Private.EMPTY
            else -> bytes.copyOf(size)
        }
    }

    @JvmOverloads
    constructor(str: String, charset: Charset? = null) {
        bytes = Private.parse(str, charset)
        size = bytes.size
    }

    constructor(str: String, enc: String?) {
        bytes = Private.parseEncoding(str, enc)
        size = bytes.size
    }

    @JvmName("byteAt")
    operator fun get(index: Int): Byte {
        checkElementIndex(index, size)
        return bytes[index]
    }

    @JvmName("setByteAt")
    operator fun set(index: Int, b: Byte) {
        checkElementIndex(index, size)
        bytes[index] = b
    }

    override fun iterator(): MutableIterator<Byte> = object: ByteIterator(), MutableIterator<Byte> {

        var index = 0
        var lastReturnedIndex = -1

        override fun hasNext(): Boolean {
            return index < size
        }

        override fun nextByte(): Byte {
            val index = this.index
            if (index >= size) throw NoSuchElementException()
            this.index = index + 1
            lastReturnedIndex = index
            return bytes[index]
        }

        override fun remove() {
            val index = lastReturnedIndex
            if (index < 0) throw IllegalStateException()
            lastReturnedIndex = -1
            delete(index)
        }
    }

    @JvmOverloads
    fun toByteArray(a: ByteArray? = null): ByteArray {
        val bytes = this.bytes
        val size = this.size
        if (a == null || a.size < size) return bytes.copyOf(size)
        bytes.copyInto(a, 0, 0, size)
        return a
    }

    fun toByteArray(from: Int, to: Int, a: ByteArray? = null): ByteArray {
        val bytes = this.bytes
        val size = this.size
        checkRangeIndexes(from, to, size)
        val range = from - to
        if (a == null || a.size < range)
            return bytes.copyOfRange(from, to)
        bytes.copyInto(a, 0, from, to)
        return a
    }

    fun toByteArray(a: ByteArray?, offset: Int): ByteArray {
        return toByteArray0(bytes, 0, size, a, offset)
    }

    fun toByteArray(from: Int, to: Int, a: ByteArray?, offset: Int): ByteArray {
        val bytes = this.bytes
        val size = this.size
        checkRangeIndexes(from, to, size)
        return toByteArray0(bytes, from, to, a, offset)
    }

    private fun toByteArray0(bytes: ByteArray, from: Int, to: Int, a: ByteArray?, offset: Int): ByteArray {
        val range = from - to
        val total = range + offset
        val array: ByteArray
        if (a == null || a.size - offset < range) {
            array = ByteArray(total)
            if (a != null && offset > 0)
                a.copyInto(array, 0, 0, offset)
        } else array = a
        bytes.copyInto(array, from, offset, total)
        return array
    }

    override fun toString(): String {
        val n = size
        val ba = bytes
        return String(CharArray(n) { i ->
            ba[i].toInt().and(0xFF).toChar()
        })
    }

    fun toString(enc: String?) = toString(enc?.let {
        try {
            charset(it)
        } catch(e: Exception) {
            null
        }
    })

    fun toString(charset: Charset?): String {
        return if (charset == null) toString()
        else String(bytes, 0, size, charset)
    }

    @Throws(/* nothing */)
    public override fun clone(): SGFBytes {
        return try {
            val clone = super.clone() as SGFBytes
            val size = clone.size
            val bytes = clone.bytes
            clone.bytes = when {
                bytes.isNotEmpty() -> bytes.clone()
                size == 0 -> Private.EMPTY
                else -> bytes.copyOf(size)
            }
            clone
        } catch(e: CloneNotSupportedException) {
            throw Error(e)
        }
    }

    fun append(b: Byte): SGFBytes {
        val size = size
        val newSize = size + 1
        var bytes = bytes
        if (bytes.size < newSize) {
            bytes = Private.grow(bytes, newSize)
        }
        bytes[size] = b
        this.bytes = bytes
        this.size = newSize
        return this
    }

    fun append(b: ByteArray): SGFBytes {
        if (b.isNotEmpty()) append0(b, 0, b.size)
        return this
    }

    fun append(b: ByteArray, start: Int, end: Int): SGFBytes {
        checkBoundsIndexes(start, end, b.size)
        if (b.isNotEmpty()) append0(b, start, end)
        return this
    }

    fun append(bs: SGFBytes): SGFBytes {
        val size = bs.size
        if (size != 0) append0(bs.bytes, 0, size)
        return this
    }

    fun append(bs: SGFBytes, start: Int, end: Int): SGFBytes {
        checkBoundsIndexes(start, end, bs.size)
        if (start != end) append0(bs.bytes, start, end)
        return this
    }

    private fun append0(b: ByteArray, start: Int, end: Int) {
        val size = this.size
        val more = end - start
        val total = size + more
        var bytes = this.bytes
        if (bytes.size < total) {
            bytes = Private.grow(bytes, total)
        }
        b.copyInto(bytes, size, start, end)
        this.bytes = bytes
        this.size = total
    }

    fun insert(index: Int, b: Byte): SGFBytes {
        val size = this.size
        checkPositionIndex(index, size)
        insert0(index, size, b)
        return this
    }

    fun insert(index: Int, ba: ByteArray): SGFBytes {
        val size = this.size
        checkPositionIndex(index, size)
        insert0(size, index, ba, 0, ba.size)
        return this
    }

    fun insert(index: Int, ba: ByteArray, start: Int, end: Int): SGFBytes {
        val size = this.size
        checkPositionIndex(index, size)
        insert0(size, index, ba, start, end)
        return this
    }

    fun insert(index: Int, bs: SGFBytes): SGFBytes {
        val size = size
        if (index != size) checkPositionIndex(index, size)
        if (this === bs) {
            if (size != 0) insertSelf(size, index, 0, size)
            return this
        } else {
            val srcSize = bs.size
            if (srcSize != 0) insert0(size, index, bs.bytes, 0, srcSize)
        }
        return this
    }

    fun insert(index: Int, bs: SGFBytes, start: Int, end: Int): SGFBytes {
        val size = this.size
        checkPositionIndex(index, size)
        checkBoundsIndexes(start, end, bs.size)
        if (start != end) {
            if (this === bs) insertSelf(size, index, start, end)
            else insert0(size, index, bs.bytes, start, end)
        }
        return this
    }

    private fun insert0(size: Int, index: Int, b: Byte) {
        val newSize = size + 1
        var bytes = bytes
        if (bytes.size < newSize)
            bytes = Private.grow(bytes, newSize)
        if (index < size)
            bytes.copyInto(bytes, index  + 1, index, size)
        bytes[index] = b
        this.bytes = bytes
        this.size = newSize
    }

    private fun insert0(size: Int, index: Int, src: ByteArray, start: Int, end: Int) {
        val more = end - start
        val total = size + more
        var bytes = bytes
        if (bytes.size < total) {
            bytes = Private.grow(bytes, total)
        }
        insertBytes(size, index, start, more, total, bytes, src)
    }

    private fun insertSelf(size: Int, index: Int, start: Int, end: Int) {
        val more = end - start
        val total = size + more
        var bytes = this.bytes
        var src: ByteArray = bytes
        insertBytes(
            size, index,
            when {
                bytes.size < total -> {
                    bytes = Private.grow(bytes, total)
                    start
                }
                index < end -> {
                    src = bytes.copyOfRange(start, end)
                    0
                }
                else -> start
            },
            more, total, bytes, src
        )
    }

    private fun insertBytes(size: Int, index: Int, start: Int, more: Int, total: Int,
                            bytes: ByteArray, src: ByteArray) {
        if (index < size)
            bytes.copyInto(bytes, index + more, index, size)
        src.copyInto(bytes, index, start, start + more)
        this.bytes = bytes
        this.size = total
    }

    fun delete(index: Int): SGFBytes {
        val size = this.size
        checkElementIndex(index, size)
        val bytes = this.bytes
        bytes.copyInto(bytes, index, index + 1, size)
        this.size = size - 1
        this.bytes = bytes
        return this
    }

    fun delete(start: Int, end: Int): SGFBytes {
        val size = this.size
        checkBoundsIndexes(start, end, size)
        if (start != end) delete0(size, bytes, start, end)
        return this
    }

    private fun delete0(size: Int, bytes: ByteArray, start: Int, end: Int) {
        bytes.copyInto(bytes, start, end, size)
        this.bytes = bytes
        this.size = size - end + start
    }

    fun replace(from: Int, to: Int, b: Byte): SGFBytes {
        val size = this.size
        checkRangeIndexes(from, to, size)
        if (from == to) {
            insert0(size, from, b)
        } else {
            val bytes = this.bytes
            val deleteRange = to - from - 1
            if (deleteRange != 0)
                bytes.copyInto(bytes, from + 1, to, size)
            bytes[from] = b
            this.bytes = bytes
            this.size = size - deleteRange
        }
        return this
    }

    fun replace(from: Int, to: Int, ba: ByteArray): SGFBytes {
        val size = this.size
        checkRangeIndexes(from, to, size)
        replace0(size, from, to, ba, 0, ba.size)
        return this
    }

    fun replace(from: Int, to: Int, ba: ByteArray, start: Int, end: Int): SGFBytes {
        val size = size
        checkRangeIndexes(from, to, size)
        checkBoundsIndexes(start, end, ba.size)
        replace0(size, from, to, ba, start, end)
        return this
    }

    fun replace(from: Int, to: Int, bs: SGFBytes): SGFBytes {
        val size = this.size
        checkRangeIndexes(from, to, size)
        if (this != bs) replace0(size, from, to, bs.bytes, 0, bs.size)
        else replaceSelf(size, from, to, 0, size)
        return this
    }

    fun replace(from: Int, to: Int, bs: SGFBytes, start: Int, end: Int): SGFBytes {
        val size = size
        checkRangeIndexes(from, to, size)
        checkBoundsIndexes(start, end, bs.size)
        if (this != bs) replace0(size, from, to, bs.bytes, start, end)
        else replaceSelf(size, from, to, start, end)
        return this
    }

    private fun replace0(size: Int, from: Int, to: Int, ba: ByteArray, start: Int, end: Int) {
        val length = end - start
        val shiftTo = from + length
        val total = size + shiftTo - to
        var bytes = bytes
        if (total > bytes.size) {
            bytes = Private.grow(bytes, total)
        }
        if (shiftTo != to)
            bytes.copyInto(bytes, shiftTo, to, size)
        ba.copyInto(bytes, from, start, end)
        this.bytes = bytes
        this.size = total
    }

    private fun replaceSelf(size: Int, dstStart: Int, dstEnd: Int, srcStart: Int, srcEnd: Int) {
        var bytes = bytes
        when {
            dstStart == srcStart -> {
                if (dstEnd < srcEnd)
                    insertSelf(size, dstEnd, dstEnd, srcEnd)
                else if (dstEnd != srcEnd)
                    delete0(size, bytes, srcEnd, dstEnd)
            }
            dstEnd == srcEnd -> {
                if (dstStart > srcStart)
                    insertSelf(size, dstStart, srcStart, dstStart)
                else delete0(size, bytes, dstStart, srcStart)
            }
            else  -> {
                val dstLength = dstEnd - dstStart
                val srcLength = srcEnd - srcStart
                val newSize = size + srcLength - dstLength
                var src: ByteArray = bytes
                var start = srcStart
                if (newSize > bytes.size) {
                    bytes = Private.grow(bytes, newSize)
                } else if (dstStart < srcEnd) {
                    src = bytes.copyOfRange(srcStart, srcEnd)
                    start = 0
                }
                if (srcLength != dstLength)
                    bytes.copyInto(bytes, dstStart + srcLength, dstEnd, size)
                src.copyInto(bytes, dstStart, start, start + srcLength)
                this.bytes = bytes
                this.size = newSize
            }
        }
    }

    fun trimToSize() {
        val size = size
        val bytes = bytes
        if (size < bytes.size) {
            this.bytes = if (size == 0) Private.EMPTY else bytes.copyOf(size)
        }
    }

    fun ensureCapacity(minCapacity: Int) {
        val bytes = bytes
        if (minCapacity >= bytes.size &&
            !(bytes === Private.DEFAULT_CAPACITY_EMPTY && minCapacity <= Private.DEFAULT_CAPACITY)) {
            this.bytes = Private.grow(bytes, minCapacity)
        }
    }

    private object Private {

        @JvmField val EMPTY = ByteArray(0)
        @JvmField val DEFAULT_CAPACITY_EMPTY = ByteArray(0)
        const val DEFAULT_CAPACITY = 10

        fun parseEncoding(str: String, enc: String?): ByteArray {
            return parse(str, enc?.let {
                try {
                    charset(it)
                } catch(e: Exception) {
                    null
                }
            })
        }

        fun parse(str: String, charset: Charset?): ByteArray {
            val bytes = str.toByteArray(charset ?: Charset.defaultCharset())
            return if (bytes.isEmpty()) EMPTY else bytes
        }

        const val MAX_ARRAY_SIZE = Int.MAX_VALUE - 8

        fun grow(bytes: ByteArray, min: Int): ByteArray {
            return bytes.copyOf(newCapacity(bytes, min))
        }

        fun newCapacity(bytes: ByteArray, min: Int): Int {
            val old = bytes.size
            val cap = old + (old shr 1)
            if (cap - min <= 0) {
                if (bytes === DEFAULT_CAPACITY_EMPTY)
                    return DEFAULT_CAPACITY.coerceAtLeast(min)
                if (min < 0) throw OutOfMemoryError()
                return min
            }
            return if (cap - MAX_ARRAY_SIZE <= 0) cap else hugeCapacity(min)
        }

        fun hugeCapacity(min: Int): Int {
            if (min < 0) throw OutOfMemoryError()
            return if (min > MAX_ARRAY_SIZE) Int.MAX_VALUE else MAX_ARRAY_SIZE
        }

    }

    companion object {

        private const val serialVersionUID = 1L
        private val serialPersistentFields = arrayOf(
            ObjectStreamField("row", Int::class.java),
            ObjectStreamField("column", Int::class.java),
            ObjectStreamField("capacity", Int::class.java),
            ObjectStreamField("size", Int::class.java)
        )

    }

    private fun writeObject(oos: ObjectOutputStream) {
        val fields: ObjectOutputStream.PutField = oos.putFields()
        fields.put("row", row)
        fields.put("column", column)
        val size = this.size
        val bytes = this.bytes
        val capacity = if (bytes === Private.DEFAULT_CAPACITY_EMPTY) -1 else bytes.size
        fields.put("capacity", capacity)
        fields.put("size", size)
        oos.writeFields()
        oos.write(bytes, 0, size)
    }

    private fun readObject(ois: ObjectInputStream) {
        val fields: ObjectInputStream.GetField = ois.readFields()
        row = fields["row", 0]
        column = fields["column", 0]
        val size = fields["size", 0]
        if (size < 0) throw InvalidObjectException("size cannot be negative ($size)")
        var capacity = fields["capacity", -1]
        this.size = size
        this.bytes = when {
            size > 0 -> {
                if (capacity < 0) capacity = Private.DEFAULT_CAPACITY
                if (capacity < size) capacity = size
                val bytes = ByteArray(capacity)
                ois.readFully(bytes, 0, size)
                bytes
            }
            capacity < 0 -> Private.DEFAULT_CAPACITY_EMPTY
            capacity == 0 -> Private.EMPTY
            else -> ByteArray(capacity)
        }
    }

}