package com.computernerd1101.goban.test

import com.computernerd1101.goban.AbstractGoban
import org.junit.Assert.*

fun assertGobanEquals(expected: AbstractGoban, actual: AbstractGoban) {
    if (expected.width != actual.width || expected.height != actual.height)
        fail("width x height: expected: ${expected.width}x${expected.height} but was: ${actual.width}x${actual.height}")
    for(y in 0 until expected.height) for(x in 0 until expected.width) {
        val expectedColor = expected[x, y]
        val actualColor = actual[x, y]
        if (expectedColor != actualColor)
            fail("[$x, $y]: expected: ${expectedColor?.name ?: "null"} but was: ${actualColor?.name ?: "null"}")
    }
}