package com.computernerd1101.sgf.internal

import com.computernerd1101.sgf.*
import java.io.OutputStream

sealed class SGFWriter {

    abstract fun write(b: Int)

    abstract fun write(s: String)

    abstract fun write(bytes: SGFBytes)

    class StringWriter(private val buffer: StringBuilder): SGFWriter() {

        override fun write(b: Int) {
            buffer.append(b.toChar())
        }

        override fun write(s: String) {
            buffer.append(s)
        }

        override fun write(bytes: SGFBytes) {
            buffer.append(bytes)
        }

    }

    class IOWriter(private val os: OutputStream): SGFWriter() {

        override fun write(b: Int) {
            os.write(b)
        }

        override fun write(s: String) {
            val len = s.length
            val bytes = ByteArray(len)
            for(i in 0 until len) {
                var ch = s[i]
                if (ch > 0xFF.toChar()) ch = '?'
                bytes[i] = ch.code.toByte()
            }
            os.write(bytes)
        }

        override fun write(bytes: SGFBytes) {
            os.write(bytes.toByteArray())
        }

    }

    fun writeTree(tree: SGFTree, tab: Int) {
        write('('.code)
        var hasPrev = false
        for (node in tree.nodes) {
            if (hasPrev) write(' '.code)
            else hasPrev = true
            writeNode(node)
            write('\n'.code)
            writeTab(tab)
        }
        for (subTree in tree.subTrees) {
            write(' '.code)
            write(' '.code)
            writeTree(subTree, tab + 1)
            write('\n'.code)
            writeTab(tab)
        }
        write(')'.code)
    }

    private fun writeTab(tab: Int) {
        for(i in 0 until tab) {
            write(' '.code)
            write(' '.code)
        }
    }

    fun writeNode(node: SGFNode) {
        write(';'.code)
        for(property in node.properties) {
            write(property.key)
            writeProperty(property.value)
        }
    }

    fun writeProperty(prop: SGFProperty) {
        for(value in prop.values) {
            writeValue(value)
        }
    }

    fun writeValue(value: SGFValue) {
        var sep = '['.code
        for(bytes in value.parts) {
            write(sep)
            sep = ':'.code
            write(bytes.escape(copy = true))
        }
        write(']'.code)
    }


}