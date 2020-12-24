package com.computernerd1101.goban.time

import com.computernerd1101.goban.internal.InternalMarker
import java.util.*
import java.util.regex.*
import kotlin.concurrent.scheduleAtFixedRate

@Suppress("NOTHING_TO_INLINE")
inline fun Long.millisToStringSeconds(): String = TimeLimit.millisToStringSeconds(this)
@Suppress("NOTHING_TO_INLINE")
inline fun String.secondsToMillis(): Long = TimeLimit.parseSeconds(this)

class TimeLimit(mainTime: Long, val overtime: Overtime?) {

    @Suppress("SpellCheckingInspection")
    companion object {

        @JvmStatic
        fun millisToStringSeconds(millis: Long): String {
            var sec = millis / 1000L
            var r = (millis - sec * 1000L).toInt()
            if (r == 0) return sec.toString()
            val sign = if (millis < 0) {
                sec = -sec
                r = -r
                "-"
            } else ""
            var q = r / 10
            val pad: String = when {
                q * 10 == r -> { // hundredths of a second
                    r = q
                    q = r / 10
                    when {
                        q * 10 == r -> { // tenths of a second
                            r = q
                            ""
                        }
                        r < 10 -> "0"
                        else -> ""
                    }
                }
                r < 10 -> "00"
                r < 100 -> "0"
                else -> ""
            }
            return "$sign$sec.$pad$r"
        }

        @JvmStatic
        fun parseSeconds(s: String): Long {
            val m: Matcher = PATTERN.matcher(s)
            if (!m.find()) throw NumberFormatException("For input string: \"$s\"")
            var s1: String? = m.group(GROUP_INT)
            val s2: String
            val s3: String
            if (s1 != null) {
                s2 = ""
                s3 = "000" // 1 second = 1000 milliseconds
            } else {
                s1 = m.group(GROUP_IPART)
                s2 = m.group(GROUP_FPART)
                s3 = when (s2.length) {
                    1 -> "00"
                    2 -> "0"
                    else -> ""
                }
            }
            return try {
                (s1 + s2 + s3).toLong()
            } catch (e: NumberFormatException) {
                throw NumberFormatException("For input string: \"$s\"")
            }
        }

        const val GROUP_IPART = 1
        const val GROUP_FPART = 2
        const val GROUP_INT = 3
        @JvmField val PATTERN: Pattern = Pattern.compile(
            "([+\\-]?\\d*)\\.(\\d{1,3})|([+\\-]?\\d+)\\.?"
        )

        private val timer = Timer(TimeLimit::class.java.name, true)

    }

    private var task: TimerTask? = null

    private var prevSystemTime: Long = 0L

    private val timeListeners = mutableListOf<TimeListener>()

    @get:Synchronized
    var timeEvent = filterEvent(TimeEvent(this, mainTime)); private set

    // already synchronized in all callers
    private fun updateTimeEvent(e: TimeEvent, marker: InternalMarker) {
        marker.ignore()
        timeEvent = e
        for(i in (timeListeners.size - 1) downTo 0)
            timeListeners[i].timeElapsed(e)
    }

    @Suppress("unused")
    inline fun addTimeListener(crossinline l: (TimeEvent) -> Unit): TimeListener {
        val listener = TimeListener { e -> l(e) }
        addTimeListener(listener)
        return listener
    }

    fun addTimeListener(l: TimeListener?) {
        if (l != null) synchronized(this) { timeListeners.add(l) }
    }

    @Suppress("unused")
    fun removeTimeListener(l: TimeListener?) {
        if (l != null) synchronized(this) { timeListeners.remove(l) }
    }

    var isTicking: Boolean
        @Synchronized
        get() = timeEvent.isTicking
        @Synchronized
        set(ticking) {
            val e = timeEvent
            when {
                ticking -> if (e.flags and TimeEvent.FLAGS_EXPIRED_TICKING == 0) {
                    prevSystemTime = System.currentTimeMillis()
                    var time = e.timeRemaining
                    updateTimeEvent(
                        TimeEvent(
                            this,
                            time,
                            e.overtimeCode,
                            e.flags or TimeEvent.FLAG_TICKING
                        ), InternalMarker
                    )
                    time %= 1000L
                    if (time <= 0) time += 1000L
                    task = timer.scheduleAtFixedRate(time, 1000L) {
                        synchronized(this@TimeLimit) {
                            var event = timeEvent
                            val timeRemaining = event.timeRemaining
                            var r = timeRemaining % 1000L
                            if (r <= 0L)
                                r += 1000L
                            prevSystemTime = System.currentTimeMillis()
                            event = filterEvent(
                                TimeEvent(
                                    this@TimeLimit,
                                    timeRemaining - r,
                                    event.overtimeCode,
                                    event.flags or TimeEvent.FLAG_TICKING
                                )
                            )
                            updateTimeEvent(event, InternalMarker)
                            if (event.isExpired) {
                                cancel()
                                task = null
                            }
                        }
                    }
                }
                e.isTicking -> {
                    task?.cancel()
                    task = null
                    val now = System.currentTimeMillis()
                    val diff = now - prevSystemTime
                    prevSystemTime = now
                    updateTimeEvent(
                        filterEvent(
                            TimeEvent(
                                this,
                                e.timeRemaining - diff,
                                e.overtimeCode,
                                e.flags and (TimeEvent.FLAG_TICKING xor TimeEvent.FLAG_MASK)
                            )
                        ), InternalMarker
                    )
                }
            }
        }

    private fun filterEvent(e: TimeEvent): TimeEvent {
        val e2 = overtime?.filterEvent(e) ?: e
        val timeRemaining = e2.timeRemaining
        val overtimeCode = e2.overtimeCode
        var flags = e2.flags
        when {
            flags and (TimeEvent.FLAG_EXPIRED or TimeEvent.FLAG_OVERTIME) == 0 && timeRemaining <= 0L ->
                flags = (flags and (TimeEvent.FLAG_MASK xor TimeEvent.FLAG_TICKING)) or TimeEvent.FLAG_EXPIRED
            !e.isTicking -> flags = flags and (TimeEvent.FLAG_MASK xor TimeEvent.FLAG_TICKING)
            flags and TimeEvent.FLAG_EXPIRED == 0 -> flags = flags or TimeEvent.FLAG_TICKING
        }
        return when {
            timeRemaining == e.timeRemaining &&
                    overtimeCode == e.overtimeCode &&
                    flags == e.flags -> e
            e !== e2 && e2.timeLimit == this &&
                    timeRemaining == e2.timeRemaining &&
                    overtimeCode == e2.overtimeCode &&
                    flags == e2.flags -> e2
            else -> TimeEvent(this, timeRemaining, overtimeCode, flags)
        }
    }

}