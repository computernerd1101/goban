package com.computernerd1101.goban.test.sandbox

import com.computernerd1101.goban.desktop.SwingUtilitiesDispatcher
import kotlinx.coroutines.Dispatchers
import java.util.*


fun main() {
    println(SwingUtilitiesDispatcher)
}

interface MyServiceLoader

object MyServiceLoaderObject: MyServiceLoader