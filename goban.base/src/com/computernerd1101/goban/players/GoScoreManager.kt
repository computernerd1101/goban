package com.computernerd1101.goban.players

import com.computernerd1101.goban.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.select

class GoScoreManager(val playerManager: GoPlayerManager) {

    private val _deadStones = Channel<GoPointSet>(Channel.UNLIMITED)
    val deadStones: SendChannel<GoPointSet> get() = _deadStones

    private val _livingStones = Channel<GoPointSet>(Channel.UNLIMITED)
    val livingStones: SendChannel<GoPointSet> get() = _livingStones

    private val _finishVerdict = Channel<GoColor>(Channel.CONFLATED)
    val finishVerdict: SendChannel<GoColor> get() = _finishVerdict

    private val _resumePlay = Channel<GoColor>(Channel.RENDEZVOUS)
    val resumePlay: SendChannel<GoColor> get() = _resumePlay

    suspend fun computeScore(): GoColor? {
        playerManager.startScoring(this)
        var blackDone = false
        var whiteDone = false
        var resumeRequest: GoColor? = null
        val node = playerManager.node
        val goban: FixedGoban = node.goban
        val finalGoban: Goban = goban.playable()
        val receiveStones = MutableGoPointSet()
        val sendStones = MutableGoPointSet()
        val group = MutableGoPointSet()
        while(resumeRequest == null && !(blackDone && whiteDone)) {
            select<Unit> {
                _deadStones.onReceive {
                    blackDone = false
                    whiteDone = false
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
                    blackDone = false
                    whiteDone = false
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
                _finishVerdict.onReceive {
                    if (it == GoColor.BLACK)
                        blackDone = true
                    else whiteDone = true
                }
                _resumePlay.onReceive {
                    resumeRequest = it
                }
            }
        }
        if (resumeRequest == null) {
            val territory = playerManager.gameInfo.rules.territoryScore
            val scoreGoban = finalGoban.getScoreGoban(territory, node.territory)
            // TODO
            playerManager.finishScoring()
        }
        return resumeRequest
    }

}