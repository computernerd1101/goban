package com.computernerd1101.goban.poet

import java.io.FileWriter

fun main() {
    val buf = StringBuilder("""
       |@file:Suppress("unused")
       |
       |package com.computernerd1101.goban.sgf.internal
       |
       |import java.lang.ref.*
       |import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
       |
       |@Suppress("UNCHECKED_CAST")
       |internal class Weak<T>(referent: T, val table: DateTable12<T>, val index: Int):
       |    WeakReference<T>(referent, table.queue as ReferenceQueue<in T>) {
       |    fun expunge() {
       |        DateTable12.weakUpdater<T>(index).compareAndSet(table, this, null)
       |    }
       |}
       |internal open class DateTable12<T>(@JvmField val queue: ReferenceQueue<*>) {
       |    @Suppress("UNCHECKED_CAST")
       |    companion object {
       |        fun <T> strongUpdater(index: Int) =
       |            strongUpdaters[index] as AtomicReferenceFieldUpdater<DateTable12<T>, T?>
       |        fun <T> weakUpdater(index: Int) =
       |            weakUpdaters[index] as AtomicReferenceFieldUpdater<DateTable12<T>, Weak<T>?>
       |        @JvmField val strongUpdaters: Array<AtomicReferenceFieldUpdater<DateTable12<*>, *>>
       |        @JvmField val weakUpdaters: Array<AtomicReferenceFieldUpdater<DateTable12<*>, Weak<*>?>>
       |        init {
       |            val buffer = CharArray(8)
       |            buffer[0] = 's'
       |            buffer[1] = 't'
       |            buffer[2] = 'r'
       |            buffer[3] = 'o'
       |            buffer[4] = 'n'
       |            buffer[5] = 'g'
       |            strongUpdaters = 
    """.trimMargin()
    )
    generateUpdaterArray(buf, 6)
    buf.append("""|Any::class.java, String(buffer))
       |            }
       |            buffer[0] = 'w'
       |            buffer[1] = 'e'
       |            buffer[2] = 'a'
       |            buffer[3] = 'k'
       |            weakUpdaters = 
    """.trimMargin())
    generateUpdaterArray(buf, 4)
    buf.append("""|Weak::class.java, String(buffer, 0, 6))
       |            }
       |        }
       |    }
       |    open val size: Int get() = 12
    |""".trimMargin())
    for(lo in '0'..'9')
        generateDateTableEntry(buf, '0', lo)
    generateDateTableEntry(buf, '0', 'A')
    generateDateTableEntry(buf, '0', 'B')
    buf.append("}\ninternal open class DateTable128<T>(queue: ReferenceQueue<*>): DateTable12<T>(queue) {\n" +
            "    override val size: Int get() = 128\n")
    for(lo in 'C'..'F')
        generateDateTableEntry(buf, '0', lo)
    for(hi in '1'..'7')
        generateDateTableEntries(buf, hi)
    buf.append("}\ninternal open class DateTable256<T>(queue: ReferenceQueue<*>): DateTable128<T>(queue) {\n" +
            "    override val size: Int get() = 256\n")
    generateDateTableEntries(buf, '8')
    generateDateTableEntries(buf, '9')
    for(hi in 'A'..'F')
        generateDateTableEntries(buf, hi)
    buf.append("}\n")
    FileWriter("goban.base/src/com/computernerd1101/goban/sgf/internal/DateTables.kt").use { writer ->
        writer.append(buf)
    }
}

private fun generateUpdaterArray(buf: StringBuilder, offset: Int) {
    val digitToChar = "] = (if (digit < 10) '0' else 'A' - 10) + digit"
    buf.append("""|Array(256) { index ->
       |                val type = when {
       |                    index < 12 -> DateTable12::class.java
       |                    index < 128 -> DateTable128::class.java
       |                    else -> DateTable256::class.java
       |                } as Class<DateTable12<*>>
       |                var digit = index shr 4
       |                buffer[
    """.trimMargin()).append(offset).append(digitToChar).append("""|
       |                digit = index and 0xF
       |                buffer[
    """.trimMargin()).append(offset + 1).append(digitToChar).append("""|
       |                AtomicReferenceFieldUpdater.newUpdater(type, 
    """.trimMargin())
}

private fun generateDateTableEntries(buf: StringBuilder, hi: Char) {
    for(lo in '0'..'9')
        generateDateTableEntry(buf, hi, lo)
    for(lo in 'A'..'F')
        generateDateTableEntry(buf, hi, lo)
}

private fun generateDateTableEntry(buf: StringBuilder, hi: Char, lo: Char) {
    val prefix = "    @Volatile @JvmField var strong"
    val middle =   ": T?     = null\n    @Volatile @JvmField var weak"
    val suffix = ": Weak<T>? = null\n"
    buf.append(prefix).append(hi).append(lo).append(middle)
        .append(hi).append(lo).append(suffix)
}