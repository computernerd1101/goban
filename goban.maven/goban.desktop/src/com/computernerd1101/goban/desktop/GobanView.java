package com.computernerd1101.goban.desktop;

import com.computernerd1101.goban.*;
import com.computernerd1101.goban.markup.*;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.util.Locale;

@SuppressWarnings("unused")
public class GobanView extends JComponent {

    private double gridScale;
    protected final double getGridScale() { return gridScale; }
    private int startX, startY;

    public GobanView() {
        this((AbstractGoban)null);
    }

    public GobanView(@Nullable AbstractGoban goban) {
        this(goban, Locale.getDefault(Locale.Category.FORMAT));
    }

    public GobanView(@Nullable Locale locale) {
        this(null, locale);
    }

    public GobanView(@Nullable AbstractGoban goban, @Nullable Locale locale) {
        this.goban = goban;
        this.formatLocale = locale;
        setForeground(Color.BLACK);
        enableEvents(AWTEvent.MOUSE_EVENT_MASK);
    }

    private AbstractGoban goban;
    public final @Nullable AbstractGoban getGoban() { return goban; }
    public void setGoban(@Nullable AbstractGoban goban) {
        AbstractGoban old = this.goban;
        this.goban = goban;
        if (old != goban) {
            if (old == null || goban == null || old.width != goban.width || old.height != goban.height)
                gridScale = 0.0;
            revalidate();
            repaint();
        }
    }

    private Locale formatLocale;
    public final @Nullable Locale getFormatLocale() { return formatLocale; }
    public void setFormatLocale(@Nullable Locale locale) {
        Locale old = formatLocale;
        formatLocale = locale;
        if (!Intrinsics.areEqual(locale, old)) {
            revalidate();
            repaint();
        }
    }

    public boolean getShowCoordinates() { return formatLocale != null; }
    public void setShowCoordinates(boolean show) {
        if (!show)
            setFormatLocale(null);
        else if (formatLocale != null)
            setFormatLocale(Locale.getDefault(Locale.Category.FORMAT));
    }

    protected @NotNull String formatX(int x) {
        AbstractGoban goban = this.goban;
        if (goban == null) return "";
        Locale locale = formatLocale;
        if (locale == null) locale = Locale.getDefault(Locale.Category.FORMAT);
        return GoPoint.formatX(x, goban.width, locale);
    }
    protected @NotNull String formatY(int y) {
        AbstractGoban goban = this.goban;
        if (goban == null) return "";
        Locale locale = formatLocale;
        if (locale == null) locale = Locale.getDefault(Locale.Category.FORMAT);
        return GoPoint.formatY(y, goban.height, locale);
    }

    private Paint gobanBackground = Color.ORANGE;
    public final @NotNull Paint getGobanBackground() { return gobanBackground; }
    public void setGobanBackground(@NotNull Paint paint) {
        Paint old = gobanBackground;
        gobanBackground = paint;
        if (!old.equals(paint)) {
            revalidate();
            repaint();
        }
    }

    private Color defaultMarkupColor = Color.RED;
    public final @NotNull Color getDefaultMarkupColor() { return defaultMarkupColor; }
    public void setDefaultMarkupColor(@NotNull Color color) {
        Color old = defaultMarkupColor;
        defaultMarkupColor = color;
        if (!old.equals(color)) {
            revalidate();
            repaint();
        }
    }

    private int edgeThickness = 3;
    public final int getEdgeThickness() { return edgeThickness; }
    public void setEdgeThickness(int value) {
        value = filterThickness(value);
        int old = edgeThickness;
        edgeThickness = value;
        if (old != value) {
            revalidate();
            repaint();
        }
    }

    private PointMarkupMap pointMarkup;
    public final @Nullable PointMarkupMap getPointMarkup() { return pointMarkup; }
    public void setPointMarkup(@Nullable PointMarkupMap map) {
        PointMarkupMap old = pointMarkup;
        pointMarkup = map;
        if (old != pointMarkup) {
            revalidate();
            repaint();
        }
    }

    private LineMarkupSet lineMarkup;
    public final @Nullable LineMarkupSet getLineMarkup() { return lineMarkup; }
    public void setLineMarkup(@Nullable LineMarkupSet set) {
        LineMarkupSet old = lineMarkup;
        lineMarkup = set;
        if (old != set) {
            revalidate();
            repaint();
        }
    }

