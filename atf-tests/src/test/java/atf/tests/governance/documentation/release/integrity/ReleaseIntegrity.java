package atf.tests.governance.documentation.release.integrity;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import static atf.tests.governance.documentation.release.integrity.ReleaseFiles.*;

/** Reconciles the declared manifest with retained bytes and independently decoded JUnit results. */
public final class ReleaseIntegrity {
    public static void verify(Path project, Path folder) throws IOException {
        JsonNode manifest = json(folder.resolve("release-validation.json"));
        require(manifest.isObject(), "MANIFEST", folder);
        require(text(manifest.path("release"), "versionName").equals(folder.getFileName().toString()), "VERSION", folder);
        ApkIntegrity.verify(project, folder, manifest.path("release"));
        payload(folder, manifest.path("integrity"));
        JsonNode qualification = manifest.path("qualification");
        var identities = qualification.path("schemaVersion").asInt(1) >= 2
                ? Set.<String>of() : JunitIntegrity.verify(folder, qualification);
        if (qualification.path("schemaVersion").asInt(1) >= 2) {
            atf.tests.governance.documentation.release.integrity.html.HtmlInvocationIntegrity.verify(
                    folder, qualification, identities, java.util.Set.of("atf-tests"));
        } else {
            atf.tests.governance.documentation.release.integrity.html.HtmlInvocationIntegrity.verify(folder, qualification, identities);
        }
        inputManifest(folder, manifest.path("source"));
        JsonNode retained = manifest.path("evidenceRetention");
        require(hash(inside(folder, text(retained, "canonicalReport"))).equals(digest(retained, "canonicalReportSha256")), "REPORT-HASH", folder);
    }
    private static void payload(Path folder, JsonNode integrity) throws IOException {
        require(integer(integrity, "schemaVersion") == 1, "RECEIPT-SCHEMA", folder);
        require(text(integrity, "kind").equals("OBSERVED_PAYLOAD_HASHES"), "RECEIPT-KIND", folder);
        try { Instant.parse(text(integrity, "observedUtc")); }
        catch (java.time.format.DateTimeParseException invalid) { throw new IOException("RELEASE-INTEGRITY-RECEIPT-TIME: " + folder, invalid); }
        var files = integrity.path("files");require(files.isArray() && !files.isEmpty(), "RECEIPT-FILES", folder);
        var declared = new HashSet<String>();
        for (var entry : files) {
            String name = text(entry, "path");require(declared.add(relative(name)), "RECEIPT-DUPLICATE", name);
            require(!name.equals("release-validation.json"), "RECEIPT-SELF", name);
            Path file = inside(folder, name);
            require(Files.size(file) == integer(entry, "bytes"), "PAYLOAD-SIZE", name);
            require(hash(file).equals(digest(entry, "sha256")), "PAYLOAD-HASH", name);
        }
        var actual = inventory(folder);actual.remove("release-validation.json");
        require(actual.equals(declared), "PAYLOAD-INVENTORY", folder);
    }
    private static void inputManifest(Path folder, JsonNode source) throws IOException {
        JsonNode inputs = json(inside(folder, text(source, "inputManifest")));
        require(digest(inputs, "fingerprint").equals(digest(source, "workingTreeFingerprint")), "SOURCE-FINGERPRINT", folder);
        JsonNode commit = source.path("sourceCommit");
        require(source.has("sourceCommit") && inputs.has("sourceCommit")
                && (commit.isNull() || commit.isTextual() && commit.textValue().matches("[0-9a-f]{40}"))
                && inputs.path("sourceCommit").equals(commit), "SOURCE-COMMIT", folder);
        var files = inputs.path("files");require(files.isArray() && !files.isEmpty(), "SOURCE-FILES", folder);
        var names = new HashSet<String>();
        for (var file : files) {
            String name = relative(text(file, "path"));require(names.add(name), "SOURCE-DUPLICATE", name);digest(file, "sha256");
        }
    }
    private ReleaseIntegrity() { }
}
