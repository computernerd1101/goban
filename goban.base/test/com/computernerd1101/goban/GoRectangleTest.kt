package com.computernerd1101.goban

import org.junit.*

import org.junit.Assert.*

internal class GoRectangleTest {

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
    }

    @Test
    fun testHashCode() {
        for(y1 in 0..51)
            for(x1 in 0..51)
                for(y2 in y1..51)
                    for(x2 in x1..51) {
                        val expected = mutableSetOf<GoPoint>()
                        for(y in y1..y2)
                            for(x in x1..x2)
                                expected.add(GoPoint(x, y))
                        val actual = GoRectangle(x1, y1, x2, y2)
                        assertEquals(expected.hashCode(), actual.hashCode())
                    }
    }
}