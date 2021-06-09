@file:Suppress("SpellCheckingInspection")

package com.computernerd1101.goban.desktop

import com.computernerd1101.goban.*
import com.computernerd1101.goban.players.ExperimentalGoPlayerApi
import com.computernerd1101.goban.players.GoGameManager
import com.computernerd1101.goban.players.GoGameSetup
import com.computernerd1101.goban.players.GoPlayer
import java.io.PrintStream
import java.util.*

@ExperimentalGoPlayerApi
class ProcessGTPPlayer(private val process: Process, game: GoGameManager, color: GoColor): GoPlayer(game, color) {

    class Factory(private vararg val cmd: String): GoPlayer.Factory {

        override fun createPlayer(game: GoGameManager, color: GoColor) =
            ProcessGTPPlayer(ProcessBuilder(*cmd).start(), game, color)

        override fun isCompatible(setup: GoGameSetup): Boolean {
            val process: Process = ProcessBuilder(*cmd).start()
            return try {
                val width = setup.width
                val height = setup.height
                val buffer = StringBuilder().append("boardsize ").append(width)
                if (width != height) buffer.append(' ').append(height)
                var output = process.callGTP(buffer.toString())
                if (!output.startsWith('=')) false
                else {
                    output = process.callGTP("query_boardsize")
                    if (!output.startsWith('=')) false
                    else {
                        var startWidth = 0
                        var endWidth = 0
                        var startHeight = 0
                        var endHeight = 0
                        for (i in 1 until output.length) {
                            val ch = output[i]
                            when {
                                startWidth == 0 -> if (!ch.isWhitespace()) startWidth = i
                                endWidth == 0 -> if (ch.isWhitespace()) endWidth = i
                                startHeight == 0 -> if (!ch.isWhitespace()) startHeight = i
                                endHeight == 0 -> if (ch.isWhitespace()) {
                                    endHeight = i
                                    break
                                }
                            }
                        }
                        if (startWidth != 0 && endWidth == 0) endWidth = output.length
                        else if (startHeight != 0 && endHeight == 0) endHeight = output.length
                        output = output.substring(1).trim()
                        val realWidth = output.substring(startWidth, endWidth).toInt()
                        val realHeight: Int = if (startHeight == 0) realWidth else
                            output.substring(startHeight, endHeight).toInt()
                        width == realWidth && height == realHeight
                    }
                }
            } catch(e: Exception) {
                false
            } finally {
                process.destroy()
            }
        }

    }

    override suspend fun generateHandicapStones(handicap: Int, goban: Goban) {
        TODO("Not yet implemented")
    }

    override suspend fun generateMove(): GoPoint? {
        TODO("Not yet implemented")
    }

}

private fun Process.callGTP(input: String): String {
    val printStream = PrintStream(outputStream)
    val inputLine = if (input.contains('\n')) input.replace('\n', ' ') else input
    printStream.println(inputLine)
    val scanner = Scanner(inputStream)
    val buffer = StringBuilder()
    while(true) {
        val line: String = try {
            scanner.nextLine()
        } catch(e: Exception) {
            break
        }
        if (line.isEmpty()) break
        if (buffer.isNotEmpty()) buffer.append('\n')
        buffer.append(line)
    }
    return buffer.toString()
}