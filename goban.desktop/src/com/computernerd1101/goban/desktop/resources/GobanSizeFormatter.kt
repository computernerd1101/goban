package com.computernerd1101.goban.desktop.resources

fun interface GobanSizeFormatter {
    fun format(width: Int, height: Int): String

    companion object Default: GobanSizeFormatter {
        override fun format(width: Int, height: Int) = "Size: ${width}x$height"
    }

}