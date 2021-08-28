package com.computernerd1101.goban.internal

import java.util.concurrent.ThreadLocalRandom
import kotlin.random.*

internal class SynchronizedRandom(private val random: Random): Random() {

    constructor(seed: Long): this(Random(seed))

    override fun nextBits(bitCount: Int): Int = synchronized(random) {
        random.nextBits(bitCount)
    }

}

internal fun Random.asSynchronizedRandom(): Random =
    if (this === Random || this is SynchronizedRandom) this
    else SynchronizedRandom(this)

internal fun java.util.Random.asSynchronizedRandom(): Random =
    if (this === ThreadLocalRandom.current()) Random else SynchronizedRandom(this.asKotlinRandom())
