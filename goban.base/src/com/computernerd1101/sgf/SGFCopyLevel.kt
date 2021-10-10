package com.computernerd1101.sgf

import com.computernerd1101.goban.internal.InternalMarker
import com.computernerd1101.sgf.internal.SGFSubTreeList

enum class SGFCopyLevel {

    NODE {
        override fun copy(o: Any, level: SGFCopyLevel): Any = o
    },
    VALUE {
        override fun copy(o: Any, level: SGFCopyLevel): Any {
            if (o is SGFNode) return o.copy(level)
            return NODE.copy(o, level)
        }
    },
    COMPOSITE {
        override fun copy(o: Any, level: SGFCopyLevel): Any {
            if (o is SGFProperty) return o.copy(level)
            return VALUE.copy(o, level)
        }
    },
    BYTES {
        override fun copy(o: Any, level: SGFCopyLevel): Any {
            if (o is SGFValue) return o.copy(level)
            return COMPOSITE.copy(o, level)
        }
    },
    ALL {
        override fun copy(o: Any, level: SGFCopyLevel): Any {
            if (o is SGFBytes) return o.clone()
            return BYTES.copy(o, level)
        }
    };

    protected abstract fun copy(o: Any, level: SGFCopyLevel): Any

    @OptIn(ExperimentalStdlibApi::class)
    @Suppress("UNCHECKED_CAST")
    fun <T: Any> copy(t: T): T {
        if (t is SGFTree) return copyRecursive(t) as T
        return copy(t, this) as T
    }

    @OptIn(ExperimentalStdlibApi::class)
    private val copyRecursive = DeepRecursiveFunction<SGFTree, SGFTree> { tree ->
        val copy = SGFTree(tree, this@SGFCopyLevel, InternalMarker)
        val subTrees = copy.subTrees as SGFSubTreeList
        val elements = subTrees.elements
        for(i in subTrees.indices) {
            val subTree = elements[i] ?: continue
            elements[i] = callRecursive(subTree)
        }
        copy
    }

}