package com.computernerd1101.goban.internal

import java.io.Serializable

internal class ImmutableEntry<K, V>(override val key: K, override val value: V):  Map.Entry<K, V>, Serializable {

    override fun equals(other: Any?): Boolean {
        return this === other || (other is Map.Entry<*, *> &&
                key == other.key && value == other.value)
    }

    override fun hashCode(): Int {
        return key.hashCode() xor value.hashCode();
    }

    override fun toString(): String {
        val value = this.value;
        return "$key=${if (value === this) "(this Map.Entry)" else value.toString()}"
    }

    companion object {
        private const val serialVersionUID = 1L;
    }

}