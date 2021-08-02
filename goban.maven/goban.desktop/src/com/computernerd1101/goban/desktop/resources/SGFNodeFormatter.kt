package com.computernerd1101.goban.desktop.resources

fun interface SGFNodeFormatter {

    fun format(index: Int, move: String?, hotspot: Int, gameInfo: Boolean): String

    companion object Default: SGFNodeFormatter {

        override fun format(index: Int, move: String?, hotspot: Int, gameInfo: Boolean) = buildString {
            if (index == 0) append("SGF")
            else {
                append(index)
                if (move != null) append(": ").append(move)
            }
            val extra = when(hotspot) {
                1 -> {
                    append(" (Hotspot")
                    true
                }
                2 -> {
                    append(" (Major hotspot")
                    true
                }
                else -> false
            }
            if (gameInfo)
                append(if (extra) ", GameInfo)" else " (GameInfo)")
            else if (extra) append(")")
        }

    }

}