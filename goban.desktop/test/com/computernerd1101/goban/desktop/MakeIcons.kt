package com.computernerd1101.goban.desktop

import java.awt.*
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.*

fun main() {
    toolbar()
}

private fun treeView() {
    val dir = File("goban.desktop/src/com/computernerd1101/goban/desktop/icons/treeview")
        .absoluteFile
    val playBlack = makeIcon(16, 18) { g ->
        g.color = Color.BLACK
        g.fillOval(0, 1, 15, 15)
        g.color = Color.WHITE
        g.drawOval(0, 1, 15, 15)
    }
    val playWhite = makeIcon(16, 18) { g ->
        g.color = Color.WHITE
        g.fillOval(0, 1, 15, 15)
        g.color = Color.BLACK
        g.drawOval(0, 1, 15, 15)
    }
    val setupBlack = makeIcon(16, 18) { g ->
        g.color = Color.WHITE
        g.fillOval(3, 5, 12, 12)
        g.color = Color.BLACK
        g.fillOval(-1, -1, 14, 14)
        g.color = Color.WHITE
        g.drawOval(-1, -1, 14, 14)
        g.color = Color.BLACK
        g.drawOval(3, 5, 12, 12)
    }
    val setupWhite = makeIcon(16, 18) { g ->
        g.color = Color.BLACK
        g.fillOval(2, 4, 14, 14)
        g.color = Color.WHITE
        g.drawOval(2, 4, 14, 14)
        g.fillOval(0, 0, 12, 12)
        g.color = Color.BLACK
        g.drawOval(0, 0, 12, 12)
    }
    ImageIO.write(playBlack, "PNG", File(dir, "PlayBlack.png"))
    ImageIO.write(playWhite, "PNG", File(dir, "PlayWhite.png"))
    ImageIO.write(setupBlack, "PNG", File(dir, "SetupBlack.png"))
    ImageIO.write(setupWhite, "PNG", File(dir, "SetupWhite.png"))
}

