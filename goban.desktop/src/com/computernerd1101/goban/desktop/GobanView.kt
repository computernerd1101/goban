package com.computernerd1101.goban.desktop

import com.computernerd1101.goban.*
import com.computernerd1101.goban.desktop.resources.*
import com.computernerd1101.goban.markup.*
import java.awt.*
import java.awt.event.MouseEvent
import java.awt.geom.*
import java.util.*
import javax.swing.JComponent
import kotlin.math.*

open class GobanView
@JvmOverloads constructor(
    goban: AbstractGoban? = null,
    locale: Locale? = Locale.getDefault(Locale.Category.FORMAT)
): JComponent() {

    init {
        foreground = Color.BLACK
        enableEvents(AWTEvent.MOUSE_EVENT_MASK)
    }

    @get:JvmName("gridScale")
    protected var gridScale: Double = 0.0; private set
    private var startX: Int = 0
    private var startY: Int = 0

    // Kotlin does not yet support properties with final getters but open setters,
    // nor vice versa, for that matter. Believe me, I've tried.
    private var _goban: AbstractGoban? = goban
    var goban: AbstractGoban?
        get() = _goban
        @JvmSynthetic
        @JvmName("setGobanKt")
        set(goban) = setGoban(goban)
    open fun setGoban(goban: AbstractGoban?) {
        val old = this._goban
        this._goban = goban
        if (old !== goban) {
            if (old == null || goban == null ||
                    old.width != goban.width ||
                    old.height != goban.height)
                gridScale = 0.0
        }
        revalidate()
        repaint()
    }

    private var _pointMarkup: PointMarkupMap? = null
    var pointMarkup: PointMarkupMap?
        get() = _pointMarkup
        @JvmSynthetic
        @JvmName("setPointMarkupKt")
        set(map) = setPointMarkup(map)
    open fun setPointMarkup(map: PointMarkupMap?) {
        val old = _pointMarkup
        _pointMarkup = map
        if (old.isNullOrEmpty() != map.isNullOrEmpty()) {
            revalidate()
            repaint()
        }
    }

    private var _lineMarkup: LineMarkupSet? = null
    var lineMarkup: LineMarkupSet?
        get() = _lineMarkup
        @JvmSynthetic
        @JvmName("setLineMarkupKt")
        set(set) = setLineMarkup(set)
    open fun setLineMarkup(set: LineMarkupSet?) {
        val old = _lineMarkup
        _lineMarkup = set
        if (old.isNullOrEmpty() != set.isNullOrEmpty()) {
            revalidate()
            repaint()
        }
    }

    private var _formatLocale: Locale? = locale
    var formatLocale: Locale?
        get() = _formatLocale
        @JvmSynthetic
        @JvmName("setFormatLocaleKt")
        set(locale) = setFormatLocale(locale)
    open fun setFormatLocale(locale: Locale?) {
        val old = _formatLocale
        _formatLocale = locale
        if (locale == null) {
            if (old != null) {
                revalidate()
                repaint()
            }
        } else if (locale != old) {
            revalidate()
            repaint()
        }
    }
    open var showCoordinates: Boolean
        get() = formatLocale != null
        set(show) {
            when {
                !show -> formatLocale = null
                formatLocale == null -> formatLocale = Locale.getDefault(Locale.Category.FORMAT)
            }
        }

    protected open fun formatX(x: Int): String {
        val width = goban?.width ?: return ""
        return GoPoint.formatX(x, width, formatLocale ?: Locale.getDefault(Locale.Category.FORMAT))
    }

    protected open fun formatY(y: Int): String {
        val height = goban?.height ?: return ""
        return GoPoint.formatY(y, height, formatLocale ?: Locale.getDefault(Locale.Category.FORMAT))
    }

    private var _gobanBackground: Paint = Color.ORANGE
    var gobanBackground: Paint
        get() = _gobanBackground
        @JvmSynthetic
        @JvmName("setGobanBackgroundKt")
        set(paint) = setGobanBackground(paint)
    open fun setGobanBackground(paint: Paint) {
        val old = _gobanBackground
        _gobanBackground = paint
        if (old != paint) {
            revalidate()
            repaint()
        }
    }

    private var _defaultMarkupColor: Color = Color.RED
    var defaultMarkupColor: Color
        get() = _defaultMarkupColor
        @JvmSynthetic
        @JvmName("setDefaultMarkupColorKt")
        set(color) = setDefaultMarkupColor(color)
    open fun setDefaultMarkupColor(color: Color) {
        val old = _defaultMarkupColor
        _defaultMarkupColor = color
        if (old != color) {
            revalidate()
            repaint()
        }
    }

    private var _edgeThickness: Int = 3
    var edgeThickness: Int
        get() = _edgeThickness
        @JvmSynthetic
        @JvmName("setEdgeThicknessKt")
        set(value) = setEdgeThickness(value)
    open fun setEdgeThickness(value: Int): Unit = setThickness(value) { thickness ->
        val old = _edgeThickness
        _edgeThickness = thickness
        old
    }

    private var _defaultPointMarkupThickness: Int = 2
    var defaultPointMarkupThickness: Int
        get() = _defaultPointMarkupThickness
        @JvmSynthetic
        @JvmName("setDefaultPointMarkupThicknessKt")
        set(value) = setDefaultPointMarkupThickness(value)
    open fun setDefaultPointMarkupThickness(value: Int): Unit = setThickness(value) { thickness ->
        val old = _defaultPointMarkupThickness
        _defaultPointMarkupThickness = thickness
        old
    }

    private var _defaultLineMarkupThickness: Int = 2
    var defaultLineMarkupThickness: Int
        get() = _defaultLineMarkupThickness
        @JvmSynthetic
        @JvmName("setDefaultLineMarkupThicknessKt")
        set(value) = setDefaultLineMarkupThickness(value)
    open fun setDefaultLineMarkupThickness(value: Int): Unit = setThickness(value) { thickness ->
        val old = _defaultLineMarkupThickness
        _defaultLineMarkupThickness = thickness
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
        val width: Int
        val height: Int
        val translate: Int
        if (showCoordinates) {
            width = gobanWidth + 3
            height = gobanHeight + 3
            translate = 2
        } else {
            width = gobanWidth + 1
            height = gobanHeight + 1
            translate = 1
        }
        var paintWidth = this.width
        var paintHeight = this.height
        var paintX = 0
        var paintY = 0
        val scale: Double
        // if (width/height > paintWidth/paintHeight) {...}
        val cmp = (width * paintHeight.toLong()).compareTo(height * paintWidth.toLong())
        if (cmp > 0) {
            scale = paintWidth.toDouble() / width
            val oldHeight = paintHeight
            paintHeight = (height * paintWidth.toLong() / width).toInt()
            paintY = oldHeight - paintHeight shr 1
        } else {
            scale = paintHeight.toDouble() / height
            if (cmp != 0) {
                val oldWidth = paintWidth
                paintWidth = (width * paintHeight.toLong() / height).toInt()
                paintX = oldWidth - paintWidth shr 1
            }
        }
        val gridStart = ((translate - 0.5) * scale).toInt()
        startX = paintX + gridStart
        startY = paintY + gridStart
        gridScale = scale
        // Draw background
        g2d.paint = gobanBackground
        g2d.fillRect(paintX, paintY, paintWidth, paintHeight)
        g2d.translate(paintX, paintY)
        g2d.scale(scale, scale)
        g2d.translate(translate, translate)
        paintGoban(g2d)
    }

    protected open fun paintGoban(g: Graphics2D) {
        val goban = goban ?: return
        val width = goban.width
        val height = goban.height
        g.paint = foreground
        if (showCoordinates) {
            var label: String
            for (x in 0 until width) {
                label = formatX(x)
                if (label.length > 1 && !(label[0].isWhitespace() && label[label.lastIndex].isWhitespace()))
                    label = " $label "
                paintLabel(g, x, -1, label)
                paintLabel(g, x, height, label)
            }
            for (y in 0 until height) {
                label = formatY(y)
                paintLabel(g, -1, y, label)
                paintLabel(g, width, y, label)
            }
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
            label.isNotEmpty() -> paintLabel(g, p, label)
            m == PointMarkup.SELECT -> paintMarkupSelect(g, p)
            m == PointMarkup.X -> paintMarkupX(g, p)
            m == PointMarkup.TRIANGLE -> paintMarkupTriangle(g, p)
            m == PointMarkup.CIRCLE -> paintMarkupCircle(g, p)
            m == PointMarkup.SQUARE -> paintMarkupSquare(g, p)
        }
    }

    protected open fun paintLabel(g: Graphics2D, p: GoPoint, label: String) {
        paintLabel(g, p.x, p.y, label)
    }

    protected open fun paintLabel(g: Graphics2D, x: Int, y: Int, label: String) {
        if (label.isBlank()) return
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