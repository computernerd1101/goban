package com.computernerd1101.goban.poet

import java.io.FileWriter

fun main() {
    val buf = StringBuilder("""
       |@file:Suppress("unused")
       |
       |package com.computernerd1101.goban.internal
       |
       |import java.util.concurrent.atomic.AtomicLongFieldUpdater
       |
       |internal object GobanRows {
       |    @JvmField val empty = arrayOf(
       |        GobanRows1(), GobanRows2()
    """.trimMargin())
    for(i in 3..51 step 2) {
        buf.append(",\n        GobanRows").append(i).append("(), GobanRows").append(i + 1).append("()")
    }
    for(i in 54..102 step 4) {
        buf.append(",\n        GobanRows").append(i).append("(), GobanRows").append(i + 2).append("()")
    }
    buf.append("""|
       |    )
       |    @JvmField val updaters: Array<AtomicLongFieldUpdater<GobanRows1>> = CharArray(6).let { buf ->
       |        buf[0] = 'r'
       |        buf[1] = 'o'
       |        buf[2] = 'w'
       |        Array(104) { index ->
       |            val nBuf = when {
       |                index >= 100 -> {
       |                    buf[3] = '1'
       |                    buf[4] = '0'
       |                    buf[5] = (index - (100 - '0'.toInt())).toChar()
       |                    6
       |                }
       |                index >= 10 -> {
       |                    buf[3] = '0' + index / 10
       |                    buf[4] = '0' + index % 10
       |                    5
       |                }
       |                else -> {
       |                    buf[3] = '0' + index
       |                    4
       |                }
       |            }
       |            val classIndex: Int = if (index < 52) index
       |            else index / 2 + 26 // (index - 52) / 2 + 52
       |            AtomicLongFieldUpdater.newUpdater(empty[classIndex].javaClass, String(buf, 0, nBuf))
       |        }
       |    }
       |}
       |internal open class GobanRows1 {
       |    open fun newInstance() = GobanRows1()
       |    open val size: Int get() = 1
       |    @Volatile @JvmField var row0: Long = 0L
       |    operator fun get(index: Int) = GobanRows.updaters[index][this]
       |    operator fun set(index: Int, value: Long) {
       |        GobanRows.updaters[index][this] = value
       |    }
       |}
    |""".trimMargin())
    for(i in 2..52) {
        buf.append("internal open class GobanRows").append(i).append(": GobanRows").append(i-1)
            .append("() {\n    override fun newInstance() = GobanRows").append(i)
            .append("()\n    override val size: Int get() = ").append(i)
            .append("\n    @Volatile @JvmField var row").append(i-1).append(": Long = 0L\n}\n")
    }
    for(i in 54..104 step 2) {
        buf.append("internal ")
        if (i != 104) buf.append("open ")
        buf.append("class GobanRows")
            .append(i).append(": GobanRows").append(i-2)
            .append("() {\n    override fun newInstance() = GobanRows").append(i)
            .append("()\n    override val size: Int get() = ").append(i)
            .append("\n    @Volatile @JvmField var row").append(i-2)
            .append(": Long = 0L\n    @Volatile @JvmField var row").append(i-1).append(": Long = 0L\n}\n")
    }
    FileWriter("goban.base/src/com/computernerd1101/goban/internal/GobanRows.kt").use { writer ->
        writer.append(buf)
    }
}
