package com.computernerd1101.sgf.internal

fun checkElementIndex(index: Int, size: Int) {
    if (index !in 0 until size)
        throw IndexOutOfBoundsException("index: $index, size: $size")
}

fun checkPositionIndex(index: Int, size: Int) {
    if (index !in 0..size)
        throw IndexOutOfBoundsException("index: $index, size: $size")
}

fun checkRangeIndexes(fromIndex: Int, toIndex: Int, size: Int) {
    if (fromIndex < 0 || toIndex > size)
        throw IndexOutOfBoundsException("fromIndex: $fromIndex, toIndex: $toIndex, size: $size")
    if (fromIndex > toIndex)
        throw IllegalArgumentException("fromIndex: $fromIndex > toIndex: $toIndex")
}

fun checkBoundsIndexes(start: Int, end: Int, size: Int) {
    if (start < 0 || end > size)
        throw IndexOutOfBoundsException("start: $start, end $end, size: $size")
    if (start > end)
        throw IllegalArgumentException("start: $start > end: $end")
}