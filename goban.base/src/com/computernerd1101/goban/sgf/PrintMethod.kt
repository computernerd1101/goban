package com.computernerd1101.goban.sgf

import com.computernerd1101.goban.resources.GobanResources
import java.util.*

enum class PrintMethod {

    DONT_PRINT,
    PRINT_ALL,
    PRINT_MOD100;

    override fun toString() = toString(Locale.getDefault())

    fun toString(locale: Locale): String {
        val resources = GobanResources.getBundle(locale)
        return resources.getStringArray("sgf.PrintMethod")[ordinal]
    }

    companion object {

        @JvmStatic
        fun parseOrdinal(s: String): PrintMethod? {
            var pm = 0
            var hasDigit = false
            for(ch in s) {
                if (ch < '0' || ch > '9') break
                hasDigit = true
                pm = pm*10 + (ch - '0')
                if (pm >= 2) return PRINT_MOD100
            }
            return when {
                !hasDigit -> null
                pm == 0 -> DONT_PRINT
                else -> PRINT_ALL
            }
        }

    }

}