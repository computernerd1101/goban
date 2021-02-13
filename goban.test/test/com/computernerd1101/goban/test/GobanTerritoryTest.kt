package com.computernerd1101.goban.test

import com.computernerd1101.goban.*
import com.computernerd1101.goban.desktop.GobanView
import java.awt.GridLayout
import javax.swing.*

fun main() {
    val goban = Goban(5)
    goban[1, 0] = GoColor.WHITE
    goban[2, 0] = GoColor.BLACK
    goban[0, 1] = GoColor.WHITE
    goban[1, 1] = GoColor.WHITE
    goban[2, 1] = GoColor.BLACK
    goban[4, 1] = GoColor.BLACK
    goban[1, 2] = GoColor.WHITE
    goban[2, 2] = GoColor.BLACK
    goban[3, 2] = GoColor.BLACK
    goban[0, 3] = GoColor.WHITE
    goban[1, 3] = GoColor.WHITE
    goban[2, 3] = GoColor.WHITE
    goban[3, 3] = GoColor.BLACK
    goban[4, 3] = GoColor.WHITE // dead
    goban[1, 4] = GoColor.BLACK // dead
    goban[2, 4] = GoColor.WHITE
    goban[3, 4] = GoColor.BLACK
    goban[4, 4] = GoColor.BLACK
    SwingUtilities.invokeLater {
        val gobanView = GobanView(goban)
        val territoryView = GobanView(goban.getTerritory(false))
        val panel = JPanel(GridLayout(1, 2))
        panel.add(gobanView)
        panel.add(territoryView)
        val frame = JFrame()
        frame.contentPane = panel
        frame.title = "CN13 Goban"
        frame.setSize(1000, 500)
        frame.setLocationRelativeTo(null)
        frame.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        frame.isVisible = true
    }
}