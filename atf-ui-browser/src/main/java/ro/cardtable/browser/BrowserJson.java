package ro.cardtable.browser;

import java.lang.reflect.InvocationTargetException;

/** Encodes only the DTOs owned by the browser contract. */
final class BrowserJson {
    private BrowserJson() { }

    static String encode(Object value) {
        var result = new StringBuilder();
        append(result, value);
        return result.toString();
    }

    private static void append(StringBuilder out, Object value) {
        if (value == null || value instanceof Number || value instanceof Boolean) {
            out.append(value);
        } else if (value instanceof String text) {
            quote(out, text);
        } else if (value instanceof Iterable<?> items) {
            out.append('[');
            boolean first = true;
            for (Object item : items) {
                if (!first) out.append(',');
                first = false;
                append(out, item);
            }
            out.append(']');
        } else if (value instanceof Record record) {
            out.append('{');
            boolean first = true;
            for (var component : record.getClass().getRecordComponents()) {
                if (!first) out.append(',');
                first = false;
                quote(out, component.getName());
                out.append(':');
                try {
                    append(out, component.getAccessor().invoke(record));
                } catch (IllegalAccessException | InvocationTargetException error) {
                    throw new IllegalStateException("Browser DTO could not be encoded", error);
                }
            }
            out.append('}');
        } else {
            throw new IllegalArgumentException("Unsupported browser DTO: " + value.getClass());
        }
    }

    static void quote(StringBuilder out, String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '"' || ch == '\\') out.append('\\').append(ch);
            else if (ch < 32 || Character.isSurrogate(ch)) {
                out.append("\\u");
                for (int shift = 12; shift >= 0; shift -= 4) out.append(Character.forDigit(ch >> shift & 15, 16));
            }
            else out.append(ch);
        }
        out.append('"');
    }
}
