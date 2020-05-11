package com.computernerd1101.sgf

enum class SGFCopyLevel {

    NODE {
        override fun copy(o: Any, level: SGFCopyLevel): Any {
            if (o is SGFTree) return o.copy(level)
            return o
        }
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

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> copy(t: T) = copy(t, this) as T
}