package com.computernerd1101.goban.desktop

import com.computernerd1101.goban.players.GoGameSetup
import com.computernerd1101.goban.players.GoPlayerManager
import com.computernerd1101.goban.time.ByoYomi
import kotlinx.coroutines.Dispatchers
import java.awt.Frame
import javax.swing.*

fun main() {
    val setup = GoGameSetup(ClientPlayer, ClientPlayer)
    val info = setup.gameInfo
    info.timeLimit = 30L*60L*1000L // 30 minutes
    info.overtime = ByoYomi()
    val manager = GoPlayerManager(Dispatchers.Default, setup)
    SwingUtilities.invokeLater {
        GoGameFrame(manager).isVisible = true
    }
}

class GoGameFrame(@Suppress("UNUSED_PARAMETER") manager: GoPlayerManager): JFrame() {

    init {
        title = "CN13 Goban"
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        setSize(1000, 500)
        extendedState = Frame.MAXIMIZED_BOTH
        setLocationRelativeTo(null)
    }

}