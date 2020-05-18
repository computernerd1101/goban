package com.computernerd1101.goban.sgf

import com.computernerd1101.goban.*
import com.computernerd1101.goban.internal.*
import com.computernerd1101.goban.markup.*
import com.computernerd1101.goban.sgf.internal.*
import com.computernerd1101.sgf.*
import java.io.*
import java.nio.charset.Charset
import java.util.function.IntBinaryOperator
import kotlin.math.min

class GoSGF(@JvmField val width: Int, @JvmField val height: Int) {

    @JvmOverloads constructor(size: Int = 19): this(size, size)

    @get:JvmName("rootNode")
    val rootNode = InternalGoSGF.setupSecrets.rootNode(this)

    var charset: Charset? = null

    @Suppress("unused")
    var encoding: String?
        get() = charset?.name()
        set(enc) {
            charset = enc?.let { Charset.forName(it) }
        }

    var variationView: Int = SUCCESSOR_VARIATION
        set(vv) { field = vv and 3 }

    @Suppress("unused")
    @Throws(IOException::class)
    fun writeSGFTree(output: OutputStream) {
        toSGFTree().write(output)
    }

    fun toSGFTree(): SGFTree = synchronized(this) {
        val node = SGFNode()
        val tree = SGFTree(node)
        node.properties["GM"] = SGFProperty(SGFValue(SGFBytes("1")))
        node.properties["FF"] = SGFProperty(SGFValue(SGFBytes("4")))
        val width = this.width
        val height = this.height
        val sizeVal = SGFValue(SGFBytes(width.toString()))
        if (width != height)
            sizeVal.list.add(SGFBytes(height.toString()))
        node.properties["SZ"] = SGFProperty(sizeVal)
        val charset = this.charset
        if (charset != null)
            node.properties["CA"] = SGFProperty(SGFValue(SGFBytes(charset.name())))
        node.properties["AP"] = SGFProperty(SGFValue("CN13 Goban", charset))
        val st = variationView
        if (st != 0)
            node.properties["ST"] = SGFProperty(SGFValue(SGFBytes(st.toString())))
        InternalGoSGF.nodeSecrets.writeSGFTree(rootNode, tree)
        tree
    }

    @Volatile
    private var warningList: SGFWarningList? = null
    var warnings: SGFWarningList
        get() = warningList ?: updateWarnings.getOrDefault(this, SGFWarningList())
        set(list) {
            warningList = if (list.javaClass == SGFWarningList::class.java) list
            else SGFWarningList(list)
        }

    companion object {

        const val SUCCESSOR_VARIATION = 0
        const val CURRENT_VARIATION = 1
        const val NO_MARKUP_VARIATION = 2

        /** Updates [GoSGF.warningList] */
        private val updateWarnings = atomicUpdater<GoSGF, SGFWarningList?>("warningList")

    }

    @JvmOverloads
    @Throws(IOException::class, SGFException::class)
    constructor(input: InputStream, warnings: SGFWarningList = SGFWarningList()): this(GoSGFReader(input, warnings))

    @Suppress("unused")
    @JvmOverloads
    @Throws(SGFException::class)
    constructor(tree: SGFTree, warnings: SGFWarningList = SGFWarningList()): this(GoSGFReader(tree, warnings))

    @Throws(SGFException::class)
    private constructor(reader: GoSGFReader): this(reader.width, reader.height) {
        val tree = reader.tree
        val warnings = reader.warnings
        warningList = warnings
        val node = tree.nodes[0]
        node.properties["CA"]?.let { prop ->
            val b: SGFBytes = prop.list[0].list[0]
            val enc = b.toString()
            if (Charset.isSupported(enc)) {
                try {
                    charset = Charset.forName(enc)
                } catch(e: RuntimeException) {
                    warnings += SGFWarning(b.row, b.column,
                        "Invalid encoding CA[$enc]: $e", e)
                }
            } else {
                warnings += SGFWarning(b.row, b.column, "Invalid encoding CA[$enc]")
            }
        }
        node.properties["ST"]?.let { prop ->
            val b: SGFBytes = prop.list[0].list[0]
            val s = b.toString()
            try {
                variationView = s.toInt()
            } catch(e: NumberFormatException) {
                warnings += SGFWarning(b.row, b.column,
                    "Invalid variation view ST[$s]: $e", e)
            }
        }
        InternalGoSGF.nodeSecrets.parseSGFNodes(rootNode, reader.fileFormat, tree, hadGameInfo=false, wasRoot=true)
    }

    private class GoSGFReader(val tree: SGFTree, val warnings: SGFWarningList = SGFWarningList()) {

        constructor(input: InputStream, warnings: SGFWarningList = SGFWarningList()):
                this(SGFTree(input, warnings), warnings)

        val fileFormat: Int
        val width: Int
        val height: Int

        init {
            val node = tree.nodes[0]
            var fileFormat = 4
            node.properties["FF"]?.let { prop ->
                val b: SGFBytes = prop.list[0].list[0]
                val s = b.toString()
                try {
                    val ff = s.toInt()
                    when {
                        ff == 0 -> warnings += SGFWarning(b.row, b.column, "Zero file format FF[0]")
                        ff < 0 -> warnings += SGFWarning(b.row, b.column, "Negative file format FF[$ff]")
                        else -> fileFormat = ff
                    }
                } catch(e: NumberFormatException) {
                    warnings += SGFWarning(b.row, b.column, "Unable to parse file format FF[$s]: $e", e)
                }
            }
            this.fileFormat = fileFormat
            node.properties["GM"]?.let { prop ->
                val b: SGFBytes = prop.list[0].list[0]
                val s = b.toString()
                var i = 0
                while(i < s.length && s[i].isWhitespace()) i++
                var isGo = false
                if (i < s.length) {
                    if (s[i] == '+') i++
                    var ch = '0'
                    while(i < s.length && ch == '0')
                        ch = s[i++]
                    if (ch == '1') {
                        i++
                        if (i >= s.length || s[i] !in '0'..'9') isGo = true
                    }
                }
                if (!isGo) warnings += SGFWarning(b.row, b.column, "Ignoring non-Go SGF property GM[$s]")
            }
            var w = 0
            var h = 0
            node.properties["SZ"]?.let { prop ->
                val v: SGFValue = prop.list[0]
                var s = v.list[0].toString()
                try {
                    w = s.toInt()
                    if (w !in 1..52) {
                        w = 0
                        warnings += SGFWarning(v.row, v.column, "$w is not between 1 and 52")
                    }
                } catch(e: NumberFormatException) {
                    warnings += SGFWarning(v.row, v.column,
                        "Invalid board size SZ[$s" +
                                (if (v.list.size > 1) ":...]: " else "]: ") + e, e)
                }
                if (v.list.size > 1) {
                    val b = v.list[1]
                    s = b.toString()
                    try {
                        h = s.toInt()
                        if (h !in 1..52) {
                            warnings += SGFWarning(b.row, b.column, "$h is not between 1 and 52")
                            h = 0
                        }
                    } catch(e: NumberFormatException) {
                        warnings += SGFWarning(b.row, b.column, "Invalid board size SZ[...:$s]: $e", e)
                    }
                }
            }
            if (h == 0) {
                if (w == 0) w = 19
                h = w
            } else if (w == 0) w = h
            width = w
            height = h
        }

    }

}

sealed class GoSGFNode {

    constructor(tree: GoSGF) {
        nullableTree = tree
        parent = null
        goban = FixedGoban(tree.width, tree.height)
        territory = MutableGoban(tree.width, tree.height)
        index = 0
    }

