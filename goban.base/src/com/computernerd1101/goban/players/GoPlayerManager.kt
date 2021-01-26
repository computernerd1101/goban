package com.computernerd1101.goban.players

import kotlinx.coroutines.*

class GoPlayerManager {

    private val gameJob = Job()
    private val blackPlayerJob = Job(gameJob)
    private val whitePlayerJob = Job(gameJob)

    private val gameScope = CoroutineScope(Dispatchers.Main + gameJob)
    private val blackPlayerScope = CoroutineScope(Dispatchers.IO + blackPlayerJob)
    private val whitePlayerScope = CoroutineScope(Dispatchers.IO + whitePlayerJob)

    fun startGame() {
        gameScope.launch {

        }
    }

}