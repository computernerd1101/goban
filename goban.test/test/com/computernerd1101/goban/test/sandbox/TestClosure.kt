package com.computernerd1101.goban.test.sandbox

fun main() {
    lateinit var s: String
    val e = " = "
    var i = 0
    val f: () -> Unit = {
        println(s + e + i)
    }
    val strings = arrayOf("three", "ultimate answer")
    val ints = intArrayOf(3, 42)
    for(index in 0..1) {
        s = strings[index]
        i = ints[index]
        f()
    }
}