@file:Suppress("NAME_SHADOWING")

package com.computernerd1101.goban.desktop

import com.computernerd1101.goban.*
import com.computernerd1101.goban.desktop.internal.*
import com.computernerd1101.goban.desktop.resources.*
import com.computernerd1101.goban.markup.*
import com.computernerd1101.goban.sgf.*
import com.computernerd1101.goban.sgf.Date
import com.computernerd1101.goban.time.*
import com.computernerd1101.sgf.*
import java.awt.*
import java.awt.event.*
import java.net.URL
import java.nio.charset.Charset
import java.text.*
import java.util.*
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater
import javax.swing.*
import javax.swing.event.*
import javax.swing.tree.TreePath
import kotlin.math.*

class GoEditorFrame private constructor(
    sgf: GoSGF,
    node: GoSGFNode,
    resources: ResourceBundle = gobanDesktopResources(),
    formatResources: ResourceBundle = gobanDesktopFormatResources()
): JFrame() {

    private val nodeDelegate = object: () -> GoSGFNode {

        @JvmField var node: GoSGFNode = node

        override fun invoke(): GoSGFNode = node

    }

    private var node: GoSGFNode
        get() = nodeDelegate.node
        set(node) { nodeDelegate.node = node }

    constructor(sgf: GoSGF): this(sgf, sgf.rootNode)

    @Suppress("unused")
    constructor(node: GoSGFNode): this(node.tree, node)

    init {
        title = "CN13 Goban"
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        setSize(1000, 500)
        extendedState = Frame.MAXIMIZED_BOTH
        setLocationRelativeTo(null)
    }

    private val sizeFormat = formatResources.getObject("GobanSizeFormatter.LONG") as GobanSizeFormatter

    @get:JvmName("getSGF")
    var sgf: GoSGF = sgf
        @JvmName("setSGF")
        set(sgf) {
            val old = field
            field = sgf
            val width = sgf.width
            val height = sgf.height
            if (old.width != width || old.height != height) {
                allPoints = GoPoint(0, 0).rect(width - 1, height - 1)
                labelSize.text = sizeFormat.format(width, height)
            }
            val variationFlags = sgf.variationView
            if (variationFlags and GoSGF.CURRENT_VARIATION == 0)
                radioChildVariations.isSelected = true
            else radioSiblingVariations.isSelected = true
            checkAutoMarkup.isSelected = variationFlags and GoSGF.NO_MARKUP_VARIATION == 0
            sgfTreeModel.root = sgf
            sgfTreeView.updateUI()
            sgfTreeView.expandRow(0)
            val node = sgf.rootNode
            this.node = node
            selectSGFNode(node)
        }
    private var allPoints: GoRectangle = GoPoint(0, 0).rect(sgf.width - 1, sgf.height - 1)
    private var blackScore: Int = 0
    private var whiteScore: Int = 0

    init {
        computeScores(node)
    }

    private var goban: AbstractGoban = node.goban
    private var pointMarkupMap: PointMarkupMap = node.pointMarkup
    private var lineMarkupSet: LineMarkupSet = node.lineMarkup

    private var goCursor: GoPoint? = null
    private var startLine: GoPoint? = null

    init {
        layout = BorderLayout()
    }

    private fun <B: AbstractButton> toolBarAbstractButton(
        resources: ResourceBundle,
        iconName: String,
        altText: String,
        toolTip: String?,
        button: B
    ): B {
        val iconURL: URL? = GoEditorFrame::class.java.getResource("icons/toolbar/$iconName.png")
        button.toolTipText = resources.getString(
            toolTip ?: "ToolBar.$altText"
        )
        if (iconURL != null)
            button.icon = ImageIcon(iconURL, altText)
        else button.text = altText
        return button
    }

    private fun toolBarButton(
        resources: ResourceBundle,
        iconName: String,
        altText: String
    ) = toolBarAbstractButton(resources, iconName, altText, null, JButton())

    private fun toolBarToggleButton(
        resources: ResourceBundle,
        iconName: String,
        altText: String,
        toolTip: String? = null
    ) = toolBarAbstractButton(resources, iconName, altText, toolTip, JToggleButton())

    private val buttonBlack =
        toolBarToggleButton(resources, "Black", "B")
    private val buttonWhite =
        toolBarToggleButton(resources, "White", "W")
    private val buttonLabelMarkup =
        toolBarToggleButton(resources, "LabelMarkup", "LB")
    private val buttonSelectMarkup =
        toolBarToggleButton(resources, "SelectMarkup", "SL")
    private val buttonXMarkup =
        toolBarToggleButton(resources, "XMarkup", "MA")
    private val buttonTriangleMarkup =
        toolBarToggleButton(resources, "TriangleMarkup", "TR")
    private val buttonCircleMarkup =
        toolBarToggleButton(resources, "CircleMarkup", "CR")
    private val buttonSquareMarkup =
        toolBarToggleButton(resources, "SquareMarkup", "SQ")
    private val buttonDeletePointMarkup =
        toolBarToggleButton(resources, "DeletePointMarkup", "X")
    // TODO fix visibility during edit
    private val buttonLineMarkup =
        toolBarToggleButton(resources, "LineMarkup", "LN")
    private val buttonArrowMarkup =
        toolBarToggleButton(resources, "ArrowMarkup", "AR")
    private val buttonDeleteLineMarkup =
        toolBarToggleButton(resources, "DeleteLineMarkup", "XLN")
    private val buttonDim =
        toolBarToggleButton(resources, "Dim", "DD")
    private val buttonResetDim =
        toolBarButton(resources, "ResetDim", "XDD")
    private val buttonInheritDim =
        toolBarToggleButton(resources, "InheritDim", "<DD", "ToolBar.DD.Inherit")
    private val buttonVisible =
        toolBarToggleButton(resources, "Visible", "VW")
    private val buttonResetVisible =
        toolBarButton(resources, "ResetVisible", "XVW")
    private val buttonInheritVisible =
        toolBarToggleButton(resources, "InheritVisible", "<VW", "ToolBar.VW.Inherit")
    private val markupButtons = arrayOf<AbstractButton>(
        buttonLabelMarkup,
        buttonSelectMarkup,
        buttonXMarkup,
        buttonTriangleMarkup,
        buttonCircleMarkup,
        buttonSquareMarkup,
        buttonDeletePointMarkup,
        buttonLineMarkup,
        buttonArrowMarkup,
        buttonDeleteLineMarkup,
        buttonDim,
        buttonResetDim,
        buttonInheritDim,
        buttonVisible,
        buttonResetVisible,
        buttonInheritVisible
    )
    private var selectedToolButton: JToggleButton = buttonBlack

    private val gobanView = object: GobanView(goban) {

        init {
            pointMarkup = pointMarkupMap
            lineMarkup = lineMarkupSet
            val ma = object: MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) = goPointClicked(e)
                override fun mousePressed(e: MouseEvent) {
                    if (!buttonDim.isSelected && !buttonVisible.isSelected) return
                    val p = toGoPoint(e) ?: return
                    if (p != goCursor) return
                    startLine = p
                    repaint()
                }

                override fun mouseReleased(e: MouseEvent) {
                    val p2 = toGoPoint(e) ?: return
                    if (p2 != goCursor) return
                    val p1 = startLine ?: return
                    val selectVisible = buttonVisible.isSelected
                    val points: MutableGoPointSet = when {
                        selectVisible ->
                            node.visiblePoints ?: MutableGoPointSet().apply {
                                node.visiblePoints = this
                            }
                        buttonDim.isSelected ->
                            node.dimPoints ?: MutableGoPointSet().apply {
                                node.dimPoints = this
                            }
                        else -> return
                    }
                    startLine = null
                    points.invertAll(p1 rect p2)
                    if (selectVisible && points.containsAll(allPoints))
                        points.clear()
                    repaint()
                }

                override fun mouseDragged(e: MouseEvent) {
                    if (buttonDim.isSelected || buttonVisible.isSelected)
                        mouseMoved(e)
                }
                override fun mouseMoved(e: MouseEvent) {
                    val p = toGoPoint(e)
                    if (p != goCursor) {
                        goCursor = p
                        repaint()
                    }
                }
            }
            addMouseListener(ma)
            addMouseMotionListener(ma)
        }

        override fun getStoneColorAt(p: GoPoint): GoColor? {
            val stone = getStoneAt(p)
            return when {
                stone != null || p != goCursor -> stone
                buttonBlack.isSelected -> GoColor.BLACK
                buttonWhite.isSelected -> GoColor.WHITE
                else -> null
            }
        }

        override fun paintGoban(g: Graphics2D) {
            super.paintGoban(g)
            if (!buttonDim.isSelected && !buttonVisible.isSelected) return
            var (x1, y1) = startLine ?: return
            var (x2, y2) = goCursor ?: return
            if (x1 == x2 && y1 == y2) return
            if (x1 > x2) {
                val tmp = x1
                x1 = x2
                x2 = tmp
            }
            if (y1 > y2) {
                val tmp = y1
                y1 = y2
                y2 = tmp
            }
            g.color = Color.BLUE
            val thickness = edgeThickness
            g.stroke = if (thickness == 1) thinStroke
            else {
                val f = (thickness / gridScale).toFloat()
                if (f > 0.125f) thinStroke
                else BasicStroke(f)
            }
            g.drawRect(x1, y1, x2 - x1, y2 - y1)
        }

        override fun paintGoPoint(g: Graphics2D, p: GoPoint) {
            val visible = node.visiblePoints
            if (((visible.isNullOrEmpty() || visible.contains(p)) xor
                        (p == goCursor && (startLine == null || startLine == p)
                                && buttonVisible.isSelected)) ||
                (p == goCursor && (buttonBlack.isSelected || buttonWhite.isSelected)))
                super.paintGoPoint(g, p)
        }

        override fun getAlphaAt(p: GoPoint): Float {
            val dim = if (node.dimPoints?.contains(p) == true) 0.25f else 1f
            return when {
                p != goCursor -> dim
                (buttonBlack.isSelected || buttonWhite.isSelected) &&
                        buttonSGFSetup.isSelected && getStoneAt(p) != null -> dim*0.5f
                buttonDim.isSelected && (startLine == null || startLine == p) -> 0.25f / dim
                else -> dim
            }
        }

        override fun getStoneAlphaAt(p: GoPoint): Float {
            return if (p == goCursor &&
                (buttonBlack.isSelected || buttonWhite.isSelected) &&
                getStoneAt(p) != null) 0.5f
            else 1f
        }

        override fun getPointMarkupAt(p: GoPoint): PointMarkup? {
            val pm = super.getPointMarkupAt(p)
            return if (p != goCursor) pm
            else when(selectedToolButton) {
                buttonLabelMarkup -> if (pm?.label == null) LABEL else pm
                buttonSelectMarkup -> PointMarkup.SELECT
                buttonXMarkup -> PointMarkup.X
                buttonTriangleMarkup -> PointMarkup.TRIANGLE
                buttonCircleMarkup -> PointMarkup.CIRCLE
                buttonSquareMarkup -> PointMarkup.SQUARE
                else -> pm
            }
        }

        override fun getMarkupColorAt(p: GoPoint): Color {
            if (p == goCursor) {
                when(selectedToolButton) {
                    buttonDeletePointMarkup -> return Color.GRAY
                    buttonLabelMarkup, buttonSelectMarkup,
                        buttonXMarkup, buttonTriangleMarkup,
                        buttonCircleMarkup, buttonSquareMarkup -> return Color.BLUE
                }
            }
            return super.getMarkupColorAt(p)
        }

        override fun isLineMarkupVisible(lm: LineMarkup): Boolean {
            if (!buttonDeleteLineMarkup.isSelected) return true
            val p: GoPoint = startLine?.also {
                if (it == goCursor) return true
            } ?: goCursor ?: return true
            val markupSet = lineMarkup
            return if (markupSet != null && lm.end == p)
                lm.isArrow && markupSet[p, lm.start] != null
            else lm.start != p
        }

        override fun paintAllLineMarkups(g: Graphics2D) {
            super.paintAllLineMarkups(g)
            if (!(buttonLineMarkup.isSelected || buttonArrowMarkup.isSelected ||
                        buttonDeleteLineMarkup.isSelected)) return
            val start = startLine
            val end = goCursor
            if (start == end) return
            var lm: LineMarkup? = null
            g.paint = Color.BLUE
            if (buttonLineMarkup.isSelected) {
                val markupSet = lineMarkup
                if (markupSet != null) {
                    val p: GoPoint? = if (start == null) end
                    else {
                        if (end != null)
                            lm = markupSet[start, end] ?: markupSet[end, start]
                        start
                    }
                    for(m in markupSet)
                        if (m != lm &&
                            (m.start == p || (m.end == p && (!m.isArrow ||
                                    markupSet[p, m.start] == null))))
                            paintLineMarkup(g, m)
                    if (lm != null) {
                        g.stroke = thinStroke
                        paintLineMarkup(g, lm)
                    }
                }
            } else if (start != null && end != null) {
                paintLineMarkup(g,
                    if (buttonArrowMarkup.isSelected) start arrowMarkup end
                    else start lineMarkup end)
            }
        }

    }

    private val buttonPass = JButton(GoPoint.format(null, sgf.width, sgf.height, locale))
    private val labelBlackScore = JLabel()
    private val labelWhiteScore = JLabel()

    init {
        val toolBar = JToolBar()
        toolBar.isFloatable = false
        val actionListener = ActionListener { e: ActionEvent ->
            val button = e.source as JToggleButton
            button.isSelected = true
            if (button != selectedToolButton) {
                selectedToolButton.isSelected = false
                selectedToolButton = button
                var enablePass = false
                when(button) {
                    buttonBlack, buttonWhite -> enablePass = true
                    buttonDim ->
                        if (node.newDimPoints == null) {
                            node.dimPoints = node.dimPoints?.copy() ?: MutableGoPointSet()
                            buttonInheritDim.isSelected = false
                        }
                    buttonVisible ->
                        if (node.newVisiblePoints == null) {
                            node.visiblePoints = node.visiblePoints?.copy() ?: MutableGoPointSet()
                            buttonInheritVisible.isSelected = false
                        }
                    buttonLineMarkup, buttonArrowMarkup, buttonDeleteLineMarkup -> Unit
                    else -> startLine = null
                }
                buttonPass.isEnabled = enablePass
            }
        }
        buttonBlack.isSelected = true
        buttonBlack.addActionListener(actionListener)
        toolBar.add(buttonBlack)
        buttonWhite.addActionListener(actionListener)
        toolBar.add(buttonWhite)
        toolBar.addSeparator()
        buttonLabelMarkup.addActionListener(actionListener)
        toolBar.add(buttonLabelMarkup)
        buttonSelectMarkup.addActionListener(actionListener)
        toolBar.add(buttonSelectMarkup)
        buttonXMarkup.addActionListener(actionListener)
        toolBar.add(buttonXMarkup)
        buttonTriangleMarkup.addActionListener(actionListener)
        toolBar.add(buttonTriangleMarkup)
        buttonCircleMarkup.addActionListener(actionListener)
        toolBar.add(buttonCircleMarkup)
        buttonSquareMarkup.addActionListener(actionListener)
        toolBar.add(buttonSquareMarkup)
        buttonDeletePointMarkup.addActionListener(actionListener)
        toolBar.add(buttonDeletePointMarkup)
        toolBar.addSeparator()
        buttonLineMarkup.addActionListener(actionListener)
        toolBar.add(buttonLineMarkup)
        buttonArrowMarkup.addActionListener(actionListener)
        toolBar.add(buttonArrowMarkup)
        buttonDeleteLineMarkup.addActionListener(actionListener)
        toolBar.add(buttonDeleteLineMarkup)
        toolBar.addSeparator()
        buttonDim.addActionListener(actionListener)
        toolBar.add(buttonDim)
        buttonResetDim.addActionListener {
            val points = node.newDimPoints
            if (points != null) points.clear()
            else node.dimPoints = MutableGoPointSet()
            buttonInheritDim.isSelected = false
            gobanView.repaint()
        }
        toolBar.add(buttonResetDim)
        buttonInheritDim.addActionListener {
            node.dimPoints = null
            buttonInheritDim.isSelected = true
            if (buttonDim.isSelected) {
                buttonDim.isSelected = false
                selectSGFPlayer(node)
            }
            gobanView.repaint()
        }
        toolBar.add(buttonInheritDim)
        toolBar.addSeparator()
        buttonVisible.addActionListener(actionListener)
        toolBar.add(buttonVisible)
        buttonResetVisible.addActionListener {
            val points = node.newVisiblePoints
            if (points != null) points.clear()
            else node.visiblePoints = MutableGoPointSet()
            buttonInheritVisible.isSelected = false
            gobanView.repaint()
        }
        toolBar.add(buttonResetVisible)
        buttonInheritVisible.addActionListener {
            node.visiblePoints = null
            buttonInheritVisible.isSelected = true
            if (buttonVisible.isSelected) {
                buttonVisible.isSelected = false
                selectSGFPlayer(node)
            }
            gobanView.repaint()
        }
        toolBar.add(buttonInheritVisible)
        add(toolBar, BorderLayout.SOUTH)
    }

    private val sgfTreeModel = SGFTreeModel()
    private val sgfTreeView = JTree(sgfTreeModel)
    private val buttonSGFUp = JButton(resources.getString("Up"))
    private val buttonSGFDown = JButton(resources.getString("Down"))
    private val buttonSGFSetup = JToggleButton(resources.getString("Setup"))
    private val buttonSGFDelete = JButton(resources.getString("Delete"))

    private val tabs = JTabbedPane()

    init {
        sgfTreeModel.root = sgf
        sgfTreeModel.addTreeModelListener(object: TreeModelListener {
            override fun treeNodesChanged(e: TreeModelEvent?) {
                selectGameInfo(node.gameInfo, resources)
            }
            override fun treeNodesInserted(e: TreeModelEvent?) = Unit
            override fun treeNodesRemoved(e: TreeModelEvent?) = Unit
            override fun treeStructureChanged(e: TreeModelEvent?) = Unit
        })
        sgfTreeView.selectionPath = SGFTreeModel.pathTo(node)
        sgfTreeView.cellRenderer = sgfTreeModel
        sgfTreeView.transferHandler = sgfTreeModel
        sgfTreeView.dragEnabled = true
        sgfTreeView.addTreeSelectionListener { e: TreeSelectionEvent ->
            val path = e.path
            val last: Any = path.lastPathComponent
            if (last is GoSGF)
                selectSGFNode(last.rootNode)
            else if (last is GoSGFNode)
                selectSGFNode(last)
        }
        sgfTreeView.addTreeExpansionListener(object: TreeExpansionListener {
            override fun treeExpanded(event: TreeExpansionEvent?) = Unit
            override fun treeCollapsed(event: TreeExpansionEvent?) {
                val path = event?.path ?: return
                if (path.parentPath == null)
                    sgfTreeView.expandPath(path)
            }
        })
        sgfTreeView.expandPath(TreePath(sgf))
        val hSplit = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
        val vSplit = JSplitPane(JSplitPane.VERTICAL_SPLIT)
        var panel = JPanel(BorderLayout())
        val scroll = JScrollPane(sgfTreeView)
        panel.add(scroll, BorderLayout.CENTER)
        var panel2 = JPanel(GridLayout(2, 2))
        panel2.add(buttonSGFUp)
        panel2.add(buttonSGFSetup)
        panel2.add(buttonSGFDown)
        panel2.add(buttonSGFDelete)
        panel.add(panel2, BorderLayout.SOUTH)
        vSplit.topComponent = panel
        vSplit.bottomComponent = tabs
        hSplit.leftComponent = vSplit
        panel = JPanel(BorderLayout())
        panel.add(gobanView, BorderLayout.CENTER)
        panel2 = JPanel(FlowLayout())
        labelBlackScore.isOpaque = true
        labelBlackScore.background = Color.BLACK
        labelBlackScore.foreground = Color.WHITE
        panel2.add(labelBlackScore)
        buttonPass.addActionListener {
            val isBlack = buttonBlack.isSelected
            if (isBlack || buttonWhite.isSelected) {
                playStoneAt(null, isBlack.goBlackOrWhite())
            }
        }
        panel2.add(buttonPass)
        labelWhiteScore.isOpaque = true
        labelWhiteScore.background = Color.WHITE
        labelWhiteScore.foreground = Color.BLACK
        panel2.add(labelWhiteScore)
        panel.add(panel2, BorderLayout.SOUTH)
        hSplit.rightComponent = panel
        add(hSplit)
        SwingUtilities.invokeLater {
            hSplit.dividerLocation = 500
            vSplit.setDividerLocation(0.5)
        }
        val actionListener = ActionListener { e: ActionEvent? ->
            val direction = when(e?.source) {
                buttonSGFUp -> -1
                buttonSGFDown -> 1
                else -> return@ActionListener
            }
            moveSGFVariation(direction)
        }
        buttonSGFUp.addActionListener(actionListener)
        buttonSGFDown.addActionListener(actionListener)
        buttonSGFSetup.addActionListener {
            val resources = gobanDesktopResources()
            if (buttonSGFSetup.isSelected) {
                sgfTreeView.isEnabled = false
                buttonSGFUp.isEnabled = false
                buttonSGFDown.isEnabled = false
                buttonSGFDelete.text = resources.getString("Create")
                buttonSGFDelete.isEnabled = true
                gobanView.pointMarkup = null
                gobanView.lineMarkup = null
                if (!buttonBlack.isSelected && !buttonWhite.isSelected) {
                    selectedToolButton.isSelected = false
                    buttonBlack.isSelected = true
                    selectedToolButton = buttonBlack
                }
                for(button in markupButtons)
                    button.isEnabled = false
                buttonPass.isEnabled = false
            } else {
                if (!(goban contentEquals node.goban) &&
                        JOptionPane.showConfirmDialog(this, resources.getString("Setup.Cancel"),
                            resources.getString("Setup"), JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                    buttonSGFSetup.isSelected = true
                    return@addActionListener
                }
                endSetupMode()
            }
        }
        buttonSGFDelete.addActionListener {
            @Suppress("NAME_SHADOWING") val node: GoSGFNode
            if (buttonSGFSetup.isSelected) {
                buttonSGFSetup.isSelected = false
                node = this.node.createNextSetupNode(goban.readOnly())
                node.moveVariation(0)
                selectSGFTreePathFast(node)
                endSetupMode()
            } else {
                node = this.node
                val prev = node.parent
                val resources = gobanDesktopResources()
                if (prev != null && JOptionPane.showConfirmDialog(this,
                        resources.getString("Confirm.Message"), resources.getString("Node.Delete"),
                        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    var path: TreePath? = sgfTreeView.selectionPath?.parentPath
                    var index = node.childIndex
                    if (index < prev.children - 1) index++
                    else index--
                    val node2: GoSGFNode
                    if (index >= 0) {
                        node2 = prev.child(index)
                        path = path?.pathByAddingChild(node2)
                    } else {
                        node2 = prev
                        if (path != null) {
                            val last = path.lastPathComponent
                            if (last != prev && (prev.parent != null || last != sgf))
                                path = path.pathByAddingChild(prev)
                        }
                    }
                    this.node = node2
                    node.delete()
                    sgfTreeView.updateUI()
                    sgfTreeView.selectionPath = path ?: SGFTreeModel.pathTo(node2)
                }
            }
        }
    }

    private val textComment = JTextArea()

    init {
        textComment.lineWrap = true
        textComment.wrapStyleWord = true
        textComment.addKeyListener(TextListener.Node(textComment, GoSGFNode::comment, nodeDelegate))
        tabs.addTab(resources.getString("Comment"), JScrollPane(textComment))
    }

    private val cardMoveSetup = CardLayout()
    private val panelMoveSetup = JPanel(cardMoveSetup)
    private val checkMoveForced = JCheckBox(resources.getString("Move.Forced"))
    private val comboMoveAnnotation = JComboBox(moveAnnotations)
    private val spinMoveNumber = CN13Spinner()
    private val spinBlackTime = CN13Spinner()
    private val spinWhiteTime = CN13Spinner()
    private val spinBlackOvertime = CN13Spinner()
    private val spinWhiteOvertime = CN13Spinner()

    init {
        checkMoveForced.addActionListener {
            (node as? GoSGFMoveNode)?.isForced = checkMoveForced.isSelected
        }
        comboMoveAnnotation.addActionListener {
            (node as? GoSGFMoveNode)?.moveAnnotation = comboMoveAnnotation.selectedItem as? MoveAnnotation
        }
        spinMoveNumber.model = MoveNumberFormatter()
        spinBlackTime.adjustCaret = true
        spinBlackTime.model = TimeFormatter(GoColor.BLACK)
        spinWhiteTime.adjustCaret = true
        spinWhiteTime.model = TimeFormatter(GoColor.WHITE)
        spinBlackOvertime.model = OvertimeFormatter(spinBlackOvertime, GoColor.BLACK)
        spinWhiteOvertime.model = OvertimeFormatter(spinWhiteOvertime, GoColor.WHITE)
        val panel2 = JPanel(GridBagLayout())
        var row = 0
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.anchor = GridBagConstraints.WEST
        gbc.gridwidth = 3
        gbc.weightx = 1.0
        panel2.add(checkMoveForced, gbc)
        gbc.gridy = ++row
        panel2.add(comboMoveAnnotation, gbc)
        gbc.fill = GridBagConstraints.NONE
        gbc.anchor = GridBagConstraints.EAST
        gbc.gridy = ++row
        gbc.gridwidth = 1
        gbc.weightx = 0.0
        panel2.add(JLabel(resources.getString("Move.Number.Prompt")), gbc)
        val gbc1 = GridBagConstraints()
        gbc1.fill = GridBagConstraints.HORIZONTAL
        gbc1.anchor = GridBagConstraints.WEST
        gbc1.gridx = 1
        gbc1.gridy = row
        gbc1.gridwidth = 2
        gbc1.weightx = 1.0
        panel2.add(spinMoveNumber, gbc1)
        val gbc2 = GridBagConstraints()
        gbc2.fill = GridBagConstraints.NONE
        gbc2.anchor = GridBagConstraints.CENTER
        gbc2.gridx = 1
        gbc2.gridy = ++row
        gbc2.weightx = 0.0
        panel2.add(JLabel(GoColor.BLACK.toString()), gbc2)
        gbc2.gridx = 2
        panel2.add(JLabel(GoColor.WHITE.toString()), gbc2)
        gbc.gridy = ++row
        gbc1.gridy = row
        gbc1.gridwidth = 1
        gbc2.gridy = row
        gbc2.fill = GridBagConstraints.HORIZONTAL
        gbc2.anchor = GridBagConstraints.WEST
        gbc2.weightx = 1.0
        panel2.add(JLabel(resources.getString("Time.Prompt")), gbc)
        panel2.add(spinBlackTime, gbc1)
        panel2.add(spinWhiteTime, gbc2)
        gbc.gridy = ++row
        gbc1.gridy = row
        gbc2.gridy = row
        panel2.add(JLabel(resources.getString("Overtime.Prompt")), gbc)
        panel2.add(spinBlackOvertime, gbc1)
        panel2.add(spinWhiteOvertime, gbc2)
        val panel = JPanel(BorderLayout())
        panel.add(panel2, BorderLayout.NORTH)
        panelMoveSetup.add(panel, "Move")
    }

    private val radioSetupDefaultPlayer = JRadioButton(resources.getString("Default"))
    private val radioSetupBlackPlayer = JRadioButton(GoColor.BLACK.toString())
    private val radioSetupWhitePlayer = JRadioButton(GoColor.WHITE.toString())

    init {
        val actionListener = ActionListener { e: ActionEvent ->
            val node = this.node
            if (node is GoSGFSetupNode) {
                node.turnPlayer = when(e.source) {
                    radioSetupBlackPlayer -> GoColor.BLACK
                    radioSetupWhitePlayer -> GoColor.WHITE
                    else -> null
                }
                sgfTreeView.updateUI()
            }
        }
        val playerGroup = ButtonGroup()
        playerGroup.add(radioSetupDefaultPlayer)
        playerGroup.add(radioSetupBlackPlayer)
        playerGroup.add(radioSetupWhitePlayer)
        radioSetupDefaultPlayer.isSelected = true
        radioSetupDefaultPlayer.addActionListener(actionListener)
        radioSetupBlackPlayer.addActionListener(actionListener)
        radioSetupWhitePlayer.addActionListener(actionListener)
        val panel = JPanel(BorderLayout())
        val panel2 = JPanel(GridLayout(0, 1))
        panel2.add(JLabel(resources.getString("Setup.NextPlayer.Prompt")))
        panel2.add(radioSetupDefaultPlayer)
        panel2.add(radioSetupBlackPlayer)
        panel2.add(radioSetupWhitePlayer)
        panel.add(panel2, BorderLayout.NORTH)
        panelMoveSetup.add(panel, "Setup")
        val node = this.node
        val str: String
        if (node is GoSGFMoveNode) {
            str = "Move"
            comboMoveAnnotation.selectedItem = node.moveAnnotation ?: MoveAnnotationHeader
            spinMoveNumber.value = node.moveNumber
        } else {
            str = "Setup"
            spinMoveNumber.value = 0
        }
        cardMoveSetup.show(panelMoveSetup, str)
        tabs.addTab(resources.getString(str), JScrollPane(panelMoveSetup))
    }

    private val textNodeName = JTextField()
    private val spinNodeValue = CN13Spinner()
    private val comboNodePosition = JComboBox(positionStates)
    private val comboHotspot = JComboBox<Any>(resources.getStringArray("Node.Hotspot.Values"))
    private val defaultPrintMethod: Any = localeToString { resources ->
        resources.getString("PrintMethod.Default.Prefix") +
                (node.parent?.printMethod ?: PrintMethod.PRINT_ALL) +
                resources.getString("PrintMethod.Default.Suffix")
    }
    private val comboPrintMethod = JComboBox(Array(1 + PRINT_METHODS.size) {
        when(it) {
            0 -> defaultPrintMethod
            else -> PRINT_METHODS[it - 1]
        }
    })
    private val comboFigure = JComboBox(FIGURE_MODES)
    private val labelFigureName = JLabel(resources.getString("Figure.Name.Prompt"))
    private val textFigureName = JTextField()
    private val checkFigureDefault = JCheckBox(resources.getString("Figure.Default"))
    private val checkFigureHideCoordinates = JCheckBox(resources.getString("Figure.Coordinates.Hide"))
    private val checkFigureHideName = JCheckBox(resources.getString("Figure.Name.Hide"))
    private val checkFigureIgnoreUnshownMoves = JCheckBox(resources.getString("Figure.Moves.Unshown"))
    private val checkFigureKeepCapturedStones = JCheckBox(resources.getString("Figure.Captured.Show"))
    private val checkFigureHideHoshiDots = JCheckBox(resources.getString("Figure.Hoshi.Hide"))

    init {
        textNodeName.addKeyListener(TextListener.Node(textNodeName, GoSGFNode::nodeName, nodeDelegate))
        spinNodeValue.model = NodeValueFormatter()
        comboNodePosition.addActionListener {
            node.positionState = comboNodePosition.selectedItem as? PositionState
        }
        comboHotspot.addActionListener {
            node.hotspot = comboHotspot.selectedIndex
            SwingUtilities.invokeLater(sgfTreeView::updateUI)
        }
        comboPrintMethod.addActionListener {
            node.printMethod = comboPrintMethod.selectedItem as? PrintMethod
        }
        comboFigure.addActionListener {
            when(comboFigure.selectedIndex) {
                FIGURE_NONE -> node.clearFigure()
                FIGURE_INHERIT -> node.setFigure(null, GoSGFNode.FIGURE_DEFAULT)
                FIGURE_NEW -> {
                    var figureNode = node
                    var name = figureNode.figureName
                    var mode = 0
                    while(name == null) {
                        figureNode = figureNode.parent ?: break
                        name = figureNode.figureName
                    }
                    if (name == null) name = ""
                    else mode = figureNode.figureMode
                    node.setFigure(name, mode)
                }
            }
            selectFigureNode()
        }
        textFigureName.addKeyListener(TextListener.Node(
            textFigureName, GoSGFNode::figureName,
            nodeDelegate
        ))
        checkFigureDefault.addActionListener(DefaultFigureModeListener())
        checkFigureHideCoordinates.addActionListener(FigureModeListener(GoSGFNode.FIGURE_HIDE_COORDINATES))
        checkFigureHideName.addActionListener(FigureModeListener(GoSGFNode.FIGURE_HIDE_DIAGRAM_NAME))
        checkFigureIgnoreUnshownMoves.addActionListener(FigureModeListener(GoSGFNode.FIGURE_IGNORE_HIDDEN_MOVES))
        checkFigureKeepCapturedStones.addActionListener(FigureModeListener(GoSGFNode.FIGURE_KEEP_CAPTURED_STONES))
        checkFigureHideHoshiDots.addActionListener(FigureModeListener(GoSGFNode.FIGURE_HIDE_HOSHI_DOTS))
        val panel = JPanel(BorderLayout())
        val panel2 = JPanel(GridBagLayout())
        panel.add(panel2, BorderLayout.NORTH)
        var row = 0
        val gbc1 = GridBagConstraints()
        gbc1.fill = GridBagConstraints.NONE
        gbc1.anchor = GridBagConstraints.EAST
        gbc1.weightx = 0.0
        gbc1.gridy = row
        panel2.add(JLabel(resources.getString("Node.Name.Prompt")), gbc1)
        val gbc2 = GridBagConstraints()
        gbc2.fill = GridBagConstraints.HORIZONTAL
        gbc2.anchor = GridBagConstraints.WEST
        gbc2.weightx = 1.0
        gbc2.gridx = 1
        gbc2.gridy = row
        panel2.add(textNodeName, gbc2)
        gbc1.gridy = ++row
        panel2.add(JLabel(resources.getString("Node.Value.Prompt")), gbc1)
        gbc2.gridy = row
        panel2.add(spinNodeValue, gbc2)
        gbc2.gridx = 0
        gbc2.gridy = ++row
        gbc2.gridwidth = 2
        panel2.add(comboNodePosition, gbc2)
        gbc2.gridy = ++row
        panel2.add(comboHotspot, gbc2)
        gbc2.gridy = ++row
        panel2.add(comboPrintMethod, gbc2)
        gbc2.gridy = ++row
        panel2.add(comboFigure, gbc2)
        gbc1.gridy = ++row
        panel2.add(labelFigureName, gbc1)
        gbc2.gridx = 1
        gbc2.gridy = row
        gbc2.gridwidth = 1
        panel2.add(textFigureName, gbc2)
        gbc2.gridx = 0
        gbc2.gridy = ++row
        gbc2.gridwidth = 2
        panel2.add(checkFigureDefault, gbc2)
        gbc2.gridy = ++row
        panel2.add(checkFigureHideCoordinates, gbc2)
        gbc2.gridy = ++row
        panel2.add(checkFigureHideName, gbc2)
        gbc2.gridy = ++row
        panel2.add(checkFigureIgnoreUnshownMoves, gbc2)
        gbc2.gridy = ++row
        panel2.add(checkFigureKeepCapturedStones, gbc2)
        gbc2.gridy = ++row
        panel2.add(checkFigureHideHoshiDots, gbc2)
        tabs.addTab(resources.getString("Node"), JScrollPane(panel))
    }

    private val gameInfoTransferHandler = GameInfoTransferHandler(tabs, sgf.charset) {
        (it as? JTabbedPane)?.selectedIndex == 3
    }
    private val buttonCopyGameInfo = JButton(resources.getString("Copy"))
    private val buttonPasteGameInfo = JButton(resources.getString("Paste"))
    private val buttonDeleteGameInfo = JButton(resources.getString("Delete"))
    private val buttonPreviousGameInfoNode = JButton(resources.getString("Previous"))
    private val buttonGameInfoNode = JButton()
    private val buttonNextGameInfoNode = JButton(resources.getString("Next"))
    private val panelGameInfo = JPanel(BorderLayout())
    private val textBlackName = JTextField()
    private val textWhiteName = JTextField()
    private val textBlackTeam = JTextField()
    private val textWhiteTeam = JTextField()
    private val comboBlackRank = JComboBox(RANKS)
    private val comboWhiteRank = JComboBox(RANKS)
    private val spinHandicap = CN13Spinner()
    private val comboKomi = JComboBox<Any>()
    private val spinKomi = CN13Spinner()
    private val spinTimeLimit = CN13Spinner()
    private val comboOvertime = JComboBox<Any>()
    private val overtimeView = PropertiesComponent<Overtime>()
    private val comboGameResult = JComboBox<GameResult>()
    private val comboGameWinner = JComboBox<GameResult>()
    private val spinGameScore = CN13Spinner()

    private val textGameName = JTextField()
    private val textGameSource = JTextField()
    private val textGameUser = JTextField()
    private val textCopyright = JTextField()
    private val textGameLocation = JTextField()
    private val textEventName = JTextField()
    private val textRoundType = JTextField()
    private val textAnnotationProvider = JTextField()
    private val comboRules = JComboBox(RULES_PRESETS)
    private val textOpeningType = JTextField()

    private val spinGameYear = CN13Spinner()
    private val spinGameMonth = CN13Spinner()
    private val spinGameDay = CN13Spinner()
    private val gameYearModel = YearFormatter()
    private val gameMonthModel = MonthFormatter()
    private val gameDayModel = MonthDayFormatter(31)
    private val buttonToday = JButton(resources.getString("Date.Today"))
    private val buttonAddDate = JButton(resources.getString("Add"))
    private val buttonRemoveDate = JButton(resources.getString("Remove"))
    private val datesModel = DateListModel()
    private val listGameDates = JList(datesModel)
    private val textGameComment = JTextArea()

    init {
        tabs.transferHandler = gameInfoTransferHandler
        tabs.addMouseListener(gameInfoTransferHandler)
        buttonCopyGameInfo.addActionListener {
            gameInfoTransferHandler.clipboardContents = node.gameInfo
        }
        buttonPasteGameInfo.addActionListener {
            var info = gameInfoTransferHandler.clipboardContents ?: return@addActionListener
            val old = node.gameInfo
            val warning = when {
                old == null ->
                    if (node.hasGameInfoExcluding(info))
                        "GameInfo.Warning.Children"
                    else null
                old == info -> return@addActionListener
                node.gameInfoNode == node ->
                    "GameInfo.Warning.Selected"
                else -> "GameInfo.Warning.Parent"
            }
            val resources = gobanDesktopResources()
            if (warning == null || JOptionPane.showConfirmDialog(
                    this, resources.getString(warning),
                    resources.getString("GameInfo.Warning.Title"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                ) == JOptionPane.YES_OPTION
            ) {
                info = info.copy()
                node.gameInfo = info
                selectGameInfo(info, resources)
                sgfTreeView.updateUI()
            }
        }
        buttonDeleteGameInfo.addActionListener {
            val resources = gobanDesktopResources()
            if (JOptionPane.showConfirmDialog(
                    this,
                    resources.getString("GameInfo.Delete.Confirm"),
                    resources.getString("GameInfo.Delete.Title"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                ) == JOptionPane.YES_OPTION
            ) {
                node.gameInfo = null
                selectGameInfo(null, resources)
                sgfTreeView.updateUI()
            }
        }
        buttonPreviousGameInfoNode.addActionListener {
            val n = node.previousGameInfoNode
            selectSGFTreePath(n)
            selectSGFNode(n)
        }
        buttonGameInfoNode.addActionListener {
            val gameInfoNode = node.gameInfoNode
            when {
                gameInfoNode == null -> {
                    if (!node.hasGameInfoChildren || gobanDesktopResources().let { resources ->
                            JOptionPane.showConfirmDialog(
                                this,
                                resources.getString("GameInfo.Warning.Children"),
                                resources.getString("GameInfo.Warning.Title"),
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE
                            ) == JOptionPane.YES_OPTION
                        }
                    ) {
                        val info = GameInfo()
                        node.gameInfo = info
                        selectGameInfo(info, resources)
                        sgfTreeView.updateUI()
                    }
                }
                gameInfoNode != node -> {
                    selectSGFTreePath(gameInfoNode)
                    selectSGFNode(gameInfoNode)
                }
            }
        }
        buttonNextGameInfoNode.addActionListener {
            val n = node.nextGameInfoNode
            selectSGFTreePath(n)
            selectSGFNode(n)
        }
        textBlackName.addKeyListener(
            TextListener.GameInfoPlayer(
                textBlackName, GoColor.BLACK,
                GameInfo.Player::name, nodeDelegate
            )
        )
        textWhiteName.addKeyListener(
            TextListener.GameInfoPlayer(
                textWhiteName, GoColor.WHITE,
                GameInfo.Player::name, nodeDelegate
            )
        )
        textBlackTeam.addKeyListener(
            TextListener.GameInfoPlayer(
                textBlackTeam, GoColor.BLACK,
                GameInfo.Player::team, nodeDelegate
            )
        )
        textWhiteTeam.addKeyListener(
            TextListener.GameInfoPlayer(
                textWhiteTeam, GoColor.WHITE,
                GameInfo.Player::team, nodeDelegate
            )
        )
        comboBlackRank.isEditable = true
        comboBlackRank.addKeyListener(
            TextListener.GameInfoPlayer(
                comboBlackRank, GoColor.BLACK,
                GameInfo.Player::rank, nodeDelegate
            )
        )
        comboBlackRank.addActionListener {
            node.gameInfo?.blackPlayer?.rank = comboBlackRank.selectedItem?.toString() ?: ""
        }
        comboWhiteRank.isEditable = true
        comboWhiteRank.addKeyListener(
            TextListener.GameInfoPlayer(
                comboWhiteRank, GoColor.WHITE,
                GameInfo.Player::rank, nodeDelegate
            )
        )
        comboWhiteRank.addActionListener {
            node.gameInfo?.whitePlayer?.rank = comboWhiteRank.selectedItem?.toString() ?: ""
        }
        spinHandicap.model = HandicapFormatter()
        val komiModel = KomiFormatter()
        spinKomi.model = komiModel
        (comboKomi.renderer as? JLabel)?.horizontalAlignment = SwingConstants.RIGHT
        comboKomi.model = komiModel
        spinTimeLimit.model = TimeLimitFormatter()
        spinTimeLimit.adjustCaret = true
        val overtimeModel = OvertimeModel(comboOvertime.renderer)
        comboOvertime.model = overtimeModel
        comboOvertime.renderer = overtimeModel
        val resultModel = ResultModel(comboGameResult.renderer)
        comboGameResult.model = resultModel
        comboGameResult.renderer = resultModel
        comboGameResult.isEditable = false
        (comboGameResult.renderer as? JLabel)?.horizontalAlignment = SwingConstants.RIGHT
        val winnerModel = WinnerModel(comboGameWinner.renderer)
        comboGameWinner.model = winnerModel
        comboGameWinner.renderer = winnerModel
        comboGameWinner.isEditable = false
        spinGameScore.model = GameScoreFormatter()
        textGameName.addKeyListener(TextListener.GameInfo(textGameName, GameInfo::gameName, nodeDelegate))
        textGameSource.addKeyListener(TextListener.GameInfo(textGameSource, GameInfo::gameSource, nodeDelegate))
        textGameUser.addKeyListener(TextListener.GameInfo(textGameUser, GameInfo::gameUser, nodeDelegate))
        textCopyright.addKeyListener(TextListener.GameInfo(textCopyright, GameInfo::copyright, nodeDelegate))
        textGameLocation.addKeyListener(TextListener.GameInfo(textGameLocation, GameInfo::gameLocation, nodeDelegate))
        textEventName.addKeyListener(TextListener.GameInfo(textEventName, GameInfo::eventName, nodeDelegate))
        textRoundType.addKeyListener(TextListener.GameInfo(textRoundType, GameInfo::roundType, nodeDelegate))
        textAnnotationProvider.addKeyListener(
            TextListener.GameInfo(
                textAnnotationProvider,
                GameInfo::annotationProvider, nodeDelegate
            )
        )
        comboRules.isEditable = true
        comboRules.addKeyListener(TextListener.GameInfo(comboRules, GameInfo::rulesString, nodeDelegate))
        comboRules.addActionListener {
            node.gameInfo?.rulesString = comboRules.selectedItem?.toString() ?: ""
        }
        textOpeningType.addKeyListener(TextListener.GameInfo(textOpeningType, GameInfo::openingType, nodeDelegate))
        spinGameYear.model = gameYearModel
        spinGameMonth.model = gameMonthModel
        spinGameDay.model = gameDayModel
        buttonToday.addActionListener {
            val cal = GregorianCalendar()
            gameYearModel.value = cal[Calendar.YEAR]
            gameMonthModel.value = cal[Calendar.MONTH] + 1
            gameDayModel.value = cal[Calendar.DAY_OF_MONTH]
            spinGameDay.isEnabled = true
            spinGameMonth.updateUI()
            spinGameDay.updateUI()
        }
        buttonAddDate.addActionListener {
            val date = Date(
                gameYearModel.year!!, gameMonthModel.value,
                gameDayModel.value
            )
            node.gameInfo?.dates?.addDate(date)
            datesModel.add(date)
            listGameDates.setSelectedValue(date, true)
        }
        buttonRemoveDate.addActionListener {
            datesModel.remove(listGameDates.selectionModel, node.gameInfo?.dates)
            listGameDates.clearSelection()
        }
        listGameDates.addListSelectionListener {
            val date: Date? = listGameDates.selectedValue
            buttonRemoveDate.isEnabled = if (date != null) {
                gameYearModel.value = date.year
                val month = date.month
                gameMonthModel.value = month
                gameDayModel.value = date.day
                spinGameDay.isEnabled = month != 0
                spinGameMonth.updateUI()
                spinGameDay.updateUI()
                true
            } else false
        }
        textGameComment.addKeyListener(TextListener.GameInfo(textGameComment, GameInfo::gameComment, nodeDelegate))
        var panel = JPanel(GridBagLayout())
        var row = 0
        val gbc1 = GridBagConstraints()
        gbc1.gridx = 0
        gbc1.weightx = 0.0
        gbc1.fill = GridBagConstraints.NONE
        gbc1.anchor = GridBagConstraints.EAST
        val gbc2 = GridBagConstraints()
        gbc2.weightx = 1.0
        gbc2.fill = GridBagConstraints.HORIZONTAL
        val gbc3 = GridBagConstraints()
        gbc3.gridx = 1
        gbc3.gridy = row
        gbc3.weightx = 0.0
        gbc3.fill = GridBagConstraints.NONE
        gbc3.anchor = GridBagConstraints.CENTER
        panel.add(JLabel(GoColor.BLACK.toString()), gbc3)
        gbc3.gridx = 2
        panel.add(JLabel(GoColor.WHITE.toString()), gbc3)
        gbc1.gridy = ++row
        panel.add(JLabel(resources.getString("GameInfo.Player.Prompt")), gbc1)
        gbc2.gridx = 1
        gbc2.gridy = row
        panel.add(textBlackName, gbc2)
        gbc2.gridx = 2
        panel.add(textWhiteName, gbc2)
        gbc1.gridy = ++row
        panel.add(JLabel(resources.getString("GameInfo.Team.Prompt")), gbc1)
        gbc2.gridx = 1
        gbc2.gridy = row
        panel.add(textBlackTeam, gbc2)
        gbc2.gridx = 2
        panel.add(textWhiteTeam, gbc2)
        gbc1.gridy = ++row
        panel.add(JLabel(resources.getString("GameInfo.Rank.Prompt")), gbc1)
        gbc2.gridx = 1
        gbc2.gridy = row
        panel.add(comboBlackRank, gbc2)
        gbc2.gridx = 2
        panel.add(comboWhiteRank, gbc2)
        gbc1.gridy = ++row
        panel.add(JLabel(resources.getString("GameInfo.Handicap.Prompt")), gbc1)
        gbc2.gridx = 1
        gbc2.gridy = row
        gbc2.gridwidth = 2
        panel.add(spinHandicap, gbc2)
        gbc1.gridy = ++row
        gbc1.fill = GridBagConstraints.HORIZONTAL
        panel.add(comboKomi, gbc1)
        gbc2.gridy = row
        panel.add(spinKomi, gbc2)
        gbc1.gridy = ++row
        gbc1.fill = GridBagConstraints.NONE
        panel.add(JLabel(resources.getString("TimeLimit.Prompt")), gbc1)
        gbc2.gridy = row
        panel.add(spinTimeLimit, gbc2)
        gbc2.gridx = 0
        gbc2.gridy = ++row
        gbc2.gridwidth = 3
        panel.add(comboOvertime, gbc2)
        gbc2.gridy = ++row
        panel.add(overtimeView, gbc2)
        var panel2 = JPanel(GridLayout(1, 3))
        panel2.add(comboGameResult)
        panel2.add(comboGameWinner)
        panel2.add(spinGameScore)
        gbc2.gridy = ++row
        panel.add(panel2, gbc2)
        gbc1.gridy = ++row
        panel.add(JLabel(resources.getString("GameInfo.Name.Prompt")), gbc1)
        gbc2.gridx = 1
        gbc2.gridy = row
        gbc2.gridwidth = 2
        panel.add(textGameName, gbc2)
        gbc1.gridy = ++row
        panel.add(JLabel(resources.getString("GameInfo.Source.Prompt")), gbc1)
        gbc2.gridy = row
        panel.add(textGameSource, gbc2)
        gbc1.gridy = ++row
        panel.add(JLabel(resources.getString("GameInfo.User.Prompt")), gbc1)
        gbc2.gridy = row
        panel.add(textGameUser, gbc2)
        gbc1.gridy = ++row
        panel.add(JLabel(resources.getString("GameInfo.Copyright.Prompt")), gbc1)
        gbc2.gridy = row
        panel.add(textCopyright, gbc2)
        gbc1.gridy = ++row
        panel.add(JLabel(resources.getString("GameInfo.Location.Prompt")), gbc1)
        gbc2.gridy = row
        panel.add(textGameLocation, gbc2)
        gbc1.gridy = ++row
        panel.add(JLabel(resources.getString("GameInfo.Event.Name.Prompt")), gbc1)
        gbc2.gridy = row
        panel.add(textEventName, gbc2)
        gbc1.gridy = ++row
        panel.add(JLabel(resources.getString("GameInfo.Round.Type.Prompt")), gbc1)
        gbc2.gridy = row
        panel.add(textRoundType, gbc2)
        gbc1.gridy = ++row
        panel.add(JLabel(resources.getString("GameInfo.Annotation.Provider.Prompt")), gbc1)
        gbc2.gridy = row
        panel.add(textAnnotationProvider, gbc2)
        gbc1.gridy = ++row
        panel.add(JLabel(resources.getString("GameInfo.Rules.Prompt")), gbc1)
        gbc2.gridy = row
        panel.add(comboRules, gbc2)
        gbc1.gridy = ++row
        panel.add(JLabel(resources.getString("GameInfo.Opening.Type.Prompt")), gbc1)
        gbc2.gridy = row
        panel.add(textOpeningType, gbc2)
        panel2 = JPanel(GridBagLayout())
        gbc3.gridx = 0
        gbc3.gridy = 0
        panel2.add(JLabel(resources.getString("Date.Year")), gbc3)
        gbc3.gridx = 1
        panel2.add(JLabel(resources.getString("Date.Month")), gbc3)
        gbc3.gridx = 2
        panel2.add(JLabel(resources.getString("Date.Day")), gbc3)
        spinGameDay.isEnabled = false
        gbc2.gridx = 0
        gbc2.gridy = 1
        gbc2.gridwidth = 1
        panel2.add(spinGameYear, gbc2)
        gbc2.gridx = 1
        panel2.add(spinGameMonth, gbc2)
        gbc2.gridx = 2
        panel2.add(spinGameDay, gbc2)
        gbc2.gridx = 0
        gbc2.gridy = 2
        panel2.add(buttonToday, gbc2)
        gbc2.gridx = 1
        panel2.add(buttonAddDate, gbc2)
        gbc2.gridx = 2
        panel2.add(buttonRemoveDate, gbc2)
        gbc2.gridx = 0
        gbc2.gridy = ++row
        gbc2.gridwidth = 3
        panel.add(panel2, gbc2)
        gbc2.gridy = ++row
        panel.add(listGameDates, gbc2)
        gbc3.gridx = 0
        gbc3.gridy = ++row
        gbc3.gridwidth = 3
        panel.add(JLabel(resources.getString("GameInfo.Comment.Prompt")), gbc3)
        panelGameInfo.add(panel, BorderLayout.NORTH)
        panelGameInfo.add(textGameComment, BorderLayout.CENTER)
        panel = JPanel(BorderLayout())
        panel2 = JPanel(GridLayout(2, 3))
        panel2.add(buttonCopyGameInfo)
        panel2.add(buttonPasteGameInfo)
        panel2.add(buttonDeleteGameInfo)
        panel2.add(buttonPreviousGameInfoNode)
        panel2.add(buttonGameInfoNode)
        panel2.add(buttonNextGameInfoNode)
        panel.add(panel2, BorderLayout.NORTH)
        panel.add(panelGameInfo, BorderLayout.CENTER)
        tabs.addTab(resources.getString("GameInfo"), JScrollPane(panel))
    }

    private val labelSize = JLabel(sizeFormat.format(sgf.width, sgf.height))
    private val comboCharset = JComboBox<Charset?>()
    private val checkAutoMarkup = JCheckBox(resources.getString("Root.Markup.Auto"))
    private val radioChildVariations = JRadioButton(resources.getString("Root.Variations.Children"))
    private val radioSiblingVariations = JRadioButton(resources.getString("Root.Variations.Siblings"))

    init {
        comboCharset.isEditable = false
        val charsetModel = CharsetModel(comboCharset.renderer)
        comboCharset.model = charsetModel
        comboCharset.renderer = charsetModel
        comboCharset.selectedItem = sgf.charset
        comboCharset.addActionListener {
            val charset = comboCharset.selectedItem as Charset?
            sgf.charset = charset
            gameInfoTransferHandler.charset = charset
        }
        val variationFlags = sgf.variationView
        checkAutoMarkup.isSelected = variationFlags and GoSGF.NO_MARKUP_VARIATION == 0
        checkAutoMarkup.addActionListener {
            val flags = sgf.variationView
            sgf.variationView = if (checkAutoMarkup.isSelected)
                flags and GoSGF.NO_MARKUP_VARIATION.inv()
            else flags or GoSGF.NO_MARKUP_VARIATION
        }
        val variationGroup = ButtonGroup()
        variationGroup.add(radioChildVariations)
        variationGroup.add(radioSiblingVariations)
        if (variationFlags and GoSGF.CURRENT_VARIATION == 0)
            radioChildVariations.isSelected = true
        else radioSiblingVariations.isSelected = true
        radioChildVariations.addActionListener {
            sgf.variationView = sgf.variationView and GoSGF.CURRENT_VARIATION.inv()
        }
        radioSiblingVariations.addActionListener {
            sgf.variationView = sgf.variationView or GoSGF.CURRENT_VARIATION
        }
        var panel = JPanel(GridLayout(6, 1))
        panel.add(labelSize)
        var panel2 = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 0.0
        gbc.anchor = GridBagConstraints.LINE_START
        gbc.fill = GridBagConstraints.NONE
        panel2.add(JLabel(resources.getString("Encoding.Prompt")), gbc)
        gbc.gridx = 1
        gbc.weightx = 1.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        panel2.add(comboCharset, gbc)
        panel.add(panel2)
        panel.add(checkAutoMarkup)
        panel.add(JLabel(resources.getString("Root.Variations.Prompt")))
        panel.add(radioChildVariations)
        panel.add(radioSiblingVariations)
        panel2 = panel
        panel = JPanel(BorderLayout())
        panel.add(panel2, BorderLayout.NORTH)
        tabs.addTab(resources.getString("Root"), JScrollPane(panel))
        selectSGFNode(node, resources)
    }

    private fun goPointClicked(e: MouseEvent) {
        val p = gobanView.toGoPoint(e) ?: return
        if (p != goCursor) return
        val isBlack = buttonBlack.isSelected
        if (isBlack || buttonWhite.isSelected) {
            val newStone = isBlack.goBlackOrWhite()
            val g = goban.playable()
            goban = g
            gobanView.goban = g
            if (buttonSGFSetup.isSelected) {
                g[p] = if (g[p] == newStone) null
                else newStone
            } else {
                var blackStones = g.blackCount
                var whiteStones = g.whiteCount
                if (isBlack) blackStones++
                else whiteStones++
                if (!g.play(p, newStone)) return
                playStoneAt(p, newStone)
                blackScore += whiteStones - g.whiteCount
                whiteScore += blackStones - g.blackCount
                val resources = gobanDesktopResources()
                labelBlackScore.text = resources.getString("Score.Black.Prefix") +
                        blackScore + resources.getString("Score.Black.Suffix")
                labelWhiteScore.text = resources.getString("Score.White.Prefix") +
                        whiteScore + resources.getString("Score.White.Suffix")
            }
        } else if (!buttonSGFSetup.isSelected) run markup@{
            val addLine = buttonLineMarkup.isSelected
            val addArrow = buttonArrowMarkup.isSelected
            if (!addLine && !addArrow && !buttonDeleteLineMarkup.isSelected) {
                if (buttonDeletePointMarkup.isSelected)
                    pointMarkupMap.remove(p)
                else {
                    pointMarkupMap[p] = when {
                        buttonLabelMarkup.isSelected -> {
                            val text: String? = JOptionPane.showInputDialog(
                                this,
                                gobanDesktopResources().getString("Markup.Label.Prompt"),
                                pointMarkupMap[p]?.label
                            )
                            if (text.isNullOrEmpty()) return@markup
                            PointMarkup.label(text)
                        }
                        buttonSelectMarkup.isSelected -> PointMarkup.SELECT
                        buttonXMarkup.isSelected -> PointMarkup.X
                        buttonTriangleMarkup.isSelected -> PointMarkup.TRIANGLE
                        buttonCircleMarkup.isSelected -> PointMarkup.CIRCLE
                        buttonSquareMarkup.isSelected -> PointMarkup.SQUARE
                        else -> return@markup
                    }
                }
            } else {
                val startLine = this.startLine
                if (startLine == null) {
                    this.startLine = p
                } else {
                    this.startLine = null
                    val lm = when {
                        startLine == p -> return@markup
                        addLine -> startLine lineMarkup p
                        addArrow -> startLine arrowMarkup p
                        else -> null
                    }
                    if (lm != null)
                        lineMarkupSet.add(lm)
                    else if (lineMarkupSet.remove(startLine, p) == null)
                        lineMarkupSet.remove(p, startLine)
                }
            }
        }
        gobanView.revalidate()
        gobanView.repaint()
    }

    private fun playStoneAt(p: GoPoint?, player: GoColor) {
        val move = node.createNextMoveNode(p, player)
        move.moveVariation(0)
        if (p == null) selectSGFTreePathFast(move)
        else {
            SUPPRESS_COMPUTE_SCORES.incrementAndGet(this)
            try {
                selectSGFTreePathFast(move)
            } finally {
                SUPPRESS_COMPUTE_SCORES.decrementAndGet(this)
            }
        }
        val nextButton = if (player == GoColor.BLACK) {
            buttonBlack.isSelected = false
            buttonWhite
        } else {
            buttonWhite.isSelected = false
            buttonBlack
        }
        nextButton.isSelected = true
        selectedToolButton = nextButton
        val pointMarkup = move.pointMarkup
        pointMarkupMap = pointMarkup
        gobanView.pointMarkup = pointMarkup
        val lineMarkup = move.lineMarkup
        lineMarkupSet = lineMarkup
        gobanView.lineMarkup = lineMarkup
    }

    private fun selectSGFTreePathFast(node: GoSGFNode) {
        var treePath: TreePath? = sgfTreeView.selectionPath
        if (this.node.parent?.children == 1)
            treePath = treePath?.parentPath
        treePath = treePath?.pathByAddingChild(node) ?: TreePath(node)
        sgfTreeView.updateUI()
        sgfTreeView.selectionPath = treePath
    }

    private fun selectSGFTreePath(node: GoSGFNode) {
        sgfTreeView.updateUI()
        sgfTreeView.selectionPath = SGFTreeModel.pathTo(node)
    }

    private fun selectSGFPlayer(node: GoSGFNode) {
        if (SGFTreeModel.getNextPlayer(node) == GoColor.BLACK) {
            selectedToolButton = buttonBlack
            buttonBlack.isSelected = true
        } else {
            selectedToolButton = buttonWhite
            buttonWhite.isSelected = true
        }
        buttonPass.isEnabled = true
    }

    private fun selectSGFNode(node: GoSGFNode,
                              resources: ResourceBundle = gobanDesktopResources()) {
        this.node = node
        textComment.text = node.comment
        val nodeType: String
        when(node) {
            is GoSGFMoveNode -> {
                nodeType = "Move"
                checkMoveForced.isSelected = node.isForced
                comboMoveAnnotation.selectedItem = node.moveAnnotation ?: MoveAnnotationHeader
                spinMoveNumber.updateUI()
                spinBlackTime.updateUI()
                spinWhiteTime.updateUI()
                spinBlackOvertime.updateUI()
                spinWhiteOvertime.updateUI()

            }
            is GoSGFSetupNode -> {
                nodeType = "Setup"
                when(node.turnPlayer) {
                    GoColor.BLACK -> radioSetupBlackPlayer
                    GoColor.WHITE -> radioSetupWhitePlayer
                    else -> radioSetupDefaultPlayer
                }.isSelected = true
            }
        }
        tabs.setTitleAt(1, resources.getString(nodeType))
        cardMoveSetup.show(panelMoveSetup, nodeType)
        textNodeName.text = node.nodeName
        spinNodeValue.updateUI()
        comboNodePosition.selectedItem = node.positionState ?: PositionStateHeader
        comboHotspot.selectedIndex = node.hotspot
        comboPrintMethod.selectedItem = if (node.printMethodNode != node)
            defaultPrintMethod
        else node.printMethod ?: defaultPrintMethod
        comboFigure.selectedIndex = when {
            node.figureName != null -> FIGURE_NEW
            node.figureMode != 0 -> FIGURE_INHERIT
            else -> FIGURE_NONE
        }
        selectFigureNode()
        if (!gameInfoTransferHandler.isDragging)
            selectGameInfo(node.gameInfo, resources)
        buttonSGFDelete.isEnabled = node.parent != null
        selectedToolButton.isSelected = false
        buttonInheritDim.isSelected = node.newDimPoints == null
        buttonInheritVisible.isSelected = node.newVisiblePoints == null
        selectSGFPlayer(node)
        updateNodeGoban(node)
        enableSGFVariations()
        blackScore = 0
        whiteScore = 0
        if (suppressComputeScores <= 0) {
            computeScores(node)
            labelBlackScore.text = resources.getString("Score.Black.Prefix") +
                    blackScore + resources.getString("Score.Black.Suffix")
            labelWhiteScore.text = resources.getString("Score.White.Prefix") +
                    whiteScore + resources.getString("Score.White.Suffix")
        }
    }

    @Volatile private var suppressComputeScores: Int = 0

    private fun selectGameInfo(info: GameInfo?, resources: ResourceBundle) {
        gameInfoTransferHandler.gameInfo = info
        if (info == null) {
            panelGameInfo.isVisible = false
            buttonCopyGameInfo.isEnabled = false
            buttonDeleteGameInfo.isEnabled = false
            buttonGameInfoNode.text = resources.getString("Create")
        } else {
            panelGameInfo.isVisible = true
            buttonCopyGameInfo.isEnabled = true
            buttonDeleteGameInfo.isEnabled = true
            buttonGameInfoNode.text = resources.getString("GameInfo.Node")
            textBlackName.text = info.blackPlayer.name
            textWhiteName.text = info.whitePlayer.name
            textBlackTeam.text = info.blackPlayer.team
            textWhiteTeam.text = info.whitePlayer.team
            comboBlackRank.selectedItem = info.blackPlayer.rank
            comboWhiteRank.selectedItem = info.whitePlayer.rank
            spinHandicap.updateUI()
            spinKomi.updateUI()
            spinTimeLimit.updateUI()
            comboOvertime.updateUI()
            overtimeView.data = info.overtime
            val enableWinner: Boolean
            val enableScore: Boolean
            val result = info.result
            if (result?.winner != null) {
                enableWinner = true
                enableScore = !result.score.isNaN()
            } else {
                enableWinner = false
                enableScore = false
            }
            comboGameWinner.isEnabled = enableWinner
            spinGameScore.isEnabled = enableScore
            comboGameResult.updateUI()
            comboGameWinner.updateUI()
            spinGameScore.updateUI()
            textGameName.text = info.gameName
            textGameSource.text = info.gameSource
            textGameUser.text = info.gameUser
            textCopyright.text = info.copyright
            textGameLocation.text = info.gameLocation
            textEventName.text = info.eventName
            textRoundType.text = info.roundType
            textAnnotationProvider.text = info.annotationProvider
            comboRules.selectedItem = info.rulesString
            textOpeningType.text = info.openingType
            datesModel.setDates(info.dates)
            listGameDates.updateUI()
            buttonRemoveDate.isEnabled = !listGameDates.isSelectionEmpty
        }
    }

    private fun updateNodeGoban(node: GoSGFNode) {
        val goban = node.goban
        this.goban = goban
        gobanView.goban = goban
        val pointMarkup = node.pointMarkup
        pointMarkupMap = pointMarkup
        gobanView.pointMarkup = pointMarkup
        val lineMarkup = node.lineMarkup
        lineMarkupSet = lineMarkup
        gobanView.lineMarkup = lineMarkup
    }

    @Suppress("DuplicatedCode")
    private fun selectFigureNode() {
        val node = this.node
        var name = node.figureName
        var mode = node.figureMode
        var enabled: Boolean
        val visible: Boolean
        if (name != null) {
            enabled = true
            visible = true
        } else {
            enabled = false
            visible = mode != 0
        }
        labelFigureName.isVisible = visible
        textFigureName.isVisible = visible
        checkFigureDefault.isVisible = visible
        checkFigureHideCoordinates.isVisible = visible
        checkFigureHideName.isVisible = visible
        checkFigureIgnoreUnshownMoves.isVisible = visible
        checkFigureKeepCapturedStones.isVisible = visible
        checkFigureHideHoshiDots.isVisible = visible
        if (!visible) return
        var figureNode = node
        while(name == null) {
            figureNode = figureNode.parent ?: break
            name = figureNode.figureName
            mode = figureNode.figureMode
        }
        textFigureName.text = name ?: ""
        textFigureName.isEnabled = enabled
        val flag = mode and GoSGFNode.FIGURE_DEFAULT != 0
        checkFigureDefault.isSelected = flag
        checkFigureDefault.isEnabled = enabled
        if (flag) enabled = false
        checkFigureHideCoordinates.isSelected = mode and GoSGFNode.FIGURE_HIDE_COORDINATES != 0
        checkFigureHideCoordinates.isEnabled = enabled
        checkFigureHideName.isSelected = mode and GoSGFNode.FIGURE_HIDE_DIAGRAM_NAME != 0
        checkFigureHideName.isEnabled = enabled
        checkFigureIgnoreUnshownMoves.isSelected = mode and GoSGFNode.FIGURE_IGNORE_HIDDEN_MOVES != 0
        checkFigureIgnoreUnshownMoves.isEnabled = enabled
        checkFigureKeepCapturedStones.isSelected = mode and GoSGFNode.FIGURE_KEEP_CAPTURED_STONES != 0
        checkFigureKeepCapturedStones.isEnabled = enabled
        checkFigureHideHoshiDots.isSelected = mode and GoSGFNode.FIGURE_HIDE_HOSHI_DOTS != 0
        checkFigureHideHoshiDots.isEnabled = enabled
    }

    private fun computeScores(node: GoSGFNode) {
        var current = node
        while(current is GoSGFMoveNode) {
            val player = if (current.playStoneAt == null) null else current.turnPlayer
            val goban = current.goban
            current = current.parent!!
            val prev = current.goban
            var prisoners = prev.whiteCount - goban.whiteCount
            if (player == GoColor.WHITE) prisoners++
            if (prisoners != 0) blackScore += prisoners
            prisoners = prev.blackCount - goban.blackCount
            if (player == GoColor.BLACK) prisoners++
            if (prisoners != 0) whiteScore += prisoners
        }
    }

    private fun moveSGFVariation(direction: Int) {
        val node = this.node
        val prev = node.parent ?: return
        val index = node.childIndex + direction
        if (index !in 0 until prev.children) return
        node.moveVariation(index)
        sgfTreeView.updateUI()
        enableSGFVariations()
    }

    private fun enableSGFVariations() {
        val node = this.node
        val prev = node.parent
        val index = node.childIndex
        buttonSGFUp.isEnabled = index > 0
        buttonSGFDown.isEnabled = prev != null && index < prev.children - 1
    }

    private fun endSetupMode() {
        for(button in markupButtons)
            button.isEnabled = true
        sgfTreeView.isEnabled = true
        enableSGFVariations()
        buttonSGFDelete.text = gobanDesktopResources().getString("Delete")
        val node = this.node
        buttonSGFDelete.isEnabled = node.parent != null
        buttonPass.isEnabled = buttonBlack.isSelected || buttonWhite.isSelected
        updateNodeGoban(node)
    }

    override fun dispose() {
        super.dispose()
        println(sgf.toSGFTree())
    }

    private object MoveAnnotationHeader {

        override fun toString(): String {
            return gobanDesktopResources().getString("Move.Annotation.Header")
        }

    }

    private enum class FigureMode {

        NONE, INHERIT, NEW;

        override fun toString(): String {
            return gobanDesktopResources().getStringArray("Figure.Mode")[ordinal]
        }

    }

    private object PositionStateHeader {

        override fun toString(): String {
            return gobanDesktopResources().getString("Node.PositionState.Header")
        }

    }

    @Suppress("RemoveExplicitTypeArguments")
    companion object {

        private val SUPPRESS_COMPUTE_SCORES: AtomicIntegerFieldUpdater<GoEditorFrame> =
            AtomicIntegerFieldUpdater.newUpdater(GoEditorFrame::class.java, "suppressComputeScores")

        private val LABEL = PointMarkup.label("A")

        private val moveAnnotations = enumValues<MoveAnnotation>().let {
            Array<Any>(1 + it.size) { index ->
                if (index == 0) MoveAnnotationHeader
                else it[index - 1]
            }
        }

        private val positionStates = enumValues<PositionState>().let {
            Array<Any>(1 + it.size) { index ->
                if (index == 0) PositionStateHeader
                else it[index - 1]
            }
        }

        private val PRINT_METHODS = enumValues<PrintMethod>()

        private val FIGURE_MODES = enumValues<FigureMode>()

        private const val FIGURE_NONE = 0
        private const val FIGURE_INHERIT = 1
        private const val FIGURE_NEW = 2

        private val RANKS = arrayOf("",
            "30k", "29k", "28k", "27k", "26k", "25k", "24k", "23k", "22k", "21k",
            "20k", "19k", "18k", "17k", "16k", "15k", "14k", "13k", "12k", "11k",
            "10k",  "9k",  "8k",  "7k",  "6k",  "5k",  "4k",  "3k",  "2k",  "1k",
            "1d", "2d", "3d", "4d", "5d", "6d", "7d", "8d", "9d",
            "1p", "2p", "3p", "4p", "5p", "6p", "7p", "8p", "9p"
        )

        private val RULES_PRESETS = arrayOf("", "Japanese", "AGA", "NZ", "GOE")

        private val CHARSETS: Array<Charset> = arrayOf(Charsets.UTF_8) +
                Charset.availableCharsets().values.filter {
            it != Charsets.UTF_8 && it.isValidSGF
        }.sorted()

    }

    private inner class MoveNumberFormatter: CN13Spinner.Formatter(NumberFormat.getIntegerInstance()) {

        init {
            minimum = 0
            allowsInvalid = false
            commitsOnValidEdit = true
        }

        override fun stringToValue(text: String?): Any? {
            return if (text.isNullOrEmpty()) 0
            else super.stringToValue(text)
        }

        override fun valueToString(value: Any?): String {
            return if ((value as? Number)?.toInt() == 0) ""
            else super.valueToString(value)
        }

        override fun getValue(): Any? {
            return (node as? GoSGFMoveNode)?.moveNumber
        }

        override fun setValue(value: Any?) {
            val node = (this@GoEditorFrame.node as? GoSGFMoveNode) ?: return
            val n = (value as? Number)?.toInt() ?: return
            if (n < 0 || n == node.moveNumber) return
            node.moveNumber = n
            fireChangeEvent()
        }

        override fun getPreviousValue(): Any? {
            val n = (node as? GoSGFMoveNode)?.moveNumber ?: return null
            return if (n > 0) n - 1
            else null
        }

        override fun getNextValue(): Any? {
            val n = (node as? GoSGFMoveNode)?.moveNumber ?: return null
            return if (n < Int.MAX_VALUE) n + 1
            else null
        }

    }

    private inner class TimeFormatter(val player: GoColor):
        CN13Spinner.Formatter(NumberFormat.getInstance()) {

        var millis: Milliseconds? = null

        init {
            commitsOnValidEdit = true
            allowsInvalid = false
        }

        override fun stringToValue(text: String?): Any? {
            return if (text.isNullOrEmpty()) null
            else try {
                Milliseconds.parse(text)
            } catch(e: NumberFormatException) {
                throw ParseException(text, 0)
            }
        }

        override fun valueToString(value: Any?): String {
            return value?.toString() ?: ""
        }

        override fun getValue(): Milliseconds? {
            val player = (node as? GoSGFMoveNode)?.time(this.player) ?: return null
            if (player.hasTime) {
                val time = player.time
                var m = millis
                if (m?.toLong() != time) {
                    m = time.toMilliseconds()
                    millis = m
                }
                return m
            }
            millis = null
            return null
        }

        override fun setValue(value: Any?) {
            val player = (node as? GoSGFMoveNode)?.time(this.player) ?: return
            var m: Milliseconds? = null
            val time: Long
            if (value is Milliseconds) {
                m = value
                millis = m
                time = m.toLong()
            } else if (value is Number) {
                time = value.toLong()
                m = millis
                if (m?.toLong() != time) {
                    m = time.toMilliseconds()
                    millis = m
                }
            } else time = 0L
            if (m != null) {
                if (!player.hasTime || player.time != time) {
                    player.time = time
                    fireChangeEvent()
                }
            } else if (player.hasTime) {
                player.omitTime()
                fireChangeEvent()
            }
        }

        override fun getNextValue(): Any? {
            val player = (node as? GoSGFMoveNode)?.time(this.player) ?: return null
            if (!player.hasTime) return Milliseconds.ZERO
            val time = player.time
            return if (time <= Long.MAX_VALUE - 1000L)
                (time + 1000L).toMilliseconds()
            else null
        }

        override fun getPreviousValue(): Any? {
            val player = (node as? GoSGFMoveNode)?.time(this.player) ?: return null
            if (!player.hasTime) return Milliseconds.ZERO
            val time = player.time
            return if (time > Long.MIN_VALUE + 1000L)
                (time - 1000L).toMilliseconds()
            else null
        }

    }

    private inner class OvertimeFormatter(val spinner: CN13Spinner, val player: GoColor):
        CN13Spinner.Formatter(NumberFormat.getIntegerInstance()) {

        var justMinusSign = false

        init {
            commitsOnValidEdit = true
            allowsInvalid = false
        }

        override fun stringToValue(text: String?): Any? {
            return when {
                text.isNullOrEmpty() -> null
                text == "-" -> text
                else -> super.stringToValue(text)
            }
        }

        override fun valueToString(value: Any?): String {
            return when(value) {
                null -> ""
                is String -> value
                else -> super.valueToString(value)
            }
        }

        override fun getValue(): Any? {
            val player = (node as? GoSGFMoveNode)?.time(this.player) ?: return null
            if (player.hasOvertime) {
                val overtime = player.overtime
                return when {
                    overtime != 0 -> {
                        justMinusSign = false
                        overtime
                    }
                    justMinusSign -> "-"
                    else -> 0
                }
            }
            justMinusSign = false
            return null
        }

        override fun setValue(value: Any?) {
            val player = (node as? GoSGFMoveNode)?.time(this.player) ?: return
            val hasOvertime: Boolean
            val overtime: Int
            val minusSign: Boolean
            when (value) {
                "-" -> {
                    minusSign = true
                    hasOvertime = true
                    overtime = 0
                }
                is Number -> {
                    hasOvertime = true
                    overtime = value.toInt()
                    minusSign = false
                }
                else -> {
                    hasOvertime = false
                    overtime = 0
                    minusSign = false
                }
            }
            justMinusSign = minusSign
            if (hasOvertime) {
                if (!player.hasOvertime || player.overtime != overtime) {
                    player.overtime = overtime
                    fireChangeEvent()
                }
            } else if (player.hasOvertime) {
                player.omitOvertime()
                fireChangeEvent()
            }
            if (minusSign)
                SwingUtilities.invokeLater {
                    spinner.editor.textField.caretPosition = 1
                }
        }

        override fun getPreviousValue(): Any? {
            val player = (node as? GoSGFMoveNode)?.time(this.player) ?: return null
            if (!player.hasOvertime) return 0
            val overtime = player.overtime
            return if (overtime > Int.MIN_VALUE) return overtime - 1
            else null
        }

        override fun getNextValue(): Any? {
            val player = (node as? GoSGFMoveNode)?.time(this.player) ?: return null
            if (!player.hasOvertime) return 0
            val overtime = player.overtime
            return if (overtime < Int.MAX_VALUE) overtime + 1
            else null
        }

    }

    private inner class NodeValueFormatter: CN13Spinner.Formatter(NumberFormat.getNumberInstance()) {

        init {
            commitsOnValidEdit = false
            allowsInvalid = true
        }

        override fun stringToValue(text: String?): Any {
            return if (text.isNullOrEmpty()) Double.NaN
            else super.stringToValue(text)
        }

        override fun valueToString(value: Any?): String {
            return if ((value as? Number)?.toDouble()?.isNaN() == false)
                super.valueToString(value)
            else ""
        }

        override fun getValue(): Any {
            return node.positionValue
        }

        override fun setValue(value: Any?) {
            if (value is Number)
                node.positionValue = value.toDouble()
        }

        override fun getNextValue(): Any {
            SwingUtilities.invokeLater(spinNodeValue::updateUI)
            var value = node.positionValue
            if (value.isNaN()) return 0.0
            if (value == ++value)
                value = value.nextUp()
            return value
        }

        override fun getPreviousValue(): Any {
            SwingUtilities.invokeLater(spinNodeValue::updateUI)
            var value = node.positionValue
            if (value.isNaN()) return 0.0
            if (value == --value)
                value = value.nextDown()
            return value
        }

    }

    private val gameResult: GameResult? get() = node.gameInfo?.result

    private object GameResults {
        val noWinner = arrayOf(
            GameResult.DRAW,
            GameResult.UNKNOWN,
            GameResult.VOID
        )
        val blackWins = arrayOf(
            GameResult.BLACK_UNKNOWN,
            GameResult.BLACK_FORFEIT,
            GameResult.BLACK_RESIGN,
            GameResult.BLACK_TIME
        )

        val whiteWins = arrayOf(
            GameResult.WHITE_UNKNOWN,
            GameResult.WHITE_FORFEIT,
            GameResult.WHITE_RESIGN,
            GameResult.WHITE_TIME
        )
    }

    private inner class ResultModel(
        renderer: ListCellRenderer<*>
    ): AbstractListModel<GameResult>(),
            ComboBoxModel<GameResult>, ListCellRenderer<GameResult> {

        @Suppress("UNCHECKED_CAST")
        private val renderer = renderer as ListCellRenderer<in Any>

        init {
            (renderer as? JLabel)?.horizontalAlignment = SwingConstants.RIGHT
        }

        override fun getListCellRendererComponent(
            list: JList<out GameResult>?,
            value: GameResult?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            return renderer.getListCellRendererComponent(
                list,
                gobanDesktopResources().getString(when {
                    value == null -> "GameInfo.Result.Header"
                    value.winner == GoColor.BLACK -> "GameInfo.Result.Winner.Black"
                    value.winner == GoColor.WHITE -> "GameInfo.Result.Winner.White"
                    value == GameResult.UNKNOWN -> "GameInfo.Result.Unknown"
                    value == GameResult.VOID -> "GameInfo.Result.Void"
                    else -> "GameInfo.Result.Draw"
                }),
                index,
                isSelected,
                cellHasFocus
            )
        }

        override fun getSelectedItem(): Any? = gameResult

        override fun setSelectedItem(anItem: Any?) {
            if (anItem !is GameResult?) return
            val info = node.gameInfo ?: return
            info.result = anItem
            val enableWinner: Boolean
            val enableScore: Boolean
            if (anItem?.winner != null) {
                enableWinner = true
                enableScore = anItem?.score?.isNaN() == false
            } else {
                enableWinner = false
                enableScore = false
            }
            comboGameWinner.isEnabled = enableWinner
            comboGameWinner.updateUI()
            spinGameScore.isEnabled = enableScore
            spinGameScore.updateUI()
        }

        override fun getSize(): Int {
            return 3 + GameResults.noWinner.size
        }

        override fun getElementAt(index: Int): GameResult? {
            if (index !in 1 until size) return null
            val winner = if (index == 1) {
                GoColor.BLACK
            } else {
                if (index != 2) return GameResults.noWinner[index - 3]
                GoColor.WHITE
            }
            val result = gameResult
            return when(result?.winner) {
                null -> if (winner == GoColor.BLACK) GameResult.BLACK_UNKNOWN else GameResult.WHITE_UNKNOWN
                winner -> result
                else -> result.opposite
            }
        }

    }

    private inner class WinnerModel(
        renderer: ListCellRenderer<*>
    ): AbstractListModel<GameResult>(),
            ComboBoxModel<GameResult>, ListCellRenderer<GameResult> {

        @Suppress("UNCHECKED_CAST")
        private val renderer = renderer as ListCellRenderer<in Any>

        init {
            (renderer as? JLabel)?.horizontalAlignment = SwingConstants.CENTER
        }

        override fun getListCellRendererComponent(
            list: JList<out GameResult>?,
            value: GameResult?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component = renderer.getListCellRendererComponent(
            list,
            if (value == null) ""
            else gobanDesktopResources().getString(when(value.charCode) {
                'F' -> "GameInfo.Result.Winner.Forfeit"
                'R' -> "GameInfo.Result.Winner.Resign"
                'T' -> "GameInfo.Result.Winner.Time"
                else -> "GameInfo.Result.Winner.Amount"
            }),
            index,
            isSelected,
            cellHasFocus
        )

        override fun getSelectedItem(): Any? {
            val result = node.gameInfo?.result
            return if (result?.winner != null) result
            else null
        }

        override fun setSelectedItem(anItem: Any?) {
            if (anItem !is GameResult) return
            val info = node.gameInfo ?: return
            if (anItem.winner == null) return
            info.result = anItem
            spinGameScore.isEnabled = !anItem.score.isNaN()
            spinGameScore.updateUI()
        }

        override fun getSize(): Int {
            return if (gameResult?.winner == null) 0
            else GameResults.blackWins.size
        }

        override fun getElementAt(index: Int): GameResult? {
            if (index < 0) return null
            val winner = gameResult?.winner ?: return null
            val elements = if (winner == GoColor.BLACK) GameResults.blackWins
            else GameResults.whiteWins
            return if (index >= elements.size) null
            else elements[index]
        }

    }

    private inner class GameScoreFormatter: CN13Spinner.Formatter(NumberFormat.getInstance()) {

        init {
            commitsOnValidEdit = true
            allowsInvalid = false
        }

        override fun stringToValue(text: String?): Any? {
            val winner = gameResult?.winner ?: GoColor.BLACK
            if (text.isNullOrEmpty()) return GameResult(winner, 0f)
            var s: String = text
            val i = s.indexOf('.')
            val hasFraction = i >= 0
            if (hasFraction) {
                s = s.substring(0, i)
            }
            var score = try {
                s.toShort().toFloat()
            } catch(e: NumberFormatException) {
                throw ParseException(text, 0)
            }
            if (hasFraction) score += 0.5f
            return GameResult(winner, score)
        }

        override fun valueToString(value: Any?): String = when {
            value !is GameResult -> ""
            value.score > 0 -> value.toString().substring(2)
            else -> ""
        }

        override fun getValue(): Any? = gameResult

        override fun setValue(value: Any?) {
            if (value !is GameResult) return
            val info = node.gameInfo ?: return
            val old = info.result ?: return
            val oldWinner = old.winner ?: return
            if (value.score.isNaN() || old.score.isNaN()) return
            val winner = value.winner ?: return
            if (winner != oldWinner)
                comboGameResult.updateUI()
            if (value != old) {
                info.result = value
                fireChangeEvent()
            }
        }

        override fun getNextValue(): Any? {
            val result = gameResult ?: return null
            val winner = result.winner ?: return null
            val score = result.score
            return if (score.isNaN()) null
            else GameResult(winner, score + 0.5f)
        }

        override fun getPreviousValue(): Any? {
            val result = gameResult ?: return null
            val winner = result.winner ?: return null
            val score = result.score
            return if (score.isNaN()) null
            else GameResult(winner, score - 0.5f)
        }

    }

    private inner class HandicapFormatter:
        CN13Spinner.Formatter(NumberFormat.getIntegerInstance()) {

        var justMinusSign = false

        init {
            commitsOnValidEdit = true
            allowsInvalid = false
        }

        @Suppress("IMPLICIT_CAST_TO_ANY")
        override fun stringToValue(text: String?): Any? {
            return when(text) {
                null, "" ->
                    if (node.gameInfo?.handicap != 0 || justMinusSign) Zero.plus
                    else throw ParseException("", 0)
                "-", "-0" -> Zero.minus
                else -> try {
                    val i = text.toInt()
                    if (i == 0) Zero.plus else i
                } catch(e: NumberFormatException) {
                    throw ParseException(text, 0)
                }
            }
        }

        override fun valueToString(value: Any?): String {
            return when(value) {
                null -> ""
                Zero.minus -> "-0"
                Zero.plus -> "0"
                else -> value.toString()
            }
        }

        override fun getValue(): Any? {
            val handicap = node.gameInfo?.handicap ?: return null
            return when {
                handicap != 0 -> {
                    justMinusSign = false
                    handicap
                }
                justMinusSign -> Zero.minus
                else -> Zero.plus
            }
        }

        override fun setValue(value: Any?) {
            val info = node.gameInfo ?: return
            val old = info.handicap
            val oldMinusSign = justMinusSign
            val handicap: Int
            val minusSign: Boolean
            when (value) {
                Zero.minus -> {
                    handicap = 0
                    minusSign = true
                }
                is Number -> {
                    handicap = value.toInt()
                    minusSign = false
                }
                else -> return
            }
            justMinusSign = minusSign
            info.handicap = handicap
            if (old != handicap || oldMinusSign != minusSign)
                fireChangeEvent()
            if (handicap == 0) SwingUtilities.invokeLater {
                spinHandicap.editor.textField.caretPosition = if (old == 0 && minusSign && !oldMinusSign) 2 else 1
            }
        }

        override fun getNextValue(): Any? {
            val value = node.gameInfo?.handicap ?: return null
            return when(value) {
                Int.MAX_VALUE -> null
                -1 -> Zero.plus
                else -> value + 1
            }
        }

        override fun getPreviousValue(): Any? {
            val value = node.gameInfo?.handicap ?: return null
            return when(value) {
                Int.MIN_VALUE -> null
                1 -> Zero.plus
                else -> value - 1
            }
        }

    }

    private inner class KomiFormatter: AbstractKomiFormatter() {

        override val gameInfo: GameInfo?
            get() = node.gameInfo

        override val spinKomi: CN13Spinner
            get() = this@GoEditorFrame.spinKomi

    }

    private object YearLimit {

        val min: Int? = Date.MIN_YEAR
        val max: Int? = Date.MAX_YEAR

    }

    private class YearFormatter: CN13Spinner.Formatter() {

        var year: Int? = 0

        init {
            commitsOnValidEdit = true
            allowsInvalid = true
            valueClass = Int::class.javaObjectType
        }

        override fun valueToString(value: Any?): String {
            return value?.toString() ?: super.valueToString(value)
        }

        override fun getValue(): Any? {
            return year
        }

        override fun setValue(value: Any?) {
            var boxed: Int? = null
            val unboxed: Int
            if (value is Int) {
                boxed = value
                unboxed = boxed
            } else {
                if (value !is Number) return
                unboxed = value.toInt()
            }
            if (year == null || year != unboxed) {
                year = when {
                    unboxed <= Date.MIN_YEAR -> YearLimit.min
                    unboxed >= Date.MAX_YEAR -> YearLimit.max
                    boxed == null -> unboxed
                    else -> boxed
                }
                fireChangeEvent()
            }
        }

        override fun getPreviousValue(): Any? {
            val y: Int = year!!
            return when {
                y <= Date.MIN_YEAR -> null
                y == Date.MIN_YEAR + 1 -> YearLimit.min
                else -> y - 1
            }
        }

        override fun getNextValue(): Any? {
            val y: Int = year!!
            return when {
                y >= Date.MAX_YEAR -> null
                y == Date.MAX_YEAR - 1 -> YearLimit.max
                else -> y + 1
            }
        }

    }

    private open class MonthDayFormatter(var max: Int): CN13Spinner.Formatter(NumberFormat.getIntegerInstance()) {

        @JvmField
        var value: Int = 0

        init {
            commitsOnValidEdit = true
            allowsInvalid = false
            valueClass = Integer::class.javaObjectType
        }

        override fun stringToValue(text: String?): Any {
            return if (text.isNullOrEmpty()) 0
            else super.stringToValue(text)
        }

        override fun valueToString(value: Any?): String {
            return if ((value as? Number)?.toInt() == 0) ""
            else super.valueToString(value)
        }

        override fun getValue(): Any = value

        override fun setValue(value: Any?) {
            if (value !is Number) return
            val m  = max + 1
            var i = value.toInt() % m
            if (i < 0) i += m
            if (this.value != i) {
                this.value = i
                fireChangeEvent()
            }
        }

        override fun getPreviousValue(): Any {
            val i = value
            return if (i == 0) max else i - 1
        }

        override fun getNextValue(): Any {
            val i = value
            return if (i == max) 0 else i + 1
        }

    }

    private inner class MonthFormatter: MonthDayFormatter(12) {

        override fun setValue(value: Any?) {
            super.setValue(value)
            val month = this.value
            spinGameDay.isEnabled = month != 0
            val max = Date.daysInMonth(month)
            gameDayModel.max = max
            if (gameDayModel.value > max) {
                gameDayModel.value = max
                spinGameDay.updateUI()
            }
        }

    }

    private inner class TimeLimitFormatter: AbstractTimeLimitFormatter() {

        override val gameInfo: GameInfo?
            get() = node.gameInfo

    }

    private inner class OvertimeModel(
        renderer: ListCellRenderer<in Any>
    ): AbstractOvertimeModel(renderer) {

        override val gameInfo: GameInfo?
            get() = node.gameInfo

        override val overtimeView: PropertiesComponent<Overtime>
            get() = this@GoEditorFrame.overtimeView

    }

    private open inner class FigureModeListener(val flag: Int): ActionListener {

        override fun actionPerformed(e: ActionEvent) {
            val src = e.source as? JCheckBox ?: return
            val mode = node.figureMode
            val checked = src.isSelected
            handleCheckbox(checked)
            node.figureMode = if (checked) mode or flag
            else mode and flag.inv()

        }

        open fun handleCheckbox(checked: Boolean) = Unit

    }

    private inner class DefaultFigureModeListener: FigureModeListener(GoSGFNode.FIGURE_DEFAULT) {

        override fun handleCheckbox(checked: Boolean) {
            val enabled = !checked
            checkFigureHideCoordinates.isEnabled = enabled
            checkFigureHideName.isEnabled = enabled
            checkFigureIgnoreUnshownMoves.isEnabled = enabled
            checkFigureKeepCapturedStones.isEnabled = enabled
            checkFigureHideHoshiDots.isEnabled = enabled
        }

    }

    private inner class DateListModel: AbstractListModel<Date>() {

        val dateList = mutableListOf<Date>()

        fun setDates(dates: DateSet?) {
            dateList.clear()
            if (dates != null) {
                for(date in dates)
                    dateList.add(date)
            }
        }

        fun add(date: Date) {
            var index = dateList.binarySearch(date)
            var end: Int
            if (index < 0) run insertDate@{
                index = -1 - index
                var other: Date
                if (date.month == 0) {
                    end = index
                    while(end < dateList.size) {
                        other = dateList[end]
                        if (date.year != other.year) break
                        end++
                    }
                    if (end > index) {
                        dateList[index] = date
                        dateList.subList(index + 1, end).clear()
                        return@insertDate
                    }
                } else {
                    if (index > 0) {
                        other = dateList[index - 1]
                        if (date.year == other.year && other.month == 0) {
                            dateList[index - 1] = date
                            return@insertDate
                        }
                    }
                    if (date.day == 0) {
                        end = index
                        while(end < dateList.size) {
                            other = dateList[end]
                            if (date.year != other.year || date.month != other.month)
                                break
                            end++
                        }
                        if (end > index) {
                            dateList[index] = date
                            dateList.subList(index + 1, end).clear()
                            return@insertDate
                        }
                    } else if (index > 0) {
                        other = dateList[index - 1]
                        if (date.year == other.year && date.month == other.month &&
                                other.day == 0) {
                            dateList[index - 1] = date
                            return@insertDate
                        }
                    }
                }
                dateList.add(index, date)
            }
            listGameDates.updateUI()
            listGameDates.selectedIndex = index
        }

        fun remove(selection: ListSelectionModel, dates: DateSet?) {
            val min = selection.minSelectionIndex
            val max = selection.maxSelectionIndex
            if (min < 0 || max < 0) return
            var until = -1
            for(i in max downTo min) {
                if (selection.isSelectedIndex(i)) {
                    if (until < 0) until = i + 1
                } else if (until >= 0) {
                    if (dates != null) for(d in (i + 1) until until) dates.remove(dateList[d])
                    if (i + 2 == until) dateList.removeAt(i + 1)
                    else dateList.subList(i + 1, until).clear()
                    until = -1
                }
            }
            if (until >= 0) {
                if (dates != null) for(d in min until until) dates.remove(dateList[d])
                if (min + 1 == until) dateList.removeAt(min)
                else dateList.subList(min, until).clear()
            }
            listGameDates.updateUI()
        }

        override fun getSize() = dateList.size

        override fun getElementAt(index: Int): Date = dateList[index]

    }

    private inner class CharsetModel(
        renderer: ListCellRenderer<*>
    ): ComboBoxModel<Charset?>, ListCellRenderer<Charset?> {

        @Suppress("UNCHECKED_CAST")
        private val renderer = renderer as ListCellRenderer<in Any>

        override fun getListCellRendererComponent(
            list: JList<out Charset?>,
            value: Charset?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component = renderer.getListCellRendererComponent(
            list,
            value?.displayName() ?: gobanDesktopResources().getString("Encoding.Default"),
            index, isSelected, cellHasFocus
        )

        override fun getSelectedItem(): Any? {
            return sgf.charset
        }

        override fun setSelectedItem(anItem: Any?) {
            var charset: Charset? = null
            val update: Boolean = when(anItem) {
                null -> true
                is Charset -> {
                    charset = anItem
                    true
                }
                is String -> try {
                    charset = charset(anItem)
                    true
                } catch(e: Exception) {
                    false
                }
                else -> false
            }
            if (update) {
                sgf.charset = charset
                gameInfoTransferHandler.charset = charset
            }
        }

        override fun getSize() = 1 + CHARSETS.size

        override fun getElementAt(index: Int): Charset? {
            return if (index == 0) null else CHARSETS[index - 1]
        }

        override fun addListDataListener(l: ListDataListener?) = Unit

        override fun removeListDataListener(l: ListDataListener?) = Unit

    }

}