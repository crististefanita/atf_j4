package atf.tests.governance.documentation.release;

import java.io.IOException;
import java.nio.file.Path;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.regex.Pattern;

/** Delimitează intrarea curentă, fără a o confunda cu tabelul tuturor versiunilor. */
final class ReleaseCatalogPresentation {
    private ReleaseCatalogPresentation() {}

    static void projectLinks(Path file, String guide, JsonNode index, String repository, boolean currentPublished) throws IOException {
        String current = index.path("currentVersion").asText();
        require(guide.contains("(release/" + current + "/README.md)"), "CURRENT", file);
        require(!guide.contains("](" + repository + "/releases/latest)"), "UNPINNED", file);
        require(current.matches("(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)(?:-[0-9A-Za-z.-]+)?"), "VERSION", file);
        String name = "WhistLab-" + current + ".apk";
        String expected = currentPublished ? repository + "/releases/download/v" + current + "/" + name
                : "release/" + current + "/" + name;
        var download = Pattern.compile("\\[Descarcă APK ([^]]+)\\]\\(([^)]+)\\)").matcher(guide);
        require(download.find() && current.equals(download.group(1)) && expected.equals(download.group(2)), "DOWNLOAD", file);
        require(!download.find(), "AMBIGUOUS", file);
        var binaries = Pattern.compile("\\[[^]]*\\]\\(([^)]*\\.apk)\\)").matcher(guide);int count = 0;
        while (binaries.find()) { require(expected.equals(binaries.group(1)), "STALE-ASSET", file);count++; }
        require(count == 1, "ASSET-POPULATION", file);
    }

    private static void require(boolean valid, String code, Path file) throws IOException {
        if (!valid) throw new IOException("RELEASE-PROJECT-LINK-" + code + ": " + file);
    }

    static String currentSection(Path file, String catalog) throws IOException {
        int start = catalog.indexOf("## Versiunea curentă");
        int end = catalog.indexOf("## Versiuni disponibile");
        if (start < 0 || end < start) {
            throw new IOException("RELEASE-CATALOG-SECTIONS: " + file
                    + "; expected '## Versiunea curentă' before '## Versiuni disponibile'");
        }
        return catalog.substring(start, end);
    }
}
