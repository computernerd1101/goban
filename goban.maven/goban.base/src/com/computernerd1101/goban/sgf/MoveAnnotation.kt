package com.computernerd1101.goban.sgf

import com.computernerd1101.goban.resources.gobanResources
import java.util.*

enum class MoveAnnotation(
    @get:JvmName("code") val code: String,
    @get:JvmName("extent") val extent: Int
) {

    INTERESTING("IT", 0),
    GOOD("TE", 1),
    VERY_GOOD("TE", 2),
    DOUBTFUL("DO", 0),
    BAD("BM", 1),
    VERY_BAD("BM", 2);

    override fun toString() = toString(Locale.getDefault())

    fun toString(locale: Locale): String {
        val resources = gobanResources(locale)
        val key = "sgf.MoveAnnotation.$code"
        return when(val extent = this.extent) {
            0 -> resources.getString(key)
            else -> resources.getStringArray(key)[extent - 1]
        }
    }

    fun toExtent(extent: Int): MoveAnnotation {
        if (this.extent == 0) return this
        val x = if (extent <= 1) 1 else 2
        return if (this.extent == x) this
        else moveAnnotationMap.getValue(code)[x - 1]
    }

    companion object {

        @Suppress("unused")
        @JvmStatic
        @JvmName("valueOf")
        fun nullable(code: String, extent: Int): MoveAnnotation? {
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