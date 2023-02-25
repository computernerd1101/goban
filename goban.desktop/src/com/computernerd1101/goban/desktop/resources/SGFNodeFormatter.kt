package com.computernerd1101.goban.desktop.resources

import com.computernerd1101.goban.sgf.MoveAnnotation

fun interface SGFNodeFormatter {

    fun format(index: Int, move: String?, annotation: MoveAnnotation?, hotspot: Int, gameInfo: Boolean): String

    companion object Default : SGFNodeFormatter {

        fun formatMoveAnnotation(annotation: MoveAnnotation): String = when(annotation) {
            MoveAnnotation.INTERESTING -> "!?"
            MoveAnnotation.GOOD -> "!"
            MoveAnnotation.VERY_GOOD -> "!!"
            MoveAnnotation.DOUBTFUL -> "?!"
            MoveAnnotation.BAD -> "?"
            MoveAnnotation.VERY_BAD -> "??"
        }

        override fun format(
            index: Int, move: String?, annotation: MoveAnnotation?,
            hotspot: Int, gameInfo: Boolean
        ) = buildString {
            if (index == 0) append("SGF")
            else {
                append(index)
                if (move != null) {
                    append(": ").append(move)
                    if (annotation != null) append(formatMoveAnnotation(annotation))
                }
            }
            val extra = when (hotspot) {
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