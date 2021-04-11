package com.computernerd1101.goban.test.sandbox

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun main() {
    val suspendFun: suspend Boolean.() -> String = Boolean::suspendIfTrue
    suspendFun.startCoroutine(false, MyCompletion)
    println(suspendIfTrueContinuation)
    suspendFun.startCoroutine(true, MyCompletion)
    println(suspendIfTrueContinuation)
    suspendIfTrueContinuation?.resume("COROUTINE_RESUMED")
    println(suspendIfTrueContinuation)
    println("Immediate result: " + suspendFun.startCoroutineUninterceptedOrReturn(false, MyCompletion))
    println(suspendIfTrueContinuation)
    println("Immediate result: " + suspendFun.startCoroutineUninterceptedOrReturn(true, MyCompletion))
    println(suspendIfTrueContinuation)
    suspendIfTrueContinuation?.resume("COROUTINE_RESUMED")
    println(suspendIfTrueContinuation)
}

suspend fun foobar(foo: Int, bar: Int): Int = foo + bar

fun foobarUnintercepted(foo: Int, bar: Int, continuation: Continuation<Int>): Any? {
    return suspend { foobar(foo, bar) }.startCoroutineUninterceptedOrReturn(continuation)
}

object MyCompletion: Continuation<String> {
    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    override fun resumeWith(result: Result<String>) {
        println("Delayed result: " + result.getOrThrow())
    }
}

suspend fun Boolean.suspendIfTrue(): String {
    val message: String
    try {
        if (this) {
            message = suspendCoroutine { cont ->
                suspendIfTrueContinuation = cont
            }
            suspendIfTrueContinuation = null
        } else message = "Not suspended"
    } finally {
        println("Suspendable finally block")
    }
    return message
}

private var suspendIfTrueContinuation: Continuation<String>? = null
