package com.computernerd1101.goban.time

import java.util.*
import kotlin.addSuppressed as suppress

abstract class Overtime: Cloneable {

    open fun filterEvent(e: TimeEvent): TimeEvent = e

    abstract fun parseThis(s: String): Boolean

    val typeString: String
        @JvmName("typeString")
        get() = getTypeString() ?: javaClass.simpleName

    protected open fun getTypeString(): String? = null

    @JvmOverloads
    fun displayName(locale: Locale = Locale.getDefault()): String = getDisplayName(locale) ?: typeString
    
    protected open fun getDisplayName(locale: Locale): String? = null

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
                            e.suppress(x)
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

    @Throws
    public final override fun clone(): Overtime {
        return (super.clone() as Overtime).apply { onClone() }
    }

}