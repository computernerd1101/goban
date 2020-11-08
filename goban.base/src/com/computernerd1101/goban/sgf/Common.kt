@file:Suppress("FunctionName", "NOTHING_TO_INLINE")

package com.computernerd1101.goban.sgf

import com.computernerd1101.goban.*
import com.computernerd1101.sgf.*
import java.nio.charset.Charset
import kotlin.contracts.*

inline fun GameResult(winner: GoColor?, score: Float): GameResult {
    return GameResult.result(winner, score)
}

@Suppress("unused")
fun GameResult(s: String): GameResult {
    return nullableGameResult(s) ?: GameResult.UNKNOWN
}

inline fun nullableGameResult(s: String) = GameResult.parse(s)

@Suppress("unused")
inline fun nullableMoveAnnotation(code: String, extent: Int) = MoveAnnotation.valueOf(code, extent)

@Suppress("unused")
inline fun nullablePositionState(code: String, extent: Int) = PositionState.valueOf(code, extent)

@Suppress("unused")
@OptIn(ExperimentalContracts::class)
inline fun GameInfo.Player?.isNullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@isNullOrEmpty != null)
    }
    return this?.isEmpty() != false
}

fun foobar(info: GameInfo, color: GoColor, player: GameInfo.Player) {
    info.player[color] = player
}
