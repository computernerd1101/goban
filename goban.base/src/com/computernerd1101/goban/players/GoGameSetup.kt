package com.computernerd1101.goban.players

import com.computernerd1101.goban.GoColor
import com.computernerd1101.goban.Goban
import com.computernerd1101.goban.sgf.*
import kotlin.random.Random

/**
 * Works exactly like a data class, except that [width] and [height]
 * are each restricted to 1 thru 52, and can be condensed into a single
 * size parameter for both the constructor and the copy method.
 */
class GoGameSetup {

    companion object {

        @JvmStatic
        fun maxFixedHandicap(size: Int): Int =
            (if (size < 3) 0 else size and 1) + (if (size < 5) 0 else 2)

        @JvmStatic
        fun cornerStarPoint(size: Int): Int = when {
            size < 5 -> 0
            size < 7 -> 1
            size < 12 -> 2
            else -> 3
        }

    }

    constructor(
        blackPlayer: GoPlayer.Factory,
        whitePlayer: GoPlayer.Factory,
        width: Int,
        height: Int,
        gameInfo: GameInfo = GameInfo().apply { dates.addDate(Date()) },
        randomPlayer: Random? = null,
        isFreeHandicap: Boolean = false
    ) {
        if (width !in 1..52) throw IllegalArgumentException("$width is not in the range [1,52]")
        if (height !in 1..52) throw IllegalArgumentException("$height is not in the range [1,52]")
        this.blackPlayer = blackPlayer
        this.whitePlayer = whitePlayer
        this.width = width
        this.height = height
        this.gameInfo = gameInfo
        this.randomPlayer = randomPlayer
        this.isFreeHandicap = isFreeHandicap
    }

    constructor(
        blackPlayer: GoPlayer.Factory,
        whitePlayer: GoPlayer.Factory,
        size: Int = 19,
        gameInfo: GameInfo = GameInfo().apply { dates.addDate(Date()) },
        randomPlayer: Random? = null,
        isFreeHandicap: Boolean = false
    ) {
        if (size !in 1..52) throw IllegalArgumentException("$size is not in the range [1,52]")
        this.blackPlayer = blackPlayer
        this.whitePlayer = whitePlayer
        width = size
        height = size
        this.gameInfo = gameInfo
        this.randomPlayer = randomPlayer
        this.isFreeHandicap = isFreeHandicap
    }

    var blackPlayer: GoPlayer.Factory
    var whitePlayer: GoPlayer.Factory

    var player1: GoPlayer.Factory
        get() = blackPlayer
        set(player) { blackPlayer = player }

    var player2: GoPlayer.Factory
        get() = whitePlayer
        set(player) { whitePlayer = player }

    var width: Int
        set(value) {
            if (value !in 1..52) throw IllegalArgumentException("$value is not in the range [1,52]")
            field = value
        }
    var height: Int
        set(value) {
            if (value !in 1..52) throw IllegalArgumentException("$value is not in the range [1,52]")
            field = value
        }

    var gameInfo: GameInfo
    var randomPlayer: Random?
    var isFreeHandicap: Boolean

    val maxHandicap: Int
        @JvmName("maxHandicap")
        get() {
            val width = this.width
            val height = this.height
            val max = if (isFreeHandicap) width*height - 1
            else maxFixedHandicap(width) * maxFixedHandicap(height)
            return if (max <= 1) 0 else max
        }

