package com.computernerd1101.goban.players

import com.computernerd1101.goban.*
import com.computernerd1101.goban.internal.*
import com.computernerd1101.goban.sgf.GameResult
import com.computernerd1101.goban.sgf.GoSGFNode
import com.computernerd1101.goban.time.*
import kotlin.coroutines.*

abstract class GoPlayer(val color: GoColor) {

    fun interface Factory {

        fun createPlayer(color: GoColor): GoPlayer

        fun isCompatible(setup: GoGameSetup) = true

    }

    suspend fun getOpponent(): GoPlayer = getOpponent(coroutineContext.goGameContext)

    fun getOpponent(game: GoGameContext) = game.getPlayer(color.opponent)

    abstract suspend fun generateHandicapStones(handicap: Int, goban: Goban)

    internal suspend fun safeGenerateMove(marker: InternalMarker): GoPoint? {
        marker.ignore()
        val opponent = getOpponent()
        val timeLimit = getTimeLimit(coroutineContext, null)
        timeLimit?.isTicking = true
        return try {
            var move: GoPoint?
            while(true) {
                move = generateMove()
                if (move == null || opponent.acceptOpponentMove(move)) break
            }
            move
        } finally {
            timeLimit?.isTicking = false
        }
    }

    protected abstract suspend fun generateMove(): GoPoint?

    open suspend fun update() = Unit

    open suspend fun acceptHandicapStones(goban: Goban): Boolean = true

    open suspend fun acceptOpponentMove(move: GoPoint?): Boolean = true

    protected suspend fun requestUndoMove(resumeNode: GoSGFNode): Boolean =
        coroutineContext.goGameContext.requestUndoMove(color, resumeNode, InternalMarker)

    open suspend fun acceptUndoMove(resumeNode: GoSGFNode): Boolean = true

    open suspend fun requestOpponentTimeExtension(requestedMilliseconds: Long): Long =
        extendOpponentTime(requestedMilliseconds)

    protected suspend fun extendOpponentTime(extension: Long): Long {
        if (extension <= 0L) return 0L
        val context = coroutineContext
        val gameContext = context[GoGameContext] ?: return 0L
        val opponent = getOpponent(gameContext)
        var realExtension = opponent.filterTimeExtension(extension)
        if (realExtension <= 0L) return 0L
        if (realExtension > extension) realExtension = extension
        opponent.getTimeLimit(context, gameContext)?.extendTime(realExtension)
        return realExtension
    }

    open fun filterTimeExtension(extension: Long): Long = extension

    suspend fun getTimeEvent(): TimeEvent? = getTimeEvent(coroutineContext)

    fun getTimeEvent(context: CoroutineContext): TimeEvent? {
        val event = getTimeLimit(context, null)?.timeEvent
        if (event == null || event.source === this) return event
        return TimeEvent(this, event.timeRemaining, event.overtimeCode, event.flags)
    }

    protected open fun onTimeEvent(e: TimeEvent) = Unit

    private fun fireTimeEvent(e: TimeEvent, gameContext: GoGameContext, marker: InternalMarker) {
        try {
            onTimeEvent(e)
        } finally {
            if (e.isExpired) gameContext.gameOver(GameResult.time(winner = color.opponent), marker)
        }
    }

    private fun getTimeLimit(context: CoroutineContext, goGameContext: GoGameContext?): TimeLimit? {
        if (timeLimit == null) {
            val gameContext = goGameContext ?: context[GoGameContext] ?: return null
            val overtime = getOvertimeDirect(context, gameContext)
            val limit = TimeLimit.fromSGF(color, gameContext.node, overtime = overtime, owner = this)
            when {
                limit == null -> if (updateTimeLimit.compareAndSet(this, null, nullMask))
                    return null
                updateTimeLimit.compareAndSet(this, null, limit) -> {
                    limit.addTimeListener { e ->
                        fireTimeEvent(e, gameContext, InternalMarker)
                    }
                    return limit
                }
            }
        }
        return timeLimit as? TimeLimit
    }

    suspend fun getOvertime(): Overtime? = getOvertime(coroutineContext)

    fun getOvertime(context: CoroutineContext): Overtime? =
        getOvertimeDirect(context, null)

    private fun getOvertimeDirect(context: CoroutineContext, gameContext: GoGameContext?): Overtime? {
        if (overtime == null) {
            val ot = (gameContext ?: context[GoGameContext] ?: return null).node.gameInfo?.overtime
            if (updateOvertime.compareAndSet(this, null, ot ?: nullMask))
                return ot
        }
        return overtime as? Overtime
    }

    companion object {

        private val updateTimeLimit = atomicUpdater<GoPlayer, Any?>("timeLimit")
        private val updateOvertime = atomicUpdater<GoPlayer, Any?>("overtime")
        private val nullMask = Any()

    }

    @Volatile private var overtime: Any? = null
    @Volatile private var timeLimit: Any? = null

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
        if (game.getPlayer(color) !== this) throw SecurityException()
    }

    protected fun submitScore(game: GoGameContext, scoreManager: GoScoreManager) {
        checkPermissions(game)
        scoreManager.submitPlayerScore(color, InternalMarker)
    }

    protected fun requestResumePlay(game: GoGameContext, scoreManager: GoScoreManager) {
        checkPermissions(game)
        scoreManager.requestResumePlay(color, InternalMarker)
    }

    final override fun equals(other: Any?): Boolean = this === other

    final override fun hashCode(): Int = super.hashCode()

}