package com.computernerd1101.sgf.internal

import com.computernerd1101.sgf.SGFCopyLevel
import java.io.*
import java.util.*
import kotlin.ConcurrentModificationException
import kotlin.NoSuchElementException
import kotlin.collections.RandomAccess

@Suppress("LeakingThis")
abstract class AbstractSGFList<E: Any>:
    AbstractMutableList<E>, RandomAccess {

    final override var size: Int

    @Transient
    var elements: Array<E?>

    abstract fun newArray(size: Int): Array<E?>

    open fun optimizedCopy(n: Int, a: Array<E?>, level: SGFCopyLevel) {}

    open fun addNew(e: E) = e

    open fun allowEmpty() = false

    open fun checkAddPrivilege() = Unit

    open fun emptyListException(): RuntimeException = IllegalStateException(emptyListMessage())

    open fun emptyListMessage() = "List cannot be empty."

    constructor(initialCapacity: Int, first: E) {
        val es = newArray(initialCapacity.coerceAtLeast(1))
        es[0] = addNew(first)
        size = 1
        elements = es
    }

    @Suppress("unused")
    constructor(initialCapacity: Int, first: E, second: E) {
        val es = newArray(initialCapacity.coerceAtLeast(2))
        es[0] = addNew(first)
        es[1] = addNew(second)
        size = 2
        elements = es
    }

    constructor() {
        elements = newArray(0)
        size = 0
    }

    constructor(initialCapacity: Int) {
        elements = newArray(initialCapacity.coerceAtLeast(0))
        size = 0
    }

    constructor(values: Array<out E>) {
        val n = values.size
        val es = newArray(n)
        elements = es
        size = n
        addNew(es, 0, values)
    }

    constructor(first: E, values: Array<out E>) {
        val n = 1 + values.size
        val es = newArray(n)
        es[0] = addNew(first)
        elements = es
        size = n
        addNew(es, 1, values)
    }

    private fun addNew(dst: Array<E?>, n: Int, src: Array<out E>) {
        var i = n
        for(e in src) {
            dst[i++] = addNew(e)
        }
    }

    constructor(c: Collection<E?>) {
        var n: Int = c.size
        val off: Int
        val src: Array<E?>
        var dst: Array<E?>
        when(c) {
            is AbstractSGFList -> {
                off = 0
                src = c.elements
            }
            is SGFSubList -> {
                off = c.offset
                src = c.root.elements
            }
            else -> {
                val allowEmpty = allowEmpty()
                if (!allowEmpty && n == 0)
                    throw IllegalArgumentException(emptyListMessage())
                size = 0
                dst = newArray(n)
                elements = dst
                n = 0
                for(e in c) {
                    if (e != null) {
                        if (n == dst.size) // more elements than expected, if iterator is faulty
                            dst = grow(dst, n + 1)
                        dst[n++] = addNew(e)
                    }
                }
                if (!allowEmpty && n == 0)
                    throw IllegalArgumentException(emptyListMessage())
                size = n
                elements = dst
                return
            }
        }
        dst = newArray(src.size - off)
        for(i in 0 until n)
            dst[i] = addNew(src[off + i]!!)
        size = n
        elements = dst
    }

    constructor(list: AbstractSGFList<E>, level: SGFCopyLevel) {
        val n = list.size
        val es = list.elements.clone()
        optimizedCopy(n, es, level)
        size = n
        elements = es
    }

    @Suppress("UNCHECKED_CAST")
    constructor(ois: ObjectInputStream, size: Int) {
        val elements = newArray(size)
        for(i in 0 until size) {
            val e = ois.readObject() ?: throw InvalidObjectException("element cannot be null")
            val eType: Class<*> = elements.javaClass.componentType
            if (!eType.isInstance(e))
                throw InvalidObjectException("${e.javaClass.name} cannot be cast to ${eType.name}")
            elements[i] = e as E
        }
        this.size = size
        this.elements = elements
    }

    fun write(oos: ObjectOutputStream, size: Int) {
        val elements = this.elements
        for(i in 0 until size)
            oos.writeObject(elements[i] ?: throw InvalidObjectException("element cannot be null"))
    }

    companion object {

        private fun newCapacity(old: Int, min: Int): Int {
            val cap = old + (old shr 1)
            if(cap - min <= 0) {
                if (min < 0) throw OutOfMemoryError()
                return min
            }
            return if (cap - MAX_ARRAY_SIZE <= 0) cap
            else hugeCapacity(min)
        }

        private fun hugeCapacity(min: Int): Int {
            if (min < 0) throw OutOfMemoryError()
            return if (min > MAX_ARRAY_SIZE) Integer.MAX_VALUE
            else MAX_ARRAY_SIZE
        }

        private const val MAX_ARRAY_SIZE = Int.MAX_VALUE - 8

    }

    private fun grow(old: Array<E?>, min: Int): Array<E?> {
        val es = newArray(
            newCapacity(
                old.size,
                min
            )
        )
        old.copyInto(es)
        elements = es
        return es
    }

    override fun contains(element: E): Boolean {
        return indexOf(element) >= 0
    }

    override fun indexOf(element: E): Int {
        for(index in 0 until size)
            if (element == elements[index]) return index
        return -1
    }

    override fun lastIndexOf(element: E): Int {
        for(index in (size - 1) downTo 0)
            if (element == elements[index]) return index
        return -1
    }

    override fun toArray(): Array<Any> {
        val es = elements
        return Array(size) { index ->
            es[index] as Any
        }
    }

    override fun <T : Any> toArray(a: Array<T?>): Array<T?> {
        val ac = a.javaClass
        val es = ac.cast(elements)
        val n = size
        if (a.size < n)
            return Arrays.copyOf(es, n, ac)
        es.copyInto(a, endIndex=n)
        if (a.size > n) a[n] = null
        return a
    }

    override fun get(index: Int): E {
        checkElementIndex(index, size)
        return elements[index]!!
    }

    final override fun set(index: Int, element: E): E {
        checkAddPrivilege()
        checkElementIndex(index, size)
        return fastSet(index, element)
    }

    private fun fastSet(index: Int, element: E): E {
        val es = elements
        val old = es[index]!!
        es[index] = element
        return old
    }

    override fun add(element: E): Boolean {
        checkAddPrivilege()
        addPrivileged(element)
        return true
    }

    override fun add(index: Int, element: E) {
        checkAddPrivilege()
        addPrivileged(index, element)
    }

    fun addPrivileged(element: E) {
        val n = size
        fastAdd(n, elements, n, element)
    }

    fun addPrivileged(index: Int, element: E) {
        val n = size
        checkPositionIndex(index, n)
        fastAdd(n, elements, index, element)
    }

    private fun fastAdd(n: Int, a: Array<E?>, index: Int, element: E) {
        modCount++
        var es = a
        if (n == es.size)
            es = grow(es, n + 1)
        if (index < n)
            es.copyInto(es, index + 1, index, n)
        es[index] = element
        size = n + 1
    }

    final override fun removeAt(index: Int): E {
        val n = size
        checkElementIndex(index, n)
        val es = elements
        val old = es[index]!!
        fastRemove(n, es, index)
        return old
    }

    final override fun remove(element: E): Boolean {
        val n = size
        return remove(n, element, 0, n)
    }

    private fun remove(n: Int, element: E, from: Int, length: Int): Boolean {
        val es = elements
        for(i in from until (from + length))
            if (element == es[i]) {
                fastRemove(n, es, i)
                return true
            }
        return false
    }

    private fun fastRemove(n: Int, es: Array<E?>, index: Int) {
        if (n == 1 && !allowEmpty())
            throw emptyListException()
        modCount++
        if (n - 1 > index)
            es.copyInto(es, index, index + 1, n)
        size = n - 1
        es[n] = null
    }

    private fun shiftTailOverGap(n: Int, es: Array<E?>, lo: Int, hi: Int) {
        es.copyInto(es, lo, hi, n)
        val s = n - (hi - lo)
        size = s
        for(i in s until n) es[i] = null
    }

    override fun clear() {
        if (!allowEmpty())
            throw UnsupportedOperationException(emptyListMessage())
        val n = size
        shiftTailOverGap(n, elements, 0, n)
    }

    override fun addAll(elements: Collection<E>): Boolean {
        checkAddPrivilege()
        val n = size
        return addAllPrivileged(n, n, elements) != 0
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        checkAddPrivilege()
        val n = size
        checkPositionIndex(index, n)
        return addAllPrivileged(n, index, elements) != 0
    }

    @Suppress("UNCHECKED_CAST")
    private fun addAllPrivileged(n: Int, index: Int, c: Collection<E>, copyLevel: SGFCopyLevel? = null): Int {
        var es = elements
        val ca = c.toTypedArray<Any?>()
        var numNew = ca.size
        if (numNew == 0) return 0
        val a = newArray(numNew)
        val type = es.javaClass.componentType as Class<E>
        try {
            for(i in 0 until numNew)
                try {
                    a[i] = type.cast(ca[i]!!)
                } catch(e: Throwable) {
                    numNew = i
                    throw e
                }
        } finally {
            if (numNew > 0) {
                if (copyLevel != null)
                    optimizedCopy(n, a, copyLevel)
                modCount++
                if (numNew > es.size - n)
                    es = grow(es, n + numNew)
                if (n - index > 0)
                    es.copyInto(es, index + numNew, index, n)
                a.copyInto(es, index, 0, numNew)
            }
        }
        return numNew
    }

    override fun removeRange(fromIndex: Int, toIndex: Int) {
        val n = size
        checkRangeIndexes(fromIndex, toIndex, n)
        if (!allowEmpty() && fromIndex == 0 && toIndex == n)
            throw emptyListException()
        shiftTailOverGap(n, elements, fromIndex, toIndex)
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        val n = size
        return batchRemove(n, elements, compliment=false, start=0, end=n) != 0
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        val n = size
        return batchRemove(n, elements, compliment=true, start=0, end=n) != 0
    }

    private fun batchRemove(n: Int, c: Collection<E>, compliment: Boolean, start: Int, end: Int): Int {
        var r = start
        val es = elements
        while(r < end && c.contains(es[r]) == compliment) r++
        var purged = 0
        if (r < end) {
            if (n == 1) throw emptyListException()
            purged = 1
            var w = r++
            var lastElement = es[w]
            var thrown: Throwable? = null
            try {
                while(r < end) {
                    lastElement = es[r]
                    if (c.contains(lastElement) == compliment) {
                        es[w++] = lastElement
                        purged++
                    }
                }
            } catch(ex: Throwable) {
                // Preserve behavioral compatibility with AbstractCollection,
                // even if c.contains() throws.
                es.copyInto(es, w, r, end)
                w += end - r
                thrown = ex
                throw ex
            } finally {
                modCount += end - w
                shiftTailOverGap(n, es, w, end)
                if (!allowEmpty() && w == 0 && end == n) {
                    es[0] = lastElement
                    size = 1
                    val ex = emptyListException()
                    if (thrown == null) throw ex
                    else thrown.addSuppressed(ex)
                }
            }
        }
        return purged
    }

    override fun iterator(): MutableIterator<E> {
        return Itr(this)
    }

    override fun listIterator(): MutableListIterator<E> {
        return ListItr(this)
    }

    override fun listIterator(index: Int): MutableListIterator<E> {
        return ListItr(this, index)
    }

    private open class Itr<E: Any>: MutableIterator<E> {

        val root: AbstractSGFList<E>
        val sList: SGFSubList<E>?
        val offset: Int
        var size: Int
        var cursor: Int = 0   // index of next element to return
        var lastRet: Int = -1 // index of last element returned; -1 if no such
        var expectedModCount: Int

        constructor(list: AbstractSGFList<E>) {
            root = list
            sList = null
            offset = 0
            size = root.size
            expectedModCount = list.modCount
        }

        constructor(list: SGFSubList<E>) {
            root = list.root
            sList = list
            offset = list.offset
            size = list.size
            expectedModCount = root.modCount
        }

        override fun hasNext() = cursor != size

        override fun next(): E {
            checkForComodification()
            val i = cursor
            if (i >= size)
                throw NoSuchElementException()
            val es = root.elements
            if (i >= es.size)
                throw ConcurrentModificationException()
            cursor = i + 1
            lastRet = i
            return es[i + offset] ?: throw ConcurrentModificationException()
        }

        override fun remove() {
            val i = lastRet
            if (i < 0) throw IllegalStateException()
            checkForComodification()
            val n = root.size
            if (n == 1 && !root.allowEmpty())
                throw root.emptyListException()
            try {
                root.fastRemove(n, root.elements, i + offset)
            } catch(ex: IndexOutOfBoundsException) {
                throw ConcurrentModificationException()
            }
        }

        fun checkForComodification() {
            if (root.modCount != expectedModCount)
                throw ConcurrentModificationException()
            sList?.checkForComodification()
        }

    }

    private class ListItr<E: Any>: Itr<E>, MutableListIterator<E> {

        constructor(list: AbstractSGFList<E>): super(list)

        constructor(list: AbstractSGFList<E>, index: Int): super(list) {
            checkPositionIndex(index, size)
            cursor = index
        }

        constructor(list: SGFSubList<E>): super(list)

        constructor(list: SGFSubList<E>, index: Int): super(list) {
            checkPositionIndex(index, size)
            cursor = index
        }

        override fun hasPrevious() = cursor != 0

        override fun nextIndex() = cursor

        override fun previousIndex() = cursor - 1

        override fun previous(): E {
            checkForComodification()
            val i = cursor - 1
            if (i < 0) throw NoSuchElementException()
            val es = root.elements
            if (i >= es.size) throw ConcurrentModificationException()
            cursor = i
            lastRet = i
            return es[i + offset] ?: throw ConcurrentModificationException()
        }

        override fun set(element: E) {
            root.checkAddPrivilege()
            val i = lastRet
            if (i < 0) throw IllegalStateException()
            checkForComodification()
            val es = root.elements
            if (i >= es.size) throw ConcurrentModificationException()
            es[i + offset] = element
        }

        override fun add(element: E) {
            root.checkAddPrivilege()
            addPrivileged(element)
        }

        fun addPrivileged(e: E) {
            checkForComodification()
            try {
                val i = cursor
                root.addPrivileged(i, e)
                cursor = i + 1
                lastRet = -1
                expectedModCount = root.modCount
                size++
                sList?.updateSizeAndModCount(1)
            } catch(ex: IndexOutOfBoundsException) {
                throw ConcurrentModificationException()
            }
        }

    }

    @Suppress("unused")
    fun addPrivileged(itr: MutableListIterator<in E>, e: E) {
        (itr as? ListItr<in E>)?.addPrivileged(e) ?: itr.add(e)
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
        checkRangeIndexes(fromIndex, toIndex, size)
        return SGFSubList(this, fromIndex, toIndex)
    }

    private class SGFSubList<E: Any>: AbstractMutableList<E>, RandomAccess {

        val root: AbstractSGFList<E>
        val parent: SGFSubList<E>?
        val offset: Int
        private var _size: Int

        override val size: Int
            get() {
                checkForComodification()
                return _size
            }

        constructor(root: AbstractSGFList<E>, fromIndex: Int, toIndex: Int) {
            this.root = root
            parent = null
            offset = fromIndex
            _size = toIndex - fromIndex
        }

        constructor(parent: SGFSubList<E>, fromIndex: Int, toIndex: Int) {
            root = parent.root
            this.parent = parent
            offset = parent.offset + fromIndex
            _size = toIndex - fromIndex
        }

        fun checkForComodification() {
            if (root.modCount != modCount)
                throw ConcurrentModificationException()
        }

        override fun get(index: Int): E {
            checkElementIndex(index, size)
            return root.elements[index + offset]!!
        }

        override fun set(index: Int, element: E): E {
            root.checkAddPrivilege()
            checkElementIndex(index, size)
            return root.fastSet(index + offset, element)
        }

        override fun add(element: E): Boolean {
            val n = size
            root.add(n + offset, element)
            _size = n + 1
            updateModCountAndParentSize(1)
            return true
        }

        override fun add(index: Int, element: E) {
            val n = size
            checkPositionIndex(index, n)
            root.add(index + offset, element)
            _size = n + 1
            updateModCountAndParentSize(1)
        }

        override fun removeAt(index: Int): E {
            val n = size
            checkElementIndex(index, n)
            val element = root.removeAt(offset + index)
            _size = n - 1
            updateModCountAndParentSize(-1)
            return element
        }

        override fun remove(element: E): Boolean {
            val n = size
            val off = offset
            if (!root.remove(root.size, element, off, off + n))
                return false
            _size = n - 1
            updateModCountAndParentSize(-1)
            return true
        }

        override fun removeRange(fromIndex: Int, toIndex: Int) {
            val n = size
            checkRangeIndexes(fromIndex, toIndex, n)
            val off = offset
            root.removeRange(off + fromIndex, off + toIndex)
            _size = n - (toIndex - fromIndex)
            updateModCountAndParentSize(fromIndex - toIndex)
        }

        override fun addAll(elements: Collection<E>): Boolean {
            root.checkAddPrivilege()
            val n = size
            return addAll(n, n, elements)
        }

        override fun addAll(index: Int, elements: Collection<E>): Boolean {
            root.checkAddPrivilege()
            val n = size
            checkPositionIndex(index, n)
            return addAll(n, index, elements)
        }

        private fun addAll(n: Int, index: Int, elements: Collection<E>): Boolean {
            val numNew = root.addAllPrivileged(root.size, index, elements)
            if (numNew == 0) return false
            _size = n + numNew
            updateModCountAndParentSize(numNew)
            return true
        }

        override fun removeAll(elements: Collection<E>) = batchRemove(elements, compliment=false)

        override fun retainAll(elements: Collection<E>) = batchRemove(elements, compliment=true)

        private fun batchRemove(elements: Collection<E>, compliment: Boolean): Boolean {
            val n = size
            val off = offset
            val purged = root.batchRemove(root.size, elements, compliment, off, off + n)
            if (purged == 0) return false
            _size = n - purged
            updateModCountAndParentSize(-purged)
            return true
        }

        override fun iterator(): MutableIterator<E> =
            Itr(this)

        override fun listIterator(): MutableListIterator<E> =
            ListItr(this)

        override fun listIterator(index: Int): MutableListIterator<E> =
            ListItr(this, index)

        override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
            checkRangeIndexes(fromIndex, toIndex, size)
            return SGFSubList(
                this,
                fromIndex,
                toIndex
            )
        }

        fun updateSizeAndModCount(sizeChange: Int) {
            var subList: SGFSubList<E>? = this
            while(subList != null) {
                subList._size += sizeChange
                subList.modCount = root.modCount
                subList = subList.parent
            }
        }

        private fun updateModCountAndParentSize(sizeChange: Int) {
            modCount = root.modCount
            var subList = parent
            while(subList != null) {
                subList._size += sizeChange
                subList.modCount = root.modCount
                subList = subList.parent
            }
        }

    }

}