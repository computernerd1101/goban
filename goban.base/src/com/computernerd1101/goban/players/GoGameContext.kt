package com.computernerd1101.goban.players

import com.computernerd1101.goban.*
import com.computernerd1101.goban.internal.*
import com.computernerd1101.goban.sgf.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.select
import kotlin.coroutines.*

@ExperimentalGoPlayerApi
val CoroutineContext.goGameContext: GoGameContext get() = this[GoGameContext] ?: throw IllegalStateException(
    "Missing Go game context"
)

@ExperimentalGoPlayerApi
private fun GoPlayer.Factory.safeCreatePlayer(color: GoColor): GoPlayer {
    val player = createPlayer(color)
    val realColor = player.color
    if (realColor != color)
        throw IllegalStateException("GoPlayer.Factory.createPlayer(GoColor." + color.name +
            ") returned a GoPlayer whose color was ${realColor.name}")
    return player
}

@ExperimentalGoPlayerApi
class GoGameContext: CoroutineContext.Element {

    companion object Key: CoroutineContext.Key<GoGameContext> {
        private val updateHypothetical = atomicUpdater<GoGameContext, GoSGFSetupNode?>("hypothetical")
    }

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

    override val key: CoroutineContext.Key<GoGameContext> get() = Key

    @Suppress("unused")
    @Volatile var hypothetical: GoSGFSetupNode? = null; private set

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
    @Throws(GoSGFResumeException::class)
    constructor(sgf: GoSGF, player1: GoPlayer, player2: GoPlayer) {
        this.sgf = sgf
        if (player1.color == GoColor.BLACK) {
            if (player2.color == GoColor.BLACK)
                throw IllegalArgumentException("Two black players")
            blackPlayer = player1
            whitePlayer = player2
        } else {
            if (player2.color == GoColor.WHITE)
                throw IllegalArgumentException("Two white players")
            whitePlayer = player1
            blackPlayer = player2
        }
        gameInfo = sgf.onResume()
        _node = sgf.lastNodeBeforePasses()
    }

    suspend fun startGame() {
        var context = coroutineContext
        val game = context[Key]
        when {
            game == null -> context += this
            game !== this ->
                throw IllegalStateException("A single coroutine cannot play two games of Go simultaneously.")
        }
        startGameWithContext(context)
    }

    private suspend fun startGameWithContext(
        context: CoroutineContext
    ) = withContext(context) {
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
        var passCount = 0
        var lastNonPass: GoSGFNode = node
        var deferredUndoMove: Deferred<GoSGFNode>? = null
        while (true) {
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
            val deferredMove = async {
                player.generateMove()
            }
            if (deferredUndoMove == null) deferredUndoMove = async {
                val node = suspendCoroutine<GoSGFNode> { undoMoveContinuation = it }
                // TODO remove debug print
                println("deferredUndoMove async completed")
                println(node)
                node
            }
            select<Unit> {
                deferredMove.onAwait { move ->
                    if (move == null || opponent.acceptOpponentMove(move)) {
                        val node = this@GoGameContext.node.createNextMoveNode(move, turnPlayer)
                        if (node.isLegalOrForced) {
                            if (move == null) passCount++
                            else {
                                passCount = 0
                                lastNonPass = node
                            }
                            node.moveVariation(0)
                            setNode(node, InternalMarker)
                        } else node.delete()
                    }
                    player.update()
                    opponent.update()
                    println("deferredMove.onAwait")
                }
                deferredUndoMove?.onAwait { resumeNode ->
                    deferredMove.cancel()
                    deferredUndoMove = null
                    // TODO remove debug print
                    println("deferredUndoMove.onAwait")
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
            println("select")
            if (passCount >= 2) {
                val scoreManager = GoScoreManager()
                this@GoGameContext.scoreManager = scoreManager
                val requestResume: GoColor = scoreManager.computeScore() ?: break
                if (gameInfo.rules.territoryScore) {
                    val hypotheticalNode = node.createNextSetupNode(node.goban)
                    hypotheticalNode.turnPlayer = requestResume.opponent
                    setNode(hypotheticalNode, InternalMarker)
                    updateHypothetical.compareAndSet(this@GoGameContext, null, hypotheticalNode)
                    // TODO start hypothetical play
                } else {
                    setNode(lastNonPass, InternalMarker)
                }
                // TODO
                player.update()
                opponent.update()
            }
        }
        println("Game over")
    }

    private var undoMoveContinuation: Continuation<GoSGFNode>? = null

    internal suspend fun requestUndoMove(
        requestingPlayer: GoColor,
        resumeNode: GoSGFNode,
        marker: InternalMarker
    ): Boolean {
        marker.ignore()
        val continuation = undoMoveContinuation ?: return false
        val response: Boolean = getPlayer(requestingPlayer.opponent).acceptUndoMove(resumeNode)
        if (response) {
            // TODO remove debug print
            println("GoGameContext.requestUndoMove")
            undoMoveContinuation = null
            continuation.resume(resumeNode)
        }
        return response
    }

    var scoreManager: GoScoreManager? = null; private set

}

