package com.computernerd1101.goban.players

import com.computernerd1101.goban.*
import com.computernerd1101.goban.sgf.*
import com.computernerd1101.goban.sgf.internal.InternalGoSGF.violatesSituationalSuperko

fun GoSGF.onResume(): GoSGFResumeException? = synchronized(this) {
    val root = rootNode
    var children = root.children
    if (children == 0) return@synchronized null
    val info = root.gameInfo
    val handicap = info?.handicap ?: 0
    if (handicap == 1 || handicap < 0)
        return@synchronized GoSGFResumeException.InvalidHandicap(root)
    val rules = info?.rules ?: GoRules.DEFAULT
    val repetitions: MutableMap<GoSGFMoveNode, GoSGFMoveNode>? =
        if (rules.superko == Superko.POSITIONAL) null
        else mutableMapOf()
    if (handicap == 0) {
        if (root.turnPlayer == GoColor.WHITE)
            return@synchronized GoSGFResumeException.FirstPlayer(root)
        try {
            root.onResume(GoColor.BLACK, rules, repetitions)
        } catch(e: GoSGFResumeException) {
            return@synchronized e
        }
    } else {
        val turnPlayer = root.turnPlayer
        val node = root.child(0)
        if (node !is GoSGFSetupNode)
            return@synchronized GoSGFResumeException.InaccurateHandicap(node, handicap, 0, 0)
        if (node.gameInfoNode == node)
            return@synchronized GoSGFResumeException.LateGameInfo(node)
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (node.turnPlayer) {
            null -> if (turnPlayer == GoColor.BLACK)
                return@synchronized GoSGFResumeException.FirstPlayerAfterHandicap(root)
            GoColor.BLACK -> return GoSGFResumeException.FirstPlayerAfterHandicap(node)
        }
        val goban = node.goban
        if (goban.emptyCount == 0 || goban.whiteCount != 0 || goban.blackCount != handicap)
            return@synchronized GoSGFResumeException.InaccurateHandicap(
                node,
                handicap,
                goban.blackCount,
                goban.whiteCount
            )
        if (children > 1) {
            val extra = root.child(1)
            if (extra is GoSGFSetupNode)
                return@synchronized GoSGFResumeException.LateSetup(extra)
            return@synchronized GoSGFResumeException.InaccurateHandicap(extra, handicap, 0, 0)
        }
        children = node.children
        try {
            for (j in 0 until children)
                node.child(j).onResume(GoColor.WHITE, rules, repetitions)
        } catch (e: GoSGFResumeException) {
            return@synchronized e
        }
    }
    return@synchronized null
}

private fun GoSGFNode.onResume(
    player: GoColor,
    rules: GoRules,
    repetitions: MutableMap<GoSGFMoveNode, GoSGFMoveNode>?
) {
    var node = this as? GoSGFMoveNode ?: throw GoSGFResumeException.LateSetup(this as GoSGFSetupNode)
    var nextPlayer = player
    val superko = rules.superko
    val isNaturalSuperko = repetitions != null && superko == Superko.NATURAL
    val allowSuicide = rules.allowSuicide
    var children: Int
    while(true) {
        if (node.gameInfoNode == node)
            throw GoSGFResumeException.LateGameInfo(node)
        val isForced = node.isForced
        if (!isForced) {
            if (nextPlayer != node.turnPlayer)
                throw GoSGFResumeException.PlayerColor(node)
            node.playStoneAt?.let { point ->
                val parentGoban = node.parent!!.goban
                if (parentGoban[point] != null)
                    throw GoSGFResumeException.OverrideStone(node)
                val goban = node.goban
                if (parentGoban == goban)
                    throw GoSGFResumeException.SingleStoneSuicide(node)
                if (!allowSuicide && goban[point] == null)
                    throw GoSGFResumeException.MultiStoneSuicide(node)
            }
            nextPlayer = nextPlayer.opponent
        } else if (nextPlayer == node.turnPlayer)
            nextPlayer = nextPlayer.opponent
        if (!isForced || repetitions != null) {
            var previousNode = node.parent as? GoSGFMoveNode
            while (previousNode != null) {
                if (previousNode.goban == node.goban) {
                    if (repetitions == null)
                        throw GoSGFResumeException.Superko(node, previousNode, Superko.POSITIONAL)
                    repetitions[node] = previousNode
                    if (!isForced) while (previousNode != null) {
                        if (violatesSituationalSuperko(node, previousNode, isNaturalSuperko))
                            throw GoSGFResumeException.Superko(node, previousNode, superko)
                        previousNode = repetitions[previousNode]
                    }
                    break
                }
                previousNode = previousNode.parent as? GoSGFMoveNode
            }
        }
        children = node.children
        if (children != 1) break
        val child = node.child(0)
        node = child as? GoSGFMoveNode ?: throw GoSGFResumeException.LateSetup(child as GoSGFSetupNode)
    }
    for(i in 0 until children)
        node.child(i).onResume(nextPlayer, rules, repetitions)
}
