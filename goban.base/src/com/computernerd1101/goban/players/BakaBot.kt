package com.computernerd1101.goban.players

import com.computernerd1101.goban.*
import com.computernerd1101.goban.internal.InternalMarker
import com.computernerd1101.goban.internal.atomicUpdater
import com.computernerd1101.goban.internal.getOrDefault
import com.computernerd1101.goban.sgf.GoSGFMoveNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.*

/**
 * A very stupid robot that chooses its moves completely at random
 * amongst all legal moves except for multi-stone suicide.
 */
class BakaBot private constructor(
    game: GoGameManager,
    color: GoColor,
    @get:JvmName("getKotlinRandom")
    val random: Random,
    @Suppress("unused")
    @get:JvmName("getRandom")
    val javaRandom: java.util.Random
): GoPlayer(game, color) {

    constructor(game: GoGameManager, color: GoColor): this(game, color, Random)

    constructor(game: GoGameManager, color: GoColor, random: Random):
            this(game, color, random, random.asJavaRandom())

    @Suppress("unused")
    constructor(game: GoGameManager, color: GoColor, random: java.util.Random):
            this(game, color, random.asKotlinRandom(), random)

    companion object DefaultFactory: GoPlayer.Factory {

        override fun createPlayer(game: GoGameManager, color: GoColor) =
            BakaBot(game, color)

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

        override fun createPlayer(game: GoGameManager, color: GoColor): BakaBot =
            BakaBot(game, color, random)

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
            points[j] = points[i]
            points[i] = p
            goban[p] = GoColor.BLACK
        }
    }

    override suspend fun generateMove(): GoPoint? = withContext(Dispatchers.IO, moveGenerator)
    private val moveGenerator: suspend CoroutineScope.() -> GoPoint? = {
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
        if (count == 0) null
        else points[random.nextInt(count)]
    }

}