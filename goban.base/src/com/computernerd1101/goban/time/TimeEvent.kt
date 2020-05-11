package com.computernerd1101.goban.time

import java.util.*

@FunctionalInterface
interface TimeListener: EventListener {
    fun timeElapsed(e: TimeEvent)
}

@Suppress("FunctionName")
@JvmName("timeListener")
inline fun TimeListener(crossinline block: (TimeEvent) -> Unit): TimeListener = object: TimeListener {
    override fun timeElapsed(e: TimeEvent) = block(e)
}

class TimeEvent(
    timeLimit: TimeLimit,
    val timeRemaining: Long,
    val overtimeCode: Int,
    flags: Int
): EventObject(timeLimit) {

    constructor(timeLimit: TimeLimit, timeRemaining: Long):
            this(timeLimit, timeRemaining, 0, 0)

    @Suppress("unused")
    constructor(timeLimit: TimeLimit, timeRemaining: Long, overtimeCode: Int):
            this(timeLimit, timeRemaining, overtimeCode, FLAG_OVERTIME)

    override fun getSource(): TimeLimit = super.getSource() as TimeLimit

    inline val timeLimit: TimeLimit get() = getSource()

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
    }

    val isExpired: Boolean get() = flags and FLAG_EXPIRED != 0
    val isOvertime: Boolean get() = flags and FLAG_OVERTIME != 0
    val isTicking: Boolean get() = flags and FLAG_TICKING != 0

    operator fun component1() = timeLimit
    operator fun component2() = timeRemaining
    operator fun component3() = overtimeCode
    operator fun component4() = flags

    override fun equals(other: Any?): Boolean {
        return this === other || (other is TimeEvent &&
                timeLimit == other.timeLimit &&
                timeRemaining == other.timeRemaining &&
                overtimeCode == other.overtimeCode &&
                flags == other.flags)
    }

    override fun hashCode(): Int {
        val hash = timeLimit.hashCode()
        val time = timeRemaining
        return ((hash*31 +
                (time.toInt() xor (time shl 32).toInt()))*31 +
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

}