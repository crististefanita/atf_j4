package atf.tests.governance.documentation.release;

import atf.tests.distribution.ApkDownloadTarget;
import atf.tests.distribution.ReleaseDownloadSelection;
import atf.tests.distribution.ReleaseVersion;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import atf.tests.governance.support.GovernanceProject;
import java.util.regex.Pattern;

/** Delimitează intrarea curentă, fără a o confunda cu tabelul tuturor versiunilor. */
final class ReleaseCatalogPresentation {
    private ReleaseCatalogPresentation() {}

    static String downloadTarget(Path releaseFolder, JsonNode manifest, String repository) throws IOException {
        var release = manifest.required("release");
        String version = release.required("versionName").asText();
        String publicationState = manifest.path("publication").path("state").asText();
        if (ReleaseDownloadSelection.isMainBranchCandidate(manifest)) {
            var target = new ApkDownloadTarget(URI.create(repository).getPath().substring(1), version,
                    release.required("apkBytes").asLong(), release.required("apkSha256").asText(), true);
            require(target.fileName().equals(release.path("apkName").asText()), "MAIN-BRANCH-APK", releaseFolder);
            return target.publicUrl();
        }
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
                    && authenticatedDownload.path("bytes").asLong(-1) == release.required("apkBytes").asLong()
                    && authenticatedDownload.path("sha256").asText().equals(release.required("apkSha256").asText())
                    && authenticatedDownload.path("zipCrc").asText().equals("PASS")
                    && authenticatedDownloadChannelMatches(authenticatedDownload),
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

    static boolean authenticatedDownloadChannelMatches(JsonNode download) throws IOException {
        Path root = GovernanceProject.locateProjectRoot();
        JsonNode policy = new ObjectMapper().readTree(root.resolve("config/release-platform-rag-policy.json").toFile());
        JsonNode channels = policy.path("rules").path("authenticatedDownloadChannels");
        if (!channels.isObject() || channels.isEmpty()) throw new IOException("AUTHENTICATED-CHANNEL-POLICY");
        String proof = channels.path(download.path("channel").asText()).asText();
        return switch (proof) {
            case "HTTP_200" -> download.path("httpStatus").asInt() == 200;
            case "COMPLETED_DOWNLOAD" -> download.path("downloadCompleted").asBoolean();
            default -> false;
        };
    }

    private static String plannedAuthenticatedTarget(JsonNode manifest, String repository, String version, String apkName)
            throws IOException {
        var publication = manifest.path("publication");
        String target = publication.path("targetUrl").asText();
        if (target.isBlank()) return null;
        String expected = releaseDownload(repository, version, true);
        require(java.util.Set.of("PREPARED", "QUALIFIED").contains(publication.path("state").asText())
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
        String advertised = index.path("candidateVersion").asText(index.path("currentVersion").asText());
        require(guide.contains("(release/README.md)"), "CATALOG", file);
        require(!guide.contains("](" + repository + "/releases/latest)"), "UNPINNED", file);
        require(ReleaseVersion.isValid(advertised), "VERSION", file);

        var download = Pattern.compile("\\[(?:Download|Descarcă) APK ([^]]+)\\]\\(([^)]+)\\)").matcher(guide);
        require(download.find() && advertised.equals(download.group(1)) && expected.equals(download.group(2)), "DOWNLOAD", file);
        require(!download.find(), "AMBIGUOUS", file);
        var binaries = Pattern.compile("\\[[^]]*\\]\\(([^)]*\\.apk)\\)").matcher(guide);int count = 0;
        while (binaries.find()) { require(expected.equals(binaries.group(1)), "STALE-ASSET", file);count++; }
        require(count == 1, "ASSET-POPULATION", file);
    }

    private static void require(boolean valid, String code, Path file) throws IOException {
        if (!valid) throw new IOException("RELEASE-PROJECT-LINK-" + code + ": " + file);
    }

    static String currentSection(Path file, String catalog) throws IOException {
        var current = Pattern.compile("(?m)^## (?:Current version|Versiunea curentă)$").matcher(catalog);
        var available = Pattern.compile("(?m)^## (?:Available versions|Versiuni disponibile)$").matcher(catalog);
        int start = current.find() ? current.start() : -1;
        int end = available.find() ? available.start() : -1;
        if (start < 0 || end < start) {
            throw new IOException("RELEASE-CATALOG-SECTIONS: " + file
                    + "; expected a Current version section before Available versions");
        }
        return catalog.substring(start, end);
    }
}
