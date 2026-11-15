package atf.tests.jvm;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public final class PackageCohesionRules {
    private PackageCohesionRules() { }

    public static List<String> check(Map<String, List<String>> inventory, int limit, JsonNode reviewed) {
        if (limit != 8) throw new IllegalArgumentException("GOV-SOURCE-CONFIG: cohesion limit must be eight");
        if (!reviewed.isObject()) throw new IllegalArgumentException("GOV-SOURCE-CONFIG: reviewedPackages must be an object");
        var result = new ArrayList<String>();
        inventory.forEach((key, files) -> {
            if (files.size() <= limit) return;
            JsonNode exception = reviewed.path(key);
            JsonNode members = exception.path("files");
            var declared = new HashSet<String>();
            for (JsonNode member : members) if (member.isTextual()) declared.add(member.textValue());
            if (!members.isArray() || members.size() != declared.size() || files.size() != declared.size()
                    || !declared.containsAll(files) || !explanation(exception, "reason") || !explanation(exception, "action"))
                result.add("GOV-COHESION: review " + key + " " + files);
        });
        reviewed.properties().stream().map(Map.Entry::getKey).sorted().forEach(key -> {
            if (inventory.getOrDefault(key, List.of()).size() <= limit) result.add("GOV-COHESION-STALE: " + key);
        });
        return List.copyOf(result);
    }

    static boolean explanation(JsonNode entry, String field) {
        JsonNode value = entry.path(field);
        return value.isTextual() && !value.textValue().isBlank();
    }
}
