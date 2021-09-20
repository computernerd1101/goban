package com.computernerd1101.goban.desktop

import com.computernerd1101.goban.GoColor
import com.computernerd1101.goban.sgf.GoSGFNode
import com.computernerd1101.goban.sgf.GameInfo as SGFGameInfo
import java.awt.event.*
import java.text.ParseException
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater
import javax.swing.*
import javax.swing.text.JTextComponent
import kotlin.reflect.KMutableProperty1

internal abstract class TextListener<T>(
    private val component: JComponent,
    private val node: () -> GoSGFNode
): KeyListener, Runnable {

    @Volatile
    private var modified: Int = 0

    companion object {

        /** Updates [TextListener.modified] */
        private val MODIFIED: AtomicIntegerFieldUpdater<TextListener<*>> =
            AtomicIntegerFieldUpdater.newUpdater(TextListener::class.java, "modified")

    }

    override fun keyTyped(e: KeyEvent?) = invokeLater()
    override fun keyPressed(e: KeyEvent?) = invokeLater()
    override fun keyReleased(e: KeyEvent?) = invokeLater()

    fun invokeLater() {
        if (MODIFIED.compareAndSet(this, 0, 1))
            SwingUtilities.invokeLater(this)
    }

    @Suppress("UNCHECKED_CAST")
    final override fun run() {
        if (MODIFIED.compareAndSet(this, 1, 0)) {
            val value: Any? = when (component) {
                is JFormattedTextField -> {
                    try {
                        component.commitEdit()
                    } catch(e: ParseException) {
                        return
                    }
                    component.value
                }
                is JTextComponent -> component.text
                is JComboBox<*> -> component.selectedItem
                else -> return
            }
            update(node(), value as T)
        }
    }

    protected abstract fun update(node: GoSGFNode, value: T)

    class Node<T>(
        component: JTextComponent,
        private val prop: KMutableProperty1<GoSGFNode, T>,
        node: () -> GoSGFNode
    ): TextListener<T>(component, node) {

        override fun update(node: GoSGFNode, value: T) {
            prop.set(node, value)
        }

    }

    /*class Move<T>(
        component: JComponent,
        node: () -> GoSGFNode,
        private val prop: KMutableProperty1<GoSGFMoveNode, T>
    ): TextListener<T>(component, node) {

        override fun update(node: GoSGFNode, value: T) {
            if (node is GoSGFMoveNode) prop.set(node, value)
        }

    }*/

    class GameInfo<T>(
        component: JComponent,
        private val prop: KMutableProperty1<SGFGameInfo, T>,
        node: () -> GoSGFNode
    ): TextListener<T>(component, node) {

        override fun update(node: GoSGFNode, value: T) {
            node.gameInfo?.let { info -> prop.set(info, value) }
        }

    }

    class GameInfoPlayer(
        component: JComponent,
        private val color: GoColor,
        private val prop: KMutableProperty1<SGFGameInfo.Player, String>,
        node: () -> GoSGFNode
    ): TextListener<String>(component, node) {

        override fun update(node: GoSGFNode, value: String) {
            node.gameInfo?.let { info -> prop.set(info.player[color], value) }
        }

    }


}