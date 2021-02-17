package com.computernerd1101.goban.players

import com.computernerd1101.goban.*
import com.computernerd1101.goban.sgf.GoSGFMoveNode
import kotlinx.coroutines.channels.SendChannel
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.*

/**
 * A very stupid robot that chooses its moves completely at random
 * amongst all legal moves except for multi-stone suicide.
 */
class BakaBot: GoPlayer {

    constructor(manager: GoPlayerManager, color: GoColor): this(manager, color, Random)

    constructor(manager: GoPlayerManager, color: GoColor, random: Random): super(manager, color) {
        this.random = random
        javaRandom = random.asJavaRandom()
    }

    @Suppress("unused")
    constructor(manager: GoPlayerManager, color: GoColor, random: java.util.Random): super(manager, color) {
        this.random = random.asKotlinRandom()
        javaRandom = random
    }

    @get:JvmName("getKotlinRandom")
    val random: Random

    @Suppress("unused")
    @get:JvmName("getRandom")
    val javaRandom: java.util.Random

    companion object DefaultFactory: GoPlayer.Factory {

        override fun createPlayer(manager: GoPlayerManager, color: GoColor) =
            BakaBot(manager, color)

    }

    @Suppress("unused")
    class Factory(random: Random): GoPlayer.Factory {

        constructor(): this(Random)

        @get:JvmName("getKotlinRandom")
        var random: Random = random
            @JvmName("setKotlinRandom")
            set(random) {
                if (field !== random) {
                    field = random
                    lazyRandom.set(null)
                }
            }

        private val lazyRandom = AtomicReference<java.util.Random>()

        constructor(random: java.util.Random): this(random.asKotlinRandom()) {
            lazyRandom.set(random)
        }

        var javaRandom: java.util.Random
            @JvmName("getRandom") get() =
                lazyRandom.get() ?: lazyRandom.compareAndExchange(null, random.asJavaRandom())
            @JvmName("setRandom") set(random) {
                lazyRandom.set(random)
                this.random = random.asKotlinRandom()
            }

        override fun createPlayer(manager: GoPlayerManager, color: GoColor): BakaBot {
            val javaRandom = lazyRandom.get() ?: return BakaBot(manager, color, random)
            return BakaBot(manager, color, javaRandom)
        }

    }

    private val points: Array<GoPoint?>

    private val goban: Goban

    init {
        val sgf = manager.sgf
        val width = sgf.width
        val height = sgf.height
        points = arrayOfNulls(width * height)
        goban = Goban(width, height)
    }

    private val superkoRestrictions = MutableGoPointSet()

    override suspend fun generateHandicapStones(handicap: Int, goban: Goban) {
        goban.clear()
        val width = goban.width
        val height = goban.height
        val size = width * height
        for(y in 0 until height) for(x in 0 until width)
            points[x + y*width] = GoPoint(x, y)
        for(i in size - 1 downTo size - handicap) {
            val j = random.nextInt(i + 1)
            val p = points[j]!!
            points[j] = points[i]
            points[i] = p
            goban[p] = GoColor.BLACK
        }
    }

    override suspend fun requestMove(channel: SendChannel<GoPoint?>) {
        val node = manager.node
        val nodeGoban = node.goban
        superkoRestrictions.clear()
        (node as? GoSGFMoveNode)?.getSuperkoRestrictions(manager.gameInfo.rules.superko, superkoRestrictions)
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
        val point = if (count == 0) null
        else points[random.nextInt(count)]
        channel.send(point)
    }

}