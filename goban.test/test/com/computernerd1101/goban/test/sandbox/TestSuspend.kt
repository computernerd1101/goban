package com.computernerd1101.goban.test.sandbox

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun main() {
}

suspend fun doSomethingUseful1() {
    val foo = doSomethingUseful2()
    doSomethingUseful3(foo)
    suspendCoroutineUninterceptedOrReturn<Unit> {
        COROUTINE_SUSPENDED
    }
}

suspend fun doSomethingUseful2(): Int = suspendCoroutineUninterceptedOrReturn {
    COROUTINE_SUSPENDED
}

suspend fun doSomethingUseful3(foo: Int) {
    suspendCoroutineUninterceptedOrReturn<Unit> { uCont ->
        if (uCont.hashCode() == foo) {
            COROUTINE_SUSPENDED
        } else Unit
    }
    println("Something useful?")
}