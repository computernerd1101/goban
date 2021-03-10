package com.computernerd1101.goban.test.players

import com.computernerd1101.goban.desktop.*
import com.computernerd1101.goban.players.*
import kotlinx.coroutines.launch

fun main() {
    val setup = GoGameSetup(9, blackPlayer = BakaBot)
    val frame = GoGameFrame(setup)
    frame.title = "BakaBot"
    frame.scope.launch {
        frame.isVisible = true
        frame.gameContext.startGame()
    }
}