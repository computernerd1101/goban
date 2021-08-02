@file:Suppress("unused")

package com.computernerd1101.goban.test.sandbox

import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

tailrec fun factorial(n: Int, scale: Int = 1): Int =
    if (n == 0)
        scale
    else
        factorial(n - 1, n * scale)

tailrec fun Int.extensionFactorial(scale: Int = 1): Int =
    if (this == 0)
        scale
    else
        (this - 1).extensionFactorial(this * scale)

tailrec suspend fun suspendTailRec(n: Int, sum: Int = 0): Int {
    if (n == 0) return sum
    val input = suspendCoroutine<Int> { cont ->
        resumeTailRec = cont
    }
    return suspendTailRec(n - 1, sum + input)
}

var resumeTailRec: Continuation<Int>? = null
