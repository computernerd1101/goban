package com.computernerd1101.goban.test.players

import com.computernerd1101.goban.desktop.*
import com.computernerd1101.goban.players.*
import javax.swing.SwingUtilities

fun main() {
    val clientFactory = GoGameFrame.PlayerFactory()
    val setup = GoGameSetup(BakaBot, clientFactory, 9)
    val manager = GoPlayerManager(setup)
    SwingUtilities.invokeLater {
        val frame = GoGameFrame(manager)
        frame.title = "BakaBot"
        clientFactory.frame = frame
        frame.isVisible = true
        manager.startGame()
    }
}