    private int defaultPointMarkupThickness = 2;
    public final @Range(from=1, to=Integer.MAX_VALUE) int getDefaultPointMarkupThickness() {
        return defaultPointMarkupThickness;
    }
    public void setDefaultPointMarkupThickness(int value) {
        value = filterThickness(value);
        int old = defaultPointMarkupThickness;
        defaultPointMarkupThickness = value;
        if (old != value) {
            revalidate();
            repaint();
        }
    }

    private int defaultLineMarkupThickness = 2;
    public final @Range(from=1, to=Integer.MAX_VALUE) int getDefaultLineMarkupThickness() {
        return defaultLineMarkupThickness;
    }
    public void setDefaultLineMarkupThickness(int value) {
        value = filterThickness(value);
        int old = defaultLineMarkupThickness;
        defaultLineMarkupThickness = value;
        if (old != value) {
            revalidate();
            repaint();
        }
    }

    private static int filterThickness(int value) {
        if (value == Integer.MIN_VALUE) return Integer.MAX_VALUE;
        if (value < 0) return -value;
        if (value == 0) return 1;
        return value;
    }

    public static final @NotNull BasicStroke thinStroke = new BasicStroke(0f);

    public static @NotNull Color applyAlpha(@NotNull Color color, float alpha) {
        if (alpha >= 1f) return color;
        int baseAlpha = (int)(color.getAlpha() * Math.max(alpha, 0f));
        int argb = (baseAlpha << 24) | (color.getRGB() & 0xFFFFFF);
        return new Color(argb, true);
    }

    @Contract("null -> null")
    public final @Nullable GoPoint toGoPoint(@Nullable MouseEvent event) {
        return event == null ? null : toGoPoint(event.getX(), event.getY());
    }

    public final @Nullable GoPoint toGoPoint(int x, int y) {
        AbstractGoban goban = this.goban;
        double scale = gridScale;
        if (goban != null && scale != 0.0) {
            int left = startX;
            int top = startY;
            if (x >= left && y >= top) {
                int gx = (int)((x - left) / scale);
                int gy = (int)((y -  top) / scale);
                if (gx < goban.width && gy < goban.height)
                    return GoPoint.pointAt(gx, gy);
            }
        }
        return null;
    }

    public final @Nullable GoColor getStoneAt(@NotNull GoPoint p) {
        AbstractGoban goban = this.goban;
        return goban == null || p.x >= goban.width || p.y >= goban.height ? null : goban.get(p);
    }

    public @Nullable GoColor getStoneColorAt(@NotNull GoPoint p) { return getStoneAt(p); }

    public @Nullable PointMarkup getPointMarkupAt(@NotNull GoPoint p) {
        AbstractGoban goban = this.goban;
        if (goban == null || p.x >= goban.width || p.y >= goban.height) return null;
        PointMarkupMap map = pointMarkup;
        return map == null ? null : map.get(p);
    }

    public boolean isVisible(@NotNull GoPoint p) {
        return true;
    }

    public float getAlphaAt(@NotNull GoPoint p) {
        return 1f;
    }

    public float getStoneAlphaAt(@NotNull GoPoint p) {
        return 1f;
    }

    public float getMarkupAlphaAt(@NotNull GoPoint p) {
        return 1f;
    }

    public @NotNull Color getMarkupColorAt(@NotNull GoPoint p) {
        return defaultMarkupColor;
    }

    public boolean isLineMarkupVisible(@NotNull LineMarkup lm) {
        return true;
    }

    public final int getPointMarkupThickness(@NotNull GoPoint p) {
        return filterThickness(computePointMarkupThickness(p));
    }

    protected int computePointMarkupThickness(@NotNull GoPoint p) {
        return defaultPointMarkupThickness;
    }

    public final int getLineMarkupThickness(@NotNull LineMarkup lm) {
        return filterThickness(computeLineMarkupThickness(lm));
    }

    protected int computeLineMarkupThickness(@NotNull LineMarkup lm) {
        return defaultLineMarkupThickness;
    }

