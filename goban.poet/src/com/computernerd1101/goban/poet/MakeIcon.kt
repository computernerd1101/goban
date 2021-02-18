package com.computernerd1101.goban.poet

import java.awt.*
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.*

internal inline fun makeIcon(width: Int, height: Int, draw: (Graphics2D) -> Unit): BufferedImage {
    val icon = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    draw(icon.graphics as Graphics2D)
    icon.flush()
    return icon
}