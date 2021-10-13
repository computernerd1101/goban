package com.computernerd1101.goban.test

import com.computernerd1101.goban.sgf.*
import com.computernerd1101.sgf.SGFException
import org.junit.*
import kotlin.test.*
import java.io.IOException

fun readSGFResource(resource: String): GoSGF {
    return try {
        ClassLoader.getSystemResourceAsStream(resource)!!.use { input ->
            GoSGF(input)
        }
    } catch(e: IOException) {
        e.printStackTrace()
        null
    } catch(e: SGFException) {
        e.printStackTrace()
        null
    } ?: GoSGF()
}

class GoSGFTest {

    private lateinit var sgf: GoSGF
    private lateinit var root: GoSGFSetupNode
    private lateinit var variation1: Array<GoSGFNode>
    private lateinit var variation2: Array<GoSGFNode>
    private lateinit var variation21: Array<GoSGFNode>
    private lateinit var variation22: Array<GoSGFNode>
    private lateinit var variation221: Array<GoSGFNode>
    private lateinit var variation222: Array<GoSGFNode>

    @Before fun setUp() {
        sgf = readSGFResource("test.sgf")
        root = sgf.rootNode
        variation1 = root.child(0).singleVariation()
        variation2 = root.child(1).singleVariation()
        var node = variation2.last()
        variation21 = node.child(0).singleVariation()
        variation22 = node.child(1).singleVariation()
        node = variation22.last()
        variation221 = node.child(0).singleVariation()
        variation222 = node.child(1).singleVariation()
    }

    private fun GoSGFNode.singleVariation(): Array<GoSGFNode> = generateSequence(this) {
        if (it.children == 1) it.child(0) else null
    }.toList().toTypedArray()

    @Test fun children() {
        assertEquals(2, root.children)
        assertEquals(3, variation1.size)
        assertEquals(0, variation1.last().children)
        assertEquals(4, variation2.size)
        assertEquals(2, variation2.last().children)
        assertEquals(4, variation21.size)
        assertEquals(0, variation21.last().children)
        assertEquals(2, variation22.size)
        assertEquals(2, variation22.last().children)
        assertEquals(2, variation221.size)
        assertEquals(0, variation221.last().children)
        assertEquals(2, variation222.size)
        assertEquals(0, variation222.last().children)
    }

    @Test fun tree() {
        assertSame(sgf, root.tree)
        for(node in variation1 + variation2 + variation21 + variation22 + variation221 + variation222) {
            assertSame(sgf, node.tree)
        }
    }

    @Test fun parent() {
        assertNull(root.parent)
        for(i in variation1.indices)
            assertSame(if (i == 0) root else variation1[i - 1], variation1[i].parent)
        for(i in variation2.indices)
            assertSame(if (i == 0) root else variation2[i - 1], variation2[i].parent)
        for(i in variation21.indices)
            assertSame(if (i == 0) variation2.last() else variation21[i - 1], variation21[i].parent)
        for(i in variation22.indices)
            assertSame(if (i == 0) variation2.last() else variation22[i - 1], variation22[i].parent)
        for(i in variation221.indices)
            assertSame(if (i == 0) variation22.last() else variation221[i - 1], variation221[i].parent)
        for(i in variation222.indices)
            assertSame(if (i == 0) variation22.last() else variation222[i - 1], variation222[i].parent)
    }

    @Test fun index() {
        assertEquals(0, root.index)
        for(i in variation1.indices) assertEquals(1 + i, variation1[i].index)
        for(i in variation2.indices) assertEquals(1 + i, variation2[i].index)
        for(i in variation21.indices) assertEquals(5 + i, variation21[i].index)
        for(i in variation22.indices) assertEquals(5 + i, variation22[i].index)
        for(i in variation221.indices) assertEquals(7 + i, variation221[i].index)
        for(i in variation222.indices) assertEquals(7 + i, variation222[i].index)
    }

    @Test fun childIndex() {
        assertEquals(0, root.childIndex)
        for(node in variation1) assertEquals(0, node.childIndex)
        var expected = 1
        for(node in variation2) {
            assertEquals(expected, node.childIndex)
            expected = 0
        }
        for(node in variation21) assertEquals(0, node.childIndex)
        expected = 1
        for(node in variation22) {
            assertEquals(expected, node.childIndex)
            expected = 0
        }
        for(node in variation221) assertEquals(0, node.childIndex)
        expected = 1
        for(node in variation222) {
            assertEquals(expected, node.childIndex)
            expected = 0
        }
    }

    @Test fun delete() {
        variation22[1].delete()
        assertSame(sgf, root.tree)
        assertNull(root.parent)
        for(i in variation1.indices) {
            val node = variation1[i]
            assertSame(sgf, node.tree)
            assertSame(if (i == 0) root else variation1[i - 1], node.parent)
            assertEquals(if (i == 2) 0 else 1, node.children)
            assertEquals(1 + i, node.index)
            assertEquals(0, node.childIndex)
        }
        for(i in variation2.indices) {
            val node = variation2[i]
            assertSame(sgf, node.tree)
            assertSame(if (i == 0) root else variation2[i - 1], node.parent)
            assertEquals(if (i == 3) 2 else 1, node.children)
            assertEquals(1 + i, node.index)
            assertEquals(if (i == 0) 1 else 0, node.childIndex)
        }
        for(i in variation21.indices) {
            val node = variation21[i]
            assertSame(sgf, node.tree)
            assertSame(if (i == 0) variation2.last() else variation21[i - 1], node.parent)
            assertEquals(if (i == 3) 0 else 1, node.children)
            assertEquals(5 + i, node.index)
            assertEquals(0, node.childIndex)
        }
        assertSame(sgf, variation22[0].tree)
        assertSame(variation2.last(), variation22[0].parent)
        assertEquals(0, variation22[0].children)
        assertEquals(5, variation22[0].index)
        assertEquals(1, variation22[0].childIndex)
        for(node in arrayOf(variation22[1]) + variation221 + variation222) {
            assertNull(node.treeOrNull)
            assertNull(node.parent)
            assertEquals(0, node.children)
            assertEquals(0, node.index)
            assertEquals(0, node.childIndex)
        }
    }

}