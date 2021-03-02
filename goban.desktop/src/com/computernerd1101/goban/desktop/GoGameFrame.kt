package com.computernerd1101.goban.desktop

import com.computernerd1101.goban.*
import com.computernerd1101.goban.desktop.internal.InternalMarker
import com.computernerd1101.goban.desktop.resources.*
import com.computernerd1101.goban.players.*
import com.computernerd1101.goban.sgf.GoSGFMoveNode
import com.computernerd1101.goban.sgf.GoSGFNode
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import java.awt.*
import java.awt.event.*
import java.awt.geom.Rectangle2D
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
import javax.swing.*

class GoGameFrame(val manager: GoPlayerManager): JFrame() {

    init {
        title = "CN13 Goban"
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        setSize(1000, 500)
        extendedState = Frame.MAXIMIZED_BOTH
        setLocationRelativeTo(null)
    }

    abstract class AbstractPlayer(manager: GoPlayerManager, color: GoColor): GoPlayer(manager, color) {

        abstract val frame: GoGameFrame

        override suspend fun generateHandicapStones(handicap: Int, goban: Goban) = withContext(Dispatchers.IO) {
            frame.generateHandicapStones(handicap, goban, InternalMarker)
        }

        override suspend fun requestMove(channel: SendChannel<GoPoint?>) = withContext(Dispatchers.IO) {
            frame.requestMove(color, channel, InternalMarker)
        }

        override suspend fun update() = withContext(Dispatchers.IO) {
            frame.update(InternalMarker)
        }

        override suspend fun acceptUndoMove(resumeNode: GoSGFNode): Boolean {
            val opponent = this.opponent as? AbstractPlayer
            if (opponent?.frame === frame) return true
            return opponent?.frame === frame || withContext(Dispatchers.IO) {
                frame.acceptUndoMove(resumeNode, InternalMarker)
            }
        }

        override suspend fun startScoring(scoreManager: GoScoreManager) = withContext(Dispatchers.IO) {
            frame.startScoring(scoreManager, InternalMarker)
        }

        override suspend fun updateScoring(
            scoreManager: GoScoreManager,
            stones: GoPointSet,
            alive: Boolean
        ) = withContext(Dispatchers.IO) {
            frame.updateScoring(scoreManager, stones, alive, InternalMarker)
        }

        override suspend fun finishScoring() = withContext(Dispatchers.IO) {
            frame.finishScoring(InternalMarker)
        }

        internal suspend fun submitScore(scoreManager: GoScoreManager, marker: InternalMarker) {
            marker.ignore()
            submitScore(scoreManager)
        }

        internal suspend fun requestResumePlay(scoreManager: GoScoreManager, marker: InternalMarker) {
            marker.ignore()
            requestResumePlay(scoreManager)
        }

    }

    class Player(
        private val factory: PlayerFactory,
        manager: GoPlayerManager,
        color: GoColor
    ): AbstractPlayer(manager, color) {

        override val frame: GoGameFrame get() = factory.frame

    }

    class PlayerFactory: GoPlayer.Factory {

        override fun createPlayer(manager: GoPlayerManager, color: GoColor) =
            Player(this, manager, color)

        private companion object {
            val updateFrame: AtomicReferenceFieldUpdater<PlayerFactory, GoGameFrame?> =
                AtomicReferenceFieldUpdater.newUpdater(
                    PlayerFactory::class.java,
                    GoGameFrame::class.java,
                    "atomicFrame"
                )
        }

        @Volatile private var atomicFrame: GoGameFrame? = null

        var frame: GoGameFrame
            get() = atomicFrame ?:
                throw UninitializedPropertyAccessException("property frame has not been initialized")
            set(frame) {
                if (!updateFrame.compareAndSet(this, null, frame))
                    throw IllegalStateException("property frame has already been initialized")
            }
    }

    private enum class GameAction {
        HANDICAP,
        PLAY_BLACK,
        PLAY_WHITE,
        COUNT_SCORE
    }

    companion object {
        private val updateGameAction: AtomicReferenceFieldUpdater<GoGameFrame, GameAction?> =
            AtomicReferenceFieldUpdater.newUpdater(
                GoGameFrame::class.java, GameAction::class.java, "gameAction"
            )

        @JvmField val TRANSPARENT_BLACK = Color(0x7F000000, true)
        @JvmField val TRANSPARENT_WHITE = Color(0x7FFFFFFF, true)
    }

