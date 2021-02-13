package com.computernerd1101.goban.test.players

import com.computernerd1101.goban.*
import com.computernerd1101.goban.desktop.GoGameFrame
import com.computernerd1101.goban.players.*
import kotlinx.coroutines.Dispatchers
import java.awt.Dimension
import java.awt.Frame
import java.awt.Toolkit
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

class TestPlayer(manager: GoPlayerManager, color: GoColor): GoGameFrame.AbstractPlayer(manager, color) {

    companion object Factory: GoPlayer.Factory {

        override fun createPlayer(manager: GoPlayerManager, color: GoColor): GoPlayer =
            TestPlayer(manager, color)

    }

    override val frame = GoGameFrame(manager)

}

fun main() {
    val setup = GoGameSetup(TestPlayer, TestPlayer, 5)
    val manager = GoPlayerManager(Dispatchers.Default, setup)
    val blackPlayer = manager.blackPlayer as TestPlayer
    val whitePlayer = manager.whitePlayer as TestPlayer
    val blackFrame = blackPlayer.frame
    val whiteFrame = whitePlayer.frame
    blackFrame.title = GoColor.BLACK.toString()
    whiteFrame.title = GoColor.WHITE.toString()
    blackFrame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    whiteFrame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    blackFrame.extendedState = Frame.MAXIMIZED_BOTH
    whiteFrame.extendedState = Frame.MAXIMIZED_BOTH
    whiteFrame.isVisible = true
    blackFrame.isVisible = true
    manager.startGame()
    SwingUtilities.invokeLater {
        val y = blackFrame.locationOnScreen.y
        blackFrame.extendedState = Frame.MAXIMIZED_HORIZ
        whiteFrame.extendedState = Frame.MAXIMIZED_HORIZ
        val toolkit: Toolkit = Toolkit.getDefaultToolkit()
        val screenSize: Dimension = toolkit.screenSize
        val width = screenSize.width
        val height = (screenSize.height - y) / 2
        blackFrame.setLocation(0, y)
        whiteFrame.setLocation(0, y + height)
        blackFrame.setSize(width, height)
        whiteFrame.setSize(width, height)
    }
}