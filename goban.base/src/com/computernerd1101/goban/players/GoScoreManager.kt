package com.computernerd1101.goban.players

import com.computernerd1101.goban.*
import com.computernerd1101.goban.internal.*
import com.computernerd1101.goban.sgf.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.select
import kotlin.coroutines.*

class GoScoreManager internal constructor(val game: GoGameManager, marker: InternalMarker) {

    init {
        marker.ignore()
    }

    private val _deadStones = Channel<GoPointSet>(Channel.UNLIMITED)
    val deadStones: SendChannel<GoPointSet> = SendOnlyChannel(_deadStones)

    private val _livingStones = Channel<GoPointSet>(Channel.UNLIMITED)
    val livingStones: SendChannel<GoPointSet> = SendOnlyChannel(_livingStones)

    companion object {
        private val updateSubmitPlayerFlags = atomicIntUpdater<GoScoreManager>("submitPlayerFlags")
    }

    @Volatile private var submitPlayerFlags: Int = 0
    private val submitted = ContinuationProxy<GoColor?>()
    private fun unSubmitScore(marker: InternalMarker) {
        marker.ignore()
        submitPlayerFlags = 0
    }
    internal fun submitPlayerScore(color: GoColor, marker: InternalMarker) {
        marker.ignore()
        val flags = updateSubmitPlayerFlags.accumulateAndGet(this,
            if (color == GoColor.BLACK) 1 else 2, BinOp.OR)
        if ((flags and 3) == 3) {
            val continuation = submitted.continuation
            submitted.continuation = null
            continuation?.resume(null)
        }
    }
    internal fun requestResumePlay(color: GoColor, marker: InternalMarker) {
        marker.ignore()
        val continuation = submitted.continuation
        submitted.continuation = null
        continuation?.resume(color)
    }

    suspend fun computeScore(): GoColor? {
        submitPlayerFlags = 0
        val scope = CoroutineScope(coroutineContext)
        val deferredSubmit: Deferred<GoColor?> = submitted.suspendAsync(scope)
        val blackPlayer = game.blackPlayer
        val whitePlayer = game.whitePlayer
        blackPlayer.startScoring()
        whitePlayer.startScoring()
        val node = game.node
        val goban: FixedGoban = node.goban
        val finalGoban: Goban = goban.playable()
        val receiveStones = MutableGoPointSet()
        val sendStones = MutableGoPointSet()
        val group = MutableGoPointSet()
        var resumeRequest: GoColor? = null
        var waiting = true
        while(waiting) select<Unit> {
            _deadStones.onReceive {
                unSubmitScore(InternalMarker)
                receiveStones.copyFrom(it)
                sendStones.clear()
                for (point in receiveStones) {
                    if (goban[point] == null) continue
                    goban.getGroup(point, group)
                    finalGoban.setAll(group, null)
                    receiveStones.removeAll(group)
                    sendStones.addAll(group)
                }
                val stones = sendStones.readOnly()
                blackPlayer.updateScoring(stones, false)
                whitePlayer.updateScoring(stones, false)
            }
            _livingStones.onReceive {
                unSubmitScore(InternalMarker)
                receiveStones.copyFrom(it)
                sendStones.clear()
                for (point in receiveStones) {
                    val color = goban[point] ?: continue
                    goban.getGroup(point, group)
                    finalGoban.setAll(group, color)
                    receiveStones.removeAll(group)
                    sendStones.addAll(group)
                }
                val stones = sendStones.readOnly()
                blackPlayer.updateScoring(stones, true)
                whitePlayer.updateScoring(stones, true)
            }
            deferredSubmit.onAwait {
                waiting = false
                resumeRequest = it
            }
        }
        if (resumeRequest != null) return resumeRequest
        val territory: Boolean = game.gameInfo.rules.territoryScore
        val scoreGoban: MutableGoban = finalGoban.getScoreGoban(territory, node.territory)
        val gameInfo = game.gameInfo
        var score = gameInfo.komi.toFloat() + (scoreGoban.whiteCount - scoreGoban.blackCount)
        if (territory && node is GoSGFMoveNode) {
            score += node.getPrisonerScoreMargin(GoColor.WHITE)
        }
        gameInfo.result = if (score == 0.0f) GameResult.DRAW
        else GameResult(GoColor.WHITE, score) // negative score means Black wins
        blackPlayer.finishScoring()
        whitePlayer.finishScoring()
        return null
    }

}