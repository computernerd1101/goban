package com.computernerd1101.goban.sgf.internal

import com.computernerd1101.goban.sgf.Date

object InternalDate {

    interface Constructor {
        operator fun invoke(value: Int): Date
    }
    lateinit var init: Constructor

    fun parse(prev: Date?, s: String): Date? {
        if (s.isEmpty()) return null
        val split = s.split('-').toTypedArray()
        val negativeYear = split[0].isEmpty()
        var off = 0
        if (negativeYear) {
            off = 1
            if (split.size == 1) return null
        }
        var y = 0
        var m = 0
        var d = 0
        if (prev != null) {
            y = prev.year
            m = prev.month
            d = prev.day
        }
        var i = 0
        var j: Int
        var err = false
        try {
            i = split[off].toInt()
        } catch (e: NumberFormatException) {
            err = true
        }
        if (negativeYear) {
            i = -i
            if (i < Date.MIN_YEAR) return null
        } else if (i > Date.MAX_YEAR) return null
        run {
            when (split.size - off) {
                1 -> {
                    if (err) return null
                    if (i > 0 && split[off].length <= 2) {
                        if (d != 0) {
                            if (i <= Date.daysInMonth(m)) {
                                d = i
                                return@run
                            }
                        } else if (m != 0 && i <= 12) {
                            m = i
                            d = 0
                            return@run
                        }
                    }
                    y = i
                    m = 0
                    d = 0
                }
                2 -> {
                    try {
                        j = split[off + 1].toInt()
                    } catch (e: NumberFormatException) {
                        if (err) return null
                        y = i
                        m = 0
                        d = 0
                        return@run
                    }
                    if (!err) {
                        if (split[off].length <= 2 && m != 0 && i > 0 && i <= 12 && j <= Date.daysInMonth(i)) {
                            m = i
                            d = j
                            return@run
                        }
                        y = i
                    }
                    if (j < 0 || j > 12) j = 0
                    m = j
                    d = 0
                }
                else -> {
                    if (!err) y = i
                    try {
                        m = split[off + 1].toInt()
                    } catch (e: NumberFormatException) {
                        m = 0
                        d = 0
                        return@run
                    }
                    if (m <= 0 || m > 12) {
                        m = 0
                        d = 0
                        return@run
                    }
                    d = try {
                        split[off + 2].toInt()
                    } catch (e: NumberFormatException) {
                        0
                    }
                    if (d < 0 || d > Date.daysInMonth(m)) d = 0
                }
            }
        }
        return Date(y, m, d)
    }

}