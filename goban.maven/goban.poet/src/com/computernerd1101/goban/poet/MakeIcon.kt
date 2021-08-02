package com.computernerd1101.goban.poet

import java.awt.*
import java.awt.image.BufferedImage

internal inline fun makeIcon(width: Int, height: Int, draw: (Graphics2D) -> Unit): BufferedImage {
    val icon = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    draw(icon.graphics as Graphics2D)
    icon.flush()
    return icon
}