    private val sgf = manager.sgf
    private val superkoRestrictions = MutableGoPointSet()
    private val suicideRestrictions = MutableGoPointSet()
    private val goPointGroup = MutableGoPointSet()
    private val prototypeGoban = Goban(sgf.width, sgf.height)
    private var scoreGoban: MutableGoban = manager.node.territory

    @Volatile private var gameAction: GameAction? = null
    private var handicap = 0
    private var channel: SendChannel<GoPoint?>? = null
    private var scoreManager: GoScoreManager? = null

    internal suspend fun generateHandicapStones(handicap: Int, goban: Goban, marker: InternalMarker) {
        marker.ignore()
        val blackCount = goban.blackCount
        if (blackCount > handicap)
            goban.clear()
        else if (goban.whiteCount > 0)
            goban.clear(GoColor.WHITE)
        if (updateGameAction.compareAndSet(this@GoGameFrame, null, GameAction.HANDICAP)) {
            superkoRestrictions.clear()
            this@GoGameFrame.handicap = handicap
            gobanView.goban = goban
            updateHandicapText(handicap, blackCount)
            actionButton.isEnabled = blackCount == handicap
            val channel = Channel<GoPoint?>()
            this@GoGameFrame.channel = channel
            channel.receive()
            channel.close(null)
        }
    }

    internal fun requestMove(player: GoColor, channel: SendChannel<GoPoint?>, marker: InternalMarker) {
        marker.ignore()
        val action = if (player == GoColor.BLACK) GameAction.PLAY_BLACK else GameAction.PLAY_WHITE
        if (updateGameAction.compareAndSet(this@GoGameFrame, null, action)) {
            val goban = manager.node.goban
            val allowSuicide = manager.gameInfo.rules.allowSuicide
            suicideRestrictions.clear()
            for(y in 0 until goban.height) for(x in 0 until goban.width) {
                val p = GoPoint(x, y)
                if (goban[p] != null) continue
                prototypeGoban.copyFrom(goban)
                prototypeGoban.play(p, player)
                if ((!allowSuicide && prototypeGoban[p] == null) || prototypeGoban contentEquals goban)
                    suicideRestrictions.add(p)
            }
            gobanView.goban = goban
            actionButton.isEnabled = true
            this@GoGameFrame.channel = channel
        }
    }

    internal fun update(marker: InternalMarker) {
        marker.ignore()
        val node = manager.node
        if (node is GoSGFMoveNode) {
            node.getSuperkoRestrictions(superkoRestrictions)
        } else {
            superkoRestrictions.clear()
        }
        if (gameAction != GameAction.COUNT_SCORE)
            scoreGoban = node.territory
        gobanView.goban = node.goban
        gobanView.repaint()
    }

    internal suspend fun acceptUndoMove(resumeNode: GoSGFNode, marker: InternalMarker): Boolean {
        marker.ignore()
        // TODO
        return false
    }

    internal fun startScoring(scoreManager: GoScoreManager, marker: InternalMarker) {
        marker.ignore()
        if (updateGameAction.compareAndSet(this, null, GameAction.COUNT_SCORE)) {
            this.scoreManager = scoreManager
            val goban = manager.node.goban
            prototypeGoban.copyFrom(goban)
            gobanView.goban = goban
            scoreGoban = prototypeGoban.getScoreGoban(manager.gameInfo.rules.territoryScore)
            gobanView.repaint()
            actionButton.text = gobanDesktopResources().getString("Score.Submit")
            actionButton.isEnabled = true
        }
    }

    internal fun updateScoring(
        scoreManager: GoScoreManager,
        stones: GoPointSet,
        alive: Boolean,
        marker: InternalMarker
    ) {
        marker.ignore()
        if (gameAction == GameAction.COUNT_SCORE && this.scoreManager === scoreManager) {
            if (alive) prototypeGoban.copyFrom(manager.node.goban, stones)
            else prototypeGoban.setAll(stones, null)
            scoreGoban = prototypeGoban.getScoreGoban(manager.gameInfo.rules.territoryScore, scoreGoban)
            gobanView.repaint()
            actionButton.isEnabled = true
        }
    }

    internal fun finishScoring(marker: InternalMarker) {
        marker.ignore()
        if (updateGameAction.compareAndSet(this, GameAction.COUNT_SCORE, null)) {
            scoreManager = null
            scoreGoban = manager.node.territory
            gobanView.repaint()
        }
    }

