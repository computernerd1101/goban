package com.computernerd1101.goban.players

import com.computernerd1101.goban.*
import com.computernerd1101.goban.sgf.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select

class GoPlayerManager {

    val gameJob = Job()

    val gameScope: CoroutineScope

    @get:JvmName("getSGF")
    val sgf: GoSGF
    var node: GoSGFNode
        set(node) {
            if (node.tree != sgf)
                throw IllegalArgumentException("node")
            field = node
        }
    val gameInfo: GameInfo
    val blackPlayer: GoPlayer
    val whitePlayer: GoPlayer

    fun getPlayer(color: GoColor) = if (color == GoColor.BLACK) blackPlayer else whitePlayer

    constructor(dispatcher: CoroutineDispatcher, setup: GoGameSetup) {
        gameScope = CoroutineScope(dispatcher + gameJob)
        val width = setup.width
        val height = setup.height
        sgf = GoSGF(width, height)
        var node = sgf.rootNode
        gameInfo = setup.gameInfo
        node.gameInfo = gameInfo
        if (!setup.isFreeHandicap) {
            val goban = setup.generateFixedHandicap()
            if (goban != null) {
                gameInfo.handicap = goban.blackCount
                node = node.createNextSetupNode(goban)
            }
        }
        this.node = node
        var player1 = setup.player1
        var player2 = setup.player2
        if (setup.randomPlayer?.nextBoolean() == true) {
            val player = player1
            player1 = player2
            player2 = player
            gameInfo.swapPlayers()
        }
        blackPlayer = player1.createPlayer(this, GoColor.BLACK)
        whitePlayer = player2.createPlayer(this, GoColor.WHITE)
    }

    @Throws(GoSGFResumeException::class)
    constructor(dispatcher: CoroutineDispatcher, blackPlayer: GoPlayer, whitePlayer: GoPlayer, sgf: GoSGF) {
        gameScope = CoroutineScope(dispatcher + gameJob)
        this.sgf = sgf
        gameInfo = sgf.onResume()
        node = sgf.lastNodeBeforePasses()
        this.blackPlayer = blackPlayer
        this.whitePlayer = whitePlayer
    }

    fun startGame() {
        gameScope.launch {
            val handicap = gameInfo.handicap
            if (handicap != 0 && node == sgf.rootNode) {
                val goban = Goban(sgf.width, sgf.height)
                while(true) {
                    blackPlayer.generateHandicapStones(handicap, goban)
                    if (goban.blackCount < handicap)
                        continue
                    if (goban.blackCount == handicap && whitePlayer.acceptHandicapStones(goban))
                        break
                    goban.clear()
                }
                if (goban.whiteCount > 0)
                    goban.clear(GoColor.WHITE)
                node = node.createNextSetupNode(goban).also {
                    it.turnPlayer = GoColor.WHITE
                }
                blackPlayer.update()
                whitePlayer.update()
            }
            val moveChannel = Channel<GoPoint?>(Channel.CONFLATED)
            var passCount = 0
            while(passCount < 2) {
                val turnPlayer: GoColor = node.turnPlayer?.let {
                    if (node is GoSGFMoveNode) it.opponent
                    else it
                } ?: GoColor.BLACK
                val player: GoPlayer
                val opponent: GoPlayer
                if (turnPlayer == GoColor.BLACK) {
                    player = blackPlayer
                    opponent = whitePlayer
                } else {
                    player = whitePlayer
                    opponent = blackPlayer
                }
                player.requestMove(moveChannel)
                select<Unit> {
                    moveChannel.onReceive { move ->
                        if (move == null) passCount++
                        else passCount = 0
                        if (move == null || opponent.acceptOpponentMove(move)) {
                            val node = this@GoPlayerManager.node.createNextMoveNode(move, turnPlayer)
                            if (node.isLegalOrForced) {
                                node.moveVariation(0)
                                this@GoPlayerManager.node = node
                            } else node.delete()
                        }
                        player.update()
                        opponent.update()
                    }
                }
            }
            val scoreManager = GoScoreManager(this@GoPlayerManager)
            this@GoPlayerManager.scoreManager = scoreManager
            scoreManager.computeScore()
            // TODO
        }
    }

    var scoreManager: GoScoreManager? = null; private set

    suspend fun startScoring(scoreManager: GoScoreManager) {
        blackPlayer.startScoring(scoreManager)
        whitePlayer.startScoring(scoreManager)
    }

    suspend fun updateScoring(scoreManager: GoScoreManager, stones: GoPointSet, alive: Boolean) {
        blackPlayer.updateScoring(scoreManager, stones, alive)
        whitePlayer.updateScoring(scoreManager, stones, alive)
    }

    suspend fun finishScoring() {
        blackPlayer.finishScoring()
        whitePlayer.finishScoring()
    }

}

