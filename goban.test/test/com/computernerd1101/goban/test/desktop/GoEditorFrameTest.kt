package com.computernerd1101.goban.test.desktop

import com.computernerd1101.goban.desktop.GoEditorFrame
import com.computernerd1101.goban.sgf.GoSGF
import javax.swing.SwingUtilities

fun main() {
    SwingUtilities.invokeLater {
        GoEditorFrame(GoSGF()).isVisible = true
    }
}