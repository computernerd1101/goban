package com.computernerd1101.goban.sgf

enum class PrintMethod(private val string: String) {

    DONT_PRINT("Don't print move numbers"),
    PRINT_ALL("Print move numbers"),
    PRINT_MOD100("Print move numbers modulo 100");

    override fun toString() = string

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