    constructor(parent: GoSGFNode, goban: AbstractGoban) {
        nullableTree = parent.nullableTree
        this.parent = parent
        this.goban = goban.readOnly()
        territory = MutableGoban(goban.width, goban.height)
        index = parent.index + 1
    }

    private var nullableTree: GoSGF?

    @get:JvmName("tree")
    val tree: GoSGF get() = nullableTree ?: throw IllegalStateException()

    val isAlive: Boolean get() = nullableTree != null

    private inline fun <R> syncTree(block: () -> R): R {
        val tree = nullableTree ?: return block()
        return synchronized(tree, block)
    }

    private inline fun <R> syncTree(default: R, block: () -> R): R {
        val tree = nullableTree ?: return default
        return synchronized(tree, block)
    }

    private inline fun syncTreeUnit(block: () -> Unit) {
        val tree = nullableTree ?: return
        synchronized(tree) {
            if (isAlive) block()
        }
    }

    private inline fun <R> syncTreeOrThrow(block: () -> R): R {
        val tree = this.tree
        return synchronized(tree) {
            if (!isAlive) throw IllegalStateException()
            block()
        }
    }

    @get:JvmName("parent")
    var parent: GoSGFNode? private set
    private var childArray: Array<GoSGFNode?>? = null
    private var childCount = 0
    val children: Int
        @JvmName("children")
        get() = syncTree(0) {
            if (isAlive) childCount else 0
        }
    @get:JvmName("index")
    var index: Int; private set
    @get:JvmName("childIndex")
    var childIndex: Int = 0; private set

    fun child(i: Int): GoSGFNode {
        if (i < 0) throw childIndexOutOfBoundsException(i)
        return syncTreeOrThrow {
            if (i >= childCount) throw childIndexOutOfBoundsException(i)
            fastChild(i)
        }
    }

    private fun fastChild(i: Int): GoSGFNode = childArray!![i]!!

    private fun childIndexOutOfBoundsException(i: Int) = IndexOutOfBoundsException(
        "$i is not in the range [0,$childCount)"
    )

    @get:JvmName("goban")
    val goban: FixedGoban

    abstract val turnPlayer: GoColor?

    private var _figureName: String? = null
    @Volatile private var _figure: Int = 0

    var figureName: String?
        get() = _figureName
        set(name) {
            lockFigure()
            _figureName = name
            _figure = when {
                name != null -> _figure and 0xFFFF
                _figure == LOCK_FIGURE -> 0
                else -> FIGURE_DEFAULT
            }
        }

    var figureMode: Int
        get() = _figure
        set(mode) {
            lockFigure()
            _figure = when {
                _figureName != null -> mode and 0xFFFF
                mode and 0xFFFF == 0 -> 0
                else -> FIGURE_DEFAULT
            }
        }

    fun setFigure(name: String?, mode: Int) {
        lockFigure()
        var maskedMode = mode and 0xFFFF
        if (name == null && maskedMode != 0) maskedMode = FIGURE_DEFAULT
        _figureName = name
        _figure = maskedMode
    }

    fun clearFigure() {
        lockFigure()
        _figureName = null
        _figure = 0
    }

    private fun lockFigure() {
        while(true) {
            val mode = _figure
            if (mode and LOCK_FIGURE == 0 &&
                updateFigure.compareAndSet(this, mode, mode or LOCK_FIGURE))
                return
        }
    }
    
    companion object {
        
        private const val LOCK_FIGURE = 0x10000
        
        const val FIGURE_HIDE_COORDINATES = 1
        const val FIGURE_HIDE_DIAGRAM_NAME = 2
        const val FIGURE_IGNORE_HIDDEN_MOVES = 4
        const val FIGURE_KEEP_CAPTURED_STONES = 0x100
        const val FIGURE_HIDE_HOSHI_DOTS = 0x200
        const val FIGURE_DEFAULT = 0x8000

        /** Updates [GoSGFNode._figure] */
        private val updateFigure = atomicIntUpdater<GoSGFNode>("_figure")

        init {
            InternalGoSGF.nodeSecrets = object: InternalGoSGF.NodeSecrets {
                override fun writeSGFTree(node: GoSGFNode, tree: SGFTree) = node.writeSGFTree(tree)
                override fun parseSGFNodes(
                    node: GoSGFNode,
                    fileFormat: Int,
                    tree: SGFTree,
                    hadGameInfo: Boolean,
                    wasRoot: Boolean
                ) = node.parseSGFNodes(fileFormat, tree, hadGameInfo, wasRoot)
            }
        }
        
    }

    @Suppress("unused")
    var ignoreFlags: Boolean
        get() = figureMode and FIGURE_DEFAULT != 0
        set(ignore) = setFigureModeFlag(FIGURE_DEFAULT, ignore)

    @Suppress("unused")
    var showCoordinates: Boolean
        get() = figureMode and FIGURE_HIDE_COORDINATES == 0
        set(show) = setFigureModeFlag(FIGURE_HIDE_COORDINATES, !show)

    @Suppress("unused")
    var showDiagramName: Boolean
        get() = figureMode and FIGURE_HIDE_DIAGRAM_NAME == 0
        set(show) = setFigureModeFlag(FIGURE_HIDE_DIAGRAM_NAME, !show)

    @Suppress("unused")
    var listHiddenMoves: Boolean
        get() = figureMode and FIGURE_IGNORE_HIDDEN_MOVES == 0
        set(list) = setFigureModeFlag(FIGURE_IGNORE_HIDDEN_MOVES, !list)

    @Suppress("unused")
    var removeCapturedStones: Boolean
        get() = figureMode and FIGURE_KEEP_CAPTURED_STONES == 0
        set(remove) = setFigureModeFlag(FIGURE_KEEP_CAPTURED_STONES, !remove)

    @Suppress("unused")
    var showHoshiDots: Boolean
        get() = figureMode and FIGURE_HIDE_HOSHI_DOTS == 0
        set(show) = setFigureModeFlag(FIGURE_HIDE_HOSHI_DOTS, !show)

    private fun setFigureModeFlag(flag: Int, value: Boolean) {
        if (_figureName == null) return
        lockFigure()
        if (_figureName == null) {
            _figure = 0
            return
        }
        val o: Int
        val a: Int
        if (value) {
            o = flag
            a = 0xFFFF
        } else {
            o = 0
            a = 0xFFFF xor flag
        }
        _figure = (_figure or o) and a
    }

    private var _hotspot: Byte = 0
    var hotspot: Int
        get() = _hotspot.toInt()
        set(value) {
            _hotspot = when {
                value < 0 -> 0
                value > 2 -> 2
                else -> value.toByte()
            }
        }

    var nodeName: String = ""
        set(name) { field = name.trim() }
    var comment: String = ""
    var positionState: PositionState? = null
    var positionValue: Double = Double.NaN

    val territory: MutableGoban

    private inline fun getInheritedNode(prop: GoSGFNode.() -> Any?): GoSGFNode = syncTree(this) {
        if (!isAlive) return@syncTree this
        var current: GoSGFNode? = this
        while(current != null) {
            if (current.prop() != null) return@syncTree current
            current = current.parent
        }
        this
    }

    var newPrintMethod: PrintMethod? = null; private set
    val printMethodNode: GoSGFNode
        get() = getInheritedNode { newPrintMethod }
    var printMethod: PrintMethod?
        get() = printMethodNode.newPrintMethod
        set(pm) = syncTree { newPrintMethod = pm }

