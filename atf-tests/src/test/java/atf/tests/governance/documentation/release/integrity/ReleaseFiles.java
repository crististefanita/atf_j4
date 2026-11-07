package atf.tests.governance.documentation.release.integrity;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

/** Strict, read-only primitives shared by release checks; paths never escape the declared owner. */
public final class ReleaseFiles {
    private static final ObjectMapper JSON = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    public static void require(boolean valid, String code, Object context) throws IOException {
        if (!valid) throw new IOException("RELEASE-INTEGRITY-" + code + ": " + context);
    }
    static JsonNode json(Path file) throws IOException {
        try {
            JsonNode value = JSON.readTree(file.toFile());require(value != null && !value.isMissingNode(), "JSON", file);return value;
        }
        catch (IOException invalid) { throw new IOException("RELEASE-INTEGRITY-JSON: " + file, invalid); }
    }
    public static String text(JsonNode node, String key) throws IOException {
        JsonNode value = node.path(key);
        require(value.isTextual() && !value.textValue().isBlank(), "FIELD", key);
        return value.textValue();
    }
    public static long integer(JsonNode node, String key) throws IOException {
        JsonNode value = node.path(key);
        require(value.isIntegralNumber() && value.canConvertToLong() && value.longValue() >= 0, "NUMBER", key);
        return value.longValue();
    }
    static String digest(JsonNode node, String key) throws IOException {
        String value = text(node, key);require(value.matches("[0-9a-f]{64}"), "HASH-FORMAT", key);return value;
    }
    static String relative(String value) throws IOException {
        try { return atf.tests.distribution.ReleasePaths.relative(value); }
        catch (IOException invalid) { throw new IOException("RELEASE-INTEGRITY-" + invalid.getMessage(), invalid); }
    }
    public static Path inside(Path owner, String value) throws IOException {
        Path file = owner.resolve(relative(value)).normalize();
        require(Files.isRegularFile(file) && !Files.isSymbolicLink(file)
                && file.toRealPath().startsWith(owner.toRealPath()), "FILE", file);
        return file;
    }
    static Set<String> inventory(Path folder) throws IOException {
        var result = new TreeSet<String>();
        try (var files = Files.walk(folder)) {
            for (Path file : files.toList()) {
                require(!Files.isSymbolicLink(file), "SYMLINK", file);
                if (Files.isRegularFile(file)) result.add(folder.relativize(file).toString().replace('\\', '/'));
            }
        }
        return result;
    }
    static String hash(Path file) throws IOException {
        return atf.tests.distribution.ApkPayload.hash(file);
    }
    private ReleaseFiles() { }
}
