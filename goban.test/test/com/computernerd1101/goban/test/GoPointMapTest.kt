package com.computernerd1101.goban.test

import com.computernerd1101.goban.*
import org.junit.*

import org.junit.Assert.*
import kotlin.random.*

internal class GoPointMapTest {

    companion object {

        private lateinit var random: Random
        private lateinit var pairs: Array<Pair<GoPoint, String>>
        private lateinit var goMap: GoPointMap<String>
        private lateinit var hashMap: HashMap<GoPoint, String>

        @BeforeClass
        @JvmStatic
        fun setUpClass() {
            val seed = System.nanoTime()
            println("Setting up GoPointMapTest...")
            println("Using seed $seed")
            random = java.util.Random(seed).asKotlinRandom()
            pairs = randomIntArray().map { y ->
                val row = randomIntArray().map { x ->
                    val p = GoPoint(x, y)
                    p to p.toString()
                }
                println(row.joinToString(separator = " ", transform = Pair<GoPoint, String>::second))
                row
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
            val (expectedKey, expectedValue) = expectedItr.next()
            val (actualKey, actualValue) = actualItr.next()
            assertEquals(expectedKey, actualKey)
            assertEquals(expectedValue, actualValue)
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