package atf.tests.governance.documentation.release;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.regex.Pattern;

/** Delimitează intrarea curentă, fără a o confunda cu tabelul tuturor versiunilor. */
final class ReleaseCatalogPresentation {
    private ReleaseCatalogPresentation() {}

    static String downloadTarget(Path releaseFolder, JsonNode manifest, String repository) throws IOException {
        var release = manifest.required("release");
        String version = release.required("versionName").asText();
        String publicationState = manifest.path("publication").path("state").asText();
        Path receipt = releaseFolder.resolve("reports/distribution-verification.json");
        if (!Files.isRegularFile(receipt)) {
            require(!publicationState.equals("PUBLISHED_SITE"), "SITE-RECEIPT", receipt);
            String planned = plannedAuthenticatedTarget(manifest, repository, version, release.path("apkName").asText());
            if (planned != null) return planned;
            return releaseDownload(repository, version, publicationState.equals("PUBLISHED"));
        }

        JsonNode distribution = new ObjectMapper().readTree(receipt.toFile());
        var authenticatedDownload = distribution.path("authenticatedDownload");
        if (!authenticatedDownload.isMissingNode()) {
            String expected = releaseDownload(repository, version, true);
            require(publicationState.equals("PUBLISHED")
                    && manifest.path("publication").path("accessMode").asText().equals("AUTHENTICATED"),
                    "AUTHENTICATED-STATE", receipt);
            require(authenticatedDownload.path("url").asText().equals(expected)
                    && authenticatedDownload.path("authenticated").asBoolean()
                    && authenticatedDownload.path("authMode").asText().equals("GITHUB_ACCOUNT")
                    && authenticatedDownload.path("httpStatus").asInt() == 200
                    && authenticatedDownload.path("bytes").asLong(-1) == release.required("apkBytes").asLong()
                    && authenticatedDownload.path("sha256").asText().equals(release.required("apkSha256").asText())
                    && authenticatedDownload.path("zipCrc").asText().equals("PASS")
                    && authenticatedDownload.path("channel").asText().equals("GitHub API authenticated"),
                    "AUTHENTICATED-EVIDENCE", receipt);
            return expected;
        }
        var publicDownload = distribution.path("publicDownload");
        if (publicDownload.isMissingNode()) {
            require(!publicationState.equals("PUBLISHED_SITE"), "SITE-RECEIPT", receipt);
            String planned = plannedAuthenticatedTarget(manifest, repository, version, release.path("apkName").asText());
            if (planned != null) return planned;
            return releaseDownload(repository, version, publicationState.equals("PUBLISHED"));
        }
        String url = publicDownload.path("url").asText();
        var address = URI.create(url);
        require("https".equals(address.getScheme()) && address.getHost() != null && address.getUserInfo() == null
                && address.getQuery() == null && address.getFragment() == null
                && address.getPath().endsWith("/v" + version + "/WhistLab-" + version + ".apk"), "PUBLIC-URL", receipt);
        require(publicDownload.path("httpStatus").asInt() == 200
                && publicDownload.path("bytes").asLong(-1) == release.required("apkBytes").asLong()
                && publicDownload.path("sha256").asText().equals(release.required("apkSha256").asText()), "PUBLIC-EVIDENCE", receipt);
        return url;
    }

    private static String plannedAuthenticatedTarget(JsonNode manifest, String repository, String version, String apkName)
            throws IOException {
        var publication = manifest.path("publication");
        String target = publication.path("targetUrl").asText();
        if (target.isBlank()) return null;
        String expected = releaseDownload(repository, version, true);
        require(publication.path("state").asText().equals("PREPARED")
                && publication.path("accessMode").asText().equals("AUTHENTICATED")
                && expected.endsWith("/" + apkName) && target.equals(expected), "PLANNED-AUTHENTICATED-URL",
                Path.of("release-validation.json"));
        return expected;
    }

    static String releaseDownload(String repository, String version, boolean published) {
        String apk = "WhistLab-" + version + ".apk";
        return published ? repository + "/releases/download/v" + version + "/" + apk : "release/" + version + "/" + apk;
    }

    static void projectLinks(Path file, String guide, JsonNode index, String repository, String expected) throws IOException {
        String current = index.path("currentVersion").asText();
        require(guide.contains("(release/" + current + "/README.md)"), "CURRENT", file);
        require(!guide.contains("](" + repository + "/releases/latest)"), "UNPINNED", file);
        require(current.matches("(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)(?:-[0-9A-Za-z.-]+)?"), "VERSION", file);

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
