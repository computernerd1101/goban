package com.computernerd1101.goban.players

import com.computernerd1101.goban.*
import com.computernerd1101.goban.internal.*
import com.computernerd1101.goban.sgf.*
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select
import kotlin.coroutines.*

class GoGameManager {

    val blackPlayer: GoPlayer
    val whitePlayer: GoPlayer

    fun getPlayer(color: GoColor): GoPlayer =
        if (color == GoColor.BLACK) blackPlayer
        else whitePlayer

    @get:JvmName("getSGF")
    val sgf: GoSGF

    private var _node: GoSGFNode
    val node: GoSGFNode get() = _node
    private fun setNode(node: GoSGFNode, marker: InternalMarker) {
        marker.ignore()
        _node = node
    }

    val gameInfo: GameInfo

    private companion object {
        private val updateHypothetical = atomicUpdater<GoGameManager, GoSGFSetupNode?>("_hypothetical")
    }

    @Volatile private var _hypothetical: GoSGFSetupNode? = null
    @Suppress("unused")
    val hypothetical: GoSGFSetupNode? get() = _hypothetical

    val job: Job = Job()

    constructor(defaultPlayerFactory: GoPlayer.Factory): this(null, defaultPlayerFactory)

    constructor(setup: GoGameSetup?, defaultPlayerFactory: GoPlayer.Factory) {
        val width = setup?.width ?: 19
        val height = setup?.height ?: 19
        sgf = GoSGF(width, height)
        var node = sgf.rootNode
        gameInfo = setup?.gameInfo ?: GameInfo()
        node.gameInfo = gameInfo
        var player1 = setup?.blackPlayer ?: defaultPlayerFactory
        var player2 = setup?.whitePlayer ?: defaultPlayerFactory
        if (setup?.randomPlayer?.nextBoolean() == true) {
            val tmp = player1
            player1 = player2
            player2 = tmp
            setup.player1 = player1
            setup.player2 = player2
            gameInfo.swapPlayers()
        }
        blackPlayer = player1.safeCreatePlayer(GoColor.BLACK)
        whitePlayer = player2.safeCreatePlayer(GoColor.WHITE)
        if (setup?.isFreeHandicap != true) {
            // If setup is null, then handicap is zero, in which case goban will be null.
            val goban = setup?.generateFixedHandicap()
            if (goban != null) {
                gameInfo.handicap = goban.blackCount
                node = node.createNextSetupNode(goban)
            }
        }
        _node = node
    }

    @Suppress("unused")
    @ExperimentalGoPlayerApi
    @Throws(GoSGFResumeException::class)
    constructor(sgf: GoSGF, blackPlayer: GoPlayer.Factory, whitePlayer: GoPlayer.Factory) {
        this.sgf = sgf
        this.blackPlayer = blackPlayer.safeCreatePlayer(GoColor.BLACK)
        this.whitePlayer = whitePlayer.safeCreatePlayer(GoColor.WHITE)
        gameInfo = sgf.onResume()
        _node = sgf.lastNodeBeforePasses()
    }

    private fun GoPlayer.Factory.safeCreatePlayer(color: GoColor): GoPlayer {
        val player = createPlayer(this@GoGameManager, color)
        val realColor = player.color
        if (realColor != color)
            throw IllegalStateException("GoPlayer.Factory.createPlayer(GoColor." + color.name +
                    ") returned a GoPlayer whose color was ${realColor.name}")
        return player
    }

    suspend fun startGame() {
        val handicap = gameInfo.handicap
        if (handicap != 0 && node == sgf.rootNode) {
            val goban = Goban(sgf.width, sgf.height)
            while (true) {
                blackPlayer.generateHandicapStones(handicap, goban)
                if (goban.blackCount < handicap)
                    continue
                if (goban.blackCount == handicap && whitePlayer.acceptHandicapStones(goban))
                    break
                goban.clear()
            }
            if (goban.whiteCount > 0)
                goban.clear(GoColor.WHITE)
            setNode(node.createNextSetupNode(goban).also {
                it.turnPlayer = GoColor.WHITE
            }, InternalMarker)
            blackPlayer.update()
            whitePlayer.update()
        }
        var context = coroutineContext
        if (context[Job] !== job) context += job
        val scope = CoroutineScope(context)
        val deferredGameOver = gameOverContinuation.suspendAsync(scope)
        var passCount = 0
        var lastNonPass: GoSGFNode = node
        var deferredUndoMove: Deferred<GoSGFNode>? = null
        var ongoing = true
        while (ongoing) {
            val turnPlayer = node.turnPlayer?.let {
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
            val deferredMove = player.generateMoveAsync(scope, InternalMarker)
            if (deferredUndoMove == null) deferredUndoMove = undoMoveContinuation.suspendAsync(scope)
            select<Unit> {
                deferredMove.onAwait { move ->
                    val node = this@GoGameManager.node.createNextMoveNode(move, turnPlayer)
                    if (node.isLegalOrForced) {
                        if (move == null) passCount++
                        else {
                            passCount = 0
                            lastNonPass = node
                        }
                        node.moveVariation(0)
                        setNode(node, InternalMarker)
                    } else node.delete()
                    player.update()
                    opponent.update()
                }
                deferredGameOver.onAwait { result ->
                    player.cancelMove(InternalMarker)
                    gameInfo.result = result
                    ongoing = false
                    passCount = 0
                }
                deferredUndoMove?.onAwait { resumeNode ->
                    deferredMove.cancel()
                    player.cancelMove(InternalMarker)
                    deferredUndoMove = null
                    passCount = 0
                    var node: GoSGFNode? = resumeNode
                    while(node != null) {
                        if (node !is GoSGFMoveNode || node.playStoneAt != null) {
                            lastNonPass = node
                            break
                        }
                        passCount++
                        node = node.parent
                    }
                    setNode(resumeNode, InternalMarker)
                    player.update()
                    opponent.update()
                }
            }
            if (passCount >= 2) {
                val scoreManager = GoScoreManager(this, InternalMarker)
                _scoreManager = scoreManager
                val requestResume: GoColor = scoreManager.computeScore() ?: break
                if (gameInfo.rules.territoryScore) {
                    val hypotheticalNode = node.createNextSetupNode(node.goban)
                    hypotheticalNode.turnPlayer = requestResume.opponent
                    setNode(hypotheticalNode, InternalMarker)
                    updateHypothetical.compareAndSet(this, null, hypotheticalNode)
                    // TODO start hypothetical play
                } else {
                    setNode(lastNonPass, InternalMarker)
                }
                // TODO
                player.update()
                opponent.update()
            }
        }
    }

    private val gameOverContinuation = ContinuationProxy<GameResult>()

    internal fun gameOver(result: GameResult, marker: InternalMarker) {
        marker.ignore()
        val continuation = gameOverContinuation.continuation ?: return
        gameOverContinuation.continuation = null
        continuation.resume(result)
    }

    private val undoMoveContinuation = ContinuationProxy<GoSGFNode>()

    internal suspend fun requestUndoMove(
        requestingPlayer: GoColor,
        resumeNode: GoSGFNode,
        marker: InternalMarker
    ): Boolean {
        marker.ignore()
        val continuation = undoMoveContinuation.continuation ?: return false
        val response: Boolean = getPlayer(requestingPlayer.opponent).acceptUndoMove(resumeNode)
        if (response) {
            undoMoveContinuation.continuation = null
            continuation.resume(resumeNode)
        }
        return response
    }

    private var _scoreManager: GoScoreManager? = null
    val scoreManager: GoScoreManager? get() = _scoreManager

}

