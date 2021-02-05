package com.computernerd1101.goban.test

import com.computernerd1101.goban.*
import org.junit.Test

import org.junit.Assert.*
import kotlin.random.Random

internal class GobanTest {

    @Test
    fun count() {
        val goban = MutableGoban()
        val rnd = Random(System.currentTimeMillis())
        var blackCount = 0
        var whiteCount = 0
        for(y in 0..18)
            for(x in 0..18) {
                when(rnd.nextInt(3)) {
                    1 -> {
                        goban[x, y] = GoColor.BLACK
                        blackCount++
                    }
                    2 -> {
                        goban[x, y] = GoColor.WHITE
                        whiteCount++
                    }
                }
            }
        assertEquals(blackCount, goban.blackCount)
        assertEquals(whiteCount, goban.whiteCount)
    }

    @Test
    fun setAll() {
        val expected = MutableGoban()
        val actual = MutableGoban()
        val black = MutableGoPointSet()
        val white = MutableGoPointSet()
        val rnd = Random(System.currentTimeMillis())
        for(y in 0..18)
            for(x in 0..18) {
                when(rnd.nextInt(3)) {
                    1 -> {
                        expected[x, y] = GoColor.BLACK
                        black.add(GoPoint(x, y))
                    }
                    2 -> {
                        expected[x, y] = GoColor.WHITE
                        white.add(GoPoint(x, y))
                    }
                }
            }
        actual.setAll(black, GoColor.BLACK)
        actual.setAll(white, GoColor.WHITE)
        assertTrue(expected contentEquals actual)
    }

    @Test
    fun play() {
        val goban = Goban()
        goban[4, 4] = GoColor.WHITE
        goban[4, 3] = GoColor.BLACK
        goban[5, 4] = GoColor.BLACK
        goban[4, 5] = GoColor.BLACK
        goban.play(GoPoint(3, 4), GoColor.BLACK)
        assertNull(goban[4, 4])
    }

}