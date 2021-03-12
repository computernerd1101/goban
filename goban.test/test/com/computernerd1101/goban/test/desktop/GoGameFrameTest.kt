package com.computernerd1101.goban.test.desktop

import com.computernerd1101.goban.GoRules
import com.computernerd1101.goban.desktop.*
import com.computernerd1101.goban.players.*
import kotlinx.coroutines.*

fun main() {
    val setup = GoGameSetup(5)
    setup.isFreeHandicap = true
    val info = setup.gameInfo
    info.handicap = 3
    info.rules = GoRules.JAPANESE
    // info.timeLimit = 30L*60L*1000L // 30 minutes
    // info.overtime = ByoYomi()
    val frame = GoGameFrame(setup)
    frame.scope.launch {
        frame.isVisible = true
        frame.gameContext.startGame()
    }
}