    public final boolean isStarPoint(@NotNull GoPoint p) {
        AbstractGoban goban = this.goban;
        if (goban == null) return false;
        int width = goban.width;
        int height = goban.height;
        int x1 = getStarPoint(width);
        if (x1 <= 0) return false;
        int x2 = (width & 1) == 0 ? -1 : (width >> 1);
        int x3 = width - 1 - x1;
        int y1 = getStarPoint(height);
        if (y1 <= 0) return false;
        int y2 = (height & 1) == 0 ? -1 : (height >> 1);
        int y3 = height - 1 - y1;
        int x = p.x, y = p.y;
        return (x == x1 || x == x2 || x == x3) && (y == y1 || y == y2 || y == y3);
    }

    public int getStarPoint(int size) {
        if (size < 5) return 0;
        if (size < 7) return 1;
        if (size < 12) return 2;
        return 3;
    }

    @NotNull public Shape getMarkupX(@NotNull GoPoint p) {
        int x = p.x, y = p.y;
        Path2D.Double path = new Path2D.Double();
        path.moveTo(x - 0.25, y - 0.25);
        path.lineTo(x + 0.25, y + 0.25);
        path.moveTo(x - 0.25, y + 0.25);
        path.lineTo(x + 0.25, y - 0.25);
        return path;
    }

    @NotNull public Shape getMarkupTriangle(@NotNull GoPoint p) {
        /*
         *                (not to scale)
         *                     /|\
         *                    / | \
         *                   /  |  \
         *                  /   |   \
         *                 /    |    \ sqrt(3)/8
         *                /     |     \
         *               /      |      \
         *              /       | 1/4   \
         *   sqrt(3)/4 /        |        \ __
         *            / ..      |      .. \
         *           /    ..    |    ..    \
         *          /       ..  |  .. 1/8   \
         *         /          __|__          \ sqrt(3)/8
         *        /         _/  .  \_         \
         *       /        _/    . 60 \_        \
         *      /       _/      .      \_ 1/4   \
         *     /      _/        .        \_      \
         *    /     _/          . 1/8      \_     \
         *   /    _/            .            \_    \
         *  /  __/              . 90        30 \__  \
         * /_________________________________________\
         *         sqrt(3)/8    |    sqrt(3)/8
         */
        final double halfWidth = 0.21650635094610965; // sqrt(3)/8
        int x = p.x, y = p.y;
        Path2D.Double path = new Path2D.Double();
        path.moveTo(x, y - 0.25);
        path.lineTo(x + halfWidth, y + 0.125);
        path.lineTo(x - halfWidth, y + 0.125);
        path.lineTo(x, y - 0.25);
        return path;
    }

    @NotNull Shape getMarkupCircle(@NotNull GoPoint p) {
        int x = p.x, y = p.y;
        return new Ellipse2D.Double(x - 0.25, y - 0.25, 0.5, 0.5);
    }

    @NotNull Shape getMarkupSquare(@NotNull GoPoint p) {
        int x = p.x, y = p.y;
        return new Rectangle2D.Double(x - 0.25, y - 0.25, 0.5, 0.5);
    }


    @Override
    protected void paintComponent(@NotNull Graphics g) {
        super.paintComponent(g);
        AbstractGoban goban = this.goban;
        if (goban == null) return;
        g = g.create();
        Intrinsics.checkNotNullExpressionValue(g, "g.create()");
        Graphics2D g2d = (Graphics2D)g;
        int gobanWidth = goban.width;
        int gobanHeight = goban.height;
        int width, height, translate;
        if (getShowCoordinates()) {
            width = gobanWidth + 3;
            height = gobanHeight + 3;
            translate = 2;
        } else {
            width = gobanWidth + 1;
            height = gobanHeight + 1;
            translate = 1;
        }
        int paintWidth = getWidth();
        int paintHeight = getHeight();
        int paintX = 0;
        int paintY = 0;
        double scale;
        // if (width/height > paintWidth/paintHeight) {...}
        int cmp = Long.compare(width*(long)paintHeight, height*(long)paintWidth);
        if (cmp > 0) {
            scale = (double)paintWidth / width;
            int oldHeight = paintHeight;
            paintHeight = (int)(height * (long)paintWidth / width);
            paintY = (oldHeight - paintHeight) >> 1;
        } else {
            scale = (double)paintHeight / height;
            if (cmp != 0) {
                int oldWidth = paintWidth;
                paintWidth = (int)(width * (long)paintHeight / height);
                paintX = (oldWidth - paintWidth) >> 1;
            }
        }
        int gridStart = (int)((translate - 0.5) * scale);
        startX = paintX + gridStart;
        startY = paintY + gridStart;
        gridScale = scale;
        // Draw background
        g2d.setPaint(gobanBackground);
        g2d.fillRect(paintX, paintY, paintWidth, paintHeight);
        g2d.translate(paintX, paintY);
        g2d.scale(scale, scale);
        g2d.translate(translate, translate);
        paintGoban(g2d);
    }

