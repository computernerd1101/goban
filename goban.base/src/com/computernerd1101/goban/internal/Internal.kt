package com.computernerd1101.goban.internal

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import java.util.concurrent.atomic.*
import kotlin.coroutines.*

/*
 * class Cacheable private constructor(val index: Int) {
 *
 *     // Avoids generating convoluted access$getValues$cp()
 *     private object Cache {
 *
 *         const val SIZE: Int = ...
 *
 *         // impossible to access outside Cacheable, assuming that JVM access limits are strictly enforced
 *         @JvmField val values = arrayOfLateInit<Cacheable>(SIZE)
 *
 *     }
 *
 *     companion object {
 *
 *         init {
 *             for(i in 0 until Cache.SIZE) {
 *                 Cache.values[i] = Cacheable(i)
 *             }
 *         }
 *
 *         // At this point, all elements of Cache.values are no longer null.
 *         @JvmStatic fun valueOf(index: Int) = Cache.values[index]
 *
 *     }
 *
 * }
 */
@Suppress("UNCHECKED_CAST")
internal inline fun <reified T: Any> arrayOfLateInit(size: Int) = arrayOfNulls<T>(size) as Array<T>

/*
 * Internal members translate to the JVM as public. I see this as a security risk.
 * Fortunately, Java has supported module-info restricting package access since version 9.
 * Since the package com.computernerd1101.goban.internal is not exported, nothing outside
 * this module can access InternalMarker, nor can it access any function that uses it as
 * a non-nullable parameter type.
 */
internal object InternalMarker {

    /*
     * internal fun blockFunction(marker: InternalMarker) {
     *     marker.ignore(); // prevents marker from issuing an unused parameter warning.
     *     <function body>
     * }
     */
    fun ignore() = Unit

    /*
     * internal fun expressionBody(marker: InternalMarker) = marker.access { <some expression> }
     */
    //inline fun <R> access(block: () -> R): R = block()

}

internal class ContinuationProxy<T> {

    @JvmField var continuation: Continuation<T>? = null

    fun suspendAsync(scope: CoroutineScope): Deferred<T> = scope.async {
        suspendCoroutine { continuation = it }
    }

}

internal class SendOnlyChannel<E>(private val channel: Channel<E>): SendChannel<E> by channel {

    override fun toString(): String = channel.toString().replaceFirst(
        "@" + Integer.toHexString(System.identityHashCode(channel)),
        "@" + Integer.toHexString(System.identityHashCode(this)),
        ignoreCase = true
    )

}

private const val deBruijn64: Long = 0x03f79d71b4ca8b09

private val deBruijn64tab = byteArrayOf(
     0,  1, 56,  2, 57, 49, 28,  3, 61, 58, 42, 50, 38, 29, 17,  4,
    62, 47, 59, 36, 45, 43, 51, 22, 53, 39, 33, 30, 24, 18, 12,  5,
    63, 55, 48, 27, 60, 41, 37, 16, 46, 35, 44, 21, 52, 32, 23, 11,
    54, 26, 40, 15, 34, 20, 31, 10, 25, 14, 19,  9, 13,  8,  7,  6
)

// The given argument must be two to the power of k.
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
// useful outside each call to this function.
internal fun trailingZerosPow2(bit: Long) = deBruijn64tab[(bit*deBruijn64).ushr(64 - 6).toInt()].toInt()

private const val deBruijn32: Int = 0x077CB531

private val deBruijn32tab = byteArrayOf(
     0,  1, 28,  2, 29, 14, 24, 3, 30, 22, 20, 15, 25, 17,  4, 8,
    31, 27, 13, 23, 21, 19, 16, 7, 26, 12, 18,  6, 11,  5, 10, 9
)

internal fun trailingZerosPow2(bit: Int) = deBruijn32tab[(bit*deBruijn32).ushr(32 - 5)].toInt()

private val lenTab = ByteArray(256).also { table ->
    for(value in 1..8)
        for(i in 1.shl(value - 1) until 1.shl(value))
            table[i] = value.toByte()
}

internal fun highestBitLength(bits: Long): Int {
    var x = (bits ushr 32).toInt()
    var n = if (x == 0) {
        x = bits.toInt()
        0
    } else 32
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

internal fun <T: Any, V: Any> AtomicReferenceFieldUpdater<T, V?>.getOrDefault(target: T, default: V): V {
    return compareAndExchange(target, null, default) ?: default
}

internal fun <T: Any, V> AtomicReferenceFieldUpdater<T, V>.compareAndExchange(target: T, expect: V, update: V): V {
    while(!compareAndSet(target, expect, update)) {
        val value: V = get(target)
        if (value !== expect) return value
    }
    return expect
}
