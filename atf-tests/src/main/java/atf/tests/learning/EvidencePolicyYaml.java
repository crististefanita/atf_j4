package atf.tests.learning;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.nio.file.*;
import java.util.*;

/** Separate strict YAML adapter; no Whist settings enter the J0 configuration vocabulary. */
public final class EvidencePolicyYaml {
    private static final Set<String> KEYS=Set.of("detailLevel","maxDetailedInvocations","maxDetailedComparisons","maxDetailedContextCharacters","passingExamplesPerFamily","optionalContextBudgetCharacters");
    private static final Set<String> PRESENTATION=Set.of("rowsPerGroup","fieldsPerGroup","optionalMaxItems","alwaysIncludeCollections");
    private EvidencePolicyYaml() { }
    public static EvidencePolicy load(Path path)throws Exception {
        var root=new ObjectMapper(new YAMLFactory().enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION)).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(Files.readAllBytes(path));
        if(root==null||!root.isObject())throw new IllegalArgumentException("Evidence policy must be a mapping");
        var keys=new HashSet<String>();root.fieldNames().forEachRemaining(keys::add);if(!keys.containsAll(KEYS)||keys.stream().anyMatch(key->!KEYS.contains(key)&&!PRESENTATION.contains(key)))throw new IllegalArgumentException(
            "Missing or unknown evidence policy settings: "+keys);
        var level=EvidencePolicy.DetailLevel.valueOf(root.path("detailLevel").asText());
        return new EvidencePolicy(level,positive(root,"maxDetailedInvocations"),positive(root,"maxDetailedComparisons"),positive(root,"maxDetailedContextCharacters"),positive(root,"passingExamplesPerFamily"),positive(root,
            "optionalContextBudgetCharacters"),optional(root,"rowsPerGroup",100),optional(root,"fieldsPerGroup",20),optional(root,"optionalMaxItems",32),collections(root));
    }
    private static Set<String> collections(JsonNode root) {
        if (!root.has("alwaysIncludeCollections")) return Set.of("regression", "documentation");
        var node=root.path("alwaysIncludeCollections");if (!node.isArray()) throw new IllegalArgumentException("Full collections must be an explicit list");
        var values=new TreeSet<String>();for (var value:node) if (!value.isTextual() || !values.add(value.asText())) throw new IllegalArgumentException("Invalid or repeated full collection");return values;
    }
    private static int positive(JsonNode root,String key){var value=root.path(key);if(!value.isIntegralNumber()||!value.canConvertToInt()||value.intValue()<=0)throw new IllegalArgumentException(
        "Positive 32-bit integer required: "+key);return value.intValue();}
    private static int optional(JsonNode root,String key,int fallback){return root.has(key)?positive(root,key):fallback;}
    public static EvidencePolicy capture()throws Exception {
        String explicit=System.getProperty("atf.evidence.policy","");if(!explicit.isBlank())return load(Path.of(explicit));
        for(Path base=Path.of("").toAbsolutePath();base!=null;base=base.getParent()){Path file=base.resolve("config/game-evidence.yml");if(Files.isRegularFile(file))return load(file);}
        throw new IllegalArgumentException("Consumer evidence policy YAML is required");
    }
}
