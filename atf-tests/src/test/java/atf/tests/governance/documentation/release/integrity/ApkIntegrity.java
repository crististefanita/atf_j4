package atf.tests.governance.documentation.release.integrity;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.zip.CRC32;
import java.util.zip.ZipFile;
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
        long bytes = integer(release, "apkBytes");require(bytes > 0 && Files.size(apk) == bytes, "APK-SIZE", apk);
        String expected = digest(release, "apkSha256");require(hash(apk).equals(expected), "APK-HASH", apk);
        Path checksum = inside(folder, name + ".sha256");
        require(Files.readString(checksum).strip().equals(expected + "  " + name), "APK-CHECKSUM", checksum);
        archive(apk);
        require(Files.size(apk) == bytes && hash(apk).equals(expected), "APK-CHANGED", apk);
    }
    private static void archive(Path apk) throws IOException {
        var names = new HashSet<String>();
        try (var zip = new ZipFile(apk.toFile())) {
            var entries = zip.entries();byte[] buffer = new byte[8192];
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();String name = entry.getName();
                require(names.add(name), "APK-ENTRY-DUPLICATE", name);
                relative(entry.isDirectory() ? name.substring(0, name.length() - 1) : name);
                if (entry.isDirectory()) continue;
                CRC32 crc = new CRC32();long count = 0;
                try (var input = zip.getInputStream(entry)) { int read;
                    while ((read = input.read(buffer)) != -1) { crc.update(buffer, 0, read);count = Math.addExact(count, read); }
                }
                require(count == entry.getSize() && crc.getValue() == entry.getCrc(), "APK-CRC", name);
            }
        } catch (java.util.zip.ZipException corrupt) { throw new IOException("RELEASE-INTEGRITY-APK-ZIP: " + apk, corrupt); }
        require(names.contains("AndroidManifest.xml") && names.contains("classes.dex"), "APK-CONTENT", apk);
    }
    private ApkIntegrity() { }
}
