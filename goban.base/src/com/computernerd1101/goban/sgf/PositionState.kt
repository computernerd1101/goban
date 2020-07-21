package com.computernerd1101.goban.sgf

import com.computernerd1101.goban.resources.GobanResources
import java.util.*

enum class PositionState(
    @get:JvmName("code") val code: String,
    @get:JvmName("extent") val extent: Int
) {

    UNCLEAR("UC", 1),
    VERY_UNCLEAR("UC", 2),
    EVEN("DM", 1),
    EVEN_JOSEKI("DM", 2),
    GOOD_FOR_BLACK("GB", 1),
    VERY_GOOD_FOR_BLACK("GB", 2),
    GOOD_FOR_WHITE("GW", 1),
    VERY_GOOD_FOR_WHITE("GW", 2);

    override fun toString() = toString(Locale.getDefault())

    fun toString(locale: Locale): String {
        val resources = GobanResources.getBundle(locale)
        return resources.getStringArray("sgf.PositionState.$code")[extent - 1]
    }

    fun toExtent(extent: Int): PositionState {
        val x = if (extent <= 1) 1 else 2
        return if (x == this.extent) this
        else positionStateMap.getValue(code)[x - 1]
    }

    companion object {

        @JvmStatic
        fun valueOf(code: String, extent: Int): PositionState? {
            return positionStateMap[code]?.get(if (extent <= 1) 0 else 1)
        }

        private val positionStateMap = mapOf(
            "UC" to arrayOf(UNCLEAR, VERY_UNCLEAR),
            "DM" to arrayOf(EVEN, EVEN_JOSEKI),
            "GB" to arrayOf(GOOD_FOR_BLACK, VERY_GOOD_FOR_BLACK),
            "GW" to arrayOf(GOOD_FOR_WHITE, VERY_GOOD_FOR_WHITE)
        )

    }

}