package com.computernerd1101.goban.internal

import com.computernerd1101.goban.*
import java.util.concurrent.atomic.*

internal object InternalGoPointSet {

    interface Secrets {
        fun init(rows: AtomicLongArray): GoPointSet
        fun rows(set: GoPointSet): AtomicLongArray
    }
    var secrets: Secrets by SecretKeeper { GoPointSet }

    interface MutableSecrets {
        fun init(rows: AtomicLongArray): MutableGoPointSet
    }
    var mutableSecrets: MutableSecrets by SecretKeeper { MutableGoPointSet }

    /** Updates [GoPointSet.sizeAndHash] */
    lateinit var sizeAndHash: AtomicLongFieldUpdater<GoPointSet>

    fun toLongArray(elements: Array<out Iterable<GoPoint>>): AtomicLongArray {
        val rows = AtomicLongArray(52)
        var secrets: Secrets? = null
        for(element in elements) when(element) {
            is GoPoint -> {
                val y = element.y
                // rows has just been created and is not yet visible to any other threads
                rows[y] = rows[y] or (1L shl element.x)
            }
            is GoRectangle -> {
                val (x1, y1) = element.start
                val (x2, y2) = element.end
                val mask = (1L shl (x2 + 1)) - (1L shl x1)
                for(y in y1..y2)
                    rows[y] = rows[y] or mask
            }
            is GoPointSet -> {
                if (secrets == null) secrets = this.secrets
                val rows2 = secrets.rows(element)
                for(y in 0..51)
                    rows[y] = rows[y] or rows2[y]
            }
            is GoPointKeys<*> -> for(y in 0..51)
                rows[y] = rows[y] or element.rowBits(y)
            else -> for((x, y) in element)
                rows[y] = rows[y] or (1L shl x)
        }
        return rows
    }

    fun sizeAndHash(rows: AtomicLongArray): Long  {
        var words = 0L
        for(y in 0..51)
            words += sizeAndHash(rows[y], y)
        return words
    }

    fun sizeAndHash(row: Long, y: Int): Long {
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

}

internal open class GoPointItr(val rows: AtomicLongArray) : Iterator<GoPoint> {

    private var unseenX = rows[0]
    private var unseenY = 0
    @JvmField var lastReturned: GoPoint? = null

    override fun hasNext(): Boolean {
        var unseen = unseenX
        var y = unseenY
        while(unseen == 0L && y < 51)
            unseen = rows[++y]
        unseenX = unseen
        unseenY = y
        return unseen != 0L
    }

    override fun next(): GoPoint {
        var unseen = unseenX
        var y = unseenY
        while(unseen == 0L && y < 51)
            unseen = rows[++y]
        val xBit = unseen and -unseen
        unseenX = unseen - xBit
        unseenY = y
        val x = if (xBit != 0L) trailingZerosPow2(xBit) else throw NoSuchElementException()
        val p = GoPoint(x, y)
        lastReturned = p
        return p
    }

}