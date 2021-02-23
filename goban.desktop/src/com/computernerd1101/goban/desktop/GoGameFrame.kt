package com.computernerd1101.goban.desktop

import com.computernerd1101.goban.*
import com.computernerd1101.goban.desktop.internal.InternalMarker
import com.computernerd1101.goban.desktop.resources.*
import com.computernerd1101.goban.players.*
import com.computernerd1101.goban.sgf.GoSGFMoveNode
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import java.awt.*
import java.awt.event.*
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
            frame.generateHandicapStones(handicap, goban)
        }

        override suspend fun requestMove(channel: SendChannel<GoPoint?>) = withContext(Dispatchers.IO) {
            frame.requestMove(color, channel)
        }

        override suspend fun update() = withContext(Dispatchers.IO) {
            frame.update()
        }

        override suspend fun startScoring(scoreManager: GoScoreManager) = withContext(Dispatchers.IO) {
            frame.startScoring(scoreManager)
        }

        override suspend fun updateScoring(
            scoreManager: GoScoreManager,
            stones: GoPointSet,
            alive: Boolean
        ) = withContext(Dispatchers.IO) {
            frame.updateScoring(scoreManager, stones, alive)
        }

        override suspend fun finishScoring() = withContext(Dispatchers.IO) {
            frame.finishScoring()
        }

        internal suspend fun sendFinishScoring(scoreManager: GoScoreManager, marker: InternalMarker) {
            marker.ignore()
            sendFinishScoring(scoreManager)
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
    }

    private val sgf = manager.sgf
    private val superkoRestrictions = MutableGoPointSet()
    private val suicideRestrictions = MutableGoPointSet()
    private val goPointGroup = MutableGoPointSet()
    private val prototypeGoban = Goban(sgf.width, sgf.height)

    @Volatile private var gameAction: GameAction? = null
    private var handicap = 0
    private var channel: SendChannel<GoPoint?>? = null
    private var scoreManager: GoScoreManager? = null

    suspend fun generateHandicapStones(handicap: Int, goban: Goban) {
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

    @Suppress("RedundantSuspendModifier")
    suspend fun requestMove(player: GoColor, channel: SendChannel<GoPoint?>) {
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

    @Suppress("RedundantSuspendModifier")
    suspend fun update() {
        val node = manager.node
        if (node is GoSGFMoveNode) {
            node.getSuperkoRestrictions(superkoRestrictions)
        } else {
            superkoRestrictions.clear()
        }
        gobanView.goban = node.goban
        gobanView.repaint()
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun startScoring(scoreManager: GoScoreManager) {
        if (updateGameAction.compareAndSet(this, null, GameAction.COUNT_SCORE)) {
            this.scoreManager = scoreManager
            val goban = manager.node.goban
            prototypeGoban.copyFrom(goban)
            gobanView.goban = goban
            gobanView.repaint()
            actionButton.text = gobanDesktopResources().getString("Score.Submit")
            actionButton.isEnabled = true
        }
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun updateScoring(scoreManager: GoScoreManager, stones: GoPointSet, alive: Boolean) {
        if (gameAction == GameAction.COUNT_SCORE && this.scoreManager === scoreManager) {
            if (alive) prototypeGoban.copyFrom(manager.node.goban, stones)
            else prototypeGoban.setAll(stones, null)
            gobanView.repaint()
            actionButton.isEnabled = true
        }
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun finishScoring() {
        if (updateGameAction.compareAndSet(this, GameAction.COUNT_SCORE, null)) {
            scoreManager = null
            gobanView.repaint()
        }
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
            val action = gameAction ?: return 1f
            val goban = this.goban ?: return 1f
            return when {
                action == GameAction.COUNT_SCORE -> if(goban[p] == null) 1f else {
                    val alive = if (goPointGroup.contains(p)) isShiftDown else prototypeGoban[p] != null
                    if (alive) 1f else 0.5f
                }
                p != goCursor -> 1f
                action == GameAction.HANDICAP -> {
                    when {
                        goban[p] != null -> 0.25f
                        goban.blackCount < handicap -> 0.5f
                        else -> 1f
                    }
                }
                // action == PLAY_BLACK or PLAY_WHITE
                else -> if (goban[p] == null && !suicideRestrictions.contains(p) &&
                    !superkoRestrictions.contains(p)) 0.5f else 1f
            }
        }

        override fun paintGoban(g: Graphics2D) {
            super.paintGoban(g)
            val goban = this.goban ?: return
            g.paint = foreground
            for(point in superkoRestrictions) {
                if (point.x < goban.width && point.y < goban.height && goban[point] == null) {
                    g.draw(getMarkupSquare(point))
                }
            }
        }

    }

    private val actionButton = JButton(GoPoint.format(null, sgf.width, sgf.height))

    init {
        actionButton.isEnabled = false
        actionButton.addActionListener {
            val action = gameAction
            val channel: SendChannel<GoPoint?>?
            val scoreManager: GoScoreManager?
            var black: AbstractPlayer? = null
            var white: AbstractPlayer? = null
            if (action == GameAction.COUNT_SCORE) {
                channel = null
                scoreManager = this.scoreManager ?: return@addActionListener
                black = manager.blackPlayer as? AbstractPlayer
                if (black?.frame !== this) black = null
                white = manager.whitePlayer as? AbstractPlayer
                if (white?.frame !== this) {
                    if (black == null) return@addActionListener
                    white = null
                }
                actionButton.isEnabled = false
            } else {
                scoreManager = null
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
                    black?.sendFinishScoring(scoreManager, InternalMarker)
                    white?.sendFinishScoring(scoreManager, InternalMarker)
                }
            }
        }
        layout = BorderLayout()
        add(gobanView, BorderLayout.CENTER)
        add(actionButton, BorderLayout.SOUTH)
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