package com.computernerd1101.goban.desktop

import com.computernerd1101.goban.*
import com.computernerd1101.goban.desktop.resources.*
import com.computernerd1101.goban.players.*
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

    abstract class Player(manager: GoPlayerManager, color: GoColor): GoPlayer(manager, color) {

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

    private enum class GameAction {
        HANDICAP,
        PLAY_BLACK,
        PLAY_WHITE
    }

    private val sgf = manager.sgf

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
            gobanView.goban = manager.node.goban
            actionButton.isEnabled = true
            this@GoGameFrame.channel = channel
        }
    }

    suspend fun update() = withContext(Dispatchers.IO) {
        gobanView.goban = manager.node.goban
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
            if (p != goCursor) return null
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
                else -> if (goban[p] == null) 0.5f else 1f
            }
        }

    }

    private val actionButton = JButton(GoPoint.formatPoint(null, sgf.width, sgf.height))

    init {
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