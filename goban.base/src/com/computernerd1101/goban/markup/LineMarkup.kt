@file:JvmName("MarkupKt")
@file:JvmMultifileClass

package com.computernerd1101.goban.markup

import com.computernerd1101.goban.*
import com.computernerd1101.goban.internal.*
import java.io.*
import java.lang.ref.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun lineMarkup(x1: Int, y1: Int, x2: Int, y2: Int) = LineMarkup.lineMarkup(x1, y1, x2, y2)

infix fun GoPoint.lineMarkup(other: GoPoint) = LineMarkup.lineMarkup(this, other)

fun arrowMarkup(x1: Int, y1: Int, x2: Int, y2: Int) = LineMarkup.arrowMarkup(x1, y1, x2, y2)

infix fun GoPoint.arrowMarkup(other: GoPoint) = LineMarkup.arrowMarkup(this, other)


class LineMarkup private constructor(
    @JvmField val start: GoPoint,
    @JvmField val end: GoPoint,
    // private parameter type ensures that even the public synthetic constructor is useless
    // outside LineMarkup and nested classes
    type: Type
): Serializable {

    @JvmField val isArrow = type === Type.ARROW

    private enum class Type { LINE, ARROW }

    companion object {

        @JvmStatic
        fun lineMarkup(x1: Int, y1: Int, x2: Int, y2: Int) = lineMarkup(GoPoint(x1, y1), GoPoint(x2, y2))

        @JvmStatic
        fun lineMarkup(a: GoPoint, b: GoPoint): LineMarkup {
            if (a == b)
                throw IllegalArgumentException("start and end points cannot be the same")
            val p1: GoPoint
            val p2: GoPoint
            if (a > b) {
                p1 = b
                p2 = a
            } else {
                p1 = a
                p2 = b
            }
            return LineMarkup(p1, p2, Type.LINE)
        }

        @JvmStatic
        fun arrowMarkup(x1: Int, y1: Int, x2: Int, y2: Int) = arrowMarkup(GoPoint(x1, y1), GoPoint(x2, y2))

        @JvmStatic
        fun arrowMarkup(start: GoPoint, end: GoPoint): LineMarkup {
            if (start == end)
                throw IllegalArgumentException("start and end points cannot be the same")
            return LineMarkup(start, end, Type.ARROW)
        }

        private const val serialVersionUID = 1L

    }

    val x1: Int @JvmName("x1") get() = start.x
    val y1: Int @JvmName("y1") get() = start.y
    val x2: Int @JvmName("x2") get() = end.x
    val y2: Int @JvmName("y2") get() = end.y

    operator fun component1(): GoPoint = start
    operator fun component2(): GoPoint = end
    operator fun component3(): Boolean = isArrow

    override fun equals(other: Any?): Boolean {
        return this === other || (other is LineMarkup &&
                start == other.start && end == other.end && isArrow == other.isArrow)
    }

    override fun hashCode(): Int {
        var hash = start.hashCode()*(52*52) + end.hashCode()
        if (isArrow) hash += 52*52*52*52
        return hash
    }

    @Transient
    private var string: String? = null

    override fun toString(): String {
        return string ?: "$start${if (isArrow) "->" else "-"}$end".also { string = it }
    }

    private fun readObject(input: ObjectInputStream) {
        input.defaultReadObject()
        if (start == end) throw InvalidObjectException("start and end points cannot be the same")
    }

    private fun readResolve(): Any {
        return if (isArrow || start < end) this else LineMarkup(end, start, Type.LINE)
    }

}

@OptIn(ExperimentalContracts::class)
fun LineMarkupSet?.isNullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@isNullOrEmpty != null)
    }
    return this?.isEmpty() != false
}

class LineMarkupSet: MutableIterable<LineMarkup> {

