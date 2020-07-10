@file:Suppress("FunctionName", "NOTHING_TO_INLINE")
@file:JvmMultifileClass
@file:JvmName("GobanKt")

package com.computernerd1101.goban

fun Boolean.goBlackOrWhite() = if (this) GoColor.BLACK else GoColor.WHITE

inline fun GoPoint(x: Int, y: Int) = GoPoint.pointAt(x, y)
inline fun GoRectangle(x1: Int, y1: Int, x2: Int, y2: Int) = GoRectangle.rect(x1, x2, y1, y2)

@Suppress("unused")
inline fun String.toGoPoint() = toGoPointOrNull() ?: throw IllegalArgumentException(this)
inline fun String.toGoPointOrNull() = GoPoint.parse(this)
inline fun Int.toGoPointChar() = GoPoint.toChar(this)
@Suppress("unused")
inline fun Char.toGoPointInt() = GoPoint.parseChar(this)
