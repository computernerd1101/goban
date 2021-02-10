package com.computernerd1101.goban.desktop.resources

interface GobanHandicapProgressFormatter {

    fun format(targetHandicap: Int, currentHandicap: Int): String

    companion object Default: GobanHandicapProgressFormatter {

        override fun format(targetHandicap: Int, currentHandicap: Int): String =
            "$currentHandicap/$targetHandicap handicap stones"

    }

}