    companion object {

        /** Updates [LineMarkupSet.count] */
        private val COUNT =
            atomicIntUpdater<LineMarkupSet>("count")
        /** Updates [LineMarkupSet.startMap] */
        private val START_MAP =
            atomicUpdater<LineMarkupSet, MutableGoPointMap<WeakMap>?>("startMap")

    }

    @Volatile
    @get:JvmName("count")
    var count: Int = 0; private set
    @Volatile
    private var startMap: MutableGoPointMap<WeakMap>? = null
    private var weakStartMap: WeakReference<MutableGoPointMap<WeakMap>>? = null
    private val queue = ReferenceQueue<GoPointMap<LineMarkup>>()

    private fun getOrCreateStartMap(): MutableGoPointMap<WeakMap> {
        var map = startMap
        if (map != null) return map
        val weakMap = weakStartMap
        if (weakMap != null) {
            map = weakMap.get()
            if (map != null) {
                startMap = map
                return map
            }
        }
        map = MutableGoPointMap()
        while(!START_MAP.compareAndSet(this, null, map)) {
            val m = startMap
            if (m != null) return m
        }
        weakStartMap = WeakReference(map)
        return map
    }

    private fun getOrCreateEndMap(startMap: MutableGoPointMap<WeakMap>,
                                  start: GoPoint): MutableGoPointMap<LineMarkup> {
        val weakMap = startMap[start]
        var endMap: MutableGoPointMap<LineMarkup>?
        if (weakMap != null) {
            endMap = weakMap.endMap
            if (endMap == null) {
                endMap = weakMap.get()
                if (endMap != null) {
                    weakMap.endMap = endMap
                    return endMap
                }
            }
        }
        endMap = MutableGoPointMap()
        startMap[start] = WeakMap(start, endMap, queue)
        return endMap
    }

    private class WeakMap(
        val start: GoPoint,
        endMap: MutableGoPointMap<LineMarkup>,
        queue: ReferenceQueue<in MutableGoPointMap<LineMarkup>>
    ): WeakReference<MutableGoPointMap<LineMarkup>>(endMap, queue) {

        var endMap: MutableGoPointMap<LineMarkup>? = endMap

    }

    private fun expungeStaleSlots() {
        var startMap = this.startMap
        if (startMap == null) {
            weakStartMap?.let { weak ->
                startMap = weak.get()
                if (startMap == null)
                    weakStartMap = null
            }
        }
        var ref = queue.poll()
        while(ref != null) {
            if (ref is WeakMap) {
                startMap?.remove(ref.start)
            }
            ref = queue.poll()
        }
    }

    fun isEmpty() = count == 0

    fun isNotEmpty() = !isEmpty()

    override fun iterator(): MutableIterator<LineMarkup> {
        return object: MutableIterator<LineMarkup> {

            val metaItr: MutableIterator<WeakMap>? = startMap?.values?.iterator()
            var currentMap: WeakMap? = null
            var currentItr: MutableIterator<LineMarkup>? = null

            fun nextItr(): Iterator<LineMarkup>? {
                var currentItr: MutableIterator<LineMarkup>
                this.currentItr?.let {
                    if (it.hasNext()) return it
                    currentItr = it
                }
                metaItr?.let { metaItr ->
                    while(metaItr.hasNext()) {
                        val currentMap = metaItr.next()
                        this.currentMap = currentMap
                        val endMap = currentMap.endMap
                        if (endMap != null) {
                            currentItr = endMap.values.iterator()
                            this.currentItr = currentItr
                            if (currentItr.hasNext())
                                return currentItr
                        }
                    }
                }
                return null
            }

            override fun hasNext(): Boolean {
                return nextItr() != null
            }

            override fun next(): LineMarkup {
                return nextItr()?.next() ?: throw NoSuchElementException()
            }

            override fun remove() {
                val currentItr = this.currentItr ?: throw IllegalStateException()
                currentItr.remove()
                currentMap?.let { currentMap ->
                    val endMap = currentMap.endMap
                    if (endMap != null && endMap.size <= 0)
                        currentMap.endMap = null
                }
                if (COUNT.decrementAndGet(this@LineMarkupSet) <= 0)
                    startMap = null
            }

        }
    }

