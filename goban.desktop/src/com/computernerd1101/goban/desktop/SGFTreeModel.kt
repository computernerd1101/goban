package com.computernerd1101.goban.desktop

import com.computernerd1101.goban.*
import com.computernerd1101.goban.desktop.internal.GameInfoTransferHandler
import com.computernerd1101.goban.desktop.resources.*
import com.computernerd1101.goban.sgf.*
import java.awt.Component
import java.awt.datatransfer.*
import javax.swing.*
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import javax.swing.tree.*

class SGFTreeModel: TransferHandler(), TreeModel, TreeCellRenderer {

    private val renderer = DefaultTreeCellRenderer()

    //private val pointFormatter: GoPointFormatter
    private val nodeFormatter: SGFNodeFormatter

    init {
        val resources = gobanDesktopFormatResources()
        //pointFormatter = resources.getObject("GoPointFormatter") as GoPointFormatter
        nodeFormatter = resources.getObject("SGFNodeFormatter") as SGFNodeFormatter
    }

    override fun getTreeCellRendererComponent(
        tree: JTree,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ): Component {
        val cmp = renderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
        val sgf = root ?: return cmp
        val move: String?
        val annotation: MoveAnnotation?
        val icon: Icon
        val node: GoSGFNode
        when(value) {
            sgf -> {
                move = null
                annotation = null
                icon = if (sgf.rootNode.turnPlayer == GoColor.WHITE) iconSetupWhite
                else iconSetupBlack
                node = sgf.rootNode
            }
            is GoSGFMoveNode -> {
                node = value
                move = value.playStoneAt.formatOrPass(sgf.width, sgf.height)
                annotation = value.moveAnnotation
                icon = if (value.turnPlayer == GoColor.BLACK) iconPlayBlack
                else iconPlayWhite
            }
            is GoSGFSetupNode -> {
                node = value
                move = null
                annotation = null
                icon = if (getNextPlayer(node) == GoColor.BLACK) iconSetupBlack
                else iconSetupWhite
            }
            else -> return cmp
        }
        renderer.text = nodeFormatter.format(node.index, move, annotation, node.hotspot,
            node.gameInfoNode === node)
        renderer.icon = icon
        return cmp
    }

    private var root: GoSGF? = null

    override fun getRoot(): GoSGF? {
        return root
    }

    fun setRoot(sgf: GoSGF?) {
        root = sgf
    }

    override fun getChild(parent: Any?, index: Int): Any? {
        if (parent == null || index < 0) return null
        val sgf = root ?: return null
        var node: GoSGFNode = if (parent == sgf) sgf.rootNode
        else {
            if (parent !is GoSGFNode || parent.parent?.children == 1)
                return null
            parent
        }
        var nextCount = node.children
        var i = index
        while(nextCount == 1) {
            node = node.child(0)
            if (i == 0) return node
            i--
            nextCount = node.children
        }
        if (i >= nextCount) return null
        return node.child(i)
    }

    override fun getChildCount(parent: Any?): Int {
        if (parent == null) return 0
        val sgf = root ?: return 0
        var node: GoSGFNode = if (parent == sgf) sgf.rootNode
        else {
            if (parent !is GoSGFNode || parent.parent?.children == 1)
                return 0
            parent
        }
        var count = 0
        var nextCount = node.children
        while(nextCount == 1) {
            count++
            node = node.child(0)
            nextCount = node.children
        }
        return count + nextCount
    }

    override fun isLeaf(node: Any?): Boolean {
        return node is GoSGFNode && node.parent?.children == 1
    }

    override fun valueForPathChanged(path: TreePath, newValue: Any?) {
        val event = TreeModelEvent(this, path)
        synchronized(this) {
            for(i in (listenerList.size - 1) downTo 0) {
                listenerList[i].treeNodesChanged(event)
            }
        }
    }

