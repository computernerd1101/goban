package com.computernerd1101.goban.test.sandbox

import kotlinx.coroutines.*
import kotlin.coroutines.*

suspend fun main() {
    val job = Job()
    var continuation: Continuation<Unit>? = null
    var n = 0
    CoroutineScope(job + Dispatchers.Unconfined).launch {
        while(job.isActive) {
            println(n)
            suspendCoroutine<Unit> { continuation = it }
        }
        println("Job over")
        println("isCompleted = ${job.isCompleted}")
        println("isCanceled = ${job.isCancelled}")
    }
    n++
    continuation?.resume(Unit)
    n++
    continuation?.resume(Unit)
    job.cancel()
    continuation?.resume(Unit)
}