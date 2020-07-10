package com.computernerd1101.goban

@Suppress("unused")
enum class GoColor {

    BLACK, WHITE;

    val opponent : GoColor
        @JvmName("opponent")
        get() = if (this == BLACK) WHITE else BLACK

    inline val isBlack : Boolean
        get() = this == BLACK

    companion object {

        @JvmStatic
        fun valueOf(isBlack: Boolean?): GoColor? = isBlack?.goBlackOrWhite()

    }

}