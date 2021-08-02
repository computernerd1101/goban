package com.computernerd1101.goban.test.sandbox

import kotlinx.coroutines.delay

suspend fun main(args: Array<String>) {
    for(arg in args) {
        println(arg)
        delay(1000L)
    }
}