package ro.whistlab.app.score.raster;

import static org.junit.Assert.*;
import java.awt.image.BufferedImage;

/** Pixel oracle independent of the frame coordinates: compares identical rasters with and without decoration. */
public final class HistoryRasterAudit {
    public static void rectanglesOnlyCoverTheirAwardedPointsCells(HistoryRaster.Spec spec,
                                                                 BufferedImage before, BufferedImage after) {
        int rowHeight = spec.fonts().rowHeight(), unit = spec.fonts().unit();
        int frameHeight = spec.fonts().textHeight() + 6 * unit;
        int[][] changed = new int[spec.rows().size()][spec.players().size()];
        assertEquals(before.getWidth(), after.getWidth());assertEquals(before.getHeight(), after.getHeight());
        int widthPixels = after.getWidth(), heightPixels = after.getHeight();
        int[] previous = before.getRGB(0, 0, widthPixels, heightPixels, null, 0, widthPixels);
        int[] current = after.getRGB(0, 0, widthPixels, heightPixels, null, 0, widthPixels);
        for (int index = 0; index < current.length; index++) {
            if (previous[index] == current[index]) continue;
            int x = index % widthPixels, y = index / widthPixels;
            int row = y / rowHeight - 2;
            assertTrue("A frame cannot change the headers", row >= 0 && row < spec.rows().size());
            int seat = -1;
            for (int candidate = 0; candidate < spec.players().size(); candidate++) {
                int left = spec.grid().pointsLeft(candidate);
                if (x >= left && x < left + spec.grid().points()) seat = candidate;
            }
            assertTrue("A frame pixel escaped its Points column at " + x + "," + y, seat >= 0);
            int series = spec.rows().get(row).cells().get(seat).series();
            assertNotEquals("An ordinary or neighbouring cell was decorated", 0, series);
            assertEquals("Penalty red follows the adjustment, not the running score", series < 0 ? 0xff97462e : 0xff29362e,
                    current[index]);
            int localX = x - spec.grid().pointsLeft(seat), localY = y - (row + 2) * rowHeight;
            int top = (rowHeight - frameHeight) / 2;
            boolean perimeter = localX <= unit || localX >= spec.grid().points() - unit - 1
                    || Math.abs(localY - top) <= unit || Math.abs(localY - (top + frameHeight - 1)) <= unit;
            assertTrue("A rectangle must not fill or strike through the score", perimeter);
            changed[row][seat]++;
        }
        for (int row = 0; row < spec.rows().size(); row++) for (int seat = 0; seat < spec.players().size(); seat++) {
            int series = spec.rows().get(row).cells().get(seat).series();
            if (series == 0) { assertEquals(0, changed[row][seat]); continue; }
            assertTrue("Awarded Points rectangle must be visible", changed[row][seat] > 0);
            int left = spec.grid().pointsLeft(seat), top = (row + 2) * rowHeight + (rowHeight - frameHeight) / 2;
            int width = spec.grid().points(), ink = series < 0 ? 0xff97462e : 0xff29362e;
            assertTrue("Top edge missing", edge(after, ink, left, top, width, unit + 1) >= width - 2 * unit);
            assertTrue("Bottom edge missing", edge(after, ink, left, top + frameHeight - unit - 1, width, unit + 1) >= width - 2 * unit);
            assertTrue("Left edge missing", edge(after, ink, left, top, unit + 1, frameHeight) >= frameHeight - 2 * unit);
            assertTrue("Right edge missing", edge(after, ink, left + width - unit - 1, top, unit + 1, frameHeight) >= frameHeight - 2 * unit);
        }
    }

    public static void identicalPixels(String context, BufferedImage before, BufferedImage after) {
        assertEquals(context, before.getWidth(), after.getWidth());
        assertEquals(context, before.getHeight(), after.getHeight());
        int width = before.getWidth(), height = before.getHeight();
        assertArrayEquals(context, before.getRGB(0, 0, width, height, null, 0, width),
                after.getRGB(0, 0, width, height, null, 0, width));
    }

    public static void separatorPixels(HistoryRaster.Spec spec, BufferedImage image, int expected) {
        for (int seat = 0; seat < spec.players().size(); seat++) {
            int left = spec.grid().separatorLeft(seat), count = 0;
            while (left + count < image.getWidth() && image.getRGB(left + count, 1) == HistoryRaster.DIVIDER) count++;
            assertEquals("Raster separator width at seat " + seat, expected, count);
        }
    }

    private static int edge(BufferedImage image, int ink, int left, int top, int width, int height) {
        int count = 0;
        for (int y = top; y < top + height; y++) for (int x = left; x < left + width; x++)
            if (x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight() && image.getRGB(x, y) == ink) count++;
        return count;
    }
    private HistoryRasterAudit() { }
}
