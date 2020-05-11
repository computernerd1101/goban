package com.computernerd1101.goban.desktop

import com.computernerd1101.goban.time.ByoYomi
import java.awt.Frame
import javax.swing.*

fun main() {
    val setup = GoGameSetup()
    val info = setup.gameInfo
    info.timeLimit = 30L*60L*1000L // 30 minutes
    info.overtime = ByoYomi()
    SwingUtilities.invokeLater {
        GoGameFrame(setup).isVisible = true
    }
}

class GoGameFrame(setup: GoGameSetup = GoGameSetup()): JFrame() {

    init {
        title = "CN13 Goban"
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        setSize(1000, 500)
        extendedState = Frame.MAXIMIZED_BOTH
        setLocationRelativeTo(null)
    }

}