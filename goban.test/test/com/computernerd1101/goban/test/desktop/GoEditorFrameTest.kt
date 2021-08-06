package com.computernerd1101.goban.test.desktop

import com.computernerd1101.goban.desktop.GoEditorFrame
import com.computernerd1101.goban.sgf.GoSGF
import com.computernerd1101.sgf.SGFException
import java.io.FileInputStream
import java.io.IOException
import javax.swing.SwingUtilities

fun main(args: Array<String>) {
    val sgf = (if (args.isNotEmpty()) readSGF(args[0]) else null) ?: GoSGF()
    SwingUtilities.invokeLater {
        GoEditorFrame(sgf).isVisible = true
    }
}

fun readSGF(file: String): GoSGF? {
    return try {
        FileInputStream(file).use { input ->
            GoSGF(input)
        }
    } catch(e: IOException) {
        e.printStackTrace()
        null
    } catch(e: SGFException) {
        e.printStackTrace()
        null
    }
}