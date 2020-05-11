package com.computernerd1101.sgf.internal

import com.computernerd1101.sgf.*
import java.io.InputStream
import kotlin.addSuppressed as suppress

sealed class SGFReader(val warnings: SGFWarningList) {

    var row = 1
    var column = 1
    private var lastBreak = 0
    var lastRead = 0

    abstract fun readImpl(): Int

    abstract fun readCodePoint(): Int

    fun read() = read(false)

    fun skipSpaces() = read(true)

    private fun read(skipSpaces: Boolean): Int {
        var oldBreak = lastBreak
        var r = row
        var c = column
        while (true) {
            var b = readImpl()
            var newLine = false
            var lineBreak = 0
            if (b == '\r'.toInt()) {
                lineBreak = '\r'.toInt()
                if (oldBreak == '\n'.toInt()) {
                    lineBreak = 0
                    b = '\n'.toInt()
                } else newLine = true
            } else if (b == '\n'.toInt()) {
                lineBreak = '\n'.toInt()
                if (oldBreak == '\r'.toInt()) lineBreak = 0
                else newLine = true
            } else c++
            if (newLine) {
                r++
                c = 1
            }
            row = r
            column = c
            oldBreak = lineBreak
            lastBreak = oldBreak
            if (!skipSpaces || b < 0 || b > ' '.toInt()) return b
        }
    }

    fun newException(expected: String): SGFException {
        val cp = readCodePoint()
        val eof = cp < 0
        val sb = StringBuilder().append("Expected ").append(expected)
        if (eof) {
            sb.append(" before end of file")
        } else {
            sb.append(" but found ")
            val showChar = if (cp >= ' '.toInt() && cp != 0x7F &&
                (cp !in Character.MIN_SURROGATE.toInt()..Character.MAX_SURROGATE.toInt())) {
                sb.append('\'')
                if (Character.isSupplementaryCodePoint(cp)) {
                    sb.append(Character.highSurrogate(cp)).append(Character.lowSurrogate(cp))
                } else {
                    sb.append(cp.toChar())
                }
                sb.append('\'')
                true
            } else false
            if (cp !in ' '.toInt() until 0x7F) {
                if (showChar) sb.append(" (")
                sb.append("U+").append(Integer.toHexString(cp))
                if (showChar) sb.append(')')
            }
        }
        return newException(sb.toString(), eof)
    }

    open fun newException(message: String, eof: Boolean) =
        SGFException(row, column, message)

    class IOReader(private val stream: InputStream, warnings: SGFWarningList):
        SGFReader(warnings) {

        private var suppressed: Throwable? = null
        private var alreadyDecoded: Boolean = false

        override fun readImpl(): Int {
            alreadyDecoded = false
            val read = stream.read()
            lastRead = read
            return read
        }

        override fun readCodePoint(): Int {
            suppressed = null
            var b = lastRead
            if (alreadyDecoded) return b
            val ch1: Int = readUTF8(b)
            alreadyDecoded = true
            if (ch1 !in Character.MIN_HIGH_SURROGATE.toInt()..Character.MAX_HIGH_SURROGATE.toInt()) {
                lastRead = ch1
                return ch1
            }
            try {
                b = readImpl()
            } catch (x: Throwable) {
                suppressed = x
                return ch1
            }
            val ch2: Int = readUTF8(b)
            if (ch2 in Character.MIN_LOW_SURROGATE.toInt()..Character.MAX_LOW_SURROGATE.toInt()) {
                return Character.toCodePoint(ch1.toChar(), ch2.toChar()).also { lastRead = it }
            }
            lastRead = ch2
            return ch1
        }

        private fun readUTF8(b: Int): Int {
            if (b < 0x80) return b
            val size: Int
            var ch: Int
            when {
                b and 0xE0 == 0xC0 -> {
                    size = 2
                    ch = b and 0x1F
                }
                b and 0xF0 == 0xE0 -> {
                    size = 3
                    ch = b and 0xF
                }
                b and 0xF8 == 0xF0 -> {
                    size = 4
                    ch = b and 7
                }
                else -> return b
            }
            for (i in 1 until size) {
                var next: Int
                try {
                    next = readImpl()
                } catch (x: Throwable) {
                    suppressed = x
                    return b
                }
                ch = (ch shl 6) or (next and 0x3F)
            }
            return ch
        }

        override fun newException(message: String, eof: Boolean): SGFException {
            val e = super.newException(message, eof)
            val s = suppressed
            if (s != null) e.suppress(s)
            return e
        }

    }

