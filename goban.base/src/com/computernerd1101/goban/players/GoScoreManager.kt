package com.computernerd1101.goban.players

import com.computernerd1101.goban.*
import com.computernerd1101.goban.internal.*
import com.computernerd1101.goban.sgf.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.SelectClause1
import kotlinx.coroutines.selects.select
import kotlin.coroutines.Continuation
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@ExperimentalGoPlayerApi
class GoScoreManager {

    private val _deadStones = Channel<GoPointSet>(Channel.UNLIMITED)
    val deadStones: SendChannel<GoPointSet> = SendOnlyChannel(_deadStones)

    private val _livingStones = Channel<GoPointSet>(Channel.UNLIMITED)
    val livingStones: SendChannel<GoPointSet> = SendOnlyChannel(_livingStones)

    private var submit: Continuation<GoColor>? = null
    private fun suspendSubmitScore(continuation: Continuation<GoColor>, marker: InternalMarker) {
        marker.ignore()
        submit = continuation
    }
    internal fun submitScore(color: GoColor, marker: InternalMarker) {
        marker.ignore()
        submit?.resume(color)
    }

    private var resumePlay: Continuation<GoColor>? = null
    private fun suspendResumePlay(continuation: Continuation<GoColor>, marker: InternalMarker) {
        marker.ignore()
        resumePlay = continuation
    }
    internal fun requestResumePlay(color: GoColor, marker: InternalMarker){
        marker.ignore()
        resumePlay?.resume(color)
    }

    suspend fun computeScore(): GoColor? {
        var waitingForBlack = true
        var waitingForWhite = true
        val deferredScore: Deferred<Unit>
        val deferredResumePlay: Deferred<GoColor>
        coroutineScope {
            deferredScore = async {
                while(waitingForBlack || waitingForWhite) {
                    val color = suspendCoroutine<GoColor> { continuation ->
                        suspendSubmitScore(continuation, InternalMarker)
                    }
                    if (color == GoColor.BLACK) waitingForBlack = false
                    else waitingForWhite = false
                }
            }
            deferredResumePlay = async {
                suspendCoroutine { continuation ->
                    suspendResumePlay(continuation, InternalMarker)
                }
            }
        }
        val gameContext = coroutineContext.goGameContext
        val blackPlayer = gameContext.blackPlayer
        val whitePlayer = gameContext.whitePlayer
        blackPlayer.startScoring(this)
        whitePlayer.startScoring(this)
        val node = gameContext.node
        val goban: FixedGoban = node.goban
        val finalGoban: Goban = goban.playable()
        val receiveStones = MutableGoPointSet()
        val sendStones = MutableGoPointSet()
        val group = MutableGoPointSet()
        while(waitingForBlack || waitingForWhite) {
            var resumeRequest: GoColor? = null
            select<Unit> {
                _deadStones.onReceive {
                    waitingForBlack = true
                    waitingForWhite = true
                    receiveStones.copyFrom(it)
                    sendStones.clear()
                    for(point in receiveStones) {
                        if (goban[point] == null) continue
                        goban.getGroup(point, group)
                        finalGoban.setAll(group, null)
                        receiveStones.removeAll(group)
                        sendStones.addAll(group)
                    }
                    val stones = sendStones.readOnly()
                    blackPlayer.updateScoring(this@GoScoreManager, stones, false)
                    whitePlayer.updateScoring(this@GoScoreManager, stones, false)
                }
                _livingStones.onReceive {
                    waitingForBlack = true
                    waitingForWhite = true
                    receiveStones.copyFrom(it)
                    sendStones.clear()
                    for(point in receiveStones) {
                        val color = goban[point] ?: continue
                        goban.getGroup(point, group)
                        finalGoban.setAll(group, color)
                        receiveStones.removeAll(group)
                        sendStones.addAll(group)
                    }
                    val stones = sendStones.readOnly()
                    blackPlayer.updateScoring(this@GoScoreManager, stones, true)
                    whitePlayer.updateScoring(this@GoScoreManager, stones, true)
                }
                deferredScore.onAwait { }
                deferredResumePlay.onAwait {
                    resumeRequest = it
                }
            }
            if (resumeRequest != null) return resumeRequest
        }
        val territory: Boolean = gameContext.gameInfo.rules.territoryScore
        val scoreGoban: MutableGoban = finalGoban.getScoreGoban(territory, node.territory)
        val gameInfo = gameContext.gameInfo
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