    protected void paintGoban(@NotNull Graphics2D g) {
        AbstractGoban goban = this.goban;
        if (goban == null) return;
        int width = goban.width, height = goban.height;
        g.setPaint(getForeground());
        if (getShowCoordinates()) {
            String label;
            for(int x = 0; x < width; x++) {
                label = formatX(x);
                if (label.length() > 1 &&
                        !(Character.isWhitespace(label.charAt(0)) &&
                                Character.isWhitespace(label.charAt(label.length() - 1))))
                    label = " " + label + " ";
                paintLabel(g, x, -1, label);
                paintLabel(g, x, height, label);
            }
            for(int y = 0; y < height; y++) {
                label = formatY(y);
                paintLabel(g, -1, y, label);
                paintLabel(g, width, y, label);
            }
        }
        paintAllGoPoints(g);
        paintAllLineMarkups(g);
        paintAllPointMarkups(g);
    }

    protected void paintAllGoPoints(@NotNull Graphics2D g) {
        AbstractGoban goban = this.goban;
        if (goban == null) return;
        int width = goban.width;
        int height = goban.height;
        for(int y = 0; y < height; y++) {
            for(int x = 0; x < width; x++) {
                GoPoint p = GoPoint.pointAt(x, y);
                paintGoPoint(g, p);
            }
        }
    }

