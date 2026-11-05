package ro.whistlab.core.ai.cost;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;

/** Validates the profile once; consumers cannot reinterpret numeric fields or mutate its identity. */
final class CostProfile {
    record SearchLimits(long bidSamples, long cardSamples, long enumeratedWorlds, long proofNodes) { }

    private final ObjectNode document;
    private final String sha256;
    private final List<Integer> seeds;
    final int warmups, repetitions;
    final long workLimit, cpuLimit, wallLimit;
    final SearchLimits core, independent;

    private CostProfile(byte[] bytes) throws IOException {
        JsonNode parsed = new ObjectMapper().readTree(bytes);
        require(parsed != null && parsed.isObject(), "Profile must be a JSON object");
        document = (ObjectNode) parsed;
        require(exactPositive("schemaVersion") == 1 && exactPositive("players") == 4, "Unsupported cost profile");
        require(document.path("scope").isTextual() && !document.path("scope").asText().isBlank(), "Missing scope");
        require(document.path("handSizes").equals(new ObjectMapper().valueToTree(List.of(2, 3, 4, 5, 6, 7, 8))), "Incomplete hand-size domain");
        var values = document.path("seeds");
        require(values.isArray() && !values.isEmpty(), "Nonempty seed list required");
        var unique = new LinkedHashSet<Integer>();
        for (var value : values) {
            require(value.isIntegralNumber() && value.canConvertToInt() && value.asInt() >= 0, "Seed must be a nonnegative integer");
            require(unique.add(value.asInt()), "Duplicate seed");
        }
        seeds = List.copyOf(unique);
        warmups = exactCount("warmups");
        repetitions = exactCount("repetitions");
        workLimit = exactPositive("maximumWorkUnits");
        cpuLimit = exactPositive("maximumThreadCpuNanos");
        wallLimit = exactPositive("maximumWallNanos");
        core = limits("core", exactPositive("coreEnumeratedWorlds"));
        independent = limits("independent", 0);
        try { sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    static CostProfile read(Path file) throws IOException { return parse(Files.readAllBytes(file)); }
    static CostProfile parse(byte[] bytes) throws IOException { return new CostProfile(bytes); }
    ObjectNode document() { return document.deepCopy(); }
    String sha256() { return sha256; }
    String scope() { return document.path("scope").textValue(); }
    List<Integer> seeds() { return seeds; }
    SearchLimits limits(CostPlayer player) { return player == CostPlayer.INDEPENDENT ? independent : core; }

    private SearchLimits limits(String prefix, long worlds) {
        return new SearchLimits(exactPositive(prefix + "BidSamples"), exactPositive(prefix + "CardSamples"),
                worlds, exactPositive(prefix + "ProofNodes"));
    }

    private int exactCount(String key) {
        long value = exactPositive(key);
        require(value <= Integer.MAX_VALUE, "Counter out of range: " + key);
        return (int) value;
    }

    private long exactPositive(String key) {
        JsonNode value = document.path(key);
        require(value.isIntegralNumber() && value.canConvertToLong() && value.longValue() > 0,
                "Positive 64-bit integer required: " + key);
        return value.longValue();
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new IllegalArgumentException(message);
    }
}
