package atf.gradle.audit.input;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

/** Projects values without invoking arbitrary getters; mutable sessions use their explicit public snapshot. */
public final class CapturedValue {
    private CapturedValue() { }
    public static String text(Object value) { return text(value, new java.util.IdentityHashMap<>()); }
    private static String text(Object value, java.util.IdentityHashMap<Object, Boolean> active) {
        if (value == null || value instanceof Number || value instanceof Boolean || value instanceof CharSequence || value instanceof Enum<?>) return String.valueOf(value);
        if (active.put(value, true) != null) return "<cycle: " + value.getClass().getName() + ">";
        try {
            if (value instanceof java.awt.image.BufferedImage image) return "BufferedImage[" + image.getWidth() + "x" + image.getHeight() + "; raster is captured separately when declared]";
            if (value.getClass().isArray()) {
                var elements = new java.util.ArrayList<String>();
                for (int i = 0; i < Array.getLength(value); i++) elements.add(text(Array.get(value, i), active));
                return elements.toString();
            }
            if (value instanceof Map<?, ?> map && value.getClass().getName().startsWith("java.util.")) {
                var fields = new java.util.ArrayList<String>(); map.forEach((key, field) -> fields.add(key + "=" + text(field, active))); return fields.toString();
            }
            if (value instanceof Collection<?> list && value.getClass().getName().startsWith("java.util.")) { var fields = new java.util.ArrayList<String>(); for (var field : list) fields.add(text(field, active)); return fields.toString(); }
            if (value.getClass().isRecord()) {
                var fields = new java.util.ArrayList<String>();
                for (var component : value.getClass().getRecordComponents()) {
                    var field = value.getClass().getDeclaredField(component.getName()); field.setAccessible(true);
                    fields.add(component.getName() + "=" + text(field.get(value), active));
                }
                return value.getClass().getSimpleName() + fields;
            }
            if (value.getClass().getName().equals("ro.whistlab.core.session.GameSession")) return text(value.getClass().getMethod("snapshot").invoke(value), active);
            if (value instanceof Throwable error) return error.getClass().getName() + ": " + error.getMessage();
            if (value instanceof java.nio.file.Path || value instanceof java.time.temporal.TemporalAccessor
                    || value instanceof java.awt.Rectangle || value instanceof java.util.concurrent.atomic.AtomicBoolean
                    || value instanceof java.util.concurrent.atomic.AtomicInteger || value instanceof java.util.concurrent.atomic.AtomicLong
                    || value instanceof Thread || value.getClass().getName().startsWith("com.fasterxml.jackson.databind.node.")
                    || value.getClass().getName().equals("org.json.JSONObject") || value.getClass().getName().equals("org.json.JSONArray")) return String.valueOf(value);
            if (value instanceof java.util.Optional<?> optional) return optional.isPresent() ? "Optional[" + text(optional.get(), active) + "]" : "Optional.empty";
            return "<" + value.getClass().getName() + ": state not projected>";
        } catch (ReflectiveOperationException error) { throw new IllegalStateException("Cannot capture declared fixture " + value.getClass().getName(), error); }
        finally { active.remove(value); }
    }
}
