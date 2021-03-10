package com.computernerd1101.goban.test.sandbox

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun main() {

    val suspendFun: suspend Boolean.() -> String = Boolean::suspendIfTrue
    println("Immediate result: " + suspendFun.startCoroutineUninterceptedOrReturn(false, MyCompletion))
    println(suspendIfTrueContinuation)
    println("Immediate result: " + suspendFun.startCoroutineUninterceptedOrReturn(true, MyCompletion))
    println(suspendIfTrueContinuation)
    suspendIfTrueContinuation?.resumeWith(Result.success("COROUTINE_RESUMED"))
    println(suspendIfTrueContinuation)
}

object MyCompletion: Continuation<String> {
    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    override fun resumeWith(result: Result<String>) {
        println("Delayed result: " + result.getOrThrow())
    }
}

suspend fun Boolean.suspendIfTrue(): String {
    if (this) {
        val message = suspendCoroutine<String> { cont ->
            suspendIfTrueContinuation = cont
        }
        suspendIfTrueContinuation = null
        return message
    }
    return "Not suspended"
}

private var suspendIfTrueContinuation: Continuation<String>? = null
