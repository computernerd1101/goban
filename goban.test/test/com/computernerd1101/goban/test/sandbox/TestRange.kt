package com.computernerd1101.goban.test.sandbox

fun main() {
    testRange(5, true)
    testRange(5, false)
}

fun testRange(n: Int, forward: Boolean) {
    for(i in if (forward) 0 until n else (n - 1) downTo 0)
        println(i)
}