package atf.tests.governance.documentation.release.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.BASELINE;
import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.JSON;
import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.VERSION;
import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.gate;
import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.hash;
import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.refreshPayload;

final class RagGithubEvidenceFixture {
    static void selectAuthenticatedPublication(RagCandidateFixture fixture) throws Exception {
        selectAuthenticatedPublication(fixture, "GitHub API authenticated");
    }

    static void selectAuthenticatedPublication(RagCandidateFixture fixture, String channel) throws Exception {
        var manifestFile = fixture.candidate().resolve("release-validation.json");
        ObjectNode manifest = (ObjectNode) JSON.readTree(manifestFile.toFile());
        ObjectNode publication = (ObjectNode) manifest.path("publication");
        publication.put("state", "PUBLISHED").put("accessMode", "AUTHENTICATED").put("repository", "example/game");
        Path receiptFile = fixture.candidate().resolve("reports/distribution-verification.json");
        Files.createDirectories(receiptFile.getParent());
        String apkHash = manifest.path("release").path("apkSha256").asText();
        long apkBytes = manifest.path("release").path("apkBytes").asLong();
        String url = "https://github.com/example/game/releases/download/v" + VERSION + "/WhistLab-" + VERSION + ".apk";
        publication.put("targetUrl", url);
        ObjectNode receipt = JSON.createObjectNode();
        ObjectNode download = receipt.putObject("authenticatedDownload").put("url", url).put("authenticated", true)
                .put("authMode", "GITHUB_ACCOUNT").put("bytes", apkBytes)
                .put("sha256", apkHash).put("zipCrc", "PASS").put("channel", channel);
        if (channel.equals("Browser MCP authenticated")) download.put("downloadCompleted", true);
        else download.put("httpStatus", 200);
        JSON.writeValue(receiptFile.toFile(), receipt);
        ((ObjectNode) manifest.path("qualification").path("platforms").path("android").path("gates"))
                .set("distributionDownload", gate("reports/distribution-verification.json"));
        JSON.writerWithDefaultPrettyPrinter().writeValue(manifestFile.toFile(), manifest);
        refreshPayload(fixture.candidate());
        writeGithubRetention(fixture);
    }

    private static void writeGithubRetention(RagCandidateFixture fixture) throws Exception {
        Path catalog = fixture.project().resolve("release/index.json");
        ObjectNode index = (ObjectNode) JSON.readTree(catalog.toFile());
        index.put("currentVersion", VERSION).putArray("versions").add(VERSION).add(BASELINE);
        JSON.writeValue(catalog.toFile(), index);
        Path currentDir = Files.createDirectories(fixture.project().resolve("release/" + VERSION));
        Path currentManifest = currentDir.resolve("release-validation.json");
        Files.copy(fixture.candidate().resolve("release-validation.json"), currentManifest);

        Path previousManifest = fixture.project().resolve("release/" + BASELINE + "/release-validation.json");
        var previous = (ObjectNode) JSON.readTree(previousManifest.toFile());
        String apk = "WhistLab-" + BASELINE + ".apk";
        ((ObjectNode) previous.path("release")).put("apkName", apk).put("apkRelativePath", apk)
                .put("apkBytes", 100).put("apkSha256", "a".repeat(64));
        ((ObjectNode) previous.path("publication")).put("repository", "example/game")
                .put("targetUrl", "https://github.com/example/game/releases/download/v" + BASELINE + "/" + apk);
        var inventory = (com.fasterxml.jackson.databind.node.ArrayNode) previous.at("/integrity/files");
        inventory.addObject().put("path", apk).put("bytes", 100).put("sha256", "a".repeat(64));
        inventory.addObject().put("path", apk + ".sha256").put("bytes", 88).put("sha256", "b".repeat(64));
        JSON.writeValue(previousManifest.toFile(), previous);

        var observation = JSON.createObjectNode().put("schemaVersion", 1).put("kind", "github-release-retention")
                .put("observedAtUtc", new java.time.format.DateTimeFormatterBuilder().appendInstant(3).toFormatter()
                        .format(java.time.Instant.now()))
                .put("repository", "example/game").put("catalogSha256", hash(catalog))
                .put("mainCommit", "e".repeat(40)).put("releaseListAccess", "AUTHENTICATED_WRITE");
        var mainFiles = observation.putObject("mainFileSha256").put("release/index.json", hash(catalog));
        for (Path file : List.of(currentManifest, previousManifest)) {
            mainFiles.put(fixture.project().relativize(file).toString().replace('\\', '/'), hash(file));
        }
        var refs = observation.putArray("tags");
        var releases = observation.putArray("releases");
        long id = 1;
        for (Path manifestPath : List.of(currentManifest, previousManifest)) {
            JsonNode manifest = JSON.readTree(manifestPath.toFile());
            String name = manifest.at("/release/apkName").asText();
            String tag = "v" + manifest.at("/release/versionName").asText();
            refs.addObject().put("tag", tag).put("commit", manifest.at("/source/sourceCommit").asText());
            var assets = releases.addObject().put("tag", tag).put("id", id++).put("draft", false)
                    .put("publishedAt", "2026-11-13T00:00:00Z").putArray("assets");
            for (String asset : List.of(name, name + ".sha256")) {
                for (JsonNode entry : manifest.at("/integrity/files")) {
                    if (!asset.equals(entry.path("path").asText())) continue;
                    long bytes = entry.path("bytes").asLong();
                    String digest = entry.path("sha256").asText();
                    assets.addObject().put("name", asset).put("id", id++).put("state", "uploaded")
                            .put("url", "https://github.com/example/game/releases/download/" + tag + "/" + asset)
                            .put("size", bytes).put("sha256", digest).putObject("download")
                            .put("httpStatus", 200).put("bytes", bytes).put("sha256", digest);
                }
            }
        }
        JSON.writeValue(fixture.output().resolveSibling("github-retention.json").toFile(), observation);
    }

    private RagGithubEvidenceFixture() { }
}
