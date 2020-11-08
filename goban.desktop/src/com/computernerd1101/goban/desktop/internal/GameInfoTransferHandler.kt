package com.computernerd1101.goban.desktop.internal

import com.computernerd1101.goban.sgf.GameInfo
import com.computernerd1101.sgf.*
import java.awt.*
import java.awt.datatransfer.*
import java.awt.event.*
import java.io.*
import java.nio.*
import java.nio.charset.*
import javax.swing.*

class GameInfoTransferHandler(
    val component: JComponent,
    var charset: Charset? = null,
    val isSelected: (GameInfoTransferHandler.(JComponent) -> Boolean)? = null
): TransferHandler(), MouseListener {

    var gameInfo: GameInfo? = null

    override fun mousePressed(e: MouseEvent) {
        if (gameInfo != null && isSelected?.invoke(this, component) != false) {
            isDragging = true
            exportAsDrag(component, e, COPY)
        }
    }

    override fun mouseReleased(e: MouseEvent?) = Unit

    override fun mouseClicked(e: MouseEvent?) = Unit

    override fun mouseEntered(e: MouseEvent?) = Unit

    override fun mouseExited(e: MouseEvent?) = Unit

    var isDragging: Boolean = false; private set

    override fun exportDone(source: JComponent?, data: Transferable?, action: Int) {
        isDragging = false
        super.exportDone(source, data, action)
    }

    override fun createTransferable(c: JComponent?): Transferable? {
        return gameInfo?.let { info ->
            GameInfoTransferable(
                info,
                charset
            )
        }
    }

    override fun getSourceActions(c: JComponent?) = COPY

    var clipboardContents: GameInfo?
        get() {
            val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val contents: Transferable = clipboard.getContents(this) ?: return null
            val flavor: DataFlavor = when {
                contents.isDataFlavorSupported(gameInfoFlavor) -> gameInfoFlavor
                contents.isDataFlavorSupported(serializedGameInfoFlavor) -> serializedGameInfoFlavor
                else -> return null
            }
            return try {
                contents.getTransferData(flavor)
            } catch(e: UnsupportedFlavorException) {
                null
            } as? GameInfo
        }
        set(info) {
            if (info != null) {
                val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
                exportToClipboard(component, clipboard, COPY)
            }
        }

    companion object {

        @JvmField
        val gameInfoFlavor =
            dataFlavor(
                DataFlavor.javaJVMLocalObjectMimeType +
                        ";class=\"com.computernerd1101.goban.sgf.GameInfo\""
            )
        @JvmField
        val serializedGameInfoFlavor =
            dataFlavor(
                DataFlavor.javaSerializedObjectMimeType +
                        ";class=\"com.computernerd1101.goban.sgf.GameInfo\""
            )

        private fun dataFlavor(mimeType: String) = try {
            DataFlavor(mimeType)
        } catch(e: ClassNotFoundException) {
            throw NoClassDefFoundError(e.message)
        }

        private val repClasses = arrayOf(
            Serializable::class.java,
            GameInfo::class.java,
            Any::class.java,
            InputStream::class.java,
            ByteArrayInputStream::class.java,
            ByteArray::class.java,
            ByteBuffer::class.java,
            String::class.java,
            CharArray::class.java,
            CharBuffer::class.java,
            Reader::class.java,
            StringReader::class.java,
            StringBuilder::class.java,
            StringBuffer::class.java
        )

        private val flavors = arrayOf(
            gameInfoFlavor,
            serializedGameInfoFlavor,
            dataFlavor(
                "text/plain;class=\"java.lang.String\""
            ),
            dataFlavor(
                "application/x-go-sgf;class=\"java.lang.String\""
            ),
            "text/plain;class=\"java.io.InputStream\"",
            "application/x-go-sgf;class=\"java.io.InputStream\"",
            "text/plain;class=\"[B\"",
            "application/x-go-sgf;class=\"[B\"",
            "text/plain;class=\"java.nio.ByteBuffer\"",
            "application/x-go-sgf;class=\"java.nio.ByteBuffer\"",
            dataFlavor(
                "text/plain;class=\"java.io.Reader\""
            ),
            dataFlavor(
                "application/x-go-sgf;class=\"java.io.Reader\""
            ),
            dataFlavor(
                "text/plain;class=\"[C\""
            ),
            dataFlavor(
                "application/x-go-sgf;class=\"[C\""
            ),
            dataFlavor(
                "text/plain;class=\"java.nio.CharBuffer\""
            ),
            dataFlavor(
                "application/x-go-sgf;class=\"java.nio.CharBuffer\""
            ),
            dataFlavor(
                "text/plain;class=\"java.lang.StringBuilder\""
            ),
            dataFlavor(
                "application/x-go-sgf;class=\"java.lang.StringBuilder\""
            ),
            dataFlavor(
                "text/plain;class=\"java.lang.StringBuffer\""
            ),
            dataFlavor(
                "application/x-go-sgf;class=\"java.lang.StringBuffer\""
            )
        )

    }

    private class GameInfoTransferable(
        val info: GameInfo,
        val charset: Charset? = null
    ): Transferable {

        override fun getTransferData(flavor: DataFlavor): Any {
            val isSGF = flavor.subType == "x-go-sgf"
            return when(val repClass: Class<*> = flavor.representationClass) {
                GameInfo::class.java, Serializable::class.java,
                Any::class.java -> info
                InputStream::class.java, ByteArrayInputStream::class.java,
                ByteArray::class.java, ByteBuffer::class.java -> {
                    val charset = flavor.getParameter("charset")?.let { enc ->
                        try {
                            Charset.forName(enc)
                        } catch(e: IllegalCharsetNameException) {
                            null
                        } catch(e: UnsupportedCharsetException) {
                            null
                        }
                    } ?: this.charset
                    val output = ByteArrayOutputStream()
                    val node = SGFNode()
                    if (isSGF) {
                        output.write('('.toInt())
                        writeRoot(node, charset)
                    }
                    info.writeSGFNode(node, charset)
                    node.write(output)
                    if (isSGF) output.write(')'.toInt())
                    val bytes = output.toByteArray()
                    when(repClass) {
                        ByteArray::class.java -> bytes
                        ByteBuffer::class.java -> ByteBuffer.wrap(bytes)
                        else -> bytes.inputStream()
                    }
                }
                String::class.java, CharArray::class.java, CharBuffer::class.java,
                Reader::class.java, StringReader::class.java,
                StringBuilder::class.java, StringBuffer::class.java -> {
                    val buffer: Appendable = if (repClass == StringBuffer::class.java) StringBuffer()
                    else StringBuilder()
                    val node = SGFNode()
                    if (isSGF) {
                        buffer.append('(')
                        writeRoot(node, null)
                    }
                    info.writeSGFNode(node, Charsets.UTF_8)
                    buffer.append(node.toString())
                    if (isSGF) buffer.append(')')
                    if (repClass == StringBuilder::class.java || repClass == StringBuffer::class.java)
                        buffer
                    else {
                        val str = buffer.toString()
                        when(repClass) {
                            CharArray::class.java -> str.toCharArray()
                            CharBuffer::class.java -> CharBuffer.wrap(str)
                            Reader::class.java, StringReader::class.java -> str.reader()
                            else -> str
                        }
                    }
                }
                else -> throw UnsupportedFlavorException(flavor)
            }
        }

        private fun writeRoot(node: SGFNode, charset: Charset?) {
            val props = node.properties
            val bytes = SGFBytes(1)
            bytes.append('1'.toByte())
            props["GM"] = SGFProperty(SGFValue(bytes.clone()))
            bytes[0] = '4'.toByte()
            props["FF"] = SGFProperty(SGFValue(bytes))
            if (charset != null)
                props["CA"] = SGFProperty(SGFValue(charset.name(), null))
            props["AP"] = SGFProperty(SGFValue("CN13 Goban", charset))
        }

        override fun getTransferDataFlavors() = Array(flavors.size) { index ->
            when(val flavor = flavors[index]) {
                is DataFlavor -> flavor
                else -> DataFlavor(
                    "$flavor;charset=${charset?.name() ?: "UTF-8"}"
                )
            }
        }

        override fun isDataFlavorSupported(flavor: DataFlavor) = flavor.representationClass in repClasses

    }

}