    protected void paintGoPoint(@NotNull Graphics2D g, @NotNull GoPoint p) {
        if (!isVisible(p)) return;
        AbstractGoban goban = this.goban;
        if (goban == null) return;
        int width = goban.width, height = goban.height;
        int x = p.x, y = p.y;
        if (x >= width || y >= height) return;
        g.setStroke(thinStroke);
        GoColor stone = getStoneColorAt(p);
        float stoneAlpha = 1f;
        Ellipse2D.Double circle = null;
        final double starRadius = 0.125;
        final double starDiameter = 0.25;
        drawGrid: {
            float alpha = getAlphaAt(p);
            if (!(alpha > 0f && alpha < 1f)) alpha = 1f;
            if (stone != null) {
                stoneAlpha = getStoneAlphaAt(p);
                if (!(stoneAlpha > 0f && stoneAlpha < 1f)) {
                    stoneAlpha = alpha;
                    break drawGrid;
                }
                stoneAlpha *= alpha;
            }
            g.setPaint(applyAlpha(getForeground(), alpha));
            double scale = gridScale;
            double thick = Math.abs((double)edgeThickness);
            double thin = 1.0 / scale;
            boolean isThick = thick > 1.0;
            thick = isThick ? Math.min(thick / scale, 0.25) : thin;
            Rectangle2D.Double rect;
            Line2D.Double line = null;
            if (width == 1) {
                circle = new Ellipse2D.Double(-starRadius, y - starRadius, starDiameter, starDiameter);
                g.fill(circle);
                line = new Line2D.Double();
                line.x1 = 0.0;
                line.x2 = 0.0;
                if (y != 0) {
                    line.y1 = y - 0.5;
                    line.y2 = y - starRadius;
                    g.draw(line);
                }
                if (y != height - 1) {
                    line.y1 = y + starRadius;
                    line.y2 = y + 0.5;
                    g.draw(line);
                }
            } else if (height == 1) {
                circle = new Ellipse2D.Double(x - starRadius, -starRadius, starDiameter, starDiameter);
                g.fill(circle);
                line = new Line2D.Double();
                line.y1 = 0.0;
                line.y2 = 0.0;
                if (x != 0) {
                    line.x1 = x - 0.5;
                    line.x2 = x - starRadius;
                    g.draw(line);
                }
                if (x != width - 1) {
                    line.x1 = x + starRadius;
                    line.x2 = x + 0.5;
                    g.draw(line);
                }
            } else if (x == 0 || x == width - 1) {
                if (isThick) {
                    rect = new Rectangle2D.Double(
                            x - 0.5*thick, y == 0 ? (-0.5)*thick : y - 0.5,
                            thick, y == 0 || y == height - 1 ? 0.5 + 0.5*thick : 1.0);
                    g.fill(rect);
                    if (y == 0 || y == height - 1) {
                        rect.x = x == 0 ? 0.5*thick : x - 0.5;
                        rect.y = y - 0.5*thick;
                        rect.width = 0.5 - 0.5*thick;
                        rect.height = thick;
                        g.fill(rect);
                    }
                } else {
                    line = new Line2D.Double(x, y == 0 ? 0.0 : y - 0.5,
                            x, y == height - 1 ? y : y + 0.5);
                    g.draw(line);
                }
                if (!(isThick && (y == 0 || y == height - 1))) {
                    if (line == null) line = new Line2D.Double();
                    if (x == 0) {
                        line.x1 = 0.5*thick;
                        line.x2 = 0.5;
                    } else {
                        line.x1 = x - 0.5;
                        line.x2 = x - 0.5*thick;
                    }
                    line.y1 = y;
                    line.y2 = y;
                    g.draw(line);
                }
            } else if (y == 0 || y == height - 1) {
                line = new Line2D.Double();
                if (isThick) {
                    rect = new Rectangle2D.Double(x - 0.5, y - 0.5*thick, 1.0, thick);
                    g.fill(rect);
                } else {
                    line.x1 = x - 0.5;
                    line.x2 = x + 0.5;
                    line.y1 = y;
                    line.y2 = y;
                    g.draw(line);
                }
                if (y == 0) {
                    line.y1 = 0.5*thick;
                    line.y2 = 0.5;
                } else {
                    line.y1 = y - 0.5;
                    line.y2 = y - 0.5*thick;
                }
                line.x1 = x;
                line.x2 = x;
                g.draw(line);
            } else {
                line = new Line2D.Double();
                line.x1 = x - 0.5;
                line.y1 = y;
                line.y2 = y;
                double ySkip;
                if (isStarPoint(p)) {
                    circle = new Ellipse2D.Double(x - starRadius, y - starRadius,
                            starDiameter, starDiameter);
                    g.fill(circle);
                    line.x2 = x - starRadius;
                    g.draw(line);
                    line.x1 = x + starRadius;
                    ySkip = starRadius;
                } else ySkip = 0.5*thin;
                line.x2 = x + 0.5;
                g.draw(line);
                line.x1 = x;
                line.x2 = x;
                line.y1 = y - 0.5;
                line.y2 = y - ySkip;
                g.draw(line);
                line.y1 = y + ySkip;
                line.y2 = y + 0.5;
                g.draw(line);
            }
        }
        if (stone != null) {
            Color stoneFill, stoneEdge;
            if (stoneAlpha == 1f) {
                stoneFill = Color.BLACK;
                stoneEdge = Color.WHITE;
            } else {
                int alphaMask = (int)(stoneAlpha * 255f) << 24;
                stoneFill = new Color(alphaMask, true);
                stoneEdge = new Color(alphaMask | 0xFFFFFF, true);
            }
            if (stone == GoColor.WHITE) {
                Color tmp = stoneFill;
                stoneFill = stoneEdge;
                stoneEdge = tmp;
            }
            if (circle == null) circle = new Ellipse2D.Double();
            circle.x = x - 0.5;
            circle.y = y - 0.5;
            circle.width = 1.0;
            circle.height = 1.0;
            g.setPaint(stoneFill);
            g.fill(circle);
            g.setPaint(stoneEdge);
            g.draw(circle);
        }
    }

    private float setStrokeThickness(Graphics2D g, float prevThickness, int newThickness) {
        float thickness = newThickness == 1 ? 1f : (float)(newThickness / gridScale);
        if (thickness > 0.125f) {
            g.setStroke(thinStroke);
            return 0f;
        }
        if (thickness != prevThickness) {
            g.setStroke(new BasicStroke(thickness));
            return thickness;
        }
        return prevThickness;
    }

