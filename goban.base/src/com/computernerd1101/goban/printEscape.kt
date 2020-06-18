package com.computernerd1101.goban

import java.util.*

fun main() {
    println("\u30ab\u30ca\u30c0")
}

fun printEscape(s: String) {
    print('"')
    for(ch in s) {
        print("\\u")
        val hex = ch.toInt().toString(16)
        for(i in hex.length until 4)
            print('0')
        print(hex)
    }
    println('"')
}