package com.computernerd1101.goban.poet

import java.awt.*
import java.io.File
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.event.TreeModelListener
import javax.swing.tree.*

fun main(args: Array<String>) {
    val dir = File("goban.desktop/src/com/computernerd1101/goban/desktop/icons/treeview")
        .absoluteFile
    val playBlack = treeViewPlayStone(Color.BLACK, Color.WHITE)
    val playWhite = treeViewPlayStone(Color.WHITE, Color.BLACK)
    val setupBlack = makeIcon(16, 18) { g ->
        g.color = Color.WHITE
        g.fillOval(3, 5, 12, 12)
        g.color = Color.BLACK
        g.fillOval(-1, -1, 14, 14)
        g.color = Color.WHITE
        g.drawOval(-1, -1, 14, 14)
        g.color = Color.BLACK
        g.drawOval(3, 5, 12, 12)
    }
    val setupWhite = makeIcon(16, 18) { g ->
        g.color = Color.BLACK
        g.fillOval(2, 4, 14, 14)
        g.color = Color.WHITE
        g.drawOval(2, 4, 14, 14)
        g.fillOval(0, 0, 12, 12)
        g.color = Color.BLACK
        g.drawOval(0, 0, 12, 12)
    }
    if (GenerateAll.NO_PREVIEW !in args) SwingUtilities.invokeLater {
        val frame = JFrame()
        frame.title = "CN13 Goban Tree View"
        frame.setSize(500, 500)
        frame.setLocationRelativeTo(null)
        frame.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        val treeModel = PreviewTreeModel(
            ImageIcon(setupBlack, "Root"),
            ImageIcon(setupWhite, "Handicap"),
            ImageIcon(playWhite, "1"),
            ImageIcon(playBlack, "2")
        )
        val treeView = JTree(treeModel)
        treeView.cellRenderer = treeModel
        frame.contentPane = treeView
        frame.isVisible = true
    }
    ImageIO.write(playBlack, "PNG", File(dir, "PlayBlack.png"))
    ImageIO.write(playWhite, "PNG", File(dir, "PlayWhite.png"))
    ImageIO.write(setupBlack, "PNG", File(dir, "SetupBlack.png"))
    ImageIO.write(setupWhite, "PNG", File(dir, "SetupWhite.png"))
}

fun treeViewPlayStone(fill: Color, draw: Color) = makeIcon(16, 18) { g ->
    g.color = fill
    g.fillOval(0, 1, 15, 15)
    g.color = draw
    g.drawOval(0, 1, 15, 15)
}

private class PreviewTreeModel(
    private vararg val icons: Icon
): TreeModel, TreeCellRenderer {

    private val renderer = DefaultTreeCellRenderer()

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
        if (value is Icon) {
            renderer.icon = value
        }
        return cmp
    }

    override fun getRoot(): Any = icons[0]

    override fun getChild(parent: Any?, index: Int): Any? =
         if (parent !== icons[0] || index !in 0 until icons.size - 1) null else icons[index + 1]

    override fun getChildCount(parent: Any?): Int = if (parent === icons[0]) icons.size - 1 else 0

    override fun isLeaf(node: Any?): Boolean {
        for(i in icons.indices) if (node === icons[i]) return i != 0
        return false
    }

    override fun getIndexOfChild(parent: Any?, child: Any?): Int {
        if (parent !== icons[0]) return -1
        for(i in icons.indices) if (child === icons[i]) return i - 1 // -1 if i == 0
        return -1
    }

    override fun valueForPathChanged(path: TreePath?, newValue: Any?) = Unit

    override fun addTreeModelListener(l: TreeModelListener?) = Unit

    override fun removeTreeModelListener(l: TreeModelListener?) = Unit

}