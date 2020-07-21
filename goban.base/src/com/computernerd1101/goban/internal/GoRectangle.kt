package com.computernerd1101.goban.internal

import com.computernerd1101.goban.*

internal object InternalGoRectangle {

    // No need for SecretKeeper, because the only accessors are GoRectangle itself and GoPoint.
    // The companion object of the latter automatically initializes that of the former.
    lateinit var init: (GoPoint, GoPoint, String) -> GoRectangle

    // Saves time and memory compared to conventional string concatenation.
    fun toString(start: GoPoint, end: GoPoint): String {
        val (x1, y1) = start
        val (x2, y2) = end
        val threadLocalBuffer = if ((x1 == x2 && y1 + 1 == y2) || (x1 + 1 == x2 && y1 == y2))
            buffer2
        else bufferTruncate
        val buffer: CharArray = threadLocalBuffer.get()
        val offset = threadLocalBuffer.offset
        buffer[1] = x1.toGoPointChar()
        buffer[2] = y1.toGoPointChar()
        buffer[offset] = x2.toGoPointChar()
        buffer[offset + 1] = y2.toGoPointChar()
        return String(buffer)
    }

    private class ThreadLocalBuffer(val offset: Int): ThreadLocal<CharArray>() {
        override fun initialValue(): CharArray {
            val buffer = CharArray(offset + 3)
            buffer[0] = '['
            // buffer[1] = x1
            // buffer[2] = y1
            buffer[3] = ','
            buffer[4] = ' '
            if (offset == 10) {
                buffer[5] = '.'
                buffer[6] = '.'
                buffer[7] = '.'
                buffer[8] = ','
                buffer[9] = ' '
                // buffer[10] = x2
                // buffer[11] = y2
                // buffer[12] = ']'
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