    protected void paintAllPointMarkups(@NotNull Graphics2D g) {
        AbstractGoban goban = this.goban;
        if (goban == null || pointMarkup == null) return;
        int width = goban.width;
        int height = goban.height;
        float prevThickness = 0f;
        for(int y = 0; y < height; y++) {
            for(int x = 0; x < width; x++) {
                GoPoint p = GoPoint.pointAt(x, y);
                PointMarkup m = getPointMarkupAt(p);
                if (m == null) continue;
                Color color = getMarkupColorAt(p);
                g.setPaint(applyAlpha(color, getMarkupAlphaAt(p)));
                prevThickness = setStrokeThickness(g, prevThickness, getPointMarkupThickness(p));
                paintPointMarkup(g, p, m);
            }
        }
    }

    protected void paintPointMarkup(@NotNull Graphics2D g, @NotNull GoPoint p, @NotNull PointMarkup m) {
        String label = m.label();
        if (!label.isEmpty()) paintLabel(g, p, label);
        else if (m == PointMarkup.SELECT) paintMarkupSelect(g, p);
        else if (m == PointMarkup.X) paintMarkupX(g, p);
        else if (m == PointMarkup.TRIANGLE) paintMarkupTriangle(g, p);
        else if (m == PointMarkup.CIRCLE) paintMarkupCircle(g, p);
        else if (m == PointMarkup.SQUARE) paintMarkupSquare(g, p);
    }

    protected void paintLabel(@NotNull Graphics2D g, @NotNull GoPoint p, @NotNull String label) {
        paintLabel(g, p.x, p.y, label);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    protected void paintLabel(@NotNull Graphics2D g, int x, int y, @NotNull String label) {
        if (StringsKt.isBlank(label)) return;
        FontMetrics metrics = g.getFontMetrics();
        Graphics2D fontGraphics = (Graphics2D)g.create();
        fontGraphics.translate(-0.5, 0.5);
        int fontHeight = metrics.getHeight();
        int fontWidth = metrics.stringWidth(label);
        int descent = metrics.getDescent();
        float xPos;
        float fontScaleX;
        float fontScaleY = 1f / fontHeight;
        if (fontHeight > fontWidth) {
            fontScaleX = fontScaleY;
            xPos = x*fontHeight + (fontHeight - fontWidth)*0.5f;
        } else {
            fontScaleX = 1f / fontWidth;
            xPos = x * fontWidth;
        }
        fontGraphics.scale(fontScaleX, fontScaleY);
        fontGraphics.drawString(label, xPos, y*fontHeight - descent);
    }

    protected void paintMarkupSelect(@NotNull Graphics2D g, @NotNull GoPoint p) {
        g.draw(getMarkupCircle(p));
    }

    protected void paintMarkupX(@NotNull Graphics2D g, @NotNull GoPoint p) {
        g.draw(getMarkupX(p));
    }

    protected void paintMarkupTriangle(@NotNull Graphics2D g, @NotNull GoPoint p) {
        g.fill(getMarkupTriangle(p));
    }

    protected void paintMarkupCircle(@NotNull Graphics2D g, @NotNull GoPoint p) {
        g.fill(getMarkupCircle(p));
    }

    protected void paintMarkupSquare(@NotNull Graphics2D g, @NotNull GoPoint p) {
        g.fill(getMarkupSquare(p));
    }

    protected void paintAllLineMarkups(@NotNull Graphics2D g) {
        LineMarkupSet set = lineMarkup;
        if (set != null) {
            g.setPaint(defaultMarkupColor);
            float prevThickness = 0f;
            for(LineMarkup lm: set) {
                if (!isLineMarkupVisible(lm)) continue;
                // TODO line markup color
                prevThickness = setStrokeThickness(g, prevThickness, getLineMarkupThickness(lm));
                paintLineMarkup(g, lm);
            }
        }
    }

    protected void paintLineMarkup(@NotNull Graphics2D g, @NotNull LineMarkup lm) {
        GoPoint start = lm.start, end = lm.end;
        int x1 = start.x, y1 = start.y;
        int x2 = end.x, y2 = end.y;
        Line2D.Double line = new Line2D.Double(x1, y1, x2, y2);
        g.draw(line);
        if (lm.isArrow) {
            int dx = x1 - x2;
            int dy = y1 - y2;
            double x3 = dx - dy;
            double y3 = dx + dy;
            double r = 0.5 / Math.hypot(x3, y3);
            x3 *= r;
            y3 *= r;
            line.x1 = x2 + x3;
            line.y1 = y2 + y3;
            g.draw(line);
            line.x1 = x2 + y3;
            line.y1 = y2 - x3;
            g.draw(line);
        }
    }

}
