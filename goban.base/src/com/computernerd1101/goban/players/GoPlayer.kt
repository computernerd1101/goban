package com.computernerd1101.goban.players

import com.computernerd1101.goban.*
import com.computernerd1101.goban.sgf.GoSGFNode
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlin.coroutines.CoroutineContext

abstract class GoPlayer(val manager: GoPlayerManager, val color: GoColor) {

    fun interface Factory {

        fun createPlayer(manager: GoPlayerManager, color: GoColor): GoPlayer

    }

    @Suppress("unused")
    val opponent: GoPlayer
        get() = manager.getPlayer(color.opponent)

    abstract suspend fun generateHandicapStones(handicap: Int, goban: Goban)

    abstract suspend fun requestMove(channel: SendChannel<GoPoint?>)

    open suspend fun update() = Unit

    open suspend fun acceptHandicapStones(goban: Goban): Boolean = true

    open suspend fun acceptOpponentMove(move: GoPoint?): Boolean = true

    open suspend fun acceptOpponentTimeExtension(millis: Long): Boolean = false

    open suspend fun acceptUndoMove(resumeNode: GoSGFNode): Boolean = false

    open suspend fun startScoring(scoreManager: GoScoreManager) {
        scoreManager.finishVerdict.send(color)
    }

    open suspend fun updateScoring(scoreManager: GoScoreManager, stones: GoPointSet, alive: Boolean) {
        scoreManager.finishVerdict.send(color)
    }

    open suspend fun finishScoring() = Unit

}