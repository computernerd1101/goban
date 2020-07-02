package com.computernerd1101.goban.internal

import com.computernerd1101.goban.*
import java.util.concurrent.atomic.*
import java.util.function.Supplier
import kotlin.reflect.*

@Suppress("NOTHING_TO_INLINE")
inline fun <T> threadLocal(supplier: Supplier<out T>): ThreadLocal<T> {
    return ThreadLocal.withInitial(supplier)
}

inline fun <T> threadLocal(crossinline supplier: () -> T): ThreadLocal<T> {
    return ThreadLocal.withInitial { supplier() }
}

operator fun <T> ThreadLocal<out T>.getValue(thisRef: Any?, prop: KProperty<*>): T = get()

operator fun <T> ThreadLocal<in T>.setValue(thisRef: Any?, prop: KProperty<*>, value: T) {
    set(value)
}

class SecretKeeper<T: Any>(
    // expected to return a companion object,
    // thereby implicitly calling its init block
    // if it hasn't already. The aforementioned init block,
    // in turn, is expected to call [setValue] on
    // this SecretKeeper.
    val companion: () -> Any
) {

    private lateinit var value: T

    operator fun getValue(thisRef: Any?, prop: KProperty<*>): T {
        companion()
        return value
    }

    operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: T) {
        this.value = value
    }

}

var internalSelfRect: (GoPoint, CharArray) -> GoRectangle by SecretKeeper { GoRectangle }

private const val deBruijn64: Long = 0x03f79d71b4ca8b09

private val deBruijn64tab = byteArrayOf(
    0, 1, 56, 2, 57, 49, 28, 3, 61, 58, 42, 50, 38, 29, 17, 4,
    62, 47, 59, 36, 45, 43, 51, 22, 53, 39, 33, 30, 24, 18, 12, 5,
    63, 55, 48, 27, 60, 41, 37, 16, 46, 35, 44, 21, 52, 32, 23, 11,
    54, 26, 40, 15, 34, 20, 31, 10, 25, 14, 19, 9, 13, 8, 7, 6
)

// the given argument must be two to the power of k.
// Multiplying by a power of two is equivalent to
// left shifting, in this case by k bits. The de Bruijn (64 bit) constant
// is such that all six bit, consecutive substrings are distinct.
// Therefore, if we have a left shifted version of this constant we can
// find by how many bits it was shifted by looking at which six bit
// substring ended up at the top of the word.
// (Knuth, volume 4, section 7.3.1)
// Normally, bit would be (x and -x), where x would be the input variable.
// However, this function is module-internal, and every caller in this module
// inputs a value that is already a power of 2, since that value is
// useful outside of each call to this function.
fun trailingZerosPow2(bit: Long) = deBruijn64tab[(bit*deBruijn64).ushr(64 - 6).toInt()].toInt()

private const val deBruijn32: Int = 0x077CB531

private val deBruijn32tab = byteArrayOf(
    0, 1, 28, 2, 29, 14, 24, 3, 30, 22, 20, 15, 25, 17, 4, 8,
    31, 27, 13, 23, 21, 19, 16, 7, 26, 12, 18, 6, 11, 5, 10, 9
)

fun trailingZerosPow2(bit: Int) = deBruijn32tab[(bit*deBruijn32).ushr(32 - 5)].toInt()

private val lenTab = byteArrayOf(
    0x00, 0x01, 0x02, 0x02, 0x03, 0x03, 0x03, 0x03, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04,
    0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05,
    0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06,
    0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06,
    0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07,
    0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07,
    0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07,
    0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07,
    0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08,
    0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08,
    0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08,
    0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08,
    0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08,
    0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08,
    0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08,
    0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08
)

fun highestBitLength(bits: Long): Int {
    val x = bits.ushr(32).toInt()
    return if (x == 0) highestBitLength(bits.toInt())
    else 32 + highestBitLength(x)
}

fun highestBitLength(bits: Int): Int {
    var x = bits
    var n = 0
    if (x >= 1 shl 16) {
        x = x ushr 16
        n += 16
    }
    if (x >= 1 shl 8) {
        x = x ushr 8
        n += 8
    }
    return n + lenTab[x]
}

inline fun <reified T: Any> atomicIntUpdater(name: String): AtomicIntegerFieldUpdater<T> {
    return AtomicIntegerFieldUpdater.newUpdater(T::class.java, name)
}

inline fun <reified T: Any> atomicLongUpdater(name: String): AtomicLongFieldUpdater<T> {
    return AtomicLongFieldUpdater.newUpdater(T::class.java, name)
}

inline fun <reified T: Any, reified V> atomicUpdater(name: String): AtomicReferenceFieldUpdater<T, V> {
    return AtomicReferenceFieldUpdater.newUpdater(T::class.java, V::class.java, name)
}

fun <T: Any, V: Any> AtomicReferenceFieldUpdater<T, V?>.getOrDefault(target: T, default: V): V {
    while(!compareAndSet(target, null, default)) {
        val value: V? = get(target)
        if (value != null) return value
    }
    return default
}