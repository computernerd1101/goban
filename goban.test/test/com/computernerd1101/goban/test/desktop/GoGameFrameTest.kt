package com.computernerd1101.goban.test.desktop

import com.computernerd1101.goban.GoRules
import com.computernerd1101.goban.desktop.*
import com.computernerd1101.goban.players.*
import com.computernerd1101.goban.time.ByoYomi
import kotlinx.coroutines.*

fun main() {
    val setup = GoGameSetup(5)
    // setup.isFreeHandicap = true
    val info = setup.gameInfo
    // info.handicap = 3
    info.rules = GoRules.JAPANESE
    info.timeLimit = 30000L // 30 seconds
    info.overtime = ByoYomi(periods = 3, millis = 10000L)
    val frame = GoGameFrame(setup)
    frame.scope.launch {
        frame.isVisible = true
        frame.gameContext.startGame()
    }
}