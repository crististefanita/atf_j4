package ro.whistlab.app.score.raster;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import ro.whistlab.app.score.HistoryRasterProjection;
import ro.whistlab.app.score.HistoryTypography;
import ro.whistlab.app.score.layout.HistoryViewport;

/** Controlled host-font composition of the production width/height policies; not an Android screenshot. */
public final class HistoryViewportRaster {
    public record Result(BufferedImage image, HistoryRaster.Spec table, Rectangle dialog, Rectangle viewport,
                         Rectangle legend, int bodyHeight, int closeTop, boolean wholeScroll, int maximumScroll) { }

    public static Result render(HistoryRaster.Spec source, int width, int height, int safeTop, int safeBottom,
                                String title, String legend, String close) {
        return render(source, width, height, safeTop, safeBottom, title, legend, close, false);
    }

    public static Result render(HistoryRaster.Spec source, int width, int height, int safeTop, int safeBottom,
                                String title, String legend, String close, boolean scrollToEnd) {
        float density = source.fonts().density;
        var size = HistoryViewport.dialogSize(width, height - safeTop - safeBottom, density);
        int padding = Math.max(1, Math.round(12 * density));
        int left = (width - size.width()) / 2, top = safeTop + (height - safeTop - safeBottom - size.height()) / 2;
        int contentWidth = size.width() - 2 * padding;
        var table = HistoryRasterProjection.fitWidth(source, contentWidth);
        BufferedImage fullTable = HistoryRaster.render(table, true);
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(0xff657263, true)); graphics.fillRect(0, 0, width, height);
            graphics.setColor(Color.WHITE); graphics.fillRect(left, top, size.width(), size.height());
            Font titleFont = source.fonts().value.deriveFont(source.fonts().value.getSize2D() * 20 / HistoryTypography.VALUE_SP);
            Font legendFont = source.fonts().value.deriveFont(source.fonts().value.getSize2D() * HistoryTypography.LEGEND_SP / HistoryTypography.VALUE_SP);
            var titleMetrics = graphics.getFontMetrics(titleFont);
            var legendMetrics = graphics.getFontMetrics(legendFont);
            List<String> lines = wrap(legend, legendMetrics, contentWidth);
            int titleHeight = titleMetrics.getHeight() + padding;
            int closeHeight = Math.max(Math.round(48 * density), titleMetrics.getHeight() + padding);
            int legendHeight = lines.size() * legendMetrics.getHeight() + padding;
            int headers = 2 * source.fonts().rowHeight();
            int available = size.height() - titleHeight - closeHeight - 2 * padding;
            int preferred = Math.round(380 * density);
            int body = HistoryViewport.bodyHeight(preferred, preferred + headers + legendHeight, available);
            boolean wholeScroll = HistoryViewport.needsWholeScroll(preferred, preferred + headers + legendHeight,
                    available, source.fonts().rowHeight());
            if (wholeScroll) body = fullTable.getHeight() - headers;
            int maximumScroll = wholeScroll ? Math.max(0, headers + body + legendHeight - available) : 0;
            int scroll = scrollToEnd ? maximumScroll : 0;
            var viewport = new Rectangle(left + padding, top + padding + titleHeight, contentWidth,
                    wholeScroll ? available : headers + body);
            var caption = new Rectangle(viewport.x, viewport.y + headers + body - scroll, contentWidth, legendHeight);
            graphics.setFont(titleFont); graphics.setColor(new Color(0xff29362e, true));
            graphics.drawString(title, left + padding, top + padding + titleMetrics.getAscent());
            var clip = graphics.getClip();
            graphics.clip(viewport); graphics.drawImage(fullTable, viewport.x, viewport.y - scroll, null);
            if (!wholeScroll) graphics.setClip(clip);
            graphics.setFont(legendFont); graphics.setColor(new Color(0xff52634f, true));
            for (int index = 0; index < lines.size(); index++)
                graphics.drawString(lines.get(index), caption.x, caption.y + padding / 2 + legendMetrics.getAscent() + index * legendMetrics.getHeight());
            graphics.setClip(clip);
            int closeTop = top + size.height() - padding - closeHeight;
            graphics.setFont(titleFont); graphics.setColor(new Color(0xff386d56, true));
            graphics.drawString(close, left + size.width() - padding - titleMetrics.stringWidth(close),
                    closeTop + (closeHeight - titleMetrics.getHeight()) / 2 + titleMetrics.getAscent());
            return new Result(image, table, new Rectangle(left, top, size.width(), size.height()), viewport, caption, body,
                    closeTop, wholeScroll, maximumScroll);
        } finally { graphics.dispose(); }
    }

    private static List<String> wrap(String value, java.awt.FontMetrics metrics, int width) {
        var lines = new ArrayList<String>();
        String line = "";
        for (String word : value.replace('\n', ' ').split("\\s+")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && metrics.stringWidth(next) > width) { lines.add(line); line = word; }
            else line = next;
        }
        if (!line.isEmpty()) lines.add(line);
        return List.copyOf(lines);
    }
    private HistoryViewportRaster() { }
}
