package com.computernerd1101.goban.resources

interface OvertimeFormatter {

    fun formatByoYomi(periodsRemaining: Int): String

    fun formatCanadian(move: Int, max: Int): String

    companion object Default: OvertimeFormatter {

        override fun formatByoYomi(periodsRemaining: Int): String =
            "$periodsRemaining periods remaining"

        override fun formatCanadian(move: Int, max: Int): String =
            "$move/$max moves"

    }

}