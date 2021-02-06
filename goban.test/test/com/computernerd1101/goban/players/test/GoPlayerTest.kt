package com.computernerd1101.goban.players.test

import com.computernerd1101.goban.GoColor
import com.computernerd1101.goban.GoPointSet
import com.computernerd1101.goban.desktop.GoGameFrame
import com.computernerd1101.goban.players.GoGameSetup
import com.computernerd1101.goban.players.GoPlayer
import com.computernerd1101.goban.players.GoPlayerManager
import kotlinx.coroutines.Dispatchers
import java.awt.Frame
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import kotlin.coroutines.coroutineContext


class TestPlayer(manager: GoPlayerManager, color: GoColor): GoPlayer(manager, color) {

    companion object Factory: GoPlayer.Factory {

        override fun createPlayer(manager: GoPlayerManager, color: GoColor): GoPlayer =
            TestPlayer(manager, color)

    }

    val frame = GoGameFrame(manager)

    override suspend fun generateHandicapStones(handicap: Int): GoPointSet {
        TODO("Not yet implemented")
    }

}

fun main() {
    val setup = GoGameSetup(TestPlayer, TestPlayer)
    SwingUtilities.invokeLater {
        val manager = GoPlayerManager(Dispatchers.Default, setup)
        val blackPlayer = manager.blackPlayer as TestPlayer
        val whitePlayer = manager.whitePlayer as TestPlayer
        val blackFrame = blackPlayer.frame
        val whiteFrame = whitePlayer.frame
        blackFrame.title = GoColor.BLACK.toString()
        whiteFrame.title = GoColor.WHITE.toString()
        blackFrame.extendedState = Frame.NORMAL
        whiteFrame.extendedState = Frame.NORMAL
        blackFrame.setSize(500, 500)
        whiteFrame.setSize(500, 500)
        blackFrame.setLocationRelativeTo(null)
        val locationX = blackFrame.x
        val locationY = blackFrame.y
        blackFrame.setLocation(locationX - 260, locationY)
        whiteFrame.setLocation(locationX + 260, locationY)
        blackFrame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        whiteFrame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        blackFrame.isVisible = true
        whiteFrame.isVisible = true
    }
}