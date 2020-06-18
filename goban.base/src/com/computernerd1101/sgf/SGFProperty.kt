package com.computernerd1101.sgf

import com.computernerd1101.sgf.internal.*
import java.io.*

class SGFProperty: RowColumn, Serializable {

    private var sequence: Sequence

    val list: MutableList<SGFValue>
        @JvmName("list") get() = sequence

    fun copy(level: SGFCopyLevel? = null): SGFProperty {
        return SGFProperty(this, level ?: SGFCopyLevel.VALUE)
    }

    private constructor(other: SGFProperty, level: SGFCopyLevel) {
        row = other.row
        column = other.column
        sequence = Sequence(other.sequence, level)
    }

    constructor(first: SGFValue) {
        sequence = Sequence(1, first)
    }

    constructor(initialCapacity: Int, first: SGFValue) {
        sequence = Sequence(initialCapacity, first)
    }

    constructor(first: SGFValue, vararg rest: SGFValue) {
        sequence = Sequence(first, rest)
    }

    constructor(elements: Collection<SGFValue>) {
        sequence = Sequence(elements)
    }

    override fun toString() = buildString {
        SGFWriter.StringWriter(this).writeProperty(this@SGFProperty)
    }

    @Throws(IOException::class)
    fun write(os: OutputStream) {
        SGFWriter.IOWriter(os).writeProperty(this)
    }

    private class Sequence: AbstractSGFList<SGFValue> {

        constructor(initialCapacity: Int, first: SGFValue): super(initialCapacity, first)

        constructor(first: SGFValue, rest: Array<out SGFValue>): super(first, rest)

        constructor(elements: Collection<SGFValue>): super(elements)

        constructor(other: Sequence, level: SGFCopyLevel): super(other, level)

        constructor(ois: ObjectInputStream, size: Int): super(ois, size)

        override fun newArray(size: Int): Array<SGFValue?> {
            return arrayOfNulls(size)
        }

        override fun optimizedCopy(n: Int, a: Array<SGFValue?>, level: SGFCopyLevel) {
            if (level == SGFCopyLevel.BYTES || level == SGFCopyLevel.ALL)
                for(i in 0 until n) a[i] = a[i]?.copy(level)
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