@file:Suppress("NOTHING_TO_INLINE")
@file:JvmMultifileClass
@file:JvmName("GobanKt")

package com.computernerd1101.goban

fun Boolean.goBlackOrWhite() = if (this) GoColor.BLACK else GoColor.WHITE

@Suppress("unused")
inline fun String.toGoPoint() = toGoPointOrNull() ?: throw IllegalArgumentException(this)
inline fun String.toGoPointOrNull() = GoPoint.parse(this)
inline fun Int.toGoPointChar() = GoPoint.toChar(this)
@Suppress("unused")
inline fun Char.toGoPointInt() = GoPoint.parseChar(this)
