package com.computernerd1101.goban.time

import com.computernerd1101.goban.annotations.*
import java.util.regex.Pattern

@PropertyOrder("Seconds", "Moves")
class CanadianOvertime(millis: Long, moves: Int): Overtime() {

    operator fun component1() = millis
    operator fun component2() = moves

    @TimeProperty(name="Seconds", min=1L)
    var millis: Long = if (millis > 0) millis else throw IllegalArgumentException("millis")
        set(value) {
            if (value <= 0L) throw IllegalArgumentException("millis")
            field = value
        }

    @IntProperty(name="Moves", min=1)
    var moves: Int = if (moves > 0) moves else throw IllegalArgumentException("moves")
        set(value) {
            if (value <= 0) throw IllegalArgumentException("moves")
            field = value
        }

    // 600,000 milliseconds = 10 minutes
    @Suppress("unused")
    constructor() : this(millis=600000L, moves=20)

    override fun filterEvent(e: TimeEvent): TimeEvent {
        var time = e.timeRemaining
        var overtime = e.overtimeCode
        var flags = e.flags
        if (time <= 0L) {
            flags = flags or (if (flags and TimeEvent.FLAG_OVERTIME == 0) {
                time += millis
                overtime = moves
                if (time > 0L) TimeEvent.FLAG_OVERTIME
                else TimeEvent.FLAG_EXPIRED
            } else TimeEvent.FLAG_EXPIRED)
        } else if (flags and (TimeEvent.FLAG_OVERTIME or TimeEvent.FLAG_TICKING) == TimeEvent.FLAG_OVERTIME) {
            overtime = if (overtime > 1) overtime - 1
            else {
                time = millis
                moves
            }
        }
        return TimeEvent(e.timeLimit, time, overtime, flags)
    }

    override fun toString() = "${millis.millisToStringSeconds()}/$moves Canadian"

    override fun getTypeString(): String? = "Canadian"

    override fun parseThis(s: String): Boolean = parse(s, this) != null

    companion object {

        @JvmStatic
        fun parse(s: String): CanadianOvertime? = parse(s, null)

        private fun parse(s: String, value: CanadianOvertime?): CanadianOvertime? {
            val m = PATTERN.matcher(s)
            if (!m.find()) return null
            val time: Long
            val moves: Int
            try {
                time = m.group(GROUP_TIME).secondsToMillis()
                moves = m.group(GROUP_MOVES).toInt()
            } catch (e: NumberFormatException) {
                return null
            }
            return value?.apply {
                this.millis = time
                this.moves = moves
            } ?: CanadianOvertime(time, moves)
        }

        private const val GROUP_TIME = 1
        private const val GROUP_MOVES = 2
        private val PATTERN = Pattern.compile(
            "^\\s*(0*\\.0*[1-9]\\d*|0*[1-9]\\d*\\.?\\d*)\\s*/\\s*(0*[1-9]\\d*)\\s*canadian\\s*$",
            Pattern.CASE_INSENSITIVE
        )

    }

}