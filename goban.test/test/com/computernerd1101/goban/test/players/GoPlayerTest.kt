package com.computernerd1101.goban.test.players

import com.computernerd1101.goban.*
import com.computernerd1101.goban.desktop.GoGameFrame
import com.computernerd1101.goban.players.*
import com.computernerd1101.goban.time.ByoYomi
import kotlinx.coroutines.*
import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

fun main() {
    val setup = GoGameSetup(5)
    setup.gameInfo.rules = GoRules.JAPANESE
    setup.gameInfo.handicap = 24
    setup.isFreeHandicap = true
    setup.gameInfo.timeLimit = 30000L
    setup.gameInfo.overtime = ByoYomi(periods = 3, millis = 20000L)
    val game = GoGameManager(setup, GoGameFrame)
    val job = game.job
    val blackPlayer = game.blackPlayer as GoGameFrame.Player
    assert(!blackPlayer.isFrameInitialized)
    val whitePlayer = game.whitePlayer as GoGameFrame.Player
    assert(!whitePlayer.isFrameInitialized)
    val handler = Thread.UncaughtExceptionHandler { _, e ->
        println(e)
    }
    val blackFrame = GoGameFrame(game)
    val whiteFrame = GoGameFrame(game)
    blackPlayer.initFrame(blackFrame)
    assert(blackPlayer.isFrameInitialized)
    whitePlayer.initFrame(whiteFrame)
    assert(whitePlayer.isFrameInitialized)
    blackFrame.title = GoColor.BLACK.toString()
    whiteFrame.title = GoColor.WHITE.toString()
    val windowListener = object: WindowAdapter() {
        override fun windowClosed(e: WindowEvent?) {
            job.cancel()
        }
    }
    blackFrame.addWindowListener(windowListener)
    whiteFrame.addWindowListener(windowListener)
    blackFrame.extendedState = Frame.MAXIMIZED_BOTH
    whiteFrame.extendedState = Frame.MAXIMIZED_BOTH
    whiteFrame.isVisible = true
    blackFrame.isVisible = true
    blackFrame.scope.launch {
        Thread.currentThread().uncaughtExceptionHandler = handler
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
    while(job.isActive) Unit
    blackFrame.dispose()
    whiteFrame.dispose()
}