    fun requestResumePlay() {
        val scoreManager = this.scoreManager ?: return
        val blackPlayer = (manager.blackPlayer as? AbstractPlayer)?.takeIf { it.frame === this }
        val whitePlayer = manager.whitePlayer as? AbstractPlayer
        val player: AbstractPlayer = when {
            whitePlayer?.frame !== this -> blackPlayer ?: return
            blackPlayer == null -> whitePlayer
            else -> {
                val node = manager.node
                if ((node.turnPlayer == GoColor.WHITE) == (node is GoSGFMoveNode)) whitePlayer else blackPlayer
            }
        }
        manager.gameScope.launch(Dispatchers.IO) { player.requestResumePlay(scoreManager, InternalMarker) }
    }

    private val gobanView = object: GobanView(manager.node.goban) {

        var isShiftDown: Boolean = false

        init {
            enableEvents(AWTEvent.KEY_EVENT_MASK)
            val ma = object: MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) = goPointClicked(e)
                override fun mouseMoved(e: MouseEvent?) {
                    val p = toGoPoint(e)
                    var changed = false
                    val countScore = gameAction == GameAction.COUNT_SCORE
                    if (p != goCursor) {
                        goCursor = p
                        changed = true
                        if (countScore) goban?.let { goban ->
                            if (p != null && goban[p] != null)
                                goban.getGroup(p, goPointGroup)
                            else goPointGroup.clear()
                        }
                    }
                    if (e != null) {
                        val shift = e.isShiftDown
                        if (shift != isShiftDown) {
                            isShiftDown = shift
                            if (countScore) changed = true
                        }
                    }
                    if (changed) repaint()
                }
                override fun mouseEntered(e: MouseEvent?) {
                    mouseMoved(e)
                }
                override fun mouseExited(e: MouseEvent?) {
                    if (goCursor != null) {
                        goCursor = null
                        goPointGroup.clear()
                        repaint()
                    }
                    if (e != null) {
                        isShiftDown = e.isShiftDown
                    }
                }
            }
            addMouseListener(ma)
            addMouseMotionListener(ma)
            val ka = object: KeyAdapter() {
                override fun keyPressed(e: KeyEvent?) {
                    if (e?.keyCode == KeyEvent.VK_SHIFT) {
                        if (!isShiftDown) {
                            isShiftDown = true
                            repaint()
                        }
                    }
                }
                override fun keyReleased(e: KeyEvent?) {
                    if (e?.keyCode == KeyEvent.VK_SHIFT) {
                        if (isShiftDown) {
                            isShiftDown = false
                            repaint()
                        }
                    }
                }
            }
            addKeyListener(ka)
        }

        override fun getStoneColorAt(p: GoPoint): GoColor? {
            val goban = this.goban ?: return null
            val color = goban[p]
            if (color != null) return color
            if (p != goCursor || suicideRestrictions.contains(p) || superkoRestrictions.contains(p)) return null
            val action = gameAction ?: return null
            return when(action) {
                GameAction.PLAY_BLACK -> GoColor.BLACK
                GameAction.PLAY_WHITE -> GoColor.WHITE
                GameAction.COUNT_SCORE -> null
                // HANDICAP
                else -> if (goban.blackCount < handicap) GoColor.BLACK else null
            }
        }

        override fun getStoneAlphaAt(p: GoPoint): Float {
            val action = gameAction
            val goban = this.goban ?: return 1f
            return when {
                action == GameAction.COUNT_SCORE -> if(goban[p] == null) 1f else {
                    val alive = if (goPointGroup.contains(p)) isShiftDown else prototypeGoban[p] != null
                    if (alive) 1f else 0.5f
                }
                action == GameAction.HANDICAP -> {
                    when {
                        p != goCursor -> 1f
                        goban[p] != null -> 0.25f
                        goban.blackCount < handicap -> 0.5f
                        else -> 1f
                    }
                }
                action == null || p != goCursor -> {
                    val color = goban[p]
                    val territory = scoreGoban[p]
                    if (color == null || territory == null || color == territory) 1f
                    else 0.5f
                }
                // action == PLAY_BLACK or PLAY_WHITE
                goban[p] == null && !suicideRestrictions.contains(p) && !superkoRestrictions.contains(p) -> 0.5f
                else -> 1f
            }
        }

        override fun paintGoban(g: Graphics2D) {
            val goban = this.goban ?: return
            var rect: Rectangle2D.Double? = null
            for(y in 0 until goban.height) for(x in 0 until goban.width) {
                val color = scoreGoban[x, y] ?: continue
                g.paint = if (color == GoColor.BLACK) TRANSPARENT_BLACK else TRANSPARENT_WHITE
                if (rect == null) {
                    rect = Rectangle2D.Double()
                    rect.width = 1.0
                    rect.height = 1.0
                }
                rect.x = x - 0.5
                rect.y = y - 0.5
                g.fill(rect)
            }
            super.paintGoban(g)
            g.paint = foreground
            for(p in superkoRestrictions) {
                if (p.x < goban.width && p.y < goban.height && goban[p] == null) {
                    g.draw(getMarkupSquare(p))
                }
            }
            val p = (manager.node as? GoSGFMoveNode)?.playStoneAt
            if (p != null) {
                val color = goban[p]
                if (color == GoColor.BLACK) g.paint = Color.WHITE
                else if (color == GoColor.WHITE) g.paint = Color.BLACK
                g.draw(getMarkupCircle(p))
            }
        }

    }

    private val cardLayout = CardLayout()
    private val cardPanel = JPanel(cardLayout)
    private val actionButton = JButton(GoPoint.format(null, sgf.width, sgf.height))

    init {
        val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
        split.leftComponent = actionButton
        split.rightComponent = gobanView
        SwingUtilities.invokeLater {
            split.dividerLocation = 500
        }
        actionButton.isEnabled = false
        actionButton.addActionListener {
            val action = gameAction
            val channel: SendChannel<GoPoint?>?
            val scoreManager: GoScoreManager?
            val black: AbstractPlayer?
            val white: AbstractPlayer?
            if (action == GameAction.COUNT_SCORE) {
                channel = null
                scoreManager = this.scoreManager ?: return@addActionListener
                black = (manager.blackPlayer as? AbstractPlayer)?.takeIf { it.frame === this }
                white = (manager.whitePlayer as? AbstractPlayer)?.takeIf { it.frame === this }
                if (black == null && white == null) return@addActionListener
                actionButton.isEnabled = false
            } else {
                scoreManager = null
                black = null
                white = null
                if (action == GameAction.HANDICAP)
                    actionButton.text = GoPoint.format(null, sgf.width, sgf.height)
                gameAction = null
                gobanView.repaint()
                actionButton.isEnabled = false
                actionButton.text = GoPoint.format(null, sgf.width, sgf.height)
                channel = this.channel
                this.channel = null
                if (channel == null) return@addActionListener
            }
            manager.gameScope.launch(Dispatchers.IO) {
                channel?.send(null)
                if (scoreManager != null) {
                    black?.submitScore(scoreManager, InternalMarker)
                    white?.submitScore(scoreManager, InternalMarker)
                }
            }
        }
        contentPane = split
    }

    private var goCursor: GoPoint? = null

    private fun updateHandicapText(targetHandicap: Int, currentHandicap: Int) {
        val formatter = gobanDesktopFormatResources().getObject("GobanHandicapProgressFormatter")
                as GobanHandicapProgressFormatter
        actionButton.text = formatter.format(targetHandicap, currentHandicap)
    }

    private fun goPointClicked(e: MouseEvent?) {
        val isShiftDown = e?.isShiftDown ?: return
        val p = gobanView.toGoPoint(e) ?: return
        if (p != goCursor) return
        val action = gameAction ?: return
        when(action) {
            GameAction.HANDICAP -> {
                val goban = gobanView.goban as? Goban ?: return
                if (goban[p] != null) {
                    goban[p] = null
                } else if (goban.blackCount < handicap)
                    goban[p] = GoColor.BLACK
                gobanView.repaint()
                val targetHandicap = handicap
                val currentHandicap = goban.blackCount
                updateHandicapText(targetHandicap, currentHandicap)
                actionButton.isEnabled = currentHandicap == targetHandicap
            }
            GameAction.COUNT_SCORE -> {
                val scoreManager = this.scoreManager ?: return
                val channel = if (isShiftDown) scoreManager.livingStones else scoreManager.deadStones
                manager.gameScope.launch(Dispatchers.IO) {
                    channel.send(goPointGroup)
                }
            }
            // PLAY_BLACK, PLAY_WHITE
            else -> {
                val channel = this.channel
                this.channel = null
                gameAction = null
                actionButton.isEnabled = false
                if (channel != null) manager.gameScope.launch(Dispatchers.IO) {
                    channel.send(p)
                }
            }
        }
    }

}