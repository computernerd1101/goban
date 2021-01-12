@file:JvmName("MarkupKt")
@file:JvmMultifileClass

package com.computernerd1101.goban.markup

import com.computernerd1101.goban.GoPoint
import com.computernerd1101.goban.MutableGoPointMap
import java.io.*

class PointMarkup: Comparable<PointMarkup>, Serializable {

    companion object {

        @JvmField
        val EMPTY_LABEL = PointMarkup("", Private)
        @JvmField
        val SELECT = PointMarkup(MarkupEnum.SL)
        @JvmField
        val X = PointMarkup(MarkupEnum.MA)
        @JvmField
        val TRIANGLE = PointMarkup(MarkupEnum.TR)
        @JvmField
        val CIRCLE = PointMarkup(MarkupEnum.CR)
        @JvmField
        val SQUARE = PointMarkup(MarkupEnum.SQ)

        const val TYPES = 6

        @JvmStatic
        @Suppress("unused")
        val VALUES: Array<PointMarkup>
            @JvmName("values")
            get() = Private.VALUES.clone()

        @JvmStatic
        fun label(label: String): PointMarkup {
            var trim = label.trim()
            val lineEnd = label.indexOf('\n')
            if (lineEnd > 0)
                trim = trim.substring(0, lineEnd).trim()
            return if (trim.isEmpty()) EMPTY_LABEL else PointMarkup(trim, Private)
        }

        @JvmStatic
        @Suppress("unused")
        fun type(type: String): PointMarkup {
            return try {
                enumValueOf<MarkupEnum>(type).value
            } catch(e: RuntimeException) {
                EMPTY_LABEL
            }
        }

        @JvmStatic
        fun ordinal(ordinal: Int): PointMarkup {
            return Private.VALUES[ordinal]
        }

        private const val serialVersionUID: Long = 1L

    }

    private object Private {

        fun ignore() = Unit

        @JvmField val VALUES = arrayOf(EMPTY_LABEL, SELECT, X, TRIANGLE, CIRCLE, SQUARE)

    }

    private enum class MarkupEnum {

        SL, MA, TR, CR, SQ;

        lateinit var value: PointMarkup
    }

    @get:JvmName("type")
    val type: String get() = enumType?.name ?: "LB"

    @get:JvmName("ordinal")
    val ordinal: Int get() = enumType?.ordinal?.plus(1) ?: 0

    private var enumType: MarkupEnum?

    @get:JvmName("label")
    var label: String private set

    private constructor(type: MarkupEnum) {
        this.enumType = type
        label = ""
        type.value = this
    }

    private constructor(label: String, marker: Private) {
        marker.ignore()
        enumType = null
        this.label = label
    }

    override fun compareTo(other: PointMarkup) : Int {
        val type1 = enumType
        val type2 = other.enumType
        return when {
            type1 == null -> if (type2 == null) label.compareTo(other.label) else -1
            type2 == null -> 1
            else -> type1.compareTo(type2)
        }
    }

    override fun equals(other: Any?): Boolean {
        return this === other ||
                (other is PointMarkup &&
                        if (enumType == null) other.enumType == null && label == other.label
                        else enumType == other.enumType)
    }

    override fun hashCode(): Int {
        return enumType?.hashCode() ?: label.hashCode()
    }

    override fun toString(): String {
        return enumType?.name ?: if (label.isEmpty()) "LB" else "LB[$label]"
    }

    private fun writeObject(out: ObjectOutputStream) {
        out.writeUTF(type)
        if (enumType == null) out.writeUTF(label)
    }

    private fun readObject(input: ObjectInputStream) {
        enumType = when(val type = input.readUTF()) {
            "LB" -> {
                val trim = input.readUTF().trim()
                val lineEnd = label.indexOf('\n')
                label = if (lineEnd <= 0) trim
                else trim.substring(0, lineEnd).trim()
                null
            }
            else -> try {
                enumValueOf<MarkupEnum>(type)
            } catch(e: RuntimeException) {
                throw InvalidObjectException("Invalid markup type: $type")
            }
        }
    }

    private fun readResolve(): Any {
        return enumType?.value ?: if (label.isEmpty()) EMPTY_LABEL else this
    }

}

class PointMarkupMap: MutableGoPointMap<PointMarkup> {

    constructor()

    @Suppress("unused")
    constructor(vararg entries: Pair<GoPoint, PointMarkup>): super(*entries)

    @Suppress("unused")
    constructor(vararg entries: Map.Entry<GoPoint, PointMarkup>): super(*entries)

    override fun isValidValue(value: Any?, throwIfInvalid: Boolean) = if (throwIfInvalid) {
        (value as PointMarkup)
        true
    } else value is PointMarkup

}
