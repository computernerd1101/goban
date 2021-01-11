package com.computernerd1101.goban.poet

import java.io.FileWriter

fun main() {
    val buf = StringBuilder("""
       |@file:Suppress("UNCHECKED_CAST", "unused")
       |
       |package com.computernerd1101.goban.internal
       |
       |import java.util.concurrent.atomic.AtomicLongFieldUpdater
       |
       |internal object GobanRows {
       |    @JvmField val init = arrayOf(
       |        GobanRows1, GobanRows2
    """.trimMargin())
    for(i in 2..52) {
        buf.append(",\n        GobanRows").append(i).append(", GobanRows").append(2*i)
    }
    buf.append("""|
       |    )
       |    @JvmField val updaters = arrayOf(
       |        GobanRows1.update0, GobanRows2.update1
    """.trimMargin())
    for(i in 1..51 step 2) {
        buf.append(",\n        GobanRows").append(i).append(".update").append(i-1)
            .append(", GobanRows").append(i+1).append(".update").append(i)
    }
    for(i in 54..104 step 2) {
        buf.append(",\n        GobanRows").append(i).append(".update").append(i-2)
            .append(", GobanRows").append(i).append(".update").append(i-1)
    }
    buf.append("\n    )\n}\n")
    buf.append("""
       |internal open class GobanRows1 {
       |    open val size: Int get() = 1
       |    @Volatile private var row0: Long = 0L
       |    operator fun get(index: Int) = GobanRows.updaters[index][this]
       |    operator fun set(index: Int, value: Long) {
       |        GobanRows.updaters[index][this] = value
       |    }
       |    companion object: () -> GobanRows1 {
       |        @JvmField internal val update0 = atomicLongUpdater<GobanRows1>("row0")
       |        override fun invoke() = GobanRows1()
       |    }
       |}
    |""".trimMargin())
    for(i in 2..52) {
        buf.append("internal open class GobanRows").append(i).append(": GobanRows").append(i-1)
            .append("() {\n    override val size: Int get() = ").append(i)
            .append("\n    @Volatile private var row").append(i-1).append("""|: Long = 0L
               |    companion object: () -> GobanRows1 {
               |        @JvmField
               |        internal val update
            """.trimMargin()).append(i-1).append(" = atomicLongUpdater<GobanRows")
            .append(i).append(">(\"row").append(i-1)
            .append("\") as AtomicLongFieldUpdater<GobanRows1>\n        override fun invoke() = GobanRows")
            .append(i).append("()\n    }\n}\n")
    }

    for(i in 54..104 step 2) {
        buf.append("internal ")
        if (i != 104) buf.append("open ")
        buf.append("class GobanRows")
            .append(i).append(": GobanRows").append(i-2)
            .append("() {\n    override val size: Int get() = ").append(i)
            .append("\n    @Volatile private var row").append(i-2)
            .append(": Long = 0L\n    @Volatile private var row").append(i-1).append("""|: Long = 0L
               |    companion object: () -> GobanRows1 {
               |        @JvmField
               |        internal val update""".trimMargin()).append(i-2)
            .append(" = atomicLongUpdater<GobanRows").append(i)
            .append(">(\"row").append(i-2).append("""|") as AtomicLongFieldUpdater<GobanRows1>
               |        @JvmField
               |        internal val update""".trimMargin()).append(i-1)
            .append(" = atomicLongUpdater<GobanRows").append(i)
            .append(">(\"row").append(i-1)
            .append("\") as AtomicLongFieldUpdater<GobanRows1>\n        override fun invoke() = GobanRows")
            .append(i).append("()\n    }\n}\n")
    }
    FileWriter("goban.base/src/com/computernerd1101/goban/internal/GobanRows.kt").use { writer ->
        writer.append(buf)
    }
}
