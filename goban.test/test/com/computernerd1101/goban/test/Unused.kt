@file:Suppress("unused")

package com.computernerd1101.goban.test

import com.computernerd1101.goban.*
import com.computernerd1101.goban.desktop.GoGameFrame
import com.computernerd1101.goban.markup.*
import com.computernerd1101.goban.players.*
import com.computernerd1101.goban.sgf.GameResult
import com.computernerd1101.sgf.*
import kotlinx.coroutines.Job
import java.util.*

abstract class UnusedPlayer(game: GoGameManager, color: GoColor): GoPlayer(game, color) {

    fun unused() {
    }

}

fun unused() {
    GoGameManager(GoGameFrame).scoreManager
    lineMarkup(0, 0, 1, 1)
    arrowMarkup(0, 0, 1, 1)
    LineMarkupSet().isNullOrEmpty()
    GameResult.forfeit(GoColor.BLACK)
    val goban1: Goban? = Goban(19)
    val goban2: MutableGoban? = MutableGoban(19)
    goban1.isNullOrEmpty()
    goban1 contentEquals goban2
    goban1?.getScoreGoban(goban2)
    GoColor.valueOf(isBlack = null)
    val nullPoint: GoPoint? = null
    "aa".toGoPoint()
    "[]".toGoPointOrNull()
    0.toGoPointChar()
    'a'.toGoPointInt()
    nullPoint.formatOrPass(19, 19, Locale.getDefault())
    nullPoint.formatOrPass(19, 19)
    "A1".gtpParse(19, 19)
    "I1".gtpParseOrNull(19, 19)
    'A'.gtpParseXOrThrow(19)
    'i'.gtpParseX(19)
    MutableGoPointSet(GoPoint(0, 0), GoPoint(1, 1))
    GoRules()
    GoRules(Superko.NATURAL)
    GoRules(Superko.NATURAL, territoryScore = true)
    GoRules(Superko.NATURAL, territoryScore = true, allowSuicide = true)
    val sgfBytes = SGFBytes().insert(0, 32)
        .insert(0, byteArrayOf()).insert(0, byteArrayOf(), 0, 0)
    sgfBytes.insert(0, sgfBytes).insert(0, sgfBytes, 0, 0)
        .replace(0, 1, 40).replace(0, 0, byteArrayOf())
        .replace(0, 0, byteArrayOf(), 0, 0)
        .replace(0, 1, sgfBytes).replace(0, 1, sgfBytes, 0, 1)
        .ensureCapacity(32)
    sgfBytes.trimToSize()
    sgfBytes.toByteArray()
    sgfBytes.toByteArray(0, 1)
    sgfBytes.toByteArray(ByteArray(sgfBytes.size), 0)
    sgfBytes.toByteArray(0, 1, ByteArray(1), 0)
    SGFException(0, 0)
    SGFException()
    SGFException("")
    SGFException(cause = null)
    SGFException(null, null).sortWarnings()
    SGFWarning(0, 0)
    SGFWarning()
    SGFWarning("")
    SGFWarning(cause = null)
    SGFWarning(null, null)
    SGFWarningList().warnings
    SGFTree(listOf(SGFNode(1)))
    SGFTree(listOf(SGFNode(emptyMap())), emptyList())
    GoGameFrame()
    GoGameFrame(Job())
    GoGameFrame(GoGameSetup(), Job())
}

