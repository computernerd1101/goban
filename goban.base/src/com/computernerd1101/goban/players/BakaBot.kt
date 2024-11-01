package com.computernerd1101.goban.players

import com.computernerd1101.goban.*
import com.computernerd1101.goban.internal.*
import com.computernerd1101.goban.sgf.GoSGFMoveNode
import kotlinx.coroutines.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.random.*

/**
 * A very stupid robot that chooses its moves completely at random
 * amongst all legal moves except for multi-stone suicide.
 */
class BakaBot: GoPlayer {

    constructor(game: GoGameManager, color: GoColor): super(game, color) {
        random = Random
        javaRandom = ThreadLocalRandom.current()
    }

    constructor(game: GoGameManager, color: GoColor, random: Random): super(game, color) {
        var kotlinRandom = random
        val javaRandom: java.util.Random
        if (kotlinRandom === Random) {
            javaRandom = ThreadLocalRandom.current()
        } else {
            javaRandom = kotlinRandom.asJavaRandom()
            if (javaRandom === ThreadLocalRandom.current())
                kotlinRandom = Random
        }
        this.random = kotlinRandom
        this.javaRandom = javaRandom
    }

    constructor(game: GoGameManager, color: GoColor, random: java.util.Random): super(game, color) {
        val kotlinRandom: Random
        var javaRandom = random
        if (javaRandom === ThreadLocalRandom.current()) {
            kotlinRandom = Random
        } else {
            kotlinRandom = javaRandom.asKotlinRandom()
            if (kotlinRandom === Random)
                javaRandom = ThreadLocalRandom.current()
        }
        this.random = kotlinRandom
        this.javaRandom = javaRandom
    }

    @Suppress("unused")
    constructor(game: GoGameManager, color: GoColor, seed: Int): this(game, color, seed.toLong())

    constructor(game: GoGameManager, color: GoColor, seed: Long): super(game, color) {
        val random = java.util.Random(seed)
        this.random = random.asKotlinRandom()
        javaRandom = random
    }

    @get:JvmName("getKotlinRandom")
    val random: Random

    @get:JvmName("getRandom")
    val javaRandom: java.util.Random

    companion object DefaultFactory: GoPlayer.Factory {

        override fun createPlayer(game: GoGameManager, color: GoColor) =
            BakaBot(game, color)

    }

    @Suppress("unused")
    class Factory: GoPlayer.Factory {

        constructor() {
            _random = Random
        }

        constructor(random: Random) {
            _random = RandomWrapper.wrapKotlin(random)
        }

        constructor(random: java.util.Random) {
            _random = RandomWrapper.wrapJava(random)
        }

        constructor(seed: Int): this(seed.toLong())

        constructor(seed: Long) {
            _random = java.util.Random(seed).asKotlinRandom()
        }

        /*
         * _random is either a kotlin.random.Random wrapped around a java.util.Random, or vice versa,
         * or kotlin.random.Random.Default. That way, neither random nor javaRandom will allocate new
         * wrapper objects with their getters. They will either return the wrapper object that already
         * exists, or unwrap it. The setters are thread-safe because they only have one field to write to.
         */
        @Volatile private var _random: Any

        var random: Random
            @JvmName("getKotlinRandom")
            get() = when(val random = _random) {
                is Random -> random
                else -> (random as java.util.Random).asKotlinRandom()
            }
            @JvmName("setKotlinRandom")
            set(random) {
                _random = RandomWrapper.wrapKotlin(random)
            }

        var javaRandom: java.util.Random
            @JvmName("getRandom") get() {
                val random = _random
                return when {
                    random is java.util.Random -> random
                    random === Random -> ThreadLocalRandom.current()
                    else -> (random as Random).asJavaRandom()
                }
            }
            @JvmName("setRandom") set(random) {
                _random = RandomWrapper.wrapJava(random)
            }

