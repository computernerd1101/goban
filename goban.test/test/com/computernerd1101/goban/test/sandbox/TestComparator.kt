package com.computernerd1101.goban.test.sandbox

inline class ComparatorScope<in T>(@PublishedApi internal val delegate: Comparator<in T>) {

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun T.compareTo(other: T): Int = delegate.compare(this, other)

    override fun toString(): String = delegate.toString()

}

inline fun <T, R> withCompare(comparator: Comparator<in T>, block: ComparatorScope<T>.() -> R): R =
    with(ComparatorScope(comparator), block)

inline fun <T, R> Comparator<in T>.runCompare(block: ComparatorScope<T>.() -> R): R =
    ComparatorScope(this).run(block)

fun main() {
}