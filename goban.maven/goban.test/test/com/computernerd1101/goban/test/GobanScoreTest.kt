package com.computernerd1101.goban.test

import com.computernerd1101.goban.*
import com.computernerd1101.goban.desktop.GobanView
import com.computernerd1101.goban.sgf.GoSGF
import java.awt.GridLayout
import java.io.FileInputStream
import javax.swing.*

fun main() {
    val goban = Goban(9)
    goban[1, 0] = GoColor.WHITE
    goban[3, 0] = GoColor.BLACK
    goban[5, 0] = GoColor.WHITE
    goban[7, 0] = GoColor.WHITE
    goban[8, 0] = GoColor.BLACK
    goban[0, 1] = GoColor.WHITE
    goban[1, 1] = GoColor.WHITE
    goban[2, 1] = GoColor.WHITE
    goban[3, 1] = GoColor.BLACK
    goban[4, 1] = GoColor.WHITE
    goban[5, 1] = GoColor.WHITE
    goban[6, 1] = GoColor.WHITE
    goban[7, 1] = GoColor.WHITE
    goban[8, 1] = GoColor.BLACK
    goban[0, 2] = GoColor.BLACK
    goban[1, 2] = GoColor.BLACK
    goban[2, 2] = GoColor.BLACK
    goban[3, 2] = GoColor.WHITE
    goban[4, 2] = GoColor.WHITE
    goban[5, 2] = GoColor.BLACK
    goban[6, 2] = GoColor.BLACK
    goban[7, 2] = GoColor.BLACK
    goban[8, 2] = GoColor.BLACK
    goban[2, 3] = GoColor.BLACK
    goban[3, 3] = GoColor.BLACK
    goban[4, 3] = GoColor.BLACK
    goban[5, 3] = GoColor.BLACK
    goban[0, 4] = GoColor.BLACK
    goban[1, 4] = GoColor.BLACK
    goban[4, 4] = GoColor.BLACK
    goban[6, 4] = GoColor.BLACK
    goban[7, 4] = GoColor.BLACK
    goban[8, 4] = GoColor.BLACK
    goban[0, 5] = GoColor.WHITE
    goban[1, 5] = GoColor.WHITE
    goban[2, 5] = GoColor.BLACK
    goban[3, 5] = GoColor.BLACK
    goban[5, 5] = GoColor.BLACK
    goban[6, 5] = GoColor.WHITE
    goban[7, 5] = GoColor.WHITE
    goban[8, 5] = GoColor.WHITE
    goban[1, 6] = GoColor.WHITE
    goban[2, 6] = GoColor.WHITE
    goban[3, 6] = GoColor.BLACK
    goban[4, 6] = GoColor.BLACK
    goban[5, 6] = GoColor.WHITE
    goban[7, 6] = GoColor.WHITE
    goban[0, 7] = GoColor.WHITE
    goban[1, 7] = GoColor.BLACK
    goban[2, 7] = GoColor.WHITE
    goban[3, 7] = GoColor.WHITE
    goban[4, 7] = GoColor.BLACK
    goban[5, 7] = GoColor.WHITE
    goban[6, 7] = GoColor.WHITE
    goban[7, 7] = GoColor.BLACK
    goban[8, 7] = GoColor.BLACK
    goban[1, 8] = GoColor.BLACK
    goban[3, 8] = GoColor.WHITE
    goban[4, 8] = GoColor.BLACK
    goban[5, 8] = GoColor.WHITE
    goban[7, 8] = GoColor.WHITE
    val scoreGoban = goban.getScoreGoban(true)
    SwingUtilities.invokeLater {
        val gobanView = GobanView(goban)
        val territoryView = GobanView(scoreGoban)
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