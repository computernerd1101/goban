package com.computernerd1101.goban.desktop.resources

fun interface TimeLimitFormatter {

    fun format(timeRemaining: Long): String

    companion object Default: TimeLimitFormatter {

        override fun format(timeRemaining: Long): String {
            if (timeRemaining <= 0L) return "0:00:00"
            var time = timeRemaining / 1000L
            if (timeRemaining % 1000L != 0L) time++
            val seconds = (time % 60L).toInt()
            time /= 60L
            val minutes = (time % 60L).toInt()
            time /= 60L
            return buildString {
                append(time).append(':')
                if (minutes < 10) append('0')
                append(minutes).append(':')
                if (seconds < 10) append('0')
                append(seconds)
            }
        }

    }

}