private fun toolbar() {
    val dir = File("goban.desktop/src/com/computernerd1101/goban/desktop/icons/toolbar")
        .absoluteFile
    val playBlack = makeIcon(32, 32) { g ->
        g.color = Color.BLACK
        g.fillOval(0, 0, 31, 31)
        g.color = Color.WHITE
        g.drawOval(0, 0, 31, 31)
    }
    val playWhite = makeIcon(32, 32) { g ->
        g.color = Color.WHITE
        g.fillOval(0, 0, 31, 31)
        g.color = Color.BLACK
        g.drawOval(0, 0, 31, 31)
    }
    val addBlack = makeIcon(32, 32) { g ->
        g.color = Color.BLACK
        g.fillOval(7, 7, 24, 24)
        g.color = Color.WHITE
        g.drawOval(7, 7, 24, 24)
        g.color = Color.BLACK
        g.fillOval(0, 0, 24, 24)
        g.color = Color.WHITE
        g.drawOval(0, 0, 24, 24)
    }
    val addWhite = makeIcon(32, 32) { g ->
        g.color = Color.WHITE
        g.fillOval(7, 7, 24, 24)
        g.color = Color.BLACK
        g.drawOval(7, 7, 24, 24)
        g.color = Color.WHITE
        g.fillOval(0, 0, 24, 24)
        g.color = Color.BLACK
        g.drawOval(0, 0, 24, 24)
    }
    val addEmpty = makeIcon(32, 32) { g ->
        g.color = Color.GRAY
        g.stroke = BasicStroke(
            2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER,
            1f, floatArrayOf((Math.PI * 1.5).toFloat()), 0f
        )
        g.drawOval(7, 7, 24, 24)
        g.drawOval(0, 0, 24, 24)
    }
    val labelMarkup = makeIcon(32, 32) { g ->
        g.color = Color.RED
        g.translate(16, 0)
        val fontMetrics = g.fontMetrics
        val fontWidth = fontMetrics.stringWidth("A")
        val fontScale = 32f / fontMetrics.height
        g.scale(fontScale.toDouble(), fontScale.toDouble())
        g.drawString("A", -0.5f * fontWidth, fontMetrics.ascent.toFloat())
    }
    val selectMarkup = makeIcon(32, 32) { g ->
        g.color = Color.RED
        g.stroke = BasicStroke(2f)
        g.drawOval(8, 8, 16, 16)
    }
    val xMarkup = makeIcon(32, 32) { g ->
        g.color = Color.RED
        g.stroke = BasicStroke(2f)
        g.drawLine(8, 8, 24, 24)
        g.drawLine(8, 24, 24, 8)
    }
    val triangleMarkup = makeIcon(32, 32) { g ->
        g.color = Color.RED
        val path = Path2D.Double()
        val halfWidth = (16 / Math.sqrt(3.0)).toInt()
        path.moveTo(16.0, 8.0)
        path.lineTo(16 - halfWidth.toDouble(), 24.0)
        path.lineTo(16 + halfWidth.toDouble(), 24.0)
        path.lineTo(16.0, 8.0)
        g.fill(path)
    }
    val circleMarkup = makeIcon(32, 32) { g ->
        g.color = Color.RED
        g.fillOval(8, 8, 16, 16)
    }
    val squareMarkup = makeIcon(32, 32) { g ->
        g.color = Color.RED
        g.fillRect(8, 8, 16, 16)
    }
    val deletePointMarkup = makeIcon(32, 32) { g ->
        g.color = Color.GRAY
        g.stroke = BasicStroke(2f)
        g.drawLine(8, 8, 24, 24)
        g.drawLine(8, 24, 24, 8)
    }
    val lineMarkup = makeIcon(32, 32) { g ->
        g.color = Color.RED
        g.stroke = BasicStroke(2f)
        g.drawLine(4, 28, 28, 4)
    }
    val arrowMarkup = makeIcon(32, 32) { g ->
        g.color = Color.RED
        g.stroke = BasicStroke(2f)
        g.drawLine(4, 28, 28, 4)
        g.drawLine(16, 4, 28, 4)
        g.drawLine(28, 4, 28, 16)
    }
    val deleteLineMarkup = makeIcon(32, 32) { g ->
        g.color = Color.GRAY
        g.stroke = BasicStroke(2f)
        g.drawLine(4, 28, 28, 4)
    }
    val dim = makeIcon(32, 32) { g ->
        g.color = Color.GRAY
        g.translate(16, 0)
        val fontMetrics = g.fontMetrics
        val fontWidth = fontMetrics.stringWidth("D")
        val fontScale = 32f / fontMetrics.height
        g.scale(fontScale.toDouble(), fontScale.toDouble())
        g.drawString("D", -0.5f * fontWidth, fontMetrics.ascent.toFloat())
    }
    val resetDim = makeIcon(32, 32) { g ->
        g.color = Color.RED
        g.translate(16, 0)
        val fontMetrics = g.fontMetrics
        val fontWidth = fontMetrics.stringWidth("D")
        val fontScale = 32f / fontMetrics.height
        g.scale(fontScale.toDouble(), fontScale.toDouble())
        g.drawString("D", -0.5f * fontWidth, fontMetrics.ascent.toFloat())
    }
    val inheritDim = makeIcon(32, 32) { g ->
        g.color = Color.BLACK
        g.translate(16, 0)
        val fontMetrics = g.fontMetrics
        val fontWidth = fontMetrics.stringWidth("D")
        val fontScale = 32f / fontMetrics.height
        g.scale(fontScale.toDouble(), fontScale.toDouble())
        g.drawString("D", -0.5f * fontWidth, fontMetrics.ascent.toFloat())
    }
    val visible = makeIcon(32, 32) { g ->
        g.color = Color.BLACK
        g.translate(16, 0)
        val fontMetrics = g.fontMetrics
        val fontWidth = fontMetrics.stringWidth("V")
        val fontScale = 32f / fontMetrics.height
        g.scale(fontScale.toDouble(), fontScale.toDouble())
        g.drawString("V", -0.5f * fontWidth, fontMetrics.ascent.toFloat())
    }
    val resetVisible = makeIcon(32, 32) { g ->
        g.color = Color.RED
        g.translate(16, 0)
        val fontMetrics = g.fontMetrics
        val fontWidth = fontMetrics.stringWidth("V")
        val fontScale = 32f / fontMetrics.height
        g.scale(fontScale.toDouble(), fontScale.toDouble())
        g.drawString("V", -0.5f * fontWidth, fontMetrics.ascent.toFloat())
    }
    val inheritVisible = makeIcon(32, 32) { g ->
        g.color = Color.GRAY
        g.translate(16, 0)
        val fontMetrics = g.fontMetrics
        val fontWidth = fontMetrics.stringWidth("V")
        val fontScale = 32f / fontMetrics.height
        g.scale(fontScale.toDouble(), fontScale.toDouble())
        g.drawString("V", -0.5f * fontWidth, fontMetrics.ascent.toFloat())
    }
    SwingUtilities.invokeLater {
        val frame = JFrame()
        frame.title = "CN13 Goban Toolbar"
        frame.setSize(1000, 500)
        frame.setLocationRelativeTo(null)
        frame.layout = BorderLayout()
        frame.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        val toolBar = JToolBar()
        toolBar.isFloatable = false
        frame.add(toolBar, BorderLayout.PAGE_START)
        var ico = ImageIcon(playBlack, "Black to play")
        var toggle = JToggleButton(ico)
        toolBar.add(toggle)
        ico = ImageIcon(playWhite, "White to play")
        toggle = JToggleButton(ico)
        toolBar.add(toggle)
        toolBar.addSeparator()
        ico = ImageIcon(addBlack, "Add black")
        toggle = JToggleButton(ico)
        toolBar.add(toggle)
        ico = ImageIcon(addWhite, "Add white")
        toggle = JToggleButton(ico)
        toolBar.add(toggle)
        ico = ImageIcon(addEmpty, "Remove stones")
        toggle = JToggleButton(ico)
        toolBar.add(toggle)
        toolBar.addSeparator()
        ico = ImageIcon(labelMarkup, "Label markup")
        toggle = JToggleButton(ico)
        toolBar.add(toggle)
        ico = ImageIcon(selectMarkup, "Selection markup")
        toggle = JToggleButton(ico)
        toolBar.add(toggle)
        ico = ImageIcon(xMarkup, "X markup")
        toggle = JToggleButton(ico)
        toolBar.add(toggle)
        ico = ImageIcon(triangleMarkup, "Triangle markup")
        toggle = JToggleButton(ico)
        toolBar.add(toggle)
        ico = ImageIcon(circleMarkup, "Circle markup")
        toggle = JToggleButton(ico)
        toolBar.add(toggle)
        ico = ImageIcon(squareMarkup, "Square markup")
        toggle = JToggleButton(ico)
        toolBar.add(toggle)
        ico = ImageIcon(deletePointMarkup, "Delete point markup")
        toggle = JToggleButton(ico)
        toolBar.add(toggle)
        ico = ImageIcon(lineMarkup, "Line markup")
        toggle = JToggleButton(ico)
        toolBar.add(toggle)
        ico = ImageIcon(arrowMarkup, "Arrow markup")
        toggle = JToggleButton(ico)
        toolBar.add(toggle)
        ico = ImageIcon(deleteLineMarkup, "Delete line markup")
        toggle = JToggleButton(ico)
        toolBar.add(toggle)
        toolBar.addSeparator()
        ico = ImageIcon(dim, "Dim part of board")
        toggle = JToggleButton(ico)
        toolBar.add(toggle)
        ico = ImageIcon(resetDim, "Un-dim entire board")
        toolBar.add(JButton(ico))
        ico = ImageIcon(inheritDim, "Inherit dim part of board")
        toggle = JToggleButton(ico)
        toolBar.add(toggle)
        toolBar.addSeparator()
        ico = ImageIcon(visible, "Visible part of board")
        toggle = JToggleButton(ico)
        toolBar.add(toggle)
        ico = ImageIcon(resetVisible, "Reset visibility of entire board")
        toolBar.add(JButton(ico))
        ico = ImageIcon(inheritVisible, "Inherit visible part of board")
        toggle = JToggleButton(ico)
        toolBar.add(toggle)
        val panel = JPanel(GridBagLayout())
        frame.add(panel, BorderLayout.CENTER)
        frame.isVisible = true
    }
    ImageIO.write(playBlack, "PNG", File(dir, "Black.png"))
    ImageIO.write(playWhite, "PNG", File(dir, "White.png"))
    ImageIO.write(labelMarkup, "PNG", File(dir, "LabelMarkup.png"))
    ImageIO.write(selectMarkup, "PNG", File(dir, "SelectMarkup.png"))
    ImageIO.write(xMarkup, "PNG", File(dir, "XMarkup.png"))
    ImageIO.write(triangleMarkup, "PNG", File(dir, "TriangleMarkup.png"))
    ImageIO.write(circleMarkup, "PNG", File(dir, "CircleMarkup.png"))
    ImageIO.write(squareMarkup, "PNG", File(dir, "SquareMarkup.png"))
    ImageIO.write(deletePointMarkup, "PNG", File(dir, "DeletePointMarkup.png"))
    ImageIO.write(lineMarkup, "PNG", File(dir, "LineMarkup.png"))
    ImageIO.write(arrowMarkup, "PNG", File(dir, "ArrowMarkup.png"))
    ImageIO.write(deleteLineMarkup, "PNG", File(dir, "DeleteLineMarkup.png"))
    ImageIO.write(dim, "PNG", File(dir, "Dim.png"))
    ImageIO.write(resetDim, "PNG", File(dir, "ResetDim.png"))
    ImageIO.write(inheritDim, "PNG", File(dir, "InheritDim.png"))
    ImageIO.write(visible, "PNG", File(dir, "Visible.png"))
    ImageIO.write(resetVisible, "PNG", File(dir, "ResetVisible.png"))
    ImageIO.write(inheritVisible, "PNG", File(dir, "InheritVisible.png"))
}

private inline fun makeIcon(width: Int, height: Int, draw: (Graphics2D) -> Unit): BufferedImage {
    val icon = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    draw(icon.graphics as Graphics2D)
    icon.flush()
    return icon
}