    var newVisiblePoints: MutableGoPointSet? = null; private set
    val visiblePointsNode: GoSGFNode
        get() = getInheritedNode { newVisiblePoints }
    var visiblePoints: MutableGoPointSet?
        get() = visiblePointsNode.newVisiblePoints
        set(points) = syncTree { newVisiblePoints = points }

    var newDimPoints: MutableGoPointSet? = null; private set
    val dimPointsNode: GoSGFNode
        get() = getInheritedNode { newDimPoints }
    var dimPoints: MutableGoPointSet?
        get() = dimPointsNode.newDimPoints
        set(points) = syncTree { newDimPoints = points }

    val pointMarkup = PointMarkupMap()
    val lineMarkup = LineMarkupSet()

    private var thisGameInfo: GameInfo? = null
    var gameInfoNode: GoSGFNode? = null; private set
    var gameInfo: GameInfo?
        get() = syncTree(null as GameInfo?) {
            if (isAlive) gameInfoNode?.thisGameInfo
            else null
        }
        set(info) = syncTreeUnit {
            var gn = gameInfoNode
            if (info == null) {
                if (gn == null)
                    gn = this
                gn.thisGameInfo = null
                gn.setGameInfoNode(null, true)
            } else {
                thisGameInfo = info
                if (gn !== this) {
                    setGameInfoNode(this, true)
                    if (gn != null) {
                        gn.thisGameInfo = null
                        gn.setGameInfoNode(null, false)
                    }
                }
            }
        }

    private fun setGameInfoNode(node: GoSGFNode?, overwrite: Boolean) {
        gameInfoNode = node
        var current = this
        while (current.childCount == 1) {
            current = current.fastChild(0)
            if (!overwrite && current.gameInfoNode?.thisGameInfo != null) return
            current.thisGameInfo = null
            current.gameInfoNode = node
        }
        // minimize risk of StackOverflowError
        val n = current.childCount
        val next = current.childArray ?: return
        for (i in 0 until n) {
            current = next[i] ?: break
            if (overwrite || current.gameInfoNode?.thisGameInfo == null) {
                current.thisGameInfo = null
                current.setGameInfoNode(node, overwrite)
            }
        }
    }

    val hasGameInfoChildren: Boolean @JvmName("hasGameInfoChildren") get() = syncTree(false) {
        isAlive && hasGameInfoChildrenRecursive(null)
    }

    fun hasGameInfoExcluding(exclude: GameInfo) = syncTree(false) {
        isAlive && hasGameInfoChildrenRecursive(exclude)
    }

    private fun hasGameInfoChildrenRecursive(exclude: GameInfo?): Boolean {
        var current = this
        while(true) {
            val node = current.gameInfoNode
            if (node != null && node.gameInfo != exclude) return true
            if (current.childCount != 1) break
            current = current.fastChild(0)
        }
        for(i in 0 until current.childCount)
            if (current.fastChild(i).hasGameInfoChildrenRecursive(exclude)) return true
        return false
    }

    val previousGameInfoNode: GoSGFNode get() = syncTree(this) {
        if (isAlive) (gameInfoNode ?: this).findGameInfoNode(forward=false)
        else this
    }

    val nextGameInfoNode: GoSGFNode get() = syncTree(this) {
        if (isAlive) (gameInfoNode ?: this).findGameInfoNode(forward=true)
        else this
    }

    private fun findGameInfoNode(forward: Boolean): GoSGFNode {
        val direction = if (forward) 1 else -1
        var start = if (forward && gameInfoNode == null) this
        else findGameInfoStart(direction)
        while(true) {
            val child = start.findGameInfoChild(this, false, forward)
            if (child != null) return child
            start = start.findGameInfoStart(direction)
            if (start == this) return this
        }
    }

    private fun findGameInfoStart(direction: Int): GoSGFNode {
        var node = this
        var parent = parent ?: return node
        while(node.childIndex + direction !in 0 until parent.childCount) {
            node = parent
            parent = node.parent ?: return node
        }
        return parent.fastChild(node.childIndex + direction)
    }

    private fun findGameInfoChild(stop: GoSGFNode, fastStop: Boolean, forward: Boolean): GoSGFNode? {
        var current = this
        var slowStop = fastStop
        while(current.childCount == 1) {
            if ((slowStop && current == stop) || current.gameInfoNode != null) return current
            current = current.fastChild(0)
            slowStop = true
        }
        if ((slowStop && current == stop) || current.gameInfoNode != null) return current
        current.forEachChild(forward) {
            val child = it.findGameInfoChild(stop, true, forward)
            if (child != null) return child
        }
        return null
    }

    private inline fun forEachChild(forward: Boolean, block: (GoSGFNode) -> Unit) {
        if (forward) for(i in 0 until childCount) block(fastChild(i))
        else for(i in (childCount - 1) downTo 0) block(fastChild(i))
    }

    val unknownProperties = SGFNode()

    fun createNextMoveNode(playStoneAt: GoPoint?, turnPlayer: GoColor): GoSGFMoveNode {
        return syncTreeOrThrow { createNextMoveNodeAsync(playStoneAt, turnPlayer) }
    }

    private fun createNextMoveNodeAsync(playStoneAt: GoPoint?, turnPlayer: GoColor): GoSGFMoveNode {
        for(i in 0 until childCount) {
            val node = childArray?.get(i)!!
            if (node is GoSGFMoveNode && node.playStoneAt == playStoneAt && node.turnPlayer == turnPlayer)
                return node
        }
        val node = InternalGoSGF.moveSecrets.moveNode(this, playStoneAt, turnPlayer)
        initNextNode(node)
        return node
    }

    fun createNextSetupNode(goban: AbstractGoban): GoSGFSetupNode {
        val fixed = goban.readOnly()
        return syncTreeOrThrow { createNextSetupNodeAsync(fixed) }
    }

    private fun createNextSetupNodeAsync(goban: FixedGoban): GoSGFSetupNode {
        for(i in 0 until childCount) {
            val node = childArray?.get(i)!!
            if (node is GoSGFSetupNode && node.goban == goban)
                return node
        }
        val node = InternalGoSGF.setupSecrets.setupNode(this, goban)
        initNextNode(node)
        return node
    }

    private fun initNextNode(node: GoSGFNode) {
        val index = childCount++
        node.childIndex = index
        node.gameInfoNode = gameInfoNode
        var children = this.childArray
        if (children == null) {
            children = arrayOfNulls(1)
            this.childArray = children
        } else if (index >= children.size) {
            var n = children.size shl 1
            if (n < 0) {
                if (children.size == Int.MAX_VALUE) throw OutOfMemoryError()
                n = Int.MAX_VALUE
            }
            children = children.copyOf(n)
            this.childArray = children
        }
        children[index] = node
    }

    fun moveVariation(index: Int): Boolean {
        return index >= 0 && syncTree(false) {
            val parent = parent
            val variations = parent?.childArray
            if (variations == null || index == childIndex || index >= parent.childCount)
                return@syncTree false
            if (index > childIndex) {
                for(i in childIndex until index) {
                    val node = variations[i + 1]
                    node?.childIndex = i
                    variations[i] = node
                }
            } else {
                for(i in (index + 1)..childIndex) {
                    val node = variations[i - 1]
                    node?.childIndex = i
                    variations[i] = node
                }
            }
            childIndex = index
            variations[index] = this
            true
        }
    }

