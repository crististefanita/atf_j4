package atf.tests.execution.components;

import atf.android.evidence.component.AndroidComponentReports;
import atf.tests.reporting.aggregate.ComponentReportCoverage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/** Executes one declared primary component gate and rejects an incomplete HTML instead of converting old XML. */
public final class ComponentSuiteExecution {
    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("suite, new external output, provenance JSON, source root required");
        Path root = Path.of(args[3]).toRealPath(), output = Path.of(args[1]).toAbsolutePath().normalize();
        if (output.startsWith(root)) throw new IllegalArgumentException("Execution output must be external");
        if (Files.exists(output)) throw new IllegalArgumentException("Execution output must be new");
        var mapper = new ObjectMapper();
        var catalog = mapper.readTree(Files.readString(root.resolve("config/atf-test-suites.json")));
        boolean allowed = false;
        for (var entry : catalog.required("suites")) if (entry.path("suite").asText().equals(args[0])
                && entry.path("role").asText().equals("primary") && entry.path("layer").asText().equals("component")) allowed = true;
        if (!allowed) throw new IllegalArgumentException("Only a declared primary component gate can run here: " + args[0]);
        Files.createDirectories(output);
        byte[] provenanceBytes = Files.readAllBytes(Path.of(args[2]));
        var provenance = mapper.readTree(provenanceBytes);
        if (!provenance.path("kind").asText().equals("live-source-components") || provenance.path("apkQualification").asBoolean(true))
            throw new IllegalArgumentException("Explicit live-source provenance without APK qualification required");
        System.setProperty("atf.governance.root", root.toString());
        System.setProperty(AndroidComponentReports.DIRECTORY, output.toString());
        System.setProperty(AndroidComponentReports.PROVENANCE, provenance.toString());
        System.setProperty(AndroidComponentReports.INPUT_PART_CHARACTERS,
                System.getProperty(AndroidComponentReports.INPUT_PART_CHARACTERS, "16384"));
        var listener = new ComponentExecutionListener();
        var request = LauncherDiscoveryRequestBuilder.request().selectors(selectClass(args[0]))
                .filters(EngineFilter.includeEngines("junit-platform-suite"))
                .configurationParameter("atf.component.aggregate", "true")
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "true")
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "false").build();
        try (var session = LauncherFactory.openSession()) { session.getLauncher().execute(request, listener); }
        List<Path> reports;
        try (var entries = Files.list(output)) { reports = entries.filter(p -> p.toString().endsWith(".html")).toList(); }
        var errors = new ArrayList<String>();
        if (reports.size() != 1) errors.add("Expected exactly one report, found " + reports.size());
        Path canonical = output.resolve("component-report.html");
        if (reports.size() == 1) {
            Files.move(reports.getFirst(), canonical);
            errors.addAll(ComponentReportCoverage.violations(listener.invocations(), Files.readString(canonical)));
        }
        var result = new LinkedHashMap<String, Object>();
        result.put("schemaVersion", 1);result.put("kind", "live-source-components");result.put("apkQualification", false);
        result.put("suite", args[0]);result.put("tests", listener.invocations().size());
        result.put("outcomes", listener.counts());result.put("coverageErrors", errors);
        result.put("containerFailures", listener.containerFailures());
        result.put("junitIdentitySha256", digest(mapper.writeValueAsBytes(listener.invocations())));
        result.put("provenanceSha256", provenanceFingerprint(provenanceBytes));
        if (Files.exists(canonical)) result.put("reportSha256", digest(Files.readAllBytes(canonical)));
        mapper.writerWithDefaultPrettyPrinter().writeValue(output.resolve("execution.json").toFile(), result);
        System.out.println(mapper.writeValueAsString(result));
        if (!errors.isEmpty() || !listener.successful()) throw new IllegalStateException("Component execution or exact HTML coverage failed; inspect " + output);
    }
    private static String digest(byte[] bytes) throws Exception {
        return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    static String provenanceFingerprint(byte[] provenanceBytes) throws Exception { return digest(provenanceBytes); }
    private ComponentSuiteExecution() { }
}