    override fun getIndexOfChild(parent: Any?, child: Any?): Int {
        if (parent == null || child !is GoSGFNode) return -1
        val sgf = root ?: return -1
        val parentNode: GoSGFNode = if (parent == sgf) sgf.rootNode
        else {
            if (parent !is GoSGFNode || parent.parent?.children == 1)
                return -1
            parent
        }
        if (parentNode == child || child.index <= parentNode.index)
            return -1
        var node = parentNode
        while(true) {
            if (node.index == child.index - 1) {
                if (child.parent != node) return -1
                break
            }
            if (node.children != 1) break
            node = node.child(0)
        }
        return child.childIndex + child.index - parentNode.index - 1
    }

    private val listenerList = mutableListOf<TreeModelListener>()

    override fun addTreeModelListener(l: TreeModelListener?) {
        if (l != null) {
            synchronized(this) {
                listenerList.add(l)
            }
        }
    }

    override fun removeTreeModelListener(l: TreeModelListener?) {
        if (l != null) {
            synchronized(this) {
                listenerList.remove(l)
            }
        }
    }

    override fun canImport(support: TransferSupport): Boolean {
        return import(support, doImport = false)
    }

    override fun importData(support: TransferSupport): Boolean {
        return import(support, doImport = true)
    }

    private fun import(support: TransferSupport, doImport: Boolean): Boolean {
        if (!support.isDrop) return false
        support.setShowDropLocation(true)
        val isNode = support.isDataFlavorSupported(Private.nodeFlavor)
        val isGameInfo = support.isDataFlavorSupported(GameInfoTransferHandler.gameInfoFlavor)
        if (!isNode && !isGameInfo) return false
        val path = (support.dropLocation as? JTree.DropLocation)?.path ?: return false
        return when {
            isNode -> importNode(support, path, doImport)
            else -> importGameInfo(support, path, doImport)
        }
    }

    private fun importNode(
        support: TransferSupport,
        path: TreePath,
        doImport: Boolean
    ): Boolean {
        val dstNode = path.lastPathComponent as? GoSGFNode ?: return false
        val data: Transferable = support.transferable
        val srcNode: GoSGFNode = try {
            data.getTransferData(Private.nodeFlavor) as? GoSGFNode
        } catch(e: Exception) {
            null
        } ?: return false
        if (srcNode.parent !== dstNode.parent) return false
        if (doImport) {
            val fromIndex = srcNode.childIndex
            val toIndex = dstNode.childIndex
            val firstIndex: Int
            val childCount: Int
            when {
                fromIndex == toIndex -> return true
                fromIndex < toIndex -> {
                    firstIndex = fromIndex
                    childCount = toIndex - fromIndex + 1
                }
                else -> {
                    firstIndex = toIndex
                    childCount = fromIndex - toIndex + 1
                }
            }
            srcNode.moveVariation(toIndex)
            val pathElements = path.path
            pathElements[pathElements.size - 1] = srcNode
            val childIndices = IntArray(childCount)
            val children = Array<Any>(childCount) { index ->
                childIndices[index] = index + firstIndex
                srcNode.parent!!.child(index + firstIndex)
            }
            val event = TreeModelEvent(
                this,
                pathElements.copyOf(pathElements.size - 1),
                childIndices, children
            )
            synchronized(this) {
                for(i in (listenerList.size - 1) downTo 0) {
                    listenerList[i].treeStructureChanged(event)
                }
            }
            val tree = support.component as JTree
            tree.updateUI()
            tree.selectionPath = TreePath(pathElements)
        }
        return true
    }

    private fun importGameInfo(
        support: TransferSupport,
        path: TreePath,
        doImport: Boolean
    ): Boolean {
        val dstNode = when(val last = path.lastPathComponent) {
            is GoSGFNode -> last
            is GoSGF -> last.rootNode
            else -> return false
        }
        val data: Transferable = support.transferable
        if (doImport) {
            val gameInfo: GameInfo = try {
                data.getTransferData(GameInfoTransferHandler.serializedGameInfoFlavor) as? GameInfo
            } catch(e: Exception) {
                null
            } ?: return false
            val oldGameInfo = dstNode.gameInfo
            val warning = when {
                oldGameInfo == null ->
                    if (dstNode.hasGameInfoExcluding(gameInfo))
                        "GameInfo.Warning.Children"
                    else null
                oldGameInfo == gameInfo -> null
                dstNode.gameInfoNode == dstNode ->
                    "GameInfo.Warning.Selected"
                else -> "GameInfo.Warning.Parent"
            }
            val component = support.component
            if (warning != null) SwingUtilities.invokeLater {
                val resources = gobanDesktopResources()
                if (JOptionPane.showConfirmDialog(
                        component,
                        resources.getString(warning),
                        resources.getString("GameInfo.Warning.Title"),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                    ) == JOptionPane.YES_OPTION)
                    Private.importGameInfo(path, dstNode, gameInfo)
            } else Private.importGameInfo(path, dstNode, gameInfo)
        }
        return true
    }

