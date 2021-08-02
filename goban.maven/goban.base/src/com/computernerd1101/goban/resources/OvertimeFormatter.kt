package com.computernerd1101.goban.resources

internal fun interface OvertimeFormatter1 {

    fun format(p1: Int): String

}

internal fun interface OvertimeFormatter2 {

    fun format(p1: Int, p2: Int): String

}

internal object ByoYomiFormatter: OvertimeFormatter1 {

    override fun format(p1: Int): String =
        if (p1 == 1) "1 period remaining" else "$p1 periods remaining"

}

internal object CanadianOvertimeFormatter: OvertimeFormatter2 {

    override fun format(p1: Int, p2: Int): String =
        if (p1 == 1 && p2 == 1) "1/1 move" else "$p1/$p2 moves"

}
