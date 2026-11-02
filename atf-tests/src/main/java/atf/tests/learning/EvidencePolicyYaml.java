package atf.tests.learning;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.nio.file.*;
import java.util.*;

/** Separate strict YAML adapter; no Whist settings enter the J0 configuration vocabulary. */
public final class EvidencePolicyYaml {
    private static final Set<String> KEYS=Set.of("detailLevel","maxDetailedInvocations","maxDetailedComparisons","maxDetailedContextCharacters","passingExamplesPerFamily","optionalContextBudgetCharacters");
    private EvidencePolicyYaml() { }
    public static EvidencePolicy load(Path path)throws Exception {
        var root=new ObjectMapper(new YAMLFactory()).readTree(Files.readAllBytes(path));
        if(root==null||!root.isObject())throw new IllegalArgumentException("Evidence policy must be a mapping");
        var keys=new HashSet<String>();root.fieldNames().forEachRemaining(keys::add);if(!keys.equals(KEYS))throw new IllegalArgumentException("Missing or unknown evidence policy settings: "+keys);
        var level=EvidencePolicy.DetailLevel.valueOf(root.path("detailLevel").asText());
        return new EvidencePolicy(level,positive(root,"maxDetailedInvocations"),positive(root,"maxDetailedComparisons"),positive(root,"maxDetailedContextCharacters"),positive(root,"passingExamplesPerFamily"),positive(root,"optionalContextBudgetCharacters"));
    }
    private static int positive(JsonNode root,String key){var value=root.path(key);if(!value.isIntegralNumber()||!value.canConvertToInt()||value.intValue()<=0)throw new IllegalArgumentException("Positive 32-bit integer required: "+key);return value.intValue();}
    public static EvidencePolicy capture()throws Exception {
        String explicit=System.getProperty("atf.evidence.policy","");if(!explicit.isBlank())return load(Path.of(explicit));
        for(Path base=Path.of("").toAbsolutePath();base!=null;base=base.getParent()){Path file=base.resolve("config/game-evidence.yml");if(Files.isRegularFile(file))return load(file);}
        throw new IllegalArgumentException("Consumer evidence policy YAML is required");
    }
}