    class StringReader(private val src: String, warnings: SGFWarningList):
        SGFReader(warnings) {

        private var pos: Int = 0
        private var codePoint: Int = 0

        private val utfBuf = ByteArray(3)
        private var utfPos: Int = 3

        override fun readImpl(): Int {
            val utf = utfBuf
            var p = utfPos
            if (p < 3) {
                utfPos = p + 1
                val read = utf[p].toInt() and 0xFF
                lastRead = read
                return read
            }
            val s = src
            p = pos
            if (p >= s.length) {
                lastRead = -1
                return -1
            }
            var cp = s.codePointAt(p)
            pos = p + if (Character.isSupplementaryCodePoint(cp)) 2 else 1
            codePoint = cp
            if (cp < 0x80) {
                lastRead = cp
                return cp
            }
            val flags: Int
            when {
                cp < 0x800 -> {
                    p = 2
                    flags = 0xC0
                }
                cp < 0x10000 -> {
                    p = 1
                    flags = 0xE0
                }
                else -> {
                    p = 0
                    flags = 0xF0
                }
            }
            for (i in 2 downTo p) {
                utf[i] = (cp and 0x3F or 0x80).toByte()
                cp = cp ushr 6
            }
            utfPos = p
            val read = cp or flags
            lastRead = read
            return read
        }

        override fun readCodePoint(): Int {
            return codePoint
        }

    }

    fun readNode(): SGFNode {
        val node = SGFNode()
        node.row = row
        node.column = column - 1
        val map = node.properties
        var ch = skipSpaces()
        var r = row
        var c = column - 1
        while(ch in 'A'.toInt()..'Z'.toInt() || ch in 'a'.toInt()..'z'.toInt()) {
            val name = readPropertyName(ch)
            val prop = readProperty()
            prop.row = r
            prop.column = c
            map[name]?.apply {
                list.addAll(prop.list)
            } ?: map.put(name, prop)
            ch = lastRead
            r = row
            c = column - 1
        }
        return node
    }

    private fun readPropertyName(firstLetter: Int): String {
        val sb = StringBuilder()
        var ch = firstLetter
        do {
            if (ch in 'A'.toInt()..'Z'.toInt()) sb.append(ch.toChar())
            ch = read()
        } while (ch in 'A'.toInt()..'Z'.toInt() || ch in 'a'.toInt()..'z'.toInt())
        if (ch <= ' '.toInt()) ch = skipSpaces()
        if (ch != '['.toInt()) throw newException("'['")
        return sb.toString()
    }

    private fun readProperty(): SGFProperty {
        // last returned character is guaranteed to be '['
        var value = readValue()
        val prop = SGFProperty(value)
        val list = prop.list
        while(true) {
            val ch = skipSpaces()
            if (ch != '['.toInt()) return prop
            value = readValue()
            list.add(value)
        }
    }

    private fun readValue(): SGFValue {
        val r = row
        val c = column
        var bytes = SGFBytes()
        bytes.row = r
        bytes.column = c
        val value = SGFValue(bytes)
        value.row =  r
        value.column = c
        val list = value.list
        // previously read character is guaranteed to be '['
        while(true) {
            val ch = readText(bytes)
            if (ch == ']'.toInt()) return value
            // ch == ':'
            bytes = SGFBytes()
            bytes.row = row
            bytes.column = column
            list.add(bytes)
        }
    }

    private fun readText(bytes: SGFBytes): Int {
        var ch = read()
        while(ch != ':'.toInt() && ch != ']'.toInt()) {
            if (ch == '\\'.toInt()) {
                ch = read()
                if (ch == '\r'.toInt()) {
                    ch = read()
                    if (ch == '\n'.toInt()) ch = read()
                    continue
                }
                if (ch == '\n'.toInt()) {
                    ch = read()
                    if (ch == '\r'.toInt()) ch = read()
                    continue
                }
            }
            bytes.append(ch.toByte())
            ch = read()
        }
        return ch
    }

}