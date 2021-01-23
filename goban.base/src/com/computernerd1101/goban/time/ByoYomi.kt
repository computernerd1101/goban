package com.computernerd1101.goban.time

import com.computernerd1101.goban.annotations.*
import com.computernerd1101.goban.resources.gobanResources
import java.util.*

@PropertyOrder("Periods", "Seconds")
class ByoYomi(periods: Int, millis: Long): Overtime(), PropertyTranslator {

    operator fun component1() = periods
    operator fun component2() = millis

    @IntProperty(name="Periods", min=1)
    var periods: Int = if (periods > 0) periods else throw IllegalArgumentException("periods")
        set(value) {
            if (value <= 0) throw IllegalArgumentException("periods")
            field = value
        }

    @TimeProperty(name="Seconds", min=1L)
    var millis: Long = if (millis > 0L) millis else throw IllegalArgumentException("millis")
        set(value) {
            if (value <= 0L) throw IllegalArgumentException("millis")
            field = value
        }

    @Suppress("unused")
    constructor(): this(periods=1, millis=10000L)

    override fun translateProperty(name: String, locale: Locale): String? {
        return if (name == "Periods" || name == "Seconds") {
            val resources = gobanResources(locale)
            resources.getString("PropertyTranslator.Overtime.$name")
        } else name
    }

    override fun filterEvent(e: TimeEvent): TimeEvent {
        var time = e.timeRemaining
        var overtime = e.overtimeCode
        var flags = e.flags
        if (time > 0L) {
            val t = millis
            if (flags and FLAGS_OVERTIME_TICKING != TimeEvent.FLAG_OVERTIME || time == t) return e
            time = t
        } else {
            overtime = (if (flags and TimeEvent.FLAG_OVERTIME != 0) overtime - 1 else periods) +
                    (time / millis).toInt()
            time = if (flags and TimeEvent.FLAG_TICKING != 0) millis + (time % millis) else millis
            flags = if (overtime > 0) flags or TimeEvent.FLAG_OVERTIME
            else TimeEvent.FLAG_EXPIRED or TimeEvent.FLAG_OVERTIME
        }
        return TimeEvent(e.timeLimit, time, overtime, flags)
    }

    override fun toString() = "$periods*${millis.millisToStringSeconds()} Byo-Yomi"

    override fun getTypeString(): String? = "Byo-Yomi"

    override fun getDisplayName(locale: Locale): String? {
        val resources = gobanResources(locale)
        return resources.getString("time.Overtime.ByoYomi")
    }

    override fun parseThis(s: String): Boolean = Regex.parse(s, this) != null

    companion object {

        private const val FLAGS_OVERTIME_TICKING = TimeEvent.FLAG_OVERTIME or TimeEvent.FLAG_TICKING

        @JvmStatic
        fun parse(s: String): ByoYomi? = Regex.parse(s, null)

    }

    private object Regex {

        const val GROUP_PERIODS = 1
        const val GROUP_TIME = 2

        @JvmField
        val REGEX = """^\s*(0*[1-9]\d*)\s*[*x]\s*(0*\.0*[1-9]\d*|0*[1-9]\d*\.?\d*)\s*byo\s*[\-_]?\s*yomi\s*$"""
            .toRegex(RegexOption.IGNORE_CASE)

        fun parse(s: String, value: ByoYomi?): ByoYomi? {
            val m = REGEX.matchEntire(s) ?: return null
            val periods: Int
            val time: Long
            try {
                periods = m.groupValues[GROUP_PERIODS].toInt()
                time = m.groupValues[GROUP_TIME].secondsToMillis()
            } catch (e: NumberFormatException) {
                return null
            }
            return value?.apply {
                this.periods = periods
                this.millis = time
            } ?: ByoYomi(periods, time)
        }

    }

}
