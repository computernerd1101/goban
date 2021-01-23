package com.computernerd1101.goban.desktop.resources

fun interface GobanSizeFormatter {
    fun format(width: Int, height: Int): String

    companion object {

        @JvmField val SHORT: GobanSizeFormatter = Default.SHORT
        @JvmField val LONG: GobanSizeFormatter = Default.LONG

    }

    private enum class Default: GobanSizeFormatter {
        SHORT {
            override fun format(width: Int, height: Int) = "${width}x$height"
        },
        LONG {
            override fun format(width: Int, height: Int) = "Size: ${width}x$height"
        }
    }

}