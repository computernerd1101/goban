package com.computernerd1101.goban.time

import com.computernerd1101.goban.GoColor
import com.computernerd1101.goban.sgf.*
import java.util.*
import java.util.regex.*
import kotlin.concurrent.scheduleAtFixedRate

fun Long.millisToStringSeconds(): String = TimeLimit.millisToStringSeconds(this)
fun String.secondsToMillis(): Long = TimeLimit.parseSeconds(this)

class TimeLimit private constructor(
    events: Events,
    remainingTime: Long,
    overtimeCode: Int,
    flags: Int,
    val overtime: Overtime?,
    owner: Any?
) {

    constructor(mainTime: Long, overtime: Overtime?, owner: Any? = null): this(
        Events, mainTime, overtime?.initialOvertimeCode ?: 0,
        0, overtime, owner
    )

    @Suppress("SpellCheckingInspection")
    companion object {

        @JvmStatic @JvmOverloads fun fromSGF(
            player: GoColor,
            node: GoSGFNode,
            owner: Any? = null,
            mainTime: Long = -1L,
            overtime: Overtime? = Events
        ): TimeLimit? {
            val tree = node.treeOrNull ?: return null
            return synchronized(tree) {
                if (!node.isAlive) return@synchronized null
                val gameInfo = node.gameInfo
                var remainingTime = mainTime
                if (remainingTime < 0L) remainingTime = gameInfo?.timeLimit ?: 0L
                val realOvertime = if (overtime === Events) gameInfo?.overtime else overtime
                if (remainingTime == 0L && realOvertime == null) return@synchronized null
                var overtimeCode = realOvertime?.initialOvertimeCode ?: 0
                var flags = 0
                var foundTime = false
                var foundOvertime = false
                var currentNode: GoSGFNode? = node
                while (currentNode != null && !(foundTime && foundOvertime)) {
                    val moveNode = currentNode as? GoSGFMoveNode
                    currentNode = currentNode.parent
                    val playerTime = (moveNode ?: continue).time(player)
                    val hasTime = playerTime.hasTime
                    if (!foundOvertime && playerTime.hasOvertime) {
                        foundOvertime = true
                        overtimeCode = playerTime.overtime
                        if (!foundTime) flags = TimeEvent.FLAG_OVERTIME
                    }
                    if (!foundTime && hasTime) {
                        foundTime = true
                        remainingTime = playerTime.time
                    }
                }
                TimeLimit(Events, remainingTime, overtimeCode, flags, realOvertime, owner)
            }
        }

        @JvmStatic fun extendTime(e: TimeEvent, extension: Long): TimeEvent {
            if (extension <= 0L) return e
            var timeRemaining = e.timeRemaining
            if (timeRemaining + extension < timeRemaining)
                timeRemaining = Long.MAX_VALUE
            else timeRemaining += extension
            return TimeEvent(e.source, timeRemaining, e.overtimeCode, e.flags)
        }

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
            val m = REGEX.find(s) ?: throw NumberFormatException("For input string: \"$s\"")
            var s1: String? = m.groups[GROUP_INT]?.value
            val s2: String?
            if (s1 != null) {
                s2 = null
            } else {
                s1 = m.groupValues[GROUP_IPART]
                s2 = m.groupValues[GROUP_FPART]
            }
            var pos = s1.length
            val buf = s1.toCharArray(CharArray(pos + 3))
            s2?.toCharArray(buf, pos)
            if (s2 != null) {
                s2.toCharArray(buf, pos)
                pos += s2.length
            }
            while(pos < buf.size) buf[pos++] = '0'
            return try {
                buf.concatToString().toLong()
            } catch (e: NumberFormatException) {
                throw NumberFormatException("For input string: \"$s\"")
            }
        }

        const val GROUP_IPART = 1
        const val GROUP_FPART = 2
        const val GROUP_INT = 3
        @JvmField val PATTERN: Pattern = Pattern.compile(
            """([+\-]?\d*)\.(\d{1,3})|([+\-]?\d+)\.?"""
        )
        @JvmField val REGEX = PATTERN.toRegex()

        private val timer = Timer(TimeLimit::class.java.name, true)

    }

    private var task: TimerTask? = null

    private fun Events.cancelTask() {
        task = nullTask
    }

    private var prevSystemTime: Long = 0L

    private fun Events.updateSystemTime() {
        prevSystemTime = systemTime()
    }

    private val timeListeners = mutableListOf<TimeListener>()

    private var _timeEvent = TimeEvent(owner ?: this, remainingTime, overtimeCode, flags)

    val timeEvent: TimeEvent @Synchronized get() = _timeEvent

    init {
        events.filterInitialEvent()
    }

    private fun Events.filterInitialEvent() {
        // In case overtime?.filterEvent(timeEvent) accesses this
        // through timeEvent.getSource(),
        // this.timeEvent will already be non-null.
        _timeEvent = getFilteredEvent(_timeEvent)
    }

    fun addTimeListener(l: TimeListener?) {
        if (l != null) synchronized(this) { timeListeners.add(l) }
    }

    @Suppress("unused")
    fun removeTimeListener(l: TimeListener?) {
        if (l != null) synchronized(this) { timeListeners.remove(l) }
    }

    var isTicking: Boolean
        get() = timeEvent.isTicking
        @Synchronized
        set(ticking) {
            val e = _timeEvent
            when {
                ticking -> if (e.flags and TimeEvent.FLAGS_EXPIRED_TICKING == 0) {
                    prevSystemTime = System.currentTimeMillis()
                    var time = e.timeRemaining
                    Events.updateTimeEvent(
                        TimeEvent(
                            e.source ?: this,
                            time,
                            e.overtimeCode,
                            e.flags or TimeEvent.FLAG_TICKING
                        ), filter=false
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
                            Events.updateSystemTime()
                            event = Events.updateTimeEvent(
                                TimeEvent(
                                    event.source ?: this@TimeLimit,
                                    timeRemaining - r,
                                    event.overtimeCode,
                                    event.flags or TimeEvent.FLAG_TICKING
                                ), filter=true
                            )
                            if (event.isExpired) {
                                cancel()
                                Events.cancelTask()
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
                    Events.updateTimeEvent(
                        TimeEvent(
                            e.source ?: this,
                            e.timeRemaining - diff,
                            e.overtimeCode,
                            e.flags and (TimeEvent.FLAG_TICKING xor TimeEvent.FLAG_MASK)
                        ), filter=true
                    )
                }
            }
        }

    fun extendTime(extension: Long) {
        if (extension <= 0L) return
        synchronized(this) {
            val overtime = this.overtime
            var e = _timeEvent
            e = if (overtime != null) {
                val e2 = overtime.extendTime(e, extension)
                val source = e.source
                if (source == null || source === e2.source) e2
                else TimeEvent(source, e2.timeRemaining, e2.overtimeCode, e2.flags)
            } else Companion.extendTime(e, extension)
            Events.updateTimeEvent(e, true)
        }
    }

    // already synchronized in all callers
    private fun Events.updateTimeEvent(e: TimeEvent, filter: Boolean): TimeEvent {
        // even synthetic public method access$updateTimeEvent,
        // which is called by a TimerTask lambda,
        // cannot be called without access to receiver of private type Events.
        val event = if (filter) getFilteredEvent(e) else e
        // The owner of this method is the same as that of the private property _timeEvent,
        // so a public synthetic setter will not be generated for that property.
        _timeEvent = event
        for(i in (timeListeners.size - 1) downTo 0)
            timeListeners[i].timeElapsed(event)
        return event
    }

    private object Events: Overtime() {

        override fun parseThis(s: String): Boolean = false

        // public member of private nested class prevents
        // synthetic public static method access$getFilteredEvent from being generated
        fun TimeLimit.getFilteredEvent(e: TimeEvent): TimeEvent {
            val e2 = overtime?.filterEvent(e) ?: e
            val source1: Any? = e.source
            val source2: Any? = e2.source
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
                e !== e2 && (source1 == null || source1 === source2) &&
                        timeRemaining == e2.timeRemaining &&
                        overtimeCode == e2.overtimeCode &&
                        flags == e2.flags -> e2
                else -> TimeEvent(source1 ?: source2 ?: this, timeRemaining, overtimeCode, flags)
            }
        }

        fun systemTime(): Long = System.currentTimeMillis()

        val nullTask: TimerTask? get() = null

    }

}