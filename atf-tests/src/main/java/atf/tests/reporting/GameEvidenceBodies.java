package atf.tests.reporting;

import atf.reporting.model.ReportJsonBodyFormatter;
import atf.reporting.model.ReportObservation;
import atf.reporting.security.SensitiveDataRedactor;
import atf.core.json.JsonDocuments;
import com.fasterxml.jackson.databind.node.POJONode;
import com.fasterxml.jackson.databind.util.RawValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/** Lossless consumer projection: whole JSON fragments or ordered Unicode-safe TEXT parts. */
public final class GameEvidenceBodies {
    private static final ObjectMapper JSON = new ObjectMapper();
    private GameEvidenceBodies() { }
    public static List<ReportObservation.Body> prepare(ReportObservation.Body body, int limit, SensitiveDataRedactor redactor) {
        if (limit <= 0) throw new IllegalArgumentException("Body budget must be positive");
        String safe = redactor.redactField(body.label(), body.text());
        List<String> parts = new ArrayList<>();
        if (body.format() == ReportObservation.Format.JSON) {
            try {
                JsonDocuments.read(body.text());
                String sanitized = redactor.redactJson(safe);
                String complete = ReportJsonBodyFormatter.format(sanitized);
                if (complete.length() <= limit) parts.add(complete);
                else jsonParts(numericSpellings(sanitized), limit, UnaryOperator.identity(), parts);
            } catch (java.io.IOException failure) {
                throw new IllegalArgumentException("Invalid required JSON evidence: " + body.label(), failure);
            }
        } else {
            int start = 0;
            do {
                int end = (int) Math.min((long) safe.length(), (long) start + limit);
                if (end < safe.length()) {
                    int newline = safe.lastIndexOf('\n', end - 1);
                    if (newline > start) end = newline + 1;
                }
                if (end < safe.length() && end > start && Character.isHighSurrogate(safe.charAt(end - 1))) end--;
                if (end == start && end < safe.length()) throw new IllegalArgumentException("Body budget cannot hold one Unicode scalar");
                parts.add(safe.substring(start, end));
                start = end;
            } while (start < safe.length());
        }
        var result = new ArrayList<ReportObservation.Body>();
        for (int i = 0; i < parts.size(); i++) {
            String label = parts.size() == 1 ? body.label() : body.label() + " · part " + (i + 1) + " of " + parts.size();
            result.add(new ReportObservation.Body(label, parts.get(i), body.format()));
        }
        return List.copyOf(result);
    }
    private static JsonNode numericSpellings(String text) throws java.io.IOException {
        JsonNode tree=JsonDocuments.read(text);
        try(var parser=JSON.getFactory().createParser(text)) {
            for(var token=parser.nextToken();token!=null;token=parser.nextToken())if(token.isNumeric()) {
                var pointer=parser.getParsingContext().pathAsPointer();var raw=new POJONode(new RawValue(parser.getText()));
                if(pointer.matches())tree=raw;
                else {
                    var parent=tree.at(pointer.head());var last=pointer.last();
                    if(parent.isArray())((ArrayNode)parent).set(last.getMatchingIndex(),raw);
                    else ((ObjectNode)parent).set(last.getMatchingProperty(),raw);
                }
            }
        }
        return tree;
    }
    private static String format(JsonNode value) { return ReportJsonBodyFormatter.format(value.toString()); }
    private static void jsonParts(JsonNode value, int limit, UnaryOperator<JsonNode> wrap, List<String> parts) {
        String complete = format(wrap.apply(value));
        if (complete.length() <= limit) { parts.add(complete); return; }
        if (value.isObject() && !value.isEmpty()) {
            value.fields().forEachRemaining(entry -> jsonParts(entry.getValue(), limit,
                    child -> wrap.apply(JSON.createObjectNode().set(entry.getKey(), child)), parts));
            return;
        }
        if (value.isArray() && !value.isEmpty()) {
            ArrayNode current = JSON.createArrayNode();
            for (JsonNode item : value) {
                ArrayNode candidate = current.deepCopy(); candidate.add(item);
                if (format(wrap.apply(candidate)).length() > limit) {
                    if (!current.isEmpty()) parts.add(format(wrap.apply(current)));
                    current = JSON.createArrayNode(); current.add(item);
                    if (format(wrap.apply(current)).length() > limit)
                        throw new IllegalArgumentException("Required JSON array item exceeds body budget; increase bodyMaxCharacters");
                } else current = candidate;
            }
            if (!current.isEmpty()) parts.add(format(wrap.apply(current)));
            return;
        }
        throw new IllegalArgumentException("Required JSON scalar exceeds body budget; increase bodyMaxCharacters");
    }
    /** Merges complete JSON fragments; contradictory scalar values are never silently overwritten. */
    public static JsonNode merge(JsonNode left, JsonNode right) {
        if (left.isObject() && right.isObject()) {
            ObjectNode result = left.deepCopy();
            right.fields().forEachRemaining(entry -> result.set(entry.getKey(), result.has(entry.getKey())
                    ? merge(result.get(entry.getKey()), entry.getValue()) : entry.getValue().deepCopy()));
            return result;
        }
        if (left.isArray() && right.isArray()) { ArrayNode result = left.deepCopy(); result.addAll((ArrayNode) right); return result; }
        if (!left.equals(right)) throw new IllegalArgumentException("Conflicting JSON evidence fragments");
        return left.deepCopy();
    }
}
