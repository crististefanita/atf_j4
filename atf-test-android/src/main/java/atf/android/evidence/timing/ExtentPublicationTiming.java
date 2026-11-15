package atf.android.evidence.timing;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Measures publication and its independent HTML audit at the renderer boundary. */
public final class ExtentPublicationTiming implements AutoCloseable {
    private final Path report;
    private final Instant started = Instant.now();
    private final long ticks = System.nanoTime();
    private boolean passed;

    public ExtentPublicationTiming(Path report) {
        this.report = report;
    }

    public void passed() { passed = true; }

    @Override public void close() throws IOException {
        String root = System.getenv("ATF_RELEASE_STAGE_TIMING_ROOT");
        if (root == null || !report.toAbsolutePath().normalize().startsWith(Path.of(root))) return;
        long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - ticks);
        var stage = Map.of("stage", "extent.publish-and-audit", "status", passed ? "PASS" : "FAIL",
                "startedUtc", started.toString(), "endedUtc", Instant.now().toString(),
                "elapsedMs", millis);
        var receipt = Map.of("schemaVersion", 1, "kind", "ATF_RELEASE_TIMING",
                "owner", "extent." + report.getFileName(), "stages", List.of(stage));
        Files.write(report.resolveSibling(report.getFileName() + ".timing.json"),
                new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsBytes(receipt), StandardOpenOption.CREATE_NEW);
    }
}
