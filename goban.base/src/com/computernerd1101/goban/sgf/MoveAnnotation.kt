package com.computernerd1101.goban.sgf

enum class MoveAnnotation(
    @get:JvmName("code") val code: String,
    @get:JvmName("extent") val extent: Int,
    private val string: String
) {

    INTERESTING("IT", 0, "Interesting"),
    GOOD("TE", 1, "Good move (Tesuji)"),
    VERY_GOOD("TE", 2, "Very good move (Tesuji)"),
    DOUBTFUL("DO", 0, "Doubtful"),
    BAD("BM", 1, "Bad move"),
    VERY_BAD("BM", 2, "Very bad move");

    override fun toString() = string

    fun toExtent(extent: Int): MoveAnnotation {
        if (this.extent == 0) return this
        val x = if (extent <= 1) 1 else 2
        return if (this.extent == x) this
        else moveAnnotationMap.getValue(code)[x - 1]
    }

    companion object {

        @JvmStatic
        fun valueOf(code: String, extent: Int): MoveAnnotation? {
            val values = moveAnnotationMap[code] ?: return null
            return values[if (values.size == 1 || extent <= 1) 0 else 1]
        }

        private val moveAnnotationMap = mapOf(
            "IT" to arrayOf(INTERESTING),
            "TE" to arrayOf(GOOD, VERY_GOOD),
            "DO" to arrayOf(DOUBTFUL),
            "BM" to arrayOf(BAD, VERY_BAD)
        )

    }

}