    @Suppress("unused")
    fun swapVariation(index: Int): Boolean {
        return index >= 0 && syncTree(false) {
            val parent = parent
            val variations = parent?.childArray
            if (variations == null || index == childIndex || index >= parent.childCount)
                return@syncTree false
            val other = variations[index]
            other?.childIndex = childIndex
            variations[childIndex] = other
            childIndex = index
            variations[index] = this
            true
        }
    }

    fun delete() {
        val parent = this.parent ?: return
        syncTreeUnit {
            val count = parent.childCount - 1
            val next = parent.childArray!!
            for(i in childIndex until count) {
                val node = next[i + 1]
                node?.childIndex = i
                next[i] = node
            }
            parent.childCount = count
            next[count] = null
            deleteRecursive()
        }
    }

    private fun deleteRecursive() {
        var current = this
        // minimize risk of StackOverflowError
        while(current.childCount == 1) {
            current.nullableTree = null
            current.parent = null
            val children = current.childArray!!
            val child = children[0]!!
            current.childArray = null
            children[0] = null
            current.childCount = 0
            current.index = 0
            current.childIndex = 0
            current.gameInfoNode = null
            current.gameInfo = null
            current = child
        }
        current.nullableTree = null
        current.parent = null
        val children = current.childArray
        current.childArray = null
        val n = current.childCount
        current.childCount = 0
        current.index = 0
        current.childIndex = 0
        current.gameInfoNode = null
        current.gameInfo = null
        if (children != null) {
            for(i in 0 until n) {
                val child = children[i]!!
                children[i] = null
                child.deleteRecursive()
            }
        }
    }

    private fun writeSGFTree(tree: SGFTree) {
        val charset = this.tree.charset
        val nodeList = tree.nodes
        var node = nodeList[0]
        var current = this
        val markupSets = Array(PointMarkup.TYPES - 1) {
            MutableGoPointSet()
        }
        var start = true
        while(true) {
            if (!start) {
                node = SGFNode()
                nodeList.add(node)
            }
            val propMap = node.properties
            current.writeSGFNode(node)
            var s = current.nodeName
            if (s.isNotEmpty())
                propMap["N"] = SGFProperty(SGFValue(s, charset))
            var i = current.hotspot
            if (i != 0)
                propMap["HO"] = SGFProperty(SGFValue(SGFBytes(byteArrayOf(('0' + i).toByte()))))
            current.positionState?.let { state ->
                propMap[state.code] = SGFProperty(SGFValue(
                    if (state.extent == 0) SGFBytes()
                    else SGFBytes(byteArrayOf(('0' + state.extent).toByte()))
                ))
            }
            val d = current.positionValue
            if (!d.isNaN())
                propMap["V"] = SGFProperty(SGFValue(SGFBytes(d.toString())))
            i = current._figure and 0xFFFF
            current._figureName?.let { name ->
                propMap["FG"] = SGFProperty(SGFValue(SGFBytes(i.toString())).addText(name, charset))
            } ?: if (i != 0) propMap["FG"] = SGFProperty(SGFValue(SGFBytes()))
            current.newVisiblePoints?.toSGFProperty(true)?.let { prop ->
                propMap["VW"] = prop
            }
            current.newDimPoints?.toSGFProperty(true)?.let { prop ->
                propMap["DD"] = prop
            }
            current.newPrintMethod?.let { pm ->
                propMap["PM"] = SGFProperty(SGFValue(SGFBytes(
                    byteArrayOf(('0' + pm.ordinal).toByte())
                )))
            }
            val territory = current.territory
            territory.toPointSet(GoColor.BLACK).toSGFProperty(false)?.let { prop ->
                propMap["TB"] = prop
            }
            territory.toPointSet(GoColor.WHITE).toSGFProperty(false)?.let { prop ->
                propMap["TW"] = prop
            }
            if (current.gameInfoNode == current)
                current.gameInfo?.writeSGFNode(node, charset)
            var prop: SGFProperty? = null
            for((point, markup) in current.pointMarkup) {
                i = markup.ordinal
                if (i == 0) {
                    val value = SGFValue(SGFBytes(point.toString())).addText(markup.label, charset)
                    prop?.list?.add(value) ?: SGFProperty(value).let { prop = it }
                } else markupSets[i - 1].add(point)
            }
            prop?.let { propMap.put("LB", it) }
            for(markup in markupSets.indices) {
                val points = markupSets[markup]
                points.toSGFProperty(true)?.let {
                    propMap[PointMarkup.ordinal(markup + 1).type] = it
                }
                points.clear()
            }
            prop = null
            var propAR: SGFProperty? = null
            for(lm in current.lineMarkup) {
                val value = SGFValue(SGFBytes(lm.start.toString()))
                value.list.add(SGFBytes(lm.end.toString()))
                if (lm.isArrow)
                    propAR?.list?.add(value) ?: SGFProperty(value).let { propAR = it }
                else
                    prop?.list?.add(value) ?: SGFProperty(value).let { prop = it }
            }
            prop?.let { propMap["LN"] = it }
            propAR?.let { propMap["AR"] = it }
            s = current.comment
            for(entry in current.unknownProperties.properties) {
                val name = entry.key
                if ((s.isEmpty() || name != "C") && !propMap.containsKey(name))
                    propMap[name] = entry.value
            }
            if (s.isNotEmpty()) propMap["C"] = SGFProperty(SGFValue(s, charset))
            if (current.childCount != 1) break
            current = current.childArray?.get(0) ?: break
            start = false
        }
        for(i in 0 until current.childCount)
            current.childArray?.get(i)?.writeSGFTree(tree.subTree(SGFNode()))
    }

    protected abstract fun writeSGFNode(node: SGFNode)

