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
        // ,\n        GobanRows3(), GobanRows4()
        // ...
        // ,\n        GobanRows51(), GobanRows52()
        buf.append(",\n        GobanRows").append(i).append("(), GobanRows").append(i + 1).append("()")
    }
    for(i in 54..102 step 4) {
        // ,\n        GobanRows54(), GobanRows56()
        // ...
        // ,\n        GobanRows102(), GobanRows104()
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
       |                index >= 100 -> { // index is always < 104
       |                    buf[3] = '1'
       |                    buf[4] = '0'
       |                    // buf[5] = '0' + index - 100
       |                    //        = (48 + index - 100).toChar()
       |                    buf[5] = (index - 52).toChar()
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
       |            AtomicLongFieldUpdater.newUpdater(empty[classIndex].javaClass, buf.concatToString(0, nBuf))
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
        // internal open class GobanRows2: GobanRows1() {
        //     override fun newInstance() = GobanRows2()
        //     override val size: Int get() = 2
        //     @Volatile @JvmField var row1: Long = 0L
        // }
        // ...
        // internal open class GobanRows52: GobanRows51() {
        //     override fun newInstance() = GobanRows52()
        //     override val size: Int get() = 52
        //     @Volatile @JvmField var row51: Long = 0L
        // }
        buf.append("internal open class GobanRows").append(i).append(": GobanRows").append(i-1)
            .append("() {\n    override fun newInstance() = GobanRows").append(i)
            .append("()\n    override val size: Int get() = ").append(i)
            .append("\n    @Volatile @JvmField var row").append(i-1).append(": Long = 0L\n}\n")
    }
    for(i in 54..104 step 2) {
        // internal open class GobanRows54: GobanRows52() {
        //     override fun newInstance() = GobanRows54()
        //     override val size: Int get() = 54
        //     @Volatile @JvmField var row52: Long = 0L
        //     @Volatile @JvmField var row53: Long = 0L
        // }
        // ...
        // internal class GobanRows104: GobanRows102() {
        //     override fun newInstance() = GobanRows104()
        //     override val size: Int get() = 104
        //     @Volatile @JvmField var row102: Long = 0L
        //     @Volatile @JvmField var row103: Long = 0L
        // }
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
