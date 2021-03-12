package com.computernerd1101.goban.players

import com.computernerd1101.goban.*
import com.computernerd1101.goban.internal.InternalMarker
import com.computernerd1101.goban.sgf.GoSGFNode
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.selects.SelectBuilder
import kotlin.coroutines.*

@ExperimentalGoPlayerApi
val CoroutineContext.blackGoPlayer: GoPlayer get() = this[GoPlayer.Black] ?: throw IllegalStateException(
    if (this[GoPlayer.White] == null) "Missing both players" else "Missing black player"
)

@ExperimentalGoPlayerApi
val CoroutineContext.whiteGoPlayer: GoPlayer get() = this[GoPlayer.White] ?: throw IllegalStateException(
    "Missing white player"
)

@ExperimentalGoPlayerApi
abstract class GoPlayer(val color: GoColor): CoroutineContext.Element {

    final override val key: GoColor get() = color

    companion object {

        @JvmField val Black = GoColor.BLACK
        @JvmField val White = GoColor.WHITE

    }

    fun interface Factory {

        fun createPlayer(color: GoColor): GoPlayer

    }

    suspend fun getOpponent(): GoPlayer? = coroutineContext[key.opponent]

    abstract suspend fun generateHandicapStones(handicap: Int, goban: Goban)

    abstract suspend fun generateMove(): GoPoint?

    open suspend fun update() = Unit

    open suspend fun acceptHandicapStones(goban: Goban): Boolean = true

    open suspend fun acceptOpponentMove(move: GoPoint?): Boolean = true

    open suspend fun acceptOpponentTimeExtension(millis: Long): Boolean = false

    open suspend fun acceptUndoMove(resumeNode: GoSGFNode): Boolean = false

    open suspend fun startScoring(scoreManager: GoScoreManager) {
        submitScore(scoreManager)
    }

    open suspend fun updateScoring(scoreManager: GoScoreManager, stones: GoPointSet, alive: Boolean) {
        submitScore(scoreManager)
    }

    open suspend fun finishScoring() = Unit

    suspend fun checkPermissions() {
        if (coroutineContext[key] != this)
            throw SecurityException()
    }

    protected suspend fun submitScore(scoreManager: GoScoreManager) {
        checkPermissions()
        scoreManager.getSubmit(InternalMarker).send(color)
    }

    protected fun <R> SelectBuilder<R>.onSubmitScore(
        scoreManager: GoScoreManager,
        block: suspend (GoScoreManager) -> R
    ) {
        scoreManager.getSubmit(InternalMarker).onSend(color, scoreSelectClause(scoreManager, block))
    }

    protected suspend fun requestResumePlay(scoreManager: GoScoreManager) {
        checkPermissions()
        scoreManager.getResumePlay(InternalMarker).send(color)
    }

    protected fun <R> SelectBuilder<R>.onRequestResumePlay(
        scoreManager: GoScoreManager,
        block: suspend (GoScoreManager) -> R
    ) {
        scoreManager.getResumePlay(InternalMarker).onSend(color, scoreSelectClause(scoreManager, block))
    }

    private fun <R> scoreSelectClause(
        scoreManager: GoScoreManager,
        block: suspend (GoScoreManager) -> R
    ): suspend (SendChannel<GoColor>) -> R = {
        checkPermissions()
        block(scoreManager)
    }

}