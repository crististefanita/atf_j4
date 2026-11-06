package atf.tests.governance.documentation.release.integrity;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;
import java.util.TreeSet;

/** Strict, read-only primitives shared by release checks; paths never escape the declared owner. */
final class ReleaseFiles {
    private static final ObjectMapper JSON = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    static void require(boolean valid, String code, Object context) throws IOException {
        if (!valid) throw new IOException("RELEASE-INTEGRITY-" + code + ": " + context);
    }
    static JsonNode json(Path file) throws IOException {
        try {
            JsonNode value = JSON.readTree(file.toFile());require(value != null && !value.isMissingNode(), "JSON", file);return value;
        }
        catch (IOException invalid) { throw new IOException("RELEASE-INTEGRITY-JSON: " + file, invalid); }
    }
    static String text(JsonNode node, String key) throws IOException {
        JsonNode value = node.path(key);
        require(value.isTextual() && !value.textValue().isBlank(), "FIELD", key);
        return value.textValue();
    }
    static long integer(JsonNode node, String key) throws IOException {
        JsonNode value = node.path(key);
        require(value.isIntegralNumber() && value.canConvertToLong() && value.longValue() >= 0, "NUMBER", key);
        return value.longValue();
    }
    static String digest(JsonNode node, String key) throws IOException {
        String value = text(node, key);require(value.matches("[0-9a-f]{64}"), "HASH-FORMAT", key);return value;
    }
    static String relative(String value) throws IOException {
        require(!value.isBlank() && !value.contains("\\") && !value.matches(".*[\\p{Cntrl}:].*"), "PATH", value);
        require(!value.startsWith("/") && !value.endsWith("/"), "PATH", value);
        for (String part : value.split("/", -1)) require(!Set.of("", ".", "..").contains(part), "PATH", value);
        return value;
    }
    static Path inside(Path owner, String value) throws IOException {
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
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(file)) { byte[] buffer = new byte[8192];int read;
                while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    private ReleaseFiles() { }
}
