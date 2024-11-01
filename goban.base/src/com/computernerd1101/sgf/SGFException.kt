package com.computernerd1101.sgf

import com.computernerd1101.goban.internal.*
import java.io.*

open class SGFException(
    @JvmField var row: Int,
    @JvmField var column: Int,
    message: String?,
    cause: Throwable?
): Exception(message, cause) {

    constructor(row: Int, column: Int): this(row, column, null, null)

    constructor(row: Int, column: Int, message: String?): this(row, column, message, null)

    constructor(row: Int, column: Int, cause: Throwable?): this(row, column, cause?.toString(), cause)

    constructor(): this(0, 0, null, null)

    constructor(message: String?): this(0, 0, message, null)

    constructor(cause: Throwable?): this(0, 0, cause)

    constructor(message: String?, cause: Throwable?): this(0, 0, message, cause)

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
        get() = warningList ?: WARNINGS.getOrDefault(this, SGFWarningList())

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
        private val WARNINGS = atomicUpdater<SGFException, SGFWarningList?>("warningList")

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
        fields.put("warnings", warningList?.takeIf { it.count != 0 })
        oos.writeFields()
    }

    private fun readObject(ois: ObjectInputStream) {
        val fields: ObjectInputStream.GetField = ois.readFields()
        row = fields["row", 0]
        column = fields["column", 0]
        warningList = fields["warnings", null] as? SGFWarningList
    }

}

open class SGFWarning(
    @JvmField var row: Int,
    @JvmField var column: Int,
    message: String?,
    cause: Throwable?
): Throwable(message, cause) {

    constructor(row: Int, column: Int, message: String?): this(row, column, message, null)

    constructor(row: Int, column: Int, cause: Throwable?): this(row, column, cause?.toString(), cause)

    constructor(row: Int, column: Int) : this(row, column, null, null)

    constructor(): this(0, 0, null, null)

    constructor(message: String?): this(0, 0, message, null)

    constructor(cause: Throwable?): this(0, 0, cause)

    constructor(message: String?, cause: Throwable?): this(0, 0, message, cause)

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

    operator fun get(index: Int): SGFWarning = (warningList ?: EMPTY_LIST)[index]

    fun addWarnings(other: SGFWarningList) {
        other.warningList?.let { list2 ->
            val list1 = warningList ?: WARNINGS.getOrDefault(this, arrayListOf())
            list1.addAll(list2)
        }
    }

    fun addWarning(warning: SGFWarning) {
        val list = warningList ?: WARNINGS.getOrDefault(this, arrayListOf())
        list.add(warning)
    }

    operator fun plusAssign(other: SGFWarningList) {
        addWarnings(other)
    }

    operator fun plusAssign(warning: SGFWarning) {
        addWarning(warning)
    }

    val warnings: Array<out SGFWarning>
        get() = warningList?.toArray(EMPTY_ARRAY) ?: EMPTY_ARRAY

    fun sortWarnings() {
        warningList?.sortWith(COMPARE_ROW_COLUMN)
    }

    companion object {

        private val WARNINGS = atomicUpdater<SGFWarningList, ArrayList<SGFWarning>?>("warningList")

        private val EMPTY_ARRAY = arrayOf<SGFWarning>()

        private val EMPTY_LIST = arrayListOf<SGFWarning>()

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
        warningList?.let { list ->
            val n = list.size
            oos.writeInt(n)
            for(i in 0 until n) {
                oos.writeObject(list[i])
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