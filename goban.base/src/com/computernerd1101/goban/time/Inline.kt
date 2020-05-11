@file:Suppress("unused", "NOTHING_TO_INLINE")

package com.computernerd1101.goban.time

inline fun Byte.toMilliseconds() = Milliseconds.valueOf(toLong())
inline fun Short.toMilliseconds() = Milliseconds.valueOf(toLong())
inline fun Int.toMilliseconds() = Milliseconds.valueOf(toLong())
inline fun Long.toMilliseconds() = Milliseconds.valueOf(this)
@ExperimentalUnsignedTypes
inline fun UByte.toMilliseconds() = Milliseconds.valueOf(toLong())
@ExperimentalUnsignedTypes
inline fun UShort.toMilliseconds() = Milliseconds.valueOf(toLong())
@ExperimentalUnsignedTypes
inline fun UInt.toMilliseconds() = Milliseconds.valueOf(toLong())
@ExperimentalUnsignedTypes
inline fun ULong.toMilliseconds() = Milliseconds.valueOf(toLong())

inline fun Long.millisToStringSeconds(): String = TimeLimit.millisToStringSeconds(this)

inline fun String.secondsToMillis(): Long = TimeLimit.parseSeconds(this)

