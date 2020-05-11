package com.computernerd1101.goban.desktop

import com.computernerd1101.goban.*
import com.computernerd1101.goban.markup.*
import java.awt.*
import java.awt.event.MouseEvent
import java.awt.geom.*
import javax.swing.JComponent
import kotlin.math.*

// Kotlin does not yet support properties with final getters but open setters,
// nor vice versa, for that matter. Believe me, I've tried.
@Suppress("DEPRECATION")
inline var GobanView.goban: AbstractGoban?
    get() = getGoban()
    set(goban) = setGoban(goban)

@Suppress("DEPRECATION")
inline var GobanView.pointMarkup: PointMarkupMap?
    get() = getPointMarkup()
    set(map) = setPointMarkup(map)

@Suppress("DEPRECATION")
inline var GobanView.lineMarkup: LineMarkupSet?
    get() = getLineMarkup()
    set(set) = setLineMarkup(set)

@Suppress("DEPRECATION", "unused")
inline var GobanView.gobanBackground: Paint
    get() = getGobanBackground()
    set(paint) = setGobanBackground(paint)

@Suppress("DEPRECATION", "unused")
inline var GobanView.defaultMarkupColor: Color
    get() = getDefaultMarkupColor()
    set(color) = setDefaultMarkupColor(color)

@Suppress("DEPRECATION", "unused")
inline var GobanView.edgeThickness: Int
    get() = getEdgeThickness()
    set(thickness) = setEdgeThickness(thickness)

@Suppress("DEPRECATION", "unused")
inline var GobanView.defaultPointMarkupThickness: Int
    get() = getDefaultPointMarkupThickness()
    set(thickness) = setDefaultPointMarkupThickness(thickness)

@Suppress("DEPRECATION", "unused")
inline var GobanView.defaultLineMarkupThickness: Int
    get() = getDefaultLineMarkupThickness()
    set(thickness) = setDefaultLineMarkupThickness(thickness)