    @Throws(SGFException::class)
    private fun parseSGFNodes(
        fileFormat: Int,
        tree: SGFTree,
        hadGameInfo: Boolean,
        wasRoot: Boolean
    ) {
        var isRoot = wasRoot
        var hasGameInfo = hadGameInfo
        val warnings = this.tree.warnings
        var gameInfoNode: GoSGFNode?
        var gameInfo: GameInfo? = null
        val width = this.tree.width
        val height = this.tree.height
        val ignoreCase = width <= 26 && height <= 26
        var currentNode = this
        val territoryPoints = arrayOf(MutableGoPointSet(), MutableGoPointSet())
        val markupPoints = Array(PointMarkup.TYPES) {
            MutableGoPointSet()
        }
        val pointMarkupProps = arrayOfNulls<SGFProperty>(PointMarkup.TYPES - 1)
        val lineMarkupProps = arrayOfNulls<SGFProperty>(2)
        for(node in tree.nodes) {
            val properties = node.properties
            var setup: GoSGFSetupNode? = null
            var move: GoSGFMoveNode? = null
            // setup properties
            val addBlack = properties["AB"]
            val addWhite = properties["AW"]
            val addEmpty = properties["AE"]
            var isSetupNode = addBlack != null || addWhite != null || addEmpty != null
            // move properties
            val moveColor: GoColor?
            val moveProp: SGFProperty?
            val moveBlack = properties["B"]
            val moveWhite = properties["W"]
            when {
                moveBlack != null -> {
                    if (moveWhite != null) {
                        val blackRow = moveBlack.row
                        val whiteRow = moveWhite.row
                        val row: Int
                        val column: Int
                        when {
                            blackRow < whiteRow -> {
                                row = blackRow
                                column = moveBlack.column
                            }
                            blackRow != whiteRow -> {
                                row = whiteRow
                                column = moveWhite.column
                            }
                            else -> {
                                row = blackRow
                                column = min(moveBlack.column, moveWhite.column)
                            }
                        }
                        throw SGFException(row, column,
                            "Move properties B[] and W[] cannot exist within the same node.")
                    }
                    moveColor = GoColor.BLACK
                    moveProp = moveBlack
                }
                moveWhite != null -> {
                    moveColor = GoColor.WHITE
                    moveProp = moveWhite
                }
                else -> {
                    moveColor = null
                    moveProp = null
                }
            }
            if (moveColor == null) isSetupNode = true
            else if (isSetupNode)
                warnings.addWarning(
                    SGFWarning(node.row, node.column,
                        "Move and setup properties were detected in the same node. " +
                                "They will be separated into two split nodes.")
                )
            if (isRoot) isSetupNode = true
            val goban: Goban?
            if (isSetupNode) {
                val blackPoints = parsePointSet(null, addBlack)
                val whitePoints = parsePointSet(null, addWhite)
                val emptyPoints = parsePointSet(null, addEmpty)
                val clashes = removeClashingGoPoints(blackPoints, whitePoints, emptyPoints)
                if (!clashes.isNullOrEmpty())
                    warnings += SGFWarning(node.row, node.column, buildString {
                        append("AB, AW and AE cannot be used on the same points: ")
                        for(point in clashes)
                            append('[').append(point).append(']')
                    })
                goban = if (isRoot) {
                    setup = currentNode as GoSGFSetupNode
                    if (blackPoints.isNullOrEmpty() && whitePoints.isNullOrEmpty()) null
                    else Goban(width, height)
                } else Goban(currentNode.goban)
                if (goban != null) {
                    if (blackPoints != null) goban.setAll(blackPoints, GoColor.BLACK)
                    if (whitePoints != null) goban.setAll(whitePoints, GoColor.WHITE)
                    if (emptyPoints != null) goban.setAll(emptyPoints, null)
                    setup = currentNode.createNextSetupNodeAsync(goban.readOnly())
                }
                if (setup != null) currentNode = setup
            }
            if (moveProp != null) {
                val moveBytes: SGFBytes = moveProp.list[0].list[0]
                var movePoint = InternalGoSGF.parsePoint(moveBytes, ignoreCase)
                if (movePoint != null && (movePoint.x >= width || movePoint.y >= height)) {
                    if (!(movePoint.x == 19 && movePoint.y == 19 &&
                                width <= 19 && height <= 19))
                        warnings += SGFWarning(moveBytes.row, moveBytes.column,
                            InternalGoSGF.pointOutOfRange(moveBytes.toString(), movePoint.x, movePoint.y,
                                width, height))
                    movePoint = null
                }
                move = currentNode.createNextMoveNodeAsync(movePoint, moveColor!!)
                currentNode = move
            }
            var propPL: SGFProperty? = null
            var propN: SGFProperty? = null
            var propMN: SGFProperty? = null
            var forceMove = false
            val moveAnnotationLimit = 4
            var indexIT = moveAnnotationLimit
            var indexDO = moveAnnotationLimit
            var indexBM = moveAnnotationLimit
            var indexTE = moveAnnotationLimit
            var moveAnnotationIndex = 0
            var positionState: PositionState? = null
            var positionName: String? = null
            var positionProp: SGFProperty? = null
            var propBM: SGFProperty? = null
            var propTE: SGFProperty? = null
            var propBL: SGFProperty? = null
            var propOB: SGFProperty? = null
            var propWL: SGFProperty? = null
            var propOW: SGFProperty? = null
            var propV: SGFProperty? = null
            var propC: SGFProperty? = null
            var propHO: SGFProperty? = null
            var propFG: SGFProperty? = null
            var propVW: SGFProperty? = null
            var propDD: SGFProperty? = null
            var propPM: SGFProperty? = null
            var propTB: SGFProperty? = null
            var propTW: SGFProperty? = null
            // Point markup properties
            var propL: SGFProperty? = null
            var propM: SGFProperty? = null
            var propLB: SGFProperty? = null
            for((name, prop) in properties) {
                var state: PositionState? = null
                var isMoveProp = false
                when(name) {
                    "PL" -> propPL = prop
                    "N" -> propN = prop
                    "KO" -> {
                        isMoveProp = true
                        forceMove = true
                    }
                    "MN" -> {
                        isMoveProp = true
                        propMN = prop
                    }
                    "IT" -> {
                        isMoveProp = true
                        indexIT = moveAnnotationIndex++
                    }
                    "DO" -> {
                        isMoveProp = true
                        indexDO = moveAnnotationIndex++
                    }
                    "BM" -> {
                        isMoveProp = true
                        indexBM = moveAnnotationIndex++
                        propBM = prop
                    }
                    "TE" -> {
                        isMoveProp = true
                        indexTE = moveAnnotationIndex++
                        propTE = prop
                    }
                    "BL" -> {
                        isMoveProp = true
                        propBL = prop
                    }
                    "OB" -> {
                        isMoveProp = true
                        propOB = prop
                    }
                    "WL" -> {
                        isMoveProp = true
                        propWL = prop
                    }
                    "OW" -> {
                        isMoveProp = true
                        propOW = prop
                    }
                    "C" -> propC = prop
                    "HO" -> propHO = prop
                    "FG" -> propFG = prop
                    "UC" -> state = PositionState.UNCLEAR
                    "DM" -> state = PositionState.EVEN
                    "GB" -> state = PositionState.GOOD_FOR_BLACK
                    "GW" -> state = PositionState.GOOD_FOR_WHITE
                    "V" -> propV = prop
                    "VW" -> propVW = prop
                    "DD" -> propDD = prop
                    "PM" -> propPM = prop
                    "TB" -> propTB = prop
                    "TW" -> propTW = prop
                    "L" -> propL = prop
                    "M" -> propM = prop
                    "LB" -> propLB = prop
                    "SL" -> pointMarkupProps[0] = prop
                    "MA" -> pointMarkupProps[1] = prop
                    "TR" -> pointMarkupProps[2] = prop
                    "CR" -> pointMarkupProps[3] = prop
                    "SQ" -> pointMarkupProps[4] = prop
                    "LN" -> lineMarkupProps[0] = prop
                    "AR" -> lineMarkupProps[1] = prop
                    // root properties
                    "GM", "FF", "SZ",
					"CA", "AP", "ST" -> if (!isRoot)
                        warnings += SGFWarning(prop.row, prop.column, "$name$prop in non-root node")
                    "PB", "BR", "BT",
					"PW", "WR", "WT",
					"KM", "HA", "DT", "TM", "OT",
                    "RE", "GN", "GC", "SO", "US", "CP",
                    "PC", "EV", "RO", "AN", "RU", "ON" -> if (hasGameInfo)
                        warnings += SGFWarning(prop.row, prop.column,
                            "$name$prop conflicts with parent game-info node")
                    else {
                        if (gameInfo == null) {
                            gameInfo = GameInfo()
                            if (setup != null && move != null) {
                                gameInfoNode = setup
                                gameInfoNode.gameInfoNode = gameInfoNode
                            } else gameInfoNode = currentNode
                            currentNode.gameInfoNode = gameInfoNode
                            gameInfoNode.gameInfo = gameInfo
                        }
                        gameInfo.parseSGFProperty(name, prop, this.tree.charset, warnings)
                    }
                    // fundamental move or setup properties
                    // that were already parsed
                    "B", "W", "AB", "AW", "AE" -> Unit
                    else -> currentNode.unknownProperties.properties[name] =
                        prop.copy(SGFCopyLevel.ALL)
                }
                if (state != null) {
                    if (positionState == null) {
                        positionState = state
                        positionName = name
                        positionProp = prop
                    } else warnings += SGFWarning(prop.row, prop.column,
                        "$name$prop cannot appear in the same node as $positionName$positionProp")
                }
                if (isMoveProp && moveColor == null)
                    warnings += SGFWarning(prop.row, prop.column,
                        "$name$prop must be accompanied by either B[] or W[] in the same node")
            }
            if (propPL != null) {
                if (isSetupNode) {
                    var nextPlayer: GoColor? = null
                    val bytes: SGFBytes = propPL.list[0].list[0]
                    loopPL@ for (b in bytes) {
                        when (b) {
                            'B'.toByte(), 'b'.toByte() -> {
                                nextPlayer = GoColor.BLACK
                                break@loopPL
                            }
                            'W'.toByte(), 'w'.toByte() -> {
                                nextPlayer = GoColor.WHITE
                                break@loopPL
                            }
                        }
                    }
                    if (nextPlayer == null)
                        warnings += SGFWarning(
                            propPL.row, propPL.column,
                            "Illegal use for PL[$bytes] - must be PL[B] or PL[W]"
                        )
                    else setup?.turnPlayer = nextPlayer
                } else warnings += SGFWarning(propPL.row, propPL.column, "Cannot use PL[] in move node")
            }
            if (propN != null)
                (setup ?: currentNode).nodeName =
                    InternalGoSGF.parseSGFValue(propN.list[0], this.tree.charset, warnings)
            if (move != null) {
                move.isForced = forceMove
                when {
                    indexIT < indexDO -> move.moveAnnotation = MoveAnnotation.INTERESTING
                    indexDO < indexIT -> move.moveAnnotation = MoveAnnotation.DOUBTFUL
                    indexTE < indexBM -> move.moveAnnotation =
                        if (indexBM < moveAnnotationLimit) MoveAnnotation.INTERESTING
                        else MoveAnnotation.GOOD.toExtent(parse1or2(propTE, warnings))
                    indexBM < indexTE -> move.moveAnnotation =
                        if (indexTE < moveAnnotationLimit) MoveAnnotation.DOUBTFUL
                        else MoveAnnotation.BAD.toExtent(parse1or2(propBM, warnings))
                }
                if (propMN != null) {
                    val bytes: SGFBytes = propMN.list[0].list[0]
                    val s = bytes.toString()
                    var num = 0
                    val isParsed = try {
                        num = s.toInt()
                        true
                    } catch (e: NumberFormatException) {
                        warnings += SGFWarning(bytes.row, bytes.column,
                            "Unable to parse move number MN[$s]: $e", e)
                        false
                    }
                    if (num > 0) move.moveNumber = num
                    else if (isParsed) warnings += SGFWarning(
                        bytes.row, bytes.column,
                        "Move number must be positive: MN[$num]"
                    )
                }
                move.black.parseTimeRemaining(propBL, propOB)
                move.white.parseTimeRemaining(propWL, propOW)
            }
            if (propC != null)
                currentNode.comment = InternalGoSGF.parseSGFValue(propC.list[0], this.tree.charset, warnings)
            if (propHO != null) currentNode.hotspot = parse1or2(propHO, warnings)
            if (propFG != null) {
                val bytesList = propFG.list[0].list
                var bytes = bytesList[0]
                val s = bytes.toString()
                if (s.isBlank() && bytes.size == 1) {
                    currentNode._figure = FIGURE_DEFAULT
                    currentNode._figureName = null
                } else {
                    var figureMode = 0
                    val isParsed = try {
                        figureMode = s.toInt()
                        true
                    } catch (e: NumberFormatException) {
                        warnings += SGFWarning(
                            bytes.row, bytes.column,
                            "Invalid figure mode FG[$s]: $e", e
                        )
                        false
                    }
                    if (isParsed && figureMode !in 0..0xFFFF)
                        warnings += SGFWarning(
                            bytes.row, bytes.column,
                            "Invalid figure mode FG[$s]"
                        )
                    currentNode._figure = figureMode and 0xFFFF
                    currentNode._figureName = if (bytesList.size > 1) {
                        bytes = bytesList[1]
                        InternalGoSGF.parseSGFBytesList(
                            bytes.row, bytes.column,
                            bytesList.subList(1, bytesList.size),
                            this.tree.charset, warnings
                        )
                    } else ""
                }
            }
            if (positionProp != null && positionState != null) {
                positionState = positionState.toExtent(parse1or2(positionProp, warnings))
                currentNode.positionState = positionState
            }
            propV?.list?.get(0)?.list?.get(0)?.let { bytes: SGFBytes ->
                val s = bytes.toString()
                var d = 0.0
                val isParsed = try {
                    d = s.toDouble()
                    true
                } catch(e: NumberFormatException) {
                    warnings += SGFWarning(bytes.row, bytes.column,
                        "Unable to parse floating-point value V[$s]: $e", e)
                    false
                }
                if (isParsed) {
                    if (d.isInfinite() || d.isNaN())
                        warnings += SGFWarning(bytes.row, bytes.column,
                            "Value is not finite: V[$s]")
                    else currentNode.positionValue = d
                }
            }
            if (propVW != null)
                currentNode.newVisiblePoints = parsePointSet(fileFormat, MutableGoPointSet(), propVW)
            if (propDD != null)
                currentNode.newDimPoints = parsePointSet(MutableGoPointSet(), propDD)
            propPM?.list?.get(0)?.list?.get(0)?.let { bytes: SGFBytes ->
                currentNode.newPrintMethod = PrintMethod.parseOrdinal(bytes.toString())
            }
            parsePointSet(territoryPoints[0], propTB)
            parsePointSet(territoryPoints[1], propTW)
            val clashes = removeClashingGoPoints(*territoryPoints)
            if (!clashes.isNullOrEmpty())
                warnings += SGFWarning(node.row, node.column, buildString {
                    append("TB and TW cannot be used on the same points: ")
                    for(point in clashes)
                        append('[').append(point).append(']')
                })
            currentNode.territory.setAll(territoryPoints[0], GoColor.BLACK)
            currentNode.territory.setAll(territoryPoints[1], GoColor.WHITE)
            val labelMap = mutableMapOf<GoPoint, String>()
            val markupLB = markupPoints[0]
            val markupMA = markupPoints[2]
            val markupTR = markupPoints[3]
            propLB?.list?.forEach next@{ value ->
                val bytesList = value.list
                val bytes = bytesList[0]
                val point = InternalGoSGF.parsePoint(bytes, ignoreCase)
                if (point == null) {
                    warnings += SGFWarning(bytes.row, bytes.column, InternalGoSGF.malformedPoint(bytes.toString()))
                    return@next // continue
                }
                if (point.x >= width && point.y >= height) {
                    warnings += SGFWarning(bytes.row, bytes.column,
                        InternalGoSGF.pointOutOfRange(bytes.toString(), point.x, point.y, width, height))
                    return@next
                }
                var s: String
                val bytesCount = bytes.size
                if (bytesCount > 1) {
                    val bytes2 = bytesList[1]
                    s = InternalGoSGF.parseSGFBytesList(bytes2.row, bytes2.column,
                        bytesList.subList(1, bytesCount),
                        this.tree.charset, warnings)
                    if (s.isNotEmpty()) {
                        labelMap[point] = s
                        markupLB.add(point)
                        return@next
                    }
                    s = "$bytes:"
                } else s = bytes.toString()
                warnings += SGFWarning(bytes.row, bytes.column,
                    "Empty label: LB[$s]")
            }
            propL?.list?.let { valueList ->
                var i = 0
                for(value in valueList) {
                    val s = Parse.LABELS[i]
                    if (++i >= 52) i = 0
                    val bytes = value.list[0]
                    val point = InternalGoSGF.parsePoint(bytes, ignoreCase)
                    if (point == null) {
                        warnings += SGFWarning(bytes.row, bytes.column,
                                InternalGoSGF.malformedPoint(bytes.toString()))
                        continue
                    }
                    if (point.x >= width || point.y >= height) {
                        warnings += SGFWarning(bytes.row, bytes.column,
                            InternalGoSGF.pointOutOfRange(bytes.toString(), point.x, point.y, width, height))
                        continue
                    }
                    val label = labelMap[point]
                    if (label != null) {
                        warnings += SGFWarning(bytes.row, bytes.column,
                            "Point already has label: LB[$bytes:$label]")
                        continue
                    }
                    labelMap[point] = s
                    markupLB.add(point)
                }
            }
            for(i in 1 until PointMarkup.TYPES)
                pointMarkupProps[i - 1]?.let {
                    parsePointSet(markupPoints[i], it)
                }
            if (propM != null) parsePointSet(null, propM)?.let { xPoints ->
                xPoints.removeAll(markupMA)
                xPoints.removeAll(markupTR)
                if (xPoints.isNotEmpty()) {
                    val g = currentNode.goban
                    if (!g.isEmpty()) {
                        val trPoints = xPoints.copy()
                        val ePoints = g.toPointSet(null)
                        xPoints.retainAll(ePoints)
                        trPoints.removeAll(ePoints)
                        markupTR.addAll(trPoints)
                    }
                    markupMA.addAll(xPoints)
                }
            }
            val markupClashes = removeClashingGoPoints(*markupPoints)
            if (!markupClashes.isNullOrEmpty())
                warnings += SGFWarning(node.row, node.column, buildString {
                    append("LB, SL, MA, TR, CR and SQ cannot be used on the same points: ")
                    for(point in markupClashes)
                        append('[').append(point).append(']')
                })
            for(point in markupPoints[0])
                labelMap[point]?.let { s ->
                    currentNode.pointMarkup[point] = PointMarkup.label(s)
                }
            for(i in 1 until PointMarkup.TYPES)
                for(point in markupPoints[i])
                    currentNode.pointMarkup[point] = PointMarkup.ordinal(i)
            for(i in 0..1) {
                val prop = lineMarkupProps[i] ?: continue
                for(value in prop.list) {
                    val bytesList = value.list
                    if (bytesList.size < 2) {
                        warnings += SGFWarning(value.row, value.column,
                                Parse.WARNING_REQUIRE_2_POINTS[i])
                        continue
                    }
                    val s1 = bytesList[0]
                    val s2 = bytesList[1]
                    val start = InternalGoSGF.parsePoint(s1, ignoreCase)
                    if (start == null) {
                        warnings += SGFWarning(s1.row, s1.column,
                            InternalGoSGF.malformedPoint(s1.toString()))
                        continue
                    }
                    if (start.x >= width || start.y >= height) {
                        warnings += SGFWarning(s1.row, s1.column,
                            InternalGoSGF.pointOutOfRange(s1.toString(), start.x, start.y, width, height))
                        continue
                    }
                    val end = InternalGoSGF.parsePoint(s2, ignoreCase)
                    if (end == null) {
                        warnings += SGFWarning(s2.row, s2.column,
                            InternalGoSGF.malformedPoint(s2.toString()))
                        continue
                    }
                    if (end.x >= width || end.y >= height) {
                        warnings += SGFWarning(s2.row, s2.column,
                            InternalGoSGF.pointOutOfRange(s2.toString(), end.x, end.y, width, height))
                        continue
                    }
                    if (end == start) {
                        warnings += SGFWarning(s2.row, s2.column,
                            Parse.WARNING_CONNECT_POINT_TO_SELF_PREFIX[i] + s2.toString() +
                                    Parse.WARNING_CONNECT_POINT_TO_SELF_SUFFIX)
                        continue
                    }
                    lineMarkup.add(
                        if (i == 0) start lineMarkup end
                        else start arrowMarkup end
                    )
                }
            }
            isRoot = false
            if (gameInfo != null) hasGameInfo = true
            pointMarkupProps.fill(null)
            lineMarkupProps.fill(null)
            for(points in markupPoints)
                points.clear()
            for(points in territoryPoints)
                points.clear()
        }
        for(subTree in tree.subTrees)
            currentNode.parseSGFNodes(fileFormat, subTree, hasGameInfo, wasRoot=false)
    }

