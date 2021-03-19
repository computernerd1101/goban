package com.computernerd1101.goban.players

import com.computernerd1101.goban.*
import com.computernerd1101.goban.internal.InternalMarker
import com.computernerd1101.goban.sgf.GoSGFNode
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.selects.SelectBuilder
import kotlin.coroutines.*

@ExperimentalGoPlayerApi
abstract class GoPlayer(val color: GoColor) {

    fun interface Factory {

        fun createPlayer(color: GoColor): GoPlayer

        suspend fun isCompatible(setup: GoGameSetup) = true

    }

    suspend fun getOpponent(): GoPlayer = coroutineContext.goGameContext.getPlayer(color.opponent)

    abstract suspend fun generateHandicapStones(handicap: Int, goban: Goban)

    abstract suspend fun generateMove(): GoPoint?

    open suspend fun update() = Unit

    open suspend fun acceptHandicapStones(goban: Goban): Boolean = true

    open suspend fun acceptOpponentMove(move: GoPoint?): Boolean = true

    open suspend fun acceptOpponentTimeExtension(requestedMilliseconds: Long): Long = requestedMilliseconds

    protected suspend fun requestUndoMove(resumeNode: GoSGFNode): Boolean =
        coroutineContext.goGameContext.requestUndoMove(color, resumeNode, InternalMarker)

    open suspend fun acceptUndoMove(resumeNode: GoSGFNode): Boolean = false

    open suspend fun startScoring(scoreManager: GoScoreManager) {
        submitScore(coroutineContext.goGameContext, scoreManager)
    }

    open suspend fun updateScoring(scoreManager: GoScoreManager, stones: GoPointSet, alive: Boolean) {
        submitScore(coroutineContext.goGameContext, scoreManager)
    }

    open suspend fun finishScoring() = Unit

    suspend fun checkPermissions() {
        checkPermissions(coroutineContext.goGameContext)
    }

    fun checkPermissions(game: GoGameContext) {
        if (game.getPlayer(color) != this) throw SecurityException()
    }

    protected fun submitScore(game: GoGameContext, scoreManager: GoScoreManager) {
        checkPermissions(game)
        scoreManager.submitScore(color, InternalMarker)
    }

    protected fun requestResumePlay(game: GoGameContext, scoreManager: GoScoreManager) {
        checkPermissions(game)
        scoreManager.requestResumePlay(color, InternalMarker)
    }

    final override fun equals(other: Any?): Boolean = this === other

    final override fun hashCode(): Int = super.hashCode()

}