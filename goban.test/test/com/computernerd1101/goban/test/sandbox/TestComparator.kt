package com.computernerd1101.goban.test.sandbox

import kotlin.jvm.JvmInline

@JvmInline
value class ComparatorScope<T> internal constructor(@PublishedApi internal val delegate: Comparator<Any?>) {

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun T.compareTo(other: T): Int = delegate.compare(this, other)

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun Incomparable<T>.compareTo(other: Incomparable<T>): Int =
        delegate.compare(_value, other._value)

}

@PublishedApi
@Suppress("UNCHECKED_CAST")
internal fun <T> comparatorScope(delegate: Comparator<T>) = ComparatorScope<T>(delegate as Comparator<Any?>)

@JvmInline
value class Incomparable<T> internal constructor(@PublishedApi internal val _value: Any?) {

    @Suppress("UNCHECKED_CAST")
    inline val value get() = _value as T

    override fun toString(): String = "incomparable($_value)"

}

fun <T> incomparable(value: T) = Incomparable<T>(value)

inline fun <T, R> withCompare(comparator: Comparator<T>, block: ComparatorScope<T>.() -> R): R =
    with(comparatorScope(comparator), block)

inline fun <T, R> Comparator<T>.runCompare(block: ComparatorScope<T>.() -> R): R =
    comparatorScope(this).run(block)

private class TestComparable(val value: Int)

private enum class TestComparator: Comparator<TestComparable> {

    NATURAL {
        override fun compare(a: TestComparable, b: TestComparable): Int = a.value.compareTo(b.value)
    },
    REVERSE {
        override fun compare(a: TestComparable, b: TestComparable): Int = b.value.compareTo(a.value)
    }

}

fun main() {
    val a = TestComparable(1)
    val b = TestComparable(2)
    withCompare(TestComparator.NATURAL) {
        println(a > b)
    }
    TestComparator.REVERSE.runCompare {
        println(a > b)
    }
    val s1 = "a"
    val s2 = "B"
    withCompare(String.CASE_INSENSITIVE_ORDER) {
        println(s1 > s2)
        println(incomparable(s1) > incomparable(s2))
    }
}