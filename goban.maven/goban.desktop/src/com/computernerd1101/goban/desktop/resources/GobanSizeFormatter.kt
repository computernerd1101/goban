package com.computernerd1101.goban.desktop.resources

fun interface GobanSizeFormatter {

    fun format(width: Int, height: Int): String

    companion object {

        @JvmField val SHORT = Default.SHORT
        @JvmField val LONG = Default.LONG

    }

    enum class Default: GobanSizeFormatter {
        SHORT {
            override fun format(width: Int, height: Int) = "${width}x$height"
        },
        LONG {
            override fun format(width: Int, height: Int) = "Size: ${width}x$height"
        }
    }

}