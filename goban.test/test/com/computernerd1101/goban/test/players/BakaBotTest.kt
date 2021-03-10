package com.computernerd1101.goban.test.players

import com.computernerd1101.goban.GoColor
import com.computernerd1101.goban.desktop.*
import com.computernerd1101.goban.players.*
import kotlinx.coroutines.launch

fun main() {
    val setup = GoGameSetup(9, BakaBot, GoGameFrame)
    val game = GoGameContext(setup)
    val frame = GoGameFrame(game + BakaBot(GoColor.BLACK))
    frame.title = "BakaBot"
    frame.scope.launch {
        frame.isVisible = true
        game.startGame()
    }
}