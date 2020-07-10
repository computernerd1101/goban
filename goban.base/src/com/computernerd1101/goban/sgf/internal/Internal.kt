package com.computernerd1101.goban.sgf.internal

import java.io.ObjectInputStream

fun ObjectInputStream.GetField.getString(name: String): String {
    return try {
        this[name, null]?.toString()
    } catch(e: Exception) {
        null
    } ?: ""
}