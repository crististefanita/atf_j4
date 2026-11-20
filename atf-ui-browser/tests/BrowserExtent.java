import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportObservation;
import atf.reporting.model.ReportScreenshot;
import atf.reporting.ui.UiExtentReportSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Publishes recorded comparisons and scenario-owned captures using the pinned Reporting API. */
class BrowserExtent {
    record Check(JsonNode result, String layer, List<ReportScreenshot> captures) { }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Evidence directory and new Extent path required");
        Path directory = Path.of(args[0]).toRealPath();
        Path report = Path.of(args[1]).toAbsolutePath().normalize();
        if (!report.getParent().equals(directory) || Files.exists(report)) {
            throw new IllegalArgumentException("Use a new report inside the evidence directory");
        }
        var mapper = new ObjectMapper();
        var checks = new ArrayList<Check>();
        readCases(mapper, directory, "browser-checks.json", "component", checks);
        readCases(mapper, directory, "browser-ui-checks.json", "ui", checks);
        Path parity = directory.resolve("whist-parity.json");
        if (Files.isRegularFile(parity)) checks.add(check(mapper.readTree(parity.toFile()), "component", directory));
        readCases(mapper, directory, "paired-android.json", "android-comparison", checks);
        if (checks.isEmpty()) throw new IllegalArgumentException("No recorded cases");
        var profile = ReportEvidenceProfile.forScreenshotPolicy(ReportScreenshotPolicy.EACH_CHECKPOINT);
        // Keep standard comparison previews; original bodies must remain complete in their disclosures.
        var settings = new ReportingSettings(ReportingSettings.DEFAULT_ASSERTION_MAX_DISPLAY_CHARACTERS,
                ReportingSettings.DISABLE_BODY_DISPLAY_LIMIT);
        try (var session = new ExtentReportSession(report, settings)) {
            for (var check : checks) {
                var test = check.result();
                String title = test.required("name").asText();
                var status = AtfReportStatus.valueOf(test.required("status").asText());
                var raw = session.startScenario(suite(check.layer()), "Card Table", title,
                        List.of(check.layer(), "recorded-evidence"));
                try (var scenario = UiExtentReportSession.adaptScenario(raw, profile, title);
                     var phase = scenario.startPhase("TEST BODY")) {
                    phase.observation(new ReportObservation("Recorded inputs and identity", List.of(
                            new ReportField("Case ID", test.path("id").asText(title)),
                            new ReportField("Evidence layer", check.layer()),
                            new ReportField("Original outcome", status.name())), List.of(
                            body("Original expected", test.required("expected")),
                            body("Original observed", test.required("observed"))), List.of()));
                    phase.uiValidation("Recorded comparisons", "Captured test result", null,
                            List.of(new AtfAssertion(title, display(test.required("expected")),
                                    display(test.required("observed")), status == AtfReportStatus.PASS, status)), status);
                    for (var capture : check.captures()) {
                        phase.uiValidationAsMedia(capture.title(), "Captured checkpoint", capture.base64Png(),
                                List.of(), List.of(), status);
                    }
                    phase.complete(status);
                    scenario.complete(status);
                }
            }
        }
        System.out.println(report);
    }

    private static void readCases(ObjectMapper mapper, Path directory, String file, String layer,
                                  List<Check> checks) throws Exception {
        Path source = directory.resolve(file);
        if (!Files.isRegularFile(source)) return;
        var cases = mapper.readTree(source.toFile());
        if (!cases.isArray() || cases.isEmpty()) throw new IllegalArgumentException("No cases in " + file);
        for (var result : cases) checks.add(check(result, layer, directory));
    }

    private static Check check(JsonNode test, String defaultLayer, Path directory) throws Exception {
        if (!test.isObject() || test.required("name").asText().isBlank())
            throw new IllegalArgumentException("Recorded case name is required");
        AtfReportStatus.valueOf(test.required("status").asText());
        test.required("expected"); test.required("observed");
        String layer = test.path("layer").asText(defaultLayer);
        suite(layer);
        var captures = new ArrayList<ReportScreenshot>();
        if (test.has("images") && !test.get("images").isArray())
            throw new IllegalArgumentException("Captured images must be an array");
        for (var image : test.path("images")) {
            Path png = directory.resolve(image.required("file").asText()).toRealPath();
            if (!png.getParent().equals(directory)) throw new IllegalArgumentException("Image escaped evidence root");
            String payload = Base64.getEncoder().encodeToString(Files.readAllBytes(png));
            if (!ReportScreenshot.hasPngSignature(payload)) throw new IllegalArgumentException("Invalid PNG: " + png.getFileName());
            captures.add(new ReportScreenshot(image.required("title").asText(), payload));
        }
        return new Check(test, layer, List.copyOf(captures));
    }

    private static String suite(String layer) {
        return switch (layer) {
            case "component" -> "Browser components";
            case "ui" -> "Browser UI";
            case "android-component" -> "Android components";
            case "android-comparison" -> "Android and Web comparison";
            default -> throw new IllegalArgumentException("Unknown evidence layer: " + layer);
        };
    }

    private static String display(JsonNode value) {
        return value.isTextual() ? value.asText() : value.toPrettyString();
    }

    private static ReportObservation.Body body(String label, JsonNode value) {
        return new ReportObservation.Body(label, display(value), value.isTextual()
                ? ReportObservation.Format.TEXT : ReportObservation.Format.JSON);
    }
}