    private object Parse {

        val LABELS: Array<String> = CharArray(1).let { buffer ->
            Array(52) {
                buffer[0] = ((if (it < 26) 'A' else ('a' - 26)) + it)
                String(buffer)
            }
        }

        val WARNING_REQUIRE_2_POINTS = arrayOf(
            "Two points are required to draw a line between them",
            "Two points are required to draw an arrow between them"
        )

        val WARNING_CONNECT_POINT_TO_SELF_PREFIX = arrayOf(
            "Cannot draw a line between point [",
            "Cannot draw an arrow between point ["
        )

        const val WARNING_CONNECT_POINT_TO_SELF_SUFFIX = "] and itself"

    }

    private fun parse1or2(prop: SGFProperty?, warnings: SGFWarningList): Int {
        val s = prop?.list?.get(0)?.let { value ->
            InternalGoSGF.parseSGFValue(value, null, warnings)
        } ?: return 0
        var i = 0
        for(ch in s) {
            if (ch !in '0'..'9') break
            i = i*10 + (ch - '0')
            if (i > 1) return 2
        }
        return i
    }

    private fun parsePointSet(fileFormat: Int, points: MutableGoPointSet?, prop: SGFProperty?): MutableGoPointSet? {
        if (fileFormat < 4 && prop != null) {
            val values = prop.list
            if (values.size == 2) {
                val v1 = values[0].list
                val v2 = values[1].list
                if (v1.size == 1 && v2.size == 1)
                    return parsePointRect(points, v1[0], v2[0])
            }
        }
        return parsePointSet(points, prop)
    }

