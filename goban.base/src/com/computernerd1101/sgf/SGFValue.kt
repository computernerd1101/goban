package com.computernerd1101.sgf

import com.computernerd1101.sgf.internal.*
import java.io.*
import java.nio.charset.Charset

class SGFValue: RowColumn, Serializable {

    private var sequence: Sequence

    val parts: MutableList<SGFBytes>
        @JvmName("parts") get() = sequence

    fun copy(level: SGFCopyLevel? = null) = SGFValue(this, level ?: SGFCopyLevel.COMPOSITE)

    private constructor(other: SGFValue, level: SGFCopyLevel) {
        row = other.row
        column = other.column
        sequence = Sequence(other.sequence, level)
    }

    constructor(first: SGFBytes) {
        sequence = Sequence(1, first)
    }

    constructor(initialCapacity: Int, first: SGFBytes) {
        sequence = Sequence(initialCapacity, first)
    }

    constructor(first: SGFBytes, vararg rest: SGFBytes) {
        sequence = Sequence(first, rest)
    }

    constructor(elements: Collection<SGFBytes>) {
        sequence = Sequence(elements)
    }

    constructor(s: String, charset: Charset?) {
        val bytes = SGFBytes()
        sequence = Sequence(1, bytes)
        addText(s, charset, bytes)
    }

    fun addText(s: String, charset: Charset?): SGFValue {
        val bytes = SGFBytes()
        sequence.add(bytes)
        addText(s, charset, bytes)
        return this
    }

    private fun addText(s: String, charset: Charset?, bs: SGFBytes) {
        var bytes = bs
        val ba: ByteArray = s.toByteArray(charset ?: Charsets.UTF_8)
        var lastIndex = 0
        for(i in ba.indices)
            if (ba[i] == ':'.toByte()) {
                bytes.append(ba, lastIndex, i)
                lastIndex = i + 1
                bytes = SGFBytes()
                sequence.add(bytes)
            }
        bytes.append(ba, lastIndex, ba.size)
    }

    override fun toString() = buildString {
        SGFWriter.StringWriter(this).writeValue(this@SGFValue)
    }

    @Throws(IOException::class)
    fun write(os: OutputStream) {
        SGFWriter.IOWriter(os).writeValue(this)
    }

    private class Sequence: AbstractSGFList<SGFBytes> {

        constructor(initialCapacity: Int, first: SGFBytes): super(initialCapacity, first)

        constructor(first: SGFBytes, rest: Array<out SGFBytes>): super(first, rest)

        constructor(elements: Collection<SGFBytes>): super(elements)

        constructor(other: Sequence, level: SGFCopyLevel): super(other, level)

        constructor(ois: ObjectInputStream, size: Int): super(ois, size)

        override fun newArray(size: Int): Array<SGFBytes?> {
            return arrayOfNulls(size)
        }

        override fun optimizedCopy(n: Int, a: Array<SGFBytes?>, level: SGFCopyLevel) {
            if (level == SGFCopyLevel.ALL)
                for(i in 0 until n) a[i] = a[i]?.clone()
        }

    }

    companion object {

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
        val size = sequence.size
        fields.put("size", size)
        oos.writeFields()
        sequence.write(oos, size)
    }

    private fun readObject(ois: ObjectInputStream) {
        val fields: ObjectInputStream.GetField = ois.readFields()
        row = fields["row", 0]
        column = fields["column", 0]
        val size = fields["size", -1]
        if (size < 0) throw InvalidObjectException("requires non-negative size")
        sequence = Sequence(ois, size)
    }

}