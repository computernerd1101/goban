@file:Suppress("unused", "ClassName")

package com.computernerd1101.goban.resources

import java.util.*

fun gobanResources(locale: Locale): ResourceBundle {
    return ResourceBundle.getBundle(
        "com.computernerd1101.goban.resources.GobanResources",
        locale
    )
}

class GobanResources: ListResourceBundle() {

    override fun getContents(): Array<out Array<out Any>> {
        return arrayOf(
            arrayOf(
                "GoColor",
                arrayOf("Black", "White")
            ),
            arrayOf("time.Overtime.ByoYomi", "Byo-Yomi"),
            arrayOf("time.Overtime.Canadian", "Canadian"),
            arrayOf(
                "sgf.PrintMethod",
                arrayOf(
                    "Don't print move numbers",
                    "Print move numbers",
                    "Print move numbers modulo 100"
                )
            ),
            arrayOf(
                "sgf.PositionState.UC",
                arrayOf("Unclear position", "Very unclear position")
            ),
            arrayOf(
                "sgf.PositionState.DM",
                arrayOf("Even position", "Joseki")
            ),
            arrayOf(
                "sgf.PositionState.GB",
                arrayOf("Good for black", "Very good for black")
            ),
            arrayOf(
                "sgf.PositionState.GW",
                arrayOf("Good for white", "Very good for white")
            ),
            arrayOf("sgf.MoveAnnotation.IT", "Interesting"),
            arrayOf(
                "sgf.MoveAnnotation.TE",
                arrayOf("Good move (Tesuji)", "Very good move (Tesuji)")
            ),
            arrayOf("sgf.MoveAnnotation.DO", "Doubtful"),
            arrayOf(
                "sgf.MoveAnnotation.BM",
                arrayOf("Bad move", "Very bad move")
            )
        )
    }

}

class GobanResources_ja: ListResourceBundle() {

    override fun getContents(): Array<out Array<out Any>> {
        return arrayOf(
            arrayOf(
                "GoColor",
                arrayOf("\u9ed2", "\u767d")
            ),
            arrayOf("time.Overtime.ByoYomi", "\u79d2\u8aad\u307f"),
            arrayOf("time.Overtime.Canadian", "\u30ab\u30ca\u30c0")
        )
    }

}