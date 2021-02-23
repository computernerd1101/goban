package com.computernerd1101.goban.players

import com.computernerd1101.goban.*
import com.computernerd1101.goban.sgf.*
import com.computernerd1101.goban.sgf.internal.InternalGoSGF.violatesSituationalSuperko


fun GoSGF.resumeNode(): GoSGFNode = synchronized(this) {
    val root = rootNode
    var children = root.children
    val handicapNode: GoSGFSetupNode?
    val handicap: Int
    if (children != 1) {
        handicapNode = null
        handicap = 0
    }
    else {
        val node = root.child(0) as? GoSGFSetupNode
        if (node == null) {
            handicapNode = null
            handicap = 0
        }
        else {
            val goban = node.goban
            val blackCount = goban.blackCount
            if (blackCount <= 1 || goban.whiteCount != 0 || goban.emptyCount == 0) {
                handicapNode = null
                handicap = 0
            } else {
                handicapNode = node
                handicap = blackCount
            }
        }
    }
    val node: GoSGFSetupNode
    val firstPlayer: GoColor
    if (handicapNode != null) {
        node = handicapNode
        firstPlayer = GoColor.WHITE
    } else {
        node = root
        firstPlayer = GoColor.BLACK
    }
    val repetitions = mutableMapOf<GoSGFMoveNode, GoSGFMoveNode>()
    TODO()
}

fun GoSGF.tryResume(): GoSGFResumeException? = try {
    onResume()
    null
} catch(e: GoSGFResumeException) {
    e
}

@Throws(GoSGFResumeException::class)
fun GoSGF.onResume(): GameInfo = synchronized(this) {
    val root = rootNode
    var children = root.children
    val handicapNode = if (children == 1) root.child(0) as? GoSGFSetupNode else null
    val node: GoSGFSetupNode
    val handicap: Int
    val firstPlayer: GoColor
    if (handicapNode != null) {
        node = handicapNode
        val goban = handicapNode.goban
        handicap = goban.blackCount
        if (goban.emptyCount == 0 || goban.whiteCount != 0 || handicap <= 1)
            throw GoSGFResumeException.LateSetup(handicapNode)
        children = handicapNode.children
        firstPlayer = GoColor.WHITE
    } else {
        node = root
        handicap = 0
        firstPlayer = GoColor.BLACK
    }
    val info = findGameInfo(handicap)
    val rules = info.rules
    val repetitions: MutableMap<GoSGFMoveNode, GoSGFMoveNode>? =
        if (rules.superko == Superko.POSITIONAL) null
        else mutableMapOf()
    for(i in 0 until children)
        node.child(i).onResume(firstPlayer, rules, repetitions)
    info.handicap = handicap
    root.gameInfo = info
    root.turnPlayer = null
    handicapNode?.turnPlayer = GoColor.WHITE
    info
}

private fun GoSGF.findGameInfo(targetHandicap: Int): GameInfo {
    val root = rootNode
    var info1: GameInfo? = null
    val first = root.nextGameInfoNode
    var info = first.gameInfo
    if (info == null)
        return GameInfo()
    var handicap = info.handicap
    if (handicap == targetHandicap)
        return info
    if (handicap == 1 && targetHandicap == 0)
        info1 = info
    var next = first.nextGameInfoNode
    while (next != first) {
        info = next.gameInfo
        if (info != null) {
            handicap = info.handicap
            if (handicap == targetHandicap)
                return info
            if (handicap == 1 && targetHandicap == 0 && info1 == null)
                info1 = info
        }
        next = next.nextGameInfoNode
    }
    return info1 ?: GameInfo()
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
                        if (violatesSituationalSuperko(node.turnPlayer, previousNode, isNaturalSuperko))
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
