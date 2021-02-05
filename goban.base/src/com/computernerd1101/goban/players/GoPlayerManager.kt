package com.computernerd1101.goban.players

import com.computernerd1101.goban.GoColor
import com.computernerd1101.goban.sgf.*
import kotlinx.coroutines.*

class GoPlayerManager {

    val gameJob = Job()
    val blackPlayerJob = Job(gameJob)
    val whitePlayerJob = Job(gameJob)

    val gameScope = CoroutineScope(Dispatchers.Main + gameJob)
    val blackPlayerScope = CoroutineScope(Dispatchers.Main + blackPlayerJob)
    val whitePlayerScope = CoroutineScope(Dispatchers.Main + whitePlayerJob)

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

    constructor(setup: GoGameSetup) {
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
        startGame()
    }

    @Throws(GoSGFResumeException::class)
    constructor(blackPlayer: GoPlayer, whitePlayer: GoPlayer, sgf: GoSGF) {
        this.sgf = sgf
        node = sgf.rootNode
        gameInfo = sgf.onResume()
        this.blackPlayer = blackPlayer
        this.whitePlayer = whitePlayer
        startGame()
    }

    private fun startGame() {
        gameScope.launch {
            // TODO
        }
    }

}

