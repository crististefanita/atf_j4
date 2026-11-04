package atf.android.evidence.timing;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Properties;

/** Additive schema inside format-1 traces; owner and every declared interval are checked before publication. */
public final class AndroidPhaseTimingCodec {
    private AndroidPhaseTimingCodec() { }

    public static void write(Properties target, String prefix, AndroidPhaseTiming value) {
        header(target, prefix, value.flowId(), value.phaseName(), value.ordinal(), value.partial() ? "PARTIAL" : "COMPLETE");
        target.setProperty(prefix + ".intervalCount", Integer.toString(value.intervals().size()));
        for (int index = 0; index < value.intervals().size(); index++) {
            var interval = value.intervals().get(index);
            String key = prefix + ".interval." + index;
            target.setProperty(key + ".startedNanos", Long.toString(interval.startedNanos()));
            target.setProperty(key + ".endedNanos", Long.toString(interval.endedNanos()));
            target.setProperty(key + ".captureNanos", Long.toString(interval.captureNanos()));
            target.setProperty(key + ".journalNanos", Long.toString(interval.journalNanos()));
        }
    }

    public static void running(Properties target, String prefix, String flowId, String name, int ordinal) {
        header(target, prefix, flowId, name, ordinal, "RUNNING");
    }

    private static void header(Properties target, String prefix, String flowId, String name, int ordinal, String state) {
        target.setProperty(prefix + ".schema", "1");
        target.setProperty(prefix + ".flowId", flowId);
        target.setProperty(prefix + ".phaseName", name);
        target.setProperty(prefix + ".ordinal", Integer.toString(ordinal));
        target.setProperty(prefix + ".state", state);
    }

    public static Optional<AndroidPhaseTiming> read(Properties input, String prefix, String flowId, String name, int ordinal) {
        boolean present = input.stringPropertyNames().stream().anyMatch(key -> key.startsWith(prefix + "."));
        if (!present) return Optional.empty();
        if (!required(input, prefix + ".schema").equals("1")
                || !required(input, prefix + ".flowId").equals(flowId)
                || !required(input, prefix + ".phaseName").equals(name)
                || number(input, prefix + ".ordinal") != ordinal) {
            throw new IllegalArgumentException("Android phase timing owner/schema mismatch: " + prefix);
        }
        String state = required(input, prefix + ".state");
        if (state.equals("RUNNING")) throw new IllegalArgumentException("Android phase timing is unfinished: " + prefix);
        if (!state.equals("COMPLETE") && !state.equals("PARTIAL")) throw new IllegalArgumentException("Unknown Android phase timing state");
        long count = number(input, prefix + ".intervalCount");
        if (count <= 0 || count > input.size()) throw new IllegalArgumentException("Invalid Android interval count");
        var intervals = new ArrayList<AndroidPhaseTiming.Interval>();
        for (int index = 0; index < count; index++) {
            String key = prefix + ".interval." + index;
            intervals.add(new AndroidPhaseTiming.Interval(number(input, key + ".startedNanos"),
                    number(input, key + ".endedNanos"), number(input, key + ".captureNanos"), number(input, key + ".journalNanos")));
        }
        return Optional.of(new AndroidPhaseTiming(flowId, name, ordinal, intervals, state.equals("PARTIAL")));
    }

    private static long number(Properties input, String key) { return Long.parseLong(required(input, key)); }
    private static String required(Properties input, String key) {
        String value = input.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing Android timing field: " + key);
        return value;
    }
}