    fun generateFixedHandicap(): Goban? {
        val width = this.width
        val height = this.height
        val maxX = maxFixedHandicap(width)
        val maxY = maxFixedHandicap(height)
        val handicap = gameInfo.handicap.coerceAtMost(maxX * maxY)
        if (handicap <= 1) return null
        val goban = Goban(width, height)
        when(handicap) {
            2, 3 -> when {
                maxX == 1 -> {
                    val x = width shr 1
                    val y1 = cornerStarPoint(height)
                    val y2 = height - 1 - y1
                    goban[x, y1] = GoColor.BLACK
                    goban[x, y2] = GoColor.BLACK
                    if (handicap == 3)
                        goban[x, height shr 1] = GoColor.BLACK
                }
                maxY == 1 -> {
                    val x1 = cornerStarPoint(width)
                    val x2 = width - 1 - x1
                    val y = height shr 1
                    goban[x1, y] = GoColor.BLACK
                    goban[x2, y] = GoColor.BLACK
                    if (handicap == 3)
                        goban[width shr 1, y] = GoColor.BLACK
                }
                else -> {
                    val x1 = cornerStarPoint(width)
                    val x2 = width - 1 - x1
                    val y1 = cornerStarPoint(height)
                    val y2 = height - 1 - y1
                    goban[x2, y1] = GoColor.BLACK
                    goban[x1, y2] = GoColor.BLACK
                    if (handicap == 3)
                        goban[x2, y2] = GoColor.BLACK
                }
            }
            in 4..9 -> {
                val x1 = cornerStarPoint(width)
                val x3 = width - 1 - x1
                val y1 = cornerStarPoint(height)
                val y3 = height - 1 - y1
                goban[x1, y1] = GoColor.BLACK
                goban[x3, y1] = GoColor.BLACK
                goban[x1, y3] = GoColor.BLACK
                goban[x3, y3] = GoColor.BLACK
                when(handicap) {
                    5 -> when {
                        width and 1 == 0 -> goban[x3, height shr 1] = GoColor.BLACK
                        height and 1 == 0 -> goban[width shr 1, y3] = GoColor.BLACK
                        else -> goban[width shr 1, height shr 1] = GoColor.BLACK
                    }
                    6, 7 -> if (width and 1 != 0 && (height and 1 == 0 || width > height)) {
                        val x2 = width shr 1
                        goban[x2, y1] = GoColor.BLACK
                        goban[x2, y3] = GoColor.BLACK
                        if (handicap == 7) goban[x2, height shr 1] = GoColor.BLACK
                    } else {
                        val y2 = height shr 1
                        goban[x1, y2] = GoColor.BLACK
                        goban[x3, y2] = GoColor.BLACK
                        if (handicap == 7) goban[width shr 1, y2] = GoColor.BLACK
                    }
                    8, 9 -> {
                        val x2 = width shr 1
                        val y2 = height shr 1
                        goban[x2, y1] = GoColor.BLACK
                        goban[x2, y3] = GoColor.BLACK
                        goban[x1, y2] = GoColor.BLACK
                        goban[x3, y2] = GoColor.BLACK
                        if (handicap == 9)
                            goban[x2, y2] = GoColor.BLACK
                    }
                }
            }
        }
        return goban
    }

    operator fun component1() = blackPlayer
    operator fun component2() = whitePlayer
    operator fun component3() = width
    operator fun component4() = height
    operator fun component5() = gameInfo
    operator fun component6() = randomPlayer
    operator fun component7() = isFreeHandicap

    @Suppress("unused")
    fun copy(
        blackPlayer: GoPlayer.Factory = this.blackPlayer,
        whitePlayer: GoPlayer.Factory = this.whitePlayer,
        width: Int = this.width,
        height: Int = this.height,
        gameInfo: GameInfo = this.gameInfo,
        randomPlayer: Random? = this.randomPlayer,
        isFreeHandicap: Boolean = this.isFreeHandicap
    ) = GoGameSetup(blackPlayer, whitePlayer,
        width, height,
        gameInfo, randomPlayer, isFreeHandicap
    )

    @Suppress("unused")
    fun copy(
        size: Int,
        blackPlayer: GoPlayer.Factory = this.blackPlayer,
        whitePlayer: GoPlayer.Factory = this.whitePlayer,
        gameInfo: GameInfo = this.gameInfo,
        randomPlayer: Random? = this.randomPlayer,
        isFreeHandicap: Boolean = this.isFreeHandicap
    ) = GoGameSetup(blackPlayer, whitePlayer,
        size, gameInfo, randomPlayer, isFreeHandicap
    )

    override fun equals(other: Any?): Boolean {
        return this === other || (
                other is GoGameSetup &&
                        blackPlayer == other.blackPlayer &&
                        whitePlayer == other.whitePlayer &&
                        width == other.width &&
                        height == other.height &&
                        gameInfo == other.gameInfo &&
                        randomPlayer == other.randomPlayer &&
                        isFreeHandicap == other.isFreeHandicap
                )
    }

    override fun hashCode(): Int {
        return (((((blackPlayer.hashCode()*31 +
                whitePlayer.hashCode())*31 +
                width)*31 +
                height)*31 +
                gameInfo.hashCode())*31 +
                randomPlayer.hashCode())*31 +
                isFreeHandicap.hashCode()
    }

    override fun toString(): String {
        val width = this.width
        val height = this.height
        val randomPlayer = this.randomPlayer
        return buildString {
            append("GoGameSetup(")
            if (randomPlayer != null)
                append("player1=").append(player1).append(", player2=").append(player2)
            else append("blackPlayer=").append(blackPlayer).append(", whitePlayer=").append(whitePlayer)
            if (width == height)
                append(", size=")
            else append(", width=").append(width).append(", height=")
            append(height).append(", gameInfo=").append(gameInfo)
                .append(", randomPlayer=").append(randomPlayer)
                .append(", isFreeHandicap=").append(isFreeHandicap).append(")")
        }
    }

}