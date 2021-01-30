package com.computernerd1101.goban.resources

import com.computernerd1101.goban.GoColor
import com.computernerd1101.goban.GoPoint
import com.computernerd1101.goban.Superko as SuperkoRule

fun interface GoSGFResumeExceptionFormat {

    fun format(vararg args: Any?): String

    companion object {

        @JvmField val InaccurateHandicap: GoSGFResumeExceptionFormat = Default.InaccurateHandicap
        @JvmField val OverrideStone: GoSGFResumeExceptionFormat = Default.OverrideStone
        @JvmField val Superko: GoSGFResumeExceptionFormat = Default.Superko

    }

    private enum class Default: GoSGFResumeExceptionFormat {
        InaccurateHandicap {

            override fun format(vararg args: Any?) = buildString {
                val expectedBlack = args[0] as Int
                val actualBlack = args[1] as Int
                val actualWhite = args[2] as Int
                append("Expected ").append(expectedBlack).append(" black handicap stones")
                if (actualWhite != 0)
                    append(" and 0 white stones")
                append("; found ").append(actualBlack).append(" black stones")
                if (actualWhite != 0)
                    append(" and ").append(actualWhite).append(" white stones")
            }

        },
        OverrideStone {
            override fun format(vararg args: Any?): String {
                val point = GoPoint.formatPoint(args[0] as GoPoint?, args[1] as Int, args[2] as Int)
                return "There is already a stone at $point"
            }
        },
        Superko {

            override fun format(vararg args: Any?): String {
                val rule = args[0] as SuperkoRule
                val player = args[1] as GoColor?
                if (player == null || rule == SuperkoRule.POSITIONAL)
                    return "The positional superko rule forbids the board position to repeat."
                return buildString {
                    val natural = rule == SuperkoRule.NATURAL
                    append(player).append(" has violated the ")
                    if (natural)
                        append("natural ")
                    append("situational superko rule by repeating the same board position from one of their ")
                    if (natural)
                        append("non-pass ")
                    append("moves.")
                }
            }

        }
    }

}