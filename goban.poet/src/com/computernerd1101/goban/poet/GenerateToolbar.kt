package com.computernerd1101.goban.poet

import java.awt.*
import java.awt.geom.Path2D
import java.io.File
import javax.imageio.ImageIO
import javax.swing.*
import kotlin.math.sqrt

fun main(args: Array<String>) {
    val dir = File("goban.desktop/src/com/computernerd1101/goban/desktop/icons/toolbar")
        .absoluteFile
    val playBlack = playStone(Color.BLACK, Color.WHITE)
    val playWhite = playStone(Color.WHITE, Color.BLACK)
    val labelMarkup = toolbarText("A", Color.RED)
    val selectMarkup = makeIcon(32, 32) { g ->
        g.color = Color.RED
        g.stroke = BasicStroke(2f)
        g.drawOval(8, 8, 16, 16)
    }
    val xMarkup = toolbarX(Color.RED)
    val triangleMarkup = makeIcon(32, 32) { g ->
        g.color = Color.RED
        val path = Path2D.Double()
        val halfWidth = (16 / sqrt(3.0)).toInt()
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
    val deletePointMarkup = toolbarX(Color.GRAY)
    val lineMarkup = toolbarLine(Color.RED, isArrow = false)
    val arrowMarkup = toolbarLine(Color.RED, isArrow = true)
    val deleteLineMarkup = toolbarLine(Color.GRAY, isArrow = false)
    val dim = toolbarText("D", Color.GRAY)
    val resetDim = toolbarText("D", Color.RED)
    val inheritDim = toolbarText("D", Color.BLACK)
    val visible = toolbarText("V", Color.BLACK)
    val resetVisible = toolbarText("V", Color.RED)
    val inheritVisible = toolbarText("V", Color.GRAY)
    if (GenerateAll.NO_PREVIEW !in args) {
        val addBlack = toolbarAddStone(Color.BLACK, Color.WHITE)
        val addWhite = toolbarAddStone(Color.WHITE, Color.BLACK)
        val addEmpty = makeIcon(32, 32) { g ->
            g.color = Color.GRAY
            g.stroke = BasicStroke(
                2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER,
                1f, floatArrayOf((Math.PI * 1.5).toFloat()), 0f
            )
            g.drawOval(7, 7, 24, 24)
            g.drawOval(0, 0, 24, 24)
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

fun playStone(fill: Color, draw: Color) =  makeIcon(32, 32) { g ->
    g.color = fill
    g.fillOval(0, 0, 31, 31)
    g.color = draw
    g.drawOval(0, 0, 31, 31)
}

fun toolbarText(text: String, color: Color) = makeIcon(32, 32) { g ->
    g.color = color
    g.translate(16, 0)
    val fontMetrics = g.fontMetrics
    val fontWidth = fontMetrics.stringWidth(text)
    val fontScale = 32f / fontMetrics.height
    g.scale(fontScale.toDouble(), fontScale.toDouble())
    g.drawString(text, -0.5f * fontWidth, fontMetrics.ascent.toFloat())
}

fun toolbarX(color: Color) = makeIcon(32, 32) { g ->
    g.color = color
    g.stroke = BasicStroke(2f)
    g.drawLine(8, 8, 24, 24)
    g.drawLine(8, 24, 24, 8)
}

fun toolbarLine(color: Color, isArrow: Boolean) = makeIcon(32, 32) { g ->
    g.color = color
    g.stroke = BasicStroke(2f)
    g.drawLine(4, 28, 28, 4)
    if (isArrow) {
        g.drawLine(16, 4, 28, 4)
        g.drawLine(28, 4, 28, 16)
    }
}

fun toolbarAddStone(fill: Color, draw: Color) = makeIcon(32, 32) { g ->
    g.color = fill
    g.fillOval(7, 7, 24, 24)
    g.color = draw
    g.drawOval(7, 7, 24, 24)
    g.color = fill
    g.fillOval(0, 0, 24, 24)
    g.color = draw
    g.drawOval(0, 0, 24, 24)
}