package com.computernerd1101.goban.test.sandbox

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun main() {
    suspendIfTrue.startCoroutine(false, MyCompletion)
    println(suspendIfTrueContinuation)
    suspendIfTrue.startCoroutine(true, MyCompletion)
    println(suspendIfTrueContinuation)
    suspendIfTrueContinuation?.resume("COROUTINE_RESUMED")
    println(suspendIfTrueContinuation)
    println("Immediate result: " + suspendIfTrue.startCoroutineUninterceptedOrReturn(false, MyCompletion))
    println(suspendIfTrueContinuation)
    println("Immediate result: " + suspendIfTrue.startCoroutineUninterceptedOrReturn(true, MyCompletion))
    println(suspendIfTrueContinuation)
    suspendIfTrueContinuation?.resume("COROUTINE_RESUMED")
    println(suspendIfTrueContinuation)
}

suspend fun suspendFoobar(foo: Int, bar: Int): Int {
    println(true.suspendIfTrue())
    return foo + bar
}

object MyCompletion: Continuation<String> {
    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    override fun resumeWith(result: Result<String>) {
        println("Delayed result: " + result.getOrThrow())
    }
}

val suspendIfTrue: suspend Boolean.() -> String = {
    try {
        if (this) {
            val message: String = suspendCoroutine { cont ->
                suspendIfTrueContinuation = cont
            }
            suspendIfTrueContinuation = null
            message
        } else "Not suspended"
    } finally {
        println("Suspendable finally block")
    }
}

private var suspendIfTrueContinuation: Continuation<String>? = null

fun resumeSuspendIfTrue(value: String) {
    suspendIfTrueContinuation?.resume(value)
}

suspend fun suspendNoInline() {
    true.suspendIfTrue()
    suspendInline()
}

suspend inline fun suspendInline() {
    val ok: Boolean = suspendFoobar(1, 2) == 3
    ok.suspendIfTrue()
}

suspend inline fun suspendCrossInline(crossinline block: suspend () -> Unit) {
    block()
}
