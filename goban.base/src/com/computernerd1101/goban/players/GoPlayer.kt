package com.computernerd1101.goban.players

import com.computernerd1101.goban.GoColor
import kotlinx.coroutines.*

abstract class GoPlayer(val manager: GoPlayerManager, val color: GoColor) {

    fun interface Factory {

        fun createPlayer(manager: GoPlayerManager, color: GoColor): GoPlayer

    }

    @Suppress("unused")
    val job: CompletableJob
        get() = if (color == GoColor.BLACK) manager.blackPlayerJob else manager.whitePlayerJob

    @Suppress("unused")
    val scope: CoroutineScope
        get() = if (color == GoColor.BLACK) manager.blackPlayerScope else manager.whitePlayerScope

}