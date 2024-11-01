package com.computernerd1101.goban.desktop

import com.computernerd1101.goban.*
import com.computernerd1101.goban.desktop.internal.InternalMarker
import com.computernerd1101.goban.desktop.resources.*
import com.computernerd1101.goban.players.*
import com.computernerd1101.goban.sgf.GoSGFMoveNode
import com.computernerd1101.goban.sgf.GoSGFNode
import com.computernerd1101.goban.time.TimeEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import java.awt.*
import java.awt.event.*
import java.awt.geom.Rectangle2D
import java.util.*
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
import javax.swing.*
import javax.swing.Timer
import kotlin.coroutines.*
import kotlin.math.min

class GoGameFrame private constructor(
    game: GoGameManager?,
    context: CoroutineContext,
    resources: ResourceBundle
): JFrame() {

    constructor(): this(null, Dispatchers.Swing, gobanDesktopResources())

    constructor(goGame: GoGameManager): this(goGame, Dispatchers.Swing, gobanDesktopResources())

    constructor(job: Job): this(null, job + Dispatchers.Swing, gobanDesktopResources())

    constructor(goGame: GoGameManager, job: Job): this(goGame, job + Dispatchers.Swing, gobanDesktopResources())

    constructor(setup: GoGameSetup): this(GoGameManager(setup, PlayerFactory)) {
        initPlayers()
    }

    constructor(setup: GoGameSetup, job: Job): this(GoGameManager(setup, PlayerFactory), job) {
        initPlayers()
    }

    private fun initPlayers() {
        (blackPlayer as? Player)?.initFrame(this)
        (whitePlayer as? Player)?.initFrame(this)
    }

    val goGame: GoGameManager = game ?: GoGameManager { _game, color -> Player(_game, color, this) }
    val blackPlayer: GoPlayer get() = goGame.blackPlayer
    val whitePlayer: GoPlayer get() = goGame.whitePlayer

    val scope: CoroutineScope = CoroutineScope(context)

    @Suppress("unused")
    val context: CoroutineContext get() = scope.coroutineContext

    init {
        title = "CN13 Goban"
        defaultCloseOperation = DISPOSE_ON_CLOSE
        setSize(1000, 500)
        extendedState = MAXIMIZED_BOTH
        setLocationRelativeTo(null)
    }

    class Player: GoPlayer {

        constructor(game: GoGameManager, color: GoColor): super(game, color) {
            _frame = null
        }

        constructor(game: GoGameManager, color: GoColor, frame: GoGameFrame): super(game, color) {
            _frame = frame
        }

        @Volatile private var _frame: GoGameFrame?

        val frame: GoGameFrame
            get() = _frame ?: throw UninitializedPropertyAccessException("property frame has not been initialized")

        val isFrameInitialized: Boolean get() = _frame != null

        fun initFrame(frame: GoGameFrame): GoGameFrame {
            val current = _frame
            if (current != null) return current
            if (FRAME.compareAndSet(this, null, frame)) {
                val resources = gobanDesktopResources()
                frame.initLayout(resources, InternalMarker)
                val opponent: GoGameFrame? =
                    ((if (color.isBlack) frame.whitePlayer else frame.blackPlayer) as? Player)?._frame
                if (opponent != null && opponent !== frame)
                    opponent.initLayout(resources, InternalMarker)
                return frame
            }
            return this.frame
        }

        private companion object {

            private val FRAME: AtomicReferenceFieldUpdater<Player, GoGameFrame?> =
                AtomicReferenceFieldUpdater.newUpdater(
                    Player::class.java,
                    GoGameFrame::class.java,
                    "_frame"
                )

        }

        override suspend fun generateHandicapStones(handicap: Int, goban: Goban) {
            frame.generateHandicapStones(handicap, goban, InternalMarker)
        }

        override suspend fun acceptHandicapStones(goban: Goban): Boolean {
            frame.acceptHandicapStones(InternalMarker)
            return true
        }

        override suspend fun generateMove(): GoPoint? {
            return frame.generateMove(color, InternalMarker)
        }

        override suspend fun update() {
            frame.update(InternalMarker)
        }

        internal suspend fun requestUndoMove(resumeNode: GoSGFNode, marker: InternalMarker): Boolean {
            marker.ignore()
            return requestUndoMove(resumeNode)
        }

        override suspend fun acceptUndoMove(resumeNode: GoSGFNode): Boolean {
            // resumeNode can be a setup node, but everything after that up to and including currentNode
            // must be a move node, and everything between but excluding resumeNode and currentNode
            // must have a different turn player than currentNode.
            val currentNode = game.node as? GoSGFMoveNode ?: return false
            if (currentNode === resumeNode) return false
            var parentNode: GoSGFNode = currentNode.parent ?: return false
            while(parentNode !== resumeNode) {
                if (parentNode !is GoSGFMoveNode || parentNode.turnPlayer == currentNode.turnPlayer)
                    return false
                parentNode = parentNode.parent ?: return false
            }
            return (opponent as? Player)?.frame === frame ||
                frame.acceptUndoMove(color, InternalMarker)
        }

        override fun requestOpponentTimeExtension(requestedMilliseconds: Long): Long {
            val frame = this.frame
            return if (requestedMilliseconds > 0L && ((opponent as? Player)?.frame === frame))
                extendOpponentTime(InternalMarker)
            else {
                frame.displayAddOneMinuteStatus(color.opponent, -1, InternalMarker)
                0L
            }
        }

        internal fun extendOpponentTime(marker: InternalMarker): Long {
            marker.ignore()
            return extendOpponentTime(ONE_MINUTE_IN_MILLIS)
        }

        override fun filterTimeExtension(extension: Long): Long {
            if (extension <= 0L) return 0L
            _frame?.displayAddOneMinuteStatus(color, 1, InternalMarker)
            return min(extension, ONE_MINUTE_IN_MILLIS)
        }

        override fun onTimeEvent(player: GoPlayer, e: TimeEvent) {
            _frame?.onTimeEvent(player, e, gobanDesktopResources(), InternalMarker)
        }

        internal fun resign(marker: InternalMarker) {
            marker.ignore()
            resign()
        }

        override fun startScoring() {
            frame.startScoring(InternalMarker)
        }

        override fun updateScoring(stones: GoPointSet, alive: Boolean) {
            frame.updateScoring(stones, alive, InternalMarker)
        }

        override fun finishScoring() {
            frame.finishScoring(InternalMarker)
        }

        internal fun submitScore(marker: InternalMarker) {
            marker.ignore()
            submitScore()
        }

        internal fun requestResumePlay(marker: InternalMarker) {
            marker.ignore()
            requestResumePlay()
        }

    }

    private enum class GameAction {
        HANDICAP,
        PLAY_BLACK,
        PLAY_WHITE,
        COUNT_SCORE
    }

    @Suppress("UNCHECKED_CAST")
    companion object PlayerFactory: GoPlayer.Factory {

        override fun createPlayer(game: GoGameManager, color: GoColor): GoPlayer = Player(game, color)

        private val GAME_ACTION: AtomicReferenceFieldUpdater<GoGameFrame, GameAction?> =
            AtomicReferenceFieldUpdater.newUpdater(
                GoGameFrame::class.java, GameAction::class.java, "gameAction"
            )

        private val UNDO_CONTINUATION:
                AtomicReferenceFieldUpdater<GoGameFrame, Continuation<Boolean>?> =
            AtomicReferenceFieldUpdater.newUpdater(
                GoGameFrame::class.java,
                Continuation::class.java as Class<Continuation<Boolean>>,
                "undoContinuation"
            )

        private val DEFERRED_REQUEST_UNDO_MOVE: AtomicReferenceFieldUpdater<GoGameFrame, Deferred<Boolean>?> =
            AtomicReferenceFieldUpdater.newUpdater(
                GoGameFrame::class.java,
                Deferred::class.java as Class<Deferred<Boolean>>,
                "deferredRequestUndoMove"
            )

        private val INIT_LAYOUT_ONCE: AtomicIntegerFieldUpdater<GoGameFrame> =
            AtomicIntegerFieldUpdater.newUpdater(GoGameFrame::class.java, "initLayoutOnce")

        private val BLACK_ADD_ONE_MINUTE: AtomicReferenceFieldUpdater<GoGameFrame, Timer?> =
            AtomicReferenceFieldUpdater.newUpdater(
                GoGameFrame::class.java,
                Timer::class.java,
                "blackAddOneMinute"
            )

        private val WHITE_ADD_ONE_MINUTE: AtomicReferenceFieldUpdater<GoGameFrame, Timer?> =
            AtomicReferenceFieldUpdater.newUpdater(
                GoGameFrame::class.java,
                Timer::class.java,
                "whiteAddOneMinute"
            )

        private val RESIGN_TIMER: AtomicReferenceFieldUpdater<GoGameFrame, Timer?> =
            AtomicReferenceFieldUpdater.newUpdater(
                GoGameFrame::class.java,
                Timer::class.java,
                "resignTimer"
            )

        const val ONE_MINUTE_IN_MILLIS: Long = 60000L

        @JvmField val TRANSPARENT_BLACK = Color(0x7F000000, true)
        @JvmField val TRANSPARENT_WHITE = Color(0x7FFFFFFF, true)

        private val southBorder = BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY)
    }

    private val sgf = goGame.sgf
    private val superkoRestrictions = MutableGoPointSet()
    private val suicideRestrictions = MutableGoPointSet()
    private val goPointGroup = MutableGoPointSet()
    private val prototypeGoban = Goban(sgf.width, sgf.height)
    private var scoreGoban: MutableGoban = goGame.node.territory
    private val allPoints = GoRectangle(0, 0, sgf.width - 1, sgf.height - 1)

    @Volatile private var gameAction: GameAction? = null
    private var handicap = 0
    private var continuation: Continuation<GoPoint?>? = null
    //private var scoreManager: GoScoreManager? = null

    internal suspend fun generateHandicapStones(
        handicap: Int,
        goban: Goban,
        marker: InternalMarker
    ) {
        marker.ignore()
        cardLayout.show(cardPanel, "Handicap")
        val blackCount = goban.blackCount
        if (blackCount > handicap)
            goban.clear()
        else if (goban.whiteCount > 0)
            goban.clear(GoColor.WHITE)
        if (!GAME_ACTION.compareAndSet(this, null, GameAction.HANDICAP))
            throw IllegalStateException("Cannot generate handicap stones while another operation is pending.")
        superkoRestrictions.clear()
        this.handicap = handicap
        gobanView.goban = goban
        actionButton.text = gobanDesktopResources().getString("Handicap.Finalize")
        updateHandicapText(handicap, blackCount)
        suspendCoroutine<GoPoint?> { continuation = it }
        cardLayout.show(cardPanel, "Moves")
    }

    internal fun acceptHandicapStones(marker: InternalMarker) {
        marker.ignore()
        cardLayout.show(cardPanel, "Moves")
    }

    internal suspend fun generateMove(player: GoColor, marker: InternalMarker): GoPoint? {
        marker.ignore()
        val action = if (player == GoColor.BLACK) GameAction.PLAY_BLACK else GameAction.PLAY_WHITE
        if (!GAME_ACTION.compareAndSet(this, null, action))
            throw IllegalStateException("Cannot request move while another operation is pending.")
        val goban = goGame.node.goban
        val allowSuicide = goGame.gameInfo.rules.allowSuicide
        suicideRestrictions.clear()
        for (y in 0 until goban.height) for (x in 0 until goban.width) {
            val p = GoPoint(x, y)
            if (goban[p] != null) continue
            prototypeGoban.copyFrom(goban)
            prototypeGoban.play(p, player)
            if ((!allowSuicide && prototypeGoban[p] == null) || prototypeGoban contentEquals goban)
                suicideRestrictions.add(p)
        }
        gobanView.goban = goban
        actionButton.isEnabled = true
        return suspendCoroutine { continuation = it }
    }

    internal fun update(marker: InternalMarker) {
        marker.ignore()
        val node = goGame.node
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

    private fun cancelPlayAction(marker: InternalMarker): Boolean {
        marker.ignore()
        while(true) {
            val action = gameAction
            if (action != GameAction.PLAY_BLACK && action != GameAction.PLAY_WHITE) return false
            if (GAME_ACTION.compareAndSet(this, action, null)) return true
        }
    }

    @Volatile private var deferredRequestUndoMove: Deferred<Boolean>? = null

    fun requestUndoMove() {
        val player: Player
        var resumeNode: GoSGFNode?
        val node = goGame.node as? GoSGFMoveNode ?: return
        val black = (blackPlayer as? Player)?.takeIf { it.frame === this }
        val white = whitePlayer as? Player
        when {
            white?.frame !== this -> {
                player = black ?: return
                resumeNode = node
            }
            black == null -> {
                player = white
                resumeNode = node
            }
            else -> {
                resumeNode = node.parent ?: return
                player = if (node.turnPlayer == GoColor.BLACK) black else white
            }
        }
        if (resumeNode === node) {
            while(resumeNode is GoSGFMoveNode && resumeNode.turnPlayer != player.color) {
                resumeNode = resumeNode.parent
            }
            resumeNode = (resumeNode as? GoSGFMoveNode)?.parent
        }
        if (resumeNode == null) {
            displayRequestUndoStatus(-1, InternalMarker)
            return
        }
        displayRequestUndoStatus(0, InternalMarker)
        var deferred: Deferred<Boolean>? = null
        // Make absolutely sure that the captured local variable deferred
        // is initialized before the coroutine starts
        deferred = scope.async(start = CoroutineStart.LAZY) {
            val response = player.requestUndoMove(resumeNode, InternalMarker)
            val mostRecent = DEFERRED_REQUEST_UNDO_MOVE.compareAndSet(this@GoGameFrame, deferred, null)
            when {
                response -> {
                    displayRequestUndoStatus(1, InternalMarker)
                    if (cancelPlayAction(InternalMarker))
                        actionButton.isEnabled = false
                }
                mostRecent -> {
                    displayRequestUndoStatus(-1, InternalMarker)
                }
            }
            response
        }
        deferredRequestUndoMove = deferred
        deferred.start()
    }

    @Suppress("unused")
    @Volatile private var undoContinuation: Continuation<Boolean>? = null

    internal suspend fun acceptUndoMove(player: GoColor, marker: InternalMarker): Boolean {
        marker.ignore()
        val allowed: Boolean = suspendCoroutine { continuation ->
            val oldContinuation = UNDO_CONTINUATION.getAndSet(this, continuation)
            if (oldContinuation != null) oldContinuation.resume(false)
            else panelOpponentRequestUndo.isVisible = true
        }
        if (allowed) {
            val cancelAction: GameAction =
                if (player == GoColor.BLACK) GameAction.PLAY_BLACK
                else GameAction.PLAY_WHITE
            if (GAME_ACTION.compareAndSet(this, cancelAction, null))
                actionButton.isEnabled = false
        }
        return allowed
    }

    private fun displayRequestUndoStatus(status: Int, marker: InternalMarker) {
        marker.ignore()
        val label = labelRequestUndo
        val key: String = if (status == 0) {
            label.background = Color.YELLOW
            label.foreground = Color.BLACK
            "Request.Waiting"
        } else {
            label.foreground = Color.WHITE
            if (status < 0) {
                label.background = Color.RED
                "Request.Denied"
            } else {
                label.background = Color.GREEN
                "Request.Allowed"
            }
        }
        label.text = gobanDesktopResources().getString(key)
        label.isVisible = true
        if (status != 0) {
            val updater = DEFERRED_REQUEST_UNDO_MOVE
            scope.launch {
                delay(1000L)
                if (updater[this@GoGameFrame] == null)
                    label.isVisible = false
            }
        }
    }

    internal fun startScoring(marker: InternalMarker) {
        marker.ignore()
        if (GAME_ACTION.compareAndSet(this, null, GameAction.COUNT_SCORE)) {
            val goban = goGame.node.goban
            prototypeGoban.copyFrom(goban)
            gobanView.goban = goban
            scoreGoban = prototypeGoban.getScoreGoban(goGame.gameInfo.rules.territoryScore)
            gobanView.repaint()
            actionButton.text = gobanDesktopResources().getString("Score.Submit")
            actionButton.isEnabled = true
        }
    }

    internal fun updateScoring(stones: GoPointSet, alive: Boolean, marker: InternalMarker) {
        marker.ignore()
        if (gameAction == GameAction.COUNT_SCORE) {
            if (alive) prototypeGoban.copyFrom(goGame.node.goban, stones)
            else prototypeGoban.setAll(stones, null)
            scoreGoban = prototypeGoban.getScoreGoban(goGame.gameInfo.rules.territoryScore, scoreGoban)
            gobanView.repaint()
            actionButton.isEnabled = true
        }
    }

    internal fun finishScoring(marker: InternalMarker) {
        marker.ignore()
        if (GAME_ACTION.compareAndSet(this, GameAction.COUNT_SCORE, null)) {
            scoreGoban = goGame.node.territory
            gobanView.repaint()
        }
    }

    fun requestResumePlay() {
        val blackPlayer = (this.blackPlayer as? Player)?.takeIf { it.frame === this }
        val whitePlayer = this.whitePlayer as? Player
        val player: Player = when {
            whitePlayer?.frame !== this -> blackPlayer ?: return
            blackPlayer == null -> whitePlayer
            else -> {
                val node = goGame.node
                if ((node.turnPlayer == GoColor.WHITE) == (node is GoSGFMoveNode)) whitePlayer else blackPlayer
            }
        }
        player.requestResumePlay(InternalMarker)
    }

    private val gobanView = object: GobanView(goGame.node.goban) {

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
                action == GameAction.HANDICAP -> when {
                    p != goCursor -> 1f
                    goban[p] != null -> 0.25f
                    goban.blackCount < handicap -> 0.5f
                    else -> 1f
                }
                action == null || p != goCursor -> {
                    val color = goban[p]
                    val territory = scoreGoban[p]
                    if (color == null || territory == null || color == territory) 1f
                    else 0.5f
                }
                // action == PLAY_BLACK or PLAY_WHITE
                goban[p] == null && p !in suicideRestrictions && p !in superkoRestrictions -> 0.5f
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
            val node = goGame.node
            val p = (node as? GoSGFMoveNode)?.playStoneAt
            if (p != null) {
                val color = goban[p]?.opponent ?: node.turnPlayer
                g.paint = if (color == GoColor.BLACK) Color.BLACK else Color.WHITE
                g.draw(getMarkupCircle(p))
            }
        }

    }

    private val panelOpponentRequestUndo = JPanel(GridLayout(2, 1))
    private val labelOpponentRequestUndo = JLabel(resources.getString("UndoMove.Request"))
    private val buttonAllowUndo = JButton(resources.getString("UndoMove.Allow"))
    private val buttonDenyUndo = JButton(resources.getString("UndoMove.Deny"))

    init {
        val requestActionListener = ActionListener { event ->
            val allow: Boolean = when(event.source) {
                buttonAllowUndo -> true
                buttonDenyUndo -> false
                else -> return@ActionListener
            }
            val continuation = UNDO_CONTINUATION.getAndSet(this, null)
            panelOpponentRequestUndo.isVisible = false
            continuation?.resume(allow)
        }
        buttonAllowUndo.addActionListener(requestActionListener)
        buttonDenyUndo.addActionListener(requestActionListener)
        panelOpponentRequestUndo.background = Color.RED
        labelOpponentRequestUndo.foreground = Color.WHITE
        labelOpponentRequestUndo.horizontalAlignment = SwingConstants.CENTER
        panelOpponentRequestUndo.add(labelOpponentRequestUndo)
        val buttonPanel = JPanel(GridLayout(1, 2))
        buttonPanel.add(buttonAllowUndo)
        buttonPanel.add(buttonDenyUndo)
        panelOpponentRequestUndo.add(buttonPanel)
        panelOpponentRequestUndo.isVisible = false
    }

    private val cardLayout = CardLayout()
    private val cardPanel = JPanel(cardLayout)

    private val handicapPanel = JPanel(BorderLayout())
    private val labelHandicapTarget = JLabel(resources.getString("Handicap.Target"))
    private val displayHandicapTarget = JLabel()
    private val labelHandicapCurrent = JLabel(resources.getString("Handicap.Current"))
    private val displayHandicapCurrent = JLabel()
    private val buttonHandicapInvert = JButton(resources.getString("Handicap.Invert"))
    private val displayHandicapInvert = JLabel()
    private val displayHandicapInvertNote = JTextPane()

    init {
        labelHandicapTarget.horizontalAlignment = SwingConstants.RIGHT
        val panel = JPanel(GridLayout(3, 2, 10, 10))
        panel.add(labelHandicapTarget)
        panel.add(displayHandicapTarget)
        labelHandicapCurrent.horizontalAlignment = SwingConstants.RIGHT
        panel.add(labelHandicapCurrent)
        panel.add(displayHandicapCurrent)
        buttonHandicapInvert.addActionListener { invertHandicap() }
        panel.add(buttonHandicapInvert)
        panel.add(displayHandicapInvert)
        handicapPanel.add(panel, BorderLayout.NORTH)
        displayHandicapInvertNote.text = resources.getString("Handicap.Invert.Note")
        displayHandicapInvertNote.isEditable = false
        displayHandicapInvertNote.isOpaque = false
        handicapPanel.add(displayHandicapInvertNote, BorderLayout.CENTER)
        cardPanel.add(handicapPanel, "Handicap")
    }

    private val gamePanel = JPanel(GridLayout(10, 2, 10, 0))
    private val labelBlackTime = JLabel()
    private val displayBlackTime = JLabel()
    private val labelWhiteTime = JLabel()
    private val displayWhiteTime = JLabel()
    private val labelBlackOvertime = JLabel()
    private val displayBlackOvertime = JLabel()
    private val labelWhiteOvertime = JLabel()
    private val displayWhiteOvertime = JLabel()
    private val buttonBlackAddOneMinute = JButton()
    private val labelBlackAddOneMinute = JLabel(resources.getString("Request.Waiting"))
    private val buttonWhiteAddOneMinute = JButton()
    private val labelWhiteAddOneMinute = JLabel(resources.getString("Request.Waiting"))
    private val labelBlackScore = JLabel(resources.getString("Score.Black"))
    private val displayBlackScore = JLabel()
    private val labelWhiteScore = JLabel(resources.getString("Score.White"))
    private val displayWhiteScore = JLabel()
    private val buttonRequestUndo = JButton(resources.getString("UndoMove"))
    private val labelRequestUndo = JLabel(resources.getString("Request.Waiting"))
    private val buttonResign = JButton(resources.getString("Resign"))
    private val labelResign = JLabel(resources.getString("Resign.Confirm"))

    private val timeLimitFormatter =
        gobanDesktopFormatResources().getObject("TimeLimitFormatter") as TimeLimitFormatter

    init {
        val none = resources.getString("TimeRemaining.None")
        labelBlackTime.background = Color.RED // no effect unto isOpaque is set to true
        labelBlackTime.horizontalAlignment = SwingConstants.RIGHT
        labelWhiteTime.background = Color.RED
        labelWhiteTime.horizontalAlignment = SwingConstants.RIGHT
        labelBlackOvertime.horizontalAlignment = SwingConstants.RIGHT
        labelWhiteOvertime.horizontalAlignment = SwingConstants.RIGHT
        var timeEvent = blackPlayer.timeEvent
        if (timeEvent == null) {
            displayBlackTime.foreground = Color.DARK_GRAY
            displayBlackTime.text = none
        } else {
            displayBlackTime.isOpaque = true
            displayBlackTime.background = Color.BLACK
            displayBlackTime.foreground = Color.WHITE
            onTimeEvent(whitePlayer, timeEvent, resources, InternalMarker)
        }
        timeEvent = whitePlayer.timeEvent
        if (timeEvent == null) {
            displayWhiteTime.foreground = Color.DARK_GRAY
            displayWhiteTime.text = none
        } else {
            displayWhiteTime.isOpaque = true
            displayWhiteTime.background = Color.WHITE
            displayWhiteTime.foreground = Color.BLACK
            onTimeEvent(whitePlayer, timeEvent, resources, InternalMarker)
        }
        val overtime = goGame.gameInfo.overtime
        if (overtime == null) {
            displayBlackOvertime.foreground = Color.DARK_GRAY
            displayWhiteOvertime.foreground = Color.DARK_GRAY
            displayBlackOvertime.text = none
            displayWhiteOvertime.text = none
        } else {
            displayBlackOvertime.isOpaque = true
            displayBlackOvertime.background = Color.BLACK
            displayBlackOvertime.foreground = Color.WHITE
            displayWhiteOvertime.isOpaque = true
            displayWhiteOvertime.background = Color.WHITE
            displayWhiteOvertime.foreground = Color.BLACK
        }
        labelBlackAddOneMinute.isOpaque = true
        labelBlackAddOneMinute.isVisible = false
        labelWhiteAddOneMinute.isOpaque = true
        labelWhiteAddOneMinute.isVisible = false
        labelBlackScore.horizontalAlignment = SwingConstants.RIGHT
        displayBlackScore.isOpaque = true
        displayBlackScore.background = Color.BLACK
        displayBlackScore.foreground = Color.WHITE
        labelWhiteScore.horizontalAlignment = SwingConstants.RIGHT
        displayWhiteScore.isOpaque = true
        displayWhiteScore.background = Color.WHITE
        displayWhiteScore.foreground = Color.BLACK
        buttonRequestUndo.addActionListener { requestUndoMove() }
        labelRequestUndo.isOpaque = true
        labelRequestUndo.isVisible = false
        labelResign.isOpaque = true
        labelResign.background = Color.RED
        labelResign.foreground = Color.WHITE
        labelResign.isVisible = false
        initLayout(resources, InternalMarker)
        cardPanel.add(gamePanel, "Moves")
        cardLayout.show(cardPanel, "Moves")
    }

    @Volatile private var initLayoutOnce: Int = 0

    internal fun initLayout(resources: ResourceBundle, marker: InternalMarker) {
        marker.ignore()
        if (initLayoutOnce != 0) return
        val blackPlayer = this.blackPlayer
        val isBlackLocal: Boolean = if (blackPlayer is Player) {
            if (!blackPlayer.isFrameInitialized) return
            blackPlayer.frame === this
        } else false
        val whitePlayer = this.whitePlayer
        val isWhiteLocal: Boolean = if (whitePlayer is Player) {
            if (!whitePlayer.isFrameInitialized) return
            whitePlayer.frame === this
        } else false
        if (!INIT_LAYOUT_ONCE.compareAndSet(this, 0, 1)) return
        val blackTimeRemainingKey: String
        val whiteTimeRemainingKey: String
        val blackAddOneMinuteKey: String
        val whiteAddOneMinuteKey: String
        when {
            isBlackLocal && !isWhiteLocal -> {
                blackTimeRemainingKey = "TimeRemaining.You"
                whiteTimeRemainingKey = "TimeRemaining.Opponent"
                blackAddOneMinuteKey = "TimeRemaining.AddOneMinute.Request"
                whiteAddOneMinuteKey = "TimeRemaining.AddOneMinute.Opponent"
                buttonBlackAddOneMinute.addActionListener(requestAddOneMinuteListener(opponent = whitePlayer))
                buttonWhiteAddOneMinute.addActionListener(addOneMinuteListener(opponent = blackPlayer))
            }
            !isBlackLocal && isWhiteLocal -> {
                blackTimeRemainingKey = "TimeRemaining.Opponent"
                whiteTimeRemainingKey = "TimeRemaining.You"
                blackAddOneMinuteKey = "TimeRemaining.AddOneMinute.Opponent"
                whiteAddOneMinuteKey = "TimeRemaining.AddOneMinute.Request"
                buttonBlackAddOneMinute.addActionListener(addOneMinuteListener(opponent = whitePlayer))
                buttonWhiteAddOneMinute.addActionListener(requestAddOneMinuteListener(opponent = blackPlayer))
            }
            else -> {
                blackTimeRemainingKey = "TimeRemaining.Black"
                whiteTimeRemainingKey = "TimeRemaining.White"
                blackAddOneMinuteKey = "TimeRemaining.AddOneMinute.Black"
                whiteAddOneMinuteKey = "TimeRemaining.AddOneMinute.White"
                if (isBlackLocal) {
                    buttonBlackAddOneMinute.addActionListener(addOneMinuteListener(opponent = whitePlayer))
                    buttonWhiteAddOneMinute.addActionListener(addOneMinuteListener(opponent = blackPlayer))
                } else {
                    buttonBlackAddOneMinute.isEnabled = false
                    buttonWhiteAddOneMinute.isEnabled = false
                }
            }
        }
        labelBlackTime.text = resources.getString(blackTimeRemainingKey)
        labelWhiteTime.text = resources.getString(whiteTimeRemainingKey)
        buttonBlackAddOneMinute.text = resources.getString(blackAddOneMinuteKey)
        buttonWhiteAddOneMinute.text = resources.getString(whiteAddOneMinuteKey)
        if (isBlackLocal || isWhiteLocal)
            buttonResign.addActionListener { resign(InternalMarker) }
        else buttonResign.isEnabled = false
        val components: Array<Array<JComponent>> = arrayOf(
            arrayOf(
                labelBlackTime, displayBlackTime,
                labelBlackOvertime, displayBlackOvertime,
                buttonBlackAddOneMinute, labelBlackAddOneMinute
            ),
            arrayOf(
                labelWhiteTime, displayWhiteTime,
                labelWhiteOvertime, displayWhiteOvertime,
                buttonWhiteAddOneMinute, labelWhiteAddOneMinute
            ),
            arrayOf(labelBlackScore, displayBlackScore),
            arrayOf(buttonRequestUndo, labelRequestUndo),
            arrayOf(labelWhiteScore, displayWhiteScore),
            arrayOf(buttonResign, labelResign)
        )
        if (isWhiteLocal && !isBlackLocal) {
            var tmp = components[0]
            components[0] = components[1]
            components[1] = tmp
            tmp = components[2]
            components[2] = components[4]
            components[4] = tmp
        }
        for(array in components) for(component in array)
            gamePanel.add(component)
    }

    private fun addOneMinuteListener(opponent: GoPlayer): ActionListener {
        val localOpponent = opponent as Player
        return ActionListener {
            displayAddOneMinuteStatus(localOpponent.color.opponent, 1, InternalMarker)
            localOpponent.extendOpponentTime(InternalMarker)
        }
    }

    private fun requestAddOneMinuteListener(opponent: GoPlayer) = ActionListener {
        displayAddOneMinuteStatus(opponent.color.opponent, 0, InternalMarker)
        opponent.requestOpponentTimeExtension(ONE_MINUTE_IN_MILLIS)
    }

    @Volatile private var blackAddOneMinute: Timer? = null
    @Volatile private var whiteAddOneMinute: Timer? = null

    internal fun displayAddOneMinuteStatus(
        player: GoColor,
        status: Int,
        marker: InternalMarker
    ) {
        marker.ignore()
        lateinit var label: JLabel
        lateinit var updater: AtomicReferenceFieldUpdater<GoGameFrame, Timer?>
        var timer: Timer? = null
        timer = Timer(1000) {
            if (updater.compareAndSet(this, timer, null))
                label.isVisible = false
        }
        timer.isRepeats = false
        if (player.isBlack) {
            label = labelBlackAddOneMinute
            updater = BLACK_ADD_ONE_MINUTE
            blackAddOneMinute = timer
        } else {
            label = labelWhiteAddOneMinute
            updater = WHITE_ADD_ONE_MINUTE
            whiteAddOneMinute = timer
        }
        val key: String = if (status > 0) {
            label.background = Color.GREEN
            "Request.Allowed"
        } else {
            label.background = if (status == 0) Color.YELLOW else Color.RED
            "Request.Waiting"
        }
        label.foreground = if (status == 0) Color.BLACK else Color.WHITE
        label.text = gobanDesktopResources().getString(key)
        label.isVisible = true
        timer.start()
    }

    internal fun onTimeEvent(player: GoPlayer, e: TimeEvent, resources: ResourceBundle, marker: InternalMarker) {
        marker.ignore()
        val labelTime: JLabel
        val displayTime: JLabel
        val labelOvertime: JLabel
        val displayOvertime: JLabel
        if (player.color == GoColor.BLACK) {
            labelTime = labelBlackTime
            displayTime = displayBlackTime
            labelOvertime = labelBlackOvertime
            displayOvertime = displayBlackOvertime
        } else {
            labelTime = labelWhiteTime
            displayTime = displayWhiteTime
            labelOvertime = labelWhiteOvertime
            displayOvertime = displayWhiteOvertime
        }
        val timeRemaining = e.timeRemaining
        if (e.isTicking && timeRemaining <= 10000L) { // 10 seconds
            labelTime.isOpaque = true
            labelTime.foreground = Color.WHITE
        } else {
            labelTime.isOpaque = false
            labelTime.foreground = Color.BLACK
        }
        displayTime.text = timeLimitFormatter.format(timeRemaining)
        val overtime = player.overtime
        if (overtime != null) {
            if (e.isOvertime) {
                labelOvertime.text = resources.getString("OvertimeRemaining.Prefix") +
                        overtime.displayName() + resources.getString("OvertimeRemaining.Suffix")
                displayOvertime.text = overtime.displayOvertime(e)
            } else {
                labelOvertime.text = resources.getString("OvertimeRemaining")
                displayOvertime.text = overtime.displayName()
            }
        }
    }

    @Volatile private var resignTimer: Timer? = null

    private fun resign(marker: InternalMarker) {
        val updater = RESIGN_TIMER
        val labelResign = this.labelResign
        var timer: Timer? = null
        val blackPlayer = this.blackPlayer
        val whitePlayer = this.whitePlayer
        timer = Timer(1000) {
            // One second has passed. Forget that the button was clicked.
            labelResign.isVisible = false
            updater.compareAndSet(this, timer, null)
        }
        timer.isRepeats = false
        if (updater.compareAndSet(this, null, timer)) {
            // Button clicked for the first time. Wait one second.
            labelResign.isVisible = true
            timer.start()
        } else {
            // Button clicked for the second time. Really resign.
            labelResign.isVisible = false
            resignTimer = null
            val player: Player = when {
                (blackPlayer as? Player)?.frame === this ->
                    if ((whitePlayer as? Player)?.frame !== this) blackPlayer
                    else when(gameAction) {
                        GameAction.PLAY_BLACK -> blackPlayer
                        GameAction.PLAY_WHITE -> whitePlayer
                        else -> return
                    }
                (whitePlayer as? Player)?.frame === this -> whitePlayer
                else -> return
            }
            player.resign(marker)
            gameAction = null
            update(marker)
        }
    }

    // TODO

    private val actionButton = JButton(GoPoint.format(null, sgf.width, sgf.height))

    init {
        val sidePanel = JPanel(BorderLayout())
        val topPanel = JPanel(GridLayout(1, 1))
        topPanel.border = southBorder
        topPanel.add(panelOpponentRequestUndo)
        sidePanel.add(topPanel, BorderLayout.NORTH)
        var panel = JPanel(BorderLayout())
        // TODO
        panel.add(cardPanel, BorderLayout.NORTH)
        sidePanel.add(panel, BorderLayout.CENTER)

        val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
        split.leftComponent = sidePanel
        panel = JPanel(BorderLayout())
        panel.add(gobanView, BorderLayout.CENTER)
        panel.add(actionButton, BorderLayout.SOUTH)
        split.rightComponent = panel
        actionButton.isEnabled = false
        actionButton.addActionListener {
            actionButton.isEnabled = false
            val action = gameAction
            if (action == GameAction.COUNT_SCORE) {
                blackPlayer.submitScoreFromFrame()
                whitePlayer.submitScoreFromFrame()
            } else {
                if (action == GameAction.HANDICAP)
                    actionButton.text = GoPoint.format(null, sgf.width, sgf.height)
                gameAction = null
                gobanView.repaint()
                val continuation = this.continuation
                this.continuation = null
                continuation?.resume(null)
            }
        }
        contentPane = split
    }

    private fun GoPlayer.submitScoreFromFrame() {
        if (this is Player && frame === this@GoGameFrame)
            submitScore(InternalMarker)
    }

    private var goCursor: GoPoint? = null

    private fun updateHandicapText(targetHandicap: Int, currentHandicap: Int) {
        displayHandicapTarget.text = targetHandicap.toString()
        displayHandicapCurrent.text = currentHandicap.toString()
        val inverted = sgf.width * sgf.height - currentHandicap
        displayHandicapInvert.text = inverted.toString()
        if (currentHandicap == 0 || inverted > targetHandicap) {
            buttonHandicapInvert.isEnabled = false
            displayHandicapInvert.foreground = Color.RED
        } else {
            buttonHandicapInvert.isEnabled = true
            displayHandicapInvert.foreground = Color.BLACK
        }
        actionButton.isEnabled = currentHandicap == targetHandicap
    }

    private fun invertHandicap() {
        val goban = gobanView.goban as? Goban ?: return
        val target = handicap
        val current = goban.blackCount
        if (current == 0) return
        val inverted = goban.width * goban.height - current
        if (inverted > target) return
        val blackStoneSet = goban.toMutablePointSet(GoColor.BLACK)
        blackStoneSet.invertAll(allPoints)
        goban.clear()
        goban.setAll(blackStoneSet, GoColor.BLACK)
        updateHandicapText(target, inverted)
        gobanView.repaint()
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
            }
            GameAction.COUNT_SCORE -> {
                val scoreManager = goGame.scoreManager ?: return
                scoreManager.submitGroupStatus(goPointGroup, isAlive = isShiftDown)
            }
            // PLAY_BLACK, PLAY_WHITE
            else -> {
                val continuation = this.continuation
                this.continuation = null
                gameAction = null
                actionButton.isEnabled = false
                continuation?.resume(p)
            }
        }
    }

}