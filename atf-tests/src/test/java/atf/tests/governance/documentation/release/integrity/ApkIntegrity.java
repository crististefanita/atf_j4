package atf.tests.governance.documentation.release.integrity;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import atf.tests.distribution.ApkPayload;
import static atf.tests.governance.documentation.release.integrity.ReleaseFiles.*;

/** Byte identity and archive consistency; certificate and Android runtime acceptance are separate. */
final class ApkIntegrity {
    static void verify(Path project, Path folder, JsonNode release) throws IOException {
        String version = text(release, "versionName"), name = text(release, "apkName");
        require(name.equals("WhistLab-" + version + ".apk") && !name.contains("/"), "APK-NAME", name);
        require(text(release, "apkRelativePath").equals(name), "APK-PATH", name);
        Path apk = folder.resolve(name);
        require(Files.isRegularFile(apk) && !Files.isSymbolicLink(apk)
                && apk.toRealPath().getParent().equals(folder.toRealPath())
                && folder.toRealPath().startsWith(project.resolve("release").toRealPath()), "APK-FILE", apk);
        long bytes = integer(release, "apkBytes");String expected = digest(release, "apkSha256");
        try { ApkPayload.verify(apk, bytes, expected); }
        catch (IOException invalid) { throw new IOException("RELEASE-INTEGRITY-" + invalid.getMessage(), invalid); }
        Path checksum = inside(folder, name + ".sha256");
        require(Files.readString(checksum).strip().equals(expected + "  " + name), "APK-CHECKSUM", checksum);
        require(Files.size(apk) == bytes && hash(apk).equals(expected), "APK-CHANGED", apk);
    }
    private ApkIntegrity() { }
}
