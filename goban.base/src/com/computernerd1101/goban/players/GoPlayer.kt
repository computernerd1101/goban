package com.computernerd1101.goban.players

import com.computernerd1101.goban.*
import com.computernerd1101.goban.internal.InternalMarker
import com.computernerd1101.goban.sgf.GoSGFNode
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.selects.SelectBuilder

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
        sendFinishScoring(scoreManager)
    }

    open suspend fun updateScoring(scoreManager: GoScoreManager, stones: GoPointSet, alive: Boolean) {
        sendFinishScoring(scoreManager)
    }

    open suspend fun finishScoring() = Unit

    fun checkPermissions() {
        if (manager.getPlayer(color) != this)
            throw SecurityException()
    }

    protected suspend fun sendFinishScoring(scoreManager: GoScoreManager) {
        checkPermissions()
        scoreManager.getFinish(InternalMarker).send(color)
    }

    protected fun <R> SelectBuilder<R>.onSendFinishScoring(
        scoreManager: GoScoreManager,
        block: suspend (GoScoreManager) -> R
    ) {
        checkPermissions()
        scoreManager.getFinish(InternalMarker).onSend(color) {
            block(scoreManager)
        }
    }

    protected suspend fun requestResumePlay(scoreManager: GoScoreManager) {
        checkPermissions()
        scoreManager.getResumePlay(InternalMarker).send(color)
    }

    protected fun <R> SelectBuilder<R>.onRequestResumePlay(
        scoreManager: GoScoreManager,
        block: suspend (GoScoreManager) -> R
    ) {
        checkPermissions()
        scoreManager.getResumePlay(InternalMarker).onSend(color) {
            block(scoreManager)
        }
    }

}