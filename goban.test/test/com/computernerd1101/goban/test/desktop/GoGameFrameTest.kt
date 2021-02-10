package com.computernerd1101.goban.test.desktop

import com.computernerd1101.goban.desktop.*
import com.computernerd1101.goban.players.*
import com.computernerd1101.goban.time.ByoYomi
import kotlinx.coroutines.Dispatchers
import javax.swing.SwingUtilities

fun main() {
    val clientFactory = ClientPlayer.Factory()
    val setup = GoGameSetup(clientFactory, clientFactory)
    val info = setup.gameInfo
    info.timeLimit = 30L*60L*1000L // 30 minutes
    info.overtime = ByoYomi()
    val manager = GoPlayerManager(Dispatchers.Default, setup)
    SwingUtilities.invokeLater {
        val frame = GoGameFrame(manager)
        clientFactory.frame = frame
        frame.isVisible = true
        manager.startGame()
    }
}