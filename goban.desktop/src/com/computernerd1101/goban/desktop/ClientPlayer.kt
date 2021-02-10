package com.computernerd1101.goban.desktop

import com.computernerd1101.goban.GoColor
import com.computernerd1101.goban.GoPoint
import com.computernerd1101.goban.GoPointSet
import com.computernerd1101.goban.players.GoPlayer
import com.computernerd1101.goban.players.GoPlayerManager
import kotlinx.coroutines.channels.SendChannel

class ClientPlayer(
    private val factory: Factory,
    manager: GoPlayerManager,
    color: GoColor
): GoGameFrame.Player(manager, color) {

    class Factory: GoPlayer.Factory {

        override fun createPlayer(manager: GoPlayerManager, color: GoColor): GoPlayer {
            return ClientPlayer(this, manager, color)
        }

        lateinit var frame: GoGameFrame

    }

    override val frame: GoGameFrame by lazy { factory.frame }

}