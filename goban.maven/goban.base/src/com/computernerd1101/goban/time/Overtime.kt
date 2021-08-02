package com.computernerd1101.goban.time

import java.util.*

abstract class Overtime: Cloneable {

    open val initialOvertimeCode: Int get() = 0

    open fun filterEvent(e: TimeEvent): TimeEvent = e

    open fun extendTime(e: TimeEvent, extension: Long): TimeEvent = TimeLimit.extendTime(e, extension)

    abstract fun parseThis(s: String): Boolean

    val typeString: String
        @JvmName("typeString")
        get() = getTypeString() ?: javaClass.simpleName

    protected open fun getTypeString(): String? = null

    @JvmOverloads
    fun displayName(locale: Locale = Locale.getDefault()): String = getDisplayName(locale) ?: typeString
    
    protected open fun getDisplayName(locale: Locale): String? = null

    @JvmOverloads
    fun displayOvertime(e: TimeEvent, locale: Locale = Locale.getDefault()): String =
        displayOvertimeImpl(e, locale) ?: e.overtimeCode.toString()

    protected open fun displayOvertimeImpl(e: TimeEvent, locale: Locale): String? = null

    companion object {

        @JvmStatic
        fun parse(s: String): Overtime? {
            val sl = ServiceLoader.load(Overtime::class.java)
            val itr: Iterator<Overtime> = sl.iterator()
            while (itr.hasNext()) {
                var o = itr.next()
                try {
                    if (o.parseThis(s)) return o
                } catch (e: Throwable) {
                    while (itr.hasNext()) {
                        o = itr.next()
                        try {
                            if (o.parseThis(s)) return o
                        } catch (x: Throwable) {
                            e.addSuppressed(x)
                        }
                    }
                    throw e
                }
            }
            return null
        }

        @JvmStatic
        fun loadTypes(): Array<Overtime> {
            val sl: ServiceLoader<Overtime> = ServiceLoader.load(Overtime::class.java)
            return sl.toList().toTypedArray()
        }

    }

    protected open fun onClone() = Unit

    @Throws(/* nothing */) // Overrides method that throws CloneNotSupportedException
    public final override fun clone(): Overtime {
        return (super.clone() as Overtime).apply { onClone() }
    }

}