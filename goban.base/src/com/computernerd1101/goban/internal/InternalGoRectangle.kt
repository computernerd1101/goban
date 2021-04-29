package com.computernerd1101.goban.internal

import com.computernerd1101.goban.*

internal object InternalGoRectangle {

    fun rowBits(rect: GoRectangle): Long = (2L shl rect.end.x) - (1L shl rect.start.x)

    // Saves time and memory compared to conventional string concatenation.
    fun toString(start: GoPoint, end: GoPoint): String {
        val (x1, y1) = start
        val (x2, y2) = end
        val (offset, buffer) = if ((x1 == x2 && y1 + 1 == y2) || (x1 + 1 == x2 && y1 == y2))
            buffer2
        else bufferTruncate
        buffer[1] = x1.toGoPointChar()
        buffer[2] = y1.toGoPointChar()
        buffer[offset] = x2.toGoPointChar()
        buffer[offset + 1] = y2.toGoPointChar()
        return String(buffer)
    }

    private class ThreadLocalBuffer(val offset: Int): ThreadLocal<CharArray>() {

        operator fun component1(): Int = offset
        operator fun component2(): CharArray = get()

        override fun initialValue(): CharArray {
            val buffer = CharArray(offset + 3)
            buffer[0] = '['
            // buffer[1] = x1
            // buffer[2] = y1
            buffer[3] = ','
            buffer[4] = ' '
            if (offset == 10) {
                // "..., "
                buffer[5] = '.'
                buffer[6] = '.'
                buffer[7] = '.'
                buffer[8] = ','
                buffer[9] = ' '
            }
            // buffer[offset] = x2
            // buffer[offset + 1] = y2
            buffer[offset + 2] = ']'
            return buffer
        }

    }

    private val buffer2 = ThreadLocalBuffer(5)

    private val bufferTruncate = ThreadLocalBuffer(10)

}