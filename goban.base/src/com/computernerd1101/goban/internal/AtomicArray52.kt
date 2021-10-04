package com.computernerd1101.goban.internal

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

@Suppress("unused")
internal open class AtomicArray52<E: Any> {

    companion object {

        @JvmField val SIZE = atomicIntUpdater<AtomicArray52<*>>("size")

        @JvmField
        val UPDATE: Array<AtomicReferenceFieldUpdater<AtomicArray52<*>, *>> = CharArray(3).let { buffer ->
            buffer[0] = 'e'
            Array(52) { index ->
                val n = if (index < 10) {
                    buffer[1] = '0' + index
                    2
                } else {
                    buffer[1] = '0' + index / 10
                    buffer[2] = '0' + index % 10
                    3
                }
                atomicUpdater<AtomicArray52<*>, Any?>(buffer.concatToString(0, n))
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun <E: Any> update(index: Int): AtomicReferenceFieldUpdater<AtomicArray52<E>, E?> =
            UPDATE[index] as AtomicReferenceFieldUpdater<AtomicArray52<E>, E?>

    }

    operator fun get(index: Int): E?    = update<E>(index)[this]
    operator fun set(index: Int, e: E?) { update<E>(index)[this] = e }

    @Volatile @JvmField var size: Int = 0

    @Volatile @JvmField var e0:  E? = null
    @Volatile @JvmField var e1:  E? = null
    @Volatile @JvmField var e2:  E? = null
    @Volatile @JvmField var e3:  E? = null
    @Volatile @JvmField var e4:  E? = null
    @Volatile @JvmField var e5:  E? = null
    @Volatile @JvmField var e6:  E? = null
    @Volatile @JvmField var e7:  E? = null
    @Volatile @JvmField var e8:  E? = null
    @Volatile @JvmField var e9:  E? = null
    @Volatile @JvmField var e10: E? = null
    @Volatile @JvmField var e11: E? = null
    @Volatile @JvmField var e12: E? = null
    @Volatile @JvmField var e13: E? = null
    @Volatile @JvmField var e14: E? = null
    @Volatile @JvmField var e15: E? = null
    @Volatile @JvmField var e16: E? = null
    @Volatile @JvmField var e17: E? = null
    @Volatile @JvmField var e18: E? = null
    @Volatile @JvmField var e19: E? = null
    @Volatile @JvmField var e20: E? = null
    @Volatile @JvmField var e21: E? = null
    @Volatile @JvmField var e22: E? = null
    @Volatile @JvmField var e23: E? = null
    @Volatile @JvmField var e24: E? = null
    @Volatile @JvmField var e25: E? = null
    @Volatile @JvmField var e26: E? = null
    @Volatile @JvmField var e27: E? = null
    @Volatile @JvmField var e28: E? = null
    @Volatile @JvmField var e29: E? = null
    @Volatile @JvmField var e30: E? = null
    @Volatile @JvmField var e31: E? = null
    @Volatile @JvmField var e32: E? = null
    @Volatile @JvmField var e33: E? = null
    @Volatile @JvmField var e34: E? = null
    @Volatile @JvmField var e35: E? = null
    @Volatile @JvmField var e36: E? = null
    @Volatile @JvmField var e37: E? = null
    @Volatile @JvmField var e38: E? = null
    @Volatile @JvmField var e39: E? = null
    @Volatile @JvmField var e40: E? = null
    @Volatile @JvmField var e41: E? = null
    @Volatile @JvmField var e42: E? = null
    @Volatile @JvmField var e43: E? = null
    @Volatile @JvmField var e44: E? = null
    @Volatile @JvmField var e45: E? = null
    @Volatile @JvmField var e46: E? = null
    @Volatile @JvmField var e47: E? = null
    @Volatile @JvmField var e48: E? = null
    @Volatile @JvmField var e49: E? = null
    @Volatile @JvmField var e50: E? = null
    @Volatile @JvmField var e51: E? = null

}