package com.computernerd1101.goban.players

import com.computernerd1101.goban.*
import com.computernerd1101.goban.sgf.GoSGFMoveNode
import kotlinx.coroutines.channels.SendChannel
import kotlin.random.*

/**
 * A very stupid robot that chooses its moves completely at random
 * amongst all legal moves except for multi-stone suicide.
 */
class BakaBot(
    manager: GoPlayerManager,
    color: GoColor,
    @get:JvmName("getKotlinRandom")
    val random: Random
): GoPlayer(manager, color) {

    constructor(manager: GoPlayerManager, color: GoColor): this(manager, color, Random)

    @Suppress("unused")
    constructor(manager: GoPlayerManager, color: GoColor, random: java.util.Random):
            this(manager, color, random.asKotlinRandom())

    @Suppress("unused")
    val javaRandom: java.util.Random
        @JvmName("getRandom") get() = random.asJavaRandom()

    companion object DefaultFactory: GoPlayer.Factory {

        override fun createPlayer(manager: GoPlayerManager, color: GoColor) =
            BakaBot(manager, color)

    }

    @Suppress("unused")
    class Factory(
        @get:JvmName("getKotlinRandom")
        @set:JvmName("setKotlinRandom")
        var random: Random
    ): GoPlayer.Factory {

        constructor(): this(Random)

        constructor(random: java.util.Random): this(random.asKotlinRandom())

        var javaRandom: java.util.Random
            @JvmName("getRandom") get() = random.asJavaRandom()
            @JvmName("setRandom") set(random) {
                this.random = random.asKotlinRandom()
            }

        override fun createPlayer(manager: GoPlayerManager, color: GoColor) =
            BakaBot(manager, color, random)

    }

    private val pointList = mutableListOf<GoPoint>()

    private val goban: Goban = manager.sgf.let { sgf -> Goban(sgf.width, sgf.height) }

    private val superkoRestrictions = MutableGoPointSet()

    override suspend fun generateHandicapStones(handicap: Int, goban: Goban) {
        goban.clear()
        pointList.clear()
        val width = goban.width
        val height = goban.height
        val size = width * height
        for(y in 0 until height) for(x in 0 until width)
            pointList.add(GoPoint(x, y))
        for(i in size - 1 downTo size - handicap) {
            val j = random.nextInt(i + 1)
            val point = pointList.set(j, pointList.set(i, pointList[j]))
            goban[point] = GoColor.BLACK
        }
    }

    override suspend fun requestMove(channel: SendChannel<GoPoint?>) {
        pointList.clear()
        val node = manager.node
        val nodeGoban = node.goban
        superkoRestrictions.clear()
        if (node is GoSGFMoveNode) {
            node.getSuperkoRestrictions(superkoRestrictions, manager.gameInfo.rules)
        }
        for(y in 0 until goban.height) for(x in 0 until goban.width) {
            val p = GoPoint(x, y)
            if (superkoRestrictions.contains(p) || nodeGoban[p] != null) continue
            goban.copyFrom(nodeGoban)
            goban.play(p, color)
            if (goban[p] != null)
                pointList.add(p)
        }
        val point = if (pointList.isEmpty()) null
        else pointList[random.nextInt(pointList.size)]
        channel.send(point)
    }

}