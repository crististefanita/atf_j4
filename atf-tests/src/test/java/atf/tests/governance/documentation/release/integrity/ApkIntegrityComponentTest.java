package atf.tests.governance.documentation.release.integrity;

import atf.testkit.assertions.AuditedAssertions;

import atf.testkit.AtfComponentTest;
import atf.tests.governance.documentation.release.integrity.fixture.ReleaseFixture;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class ApkIntegrityComponentTest {
    @TempDir Path temporary;
    @Test void aConsistentApkAndItsPayloadAreReadWithoutChangingThem() throws Exception {
        var fixture = ReleaseFixture.create(temporary);
        byte[] manifest = Files.readAllBytes(fixture.folder().resolve("release-validation.json"));
        byte[] apk = Files.readAllBytes(fixture.apk());
        ReleaseIntegrity.verify(temporary, fixture.folder());
        AuditedAssertions.assertArrayEquals(apk, Files.readAllBytes(fixture.apk()));
        AuditedAssertions.assertArrayEquals(manifest, Files.readAllBytes(fixture.folder().resolve("release-validation.json")));
    }
    @ParameterizedTest @ValueSource(strings = {"missing", "same-size-change", "valid-changed-zip", "truncated", "checksum", "missing-checksum", "path", "legacy-dist", "name", "hash"})
    void missingChangedOrMisidentifiedBinariesCannotPass(String defect) throws Exception {
        var fixture = ReleaseFixture.create(temporary);
        switch (defect) {
            case "missing" -> Files.delete(fixture.apk());
            case "same-size-change" -> { byte[] bytes = Files.readAllBytes(fixture.apk());bytes[40] ^= 1;Files.write(fixture.apk(), bytes); }
            case "valid-changed-zip" -> {
                var declared = fixture.release().deepCopy();
                Path checksum = fixture.apk().resolveSibling(fixture.apk().getFileName() + ".sha256");
                String originalChecksum = Files.readString(checksum);
                fixture.archive(Map.of("AndroidManifest.xml", "manifest", "classes.dex", "DEX-CHANGED"));
                AuditedAssertions.assertEquals(declared.path("apkBytes").longValue(), Files.size(fixture.apk()));
                fixture.manifest().set("release", declared);Files.writeString(checksum, originalChecksum);
            }
            case "truncated" -> Files.write(fixture.apk(), new byte[]{1});
            case "checksum" -> Files.writeString(fixture.apk().resolveSibling(fixture.apk().getFileName() + ".sha256"), "wrong");
            case "missing-checksum" -> Files.delete(fixture.apk().resolveSibling(fixture.apk().getFileName() + ".sha256"));
            case "path" -> fixture.release().put("apkRelativePath", "../../outside.apk");
            case "legacy-dist" -> fixture.release().put("apkRelativePath", "../../dist/" + fixture.apk().getFileName());
            case "name" -> fixture.release().put("apkName", "WhistLab-9.9.9.apk");
            case "hash" -> fixture.release().put("apkSha256", "0".repeat(64));
            default -> fail(defect);
        }
        fixture.save();rejected(fixture, defect.equals("missing-checksum") ? "FILE" : "APK-");
    }
    @ParameterizedTest @ValueSource(strings = {"bad-zip", "bad-crc", "no-manifest", "no-dex", "traversal", "duplicate"})
    void matchingHashesDoNotSubstituteForAConsistentApkArchive(String defect) throws Exception {
        var fixture = ReleaseFixture.create(temporary);
        switch (defect) {
            case "bad-zip" -> Files.write(fixture.apk(), new byte[]{1, 2, 3});
            case "bad-crc" -> {
                byte[] bytes = Files.readAllBytes(fixture.apk());
                int offset = new String(bytes, StandardCharsets.ISO_8859_1).indexOf("DEX-FIXTURE");
                AuditedAssertions.assertTrue(offset >= 0);bytes[offset] ^= 1;Files.write(fixture.apk(), bytes);
            }
            case "no-manifest" -> fixture.archive(Map.of("classes.dex", "dex"));
            case "no-dex" -> fixture.archive(Map.of("AndroidManifest.xml", "manifest"));
            case "traversal" -> fixture.archive(Map.of("AndroidManifest.xml", "manifest", "classes.dex", "dex", "../escape", "bad"));
            case "duplicate" -> {
                fixture.archive(Map.of("AndroidManifest.xml", "manifest", "classes.dex", "dex", "copy000.dex", "dex"));
                byte[] bytes = Files.readAllBytes(fixture.apk());
                bytes = new String(bytes, StandardCharsets.ISO_8859_1).replace("copy000.dex", "classes.dex").getBytes(StandardCharsets.ISO_8859_1);
                Files.write(fixture.apk(), bytes);
            }
            default -> fail(defect);
        }
        fixture.refreshApk();fixture.capture();
        rejected(fixture, defect.equals("traversal") ? "PATH" : "APK-");
    }
    private static void rejected(ReleaseFixture fixture, String code) {
        var error = AuditedAssertions.assertThrows(java.io.IOException.class, () -> ReleaseIntegrity.verify(fixture.project(), fixture.folder()));
        AuditedAssertions.assertTrue(error.getMessage().startsWith("RELEASE-INTEGRITY-" + code), error::toString);
    }
}
