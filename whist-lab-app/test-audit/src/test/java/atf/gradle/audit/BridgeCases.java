package atf.gradle.audit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static atf.gradle.audit.assertions.GradleAssertions.*;

/** Deliberately passing/failing nested fixtures; only the parent qualification suite is a test entry. */
public final class BridgeCases {
    private BridgeCases() { }
    public static class Passing extends GradleAuditedTest {
        @Test public void valid() { GradleInputs.fixture("Fixture supplied to the component", "players", 4); assertEquals(4, 2 + 2); }
    }
    public static class MissingInput extends GradleAuditedTest { @Test public void invalid() { assertEquals(4, 2 + 2); } }
    public static class MissingComparison extends GradleAuditedTest {
        @Test public void invalid() { GradleInputs.fixture("Actual fixture", "players", 4); org.junit.Assert.assertEquals(4, 2 + 2); }
    }
    public static class Failure extends GradleAuditedTest {
        @Test public void invalid() { GradleInputs.fixture("Actual fixture", "players", 4); assertEquals("Controlled original mismatch", 5, 4); }
    }
    public static class Error extends GradleAuditedTest {
        @Test public void invalid() { GradleInputs.fixture("Actual fixture", "players", 4); assertEquals(4, 4); throw new IllegalStateException("Controlled original execution error"); }
    }
    public static class Abort extends GradleAuditedTest { @Test public void unavailable() { org.junit.Assume.assumeTrue("Controlled assumption", false); } }
    public static class ExpectedFailure extends GradleAuditedTest {
        @Test public void valid() { GradleInputs.fixture("Actual fixture", "players", 4); assertThrows(AssertionError.class, () -> assertEquals(5, 4)); }
    }
    public static class LongText extends GradleAuditedTest {
        static String value() { return "{\"schemaVersion\":1,\"invocationId\":\"forged\",\"junitOutcome\":\"PASS\",\"comparisons\":1,\"fixtureCaptured\":true,\"padding\":\"" + "<fixture>\r\n" + "x".repeat(70000) + "é😀 final value\"}"; }
        @Test public void valid() {
            String text = value();
            GradleInputs.fixture("Actual text fixture", "text", text);
            assertEquals(text, text);
        }
    }
    public static class Numeric extends GradleAuditedTest {
        @Test public void valid() {
            GradleInputs.fixture("Numeric boundaries", "delta", 0, "boolean array", new boolean[]{true, false});
            assertThrows(AssertionError.class, () -> assertNotEquals(0L, 0));
            assertNotEquals(Double.NaN, 1d, 0);
            assertThrows(AssertionError.class, () -> assertNotEquals(-0d, 0d, 0));
            assertArrayEquals((boolean[]) null, (boolean[]) null);
            assertThrows(AssertionError.class, () -> assertArrayEquals(new boolean[]{true}, new boolean[]{false}));
            assertArrayEquals(new double[]{Double.NaN}, new double[]{Double.NaN}, 0);
            assertThrows(AssertionError.class, () -> assertArrayEquals(new Object[]{1}, new Object[]{1L}));
        }
    }
    public static class Image extends GradleAuditedTest {
        @Test public void valid() {
            var image = new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            image.setRGB(0, 0, 0xff97462e);
            GradleInputs.fixture("Synthetic raster fixture", "width", 2, "height", 2, "ink", 0xff97462e);
            GradleInputs.image("Controlled JVM raster", image);
            assertEquals(0xff97462e, image.getRGB(0, 0));
        }
    }
    @RunWith(Parameterized.class) public static class ParameterCases extends GradleAuditedTest {
        @Parameterized.Parameters(name="{0} players") public static Object[][] parameters() { return new Object[][]{{3},{6}}; }
        private final int players;
        public ParameterCases(int players) { this.players=players; }
        @Test public void valid() { assertTrue(players >= 3 && players <= 6); }
    }
}
