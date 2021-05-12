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
import kotlin.coroutines.*
import kotlin.math.min

class GoGameFrame private constructor(
    _context: CoroutineContext,
    _scope: CoroutineScope?,
    resources: ResourceBundle = gobanDesktopResources()
): JFrame() {

    @JvmOverloads
    constructor(context: CoroutineContext = EmptyCoroutineContext): this(context, null)

    @Suppress("unused")
    val context: CoroutineContext get() = scope.coroutineContext

    constructor(scope: CoroutineScope): this(scope.coroutineContext, scope)

    val scope: CoroutineScope

    constructor(setup: GoGameSetup): this(GoGameContext(setup, PlayerFactory)) {
        (blackPlayer as? Player)?.initFrame(this)
        (whitePlayer as? Player)?.initFrame(this)
    }

    val blackPlayer: GoPlayer
    val whitePlayer: GoPlayer
    val gameContext: GoGameContext

    init {
        var context = _context
        var scope = _scope
        var interceptor = context[ContinuationInterceptor]
        if (interceptor != Dispatchers.Swing) {
            scope = null
            if (interceptor != null) {
                context = context.minusKey(ContinuationInterceptor)
                interceptor = null
            }
        }
        gameContext = context[GoGameContext] ?: GoGameContext { color -> Player(color, this) }.also {
            context += it
            scope = null
        }
        blackPlayer = gameContext.blackPlayer
        whitePlayer = gameContext.whitePlayer
        if (interceptor == null) {
            // scope is definitely null at this point
            context += Dispatchers.Swing
        }
        this.scope = scope ?: CoroutineScope(context)
        title = "CN13 Goban"
        defaultCloseOperation = DISPOSE_ON_CLOSE
        setSize(1000, 500)
        extendedState = MAXIMIZED_BOTH
        setLocationRelativeTo(null)
    }

    class Player: GoPlayer {

        constructor(color: GoColor): super(color) {
            _frame = null
        }

        constructor(color: GoColor, frame: GoGameFrame): super(color) {
            _frame = frame
        }

        @Volatile private var _frame: GoGameFrame?

        val frame: GoGameFrame
            get() = _frame ?: throw UninitializedPropertyAccessException("property frame has not been initialized")

        val isFrameInitialized: Boolean get() = _frame != null

        fun initFrame(frame: GoGameFrame): GoGameFrame {
            val current = _frame
            if (current != null) return current
            if (updateFrame.compareAndSet(this, null, frame)) {
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

            private val updateFrame: AtomicReferenceFieldUpdater<Player, GoGameFrame?> =
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
            val currentNode = coroutineContext[GoGameContext]?.node as? GoSGFMoveNode ?: return false
            if (currentNode === resumeNode) return false
            var parentNode: GoSGFNode = currentNode.parent ?: return false
            while(parentNode !== resumeNode) {
                if (parentNode !is GoSGFMoveNode || parentNode.turnPlayer == currentNode.turnPlayer)
                    return false
                parentNode = parentNode.parent ?: return false
            }
            return (getOpponent() as? Player)?.frame === frame ||
                frame.acceptUndoMove(color, InternalMarker)
        }

        override suspend fun requestOpponentTimeExtension(requestedMilliseconds: Long): Long {
            val frame = this.frame
            return if (requestedMilliseconds > 0L && ((getOpponent() as? Player)?.frame === frame))
                extendOpponentTime(InternalMarker)
            else {
                frame.displayAddOneMinuteStatus(color.opponent, -1, InternalMarker)
                0L
            }
        }

        internal suspend fun extendOpponentTime(marker: InternalMarker): Long {
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

        internal suspend fun resign(marker: InternalMarker) {
            marker.ignore()
            resign()
        }

        override fun startScoring(game: GoGameContext, scoreManager: GoScoreManager) {
            frame.startScoring(scoreManager, InternalMarker)
        }

        override fun updateScoring(
            game: GoGameContext,
            scoreManager: GoScoreManager,
            stones: GoPointSet,
            alive: Boolean
        ) {
            frame.updateScoring(scoreManager, stones, alive, InternalMarker)
        }

        override fun finishScoring(game: GoGameContext) {
            frame.finishScoring(InternalMarker)
        }

        internal fun submitScore(
            game: GoGameContext,
            scoreManager: GoScoreManager,
            marker: InternalMarker
        ) {
            marker.ignore()
            submitScore(game, scoreManager)
        }

        internal fun requestResumePlay(
            game: GoGameContext,
            scoreManager: GoScoreManager,
            marker: InternalMarker
        ) {
            marker.ignore()
            requestResumePlay(game, scoreManager)
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

        override fun createPlayer(color: GoColor): GoPlayer = Player(color)

        private val updateGameAction: AtomicReferenceFieldUpdater<GoGameFrame, GameAction?> =
            AtomicReferenceFieldUpdater.newUpdater(
                GoGameFrame::class.java, GameAction::class.java, "gameAction"
            )

        private val updateUndoContinuation:
                AtomicReferenceFieldUpdater<GoGameFrame, Continuation<Boolean>?> =
            AtomicReferenceFieldUpdater.newUpdater(
                GoGameFrame::class.java,
                Continuation::class.java as Class<Continuation<Boolean>>,
                "undoContinuation"
            )

        private val updateRequestUndoMove: AtomicReferenceFieldUpdater<GoGameFrame, Deferred<Boolean>?> =
            AtomicReferenceFieldUpdater.newUpdater(
                GoGameFrame::class.java,
                Deferred::class.java as Class<Deferred<Boolean>>,
                "deferredRequestUndoMove"
            )

        private val updateInitLayout: AtomicIntegerFieldUpdater<GoGameFrame> =
            AtomicIntegerFieldUpdater.newUpdater(GoGameFrame::class.java, "initLayoutOnce")

        private val updateBlackAddOneMinute: AtomicReferenceFieldUpdater<GoGameFrame, Deferred<Unit>?> =
            AtomicReferenceFieldUpdater.newUpdater(
                GoGameFrame::class.java,
                Deferred::class.java as Class<Deferred<Unit>>,
                "deferredBlackAddOneMinute"
            )

        private val updateWhiteAddOneMinute: AtomicReferenceFieldUpdater<GoGameFrame, Deferred<Unit>?> =
            AtomicReferenceFieldUpdater.newUpdater(
                GoGameFrame::class.java,
                Deferred::class.java as Class<Deferred<Unit>>,
                "deferredWhiteAddOneMinute"
            )

        private val updateResign: AtomicReferenceFieldUpdater<GoGameFrame, Deferred<Unit>?> =
            AtomicReferenceFieldUpdater.newUpdater(
                GoGameFrame::class.java,
                Deferred::class.java as Class<Deferred<Unit>>,
                "deferredResign"
            )

        const val ONE_MINUTE_IN_MILLIS: Long = 60000L

        @JvmField val TRANSPARENT_BLACK = Color(0x7F000000, true)
        @JvmField val TRANSPARENT_WHITE = Color(0x7FFFFFFF, true)

        private val southBorder = BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY)
    }

    private val sgf = gameContext.sgf
    private val superkoRestrictions = MutableGoPointSet()
    private val suicideRestrictions = MutableGoPointSet()
    private val goPointGroup = MutableGoPointSet()
    private val prototypeGoban = Goban(sgf.width, sgf.height)
    private var scoreGoban: MutableGoban = gameContext.node.territory
    private val allPoints = GoRectangle(0, 0, sgf.width - 1, sgf.height - 1)

    @Volatile private var gameAction: GameAction? = null
    private var handicap = 0
    private var continuation: Continuation<GoPoint?>? = null
    private var scoreManager: GoScoreManager? = null

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
        if (!updateGameAction.compareAndSet(this, null, GameAction.HANDICAP))
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
        if (!updateGameAction.compareAndSet(this@GoGameFrame, null, action))
            throw IllegalStateException("Cannot request move while another operation is pending.")
        val goban = gameContext.node.goban
        val allowSuicide = gameContext.gameInfo.rules.allowSuicide
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
        val node = gameContext.node
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
            if (updateGameAction.compareAndSet(this, action, null)) return true
        }
    }

    @Volatile private var deferredRequestUndoMove: Deferred<Boolean>? = null

    fun requestUndoMove() {
        val player: Player
        var resumeNode: GoSGFNode?
        val node = gameContext.node as? GoSGFMoveNode ?: return
        val black = (blackPlayer as? Player)?.takeIf { it.frame === this }
        val white = (whitePlayer as? Player)
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
        deferred = scope.async {
            val response = player.requestUndoMove(resumeNode, InternalMarker)
            val mostRecent = updateRequestUndoMove.compareAndSet(this@GoGameFrame, deferred, null)
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
    }

    @Suppress("unused")
    @Volatile private var undoContinuation: Continuation<Boolean>? = null

    internal suspend fun acceptUndoMove(player: GoColor, marker: InternalMarker): Boolean {
        marker.ignore()
        val allowed: Boolean = suspendCoroutine { continuation ->
            val oldContinuation = updateUndoContinuation.getAndSet(this, continuation)
            if (oldContinuation != null) oldContinuation.resume(false)
            else panelOpponentRequestUndo.isVisible = true
        }
        if (allowed) {
            val cancelAction: GameAction =
                if (player == GoColor.BLACK) GameAction.PLAY_BLACK
                else GameAction.PLAY_WHITE
            if (updateGameAction.compareAndSet(this, cancelAction, null))
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
            val updater = updateRequestUndoMove
            scope.launch {
                delay(1000L)
                if (updater[this@GoGameFrame] == null)
                    label.isVisible = false
            }
        }
    }

    internal fun startScoring(scoreManager: GoScoreManager, marker: InternalMarker) {
        marker.ignore()
        if (updateGameAction.compareAndSet(this, null, GameAction.COUNT_SCORE)) {
            this.scoreManager = scoreManager
            val goban = gameContext.node.goban
            prototypeGoban.copyFrom(goban)
            gobanView.goban = goban
            scoreGoban = prototypeGoban.getScoreGoban(gameContext.gameInfo.rules.territoryScore)
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
            if (alive) prototypeGoban.copyFrom(gameContext.node.goban, stones)
            else prototypeGoban.setAll(stones, null)
            scoreGoban = prototypeGoban.getScoreGoban(gameContext.gameInfo.rules.territoryScore, scoreGoban)
            gobanView.repaint()
            actionButton.isEnabled = true
        }
    }

    internal fun finishScoring(marker: InternalMarker) {
        marker.ignore()
        if (updateGameAction.compareAndSet(this, GameAction.COUNT_SCORE, null)) {
            scoreManager = null
            scoreGoban = gameContext.node.territory
            gobanView.repaint()
        }
    }

    fun requestResumePlay() {
        val scoreManager = this.scoreManager ?: return
        val blackPlayer = (this.blackPlayer as? Player)?.takeIf { it.frame === this }
        val whitePlayer = this.whitePlayer as? Player
        val player: Player = when {
            whitePlayer?.frame !== this -> blackPlayer ?: return
            blackPlayer == null -> whitePlayer
            else -> {
                val node = gameContext.node
                if ((node.turnPlayer == GoColor.WHITE) == (node is GoSGFMoveNode)) whitePlayer else blackPlayer
            }
        }
        player.requestResumePlay(gameContext, scoreManager, InternalMarker)
    }

    private val gobanView = object: GobanView(gameContext.node.goban) {

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
            val p = (gameContext.node as? GoSGFMoveNode)?.playStoneAt
            if (p != null) {
                val color = goban[p]
                if (color == GoColor.BLACK) g.paint = Color.WHITE
                else if (color == GoColor.WHITE) g.paint = Color.BLACK
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
            val continuation = updateUndoContinuation.getAndSet(this, null)
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
        var timeEvent = blackPlayer.getTimeEvent(gameContext)
        if (timeEvent == null) {
            displayBlackTime.foreground = Color.DARK_GRAY
            displayBlackTime.text = none
        } else {
            displayBlackTime.isOpaque = true
            displayBlackTime.background = Color.BLACK
            displayBlackTime.foreground = Color.WHITE
            onTimeEvent(whitePlayer, timeEvent, resources, InternalMarker)
        }
        timeEvent = whitePlayer.getTimeEvent(gameContext)
        if (timeEvent == null) {
            displayWhiteTime.foreground = Color.DARK_GRAY
            displayWhiteTime.text = none
        } else {
            displayWhiteTime.isOpaque = true
            displayWhiteTime.background = Color.WHITE
            displayWhiteTime.foreground = Color.BLACK
            onTimeEvent(whitePlayer, timeEvent, resources, InternalMarker)
        }
        val overtime = gameContext.gameInfo.overtime
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
        val isBlackLocal: Boolean = if (blackPlayer is Player) {
            if (!blackPlayer.isFrameInitialized) return
            blackPlayer.frame === this
        } else false
        val isWhiteLocal: Boolean = if (whitePlayer is Player) {
            if (!whitePlayer.isFrameInitialized) return
            whitePlayer.frame === this
        } else false
        if (!updateInitLayout.compareAndSet(this, 0, 1)) return
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
            scope.launch {
                localOpponent.extendOpponentTime(InternalMarker)
            }
        }
    }

    private fun requestAddOneMinuteListener(opponent: GoPlayer) = ActionListener {
        displayAddOneMinuteStatus(opponent.color.opponent, 0, InternalMarker)
        scope.launch {
            opponent.requestOpponentTimeExtension(ONE_MINUTE_IN_MILLIS)
        }
    }

    @Suppress("unused") @Volatile private var deferredBlackAddOneMinute: Deferred<Unit>? = null
    @Suppress("unused") @Volatile private var deferredWhiteAddOneMinute: Deferred<Unit>? = null

    internal fun displayAddOneMinuteStatus(
        player: GoColor,
        status: Int,
        marker: InternalMarker
    ) {
        marker.ignore()
        val label: JLabel
        val updater: AtomicReferenceFieldUpdater<GoGameFrame, Deferred<Unit>?>
        if (player.isBlack) {
            label = labelBlackAddOneMinute
            updater = updateBlackAddOneMinute
        } else {
            label = labelWhiteAddOneMinute
            updater = updateWhiteAddOneMinute
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
        var deferred: Deferred<Unit>? = null
        deferred = scope.async {
            updater[this@GoGameFrame] = deferred
            delay(1000L)
            if (updater.compareAndSet(this@GoGameFrame, deferred, null))
                label.isVisible = false
        }
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
        val overtime = player.getOvertime(gameContext)
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

    @Suppress("unused")
    @Volatile private var deferredResign: Deferred<Unit>? = null

    private fun resign(marker: InternalMarker) {
        val updateResign = GoGameFrame.updateResign
        val updateGameAction = GoGameFrame.updateGameAction
        val labelResign = this.labelResign
        var deferred: Deferred<Unit>? = null
        deferred = scope.async {
            if (updateResign.compareAndSet(this@GoGameFrame, null, deferred)) {
                labelResign.isVisible = true
                delay(1000L)
                labelResign.isVisible = false
                updateResign.compareAndSet(this@GoGameFrame, deferred, null)
            } else {
                labelResign.isVisible = false
                updateResign[this@GoGameFrame] = null
                val player: Player = when {
                    (blackPlayer as? Player)?.frame === this@GoGameFrame ->
                        if ((whitePlayer as? Player)?.frame !== this@GoGameFrame) blackPlayer
                        else when(updateGameAction[this@GoGameFrame]) {
                            GameAction.PLAY_BLACK -> blackPlayer
                            GameAction.PLAY_WHITE -> whitePlayer
                            else -> return@async
                        }
                    (whitePlayer as? Player)?.frame === this@GoGameFrame -> whitePlayer
                    else -> return@async
                }
                player.resign(marker)
                updateGameAction[this@GoGameFrame] = null
                update(marker)
            }
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
                val scoreManager = this.scoreManager
                if (scoreManager != null) {
                    blackPlayer.submitScoreFromFrame(scoreManager)
                    whitePlayer.submitScoreFromFrame(scoreManager)
                }
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

    private fun GoPlayer.submitScoreFromFrame(scoreManager: GoScoreManager) {
        val player = this as? Player
        if (player !== null && player.frame === this@GoGameFrame)
            player.submitScore(gameContext, scoreManager, InternalMarker)
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
                val scoreManager = this.scoreManager ?: return
                val channel = if (isShiftDown) scoreManager.livingStones else scoreManager.deadStones
                scope.launch {
                    channel.send(goPointGroup)
                }
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