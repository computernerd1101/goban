package com.computernerd1101.goban.players

import com.computernerd1101.goban.*
import com.computernerd1101.goban.internal.*
import com.computernerd1101.goban.sgf.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.select

class GoScoreManager(val playerManager: GoPlayerManager) {

    private val _deadStones = Channel<GoPointSet>(Channel.UNLIMITED)
    val deadStones: SendChannel<GoPointSet> = SendOnlyChannel(_deadStones)

    private val _livingStones = Channel<GoPointSet>(Channel.UNLIMITED)
    val livingStones: SendChannel<GoPointSet> = SendOnlyChannel(_livingStones)

    private val submit = Channel<GoColor>(Channel.CONFLATED)
    internal fun getSubmit(marker: InternalMarker): SendChannel<GoColor> {
        marker.ignore()
        return submit
    }

    private val resumePlay = Channel<GoColor>(Channel.RENDEZVOUS)
    internal fun getResumePlay(marker: InternalMarker): SendChannel<GoColor> {
        marker.ignore()
        return resumePlay
    }

    suspend fun computeScore(): GoColor? {
        playerManager.startScoring(this)
        var waitingForBlack = true
        var waitingForWhite = true
        val node = playerManager.node
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
                    playerManager.updateScoring(this@GoScoreManager, sendStones.readOnly(), false)
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
                    playerManager.updateScoring(this@GoScoreManager, sendStones.readOnly(), true)
                }
                submit.onReceive {
                    if (it == GoColor.BLACK)
                        waitingForBlack = false
                    else waitingForWhite = false
                }
                resumePlay.onReceive {
                    resumeRequest = it
                }
            }
            if (resumeRequest != null) return resumeRequest
        }
        val territory: Boolean = playerManager.gameInfo.rules.territoryScore
        val scoreGoban: MutableGoban = finalGoban.getScoreGoban(territory, node.territory)
        val gameInfo = playerManager.gameInfo
        var score = gameInfo.komi.toFloat() + (scoreGoban.whiteCount - scoreGoban.blackCount)
        if (territory && node is GoSGFMoveNode) {
            score += node.getPrisonerScoreMargin(GoColor.WHITE)
        }
        gameInfo.result = if (score == 0.0f) GameResult.DRAW
        else GameResult(GoColor.WHITE, score) // negative score means Black wins
        playerManager.finishScoring()
        return null
    }

}