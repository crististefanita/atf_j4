package atf.gradle.audit.verification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import java.util.List;
import java.util.Map;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/** Publishes a passing Gradle selection after Extent and its original JUnit inventory agree. */
final class GradleTaskCheckpoint {
    private static final ObjectMapper JSON = new ObjectMapper();

    private GradleTaskCheckpoint() { }

    static void publish(Path plan, String task, Path report, Map<String, String> outcomes) throws Exception {
        var selection = JSON.readTree(plan.toFile()).required("tasks").required(task);
        String group = selection.required("group").asText();
        String name = selection.required("selection").asText();
        if (!group.matches("[a-z0-9][a-z0-9-]{0,79}") || name.isBlank()
                || !(task.equals(name) || task.endsWith(":" + name)))
            throw new IllegalStateException("Invalid Gradle checkpoint selection: " + task);

        Path root = plan.toAbsolutePath().normalize().getParent();
        Path destination = root.resolve(group);
        if (!report.toAbsolutePath().normalize().equals(destination.resolve("component-report.html")))
            throw new IllegalStateException("Gradle checkpoint report is outside its selection: " + task);
        if (outcomes.isEmpty() || outcomes.values().stream().anyMatch(outcome -> !"PASS".equals(outcome)))
            return;

        Path journal = root.resolve("component-checkpoint.json");
        var checkpoint = (ObjectNode) JSON.readTree(journal.toFile());
        String provenance = sha256(root.resolve("source-provenance.json"));
        if (!provenance.equals(checkpoint.required("provenanceReceiptSha256").asText()))
            throw new IllegalStateException("Gradle checkpoint provenance changed");
        var rows = checkpoint.withArray("results");
        for (var row : rows) {
            if (group.equals(row.required("module").asText()))
                throw new IllegalStateException("Gradle checkpoint already contains " + group);
        }

        var totals = Map.of("PASS", outcomes.size(), "FAIL", 0, "ERROR", 0, "SKIP", 0);
        var execution = JSON.createObjectNode().put("schemaVersion", 1).put("kind", "live-source-components")
                .put("apkQualification", false).put("selection", name).put("tests", outcomes.size())
                .putPOJO("outcomes", totals).putPOJO("coverageErrors", List.of()).putPOJO("containerFailures", List.of())
                .put("provenanceSha256", provenance).put("reportSha256", sha256(report));
        var row = JSON.createObjectNode().put("module", group).put("suite", name).put("exitCode", 0);
        row.set("receipt", execution);
        rows.add(row);

        atomicJson(destination.resolve("execution.json"), execution);
        atomicJson(journal, checkpoint);
        System.out.println("Checkpoint saved for " + task);
    }

    private static String sha256(Path file) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        try (var stream = new java.security.DigestInputStream(Files.newInputStream(file), digest)) {
            stream.transferTo(java.io.OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void atomicJson(Path path, ObjectNode value) throws Exception {
        Path pending = path.resolveSibling(path.getFileName() + ".writing");
        JSON.writeValue(pending.toFile(), value);
        Files.move(pending, path, ATOMIC_MOVE, REPLACE_EXISTING);
    }
}
