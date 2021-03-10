package com.computernerd1101.goban.players

import com.computernerd1101.goban.*
import com.computernerd1101.goban.internal.*
import com.computernerd1101.goban.sgf.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.select
import kotlin.coroutines.*

@ExperimentalGoPlayerApi
class GoGameContext: CoroutineContext.Element {

    @get:JvmName("getSGF")
    val sgf: GoSGF
    var node: GoSGFNode private set
    val gameInfo: GameInfo

    companion object Key: CoroutineContext.Key<GoGameContext> {
        private val updateHypothetical = atomicUpdater<GoGameContext, GoSGFSetupNode?>("hypothetical")
    }

    override val key: CoroutineContext.Key<GoGameContext> get() = Key

    @Volatile var hypothetical: GoSGFSetupNode? = null; private set

    @JvmOverloads
    constructor(setup: GoGameSetup? = null) {
        val width = setup?.width ?: 19
        val height = setup?.height ?: 19
        sgf = GoSGF(width, height)
        var node = sgf.rootNode
        gameInfo = setup?.gameInfo ?: GameInfo()
        node.gameInfo = gameInfo
        if (setup?.isFreeHandicap != true) {
            // If setup is null, then handicap is zero, in which case goban will be null.
            val goban = setup?.generateFixedHandicap()
            if (goban != null) {
                gameInfo.handicap = goban.blackCount
                node = node.createNextSetupNode(goban)
            }
        }
        this.node = node
    }

    @Throws(GoSGFResumeException::class)
    constructor(sgf: GoSGF) {
        this.sgf = sgf
        gameInfo = sgf.onResume()
        node = sgf.lastNodeBeforePasses()
    }

    private lateinit var coroutineScope: CoroutineScope

    suspend fun startGame() {
        var context = coroutineContext
        val game = context[Key]
        val black = context[GoColor.BLACK]
        val white = context[GoColor.WHITE]
        if (black == null) {
            if (white == null) throw IllegalStateException("Missing black and white players")
            throw IllegalStateException("Missing black player")
        }
        if (white == null) throw IllegalStateException("Missing white player")
        when {
            game == null -> context += this
            game !== this ->
                throw IllegalStateException("A single coroutine cannot play two games of Go simultaneously.")
        }
        startGameWithContext(context, black, white)
    }

    private suspend fun startGameWithContext(
        context: CoroutineContext,
        blackPlayer: GoPlayer,
        whitePlayer: GoPlayer
    ) = withContext(context) {
        coroutineScope = CoroutineScope(context)
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
            node = node.createNextSetupNode(goban).also {
                it.turnPlayer = GoColor.WHITE
            }
            blackPlayer.update()
            whitePlayer.update()
        }
        lateinit var turnPlayer: GoColor
        lateinit var player: GoPlayer
        lateinit var opponent: GoPlayer
        val generateMove: suspend CoroutineScope.() -> GoPoint? = {
            player.generateMove()
        }
        var passCount = 0
        var lastNonPass: GoSGFNode = node
        val onGenerateMove: suspend (GoPoint?) -> Unit = { move ->
            if (move == null || opponent.acceptOpponentMove(move)) {
                val node = this@GoGameContext.node.createNextMoveNode(move, turnPlayer)
                if (node.isLegalOrForced) {
                    if (move == null) passCount++
                    else {
                        passCount = 0
                        lastNonPass = node
                    }
                    node.moveVariation(0)
                    this@GoGameContext.node = node
                } else node.delete()
            }
            player.update()
            opponent.update()
        }
        while (true) {
            turnPlayer = node.turnPlayer?.let {
                if (node is GoSGFMoveNode) it.opponent
                else it
            } ?: GoColor.BLACK
            if (turnPlayer == GoColor.BLACK) {
                player = blackPlayer
                opponent = whitePlayer
            } else {
                player = whitePlayer
                opponent = blackPlayer
            }
            val deferredMove = coroutineScope.async(block = generateMove)
            select<Unit> {
                deferredMove.onAwait(onGenerateMove)
            }
            if (passCount >= 2) {
                val scoreManager = GoScoreManager()
                this@GoGameContext.scoreManager = scoreManager
                val requestResume: GoColor = scoreManager.computeScore() ?: break
                if (gameInfo.rules.territoryScore) {
                    val hypotheticalNode = node.createNextSetupNode(node.goban)
                    hypotheticalNode.turnPlayer = requestResume.opponent
                    node = hypotheticalNode
                    updateHypothetical.compareAndSet(this@GoGameContext, null, hypotheticalNode)
                    // TODO start hypothetical play
                } else {
                    node = lastNonPass
                }
                // TODO
                player.update()
                opponent.update()
            }
        }
    }

    var scoreManager: GoScoreManager? = null; private set

}

