package atf.tests.governance.documentation.release.integrity.html;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.jsoup.Jsoup;
import static atf.tests.governance.documentation.release.integrity.ReleaseFiles.*;

/** Independently recounts canonical HTML invocations after temporary JUnit XML has been reconciled. */
public final class HtmlInvocationIntegrity {
    private HtmlInvocationIntegrity() { }
    public static void verify(Path folder, JsonNode qualification, Set<String> originalIdentities) throws IOException {
        var groups = qualification.path("auditedGroups");
        if (groups.isMissingNode() && qualification.path("schemaVersion").asInt(1) < 2) return;
        require(groups.isObject() && !groups.isEmpty(), "HTML-GROUPS", folder);
        var identities = new HashSet<String>(); originalIdentities.forEach(id -> identities.add(id.replace((char) 0, '#')));
        var reports = new HashSet<String>(); long count = originalIdentities.size();
        var fields = groups.fields();
        while (fields.hasNext()) {
            var entry = fields.next(); String name = entry.getKey(); var group = entry.getValue();
            require(name.matches("[a-z][a-z0-9-]*"), "HTML-GROUP-NAME", name);
            require(!qualification.path("groups").has(name), "DUPLICATE-TEST-GROUP", name);
            require(!Files.exists(folder.resolve("reports/test-results").resolve(name)), "DUPLICATE-AUDITED-JUNIT", name);
            String file = text(group, "report"); require(file.endsWith(".html") && reports.add(file), "HTML-REPORT-OWNER", file);
            long actual = inspect(inside(folder, file), text(group, "protocol"), identities);
            require(actual > 0 && actual == integer(group, "tests") && integer(group, "failures") == 0
                    && integer(group, "errors") == 0 && integer(group, "skipped") == 0, "HTML-TOTALS", name);
            count = Math.addExact(count, actual);
        }
        require(count == integer(qualification, "uniqueJunitInvocations"), "HTML-UNIQUE-TOTAL", folder);
    }
    static long inspect(Path file, String protocol, Set<String> identities) throws IOException {
        require(Set.of("GRADLE_COMPONENT_V1", "JUPITER_COMPONENT_V1").contains(protocol), "HTML-PROTOCOL", protocol);
        var doc = Jsoup.parse(file.toFile(), "UTF-8"); doc.select("template,noscript").remove();
        require(atf.reporting.artifact.resources.ReportArtifactResources.inspect(doc).isAutonomous(), "HTML-AUTONOMY", file);
        String title = protocol.equals("GRADLE_COMPONENT_V1") ? "JUnit invocation receipt" : "Component invocation receipt";
        long count = 0;
        for (var owner : doc.select("#test-view .test-list-wrapper > .test-list-item > .test-item")) {
            var contents = owner.selectFirst(".test-contents"); var accordion = contents == null ? null : contents.selectFirst(".mt-4 > .accordion");
            if (accordion == null) continue;
            for (var scenario : accordion.children()) {
                if (!scenario.hasClass("card")) continue;
                var receipts = receiptNodes(scenario, title); require(receipts.size() == 1, "HTML-RECEIPT-COUNT", file);
                var bodies = receipts.get(0).select("pre"); require(bodies.size() == 1, "HTML-RECEIPT-BODY", file);
                var receipt = new com.fasterxml.jackson.databind.ObjectMapper().readTree(bodies.first().wholeText());
                require(identities.add(text(receipt, "invocationId")), "HTML-DUPLICATE-INVOCATION", file);
                String outcome = text(receipt, protocol.equals("GRADLE_COMPONENT_V1") ? "junitOutcome" : "outcome");
                var rows = scenario.select("table.helm-validation-table > tbody > tr");
                require(outcome.equals("PASS") && !rows.isEmpty() && rows.size() == integer(receipt, "comparisons"), "HTML-COMPARISON-COUNT", file);
                require(rows.stream().allMatch(row -> row.select("td").size() == 4 && row.select("td").last().text().contains("✅")), "HTML-COMPARISON-VERDICT", file);
                if (protocol.equals("GRADLE_COMPONENT_V1")) require(receipt.path("fixtureCaptured").asBoolean(), "HTML-FIXTURE", file);
                count++;
            }
        }
        require(count > 0 && receiptNodes(doc, title).size() == count, "HTML-RECEIPT-OWNERSHIP", file);
        return count;
    }
    private static java.util.List<org.jsoup.nodes.Element> receiptNodes(org.jsoup.nodes.Element parent, String title) {
        return parent.select("details.atf-observation-evidence").stream().filter(node -> node.children().stream()
                .anyMatch(child -> child.tagName().equals("summary") && child.text().equals(title))).toList();
    }
}
