package com.computernerd1101.goban.players

import com.computernerd1101.goban.GoColor
import com.computernerd1101.goban.Superko as SuperkoRule
import com.computernerd1101.goban.resources.*
import com.computernerd1101.goban.sgf.*

sealed class GoSGFResumeException(
    open val node: GoSGFNode,
    message: String
): Exception(message) {

    constructor(
        type: Class<out GoSGFResumeException>,
        node: GoSGFNode
    ): this(node, gobanResources().getString("players.GoSGFResumeException." + type.simpleName))

    constructor(
        node: GoSGFNode,
        type: Class<out GoSGFResumeException>,
        vararg formatArgs: Any?
    ): this(node, (gobanFormatResources().getObject("players.GoSGFResumeException.${type.simpleName}.Format")
                    as GoSGFResumeExceptionFormat).format(*formatArgs))

    class FirstPlayer(node: GoSGFNode): GoSGFResumeException(FirstPlayer::class.java, node)

    class FirstPlayerAfterHandicap(node: GoSGFNode):
        GoSGFResumeException(FirstPlayerAfterHandicap::class.java, node)

    class InvalidHandicap(node: GoSGFNode): GoSGFResumeException(InvalidHandicap::class.java, node)

    @Suppress("unused", "CanBeParameter")
    class InaccurateHandicap(
        node: GoSGFNode,
        val expectedBlack: Int,
        val actualBlack: Int,
        val actualWhite: Int
    ): GoSGFResumeException(node, InaccurateHandicap::class.java, expectedBlack, actualBlack, actualWhite)

    class LateGameInfo(node: GoSGFNode): GoSGFResumeException(LateGameInfo::class.java, node)

    class LateSetup(node: GoSGFSetupNode): GoSGFResumeException(LateSetup::class.java, node) {

        override val node: GoSGFSetupNode get() = super.node as GoSGFSetupNode

    }

    class PlayerColor private constructor(
        node: GoSGFMoveNode,
        @Suppress("unused")
        val expectedPlayerColor: GoColor
    ): GoSGFResumeException(node, PlayerColor::class.java, expectedPlayerColor) {

        override val node: GoSGFMoveNode get() = super.node as GoSGFMoveNode

        constructor(node: GoSGFMoveNode): this(node, node.turnPlayer.opponent)

    }

    class OverrideStone(
        node: GoSGFMoveNode
    ): GoSGFResumeException(node, OverrideStone::class.java, node.playStoneAt, node.goban.width, node.goban.height) {

        override val node: GoSGFMoveNode get() = super.node as GoSGFMoveNode

    }

    class SingleStoneSuicide(node: GoSGFMoveNode): GoSGFResumeException(SingleStoneSuicide::class.java, node) {

        override val node: GoSGFMoveNode get() = super.node as GoSGFMoveNode

    }

    class MultiStoneSuicide(node: GoSGFMoveNode): GoSGFResumeException(MultiStoneSuicide::class.java, node) {

        override val node: GoSGFMoveNode get() = super.node as GoSGFMoveNode

    }

    class Superko private constructor(
        node: GoSGFMoveNode,
        @Suppress("unused")
        val previousNode: GoSGFMoveNode,
        val rule: SuperkoRule,
        val player: GoColor?
    ): GoSGFResumeException(node, Superko::class.java, rule, player) {

        constructor(node: GoSGFMoveNode, previousNode: GoSGFMoveNode, rule: SuperkoRule):
                this(node, previousNode,
                    when {
                        rule == SuperkoRule.POSITIONAL || node.turnPlayer != previousNode.turnPlayer ->
                            SuperkoRule.POSITIONAL
                        rule == SuperkoRule.SITUATIONAL || previousNode.playStoneAt != null ->
                            SuperkoRule.SITUATIONAL
                        else -> SuperkoRule.NATURAL
                    },
                    if (rule == SuperkoRule.POSITIONAL) null
                    else when(val player = node.turnPlayer) {
                        previousNode.turnPlayer -> player
                        else -> null
                    }
                )

        override val node: GoSGFMoveNode get() = super.node as GoSGFMoveNode

    }


}