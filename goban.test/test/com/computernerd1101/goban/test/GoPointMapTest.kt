package com.computernerd1101.goban.test

import com.computernerd1101.goban.*
import org.junit.*

import org.junit.Assert.*
import kotlin.random.Random

internal class GoPointMapTest {

    private lateinit var random: Random
    private lateinit var pairs: Array<Pair<GoPoint, String>>
    private lateinit var goMap: GoPointMap<String>
    private lateinit var hashMap: HashMap<GoPoint, String>

    @Before fun setUp() {
        random = Random
        pairs = randomIntArray().map { y ->
            randomIntArray().map { x ->
                val p = GoPoint(x, y)
                p to p.toString()
            }
        }.flatten().toTypedArray()
        goMap = GoPointMap(*pairs)
        hashMap = hashMapOf(*pairs)
    }

    private fun randomIntArray(): IntArray {
        var array = IntArray(52) { it }
        array.shuffle(random)
        array = array.copyOf(random.nextInt(5, 11))
        array.sort()
        return array
    }

    @Test fun get() {
        for((k, v) in pairs) {
            assertEquals(v, goMap[k])
        }
    }

    @Test fun set() {
        val actual = MutableGoPointMap<String>()
        for((k, v) in pairs) {
            actual[k] = v
        }
        assertEquals(goMap, actual)
    }

    @Test fun size() {
        assertEquals(pairs.size, goMap.size)
    }

    @Test fun iterator() {
        val expectedItr = pairs.iterator()
        val actualItr = goMap.iterator()
        while(expectedItr.hasNext()) {
            assertTrue(actualItr.hasNext())
            val (ek, ev) = expectedItr.next()
            val (ak, av) = actualItr.next()
            assertEquals(ek, ak)
            assertEquals(ev, av)
        }
        assertFalse(actualItr.hasNext())
    }

    @Test fun equals() {
        assertEquals(hashMap, goMap)
    }

    @Test fun testHashCode() {
        assertEquals(hashMap.hashCode(), goMap.hashCode())
    }

}