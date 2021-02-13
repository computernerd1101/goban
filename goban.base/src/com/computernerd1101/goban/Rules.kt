@file:Suppress("unused")
@file:JvmMultifileClass
@file:JvmName("GobanKt")

package com.computernerd1101.goban

import com.computernerd1101.goban.resources.gobanResources
import java.util.*

enum class Superko {

    NATURAL,
    SITUATIONAL,
    POSITIONAL;

    override fun toString(): String = toString(Locale.getDefault())

    fun toString(locale: Locale): String {
        val resources = gobanResources(locale)
        return resources.getString("Superko.$name")
    }

}

fun GoRules(string: String) = GoRules.parse(string)

fun GoRules(superko: Superko) = GoRules.valueOf(superko)

fun GoRules(
    superko: Superko,
    territoryScore: Boolean
) = GoRules.valueOf(superko, territoryScore)

fun GoRules(
    superko: Superko,
    territoryScore: Boolean,
    allowSuicide: Boolean
) = GoRules.valueOf(superko, territoryScore, allowSuicide)

fun GoRules() = GoRules.DEFAULT

enum class GoRules(
    @get:JvmName("superko")
    val superko: Superko,
    private val string: String
) {

    DEFAULT(Superko.NATURAL, ""), // area
    SUICIDE(Superko.NATURAL, "Suicide"), // area, allow suicide
    JAPANESE(Superko.NATURAL, "Japanese"), // territory
    JAPANESE_SUICIDE(Superko.NATURAL, "Japanese:Suicide"), // territory, allow suicide
    AGA(Superko.SITUATIONAL, "AGA"), // area
    NEW_ZEALAND(Superko.SITUATIONAL, "NZ"), // area, allow suicide
    JAPANESE_SSK(Superko.SITUATIONAL, "Japanese:SSK"), // territory
    JAPANESE_SUICIDE_SSK(Superko.SITUATIONAL, "Japanese:Suicide:SSK"), // territory, allow suicide
    PSK(Superko.POSITIONAL, "PSK"), // area
    ING(Superko.POSITIONAL, "Goe"), // area, allow suicide
    JAPANESE_PSK(Superko.POSITIONAL, "Japanese:PSK"), // territory
    JAPANESE_SUICIDE_PSK(Superko.POSITIONAL, "Japanese:Suicide:PSK"); // territory, allow suicide

    val territoryScore: Boolean
        @JvmName("territoryScore")
        get() = ordinal and 2 != 0

    val allowSuicide: Boolean
        @JvmName("allowSuicide")
        get() = ordinal and 1 != 0

    override fun toString() = string

    operator fun component1() = superko
    operator fun component2() = territoryScore
    operator fun component3() = allowSuicide

    fun copy(
        superko: Superko = this.superko,
        territoryScore: Boolean = this.territoryScore,
        allowSuicide: Boolean = this.allowSuicide,
        string: String? = null
    ): GoRules {
        var ko = superko
        var setKo = false
        var territory = territoryScore
        var suicide = allowSuicide
        var preset = false
        if (string != null) for(part in string.split(':')) when(part.toUpperCase()) {
            "JAPANESE" -> if (!preset) {
                territory = true
                preset = true
            }
            "AGA" -> if (!preset) {
                if (!setKo) ko = Superko.SITUATIONAL
                preset = true
            }
            "NZ" -> if (!preset) {
                if (!setKo) ko = Superko.SITUATIONAL
                suicide = true
                preset = true
            }
            "GOE", "ING" -> if (!preset) {
                if (!setKo) ko = Superko.POSITIONAL
                suicide = true
                preset = true
            }
            "SSK" -> if (!setKo) {
                ko = Superko.SITUATIONAL
                setKo = true
            }
            "PSK" -> if (!setKo) {
                ko = Superko.POSITIONAL
                setKo = true
            }
            "SUICIDE" -> suicide = true
        }
        return valueOf(ko, territory, suicide)
    }

    companion object {

        @JvmStatic
        fun parse(string: String): GoRules {
            return DEFAULT.copy(string = string)
        }

        @JvmStatic
        fun valueOf(superko: Superko): GoRules {
            return Private.VALUES[superko.ordinal*4]
        }

        @JvmStatic
        fun valueOf(superko: Superko, territoryScore: Boolean): GoRules {
            return Private.VALUES[superko.ordinal*4 + (if (territoryScore) 2 else 0)]
        }

        @JvmStatic
        fun valueOf(
            superko: Superko,
            territoryScore: Boolean,
            allowSuicide: Boolean
        ): GoRules {
            return Private.VALUES[superko.ordinal*4 + (if (territoryScore) 2 else 0) + (if (allowSuicide) 1 else 0)]
        }

    }

    private object Private {

        @JvmField val VALUES = enumValues<GoRules>()

    }

}