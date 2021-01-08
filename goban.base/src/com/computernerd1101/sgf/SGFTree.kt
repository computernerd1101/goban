@file:JvmName("Util")
@file:JvmMultifileClass

package com.computernerd1101.sgf

import com.computernerd1101.sgf.internal.*
import java.io.*

@Suppress("unused")
inline fun SGFTree(node1: SGFNode, vararg nodes: SGFNode, subTrees: SGFTree.() -> Unit): SGFTree {
    val tree = SGFTree(node1, *nodes)
    tree.subTrees()
    return tree
}

class SGFTree: RowColumn, Serializable {

    private var nodeList: Nodes
    private var subTreeList: SubTrees

    val nodes: MutableList<SGFNode>
        @JvmName("nodes") get() = nodeList
    val subTrees: MutableList<SGFTree>
        @JvmName("subTrees") get() = subTreeList

    inline fun subTree(node1: SGFNode, vararg nodes: SGFNode, subTrees: SGFTree.() -> Unit): SGFTree {
        val subTree = subTree(node1, *nodes)
        subTree.subTrees()
        return subTree
    }

    fun subTree(node1: SGFNode, vararg nodes: SGFNode): SGFTree {
        val subTree = SGFTree(node1, *nodes)
        subTreeList.addPrivileged(subTree)
        return subTree
    }

    fun copy(level: SGFCopyLevel? = null) = SGFTree(this, level ?: SGFCopyLevel.NODE)

    private constructor(other: SGFTree, level: SGFCopyLevel) {
        row = other.row
        column = other.column
        nodeList = Nodes(other.nodeList, level)
        subTreeList = SubTrees(other.subTreeList, level)
    }

    constructor(firstNode: SGFNode) {
        nodeList = Nodes(firstNode)
        subTreeList = SubTrees()
    }

    constructor(nodeCapacity: Int, firstNode: SGFNode) {
        nodeList = Nodes(nodeCapacity, firstNode)
        subTreeList = SubTrees()
    }

    constructor(nodeCapacity: Int, firstNode: SGFNode, subTreeCapacity: Int) {
        nodeList = Nodes(nodeCapacity, firstNode)
        subTreeList = SubTrees(subTreeCapacity)
    }

    constructor(nodeCapacity: Int, firstNode: SGFNode, vararg subTrees: SGFTree) {
        nodeList = Nodes(nodeCapacity, firstNode)
        subTreeList = SubTrees(subTrees)
    }

    constructor(node1: SGFNode, vararg nodes: SGFNode) {
        nodeList = Nodes(node1, nodes)
        subTreeList = SubTrees()
    }

    @Suppress("unused")
    constructor(node1: SGFNode, nodes: Array<out SGFNode>, vararg subTrees: SGFTree) {
        nodeList = Nodes(node1, nodes)
        subTreeList = SubTrees(subTrees)
    }

    @Suppress("unused")
    constructor(nodes: Collection<SGFNode>) {
        nodeList = Nodes(nodes)
        subTreeList = SubTrees()
    }

    @Suppress("unused")
    constructor(nodes: Collection<SGFNode>, subTrees: Collection<SGFTree>) {
        nodeList = Nodes(nodes)
        subTreeList = SubTrees(subTrees)
    }

    @Throws(SGFException::class)
    @JvmOverloads
    @Suppress("unused")
    constructor(toParse: String, warnings: SGFWarningList = SGFWarningList()):
            this(SGFReader.StringReader(toParse, warnings).startReading())

    @Throws(IOException::class, SGFException::class)
    @JvmOverloads
    constructor(input: InputStream, warnings: SGFWarningList = SGFWarningList()):
            this(SGFReader.IOReader(input, warnings).startReading())

