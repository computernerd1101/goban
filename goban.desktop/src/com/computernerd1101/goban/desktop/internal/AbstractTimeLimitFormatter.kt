package com.computernerd1101.goban.desktop.internal

import com.computernerd1101.goban.desktop.CN13Spinner
import com.computernerd1101.goban.sgf.GameInfo
import com.computernerd1101.goban.time.Milliseconds
import com.computernerd1101.goban.time.millisToStringSeconds
import com.computernerd1101.goban.time.toMilliseconds
import java.text.NumberFormat
import java.text.ParseException

abstract class AbstractTimeLimitFormatter: CN13Spinner.Formatter(NumberFormat.getInstance()) {

    var millis: Milliseconds = Milliseconds.ZERO

    init {
        commitsOnValidEdit = true
        allowsInvalid = false
    }

    abstract val gameInfo: GameInfo?

    override fun stringToValue(text: String?): Any? {
        return if (text.isNullOrEmpty()) Milliseconds.ZERO
        else try {
            val m = Milliseconds.parse(text)
            if (m.toLong() < 0) throw ParseException(text, 0)
            m
        } catch(e: NumberFormatException) {
            throw ParseException(text, 0)
        }
    }

    override fun valueToString(value: Any?): String {
        return when(value) {
            is Milliseconds -> if (value.toLong() == 0L) "" else value.toString()
            is Number -> value.toLong().let {
                if (it == 0L) "" else it.millisToStringSeconds()
            }
            else -> ""
        }
    }

    override fun getValue(): Milliseconds? {
        val time = gameInfo?.timeLimit ?: return null
        var m = millis
        if (m.toLong() != time) {
            m = time.toMilliseconds()
            millis = m
        }
        return m
    }

    override fun setValue(value: Any?) {
        val info = gameInfo ?: return
        val time: Long
        when(value) {
            is Milliseconds -> {
                time = value.toLong()
                if (time < 0) return
                millis = value
            }
            is Number -> {
                time = value.toLong()
                if (time < 0) return
                if (millis.toLong() != time)
                    millis = time.toMilliseconds()
            }
            else -> return
        }
        if (info.timeLimit != time) {
            info.timeLimit = time
            fireChangeEvent()
        }
    }

    override fun getNextValue(): Any? {
        val info = gameInfo ?: return null
        val time = info.timeLimit
        return if (time <= Long.MAX_VALUE - 1000L)
            (time + 1000L).toMilliseconds()
        else null
    }

    override fun getPreviousValue(): Any? {
        val info = gameInfo ?: return null
        val time = info.timeLimit
        return if (time >= 1000L)
            (time - 1000L).toMilliseconds()
        else null
    }

}