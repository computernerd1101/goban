package com.computernerd1101.goban.resources

fun interface OvertimeFormatter1 {

    fun format(p1: Int): String

}

fun interface OvertimeFormatter2 {

    fun format(p1: Int, p2: Int): String

}

object ByoYomiFormatter: OvertimeFormatter1 {

    override fun format(p1: Int): String = "$p1 periods remaining"

}

object CanadianOvertimeFormatter: OvertimeFormatter2 {

    override fun format(p1: Int, p2: Int): String =
        "$p1/$p2 moves"


}
