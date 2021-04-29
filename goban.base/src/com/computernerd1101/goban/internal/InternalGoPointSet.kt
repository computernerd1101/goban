package com.computernerd1101.goban.internal

import com.computernerd1101.goban.*
import java.util.concurrent.atomic.*

internal object InternalGoPointSet {

    /** Updates [GoPointSet.sizeAndHash] */
    lateinit var sizeAndHash: AtomicLongFieldUpdater<GoPointSet>

    @JvmField val rowUpdaters = arrayOfLateInit<AtomicLongFieldUpdater<GoPointSet>>(52)

    fun checkType(klass: Class<out GoPointSet>) {
        if (klass != GoPointSet::class.java && klass != MutableGoPointSet::class.java)
            throw IllegalAccessError(
                "${klass.name} does not have permission to inherit from com.computernerd1101.goban.GoPointSet"
            )
    }

    fun init(set: GoPointSet, elements: Array<out Iterable<GoPoint>>) {
        for(element in elements) when(element) {
            is GoPoint -> {
                val updater = rowUpdaters[element.y]
                // set has just been created and is not yet visible to any other threads
                updater[set] = updater[set] or (1L shl element.x)
            }
            is GoRectangle -> {
                val mask = InternalGoRectangle.rowBits(element)
                for (y in element.start.y..element.end.y) {
                    val updater = rowUpdaters[y]
                    updater[set] = updater[set] or mask
                }
            }
            is GoPointSet -> {
                for(updater in rowUpdaters)
                    updater[set] = updater[set] or updater[element]
            }
            is GoPointKeys<*> -> for (y in 0..51) {
                val updater = rowUpdaters[y]
                updater[set] = updater[set] or element.rowBits(y)
            }
            else -> for((x, y) in element) {
                val updater = rowUpdaters[y]
                updater[set] = updater[set] or (1L shl x)
            }
        }
        sizeAndHash[set] = sizeAndHash(set)
    }

    fun sizeAndHash(set: GoPointSet): Long  {
        var words = 0L
        for(y in 0..51)
            words += sizeAndHash(y, rowUpdaters[y][set])
        return words
    }

    fun sizeAndHash(y: Int, row: Long): Long {
        var words = 0L
        val inc = (52L shl 32)*y + 1L
        var unseen = row
        while(unseen != 0L) {
            val bit = unseen and -unseen
            unseen -= bit
            // add x + 52*y to hash and add 1 to size, broken up as follows:
            //       add x to hash                                     add 52*y to hash and add 1 to size
            words += trailingZerosPow2(bit).toLong().shl(32) + inc
        }
        return words
    }

    fun copyRowFrom(set: GoPointSet, y: Int, newBits: Long): Boolean {
        val oldBits = rowUpdaters[y].getAndSet(set, newBits)
        return if (newBits != oldBits) {
            sizeAndHash.addAndGet(
                set,
                sizeAndHash(y, newBits) -
                        sizeAndHash(y, oldBits)
            )
            true
        } else false
    }

}

internal open class GoPointItr(val set: GoPointSet) : Iterator<GoPoint> {

    private var unseenX = InternalGoPointSet.rowUpdaters[0][set]
    private var unseenY = 0

    override fun hasNext(): Boolean {
        var unseen = unseenX
        var y = unseenY
        while(unseen == 0L && y < 51)
            unseen = InternalGoPointSet.rowUpdaters[++y][set]
        unseenX = unseen
        unseenY = y
        return unseen != 0L
    }

    override fun next(): GoPoint {
        var unseen = unseenX
        var y = unseenY
        while(unseen == 0L && y < 51)
            unseen = InternalGoPointSet.rowUpdaters[++y][set]
        val xBit = unseen and -unseen
        unseenX = unseen - xBit
        unseenY = y
        val x = if (xBit != 0L) trailingZerosPow2(xBit) else throw NoSuchElementException()
        return GoPoint(x, y)
    }

}