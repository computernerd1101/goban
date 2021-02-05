package com.computernerd1101.goban.players.test

import com.computernerd1101.goban.GoColor
import com.computernerd1101.goban.players.GoPlayer
import com.computernerd1101.goban.players.GoPlayerManager

class GoPlayerTest {
}

object TestPlayerFactory: GoPlayer.Factory {

    override fun createPlayer(manager: GoPlayerManager, color: GoColor): GoPlayer {
        return if (color == GoColor.BLACK) TestBlackPlayer(manager)
        else TestWhitePlayer(manager)
    }

}

class TestBlackPlayer(manager: GoPlayerManager): GoPlayer(manager, GoColor.BLACK)

class TestWhitePlayer(manager: GoPlayerManager): GoPlayer(manager, GoColor.WHITE)