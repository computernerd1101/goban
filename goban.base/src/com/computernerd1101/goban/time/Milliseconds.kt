package com.computernerd1101.goban.time

import java.util.regex.Matcher
import java.util.regex.Pattern

@Suppress("unused")
class Milliseconds private constructor(private val value: Long, private val extra: Int):
    Number(), Comparable<Milliseconds> {

    companion object {

        private val MINUS_ZERO = Array(4) { Milliseconds(0, -1 - it) }
        private val  PLUS_ZERO = Array(4) { Milliseconds(0, it) }

        @JvmField val MINUS_SIGN = MINUS_ZERO[0]
        @JvmField val       ZERO =  PLUS_ZERO[0]

        @JvmStatic
        fun zeroPoint(negative: Boolean = false, zeros: Int): Milliseconds {
            return (if (negative) MINUS_ZERO else PLUS_ZERO)[when {
                zeros < 0 -> 1
                zeros > 2 -> 3
                else -> zeros + 1
            }]
        }

        @JvmStatic fun withTrailingZeros(millis: Long,  zeros: Int) = valueOf(millis,  zeros,  true)
        @JvmStatic fun withDecimalPlaces(millis: Long, digits: Int) = valueOf(millis, digits, false)

        @JvmStatic
        fun valueOf(value: Long) = if (value == 0L) ZERO else Milliseconds(value, 0)

        private fun valueOf(value: Long, digits: Int, zeros: Boolean): Milliseconds {
            var realDigits = when {
                digits < 0 -> 0
                digits > 2 -> 2
                else -> digits
            }
            if (value == 0L) return PLUS_ZERO[realDigits + 1]
            var millis = value % 1000
            if (millis < 0) millis = -millis
            var decimalPoint = 0
            var nonZeroDigits = 0
            when {
                millis == 0L -> decimalPoint = 1
                millis % 100 == 0L -> nonZeroDigits = 1
                millis % 10 == 0L -> nonZeroDigits = 2
                else -> nonZeroDigits = 3
            }
            if (!zeros) {
                realDigits -= nonZeroDigits
                if (realDigits < 0) realDigits = 0
            } else if (realDigits + nonZeroDigits > 2) {
                realDigits = 2 - nonZeroDigits
                if (realDigits < 0) realDigits = 0
            }
            return Milliseconds(value, realDigits + decimalPoint)
        }

        @JvmStatic
        fun parse(s: String): Milliseconds {
            val m: Matcher = PATTERN.matcher(s)
            if (!m.find()) throw NumberFormatException("For input string: \"$s\"")
            val s1: String = m.group(GROUP_SIGN)
            val s2: String = m.group(GROUP_IPART)
            var s3: String? = m.group(GROUP_FPART)
            var s4: String? = null
            var extra = 0
            when (s3) {
                null -> s3 = "000"
                "." -> {
                    extra = 1
                    s3 = "000"
                }
                ".0" -> {
                    extra = 2
                    s3 = "000"
                }
                ".00", ".000" -> {
                    extra = 3
                    s3 = "000"
                }
                else -> {
                    s3 = s3.substring(1)
                    var i = s3.length - 1
                    while (i >= 0) {
                        val ch = s3[i]
                        if (ch != '0') break
                        extra++
                        i--
                    }
                    when (s3.length) {
                        1 -> s4 = "00"
                        2 -> s4 = "0"
                        3 -> if (extra > 0) extra--
                    }
                }
            }
            if (s4 == null) s4 = ""
            val value: Long
            value = try {
                (s1 + s2 + s3 + s4).toLong()
            } catch (e: NumberFormatException) {
                throw NumberFormatException("For input string: \"$s\"")
            }
            if (value == 0L && s1 == "-") extra = -1 - extra
            return Milliseconds(value, extra)
        }

        private const val GROUP_SIGN = 1
        private const val GROUP_IPART = 2
        private const val GROUP_FPART = 3
        private val PATTERN = Pattern.compile(
            "([+\\-]?)(\\d*)(\\.\\d{0,3})?"
        )

    }

    operator fun compareTo(other:         Byte) = value.compareTo(other)
    operator fun compareTo(other:        Short) = value.compareTo(other)
    operator fun compareTo(other:          Int) = value.compareTo(other)
    operator fun compareTo(other:         Long) = value.compareTo(other)
    operator fun compareTo(other:        Float) = value.compareTo(other)
    operator fun compareTo(other:       Double) = value.compareTo(other)
    @ExperimentalUnsignedTypes
    operator fun compareTo(other:        UByte) = value.compareTo(other.toLong())
    @ExperimentalUnsignedTypes
    operator fun compareTo(other:       UShort) = value.compareTo(other.toLong())
    @ExperimentalUnsignedTypes
    operator fun compareTo(other:         UInt) = value.compareTo(other.toLong())
    @ExperimentalUnsignedTypes
    operator fun compareTo(other:        ULong) = if (value < 0) -1 else toULong().compareTo(other)
    override fun compareTo(other: Milliseconds) = value.compareTo(other.value)

    override                   fun   toChar() = value.toChar()
    override                   fun   toByte() = value.toByte()
    override                   fun  toShort() = value.toShort()
    override                   fun    toInt() = value.toInt()
    override                   fun   toLong() = value
    override                   fun  toFloat() = value.toFloat()
    override                   fun toDouble() = value.toDouble()
    @ExperimentalUnsignedTypes fun  toUByte() = value.toUByte()
    @ExperimentalUnsignedTypes fun toUShort() = value.toUShort()
    @ExperimentalUnsignedTypes fun   toUInt() = value.toUInt()
    @ExperimentalUnsignedTypes fun  toULong() = value.toULong()

    override fun equals(other: Any?) = this === other ||
            (other is Milliseconds && value == other.value && extra == other.extra)

    override fun hashCode(): Int {
        var hash = value*4
        var ex = extra
        if (value < 0) {
            hash--
            ex = -ex
        }
        hash += ex
        return (hash + hash.shr(32)).toInt()
    }

    override fun toString(): String {
        val withPoint: String
        val withoutPoint: String
        when(extra) {
            -4 -> return "-0.00"
            -3 -> return "-0.0"
            -2 -> return "-0."
            -1 -> return "-"
            0 -> {
                withPoint = ""
                withoutPoint = ""
            }
            1 -> {
                withPoint = "."
                withoutPoint = "0"
            }
            2 -> {
                withPoint = ".0"
                withoutPoint = "00"
            }
            else -> {
                withPoint = ".00"
                withoutPoint = ""
            }
        }
        return value.millisToStringSeconds() + if (value % 1000 == 0L) withPoint else withoutPoint
    }

}