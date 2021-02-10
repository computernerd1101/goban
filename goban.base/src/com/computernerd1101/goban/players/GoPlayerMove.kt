package com.computernerd1101.goban.players

import com.computernerd1101.goban.GoPoint
import com.computernerd1101.goban.internal.InternalMarker
import java.io.Serializable

@Suppress("unused")
sealed class GoPlayerMove: Serializable {

    class Stone internal constructor(val point: GoPoint?, marker: InternalMarker): GoPlayerMove() {

        @Transient
        private val string: String

        init {
            if (point == null)
                string = "GoPlayerMove.Pass"
            else {
                string = "GoPlayerMove.Stone($point)"
                point.setMove(this, marker)
            }
        }

        override fun equals(other: Any?): Boolean {
            return this === other || (other is Stone && point == other.point)
        }

        override fun hashCode(): Int = point.hashCode()

        override fun toString(): String = string

        private companion object {
            private const val serialVersionUID = 1L
        }

        private fun readResolve(): Any = point?.getMove(InternalMarker) ?: Pass

    }

    companion object {

        init {
            for(y in 0..51) for(x in 0..51) Stone(GoPoint(x, y), InternalMarker)
        }

        fun Stone(point: GoPoint): Stone = point.getMove(InternalMarker)

        fun Stone(x: Int, y: Int): Stone = GoPoint(x, y).getMove(InternalMarker)

        @JvmField
        val Pass = Stone(null, InternalMarker)

    }

    object Resign: GoPlayerMove() {

        private const val serialVersionUID = 1L

        private fun readResolve(): Any = Resign

    }

    object RequestUndo: GoPlayerMove() {

        private const val serialVersionUID = 1L

        private fun readResolve(): Any = RequestUndo

    }

    object AcceptUndo: GoPlayerMove() {

        private const val serialVersionUID = 1L

        private fun readResolve(): Any = AcceptUndo

    }

}
