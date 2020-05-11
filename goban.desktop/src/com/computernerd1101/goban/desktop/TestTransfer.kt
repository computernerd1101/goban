package com.computernerd1101.goban.desktop

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener

fun main() {
    SwingUtilities.invokeLater {
        val frame = JFrame()
        frame.title = "TestTransferFrame"
        frame.setSize(1000, 500)
        frame.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        frame.setLocationRelativeTo(null)
        val dragTabs = JTabbedPane()
        val fooTab = JLabel("foo")
        val barTab = JLabel("bar")
        dragTabs.addTab("foo", fooTab)
        dragTabs.addTab("bar", barTab)
        val dragHandler = DragHandler(dragTabs)
        dragTabs.transferHandler = dragHandler
        dragTabs.addMouseListener(dragHandler)
        val dropModel = DropListModel()
        val dropList = JList<String>(dropModel)
        dropList.transferHandler = dropModel
        dropList.dragEnabled = true
        val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, dragTabs, dropList)
        SwingUtilities.invokeLater {
            split.setDividerLocation(0.5)
        }
        frame.contentPane = split
        frame.isVisible = true
    }
}

private data class TransferText(val text: String) : Transferable {

    override fun getTransferData(flavor: DataFlavor?): Any {
        return text
    }

    override fun getTransferDataFlavors() = arrayOf(DataFlavor.stringFlavor)

    override fun isDataFlavorSupported(flavor: DataFlavor?) = flavor == DataFlavor.stringFlavor

}

private class DragHandler(val tabs: JTabbedPane): TransferHandler(),
    MouseListener {

    override fun createTransferable(c: JComponent?): Transferable? {
        return TransferText(tabs.getTitleAt(tabs.selectedIndex))
    }

    override fun getSourceActions(c: JComponent?) = COPY

    override fun mousePressed(e: MouseEvent?) {
        exportAsDrag(tabs, e, COPY)
    }

    override fun mouseReleased(e: MouseEvent?) = Unit

    override fun mouseClicked(e: MouseEvent?) = Unit

    override fun mouseEntered(e: MouseEvent?) = Unit

    override fun mouseExited(e: MouseEvent?) = Unit

}

private class DropListModel: TransferHandler(), ListModel<String> {

    private val list =  mutableListOf<String>()

    override fun getSize() = list.size

    override fun getElementAt(index: Int) = list[index]

    private val listeners = mutableListOf<ListDataListener>()
    private var event: ListDataEvent? = null

    override fun addListDataListener(l: ListDataListener?) {
        if (l != null) listeners.add(l)
    }

    override fun removeListDataListener(l: ListDataListener?) {
        if (l != null) listeners.remove(l)
    }

    override fun canImport(support: TransferSupport): Boolean {
        if (!support.isDrop) return false
        support.setShowDropLocation(true)
        return support.isDataFlavorSupported(DataFlavor.stringFlavor)
    }

    override fun importData(support: TransferSupport): Boolean {
        if (!canImport(support)) return false
        val n = list.size
        list.add(support.transferable.getTransferData(DataFlavor.stringFlavor) as String)
        var e = event
        for(i in (listeners.size - 1) downTo 0) {
            if (e == null) {
                e = ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, n, n)
                event = e
            }
            listeners[i].intervalAdded(e)
        }
        return true
    }

}