package atf.tests.reporting.aggregate;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.jsoup.Jsoup;

/** Reconciles original launcher identities with captured invocation receipts, never with totals alone. */
public final class ComponentReportCoverage {
    public record Invocation(String id, String outcome) { }
    public static List<String> violations(List<Invocation> executed, String html) {
        var problems = new ArrayList<String>();
        var expected = new LinkedHashMap<String, String>();
        for (var test : executed) if (expected.put(test.id(), test.outcome()) != null) problems.add("Duplicate launcher identity: " + test.id());
        if (expected.isEmpty()) problems.add("Empty launcher population");
        var observed = new LinkedHashMap<String, String>();
        for (var details : Jsoup.parse(html).select("details")) {
            if (details.children().stream().noneMatch(n -> n.tagName().equals("summary") && n.text().equals("Component invocation receipt"))) continue;
            try {
                var receipt = new ObjectMapper().readTree(details.select("pre").stream().map(n -> n.wholeText()).collect(java.util.stream.Collectors.joining()));
                if (receipt.path("schemaVersion").asInt() != 1) throw new IllegalArgumentException("Unknown receipt schema");
                String id = receipt.required("invocationId").asText(), outcome = receipt.required("outcome").asText();
                if (id.isBlank()) throw new IllegalArgumentException("Empty invocation identity");
                if (observed.put(id, outcome) != null) problems.add("Duplicate HTML identity: " + id);
                if (outcome.equals("PASS") && receipt.path("comparisons").asInt() == 0) problems.add("No captured comparisons: " + id);
            } catch (Exception invalid) { problems.add("Invalid invocation receipt: " + invalid.getMessage()); }
        }
        expected.forEach((id, outcome) -> {
            if (!observed.containsKey(id)) problems.add("Missing HTML invocation: " + id);
            else if (!outcome.equals(observed.get(id))) problems.add("Outcome differs: " + id + " launcher=" + outcome + " HTML=" + observed.get(id));
        });
        observed.keySet().stream().filter(id -> !expected.containsKey(id)).forEach(id -> problems.add("Unexpected HTML invocation: " + id));
        return List.copyOf(problems);
    }
    private ComponentReportCoverage() { }
}
