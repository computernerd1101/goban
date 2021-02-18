package com.computernerd1101.goban.test.sandbox

fun main() {
    printEscape("パス")
    println("\u30D1\u30B9")
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