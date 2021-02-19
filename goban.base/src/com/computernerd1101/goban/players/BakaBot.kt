package com.computernerd1101.goban.players

import com.computernerd1101.goban.*
import com.computernerd1101.goban.internal.atomicUpdater
import com.computernerd1101.goban.internal.getOrDefault
import com.computernerd1101.goban.sgf.GoSGFMoveNode
import kotlinx.coroutines.channels.SendChannel
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
                    randomUpdater[this] = null
                }
            }

        private companion object {
            val randomUpdater = atomicUpdater<Factory, java.util.Random?>("lazyRandom")
        }

        @Volatile private var lazyRandom: java.util.Random? = null

        constructor(random: java.util.Random): this(random.asKotlinRandom()) {
            lazyRandom = random
        }

        var javaRandom: java.util.Random
            @JvmName("getRandom") get() =
                lazyRandom ?: randomUpdater.getOrDefault(this, random.asJavaRandom())
            @JvmName("setRandom") set(random) {
                randomUpdater[this] = random
                this.random = random.asKotlinRandom()
            }

        override fun createPlayer(manager: GoPlayerManager, color: GoColor): BakaBot =
            BakaBot(manager, color, random)

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