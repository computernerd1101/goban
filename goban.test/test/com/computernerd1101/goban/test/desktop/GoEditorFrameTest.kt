package com.computernerd1101.goban.test.desktop

import com.computernerd1101.goban.desktop.GoEditorFrame
import com.computernerd1101.goban.sgf.GoSGF
import com.computernerd1101.sgf.SGFException
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import javax.swing.SwingUtilities
import kotlin.reflect.jvm.javaMethod

fun main() {
    val sgf = InputStream::readSGF.javaMethod!!.declaringClass.getResourceAsStream("test.sgf")!!.readSGF()
    SwingUtilities.invokeLater {
        GoEditorFrame(sgf).isVisible = true
    }
}

fun InputStream.readSGF(): GoSGF {
    return try {
        use { input ->
            GoSGF(input)
        }
    } catch(e: IOException) {
        e.printStackTrace()
        null
    } catch(e: SGFException) {
        e.printStackTrace()
        null
    } ?: GoSGF()
}