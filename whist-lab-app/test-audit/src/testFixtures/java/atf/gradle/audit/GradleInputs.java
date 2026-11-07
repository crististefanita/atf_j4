package atf.gradle.audit;

import atf.gradle.audit.input.CapturedValue;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import atf.reporting.model.ReportScreenshot;

/** Captures runtime fixture values before evaluation; repeated identical contexts have one body. */
public final class GradleInputs {
    public record Entry(String name, String value) { }
    private static final ThreadLocal<GradleInputs> CURRENT = new ThreadLocal<>();
    private final LinkedHashSet<Entry> entries = new LinkedHashSet<>();
    private final List<ReportScreenshot> images = new ArrayList<>();
    private final GradleInputs previous;
    private boolean fixture;
    private GradleInputs() { previous = CURRENT.get(); CURRENT.set(this); }
    static GradleInputs open(Object instance) {
        return new GradleInputs();
    }
    void captureFields(Object instance) {
        var fields = new java.util.LinkedHashMap<String, Object>();
        for (var field : instance.getClass().getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
            try { field.setAccessible(true); fields.put(field.getName(), field.get(instance)); }
            catch (ReflectiveOperationException error) { throw new IllegalStateException("Unreadable fixture field", error); }
        }
        if (!fields.isEmpty()) {
            add("Constructed test fixture fields", CapturedValue.text(fields));
            fixture = fields.values().stream().filter(java.util.Objects::nonNull).map(CapturedValue::text)
                    .anyMatch(value -> !value.startsWith("<"));
        }
    }
    static void close(GradleInputs expected) {
        if (CURRENT.get() != expected) throw new IllegalStateException("Fixture scopes must close in order");
        if (expected.previous == null) CURRENT.remove(); else CURRENT.set(expected.previous);
    }
    public static void fixture(String name, Object... namedValues) {
        var input = CURRENT.get(); if (input == null) return;
        if (namedValues.length % 2 != 0) throw new IllegalArgumentException("Fixture context requires name/value pairs");
        var fields = new java.util.LinkedHashMap<String, Object>();
        for (int i = 0; i < namedValues.length; i += 2) fields.put((String) namedValues[i], namedValues[i + 1]);
        input.add(name, CapturedValue.text(fields)); input.fixture = true;
    }
    public static <T> T created(String name, T value) { fixture(name, "value", value); return value; }
    public static void note(String name, String value) { var input = CURRENT.get(); if (input != null) input.add(name, value); }
    public static void image(String name, java.awt.image.BufferedImage image) {
        var input = CURRENT.get(); if (input == null) return;
        try {
            var bytes = new java.io.ByteArrayOutputStream();
            if (!javax.imageio.ImageIO.write(image, "png", bytes)) throw new IllegalStateException("PNG writer absent");
            var screenshot = new ReportScreenshot(name, java.util.Base64.getEncoder().encodeToString(bytes.toByteArray()));
            if (!input.images.contains(screenshot)) input.images.add(screenshot);
        } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
    }
    public void add(String name, String value) { entries.add(new Entry(java.util.Objects.requireNonNull(name), java.util.Objects.requireNonNull(value))); }
    boolean hasFixture() { return fixture; }
    List<Entry> entries() { return List.copyOf(entries); }
    List<ReportScreenshot> images() { return List.copyOf(images); }
}
