@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.computernerd1101.goban

import com.computernerd1101.goban.sgf.GoSGF
import com.computernerd1101.goban.time.millisToStringSeconds
import kotlinx.coroutines.*
import kotlin.jvm.functions.FunctionN
import kotlin.reflect.*
import kotlin.reflect.full.memberProperties

fun main() {
    println(typeOf<Array<in String?>>())
}

fun unitFunction(foo: Unit) {
    println(foo)
}

class PrivateMutableProperty {

    var prop: String = ""; private set

    fun reflectProp(): KProperty1<PrivateMutableProperty, String> {
        return PrivateMutableProperty::prop
    }

}

lateinit var mainFoobar: KProperty0<Int>

fun isStringArray(array: Array<*>): Boolean {
    return array.isArrayOf<String>()
}

class TestJvmField {

    @JvmField var foobar: Any = Any()

}

lateinit var foo: Any private set

class MyFun<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22, P23, R>:
    (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22, P23) -> R {

    override fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        p9: P9,
        p10: P10,
        p11: P11,
        p12: P12,
        p13: P13,
        p14: P14,
        p15: P15,
        p16: P16,
        p17: P17,
        p18: P18,
        p19: P19,
        p20: P20,
        p21: P21,
        p22: P22,
        p23: P23
    ): R {
        TODO("Not yet implemented")
    }

    val arity: Int get() = 23

}

object MyProperty {

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String = "MyProperty.getValue()"

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        println("MyProperty.setValue($value)")
    }

}

var myProperty by MyProperty