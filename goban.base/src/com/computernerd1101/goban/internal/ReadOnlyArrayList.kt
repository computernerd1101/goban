package com.computernerd1101.goban.internal

import java.io.*
import java.util.Arrays

internal class ReadOnlyArrayList<out E> private constructor(
    private val array: Array<out E>,
    @Transient private val offset: Int,
    @Transient private var _size: Int
): List<E>, RandomAccess, Serializable {

    constructor(elements: Array<out E>): this(elements, 0, elements.size)

    override val size: Int get() = _size
    override fun isEmpty() = _size == 0
    override fun contains(element: @UnsafeVariance E): Boolean = indexOf(element) >= 0
    override fun iterator(): Iterator<E> = Itr(array, offset, _size, 0)
    override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean = elements.all { contains(it) }
    override fun get(index: Int): E {
        val size = _size
        if (index !in 0 until size)
            throw IndexOutOfBoundsException("index: $index, size: $size")
        return array[index + offset]
    }

    override fun indexOf(element: @UnsafeVariance E): Int {
        val limit = offset + _size
        if (element == null) {
            for(i in offset until limit)
                if (array[i] == null) return i
        } else {
            for(i in offset until limit)
                if (element == array[i]) return i
        }
        return -1
    }
    override fun lastIndexOf(element: @UnsafeVariance E): Int {
        var i = offset + _size
        if (element == null) {
            while(i > offset)
                if (array[--i] == null) return i
        } else {
            while(i > offset)
                if (element == array[--i]) return i
        }
        return -1
    }

    override fun listIterator(): ListIterator<E> = ListItr(array, offset, _size, 0)
    override fun listIterator(index: Int): ListIterator<E> {
        val size = _size
        if (index !in 0..size)
            throw IndexOutOfBoundsException("index: $index, size: $size")
        return ListItr(array, offset, size, index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<E> {
        val size = _size
        if (fromIndex < 0 || toIndex > size)
            throw IndexOutOfBoundsException("fromIndex: $fromIndex, toIndex: $toIndex, size: $size")
        if (fromIndex == toIndex)
            return emptyList()
        if (fromIndex > toIndex)
            throw IllegalArgumentException("fromIndex: $fromIndex > toIndex: $toIndex")
        return if (fromIndex == 0 && toIndex == size) this
        else ReadOnlyArrayList(array, fromIndex + offset, toIndex - fromIndex)
    }

    @Suppress("unused", "ReplaceJavaStaticMethodWithKotlinAnalog")
    fun toArray(): Array<Any?> = Arrays.copyOfRange(array, offset, offset + _size, Array<Any?>::class.java)

    @Suppress("unused", "ReplaceJavaStaticMethodWithKotlinAnalog", "UNCHECKED_CAST")
    fun <T> toArray(a: Array<T>): Array<T> {
        val size = _size
        if (a.size < size) return Arrays.copyOfRange(array, offset, offset + size, a.javaClass)
        System.arraycopy(array, offset, a, 0, size)
        if (a.size != size) a[size] = null as T
        return a
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val list = other as? List<*> ?: return false
        val size = _size
        if (size != list.size) return false
        for(i in 0 until size)
            if (array[i + offset] != list[i]) return false
        return true
    }

    @Transient private var hashCodeInitialized: Boolean = false
    @Transient private var hashCode: Int = 0

    override fun hashCode(): Int {
        if (hashCodeInitialized)
            return hashCode
        var h = 1
        for(i in offset until offset + _size)
            h = 31*h + array[i].hashCode()
        hashCode = h
        hashCodeInitialized = true
        return h
    }

    override fun toString(): String = joinToString(prefix = "[", postfix = "]")

    private open class Itr<out E>(
        @JvmField protected val array: Array<out E>,
        offset: Int,
        size: Int,
        index: Int
    ): Iterator<E> {

        @JvmField protected var cursor = index + offset
        private val limit = offset + size

        override fun hasNext(): Boolean = cursor < limit
        override fun next(): E {
            val i = cursor
            if (i >= limit) throw NoSuchElementException()
            cursor = i + 1
            return array[i]
        }

    }

    private class ListItr<out E>(
        array: Array<out E>,
        private val offset: Int,
        size: Int,
        index: Int
    ): Itr<E>(array, offset, size, index), ListIterator<E> {

        override fun hasPrevious(): Boolean = cursor > offset
        override fun previous(): E {
            val i = cursor - 1
            if (i < offset) throw NoSuchElementException()
            cursor = i
            return array[i]
        }

        override fun nextIndex(): Int = cursor - offset
        override fun previousIndex(): Int = cursor - offset - 1

    }

    private companion object {
        private const val serialVersionUID = 1L
    }

    private fun readObject(input: ObjectInputStream) {
        input.defaultReadObject()
        _size = array.size
    }

    private fun readResolve(): Any = if (_size == 0) emptyList() else this

    private fun writeReplace(): Any {
        val offset = this.offset
        val size = _size
        return when {
            size == 0 -> emptyList()
            offset == 0 && size == array.size -> this
            else -> ReadOnlyArrayList(array.copyOfRange(offset, offset + size), 0, size)
        }
    }

}