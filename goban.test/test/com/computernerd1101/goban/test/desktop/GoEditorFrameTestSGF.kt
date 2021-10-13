package com.computernerd1101.goban.test.desktop

import com.computernerd1101.goban.desktop.GoEditorFrame
import com.computernerd1101.goban.test.readSGFResource
import javax.swing.SwingUtilities

fun main() {
    val sgf = readSGFResource("test.sgf")
    SwingUtilities.invokeLater {
        GoEditorFrame(sgf).isVisible = true
    }
}