    fun add(markup: LineMarkup): Boolean {
        expungeStaleSlots()
        val (a, b, isArrow) = markup
        val startMap = getOrCreateStartMap()
        var oldValue = getOrCreateEndMap(startMap, a).put(b, markup)
        var delta = if (oldValue == null) 1 else 0
        if (isArrow) {
            if (oldValue != null && oldValue.isArrow) return false
            if (a > b) {
                val weakMap = startMap[b]
                if (weakMap != null) {
                    val endMap = weakMap.endMap
                    if (endMap != null) {
                        oldValue = endMap[a]
                        if (oldValue != null && !oldValue.isArrow) {
                            endMap.remove(a)
                            delta--
                            if (endMap.size == 0) weakMap.endMap = null
                        }
                    }
                }
            }
        } else {
            if (oldValue != null && !oldValue.isArrow) return false
            val weakMap = startMap[b]
            if (weakMap != null) {
                val endMap = weakMap.endMap
                if (endMap != null) {
                    oldValue = endMap[a]
                    if (oldValue != null && oldValue.isArrow) {
                        endMap.remove(a)
                        delta--
                        if (endMap.size == 0) weakMap.endMap = null
                    }
                }
            }
        }
        if (delta != 0) COUNT.addAndGet(this, delta)
        return true
    }

    @Suppress("unused")
    fun addAll(other: LineMarkupSet) {
        expungeStaleSlots()
        if (this === other) return
        other.expungeStaleSlots()
        var startMap: MutableGoPointMap<WeakMap>? = null
        val otherStart = other.startMap ?: return
        for(startEntry in otherStart) {
            val a = startEntry.key
            val otherMap = startEntry.value.endMap ?: continue
            if (startMap == null) startMap = getOrCreateStartMap()
            var endMap = getOrCreateEndMap(startMap, a)
            var delta = endMap.size
            endMap.putAll(otherMap)
            delta = endMap.size - delta
            for(markup in otherMap.values) {
                val weak = startMap[markup.end] ?: continue
                endMap = weak.endMap ?: continue
                val reverse = endMap[markup.start]
                if (reverse != null && !(markup.isArrow && reverse.isArrow)) {
                    endMap.remove(markup.start)
                    delta--
                    if (endMap.size == 0) weak.endMap = null
                }
            }
            if (delta != 0) COUNT.addAndGet(this, delta)
        }
    }

    operator fun get(a: GoPoint, b: GoPoint): LineMarkup? {
        expungeStaleSlots()
        var m = getMarkupExact(a, b, removeLine=false, removeArrow=false)
        if (m != null) return m
        if (a > b) {
            m = getMarkupExact(b, a, removeLine=false, removeArrow=false)
            if (m != null && !m.isArrow) return m
        }
        return null
    }

    fun remove(a: GoPoint, b: GoPoint): LineMarkup? {
        return try {
            var m = getMarkupExact(a, b, removeLine=true, removeArrow=true)
            if (m == null && a > b) {
                m = getMarkupExact(b, a, removeLine=true, removeArrow=false)
                if (m?.isArrow == true) m = null
            }
            m
        } finally {
            expungeStaleSlots()
        }
    }

    private fun getMarkupExact(a: GoPoint, b: GoPoint, removeLine: Boolean, removeArrow: Boolean): LineMarkup? {
        val startMap = this.startMap ?: return null
        val weakMap = startMap[a] ?: return null
        val endMap = weakMap.endMap ?: return null
        val m = endMap[b]
        if (m != null && if (m.isArrow) removeArrow else removeLine) {
            endMap.remove(b)
            if (endMap.size <= 0)
                weakMap.endMap = null
            if (COUNT.decrementAndGet(this) <= 0)
                this.startMap = null
        }
        return m
    }

}