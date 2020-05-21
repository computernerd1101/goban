package com.computernerd1101.goban.desktop.internal

import java.util.*

inline fun <reified K: Enum<K>, V> enumMap() = EnumMap<K, V>(K::class.java)