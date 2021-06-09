package com.computernerd1101.goban.players

import com.computernerd1101.goban.*
import com.computernerd1101.goban.internal.*
import com.computernerd1101.goban.sgf.*
import com.computernerd1101.goban.time.*
import kotlinx.coroutines.*

abstract class GoPlayer(val game: GoGameManager, val color: GoColor) {

    fun interface Factory {

        fun createPlayer(game: GoGameManager, color: GoColor): GoPlayer

        fun isCompatible(setup: GoGameSetup) = true

    }

    val opponent: GoPlayer get() = game.getPlayer(color.opponent)

    abstract suspend fun generateHandicapStones(handicap: Int, goban: Goban)

    internal fun generateMoveAsync(scope: CoroutineScope, marker: InternalMarker): Deferred<GoPoint?> {
        marker.ignore()
        val timeLimit: TimeLimit? = this.timeLimit
        val opponent = this.opponent
        return scope.async {
            timeLimit?.isTicking = true
            var move: GoPoint?
            try {
                do {
                    move = generateMove()
                } while(move != null && !opponent.acceptOpponentMove(move))
            } finally {
                timeLimit?.isTicking = false
            }
            move
        }
    }

    internal fun cancelMove(marker: InternalMarker) {
        marker.ignore()
        timeLimit?.isTicking = false
    }

    protected abstract suspend fun generateMove(): GoPoint?

    open suspend fun update() = Unit

    open suspend fun acceptHandicapStones(goban: Goban): Boolean = true

    open suspend fun acceptOpponentMove(move: GoPoint?): Boolean = true

    protected suspend fun requestUndoMove(resumeNode: GoSGFNode): Boolean =
        game.requestUndoMove(color, resumeNode, InternalMarker)

    open suspend fun acceptUndoMove(resumeNode: GoSGFNode): Boolean = true

    open fun requestOpponentTimeExtension(requestedMilliseconds: Long): Long =
        extendOpponentTime(requestedMilliseconds)

    protected fun extendOpponentTime(extension: Long): Long {
        if (extension <= 0L) return 0L
        val opponent = this.opponent
        var millis = opponent.filterTimeExtension(extension)
        if (millis <= 0L) return 0L
        if (millis > extension) millis = extension
        opponent.timeLimit?.extendTime(millis)
        return millis
    }

    open fun filterTimeExtension(extension: Long): Long = extension

    val timeEvent: TimeEvent? get() {
        val event = timeLimit?.timeEvent
        if (event == null || event.source === this) return event
        return TimeEvent(this, event.timeRemaining, event.overtimeCode, event.flags)
    }

    protected open fun onTimeEvent(player: GoPlayer, e: TimeEvent) = Unit

    private fun fireTimeEvent(player: GoPlayer, e: TimeEvent, marker: InternalMarker) {
        try {
            onTimeEvent(player, e)
        } finally {
            if (e.isExpired) game.gameOver(GameResult.time(winner = player.color.opponent), marker)
        }
    }

    val overtime: Overtime? = game.node.gameInfo?.overtime

    @Suppress("LeakingThis")
    private val timeLimit: TimeLimit? = TimeLimit.fromSGF(color, game.node, this, overtime = overtime)?.also {
        it.addTimeListener { e ->
            fireTimeEvent(this, e, InternalMarker)
            opponent.fireTimeEvent(this, e, InternalMarker)
        }
    }


    protected fun resign() {
        game.gameOver(GameResult.resign(winner = color.opponent), InternalMarker)
    }

    open fun startScoring() {
        submitScore()
    }

    open fun updateScoring(stones: GoPointSet, alive: Boolean) {
        submitScore()
    }

    open fun finishScoring() = Unit

    protected fun submitScore() {
        game.scoreManager?.submitPlayerScore(color, InternalMarker)
    }

    protected fun requestResumePlay() {
        game.scoreManager?.requestResumePlay(color, InternalMarker)
    }

    final override fun equals(other: Any?): Boolean = this === other

    final override fun hashCode(): Int = super.hashCode()

}