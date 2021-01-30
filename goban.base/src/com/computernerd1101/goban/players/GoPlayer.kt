package com.computernerd1101.goban.players

import com.computernerd1101.goban.GoColor

abstract class GoPlayer(val manager: GoPlayerManager, val color: GoColor) {

    fun interface Factory {

        fun createPlayer(manager: GoPlayerManager, color: GoColor): GoPlayer

    }

}