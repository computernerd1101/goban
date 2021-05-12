@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.computernerd1101.goban.test.sandbox

import kotlin.reflect.*

fun main() {
    val f: (Int, Int) -> Int = ::testLambda
    println(f(2, 3))
}

fun testLambda(a: Int, b: Int): Int = a + b


@Suppress("unused")
internal class MyList<E>(private val delegate: MutableList<E>): MutableList<E> by delegate {

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> = MyList(delegate.subList(fromIndex, toIndex))

    fun toArray(): Array<Any?> = (delegate as java.util.List<*>).toArray()

    fun <T> toArray(a: Array<T>): Array<T> {
        println("ReadOnlyList.toArray")
        return (delegate as java.util.List<*>).toArray(a)
    }

    @Suppress("SuspiciousEqualsCombination")
    override fun equals(other: Any?): Boolean = this === other || delegate == other

    override fun hashCode(): Int = delegate.hashCode()

    override fun toString(): String = delegate.toString()

}

typealias MyAlias = MyNumber

class MyNumber: Number() {

    override fun toByte(): Byte = Byte.MAX_VALUE

    override fun toShort(): Short = Short.MAX_VALUE

    override fun toChar(): Char = '0'

    override fun toInt(): Int = 100

    override fun toLong(): Long = Long.MAX_VALUE

    override fun toFloat(): Float = Float.MAX_VALUE

    override fun toDouble(): Double = Double.MAX_VALUE

}

open class MyComparable1: Comparable<MyComparable1> {

    override fun compareTo(other: MyComparable1): Int {
        TODO("Not yet implemented")
    }

}

class MyComparable2: MyComparable1()

fun midVarargs(a: Int, vararg b: Int, c: Int) {
    println(a)
    println(b.contentToString())
    println(c)
}



fun nullable(s: String?): String? = maybeNullable<String?, String?>(s, {this}, {null})

fun notNullable(s: String?): String = maybeNullable<String, Nothing>(s, {this}) {
    throw NullPointerException()
}

inline fun <T: String?, E: T> maybeNullable(s: String?, cast: String.() -> T, errorHandler: () -> E): T {
    return s?.cast() ?: errorHandler()
}

object TestParams {

    fun test(foo: String, bar: String) = Unit

}

@JvmField
var testNotNullProperty: String = "Hello, World!"

fun notNullToNotNull(s: String): String {
    return s;
}

fun testNullability(s: String): String {
    return MyJava().notNullToNotNull(s)
}


object TestIterator {

    operator fun get(vararg index: Int): Any? = null

    operator fun set(vararg index: Int, value: Any?) = Unit

    fun foo(vararg index: Int, value: Any?) = Unit

    operator fun iterator(): IntIterator {
        return intArrayOf(1, 2, 3, 4, 5).iterator()
    }

}

fun testVarargs1(foo: String, vararg args: String) {
    println(foo)
    println(args.contentToString())
}

fun testVarargs2(foo: String, bar: String, vararg args: String) {
    testVarargs1(foo, bar, *args, *args, bar)
}

object TestStatic {

    @JvmStatic fun foo() = println("foo")

    fun bar() = println("bar")


}

fun testDefaultParam(width: Int, height: Int = width) {
    println("${width}x$height")
}

class TestCompanion {

    companion object: Runnable {

        var myProp: Any = Any()

        @JvmStatic
        override fun run() {
            TODO("Not yet implemented")
        }

    }

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

//class MyFun<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22, P23, R>:
//    (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22, P23) -> R {
//
//    override fun invoke(
//        p1: P1,
//        p2: P2,
//        p3: P3,
//        p4: P4,
//        p5: P5,
//        p6: P6,
//        p7: P7,
//        p8: P8,
//        p9: P9,
//        p10: P10,
//        p11: P11,
//        p12: P12,
//        p13: P13,
//        p14: P14,
//        p15: P15,
//        p16: P16,
//        p17: P17,
//        p18: P18,
//        p19: P19,
//        p20: P20,
//        p21: P21,
//        p22: P22,
//        p23: P23
//    ): R {
//        TODO("Not yet implemented")
//    }
//
//    val arity: Int get() = 23
//
//}

fun testVarArgsLambda(vararg args: Any?, lambda: (Array<out Any?>) -> Any?): Any? {
    return lambda(args)
}

object MyProperty {

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String = "MyProperty.getValue()"

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        println("MyProperty.setValue($value)")
    }

}

var myProperty by MyProperty