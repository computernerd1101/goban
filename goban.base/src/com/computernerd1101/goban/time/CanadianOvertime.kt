package com.computernerd1101.goban.time

import com.computernerd1101.goban.properties.*
import com.computernerd1101.goban.resources.*
import java.util.*

@PropertyOrder("Seconds", "Moves")
class CanadianOvertime(millis: Long, moves: Int): Overtime(), PropertyTranslator {

    operator fun component1() = millis
    operator fun component2() = moves

    @TimeProperty(name="Seconds", min=1L)
    var millis: Long = if (millis > 0L) millis else throw IllegalArgumentException("millis")
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

    override fun translateProperty(name: String, locale: Locale): String? {
        return if (name == "Seconds" || name == "Moves") {
            val resources = gobanResources(locale)
            resources.getString("PropertyTranslator.Overtime.$name")
        } else name
    }

    override val initialOvertimeCode: Int get() = moves

    override fun filterEvent(e: TimeEvent): TimeEvent {
        var time = e.timeRemaining
        var overtime = e.overtimeCode
        var flags = e.flags
        if (time <= 0L) {
            flags = flags or (if (flags and TimeEvent.FLAG_OVERTIME == 0) {
                time += millis
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
        return TimeEvent(e.source ?: this, time, overtime, flags)
    }

    override fun extendTime(e: TimeEvent, extension: Long): TimeEvent {
        if (extension <= 0L) return e
        var timeRemaining = e.timeRemaining
        if (timeRemaining + extension < timeRemaining)
            timeRemaining = Long.MAX_VALUE
        else timeRemaining += extension
        val flags = e.flags
        var overtime = e.overtimeCode
        if (flags and (TimeEvent.FLAG_OVERTIME or TimeEvent.FLAG_TICKING) == TimeEvent.FLAG_OVERTIME)
            overtime++ // TODO will be immediately canceled out by filterEvent? Test to make sure.
        return TimeEvent(e.source, timeRemaining, overtime, flags)
    }

    override fun toString() = "${millis.millisToStringSeconds()}/$moves Canadian"

    override fun getTypeString(): String = "Canadian"

    override fun getDisplayName(locale: Locale): String? {
        val resources = gobanResources(locale)
        return resources.getString("time.Overtime.Canadian")
    }

    override fun displayOvertimeImpl(e: TimeEvent, locale: Locale): String? =
        (gobanFormatResources(locale).getObject("CanadianOvertimeFormatter") as? OvertimeFormatter2)
            ?.format(e.overtimeCode, moves)

    override fun parseThis(s: String): Boolean = Regex.parse(s, this) != null

    companion object {

        @JvmStatic
        fun parse(s: String): CanadianOvertime? = Regex.parse(s, null)

    }

    private object Regex {

        const val GROUP_TIME = 1
        const val GROUP_MOVES = 2

        @JvmField
        val REGEX = """^\s*(0*\.0*[1-9]\d*|0*[1-9]\d*\.?\d*)\s*/\s*(0*[1-9]\d*)\s*canadian\s*$"""
            .toRegex(RegexOption.IGNORE_CASE)

        fun parse(s: String, value: CanadianOvertime?): CanadianOvertime? {
            val m = REGEX.matchEntire(s) ?: return null
            val time: Long = m.groupValues[GROUP_TIME].secondsToMillisOrNull() ?: return null
            val moves: Int = m.groupValues[GROUP_MOVES].toIntOrNull() ?: return null
            return value?.apply {
                this.millis = time
                this.moves = moves
            } ?: CanadianOvertime(time, moves)
        }

    }

}