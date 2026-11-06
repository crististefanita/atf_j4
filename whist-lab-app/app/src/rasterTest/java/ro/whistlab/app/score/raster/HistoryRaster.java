package ro.whistlab.app.score.raster;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import ro.whistlab.app.score.award.SeriesPointsFrame;

/** Controlled JVM raster of the production projection and geometry, with host fonts rather than Android fonts. */
public final class HistoryRaster {
    public static final int DIVIDER = 0xff71846c;
    public record Cell(String bid, String points, int series, boolean missed) { }
    public record Row(int hand, String cards, boolean current, List<Cell> cells) {
        public Row { cells = List.copyOf(cells); }
    }
    public record Grid(int hand, int bid, int points, int separator, int padding) {
        public int group() { return bid + points; }
        public int width(int seats) { return 2 * padding + hand + seats * (separator + group()); }
        public int pointsLeft(int seat) { return padding + hand + seat * (separator + group()) + separator + bid; }
        public int separatorLeft(int seat) { return padding + hand + seat * (separator + group()); }
    }
    public record Spec(List<String> players, List<Row> rows, Grid grid, Fonts fonts,
                       String cardsHeading, String bidHeading, String pointsHeading) {
        public Spec { players = List.copyOf(players); rows = List.copyOf(rows); }
    }

    public static final class Fonts {
        final Font value, bold, caption;
        final FontMetrics regularMetrics, boldMetrics, captionMetrics;
        public final float density;
        public Fonts(float density, float scale) {
            this.density = density;
            value = new Font(Font.DIALOG, Font.PLAIN, Math.round(12 * density * scale));
            bold = value.deriveFont(Font.BOLD);
            caption = new Font(Font.DIALOG, Font.BOLD, Math.round(11 * density * scale));
            var image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            var graphics = image.createGraphics();
            try {
                regularMetrics = graphics.getFontMetrics(value);
                boldMetrics = graphics.getFontMetrics(bold);
                captionMetrics = graphics.getFontMetrics(caption);
            } finally { graphics.dispose(); }
        }
        public int valueWidth(String text) { return boldMetrics.stringWidth(text); }
        public int captionWidth(String text) { return captionMetrics.stringWidth(text); }
        public int unit() { return Math.max(1, Math.round(density)); }
        public int textHeight() { return regularMetrics.getHeight(); }
        public int rowHeight() { return Math.max(Math.round(40 * density), textHeight() + 2 * Math.round(6 * density)); }
    }

    public static BufferedImage render(Spec spec, boolean decorations) {
        var grid = spec.grid(); var fonts = spec.fonts();
        int rowHeight = fonts.rowHeight(), width = grid.width(spec.players().size());
        var image = new BufferedImage(width, rowHeight * (2 + spec.rows().size()), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            graphics.setColor(new Color(0xfffffef8, true)); graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            fill(graphics, 0xffdfeada, 0, 0, width, rowHeight);
            fill(graphics, 0xffeef3ea, 0, rowHeight, width, rowHeight);
            text(graphics, spec.cardsHeading(), fonts.caption, 0xff29362e, grid.padding(), 0, grid.hand(), rowHeight);
            for (int seat = 0; seat < spec.players().size(); seat++) {
                int start = grid.separatorLeft(seat) + grid.separator();
                text(graphics, spec.players().get(seat), fonts.bold, 0xff29362e, start, 0, grid.group(), rowHeight);
                text(graphics, spec.bidHeading(), fonts.value, 0xff29362e, start, rowHeight, grid.bid(), rowHeight);
                text(graphics, spec.pointsHeading(), fonts.value, 0xff29362e, start + grid.bid(), rowHeight, grid.points(), rowHeight);
            }
            for (int index = 0; index < spec.rows().size(); index++) {
                var row = spec.rows().get(index);int top = (index + 2) * rowHeight;
                fill(graphics, row.current() ? 0xffe5eedc : row.hand() % 2 == 0 ? 0xfff1f5ee : 0xfffffef8,
                        0, top, width, rowHeight);
                text(graphics, row.cards(), fonts.value, 0xff29362e, grid.padding(), top, grid.hand(), rowHeight);
                for (int seat = 0; seat < spec.players().size(); seat++) {
                    var cell = row.cells().get(seat);int pointsLeft = grid.pointsLeft(seat);
                    text(graphics, cell.bid(), fonts.value, cell.missed() ? 0xff97462e : 0xff29362e,
                            pointsLeft - grid.bid(), top, grid.bid(), rowHeight);
                    if (cell.missed()) {
                        graphics.setColor(new Color(0xff97462e, true));graphics.setStroke(new BasicStroke(fonts.unit()));
                        int glyph = fonts.regularMetrics.stringWidth(cell.bid()), left = pointsLeft - grid.bid() + (grid.bid() - glyph) / 2;
                        graphics.drawLine(left, top + rowHeight / 2 + fonts.textHeight() / 3,
                                left + glyph, top + rowHeight / 2 - fonts.textHeight() / 3);
                    }
                    text(graphics, cell.points(), fonts.value, cell.points().startsWith("-") ? 0xff97462e : 0xff29362e,
                            pointsLeft, top, grid.points(), rowHeight);
                    if (decorations && cell.series() != 0) {
                        var frame = SeriesPointsFrame.inside(grid.points(), rowHeight,
                                SeriesPointsFrame.requiredHeight(fonts.textHeight(), fonts.unit()), fonts.unit());
                        if (frame == null) throw new AssertionError("Measured frame cannot fit its points cell");
                        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                        graphics.setColor(new Color(SeriesPointsFrame.color(cell.series()), true));
                        graphics.setStroke(new BasicStroke(fonts.unit()));
                        graphics.draw(new Rectangle2D.Float(pointsLeft + frame.left(), top + frame.top(),
                                frame.right() - frame.left(), frame.bottom() - frame.top()));
                    }
                }
            }
            for (int seat = 0; seat < spec.players().size(); seat++)
                fill(graphics, DIVIDER, grid.separatorLeft(seat), 0, grid.separator(), image.getHeight());
        } finally { graphics.dispose(); }
        return image;
    }

    private static void fill(Graphics2D graphics, int color, int x, int y, int width, int height) {
        graphics.setColor(new Color(color, true));graphics.fillRect(x, y, width, height);
    }
    private static void text(Graphics2D graphics, String value, Font font, int color, int x, int y, int width, int height) {
        graphics.setFont(font);graphics.setColor(new Color(color, true));
        var metrics = graphics.getFontMetrics(font);
        graphics.drawString(value, x + (width - metrics.stringWidth(value)) / 2,
                y + (height - metrics.getHeight()) / 2 + metrics.getAscent());
    }
    private HistoryRaster() { }
}
