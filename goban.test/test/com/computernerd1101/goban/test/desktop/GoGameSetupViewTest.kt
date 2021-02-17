package com.computernerd1101.goban.test.desktop

import com.computernerd1101.goban.desktop.*
import com.computernerd1101.goban.players.GoPlayerManager
import javax.swing.SwingUtilities

fun main() {
    SwingUtilities.invokeLater {
        val setup = GoGameSetupView().showDialog(null)
        if (setup != null)
            GoGameFrame(GoPlayerManager(setup)).isVisible = true
    }
}