package atf.gradle.audit.assertions;

import atf.testkit.assertions.AuditedAssertions;
import org.junit.function.ThrowingRunnable;

/** JUnit 4 message ordering; evaluation and comparison capture remain owned by the common Kit. */
public final class GradleAssertions {
    private GradleAssertions() { }
    private static String context(String message) {
        var site = StackWalker.getInstance().walk(stack -> stack.filter(frame -> !frame.getClassName().equals(GradleAssertions.class.getName()))
                .findFirst().map(frame -> frame.getFileName() + ":" + frame.getLineNumber()).orElse("unknown source"));
        return site + (message == null ? "" : " · " + message);
    }
    public static void assertEquals(Object expected, Object actual) { assertEquals(null, expected, actual); }
    public static void assertEquals(String message, Object expected, Object actual) { AuditedAssertions.assertEquals(expected, actual, context(message)); }
    public static void assertEquals(long expected, long actual) { assertEquals(null, expected, actual); }
    public static void assertEquals(String message, long expected, long actual) { AuditedAssertions.assertEquals(expected, actual, context(message)); }
    public static void assertEquals(double expected, double actual, double delta) { assertEquals("Numeric values match within delta " + delta, expected, actual, delta); }
    public static void assertEquals(String message, double expected, double actual, double delta) { AuditedAssertions.assertEquals(expected, actual, delta, context(message)); }
    public static void assertNotEquals(Object expected, Object actual) { assertNotEquals(null, expected, actual); }
    public static void assertNotEquals(long expected, long actual) { assertNotEquals(null, Long.valueOf(expected), Long.valueOf(actual)); }
    public static void assertNotEquals(String message, long expected, long actual) { assertNotEquals(message, Long.valueOf(expected), Long.valueOf(actual)); }
    public static void assertNotEquals(String message, Object expected, Object actual) { AuditedAssertions.assertNotEquals(expected, actual, context(message)); }
    public static void assertNotEquals(double expected, double actual, double delta) {
        boolean equalWithinDelta = Double.compare(expected, actual) == 0 || Math.abs(expected - actual) <= delta;
        AuditedAssertions.assertFalse(equalWithinDelta, context("Expected values to differ beyond delta " + delta + ": " + expected + " vs " + actual));
    }
    public static void assertTrue(boolean actual) { assertTrue(null, actual); }
    public static void assertTrue(String message, boolean actual) { AuditedAssertions.assertTrue(actual, context(message)); }
    public static void assertFalse(boolean actual) { assertFalse(null, actual); }
    public static void assertFalse(String message, boolean actual) { AuditedAssertions.assertFalse(actual, context(message)); }
    public static void assertNull(Object actual) { assertNull(null, actual); }
    public static void assertNull(String message, Object actual) { AuditedAssertions.assertNull(actual, context(message)); }
    public static void assertNotNull(Object actual) { assertNotNull(null, actual); }
    public static void assertNotNull(String message, Object actual) { AuditedAssertions.assertNotNull(actual, context(message)); }
    public static void assertSame(Object expected, Object actual) { assertSame(null, expected, actual); }
    public static void assertSame(String message, Object expected, Object actual) { AuditedAssertions.assertSame(expected, actual, context(message)); }
    public static void assertNotSame(Object expected, Object actual) { AuditedAssertions.assertNotSame(expected, actual); }
    public static void assertNotSame(String message, Object expected, Object actual) {
        atf.gradle.audit.GradleInputs.note("Reference identity invariant", message); AuditedAssertions.assertNotSame(expected, actual);
    }
    public static void assertArrayEquals(int[] expected, int[] actual) { assertArrayEquals(null, expected, actual); }
    public static void assertArrayEquals(String message, int[] expected, int[] actual) { AuditedAssertions.assertEquals(java.util.Arrays.toString(expected), java.util.Arrays.toString(actual), context(message)); }
    public static void assertArrayEquals(byte[] expected, byte[] actual) { AuditedAssertions.assertArrayEquals(expected, actual); }
    public static void assertArrayEquals(boolean[] expected, boolean[] actual) {
        AuditedAssertions.assertEquals(java.util.Arrays.toString(expected), java.util.Arrays.toString(actual), context(null));
    }
    public static void assertArrayEquals(double[] expected, double[] actual, double delta) {
        if (expected == null || actual == null) { assertEquals((Object) expected, actual); return; }
        assertEquals("Array length", expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) assertEquals("Array element " + i, expected[i], actual[i], delta);
    }
    public static void assertArrayEquals(Object[] expected, Object[] actual) { AuditedAssertions.assertArrayEquals(expected, actual); }
    public static <T extends Throwable> T assertThrows(Class<T> type, ThrowingRunnable action) { return assertThrows(null, type, action); }
    public static <T extends Throwable> T assertThrows(String message, Class<T> type, ThrowingRunnable action) { return AuditedAssertions.assertThrows(type, action::run, context(message)); }
    public static void fail(String message) { AuditedAssertions.assertTrue(false, context(message)); }
    public static void fail() { fail("Explicit failure"); }
}
