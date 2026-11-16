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
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Evidence directory and new Extent path required");
        Path directory = Path.of(args[0]).toRealPath();
        Path report = Path.of(args[1]).toAbsolutePath().normalize();
        if (!report.getParent().equals(directory) || Files.exists(report)) {
            throw new IllegalArgumentException("Use a new report inside the evidence directory");
        }
        var mapper = new ObjectMapper();
        var cases = mapper.readTree(directory.resolve("browser-checks.json").toFile());
        if (!cases.isArray() || cases.isEmpty()) throw new IllegalArgumentException("No browser cases");
        var checks = new ArrayList<JsonNode>();
        cases.forEach(checks::add);
        checks.add(mapper.readTree(directory.resolve("whist-parity.json").toFile()));
        var captures = new ArrayList<ExtentReportUiEvidence.Capture>();
        try (var run = new AssertionAuditRun(new ExtentReportSession(report), new SensitiveDataRedactor())) {
            int number = 0;
            for (var test : checks) {
                String title = test.required("name").asText();
                String key = "WEB-01-" + ++number;
                var status = AtfReportStatus.valueOf(test.required("status").asText());
                var id = new AssertionAuditReport.Identity("Browser components", "Card Table", title, key,
                        List.of("component", "browser"));
                try (var audit = run.open(id)) {
                    audit.phase("Rezultatul componentei", List.of(new AtfAssertion(title,
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

    private static String display(JsonNode value) {
        return value.isTextual() ? value.asText() : value.toPrettyString();
    }
}
