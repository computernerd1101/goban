package com.computernerd1101.goban.desktop.resources

fun interface SGFNodeFormatter {

    fun format(index: Int, move: String?, hotspot: Int, gameInfo: Boolean): String

    companion object Default: SGFNodeFormatter {

        override fun format(index: Int, move: String?, hotspot: Int, gameInfo: Boolean): String {
            val text = when {
                index == 0 -> "SGF"
                move != null -> "$index: $move"
                else -> index.toString()
            }
            var hotspotString = when(hotspot) {
                1 -> " (Hotspot"
                2 -> " (Major hotspot"
                else -> ""
            }
            var separator = ""
            val gameInfoString = when {
                gameInfo -> {
                    if (hotspotString.isEmpty()) hotspotString = " ("
                    else separator = ", "
                    "Game Info)"
                }
                hotspotString.isNotEmpty() -> ")"
                else -> ""
            }
            return "$text$hotspotString$separator$gameInfoString"
        }

    }

}