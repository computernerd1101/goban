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
    val blackPlayer = game.blackPlayer as GoGameFrame.Player
    assert(!blackPlayer.isFrameInitialized)
    val whitePlayer = game.whitePlayer as GoGameFrame.Player
    assert(!whitePlayer.isFrameInitialized)
    val handler = Thread.UncaughtExceptionHandler { _, e ->
        System.err.println(e)
    }
    val job = Job()
    val blackFrame = GoGameFrame(game, job)
    val whiteFrame = GoGameFrame(game, job)
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
        val x = blackFrame.locationOnScreen.x
        blackFrame.extendedState = Frame.MAXIMIZED_VERT
        whiteFrame.extendedState = Frame.MAXIMIZED_VERT
        val toolkit: Toolkit = Toolkit.getDefaultToolkit()
        val screenSize: Dimension = toolkit.screenSize
        val width = (screenSize.width - x) / 2
        val height = screenSize.height
        blackFrame.setLocation(x, 0)
        whiteFrame.setLocation(x + width, 0)
        blackFrame.setSize(width, height)
        whiteFrame.setSize(width, height)
        game.startGame(job)
    }
    while(job.isActive) Unit
    blackFrame.dispose()
    whiteFrame.dispose()
}