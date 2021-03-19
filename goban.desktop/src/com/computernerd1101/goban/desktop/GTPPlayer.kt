package com.computernerd1101.goban.desktop

import com.computernerd1101.goban.*
import com.computernerd1101.goban.players.ExperimentalGoPlayerApi
import com.computernerd1101.goban.players.GoGameSetup
import com.computernerd1101.goban.players.GoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@ExperimentalGoPlayerApi
class GTPPlayer(private val process: Process, color: GoColor): GoPlayer(color) {

    class Factory(vararg cmd: String): GoPlayer.Factory {

        private val process: Process = ProcessBuilder(*cmd).start()

        override fun createPlayer(color: GoColor) = GTPPlayer(process, color)

        override suspend fun isCompatible(setup: GoGameSetup): Boolean = withContext(Dispatchers.IO) {
            TODO("Not yet implemented")
        }

    }

    override suspend fun generateHandicapStones(handicap: Int, goban: Goban) {
        TODO("Not yet implemented")
    }

    override suspend fun generateMove(): GoPoint? {
        TODO("Not yet implemented")
    }

}