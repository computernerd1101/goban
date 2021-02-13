package com.computernerd1101.goban.desktop

import com.computernerd1101.goban.*
import com.computernerd1101.goban.desktop.resources.*
import com.computernerd1101.goban.players.*
import com.computernerd1101.goban.sgf.GoSGFMoveNode
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import java.awt.*
import java.awt.event.*
import java.util.concurrent.atomic.AtomicReference
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

        override suspend fun generateHandicapStones(handicap: Int, goban: Goban) {
            frame.generateHandicapStones(handicap, goban)
        }

        override suspend fun requestMove(channel: SendChannel<GoPoint?>) {
            frame.requestMove(color, channel)
        }

        override suspend fun update() {
            frame.update()
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

        private val atomicFrame = AtomicReference<GoGameFrame>()

        var frame: GoGameFrame
            get() = atomicFrame.get() ?:
                throw UninitializedPropertyAccessException("property frame has not been initialized")
            set(frame) {
                if (!atomicFrame.compareAndSet(null, frame))
                    throw IllegalStateException("property frame has already been initialized")
            }
    }

    private enum class GameAction {
        HANDICAP,
        PLAY_BLACK,
        PLAY_WHITE
    }

    private val sgf = manager.sgf
    private val superkoRestrictions = MutableGoPointSet()
    private val suicideRestrictions = MutableGoPointSet()
    private val prototypeGoban = Goban(sgf.width, sgf.height)

    private var gameAction = AtomicReference<GameAction?>(null)
    private var handicap = 0
    private var channel: SendChannel<GoPoint?>? = null

    suspend fun generateHandicapStones(handicap: Int, goban: Goban) {
        val blackCount = goban.blackCount
        if (blackCount > handicap)
            goban.clear()
        else if (goban.whiteCount > 0)
            goban.clear(GoColor.WHITE)
        withContext(Dispatchers.IO) {
            if (gameAction.compareAndSet(null, GameAction.HANDICAP)) {
                superkoRestrictions.clear()
                this@GoGameFrame.handicap = handicap
                gobanView.goban = goban
                updateHandicapText(handicap, blackCount)
                actionButton.isEnabled = blackCount == handicap
                val channel = Channel<GoPoint?>(1)
                this@GoGameFrame.channel = channel
                channel.receive()
                channel.close(null)
            }
        }
    }

    suspend fun requestMove(player: GoColor, channel: SendChannel<GoPoint?>): Unit = withContext(Dispatchers.IO) {
        val action = if (player == GoColor.BLACK) GameAction.PLAY_BLACK else GameAction.PLAY_WHITE
        if (gameAction.compareAndSet(null, action)) {
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

    suspend fun update() = withContext(Dispatchers.IO) {
        val node = manager.node
        if (node is GoSGFMoveNode) {
            node.getSuperkoRestrictions(superkoRestrictions)
        } else {
            superkoRestrictions.clear()
        }
        gobanView.goban = node.goban
        gobanView.repaint()
    }

    private val gobanView = object: GobanView(manager.node.goban) {

        init {
            val ma = object: MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) = goPointClicked(e)
                override fun mouseMoved(e: MouseEvent?) {
                    val p = toGoPoint(e)
                    if (p != goCursor) {
                        goCursor = p
                        repaint()
                    }
                }
                override fun mouseExited(e: MouseEvent?) {
                    if (goCursor != null) {
                        goCursor = null
                        repaint()
                    }
                }
            }
            addMouseListener(ma)
            addMouseMotionListener(ma)
        }

        override fun getStoneColorAt(p: GoPoint): GoColor? {
            val goban = this.goban ?: return null
            val color = goban[p]
            if (color != null) return color
            if (p != goCursor || suicideRestrictions.contains(p) || superkoRestrictions.contains(p)) return null
            val action = gameAction.get() ?: return null
            return when(action) {
                GameAction.PLAY_BLACK -> GoColor.BLACK
                GameAction.PLAY_WHITE -> GoColor.WHITE
                // HANDICAP
                else -> if (goban.blackCount < handicap) GoColor.BLACK else null
            }
        }

        override fun getStoneAlphaAt(p: GoPoint): Float {
            if (p != goCursor) return 1f
            val action = gameAction.get() ?: return 1f
            val goban = this.goban ?: return 1f
            return when(action) {
                GameAction.HANDICAP -> {
                    when {
                        goban[p] != null -> 0.25f
                        goban.blackCount < handicap -> 0.5f
                        else -> 1f
                    }
                }
                // PLAY_BLACK, PLAY_WHITE
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

    private val actionButton = JButton(GoPoint.formatPoint(null, sgf.width, sgf.height))

    init {
        actionButton.isEnabled = false
        actionButton.addActionListener {
            if (gameAction.get() == GameAction.HANDICAP)
                actionButton.text = GoPoint.formatPoint(null, sgf.width, sgf.height)
            gameAction.set(null)
            gobanView.repaint()
            actionButton.isEnabled = false
            actionButton.text = GoPoint.formatPoint(null, sgf.width, sgf.height)
            val channel = this.channel
            this.channel = null
            if (channel != null) manager.gameScope.launch(Dispatchers.IO) {
                channel.send(null)
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
        val p = gobanView.toGoPoint(e) ?: return
        if (p != goCursor) return
        val action = gameAction.get() ?: return
        when(action) {
            GameAction.HANDICAP -> {
                val goban = gobanView.goban as? Goban
                if (goban != null) {
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
            }
            // PLAY_BLACK, PLAY_WHITE
            else -> {
                val channel = this.channel
                this.channel = null
                gameAction.set(null)
                actionButton.isEnabled = false
                if (channel != null) manager.gameScope.launch(Dispatchers.IO) {
                    channel.send(p)
                }
            }
        }
    }

}