    private fun parsePointSet(points: MutableGoPointSet?, prop: SGFProperty?): MutableGoPointSet? {
        if (prop == null) return points
        var pointSet = points
        for(value in prop.list) {
            val bytes = value.list
            val s1 = bytes[0]
            val s2 = if (bytes.size >= 2) bytes[1] else null
            pointSet = parsePointRect(pointSet, s1, s2)
        }
        return pointSet
    }

    private fun parsePointRect(points: MutableGoPointSet?, s1: SGFBytes, s2: SGFBytes?): MutableGoPointSet? {
        val width = tree.width
        val height = tree.height
        val warnings = tree.warnings
        val ignoreCase = width <= 26 && height <= 26
        var p1 = InternalGoSGF.parsePoint(s1, ignoreCase)
        if (p1 == null) {
            if (s1.size != 0)
                warnings += SGFWarning(s1.row, s1.column, InternalGoSGF.malformedPoint(s1.toString()))
        } else if (p1.x >= width || p1.y >= height)
            warnings += SGFWarning(s1.row, s1.column,
                InternalGoSGF.pointOutOfRange(s1.toString(), p1.x, p1.y, width, height))
        var p2 = InternalGoSGF.parsePoint(s2, ignoreCase)
        if (p2 != null) {
            if (s2 != null && (p2.x >= width || p2.y >= height))
                warnings += SGFWarning(s2.row, s2.column,
                    InternalGoSGF.pointOutOfRange(s2.toString(), p2.x, p2.y, width, height))
            if (p1 == null) p1 = p2
        } else {
            if (s2 != null && s2.size != 0)
                warnings += SGFWarning(s2.row, s2.column, InternalGoSGF.malformedPoint(s2.toString()))
            p2 = p1 ?: return points
        }
        var (x1, y1) = p1
        var (x2, y2) = p2
        if (x1 > x2) {
            val tmp = x1
            x1 = x2
            x2 = tmp
        }
        if (y1 > y2) {
            val tmp = y1
            y1 = y2
            y2 = tmp
        }
        if (x1 >= width || y1 >= height) // warnings already registered
            return points
        if (x2 >= width) x2 = width - 1
        if (y2 >= height) y2 = height - 1
        val pointSet = points ?: MutableGoPointSet()
        pointSet.addAll(GoRectangle(x1, y1, x2, y2))
        return pointSet
    }

}

