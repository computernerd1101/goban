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

    @Test
    fun getGroup() {
        val goban = Goban(5)
        goban[0, 0] = GoColor.BLACK
        goban[1, 0] = GoColor.BLACK
        goban[2, 0] = GoColor.BLACK
        goban[0, 1] = GoColor.WHITE
        goban[1, 1] = GoColor.BLACK
        goban[3, 1] = GoColor.BLACK
        goban[0, 2] = GoColor.WHITE
        goban[1, 2] = GoColor.WHITE
        goban[2, 2] = GoColor.WHITE
        goban[3, 2] = GoColor.BLACK
        goban[4, 2] = GoColor.BLACK
        goban[2, 3] = GoColor.WHITE
        goban[3, 3] = GoColor.WHITE
        goban[4, 3] = GoColor.WHITE
        val group = MutableGoPointSet()
        val p = GoPoint(0, 0)
        goban.getGroup(1, 0, group)
        assert(group.contains(p))
        goban.getGroup(2, 0, group)
        assert(group.contains(p))
        goban.getGroup(1, 1, group)
        assert(group.contains(p))
        goban.getGroup(3, 1, group)
        assert(group.contains(p))
        goban.getGroup(3, 2, group)
        assert(group.contains(p))
        goban.getGroup(4, 2, group)
        assert(group.contains(p))
    }

    @Test
    fun copyFrom() {
        val p = GoPoint(34, 0)
        val goban = Goban(52)
        goban[p] = GoColor.WHITE
        val goban2 = Goban(52)
        goban2.copyFrom(goban, GoPointSet(p))
        assertEquals(GoColor.WHITE, goban2[p])

    }

    @Test
    fun getScoreGoban() {
        val goban = Goban(9)
        goban[1, 0] = GoColor.WHITE
        goban[3, 0] = GoColor.BLACK
        goban[5, 0] = GoColor.WHITE
        goban[7, 0] = GoColor.WHITE
        goban[8, 0] = GoColor.BLACK
        goban[0, 1] = GoColor.WHITE
        goban[1, 1] = GoColor.WHITE
        goban[2, 1] = GoColor.WHITE
        goban[3, 1] = GoColor.BLACK
        goban[4, 1] = GoColor.WHITE
        goban[5, 1] = GoColor.WHITE
        goban[6, 1] = GoColor.WHITE
        goban[7, 1] = GoColor.WHITE
        goban[8, 1] = GoColor.BLACK
        goban[0, 2] = GoColor.BLACK
        goban[1, 2] = GoColor.BLACK
        goban[2, 2] = GoColor.BLACK
        goban[3, 2] = GoColor.WHITE
        goban[4, 2] = GoColor.WHITE
        goban[5, 2] = GoColor.BLACK
        goban[6, 2] = GoColor.BLACK
        goban[7, 2] = GoColor.BLACK
        goban[8, 2] = GoColor.BLACK
        goban[2, 3] = GoColor.BLACK
        goban[3, 3] = GoColor.BLACK
        goban[4, 3] = GoColor.BLACK
        goban[5, 3] = GoColor.BLACK
        goban[0, 4] = GoColor.BLACK
        goban[1, 4] = GoColor.BLACK
        goban[4, 4] = GoColor.BLACK
        goban[6, 4] = GoColor.BLACK
        goban[7, 4] = GoColor.BLACK
        goban[8, 4] = GoColor.BLACK
        goban[0, 5] = GoColor.WHITE
        goban[1, 5] = GoColor.WHITE
        goban[2, 5] = GoColor.BLACK
        goban[3, 5] = GoColor.BLACK
        goban[5, 5] = GoColor.BLACK
        goban[6, 5] = GoColor.WHITE
        goban[7, 5] = GoColor.WHITE
        goban[8, 5] = GoColor.WHITE
        goban[1, 6] = GoColor.WHITE
        goban[2, 6] = GoColor.WHITE
        goban[3, 6] = GoColor.BLACK
        goban[4, 6] = GoColor.BLACK
        goban[5, 6] = GoColor.WHITE
        goban[7, 6] = GoColor.WHITE
        goban[0, 7] = GoColor.WHITE
        goban[1, 7] = GoColor.BLACK
        goban[2, 7] = GoColor.WHITE
        goban[3, 7] = GoColor.WHITE
        goban[4, 7] = GoColor.BLACK
        goban[5, 7] = GoColor.WHITE
        goban[6, 7] = GoColor.WHITE
        goban[7, 7] = GoColor.BLACK
        goban[8, 7] = GoColor.BLACK
        goban[1, 8] = GoColor.BLACK
        goban[3, 8] = GoColor.WHITE
        goban[4, 8] = GoColor.BLACK
        goban[5, 8] = GoColor.WHITE
        goban[7, 8] = GoColor.WHITE
        val expectedTerritory = FixedGoban(9) {
            this[0, 3] = GoColor.BLACK
            this[1, 3] = GoColor.BLACK
            this[6, 3] = GoColor.BLACK
            this[7, 3] = GoColor.BLACK
            this[8, 3] = GoColor.BLACK
            this[2, 4] = GoColor.BLACK
            this[3, 4] = GoColor.BLACK
            this[5, 4] = GoColor.BLACK
            this[4, 5] = GoColor.BLACK
        }
        val expectedArea = FixedGoban(9) {
            this[0, 0] = GoColor.WHITE
            this[1, 0] = GoColor.WHITE
            this[3, 0] = GoColor.BLACK
            this[5, 0] = GoColor.WHITE
            this[6, 0] = GoColor.WHITE
            this[7, 0] = GoColor.WHITE
            this[8, 0] = GoColor.BLACK
            this[0, 1] = GoColor.WHITE
            this[1, 1] = GoColor.WHITE
            this[2, 1] = GoColor.WHITE
            this[3, 1] = GoColor.BLACK
            this[4, 1] = GoColor.WHITE
            this[5, 1] = GoColor.WHITE
            this[6, 1] = GoColor.WHITE
            this[7, 1] = GoColor.WHITE
            this[8, 1] = GoColor.BLACK
            this[0, 2] = GoColor.BLACK
            this[1, 2] = GoColor.BLACK
            this[2, 2] = GoColor.BLACK
            this[3, 2] = GoColor.WHITE
            this[4, 2] = GoColor.WHITE
            this[5, 2] = GoColor.BLACK
            this[6, 2] = GoColor.BLACK
            this[7, 2] = GoColor.BLACK
            this[8, 2] = GoColor.BLACK
            this[0, 3] = GoColor.BLACK
            this[1, 3] = GoColor.BLACK
            this[2, 3] = GoColor.BLACK
            this[3, 3] = GoColor.BLACK
            this[4, 3] = GoColor.BLACK
            this[5, 3] = GoColor.BLACK
            this[6, 3] = GoColor.BLACK
            this[7, 3] = GoColor.BLACK
            this[8, 3] = GoColor.BLACK
            this[0, 4] = GoColor.BLACK
            this[1, 4] = GoColor.BLACK
            this[2, 4] = GoColor.BLACK
            this[3, 4] = GoColor.BLACK
            this[4, 4] = GoColor.BLACK
            this[5, 4] = GoColor.BLACK
            this[6, 4] = GoColor.BLACK
            this[7, 4] = GoColor.BLACK
            this[8, 4] = GoColor.BLACK
            this[0, 5] = GoColor.WHITE
            this[1, 5] = GoColor.WHITE
            this[2, 5] = GoColor.BLACK
            this[3, 5] = GoColor.BLACK
            this[4, 5] = GoColor.BLACK
            this[5, 5] = GoColor.BLACK
            this[6, 5] = GoColor.WHITE
            this[7, 5] = GoColor.WHITE
            this[8, 5] = GoColor.WHITE
            this[0, 6] = GoColor.WHITE
            this[1, 6] = GoColor.WHITE
            this[2, 6] = GoColor.WHITE
            this[3, 6] = GoColor.BLACK
            this[4, 6] = GoColor.BLACK
            this[5, 6] = GoColor.WHITE
            this[6, 6] = GoColor.WHITE
            this[7, 6] = GoColor.WHITE
            this[0, 7] = GoColor.WHITE
            this[1, 7] = GoColor.BLACK
            this[2, 7] = GoColor.WHITE
            this[3, 7] = GoColor.WHITE
            this[4, 7] = GoColor.BLACK
            this[5, 7] = GoColor.WHITE
            this[6, 7] = GoColor.WHITE
            this[7, 7] = GoColor.BLACK
            this[8, 7] = GoColor.BLACK
            this[1, 8] = GoColor.BLACK
            this[3, 8] = GoColor.WHITE
            this[4, 8] = GoColor.BLACK
            this[5, 8] = GoColor.WHITE
            this[6, 8] = GoColor.WHITE
            this[7, 8] = GoColor.WHITE
        }
        assertGobanEquals(expectedArea, goban.scoreGoban)
        assertGobanEquals(expectedTerritory, goban.getScoreGoban(true))
    }

}