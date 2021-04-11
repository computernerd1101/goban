package com.computernerd1101.goban.time

import java.io.*
import java.util.*

@FunctionalInterface
fun interface TimeListener: EventListener {
    fun timeElapsed(e: TimeEvent)
}

class TimeEvent(
    source: Any,
    val timeRemaining: Long,
    val overtimeCode: Int,
    flags: Int
): EventObject(source) {

    @Suppress("unused")
    constructor(source: Any, timeRemaining: Long):
            this(source, timeRemaining, 0, 0)

    @Suppress("unused")
    constructor(source: Any, timeRemaining: Long, overtimeCode: Int):
            this(source, timeRemaining, overtimeCode, FLAG_OVERTIME)

    val flags: Int = flags and (
            if (flags and FLAGS_EXPIRED_TICKING == FLAGS_EXPIRED_TICKING)
                FLAG_MASK xor FLAG_TICKING
            else FLAG_MASK)

    companion object {
        const val FLAG_EXPIRED = 1
        const val FLAG_OVERTIME = 2
        const val FLAG_TICKING = 4
        const val FLAG_MASK = 7

        const val FLAGS_EXPIRED_TICKING = FLAG_EXPIRED or FLAG_TICKING

        private const val serialVersionUID = 1L
    }

    val isExpired: Boolean get() = flags and FLAG_EXPIRED != 0
    val isOvertime: Boolean get() = flags and FLAG_OVERTIME != 0
    val isTicking: Boolean get() = flags and FLAG_TICKING != 0

    operator fun component1(): Any? = source
    operator fun component2() = timeRemaining
    operator fun component3() = overtimeCode
    operator fun component4() = flags

    override fun equals(other: Any?): Boolean {
        return this === other || (other is TimeEvent &&
                source == other.source &&
                timeRemaining == other.timeRemaining &&
                overtimeCode == other.overtimeCode &&
                flags == other.flags)
    }

    override fun hashCode(): Int {
        val hash = source.hashCode()
        val time = timeRemaining
        return ((hash*31 +
                (time.xor(time shr 32).toInt()))*31 +
                overtimeCode)*31 + flags
    }

    override fun toString(): String {
        val flags = this.flags
        return buildString(158) {
            append(TimeEvent::class.java.name)
            .append("[timeRemaining=")
            .append(timeRemaining.millisToStringSeconds())
            .append("s,overtimeCode=")
            .append(overtimeCode)
            .append(",flags=")
            .append(flags)
            if (flags and FLAG_EXPIRED != 0) append(",isExpired=true")
            if (flags and FLAG_OVERTIME != 0) append(",isOvertime=true")
            if (flags and FLAG_TICKING != 0) append(",isTicking=true")
            append("]")
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun readObject(ois: ObjectInputStream) {
        throw InvalidObjectException("cannot deserialize source of type TimeLimit")
    }

}