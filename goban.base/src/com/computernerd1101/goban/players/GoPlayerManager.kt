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
        node = sgf.primaryLeafNode()
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
            val moveChannel = Channel<GoPoint?>(1)
            while(gameJob.isActive) {
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
                        if (opponent.acceptOpponentMove(move)) {
                            node = node.createNextMoveNode(move, turnPlayer)
                            player.update()
                            opponent.update()
                        }
                    }
                }
            }
        }
    }

}

