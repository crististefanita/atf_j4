package atf.android.evidence.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Contiguous display parts preserve complete sanitized text within a configured UTF-16 body budget. */
public final class EvidenceTextParts {
    public static List<String> split(String content, int maximum) {
        Objects.requireNonNull(content, "content");
        if (maximum < 0) throw new IllegalArgumentException("Body budget must be zero or positive");
        if (maximum == 0 || content.length() <= maximum) return List.of(content);
        var parts = new ArrayList<String>();
        for (int start = 0; start < content.length();) {
            int end = start + Math.min(maximum, content.length() - start);
            if (end < content.length() && Character.isHighSurrogate(content.charAt(end - 1))
                    && Character.isLowSurrogate(content.charAt(end))) end--;
            if (end == start) throw new IllegalArgumentException("Body budget cannot contain a complete Unicode character");
            parts.add(content.substring(start, end));start = end;
        }
        return List.copyOf(parts);
    }
    private EvidenceTextParts() { }
}
