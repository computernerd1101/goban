package com.computernerd1101.goban.players

import com.computernerd1101.goban.internal.IntBinOp
import com.computernerd1101.goban.sgf.*
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater

/**
 * Works exactly like a data class, except that [width] and [height]
 * are each restricted to 1 thru 52, and can be condensed into a single
 * size parameter for both the constructor and the copy method.
 * Also, Boolean fields are condensed into a single volatile Int field.
 */
class GoGameSetup {

    companion object {

        @JvmStatic
        fun maxFixedHandicap(size: Int): Int =
            (if (size < 3) 0 else size and 1) + (if (size < 5) 0 else 2)

        private val updateFlags: AtomicIntegerFieldUpdater<GoGameSetup> =
            AtomicIntegerFieldUpdater.newUpdater(GoGameSetup::class.java, "flags")

        private const val RANDOM_PLAYER = 1
        private const val FREE_HANDICAP = 2

    }

    constructor(
        width: Int,
        height: Int,
        blackPlayer: GoPlayerController? = null,
        whitePlayer: GoPlayerController? = null,
        isRandomPlayer: Boolean = false,
        gameInfo: GameInfo = GameInfo().apply { dates.addDate(Date()) },
        isFreeHandicap: Boolean = false
    ) {
        if (width !in 1..52) throw IllegalArgumentException("$width is not in the range [1,52]")
        if (height !in 1..52) throw IllegalArgumentException("$height is not in the range [1,52]")
        this.width = width
        this.height = height
        this.blackPlayer = blackPlayer
        this.whitePlayer = whitePlayer
        this.gameInfo = gameInfo
        flags = getFlags(
            isRandomPlayer,
            isFreeHandicap
        )
    }

    constructor(
        size: Int = 19,
        blackPlayer: GoPlayerController? = null,
        whitePlayer: GoPlayerController? = null,
        isRandomPlayer: Boolean = false,
        gameInfo: GameInfo = GameInfo().apply { dates.addDate(Date()) },
        isFreeHandicap: Boolean = false
    ) {
        if (size !in 1..52) throw IllegalArgumentException("$size is not in the range [1,52]")
        width = size
        height = size
        this.blackPlayer = blackPlayer
        this.whitePlayer = whitePlayer
        this.gameInfo = gameInfo
        flags = getFlags(
            isRandomPlayer,
            isFreeHandicap
        )
    }

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
    var blackPlayer: GoPlayerController?
    var whitePlayer: GoPlayerController?

    var player1: GoPlayerController?
        get() = blackPlayer
        set(player) { blackPlayer = player}

    var player2: GoPlayerController?
        get() = whitePlayer
        set(player) { whitePlayer = player}

    var isRandomPlayer: Boolean
        get() = flags and RANDOM_PLAYER != 0
        set(value) = setFlag(RANDOM_PLAYER, value)
    var gameInfo: GameInfo
    var isFreeHandicap: Boolean
        get() = flags and FREE_HANDICAP != 0
        set(value) = setFlag(FREE_HANDICAP, value)

    private fun getFlags(
        isRandomPlayer: Boolean,
        isFreeHandicap: Boolean
    ) = (if (isRandomPlayer) RANDOM_PLAYER else 0) or
            (if (isFreeHandicap) FREE_HANDICAP else 0)

    private fun setFlag(on: Int, value: Boolean) {
        val op: IntBinOp
        val flag: Int
        if (value) {
            op = IntBinOp.OR
            flag = on
        } else {
            op = IntBinOp.AND
            flag = on.inv()
        }
        updateFlags.accumulateAndGet(this, flag, op)
    }

    @Volatile private var flags: Int

    val maxHandicap: Int
        @JvmName("maxHandicap")
        get() {
            val width = this.width
            val height = this.height
            val max = if (isFreeHandicap) width*height - 1
            else maxFixedHandicap(width)*maxFixedHandicap(height)
            return if (max <= 1) 0 else max
        }

    operator fun component1() = width
    operator fun component2() = height
    operator fun component3() = blackPlayer
    operator fun component4() = whitePlayer
    operator fun component5() = isRandomPlayer
    operator fun component6() = gameInfo
    operator fun component7() = isFreeHandicap

    @Suppress("unused")
    fun copy(
        width: Int = this.width,
        height: Int = this.height,
        blackPlayer: GoPlayerController? = this.blackPlayer,
        whitePlayer: GoPlayerController? = this.whitePlayer,
        isRandomPlayer: Boolean = this.isRandomPlayer,
        gameInfo: GameInfo = this.gameInfo,
        isFreeHandicap: Boolean = this.isFreeHandicap
    ) = GoGameSetup(width, height,
        blackPlayer, whitePlayer,
        isRandomPlayer, gameInfo, isFreeHandicap
    )

    @Suppress("unused")
    fun copy(
        size: Int,
        blackPlayer: GoPlayerController? = this.blackPlayer,
        whitePlayer: GoPlayerController? = this.whitePlayer,
        isRandomPlayer: Boolean = this.isRandomPlayer,
        gameInfo: GameInfo = this.gameInfo,
        isFreeHandicap: Boolean = this.isFreeHandicap
    ) = GoGameSetup(size,
        blackPlayer, whitePlayer,
        isRandomPlayer, gameInfo, isFreeHandicap
    )

    override fun equals(other: Any?): Boolean {
        return this === other || (
                other is GoGameSetup &&
                        width == other.width &&
                        height == other.height &&
                        blackPlayer == other.blackPlayer &&
                        whitePlayer == other.whitePlayer &&
                        gameInfo == other.gameInfo &&
                        flags == other.flags
                )
    }

    override fun hashCode(): Int {
        return (((((width*31 +
                height)*31 +
                blackPlayer.hashCode())*31 +
                whitePlayer.hashCode())*31 +
                isRandomPlayer.hashCode())*31 +
                gameInfo.hashCode())*31 +
                isFreeHandicap.hashCode()
    }

    override fun toString(): String {
        val width = this.width
        val height = this.height
        return buildString {
            append("GoGameSetup(")
            if (width == height)
                append("size=")
            else append("width=").append(width).append("height=")
            append(height)
            if (isRandomPlayer)
                append(", player1=").append(player1).append(", player2=").append(player2)
                    .append(", isRandomPlayer=true, gameInfo=")
            else append(", blackPlayer=").append(blackPlayer).append(", whitePlayer=").append(whitePlayer)
                .append(", isRandomPlayer=false, gameInfo=")
            append(gameInfo).append(", isFreeHandicap=").append(isFreeHandicap).append(")")
        }
    }

}