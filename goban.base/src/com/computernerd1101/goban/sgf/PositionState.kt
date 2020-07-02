package com.computernerd1101.goban.sgf

enum class PositionState(
    @get:JvmName("code") val code: String,
    @get:JvmName("extent") val extent: Int,
    private val string: String) {

    UNCLEAR("UC", 1, "Unclear position"),
    VERY_UNCLEAR("UC", 2, "Very unclear position"),
    EVEN("DM", 1, "Even position"),
    EVEN_JOSEKI("DM", 2, "Joseki"),
    GOOD_FOR_BLACK("GB", 1, "Good for black"),
    VERY_GOOD_FOR_BLACK("GB", 2, "Very good for black"),
    GOOD_FOR_WHITE("GW", 1, "Good for white"),
    VERY_GOOD_FOR_WHITE("GW", 2, "Very good for white");

    override fun toString() = string

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