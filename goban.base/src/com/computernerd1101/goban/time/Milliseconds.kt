@file:Suppress("unused", "NOTHING_TO_INLINE")

package com.computernerd1101.goban.time

import com.computernerd1101.goban.internal.unsafeArrayOfNulls
import java.io.*

class Milliseconds private constructor(
    private val value: Long,
    private var extra: Int,
    cache: Private
): Number(), Comparable<Milliseconds>, Serializable {

    init {
        if (value == 0L) {
            if (extra < 0)
                cache.MINUS_ZERO[-1 - extra] = this
            else
                cache.PLUS_ZERO[extra] = this
        }
    }

    @Suppress("SpellCheckingInspection", "UNCHECKED_CAST")
    private object Private {

        const val GROUP_IPART = 1
        const val GROUP_FPART = 2

        @JvmField val REGEX = """([+\-]?\d*)(\.\d{0,3})?""".toRegex()

        @JvmField val MINUS_ZERO = unsafeArrayOfNulls<Milliseconds>(4)
        @JvmField val  PLUS_ZERO = unsafeArrayOfNulls<Milliseconds>(4)

        fun ignore() = Unit

    }

    companion object {

        @JvmField val MINUS_SIGN = Milliseconds(0, -1, Private)
        @JvmField val       ZERO = Milliseconds(0, 0, Private)

        init {
            for(i in 1..3) {
                Milliseconds(0, -1 - i, Private)
                Milliseconds(0, i, Private)
            }
        }

        @JvmStatic
        @JvmOverloads
        fun zeroPoint(zeros: Int, negative: Boolean = false): Milliseconds {
            return (if (negative) Private.MINUS_ZERO else Private.PLUS_ZERO)[when {
                zeros < 0 -> 1
                zeros > 2 -> 3
                else -> zeros + 1
            }]
        }

        @JvmStatic fun withTrailingZeros(millis: Long,  zeros: Int) = valueOf(millis,  zeros, zeros =  true)
        @JvmStatic fun withDecimalPlaces(millis: Long, digits: Int) = valueOf(millis, digits, zeros = false)

        @JvmStatic
        fun valueOf(value: Long) = if (value == 0L) ZERO else Milliseconds(value, 0, Private)

        private fun valueOf(value: Long, digits: Int, zeros: Boolean): Milliseconds {
            var extra = when {
                digits < 0 -> 0
                digits > 2 -> 2
                else -> digits
            }
            if (value == 0L) return Private.PLUS_ZERO[extra + 1]
            var millis = value % 1000L
            if (millis < 0) millis = -millis
            val nonZeroDigits = when {
                millis == 0L -> 0
                millis % 100 == 0L -> 1
                millis % 10 == 0L -> 2
                else -> 3
            }
            if (!zeros) {
                extra -= nonZeroDigits
                if (extra < 0) extra = 0
            } else if (extra + nonZeroDigits > 2) {
                extra = 2 - nonZeroDigits
                if (extra < 0) extra = 0
            }
            if (nonZeroDigits == 0) extra++ // decimal point
            return Milliseconds(value, extra, Private)
        }

        @JvmStatic
        fun parse(s: String): Milliseconds {
            val m = Private.REGEX.find(s) ?: throw NumberFormatException("For input string: \"$s\"")
            val iPart: String = m.groupValues[Private.GROUP_IPART]
            val sb = StringBuilder()
                .append(iPart)
            var extra = 0
            when (val fPart: String? = m.groups[Private.GROUP_FPART]?.value) {
                null -> sb.append("000")
                "." -> {
                    extra = 1
                    sb.append("000")
                }
                ".0" -> {
                    extra = 2
                    sb.append("000")
                }
                ".00", ".000" -> {
                    extra = 3
                    sb.append("000")
                }
                else -> {
                    val sf = fPart.substring(1)
                    for(i in (sf.length - 1) downTo 0) {
                        val ch = sf[i]
                        if (ch != '0') break
                        extra++
                    }
                    sb.append(sf)
                    when (sf.length) {
                        1 -> sb.append("00")
                        2 -> sb.append("0")
                        3 -> if (extra > 0) extra--
                    }
                }
            }
            val value: Long = try {
                sb.toString().toLong()
            } catch (e: NumberFormatException) {
                throw NumberFormatException("For input string: \"$s\"")
            }
            if (value == 0L && iPart.isNotEmpty() && iPart[0] == '-') extra = -1 - extra
            return Milliseconds(value, extra, Private)
        }

        private const val serialVersionUID = 1L

    }

    val hasDecimalPoint: Boolean
        @JvmName("hasDecimalPoint")
        get() = value % 1000L != 0L || extra > 0 || extra < -1

    val trailingZeros: Int
        @JvmName("trailingZeros")
        get() = when {
            extra < -1 -> -2 - extra
            extra <= 0 -> 0
            value % 1000L == 0L -> extra - 1
            else -> extra
        }

    val decimalPlaces: Int
        @JvmName("decimalPlaces")
        get() = trailingZeros + when {
            value % 1000L == 0L -> 0
            value % 100L == 0L -> 1
            value % 10L == 0L -> 2
            else -> 3
        }

    override fun compareTo(other: Milliseconds) = value.compareTo(other.value)

    operator fun compareTo(other:   Byte) = value.compareTo(other)
    operator fun compareTo(other:  Short) = value.compareTo(other)
    operator fun compareTo(other:    Int) = value.compareTo(other)
    operator fun compareTo(other:   Long) = value.compareTo(other)
    operator fun compareTo(other:  Float) = value.compareTo(other)
    operator fun compareTo(other: Double) = value.compareTo(other)

    @ExperimentalUnsignedTypes
    operator fun compareTo(other:  UByte) = value.compareTo(other.toLong())
    @ExperimentalUnsignedTypes
    operator fun compareTo(other: UShort) = value.compareTo(other.toLong())
    @ExperimentalUnsignedTypes
    operator fun compareTo(other:   UInt) = value.compareTo(other.toLong())

    @ExperimentalUnsignedTypes
    inline operator fun compareTo(other: ULong) = compareToUnsigned(other.toLong())

    fun compareToUnsigned(other: Long) = if (value < 0L) -1 else
        (value xor Long.MIN_VALUE).compareTo(other xor Long.MIN_VALUE)

    override fun   toChar() = value.toChar()
    override fun   toByte() = value.toByte()
    override fun  toShort() = value.toShort()
    override fun    toInt() = value.toInt()
    override fun   toLong() = value
    override fun  toFloat() = value.toFloat()
    override fun toDouble() = value.toDouble()

    @ExperimentalUnsignedTypes fun  toUByte() = toByte().toUByte()
    @ExperimentalUnsignedTypes fun toUShort() = toShort().toUShort()
    @ExperimentalUnsignedTypes fun   toUInt() = toInt().toUInt()
    @ExperimentalUnsignedTypes fun  toULong() = toLong().toULong()

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
        return (hash + (hash shr 32)).toInt()
    }

    private var string: String? = null

    override fun toString(): String = string ?: run {
        val withPoint: String
        val withoutPoint: String
        when (extra) {
            -4 -> return@run "-0.00"
            -3 -> return@run "-0.0"
            -2 -> return@run "-0."
            -1 -> return@run "-"
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
        value.millisToStringSeconds() + if (value % 1000 == 0L) withPoint else withoutPoint
    }.also { string = it }

    private fun readObject(input: ObjectInputStream) {
        input.defaultReadObject()
        var millis = value
        var extra = this.extra
        when {
            extra == 0 -> return
            millis != 0L -> {
                millis %= 1000L
                if (millis < 0) millis = -millis
                val limit = when {
                    millis == 0L -> 3
                    millis % 100 == 0L -> 2
                    millis % 10 == 0L -> 1
                    else -> 0
                }
                when {
                    extra < 0 -> {
                        extra = -1 - extra
                        if (extra > limit) extra = limit
                    }
                    extra > limit -> extra = limit
                    else -> return
                }
            }
            extra < -4 -> extra = -4
            extra > 3 -> extra = 3
            else -> return
        }
        this.extra = extra
    }

    private fun readResolve(): Any = when {
        value != 0L -> this
        extra < 0 -> Private.MINUS_ZERO[-1 - extra]
        else -> Private.PLUS_ZERO[extra]
    }

}

fun Byte.toMilliseconds() = Milliseconds.valueOf(toLong())
fun Short.toMilliseconds() = Milliseconds.valueOf(toLong())
fun Int.toMilliseconds() = Milliseconds.valueOf(toLong())
fun Long.toMilliseconds() = Milliseconds.valueOf(this)
@ExperimentalUnsignedTypes
fun UByte.toMilliseconds() = Milliseconds.valueOf(toLong())
@ExperimentalUnsignedTypes
fun UShort.toMilliseconds() = Milliseconds.valueOf(toLong())
@ExperimentalUnsignedTypes
fun UInt.toMilliseconds() = Milliseconds.valueOf(toLong())
@ExperimentalUnsignedTypes
fun ULong.toMilliseconds() = Milliseconds.valueOf(toLong())