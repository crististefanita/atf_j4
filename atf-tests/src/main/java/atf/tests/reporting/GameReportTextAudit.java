package atf.tests.reporting;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** One parsed publication per acceptor; literal TEXT is decoded once, JSON is compared as a complete typed value. */
public final class GameReportTextAudit {
    private static final ObjectMapper JSON = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);
    private static final Pattern PART = Pattern.compile("(.*) · part (\\d+) of (\\d+)");
    private final Set<String> texts = new HashSet<>();
    private final Set<JsonNode> documents = new HashSet<>();
    public GameReportTextAudit(String html) {
        var document = Jsoup.parse(html);
        for (var element : document.select("pre,td,.node,h5,h2,span.se-title")) {
            if (element.tagName().equals("pre") || element.select("table,pre,details").isEmpty()) {
                add(element.wholeText());
                for (var label : element.select("span")) if (label.childrenSize() == 0) add(label.wholeText());
                if (element.hasClass("node")) for (String keyword : List.of("GIVEN ", "WHEN ", "THEN ", "AND "))
                    if (element.text().startsWith(keyword)) add(element.text().substring(keyword.length()));
            }
        }
        if (document.select("pre,td").isEmpty()) add(document.body().wholeText());
        for (var parent : document.select("details,details > .body")) {
            var groups = new LinkedHashMap<String,List<Element>>();
            for (var detail : parent.children()) if (detail.tagName().equals("details")) {
                var match = PART.matcher(detail.select("> summary").text());
                if (match.matches()) groups.computeIfAbsent(match.group(1), key -> new ArrayList<>()).add(detail);
            }
            for (var group : groups.values()) join(group);
        }
    }
    public boolean contains(String value) {
        if (texts.contains(value)) return true;
        JsonNode json = json(value); return json != null && documents.contains(json);
    }
    private void add(String value) { texts.add(value); JsonNode json = json(value); if (json != null) documents.add(json); }
    private void join(List<Element> parts) {
        var text = new StringBuilder(); JsonNode combined = null; boolean allJson = true;
        for (int i = 0; i < parts.size(); i++) {
            var match = PART.matcher(parts.get(i).select("> summary").text());
            if (!match.matches() || Integer.parseInt(match.group(2)) != i + 1 || Integer.parseInt(match.group(3)) != parts.size()) return;
            var detail = parts.get(i);
            var pre = detail.select("pre").stream().filter(node -> node.closest("details") == detail).toList();
            if (pre.size() != 1) return;
            String value = pre.get(0).wholeText(); text.append(value); JsonNode node = json(value);
            if (node == null) allJson = false;
            else if (allJson) {
                try { combined = combined == null ? node : GameEvidenceBodies.merge(combined, node); }
                catch (IllegalArgumentException conflicting) { allJson = false; }
            }
        }
        add(text.toString()); if (allJson && combined != null) documents.add(combined);
    }
    private static JsonNode json(String value) {
        String trimmed = value.strip(); if (!trimmed.startsWith("[") && !trimmed.startsWith("{")) return null;
        try { return JSON.readTree(trimmed); } catch (java.io.IOException invalid) { return null; }
    }
}