open class GobanView
@JvmOverloads constructor(private var goban: AbstractGoban? = null): JComponent() {

    init {
        foreground = Color.BLACK
        enableEvents(AWTEvent.MOUSE_EVENT_MASK)
    }

    @get:JvmName("gridScale")
    protected var gridScale: Double = 0.0; private set
    private var startX: Int = 0
    private var startY: Int = 0

    @Deprecated("use this.goban instead", ReplaceWith("this.goban"))
    fun getGoban() = goban
    open fun setGoban(goban: AbstractGoban?) {
        val old = this.goban
        this.goban = goban
        if (old !== goban) {
            if (old == null || goban == null ||
                    old.width != goban.width ||
                    old.height != goban.height)
                gridScale = 0.0
        }
        revalidate()
        repaint()
    }

    private var pointMarkup: PointMarkupMap? = null
    @Deprecated("use this.pointMarkup instead", ReplaceWith("this.pointMarkup"))
    fun getPointMarkup() = pointMarkup
    open fun setPointMarkup(map: PointMarkupMap?) {
        val old = pointMarkup
        pointMarkup = map
        if (old.isNullOrEmpty() != map.isNullOrEmpty()) {
            revalidate()
            repaint()
        }
    }

    private var lineMarkup: LineMarkupSet? = null
    fun getLineMarkup() = lineMarkup
    @Deprecated("use this.lineMarkup instead", ReplaceWith("this.lineMarkup"))
    open fun setLineMarkup(set: LineMarkupSet?) {
        val old = lineMarkup
        lineMarkup = set
        if (old.isNullOrEmpty() != set.isNullOrEmpty()) {
            revalidate()
            repaint()
        }
    }

    private var gobanBackground: Paint = Color.ORANGE
    @Deprecated("Use this.gobanBackground instead", ReplaceWith("this.gobanBackground"))
    fun getGobanBackground() = gobanBackground
    open fun setGobanBackground(paint: Paint) {
        val old = gobanBackground
        gobanBackground = paint
        if (old != paint) {
            revalidate()
            repaint()
        }
    }

    private var defaultMarkupColor: Color = Color.RED
    @Deprecated("Use this.defaultMarkupColor instead", ReplaceWith("this.defaultMarkupColor"))
    fun getDefaultMarkupColor() = defaultMarkupColor
    open fun setDefaultMarkupColor(color: Color) {
        val old = defaultMarkupColor
        defaultMarkupColor = color
        if (old != color) {
            revalidate()
            repaint()
        }
    }

    private var edgeThickness: Int = 3
    @Deprecated("Use this.edgeThickness instead", ReplaceWith("this.edgeThickness"))
    fun getEdgeThickness() = edgeThickness
    open fun setEdgeThickness(value: Int) = setThickness(value) { thickness ->
        val old = edgeThickness
        edgeThickness = thickness
        old
    }

    private var defaultPointMarkupThickness: Int = 2
    @Deprecated("Use this.defaultPointMarkupThickness instead",
        ReplaceWith("this.defaultPointMarkupThickness"))
    fun getDefaultPointMarkupThickness() = defaultPointMarkupThickness
    open fun setDefaultPointMarkupThickness(value: Int) = setThickness(value) { thickness ->
        val old = defaultPointMarkupThickness
        defaultPointMarkupThickness = thickness
        old
    }

    private var defaultLineMarkupThickness: Int = 2
    @Deprecated("Use this.defaultLineMarkupThickness instead",
        ReplaceWith("this.defaultLineMarkupThickness"))
    fun getDefaultLineMarkupThickness() = defaultLineMarkupThickness
    open fun setDefaultLineMarkupThickness(value: Int) = setThickness(value) { thickness ->
        val old = defaultLineMarkupThickness
        defaultLineMarkupThickness = thickness
        old
    }

    private inline fun setThickness(value: Int, swap: (Int) -> Int) {
        val thickness = when {
            value < 0 -> -value
            value == 0 -> 1
            else -> value
        }
        val old = swap(thickness)
        if (old != thickness) {
            revalidate()
            repaint()
        }
    }

    companion object {
        @JvmField
        val thinStroke = BasicStroke(0f)

        @JvmStatic
        fun applyAlpha(color: Color, alpha: Float): Color {
            if (alpha >= 1f) return color
            val baseAlpha = (color.alpha * alpha.coerceAtLeast(0f)).toInt()
            val argb = (baseAlpha shl 24) or (color.rgb and 0xFFFFFF)
            return Color(argb, true)
        }
    }

    fun toGoPoint(event: MouseEvent): GoPoint? {
        return toGoPoint(event.x, event.y)
    }

    fun toGoPoint(x: Int, y: Int): GoPoint? {
        val goban = this.goban
        val scale = gridScale
        if (goban != null && scale != 0.0) {
            val left = startX
            val top = startY
            if (x >= left && y >= top) {
                val gx = ((x - left) / scale).toInt()
                val gy = ((y - top) / scale).toInt()
                if (gx < goban.width && gy < goban.height)
                    return GoPoint(gx, gy)
            }
        }
        return null
    }

    fun getStoneAt(p: GoPoint): GoColor? {
        val goban = this.goban ?: return null
        return if (p.x >= goban.width || p.y >= goban.height) null
        else goban[p]
    }

    open fun getStoneColorAt(p: GoPoint) = getStoneAt(p)

    open fun getPointMarkupAt(p: GoPoint): PointMarkup? {
        val goban = this.goban ?: return null
        return if (p.x >= goban.width || p.y >= goban.height) null
        else pointMarkup?.get(p)
    }

    open fun isVisible(p: GoPoint) = true

    open fun getAlphaAt(p: GoPoint) = 1f

    open fun getStoneAlphaAt(p: GoPoint) = 1f

    open fun getMarkupAlphaAt(p: GoPoint) = 1f

    open fun getMarkupColorAt(p: GoPoint) = defaultMarkupColor

    open fun isLineMarkupVisible(lm: LineMarkup) = true

    fun getPointMarkupThickness(p: GoPoint): Int {
        val thickness = computePointMarkupThickness(p)
        return when {
            thickness < 0 -> -thickness
            thickness == 0 -> 1
            else -> thickness
        }
    }

    protected open fun computePointMarkupThickness(p: GoPoint) = defaultPointMarkupThickness

    fun getLineMarkupThickness(lm: LineMarkup): Int {
        val thickness = computeLineMarkupThickness(lm)
        return when {
            thickness < 0 -> -thickness
            thickness == 0 -> 1
            else -> thickness
        }
    }

    protected open fun computeLineMarkupThickness(lm: LineMarkup) = defaultLineMarkupThickness

    fun isStarPoint(p: GoPoint): Boolean {
        val goban = this.goban ?: return false
        val width = goban.width
        val height = goban.height
        val x1 = getStarPoint(width)
        if (x1 <= 0) return false
        val x2 = if (width and 1 == 0) -1 else (width shr 1)
        val x3 = width - 1 - x1
        val y1 = getStarPoint(height)
        if (y1 <= 0) return false
        val y2 = if (height and 1 == 0) -1 else (height shr 1)
        val y3 = height - 1 - y1
        val (x, y) = p
        return (x == x1 || x == x2 || x == x3) &&
                (y == y1 || y == y2 || y == y3)
    }

    open fun getStarPoint(size: Int): Int {
        return when {
            size < 5 -> 0
            size < 7 -> 1
            size < 12 -> 2
            else -> 3
        }
    }

    fun getMarkupX(p: GoPoint): Shape {
        val (x, y) = p
        val path = Path2D.Double()
        path.moveTo(x - 0.25, y - 0.25)
        path.lineTo(x + 0.25, y + 0.25)
        path.moveTo(x - 0.25, y + 0.25)
        path.lineTo(x + 0.25, y - 0.25)
        return path
    }

    fun getMarkupTriangle(p: GoPoint): Shape {
        val halfWidth = 0.21650635094610965 // sqrt(3)/8
        val (x, y) = p
        val path = Path2D.Double()
        path.moveTo(x.toDouble(), y - 0.25)
        path.lineTo(x + halfWidth, y + 0.125)
        path.lineTo(x - halfWidth, y + 0.125)
        path.lineTo(x.toDouble(), y - 0.25)
        return path
    }

    fun getMarkupCircle(p: GoPoint): Shape {
        val (x, y) = p
        return Ellipse2D.Double(x - 0.25, y - 0.25, 0.5, 0.5)
    }

    fun getMarkupSquare(p: GoPoint): Shape {
        val (x, y) = p
        return Rectangle2D.Double(x - 0.25, y - 0.25, 0.5, 0.5)
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        val goban = goban ?: return
        val g2d = g!!.create() as Graphics2D
        val gobanWidth = goban.width
        val gobanHeight = goban.height
        val width = gobanWidth + 3
        val height = gobanHeight + 3
        var paintWidth = this.width
        var paintHeight = this.height
        var paintX = 0
        var paintY = 0
        val scale: Double
        // if (width/height > paintWidth/paintHeight) {...}
        if (width * paintHeight.toLong() > height * paintWidth.toLong()) {
            scale = paintWidth.toDouble() / width
            val oldHeight = paintHeight
            paintHeight = (height * paintWidth.toLong() / width).toInt()
            paintY = oldHeight - paintHeight shr 1
        } else {
            scale = paintHeight.toDouble() / height
            if (width * paintHeight.toLong() < height * paintWidth.toLong()) {
                val oldWidth = paintWidth
                paintWidth = (width * paintHeight.toLong() / height).toInt()
                paintX = oldWidth - paintWidth shr 1
            }
        }
        val gridStart = (1.5 * scale).toInt()
        startX = paintX + gridStart
        startY = paintY + gridStart
        gridScale = scale
        // Draw background
        g2d.paint = gobanBackground
        g2d.fillRect(paintX, paintY, paintWidth, paintHeight)
        g2d.translate(paintX, paintY)
        g2d.scale(scale, scale)
        g2d.translate(2, 2)
        paintGoban(g2d)
    }

    protected open fun paintGoban(g: Graphics2D) {
        val goban = goban ?: return
        val width = goban.width
        val height = goban.height
        val metrics = g.fontMetrics
        g.paint = foreground
        var fontGraphics = g.create() as Graphics2D
        fontGraphics.translate(0.0, -1.5)
        val fontHeight = metrics.height
        val fontWidth = metrics.stringWidth("M ")
        var fontScale = 1.0 / fontWidth
        fontGraphics.scale(fontScale, fontScale)
        val buffer = CharArray(2)
        for (x in 0 until width) {
            var ch: Char
            when {
                x == 52 -> ch = 'i'
                x == 51 -> ch = 'I'
                x >= 25 -> {
                    ch = ('a' - 25) + x
                    if (ch >= 'i') ch++
                }
                else -> {
                    ch = 'A' + x
                    if (ch >= 'I') ch++
                }
            }
            buffer[0] = ch
            val label = String(buffer, 0, 1)
            val xPos = x * fontWidth - metrics.charWidth(ch) * 0.5f
            fontGraphics.drawString(label, xPos, fontHeight - fontWidth * 0.25f)
            fontGraphics.drawString(label, xPos, fontHeight + (height + 0.75f) * fontWidth)
        }
        fontGraphics = g.create() as Graphics2D
        fontGraphics.translate(-0.5, 0.5)
        fontScale = 1.0 / fontHeight
        fontGraphics.scale(fontScale, fontScale)
        val descent = metrics.descent
        for (y in 0 until height) {
            val y2 = height - y
            val length = if (y2 < 10) {
                buffer[0] = '0' + y2
                1
            } else {
                buffer[0] = '0' + (y2 / 10)
                buffer[1] = '0' + (y2 % 10)
                2
            }
            val label = String(buffer, 0, length)
            val yPos = y * fontHeight - descent.toFloat()
            fontGraphics.drawString(
                label, -(metrics.stringWidth(label) + fontHeight * 0.25f),
                yPos
            )
            fontGraphics.drawString(label, (width + 0.25f) * fontHeight, yPos)
        }
        paintAllGoPoints(g)
        paintAllLineMarkups(g)
        paintAllPointMarkups(g)
    }

    protected open fun paintAllGoPoints(g: Graphics2D) {
        val goban = this.goban ?: return
        val width = goban.width
        val height = goban.height
        for (y in 0 until height) {
            for (x in 0 until width) {
                val p = GoPoint(x, y)
                paintGoPoint(g, p)
            }
        }
    }

    @Suppress("DuplicatedCode")
    protected open fun paintGoPoint(g: Graphics2D, p: GoPoint) {
        if (!isVisible(p)) return
        val goban = this.goban ?: return
        val width = goban.width
        val height = goban.height
        val x = p.x
        val y = p.y
        if (x >= width || y >= height) return
        g.stroke = thinStroke
        val stone = getStoneColorAt(p)
        var stoneAlpha = 1f
        var circle: Ellipse2D.Double? = null
        val starRadius = 0.125
        val starDiameter = 0.25
        run drawGrid@{
            var alpha = getAlphaAt(p)
            if (!(alpha > 0f && alpha < 1f))
                alpha = 1f
            if (stone != null) {
                stoneAlpha = getStoneAlphaAt(p)
                if (stoneAlpha > 0f && stoneAlpha < 1f)
                    stoneAlpha *= alpha
                else {
                    stoneAlpha = alpha
                    return@drawGrid
                }
            }
            g.paint = applyAlpha(foreground, alpha)
            val scale = gridScale
            var thick = edgeThickness.toDouble().absoluteValue
            val thin = 1.0 / scale
            val isThick = thick > 1.0
            thick = if (isThick) (thick / scale).coerceAtMost(0.25)
            else thin
            val rect: Rectangle2D.Double
            var line: Line2D.Double? = null
            when {
                width == 1 && height == 1 -> {
                    rect = Rectangle2D.Double(-0.5*thick, -0.5*thick, thick, thick)
                    g.fill(rect)
                }
                x == 0 || x == width - 1 -> {
                    if (isThick) {
                        rect = Rectangle2D.Double(
                            x - 0.5*thick, if (y == 0) -0.5*thick else y - 0.5,
                            thick, if (y == 0 || y == height - 1) 0.5 + 0.5*thick else 1.0)
                        g.fill(rect)
                        if (height != 1 && (y == 0 || y == height - 1)) {
                            rect.x = if (x == 0) 0.5*thick else x - 0.5
                            rect.y = y - 0.5*thick
                            rect.width = 0.5 - 0.5*thick
                            rect.height = thick
                            g.fill(rect)
                        }
                    } else {
                        line = Line2D.Double(
                            x.toDouble(), if (y == 0) 0.0 else y - 0.5,
                            x.toDouble(), if (y == height - 1) y.toDouble() else y + 0.5)
                        g.draw(line)
                    }
                    if (height != 1 && !(isThick && (y == 0 || y == height - 1))) {
                        if (line == null) line = Line2D.Double()
                        if (x == 0) {
                            line.x1 = 0.5*thick
                            line.x2 = 0.5
                        } else {
                            line.x1 = x - 0.5
                            line.x2 = x - 0.5*thick
                        }
                        line.y1 = y.toDouble()
                        line.y2 = y.toDouble()
                        g.draw(line)
                    }
                }
                y == 0 || y == height - 1 -> {
                    line = Line2D.Double()
                    if (isThick) {
                        rect = Rectangle2D.Double(x - 0.5, y - 0.5*thick, 1.0, thick)
                        g.fill(rect)
                    } else {
                        line.x1 = x - 0.5
                        line.x2 = x + 0.5
                        line.y1 = y.toDouble()
                        line.y2 = y.toDouble()
                        g.draw(line)
                    }
                    if (y == 0) {
                        line.y1 = 0.5*thick
                        line.y2 = 0.5
                    } else {
                        line.y1 = y - 0.5
                        line.y2 = y - 0.5*thick
                    }
                    line.x1 = x.toDouble()
                    line.x2 = x.toDouble()
                    g.draw(line)
                }
                else -> {
                    line = Line2D.Double()
                    line.x1 = x - 0.5
                    line.y1 = y.toDouble()
                    line.y2 = y.toDouble()
                    val ySkip: Double = if (isStarPoint(p)) {
                        circle = Ellipse2D.Double(x - starRadius, y - starRadius,
                            starDiameter, starDiameter)
                        g.fill(circle)
                        line.x2 = x - starRadius
                        g.draw(line)
                        line.x1 = x + starRadius
                        starRadius
                    } else 0.5*thin
                    line.x2 = x + 0.5
                    g.draw(line)
                    line.x1 = x.toDouble()
                    line.x2 = x.toDouble()
                    line.y1 = y - 0.5
                    line.y2 = y - ySkip
                    g.draw(line)
                    line.y1 = y + ySkip
                    line.y2 = y + 0.5
                    g.draw(line)
                }
            }
        }
        var stoneFill: Color
        var stoneEdge: Color
        if (stoneAlpha == 1f) {
            stoneFill = Color.BLACK
            stoneEdge = Color.WHITE
        } else {
            val alphaMask = (stoneAlpha * 255f).toInt() shl 24
            stoneFill = Color(alphaMask, true)
            stoneEdge = Color(alphaMask or 0xFFFFFF, true)
        }
        if (stone == GoColor.WHITE) {
            val tmp = stoneFill
            stoneFill = stoneEdge
            stoneEdge = tmp
        }
        if (stone != null) {
            circle = (circle ?: Ellipse2D.Double()).apply {
                this.x = x - 0.5
                this.y = y - 0.5
                this.width = 1.0
                this.height = 1.0
            }
            g.paint = stoneFill
            g.fill(circle)
            g.paint = stoneEdge
            g.draw(circle)
        }
    }

    private fun setStrokeThickness(g: Graphics2D, prevThickness: Float, newThickness: Int): Float {
        val thickness = if (newThickness == 1) 1f
        else (newThickness / gridScale).toFloat()
        return when {
            thickness > 0.125f -> {
                g.stroke = thinStroke
                0f
            }
            thickness != prevThickness -> {
                g.stroke = BasicStroke(thickness)
                thickness
            }
            else -> prevThickness
        }
    }

    protected open fun paintAllPointMarkups(g: Graphics2D) {
        val goban = this.goban ?: return
        pointMarkup ?: return
        val width = goban.width
        val height = goban.height
        var prevThickness = 0f
        for(y in 0 until height) {
            for(x in 0 until width) {
                val p = GoPoint(x, y)
                val m = getPointMarkupAt(p) ?: continue
                val color = getMarkupColorAt(p)
                g.paint = applyAlpha(color, getMarkupAlphaAt(p))
                prevThickness = setStrokeThickness(g, prevThickness, getPointMarkupThickness(p))
                paintPointMarkup(g, p, m)
            }
        }
    }

    protected open fun paintPointMarkup(g: Graphics2D, p: GoPoint, m: PointMarkup) {
        val label = m.label
        when {
            label.isNotEmpty() -> paintMarkupLabel(g, p, label)
            m == PointMarkup.SELECT -> paintMarkupSelect(g, p)
            m == PointMarkup.X -> paintMarkupX(g, p)
            m == PointMarkup.TRIANGLE -> paintMarkupTriangle(g, p)
            m == PointMarkup.CIRCLE -> paintMarkupCircle(g, p)
            m == PointMarkup.SQUARE -> paintMarkupSquare(g, p)
        }
    }

    protected open fun paintMarkupLabel(g: Graphics2D, p: GoPoint, label: String) {
        val (x, y) = p
        val metrics = g.fontMetrics
        val fontGraphics = g.create() as Graphics2D
        fontGraphics.translate(-0.5, 0.5)
        val fontHeight = metrics.height
        val fontWidth = metrics.stringWidth(label)
        val descent = metrics.descent
        val xPos: Float
        val fontScaleX: Float
        val fontScaleY = 1f / fontHeight
        if (fontHeight > fontWidth) {
            fontScaleX = fontScaleY
            xPos = x*fontHeight + (fontHeight - fontWidth)*0.5f
        } else {
            fontScaleX = 1f / fontWidth
            xPos = (x * fontWidth).toFloat()
        }
        fontGraphics.scale(fontScaleX.toDouble(), fontScaleY.toDouble())
        fontGraphics.drawString(label, xPos, (y*fontHeight - descent).toFloat())
    }

    protected open fun paintMarkupSelect(g: Graphics2D, p: GoPoint) = g.draw(getMarkupCircle(p))

    protected open fun paintMarkupX(g: Graphics2D, p: GoPoint) = g.draw(getMarkupX(p))

    protected open fun paintMarkupTriangle(g: Graphics2D, p: GoPoint) = g.fill(getMarkupTriangle(p))

    protected open fun paintMarkupCircle(g: Graphics2D, p: GoPoint) = g.fill(getMarkupCircle(p))

    protected open fun paintMarkupSquare(g: Graphics2D, p: GoPoint) = g.fill(getMarkupSquare(p))

    protected open fun paintAllLineMarkups(g: Graphics2D) {
        lineMarkup?.let { set ->
            g.paint = defaultMarkupColor
            var prevThickness = 0f
            for(lm in set) {
                if (!isLineMarkupVisible(lm)) continue
                // TODO line markup color
                prevThickness = setStrokeThickness(g, prevThickness, getLineMarkupThickness(lm))
                paintLineMarkup(g, lm)
            }
        }
    }

    protected open fun paintLineMarkup(g: Graphics2D, lm: LineMarkup) {
        val (x1, y1) = lm.start
        val (x2, y2) = lm.end
        val line = Line2D.Double(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())
        g.draw(line)
        if (lm.isArrow) {
            val dx = x1 - x2
            val dy = y1 - y2
            var x3 = (dx - dy).toDouble()
            var y3 = (dx + dy).toDouble()
            val r = 0.5 / hypot(x3, y3)
            x3 *= r
            y3 *= r
            line.x1 = x2 + x3
            line.y1 = y2 + y3
            g.draw(line)
            line.x1 = x2 + y3
            line.y1 = y2 - x3
            g.draw(line)
        }
    }

}