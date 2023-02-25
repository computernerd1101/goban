package com.computernerd1101.sgf

import com.computernerd1101.goban.internal.InternalMarker
import com.computernerd1101.sgf.internal.SGFSubTreeList

/**
 * Represents the first level NOT to copy
 */
enum class SGFCopyLevel {

    /**
     * Only copy [SGFTree]s
     */
    NODE {
        override fun copy(o: Any, level: SGFCopyLevel): Any = o
    },

    /**
     * Copy [SGFTree]s and [SGFNode]s
     */
    VALUE {
        override fun copy(o: Any, level: SGFCopyLevel): Any {
            if (o is SGFNode) return o.copy(level)
            return NODE.copy(o, level)
        }
    },

    /**
     * Copy [SGFTree]s, [SGFNode]s and [SGFProperties](SGFProperty)
     */
    COMPOSITE {
        override fun copy(o: Any, level: SGFCopyLevel): Any {
            if (o is SGFProperty) return o.copy(level)
            return VALUE.copy(o, level)
        }
    },

    /**
     * Copy [SGFTree]s, [SGFNode]s, [SGFProperties](SGFProperty) and [SGFBytes]
     */
    BYTES {
        override fun copy(o: Any, level: SGFCopyLevel): Any {
            if (o is SGFValue) return o.copy(level)
            return COMPOSITE.copy(o, level)
        }
    },

    /**
     * Copy everything between [SGFTree]s and [SGFBytes]
     */
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
        return (if (t is SGFTree) copyTree(t) else copy(t, this)) as T
    }

    @OptIn(ExperimentalStdlibApi::class)
    private val copyTree: DeepRecursiveFunction<SGFTree, SGFTree> = DeepRecursiveFunction { tree ->
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