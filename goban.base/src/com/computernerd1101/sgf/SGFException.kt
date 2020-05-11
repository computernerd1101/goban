package com.computernerd1101.sgf

import com.computernerd1101.goban.internal.*
import java.io.*

@Suppress("unused")
open class SGFException: Exception {

    @JvmField var row: Int
    @JvmField var column: Int

    constructor() {
        row = 0
        column = 0
    }

    constructor(message: String?): super(message) {
        row = 0
        column = 0
    }

    constructor(cause: Throwable?): super(cause) {
        row = 0
        column = 0
    }

    constructor(message: String?, cause: Throwable?): super(message, cause) {
        row = 0
        column = 0
    }

    constructor(row: Int, column: Int) {
        this.row = row
        this.column = column
    }

    constructor(row: Int, column: Int, message: String?): super(message) {
        this.row = row
        this.column = column
    }

    constructor(row: Int, column: Int, cause: Throwable?): super(cause) {
        this.row = row
        this.column = column
    }

    constructor(row: Int, column: Int, message: String?, cause: Throwable?): super(message, cause) {
        this.row = row
        this.column = column
    }

    override fun toString(): String {
        return buildString {
            append(this@SGFException.javaClass.name)
            val r = row
            val c = column
            if (r > 0 && c > 0)
                append("[Row ").append(r).append(", Column ").append(c).append(']')
            localizedMessage?.let { append(": ").append(it) }
        }
    }

    @Volatile
    private var warningList: SGFWarningList? = null

    val warnings: SGFWarningList
        @JvmName("warnings")
        get() = warningList ?: updateWarnings.getOrDefault(this, SGFWarningList())

    @JvmName("addWarnings")
    operator fun plusAssign(warnings: SGFWarningList) {
        this.warnings += warnings
    }

    @JvmName("addWarning")
    operator fun plusAssign(warning: SGFWarning) {
        warnings += warning
    }

    fun sortWarnings() {
        warningList?.sortWarnings()
    }

    companion object {

        /** Updates [SGFException.warningList] */
        private val updateWarnings = atomicUpdater<SGFException, SGFWarningList?>("warningList")

        private const val serialVersionUID = 1L

        private val serialPersistentFields = arrayOf(
            ObjectStreamField("row", Int::class.java),
            ObjectStreamField("column", Int::class.java),
            ObjectStreamField("warnings", SGFWarningList::class.java, true)
        )

    }

    private fun writeObject(oos: ObjectOutputStream) {
        val fields: ObjectOutputStream.PutField = oos.putFields()
        fields.put("row", row)
        fields.put("column", column)
        fields.put("warnings", warningList?.let {
            if (it.count == 0) null else it
        })
        oos.writeFields()
    }

    private fun readObject(ois: ObjectInputStream) {
        val fields: ObjectInputStream.GetField = ois.readFields()
        row = fields["row", 0]
        column = fields["column", 0]
        warningList = fields["warnings", null] as? SGFWarningList
    }

}

@Suppress("unused")
open class SGFWarning: Throwable {

    @JvmField var row: Int
    @JvmField var column: Int

    constructor() {
        row = 0
        column = 0
    }

    constructor(message: String?): super(message) {
        row = 0
        column = 0
    }

    constructor(cause: Throwable?): super(cause) {
        row = 0
        column = 0
    }

    constructor(message: String?, cause: Throwable?): super(message, cause) {
        row = 0
        column = 0
    }

    constructor(row: Int, column: Int) {
        this.row = row
        this.column = column
    }

    constructor(row: Int, column: Int, message: String?): super(message) {
        this.row = row
        this.column = column
    }

    constructor(row: Int, column: Int, cause: Throwable?): super(cause) {
        this.row = row
        this.column = column
    }

    constructor(row: Int, column: Int, message: String?, cause: Throwable?): super(message, cause) {
        this.row = row
        this.column = column
    }

    override fun toString(): String {
        return buildString {
            append(this@SGFWarning.javaClass.name)
            val r = row
            val c = column
            if (r > 0 && c > 0)
                append("[Row ").append(r).append(", Column ").append(c).append(']')
            localizedMessage?.let { append(": ").append(it) }
        }
    }

    companion object {
        private const val serialVersionUID: Long = 1L
    }

}

open class SGFWarningList: Serializable {

    @Transient @Volatile private var warningList: ArrayList<SGFWarning>?

    constructor() {
        warningList = null
    }

    @Suppress("UNCHECKED_CAST")
    constructor(other: SGFWarningList) {
        warningList = other.warningList?.clone() as ArrayList<SGFWarning>?
    }

    val count: Int
        @JvmName("count")
        get() = warningList?.size ?: 0

    @Suppress("UNCHECKED_CAST")
    fun addWarnings(other: SGFWarningList) {
        other.warningList?.let { list2 ->
            val list1 = warningList ?: updateWarnings.getOrDefault(this, arrayListOf())
            list1.addAll(list2)
        }
    }

    fun addWarning(warning: SGFWarning) {
        val list = warningList ?: updateWarnings.getOrDefault(this, arrayListOf())
        list.add(warning)
    }

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun plusAssign(other: SGFWarningList) {
        addWarnings(other)
    }

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun plusAssign(warning: SGFWarning) {
        addWarning(warning)
    }

    @Suppress("unused")
    val warnings: Array<out SGFWarning>
        get() = warningList?.toArray(EMPTY) ?: EMPTY

    fun sortWarnings() {
        warningList?.sortWith(COMPARE_ROW_COLUMN)
    }

    companion object {

        private val updateWarnings = atomicUpdater<SGFWarningList, ArrayList<SGFWarning>?>("warningList")

        private val EMPTY = arrayOf<SGFWarning>()

        private val COMPARE_ROW_COLUMN = Comparator<SGFWarning> { lhs, rhs ->
            val a = lhs.row
            val b = rhs.row
            when {
                a > b -> 1
                a == b -> lhs.column.compareTo(rhs.column)
                else -> -1
            }
        }

        private const val serialVersionUID: Long = 1L

    }

    private fun writeObject(oos: ObjectOutputStream) {
        oos.defaultWriteObject()
        warningList?.apply {
            val n = size
            oos.writeInt(n)
            for(i in 0 until n) {
                oos.writeObject(this[i])
            }
        } ?: oos.writeInt(0)
    }

    private fun readObject(ois: ObjectInputStream) {
        ois.defaultReadObject()
        var n = ois.readInt()
        if (n > 0) {
            val list = ArrayList<SGFWarning>(n)
            while(n > 0) {
                list.add(ois.readObject() as SGFWarning)
                n--
            }
            warningList = list
        }
    }

}