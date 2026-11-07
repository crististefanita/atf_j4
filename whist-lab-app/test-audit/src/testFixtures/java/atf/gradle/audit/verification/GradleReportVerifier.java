package atf.gradle.audit.verification;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jsoup.Jsoup;

/** Recounts original JUnit and the actual HTML independently; omission and duplicate receipts are fatal. */
public final class GradleReportVerifier {
    private GradleReportVerifier() { }
    public static void main(String[] args) throws Exception { verify(Path.of(args[0]), Path.of(args[1])); }
    public static void verify(Path report, Path junit) throws Exception {
        var expected = new LinkedHashMap<String, String>();
        var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, ""); factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        try (var paths = Files.list(junit)) {
            for (var file : paths.filter(p -> p.getFileName().toString().matches("TEST-.*\\.xml")).toList()) {
                var cases = factory.newDocumentBuilder().parse(file.toFile()).getElementsByTagName("testcase");
                for (int i = 0; i < cases.getLength(); i++) {
                    var item = (org.w3c.dom.Element) cases.item(i);
                    String outcome = item.getElementsByTagName("failure").getLength() > 0 ? "FAIL" : item.getElementsByTagName("error").getLength() > 0 ? "ERROR"
                            : item.getElementsByTagName("skipped").getLength() > 0 ? "SKIP" : "PASS";
                    if (outcome.equals("FAIL")) {
                        var original = (org.w3c.dom.Element) item.getElementsByTagName("failure").item(0);
                        String type = original.getAttribute("type");
                        if (!type.isBlank() && !AssertionError.class.isAssignableFrom(Class.forName(type, false, GradleReportVerifier.class.getClassLoader()))) outcome = "ERROR";
                    }
                    String id = item.getAttribute("classname") + "#" + item.getAttribute("name");
                    if (expected.put(id, outcome) != null) throw new IllegalStateException("Duplicate original JUnit invocation: " + id);
                }
            }
        }
        if (expected.isEmpty()) throw new IllegalStateException("Empty JUnit selection");
        var actual = receipts(report);
        if (!expected.equals(actual)) throw new IllegalStateException("JUnit/HTML inventory differs. JUnit=" + expected + "; HTML=" + actual);
        System.out.println("PASS: " + actual.size() + " exact JUnit/HTML identities and outcomes; one canonical HTML");
    }
    public static Map<String, String> receipts(Path report) throws Exception {
        var doc = Jsoup.parse(report.toFile(), "UTF-8"); var result = new LinkedHashMap<String, String>();
        doc.select("template,noscript").remove();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var intent = new java.util.ArrayList<atf.reporting.artifact.ReportArtifactProfile.Scenario>();
        int observed = 0;
        for (var owner : doc.select("#test-view .test-list-wrapper > .test-list-item > .test-item")) {
            var contents = owner.selectFirst(".test-contents");
            var accordion = contents == null ? null : contents.selectFirst(".mt-4 > .accordion");
            if (accordion == null) continue;
            for (var scenario : accordion.children()) {
                if (!scenario.hasClass("card")) continue;
                var payloads = receiptNodes(scenario);
                if (payloads.size() != 1) throw new IllegalStateException("A rendered scenario must have exactly one owned receipt");
                observed++;
                var payload = payloads.get(0).select("pre");
                if (payload.size() != 1) throw new IllegalStateException("Malformed invocation receipt body");
                var receipt = mapper.readTree(payload.first().wholeText()); String id = receipt.required("invocationId").asText();
                CapturedPngVerifier.verify(doc, scenario, receipt.required("images"));
                var declarations = scenario.select("details.atf-observation-evidence").stream().filter(e -> e.children().stream().anyMatch(child ->
                        child.tagName().equals("summary") && child.text().equals(atf.gradle.audit.GradleIntent.TITLE))).toList();
                if (declarations.size() != 1) throw new IllegalStateException("Captured publication intent is missing or duplicated");
                String serialized = declarations.get(0).select("pre").stream().map(org.jsoup.nodes.Element::wholeText).collect(java.util.stream.Collectors.joining());
                intent.add(atf.gradle.audit.GradleIntent.withMetadata(atf.gradle.audit.GradleIntent.read(mapper.readTree(serialized)), serialized));
                String title = scenario.selectFirst(".card-header > .card-title > a.node > span").text();
                if (!id.substring(id.indexOf('#') + 1).replaceAll("\\s+", " ").equals(title)) throw new IllegalStateException("Receipt belongs to a different rendered scenario: " + id);
                var rows = scenario.select("table.helm-validation-table > tbody > tr");
                if (rows.size() != receipt.required("comparisons").asInt()) throw new IllegalStateException("Rendered comparison inventory differs: " + id);
                if (receipt.required("junitOutcome").asText().equals("PASS") && rows.stream().anyMatch(row -> !row.select("td").last().text().contains("✅")))
                    throw new IllegalStateException("Passing receipt hides a failing comparison: " + id);
            if (result.put(id, receipt.required("junitOutcome").asText()) != null) throw new IllegalStateException("Duplicate HTML receipt: " + id);
            if (receipt.required("junitOutcome").asText().equals("PASS") && (!receipt.required("fixtureCaptured").asBoolean() || receipt.required("comparisons").asInt() < 1))
                throw new IllegalStateException("Passing invocation without captured fixture/comparisons: " + id);
            }
        }
        if (receiptNodes(doc).size() != observed) throw new IllegalStateException("Orphan or duplicated invocation receipt outside rendered owners");
        var profile = new atf.reporting.artifact.ReportArtifactProfile(atf.reporting.artifact.ReportArtifactProfile.Renderer.EXTENT, intent);
        var inspection = atf.reporting.artifact.ReportArtifactInspector.inspect(report, profile);
        if (!inspection.violations().isEmpty()) throw new IllegalStateException("Captured publication intent differs: " + inspection.violations());
        return result;
    }
    private static java.util.List<org.jsoup.nodes.Element> receiptNodes(org.jsoup.nodes.Element root) {
        return root.select("details.atf-observation-evidence").stream().filter(e -> e.children().stream()
                .anyMatch(child -> child.tagName().equals("summary") && child.text().equals("JUnit invocation receipt"))).toList();
    }
}
