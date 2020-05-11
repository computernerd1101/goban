package com.computernerd1101.goban.desktop

import com.computernerd1101.goban.GoColor
import com.computernerd1101.goban.Goban
import com.computernerd1101.goban.GoPoint
import com.computernerd1101.goban.sgf.*
import org.junit.jupiter.api.*

import org.junit.jupiter.api.Assertions.*

class SGFTreeModelTest {

    private val sgf = GoSGF()
    private val node1 = sgf.rootNode.createNextMoveNode(GoPoint(16, 4), GoColor.BLACK)
    private val node11 = node1.createNextMoveNode(GoPoint(4, 16), GoColor.WHITE)
    private val node111 = node11.createNextMoveNode(GoPoint(16, 16), GoColor.BLACK)
    private val node112 = node11.createNextMoveNode(GoPoint(4, 4), GoColor.BLACK)
    private val node12 = node1.createNextMoveNode(GoPoint(16, 16), GoColor.WHITE)
    private val node121 = node12.createNextMoveNode(GoPoint(4, 4), GoColor.BLACK)
    private val node2 = sgf.rootNode.createNextSetupNode(Goban().apply {
        this[4, 16] = GoColor.BLACK
    })
    private val node21 = node2.createNextMoveNode(GoPoint(16, 16), GoColor.WHITE)
    private lateinit var treeModel: SGFTreeModel

    @BeforeEach
    fun setUp() {
        treeModel = SGFTreeModel()
        treeModel.root = sgf
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    fun getIndexOfChild() {
        getIndexOfChild(sgf)
    }

    private fun getIndexOfChild(parent: Any?) {
        for(i in 0 until treeModel.getChildCount(parent)) {
            val child = treeModel.getChild(parent, i)
            assertNotNull(child)
            assertEquals(i, treeModel.getIndexOfChild(parent, child))
            getIndexOfChild(child)
        }
    }

}