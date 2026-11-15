package atf.tests.jvm;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** The same package, import and dependency rules serve the CLI and the governance suite. */
public final class JvmArchitecture {
    private JvmArchitecture() { }

    public static Report inspect(Path project) throws IOException {
        Path root = project.toRealPath();
        JsonNode policy = JvmSourceInventory.readPolicy(root);
        for (String array : List.of("mavenModules", "additionalSourceRoots")) {
            if (!policy.path(array).isArray()) throw invalid(array + " must be an array");
        }
        JsonNode limit = policy.path("packageLimit");
        if (!limit.isIntegralNumber() || !limit.canConvertToInt() || limit.intValue() != 8)
            throw invalid("packageLimit must be eight");
        var inventory = JvmSourceInventory.scan(root, policy);
        if (inventory.files().isEmpty()) throw invalid("the governed source population is empty");
        return inspect(root, inventory, policy);
    }

    private static Report inspect(Path root, JvmSourceInventory.Inventory inventory, JsonNode policy) throws IOException {
        JsonNode boundaries = object(policy, "productionForbiddenNamespaces");
        JsonNode adapters = object(policy, "productionBoundaryAdapters");
        JsonNode reviews = object(policy, "reviewedPackages");
        var observedAdapters = new HashSet<String>();
        var packages = new TreeMap<String, List<String>>();
        var diagnostics = new ArrayList<String>();
        var moduleBoundaries = new TreeMap<String, List<String>>();
        for (var sourceSet : inventory.sourceSets()) {
            if (sourceSet.set().equals("main") && !moduleBoundaries.containsKey(sourceSet.module()))
                moduleBoundaries.put(sourceSet.module(), namespaces(boundaries.path(sourceSet.module()), sourceSet.module()));
        }
        for (var source : inventory.files()) {
            String path = JvmSourceInventory.relative(root, source.file());
            List<String> forbidden = source.test() ? List.of() : moduleBoundaries.get(source.module());
            var observation = SourceQualityRules.inspect(source, Files.readString(source.file()), forbidden);
            if (!observation.packageName().isEmpty()) {
                packages.computeIfAbsent(source.scope() + ":" + observation.packageName(), ignored -> new ArrayList<>())
                        .add(source.file().getFileName().toString());
            }
            observation.findings().forEach(finding -> diagnostics.add(path + ": " + finding));
            Set<String> allowed = Set.of();
            if (!source.test() && adapters.has(path)) {
                observedAdapters.add(path);
                allowed = readAdapter(adapters.get(path), path, observation.forbiddenReferences(), diagnostics);
            }
            for (String namespace : observation.forbiddenReferences()) {
                if (!allowed.contains(namespace)) diagnostics.add(path + ": GOV-BOUNDARY: forbidden " + namespace);
            }
        }
        adapters.properties().stream().map(Map.Entry::getKey).filter(path -> !observedAdapters.contains(path))
                .forEach(path -> diagnostics.add(path + ": GOV-BOUNDARY-STALE: missing production source"));
        packages.values().forEach(files -> files.sort(String::compareTo));
        diagnostics.addAll(PackageCohesionRules.check(packages, policy.get("packageLimit").intValue(), reviews));
        return new Report(inventory.modules(), inventory.sourceSets(), inventory.files().size(), packages.size(),
                new Exceptions(reviews, adapters), diagnostics.stream().sorted().toList());
    }

    private static Set<String> readAdapter(JsonNode adapter, String file, List<String> references, List<String> diagnostics) {
        try {
            var allowed = namespaces(adapter.path("allows"), file);
            if (allowed.isEmpty() || !PackageCohesionRules.explanation(adapter, "reason")
                    || !references.containsAll(allowed)) {
                diagnostics.add(file + ": GOV-BOUNDARY-ADAPTER: expected a reason and only observed forbidden namespaces");
                return Set.of();
            }
            return Set.copyOf(allowed);
        } catch (IOException invalid) {
            diagnostics.add(file + ": GOV-BOUNDARY-ADAPTER: " + invalid.getMessage());
            return Set.of();
        }
    }

    private static JsonNode object(JsonNode policy, String key) throws IOException {
        JsonNode value = policy.path(key);
        if (!value.isObject()) throw invalid(key + " must be an object");
        return value;
    }

    private static List<String> namespaces(JsonNode value, String owner) throws IOException {
        if (!value.isArray()) throw invalid(owner + " requires an explicit namespace list");
        var result = new LinkedHashSet<String>();
        for (JsonNode entry : value) {
            if (!entry.isTextual() || !entry.textValue().matches("[\\p{javaJavaIdentifierStart}][\\p{javaJavaIdentifierPart}]*"
                    + "(\\.[\\p{javaJavaIdentifierStart}][\\p{javaJavaIdentifierPart}]*)*") || !result.add(entry.textValue()))
                throw invalid(owner + " has an invalid or duplicate namespace " + entry);
        }
        return List.copyOf(result);
    }

    private static IOException invalid(String finding) {
        return new IOException("GOV-SOURCE-CONFIG: " + finding);
    }

    public record Report(List<String> modules, List<JvmSourceInventory.SourceSet> sourceSets, int files, int packages,
                         Exceptions exceptions, List<String> diagnostics) { }
    public record Exceptions(JsonNode reviewedPackages, JsonNode productionBoundaryAdapters) { }
}