    private constructor(reader: SGFReader) {
        row = reader.row
        column = reader.column - 1
        var ch = reader.skipSpaces()
        if (ch != ';'.toInt()) throw reader.newException("';'")
        val nodes = Nodes(reader.readNode())
        nodeList = nodes
        while (true) {
            ch = reader.lastRead
            if (ch == '('.toInt()) break
            if (ch == ')'.toInt()) {
                subTreeList = SubTrees()
                return
            }
            if (ch != ';'.toInt()) throw reader.newException("';', '(' or ')'")
            nodes.add(reader.readNode())
        }
        // last read character was '('
        val subTrees = SubTrees(SGFTree(reader))
        subTreeList = subTrees
        ch = reader.skipSpaces()
        while(ch == '('.toInt()) {
            subTrees.addPrivileged(SGFTree(reader))
            ch = reader.skipSpaces()
        }
        if (ch != ')'.toInt()) throw reader.newException("')'")
    }

    override fun toString() = buildString {
        SGFWriter.StringWriter(this).writeTree(this@SGFTree, 0)
    }

    @Throws(IOException::class)
    fun write(os: OutputStream) {
        SGFWriter.IOWriter(os).writeTree(this, 0)
    }

    private fun writeObject(oos: ObjectOutputStream) {
        val fields: ObjectOutputStream.PutField = oos.putFields()
        fields.put("row", row)
        fields.put("column", column)
        val nodes = nodeList.size
        fields.put("nodes", nodes)
        val subTrees = subTreeList.size
        fields.put("subTrees", subTrees)
        oos.writeFields()
        nodeList.write(oos, nodes)
        subTreeList.write(oos, subTrees)
    }

    private fun readObject(ois: ObjectInputStream) {
        val fields: ObjectInputStream.GetField = ois.readFields()
        row = fields["row", 0]
        column = fields["column", 0]
        val nodes = fields["nodes", -1]
        if (nodes < 0) throw InvalidObjectException("nodes require non-negative size")
        val subTrees = fields["subTrees", -1]
        if (subTrees < 0) throw InvalidObjectException("subTrees require non-negative size")
        nodeList = Nodes(ois, nodes)
        subTreeList = SubTrees(ois, nodes)
    }

    companion object {

        private const val serialVersionUID = 1L
        private val serialPersistentFields = arrayOf(
            ObjectStreamField("row", Int::class.java),
            ObjectStreamField("column", Int::class.java),
            ObjectStreamField("nodes", Int::class.java),
            ObjectStreamField("subTrees", Int::class.java)
        )



    }

    private class Nodes: AbstractSGFList<SGFNode> {

        constructor(first: SGFNode): super(1, first)

        constructor(initialCapacity: Int, first: SGFNode): super(initialCapacity, first)

        constructor(first: SGFNode, rest: Array<out SGFNode>): super(first, rest)

        constructor(elements: Collection<SGFNode>): super(elements)

        constructor(other: Nodes, level: SGFCopyLevel): super(other, level)

        constructor(ois: ObjectInputStream, size: Int): super(ois, size)

        override fun newArray(size: Int) = arrayOfNulls<SGFNode>(size)

        override fun emptyListMessage() = "SGF tree must directly contain at least one node."

        override fun optimizedCopy(n: Int, a: Array<SGFNode?>, level: SGFCopyLevel) {
            if (level != SGFCopyLevel.NODE)
                for(i in 0 until n) a[i] = a[i]?.copy(level)
        }

    }

    private class SubTrees: AbstractSGFList<SGFTree> {

        constructor()

        constructor(initialCapacity: Int): super(initialCapacity)

        constructor(first: SGFTree): super(1, first)

        constructor(elements: Array<out SGFTree>): super(elements)

        constructor(elements: Collection<SGFTree>): super(elements)

        constructor(other: SubTrees, level: SGFCopyLevel): super(other, level)

        constructor(ois: ObjectInputStream, size: Int): super(ois, size)

        override fun newArray(size: Int) = arrayOfNulls<SGFTree>(size)

        override fun allowEmpty() = true

        override fun checkAddPrivilege() {
            throw UnsupportedOperationException()
        }

        override fun addNew(e: SGFTree) = e.copy()

        override fun optimizedCopy(n: Int, a: Array<SGFTree?>, level: SGFCopyLevel) {
            for(i in 0 until n) a[i] = a[i]?.copy(level)
        }

    }

}