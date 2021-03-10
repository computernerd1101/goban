package com.computernerd1101.goban.test.players

import com.computernerd1101.goban.*
import com.computernerd1101.goban.desktop.GoGameFrame
import com.computernerd1101.goban.desktop.SwingUtilitiesDispatcher
import com.computernerd1101.goban.players.*
import kotlinx.coroutines.*
import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

fun main() {
    val blackPlayer = GoGameFrame.Player(GoPlayer.Black)
    val whitePlayer = GoGameFrame.Player(GoPlayer.White)
    val setup = GoGameSetup(5, GoGameFrame, GoGameFrame)
    val game = GoGameContext(setup)
    val scope = CoroutineScope(blackPlayer + whitePlayer + game + SwingUtilitiesDispatcher)
    val blackFrame = GoGameFrame(scope)
    val whiteFrame = GoGameFrame(scope)
    blackPlayer.initFrame(blackFrame)
    whitePlayer.initFrame(whiteFrame)
    blackFrame.title = GoColor.BLACK.toString()
    whiteFrame.title = GoColor.WHITE.toString()
    val windowListener = object: WindowAdapter() {
        override fun windowClosed(e: WindowEvent?) {
            scope.cancel()
        }
    }
    blackFrame.addWindowListener(windowListener)
    whiteFrame.addWindowListener(windowListener)
    blackFrame.extendedState = Frame.MAXIMIZED_BOTH
    whiteFrame.extendedState = Frame.MAXIMIZED_BOTH
    whiteFrame.isVisible = true
    blackFrame.isVisible = true
    scope.launch {
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
        game.startGame()
    }
    while(scope.isActive) Unit
    blackFrame.dispose()
    whiteFrame.dispose()
}