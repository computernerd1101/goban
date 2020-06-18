@file:JvmName("Util")

package com.computernerd1101.sgf

import java.nio.charset.Charset
import java.util.*

@Suppress("SpellCheckingInspection")
private const val sgfChars = "\n\r ()-.0123456789:;ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]abcdefghijklmnopqrstuvwxyz"
private val sgfBytes = sgfChars.toByteArray()

private val sgfCharsets = WeakHashMap<Charset, Boolean>()

val Charset.isValidSGF: Boolean
    @JvmName("isValidSGFCharset")
    get() = sgfCharsets[this] ?: try {
        sgfChars.toByteArray(this) contentEquals sgfBytes &&
                sgfChars == String(sgfBytes, this)
    } catch(e: Exception) {
        false
    }.also { valid -> sgfCharsets[this] = valid }

fun SGFBytes.escape(copy: Boolean): SGFBytes {
    val ba = toByteArray()
    var start = 0
    var b: Byte
    while(true) {
        if (start >= ba.size) return when {
            !copy -> this
            ba.isEmpty() -> clone()
            else -> SGFBytes(ba)
        }
        b = ba[start]
        if (b == '\\'.toByte() || b == ']'.toByte() || b == ':'.toByte())
            break
        start++
    }
    val bytes = if (copy) {
        SGFBytes(ba.size).append(ba, 0, start)
    } else {
        delete(start, size)
        this
    }
    bytes.append('\\'.toByte())
    for(end in (start + 1) until ba.size) {
        b = ba[end]
        if (b == '\\'.toByte() || b == ']'.toByte() || b == ':'.toByte()) {
            bytes.append(ba, start, end)
            bytes.append('\\'.toByte())
            start = end
        }
    }
    bytes.append(ba, start, ba.size)
    return bytes
}

@Suppress("unused")
fun SGFBytes.escape(): ByteArray {
    val ba = toByteArray()
    var size = ba.size
    var start = -1
    var b: Byte
    for(i in ba.indices) {
        b = ba[i]
        if (b != '\\'.toByte() && b != ']'.toByte() && b != ':'.toByte())
            continue
        if (start < 0) start = i
        size++
    }
    if (start < 0) return ba
    val r = ByteArray(size)
    if (start != 0) ba.copyInto(r, endIndex=start)
    r[start] = '\\'.toByte()
    var n = start + 1
    for(end in (start + 1) until ba.size) {
        b = ba[end]
        if (b != '\\'.toByte() && b != ']'.toByte() && b != ':'.toByte())
            continue
        ba.copyInto(r, n, start, end)
        val range = end - start
        r[n + range] = '\\'.toByte()
        n += range + 1
        start = end
    }
    ba.copyInto(r, n, start, ba.size)
    return r
}