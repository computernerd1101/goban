package com.computernerd1101.goban.desktop

import com.computernerd1101.goban.GoColor
import com.computernerd1101.goban.players.GoPlayer
import com.computernerd1101.goban.players.GoPlayerManager

class ClientPlayer(manager: GoPlayerManager, color: GoColor): GoPlayer(manager, color) {

    companion object Factory: GoPlayer.Factory {

        override fun createPlayer(manager: GoPlayerManager, color: GoColor): GoPlayer {
            return ClientPlayer(manager, color)
        }

    }

}