class GoSGFMoveNode private constructor(
        parent: GoSGFNode,
        val playStoneAt: GoPoint?,
        override val turnPlayer: GoColor
): GoSGFNode(parent, InternalGoSGF.playStoneAt(parent, playStoneAt, turnPlayer)) {

    @Volatile private var flags: Int = 0

    companion object {

        init {
            InternalGoSGF.moveSecrets = object: InternalGoSGF.MoveSecrets {
                override fun moveNode(
                    parent: GoSGFNode,
                    playStoneAt: GoPoint?,
                    turnPlayer: GoColor
                ) = GoSGFMoveNode(parent, playStoneAt, turnPlayer)
            }
        }

        /** Updates [GoSGFMoveNode.flags] */
        private val updateFlags = atomicIntUpdater<GoSGFMoveNode>("flags")

    }

    private object Flags {
        const val BLACK_TIME = 0x1
        const val WHITE_TIME = 0x2
        const val BLACK_OVERTIME = 0x4
        const val WHITE_OVERTIME = 0x8
        const val FORCED = 0x10
    }

    var isForced: Boolean
        get() = flags and Flags.FORCED != 0
        set(forced) {
            val flag: Int
            val op: IntBinaryOperator
            if (forced) {
                flag = Flags.FORCED
                op = IntBinOp.OR
            } else {
                flag = Flags.FORCED.inv()
                op = IntBinOp.AND
            }
            updateFlags.getAndAccumulate(this, flag, op)
        }

    @get:JvmName("black")
    val black = InternalGoSGF.timeSecrets.playerTime(this, GoColor.BLACK)
    @get:JvmName("white")
    val white = InternalGoSGF.timeSecrets.playerTime(this, GoColor.WHITE)

    fun time(player: GoColor) = if (player == GoColor.BLACK) black else white

    var moveAnnotation: MoveAnnotation? = null

    var moveNumber: Int = 0
        set(n) { field = n.coerceAtLeast(0) }

    class PlayerTime private constructor(
        val node: GoSGFMoveNode,
        val color: GoColor
    ) {

        companion object {
            init {
                InternalGoSGF.timeSecrets = object: InternalGoSGF.TimeSecrets {
                    override fun playerTime(node: GoSGFMoveNode, turnPlayer: GoColor) = PlayerTime(node, turnPlayer)
                }
            }
        }

        private var _time: Long = 0L
        private var _overtime: Int = 0

        val hasTime: Boolean
            @JvmName("hasTime")
            get() = node.flags and (if (color == GoColor.BLACK) Flags.BLACK_TIME else Flags.WHITE_TIME) != 0

        var time: Long
            get() = _time
            set(time) {
                _time = time
                updateFlags.getAndAccumulate(node,
                    if (color == GoColor.BLACK) Flags.BLACK_TIME
                    else Flags.WHITE_TIME, IntBinOp.OR)
            }

        fun omitTime() {
            _time = 0L
            updateFlags.getAndAccumulate(node,
                if (color == GoColor.BLACK) Flags.BLACK_TIME xor -1
                else Flags.WHITE_TIME xor -1, IntBinOp.AND)
        }

        val hasOvertime: Boolean
            @JvmName("hasOvertime")
            get() = node.flags and (if (color == GoColor.BLACK) Flags.BLACK_OVERTIME else Flags.WHITE_OVERTIME) != 0

        var overtime: Int
            get() = _overtime
            set(overtime) {
                _overtime = overtime
                updateFlags.getAndAccumulate(node,
                    if (color == GoColor.BLACK) Flags.BLACK_OVERTIME
                    else Flags.WHITE_OVERTIME, IntBinOp.OR)
            }

        fun omitOvertime() {
            _overtime = 0
            updateFlags.getAndAccumulate(node,
                if (color == GoColor.BLACK) Flags.BLACK_OVERTIME xor -1
                else Flags.WHITE_OVERTIME xor -1, IntBinOp.AND)
        }

    }

    override fun writeSGFNode(node: SGFNode) {
        val propMap = node.properties
        propMap[if (turnPlayer == GoColor.BLACK) "B" else "W"] =
            SGFProperty(SGFValue(playStoneAt?.let { SGFBytes(it.toString()) } ?: SGFBytes()))
        if (isForced) propMap["KO"] = SGFProperty(SGFValue(SGFBytes()))
        if (moveNumber != 0)
            propMap["MN"] = SGFProperty(SGFValue(SGFBytes(moveNumber.toString())))
        moveAnnotation?.let{
            propMap[it.code] = SGFProperty(SGFValue(when(it.extent) {
                0 -> SGFBytes()
                1 -> SGFBytes("1")
                else -> SGFBytes("2")
            }))
        }
        black.writeSGFTime(propMap, "BT", "OB")
        white.writeSGFTime(propMap, "WT", "OW")
    }

}

class GoSGFSetupNode: GoSGFNode {

    companion object {

        init {
            InternalGoSGF.setupSecrets = object: InternalGoSGF.SetupSecrets {
                override fun setupNode(parent: GoSGFNode, goban: AbstractGoban) = GoSGFSetupNode(parent, goban)
                override fun rootNode(tree: GoSGF) = GoSGFSetupNode(tree)
            }
        }

    }

    private constructor(tree: GoSGF): super(tree)

    private constructor(parent: GoSGFNode, goban: AbstractGoban): super(parent, goban)

    override var turnPlayer: GoColor? = null

    override fun writeSGFNode(node: SGFNode) {
        val propMap = node.properties
        val addBlack = goban.toMutablePointSet(GoColor.BLACK)
        val addWhite = goban.toMutablePointSet(GoColor.WHITE)
        var addEmpty: MutableGoPointSet? = null
        val prev = parent
        if (prev != null) {
            val prevGoban = prev.goban
            addBlack.removeAll(prevGoban.toPointSet(GoColor.BLACK))
            addWhite.removeAll(prevGoban.toPointSet(GoColor.WHITE))
            addEmpty = goban.toMutablePointSet(null)
            addEmpty.removeAll(prevGoban.toPointSet(null))
        }
        addBlack.toSGFProperty(false)?.let { propMap["AB"] = it }
        addWhite.toSGFProperty(false)?.let { propMap["AW"] = it }
        addEmpty?.toSGFProperty(false)?.let { propMap["AE"] = it }
        turnPlayer?.let { nextPlayer ->
            propMap["PL"] = SGFProperty(SGFValue(SGFBytes(
                if (nextPlayer == GoColor.BLACK) "B" else "W"
            )))
        }
    }

}