    private fun Private.importGameInfo(path: TreePath, dstNode: GoSGFNode, info: GameInfo) {
        // setting dstNode.gameInfo to the value it already has
        // is NOT redundant, as it sets dstNode.gameInfoNode to itself.
        dstNode.gameInfo = if (dstNode.gameInfo === info) info
        else info.copy()
        val event = TreeModelEvent(this, path)
        synchronized(this@SGFTreeModel) {
            for(i in (listenerList.size - 1) downTo 0)
                listenerList[i].treeNodesChanged(event)
        }
    }

    override fun createTransferable(c: JComponent?): Transferable? {
        val o = (c as JTree).lastSelectedPathComponent
        return if (o !is GoSGFNode || isLeaf(o)) null
        else NodeTransferable(o)
    }

    override fun getSourceActions(c: JComponent?) = MOVE

    @Suppress("SpellCheckingInspection")
    companion object {
        private val iconPlayBlack = ImageIcon(
            SGFTreeModel::class.java.getResource("icons/treeview/PlayBlack.png"))
        private val iconPlayWhite = ImageIcon(
            SGFTreeModel::class.java.getResource("icons/treeview/PlayWhite.png"))
        private val iconSetupBlack = ImageIcon(
            SGFTreeModel::class.java.getResource("icons/treeview/SetupBlack.png"))
        private val iconSetupWhite = ImageIcon(
            SGFTreeModel::class.java.getResource("icons/treeview/SetupWhite.png"))

        @JvmStatic
        fun pathTo(node: GoSGFNode): TreePath {
            val sgf = node.tree
            var current = node
            var parent = node.parent
            return if (parent == null) TreePath(sgf)
            else {
                val pathList = mutableListOf<Any>(current)
                while(parent != null) {
                    current = parent
                    parent = current.parent
                    when {
                        parent == null -> pathList.add(sgf)
                        parent.children != 1 -> pathList.add(current)
                    }
                }
                pathList.reverse()
                TreePath(pathList.toTypedArray())
            }
        }

        @JvmStatic
        fun getNextPlayer(node: GoSGFNode?): GoColor {
            var current: GoSGFNode? = node ?: return GoColor.BLACK
            while(current is GoSGFSetupNode) {
                val player = current.turnPlayer
                if (player != null) return player
                current = current.parent
            }
            if (current is GoSGFMoveNode)
                return current.turnPlayer.opponent
            val goban = node.goban
            return (goban.whiteCount != 0 || goban.blackCount == 0).goBlackOrWhite()
        }

    }

    private object Private {

        @JvmField val nodeFlavor = try {
            DataFlavor(DataFlavor.javaJVMLocalObjectMimeType +
                    ";class=\"com.computernerd1101.goban.sgf.GoSGFNode\"")
        } catch(e: ClassNotFoundException) {
            throw NoClassDefFoundError(e.message)
        }

    }

    private class NodeTransferable(val node: GoSGFNode): Transferable {

        override fun getTransferData(flavor: DataFlavor?): Any {
            if (!isDataFlavorSupported(flavor)) throw UnsupportedFlavorException(flavor)
            return node
        }

        override fun getTransferDataFlavors() = arrayOf(Private.nodeFlavor)

        override fun isDataFlavorSupported(flavor: DataFlavor?) = Private.nodeFlavor == flavor

    }

}