        /**
         * Sets the seed of [javaRandom]. If [javaRandom] is a direct instance of [java.util.Random],
         * and not a subclass thereof, then [setSeed] sets the seed of the instance that already exists.
         * Otherwise, [javaRandom] is set to a new direct instance of [java.util.Random] with the given [seed].
         */
        fun setSeed(seed: Long) {
            val random = _random
            if (random !== Random && random is Random) {
                val javaRandom: java.util.Random = random.asJavaRandom()
                if (javaRandom.javaClass == java.util.Random::class.java) {
                    javaRandom.setSeed(seed)
                    return
                }
            }
            _random = java.util.Random(seed).asKotlinRandom()
        }

        override fun createPlayer(game: GoGameManager, color: GoColor): BakaBot = when(val random = _random) {
            is Random -> BakaBot(game, color, random)
            else -> BakaBot(game, color, random as java.util.Random)
        }

    }

    private object RandomWrapper: Random() {

        override fun nextBits(bitCount: Int): Int = Random.nextBits(bitCount)

        private val kotlinWrapper = java.util.Random().asKotlinRandom().javaClass
        private val javaWrapper = RandomWrapper.asJavaRandom().javaClass

        @JvmStatic fun wrapKotlin(random: Random): Any = when {
            random === Random -> random
            kotlinWrapper.isInstance(random) -> {
                if (random.asJavaRandom() === ThreadLocalRandom.current()) Random else random
            }
            else -> random.asJavaRandom()
        }

        @JvmStatic fun wrapJava(random: java.util.Random): Any = when {
            random === ThreadLocalRandom.current() -> Random
            javaWrapper.isInstance(random) -> {
                if (random.asKotlinRandom() === Random) Random else random
            }
            else -> random.asKotlinRandom()
        }

    }

    private var points: Array<GoPoint?>? = null

    private var goban: Goban? = null

    private fun getPoints(marker: InternalMarker): Array<GoPoint?> {
        marker.ignore()
        var points = this.points
        if (points == null) {
            val sgf = game.sgf
            val width = sgf.width
            val height = sgf.height
            points = arrayOfNulls(width * height)
            this.points = points
            goban = Goban(width, height)
        }
        return points
    }

    private fun getGoban(marker: InternalMarker): Goban {
        marker.ignore()
        var goban = this.goban
        if (goban == null) {
            val sgf = game.sgf
            val width = sgf.width
            val height = sgf.height
            points = arrayOfNulls(width * height)
            goban = Goban(width, height)
            this.goban = goban
        }
        return goban
    }

    private val superkoRestrictions = MutableGoPointSet()

    override suspend fun generateHandicapStones(handicap: Int, goban: Goban) = withContext(Dispatchers.IO) {
        goban.clear()
        val points = getPoints(InternalMarker)
        val width = goban.width
        val height = goban.height
        val size = width * height
        for(y in 0 until height) for(x in 0 until width)
            points[x + y*width] = GoPoint(x, y)
        for(i in size - 1 downTo size - handicap) {
            val j = random.nextInt(i + 1)
            val p = points[j]!!
            if (j < i) {
                points[j] = points[i]
                points[i] = p
            }
            goban[p] = GoColor.BLACK
        }
    }

    override suspend fun generateMove(): GoPoint? {
        val points = getPoints(InternalMarker)
        val goban = getGoban(InternalMarker)
        val node = game.node
        val nodeGoban = node.goban
        superkoRestrictions.clear()
        (node as? GoSGFMoveNode)?.getSuperkoRestrictions(game.gameInfo.rules.superko, superkoRestrictions)
        var count = 0
        for(y in 0 until goban.height) for(x in 0 until goban.width) {
            val p = GoPoint(x, y)
            if (superkoRestrictions.contains(p) || nodeGoban[p] != null) continue
            goban.copyFrom(nodeGoban)
            goban.play(p, color)
            if (goban[p] != null) {
                points[count++] = p
            }
        }
        return if (count == 0) null
        else points[random.nextInt(count)]
    }

}
