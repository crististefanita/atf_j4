import atf.reporting.audit.AssertionAuditReport;
import atf.reporting.audit.AssertionAuditRun;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.ExtentReportUiEvidence;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.security.SensitiveDataRedactor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Publishes browser observations and comparisons through the repository's reporting JAR. */
class BrowserExtent {
    record Check(JsonNode result, String layer) { }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Evidence directory and new Extent path required");
        Path directory = Path.of(args[0]).toRealPath();
        Path report = Path.of(args[1]).toAbsolutePath().normalize();
        if (!report.getParent().equals(directory) || Files.exists(report)) {
            throw new IllegalArgumentException("Use a new report inside the evidence directory");
        }
        var mapper = new ObjectMapper();
        var checks = new ArrayList<Check>();
        readCases(mapper, directory.resolve("browser-checks.json"), "component", checks);
        readCases(mapper, directory.resolve("browser-ui-checks.json"), "ui", checks);
        if (checks.isEmpty()) throw new IllegalArgumentException("No browser cases");
        Path parity = directory.resolve("whist-parity.json");
        if (Files.isRegularFile(parity)) checks.add(new Check(mapper.readTree(parity.toFile()), "component"));
        Path mobile = directory.resolve("paired-android.json");
        readCases(mapper, mobile, "android-comparison", checks);
        var captures = new ArrayList<ExtentReportUiEvidence.Capture>();
        try (var run = new AssertionAuditRun(new ExtentReportSession(report), new SensitiveDataRedactor())) {
            int number = 0;
            for (var check : checks) {
                var test = check.result();
                String title = test.required("name").asText();
                String key = test.path("id").asText("WEB-01") + "-" + ++number;
                var status = AtfReportStatus.valueOf(test.required("status").asText());
                String suite = switch (check.layer()) {
                    case "ui" -> "Browser UI · Playwright";
                    case "android-comparison" -> "Comparație cu Android";
                    default -> "Componente browser";
                };
                var id = new AssertionAuditReport.Identity(suite, "Card Table", title, key,
                        List.of(check.layer(), "browser"));
                try (var audit = run.open(id)) {
                    audit.phase("Rezultatul verificării", List.of(new AtfAssertion(title,
                            display(test.required("expected")), display(test.required("observed")),
                            status == AtfReportStatus.PASS, status)), status, "");
                    audit.complete(status);
                }
                for (var image : test.path("images")) {
                    Path png = directory.resolve(image.required("file").asText()).toRealPath();
                    if (!png.getParent().equals(directory)) throw new IllegalArgumentException("Image escaped evidence root");
                    captures.add(new ExtentReportUiEvidence.Capture(png, image.required("title").asText(), List.of(title)));
                }
            }
        }
        if (!captures.isEmpty()) ExtentReportUiEvidence.embedInlinePngs(report, captures);
        System.out.println(report);
    }

    private static void readCases(ObjectMapper mapper, Path file, String layer, List<Check> checks) throws Exception {
        if (!Files.isRegularFile(file)) return;
        var cases = mapper.readTree(file.toFile());
        if (!cases.isArray() || cases.isEmpty()) throw new IllegalArgumentException("No cases in " + file.getFileName());
        cases.forEach(result -> checks.add(new Check(result, layer)));
    }

    private static String display(JsonNode value) {
        return value.isTextual() ? value.asText() : value.toPrettyString();
    }
}
