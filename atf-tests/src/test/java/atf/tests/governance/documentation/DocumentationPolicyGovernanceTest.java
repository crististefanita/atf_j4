package atf.tests.governance.documentation;

import atf.testkit.assertions.AuditedAssertions;

import atf.testkit.AtfComponentTest;
import atf.tests.governance.documentation.links.DocumentationLinkGovernanceTest;
import atf.tests.governance.support.GovernanceProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class DocumentationPolicyGovernanceTest {
    @Test void theInventoryContainsAllModuleReadmesAndNoPerReleaseVerificationNotes() throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        var files = GovernanceProject.markdown(root);
        String publicRepository = GovernanceProject.policy(root).path("publicRepository").asText("");
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        var index = json.readTree(root.resolve("release/index.json").toFile());
        String advertised = atf.tests.distribution.ReleasePaths.relative(index.path("candidateVersion").asText(
                index.required("currentVersion").asText()));
        Path manifestPath = index.path("candidateVersion").isTextual()
                ? root.resolve("release/candidate.json")
                : root.resolve("release").resolve(advertised).resolve("release-validation.json");
        var manifest = json.readTree(manifestPath.toFile());
        var declaredDownload = atf.tests.distribution.ReleaseDownloadSelection.select("advertised",
                publicRepository.replace("https://github.com/", ""), index, manifest);
        AuditedAssertions.assertTrue(files.contains(root.resolve("README.md")));
        var modules = GovernanceProject.policy(root).required("mavenModules");
        for (var module : modules) AuditedAssertions.assertTrue(files.contains(root.resolve(module.asText()).resolve("README.md")),
                () -> "Missing governed module README: " + module);
        for (Path file : files) {
            AuditedAssertions.assertFalse(file.getFileName().toString().matches("ATF_Reporting_J[0-9]+_Verification\\.md"),
                    () -> "Release chronology belongs in the external archive: " + file);
            if (file.getFileName().toString().equalsIgnoreCase("README.md") || GovernanceProject.isLibraryGuide(root, file)) {
                AuditedAssertions.assertFalse(hasEnglishReadmeProse(Files.readString(file)), () -> "README prose must be Romanian: " + file);
            }
            String prose = deploymentProse(Files.readString(file));
            String relative = GovernanceProject.relative(root, file);
            prose = publicReleaseProse(prose, publicRepository, declaredDownload);
            prose = historicalReleaseProse(relative, prose, publicRepository);
            AuditedAssertions.assertFalse(java.util.regex.Pattern.compile("(?i)\\b(?:atf[_-]j[0-9]+|J[0-9]+)\\b").matcher(prose).find(),
                    () -> "Project documentation must use its local contracts, not external project identifiers: " + file);
        }
    }

    /** A declared repository's stable release URI is technical data; visible link text remains prose. */
    static String publicReleaseProse(String source, String repository) {
        return publicReleaseProse(source, repository, null);
    }

    static String publicReleaseProse(String source, String repository, atf.tests.distribution.ApkDownloadTarget published) {
        if (published != null && !repository.equals("https://github.com/" + published.repository()))
            throw new IllegalArgumentException("Published technical data must belong to the declared repository");
        if (repository.isEmpty()) return source;
        if (!repository.matches("https://github\\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")
                || !java.net.URI.create(repository).normalize().toString().equals(repository))
            throw new IllegalArgumentException("Expected a canonical public GitHub repository URI");
        var links = java.util.regex.Pattern.compile("(?<!!)\\[([^\\]\\r\\n]*)\\]\\(([^)\\s]+)\\)").matcher(source);
        var result = new StringBuffer();
        while (links.find()) {
            String replacement = (links.group(2).equals(repository + "/releases/latest")
                    || published != null && (links.group(2).equals(published.releasePageUrl())
                            || links.group(2).equals(published.publicUrl())
                            || links.group(2).equals(published.publicUrl() + ".sha256")))
                    ? "[" + links.group(1) + "](<resursă-publică>)" : links.group();
            links.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        return links.appendTail(result).toString();
    }

    /** Historical release READMEs may link to their own tagged asset; visible labels remain governed prose. */
    static String historicalReleaseProse(String relative, String source, String repository) {
        var archive = java.util.regex.Pattern.compile(
                "^release/([0-9]+\\.[0-9]+\\.[0-9]+)/README\\.md$").matcher(relative.replace('\\', '/'));
        if (!archive.matches() || repository.isEmpty()) return source;
        String version = archive.group(1);
        String page = repository + "/releases/tag/v" + version;
        String apk = repository + "/releases/download/v" + version + "/WhistLab-" + version + ".apk";
        String checksum = apk + ".sha256";
        var links = java.util.regex.Pattern.compile("(?<!!)\\[([^\\]\\r\\n]*)\\]\\(([^)\\s]+)\\)").matcher(source);
        var result = new StringBuffer();
        while (links.find()) {
            String replacement = links.group(2).equals(repository)
                    || links.group(2).equals(page) || links.group(2).equals(apk) || links.group(2).equals(checksum)
                    ? "[" + links.group(1) + "](<resursă-publică>)" : links.group();
            links.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        return links.appendTail(result).toString();
    }

    @Test void historicalReleaseReadmeRecognizesOnlyItsOwnTaggedUrisAsTechnicalData() {
        String repository = "https://github.com/example/atf_j4";
        String page = repository + "/releases/tag/v0.17.1";
        String apk = repository + "/releases/download/v0.17.1/WhistLab-0.17.1.apk";
        String checksum = apk + ".sha256";
        String source = "[README principal](" + repository + ") · [GitHub Release](" + page + ") · [APK](" + apk + ") · [Checksum](" + checksum + ")";
        AuditedAssertions.assertEquals(
                "[README principal](<resursă-publică>) · [GitHub Release](<resursă-publică>) · [APK](<resursă-publică>) · [Checksum](<resursă-publică>)",
                historicalReleaseProse("release/0.17.1/README.md", source, repository));
        AuditedAssertions.assertEquals("Context J4. [APK](<resursă-publică>)",
                historicalReleaseProse("release/0.17.1/README.md", "Context J4. [APK](" + apk + ")", repository));
        for (String unchanged : List.of(
                "[APK](" + repository + "/releases/download/v0.17.0/WhistLab-0.17.0.apk)",
                "[Checksum](" + repository + "/releases/download/v0.17.0/WhistLab-0.17.0.apk.sha256)",
                "[APK](" + repository + "/releases/download/v0.17.1/Other.apk)",
                "[README](" + repository + "/tree/main)",
                "[README](https://github.com/foreign/atf_j4)",
                "[APK](https://github.com/foreign/atf_j4/releases/download/v0.17.1/WhistLab-0.17.1.apk)",
                "![APK](" + apk + ")")) {
            AuditedAssertions.assertEquals(unchanged,
                    historicalReleaseProse("release/0.17.1/README.md", unchanged, repository));
        }
        AuditedAssertions.assertEquals("[README](" + repository + ")",
                historicalReleaseProse("README.md", "[README](" + repository + ")", repository));
        AuditedAssertions.assertEquals("[APK](" + apk + ")",
                historicalReleaseProse("README.md", "[APK](" + apk + ")", repository));
    }

    @Test void publicReleaseUriDoesNotExemptVisibleProseOrForeignAndAlteredDestinations() {
        String repository = "https://github.com/example/atf_j4", latest = repository + "/releases/latest";
        AuditedAssertions.assertEquals("[APK](<resursă-publică>)", publicReleaseProse("[APK](" + latest + ")", repository));
        AuditedAssertions.assertEquals("Context J4. [atf_j4](<resursă-publică>)",
                publicReleaseProse("Context J4. [atf_j4](" + latest + ")", repository));
        for (String original : List.of(latest, "[APK](https://github.com/example/atf_j1/releases/latest)",
                "[APK](" + repository + "/tree/main)", "[APK](" + latest + "?next=atf_j1)",
                "[APK](" + latest + "#J1)", "[APK](" + latest + "/)", "![APK](" + latest + ")"))
            AuditedAssertions.assertEquals(original, publicReleaseProse(original, repository), original);
        AuditedAssertions.assertEquals("[APK](" + latest + ")", publicReleaseProse("[APK](" + latest + ")", ""));
        for (String invalid : List.of("https://github.com/example/../atf_j4", "http://github.com/example/atf_j4",
                repository + "?query=1", repository + "/", "https://elsewhere.test/example/atf_j4"))
            AuditedAssertions.assertThrows(IllegalArgumentException.class, () -> publicReleaseProse("Control J4", invalid));
    }

    @Test void onlyTheDeclaredPublishedAssetIsTechnicalDataWhileItsVisibleTextRemainsProse() {
        var published = new atf.tests.distribution.ApkDownloadTarget("example/atf_j4", "1.2.0", 42, "a".repeat(64));
        String repository = "https://github.com/" + published.repository(), asset = published.publicUrl();
        AuditedAssertions.assertEquals("[APK](<resursă-publică>)", publicReleaseProse("[APK](" + asset + ")", repository, published));
        AuditedAssertions.assertEquals("[SHA-256](<resursă-publică>)",
                publicReleaseProse("[SHA-256](" + asset + ".sha256)", repository, published), "The declared APK and its checksum share one identity");
        AuditedAssertions.assertEquals("Context J4. [atf_j4](<resursă-publică>)",
                publicReleaseProse("Context J4. [atf_j4](" + asset + ")", repository, published));
        for (String original : List.of(asset, "![APK](" + asset + ")", "[APK](" + asset + "?next=atf_j1)",
                "[APK](" + asset + "#J1)", "[APK](" + asset + "/)", "[APK](" + asset.replace("WhistLab-", "Other-") + ")",
                "[APK](" + asset.replace("1.2.0", "1.1.0") + ")", "[APK](" + asset.replace("example/", "foreign/") + ")",
                "[SHA-256](" + asset.replace("1.2.0", "1.1.0") + ".sha256)", "[SHA-256](" + asset + ".sha256?next=atf_j1)"))
            AuditedAssertions.assertEquals(original, publicReleaseProse(original, repository, published), "Only the declared canonical asset URI is technical data");
        AuditedAssertions.assertThrows(IllegalArgumentException.class,
                () -> publicReleaseProse("Control J4", "https://github.com/foreign/atf_j4", published));
    }


    @Test void onlyTheDeclaredReleasePageIsTechnicalDataAlongsideItsAsset() {
        var published = new atf.tests.distribution.ApkDownloadTarget("example/atf_j4", "1.2.0", 42, "a".repeat(64));
        String repository = "https://github.com/" + published.repository();
        String page = repository + "/releases/tag/v1.2.0";
        AuditedAssertions.assertEquals("[Versiune](<resursă-publică>)",
                publicReleaseProse("[Versiune](" + page + ")", repository, published));
        AuditedAssertions.assertEquals("Context J4. [atf_j4](<resursă-publică>)",
                publicReleaseProse("Context J4. [atf_j4](" + page + ")", repository, published));
        AuditedAssertions.assertEquals("[Versiune](" + page + ")",
                publicReleaseProse("[Versiune](" + page + ")", repository), "An undeclared version page has no exemption");
        for (String unchanged : List.of(page, "![Versiune](" + page + ")",
                "[Versiune](" + page + "?download=1)", "[Versiune](" + page + "#Assets)",
                "[Versiune](" + page + "/)", "[Versiune](" + page + ".sha256)",
                "[Versiune](" + page.replace("1.2.0", "1.1.0") + ")",
                "[Versiune](" + page.replace("/tag/v", "/tag/") + ")",
                "[Versiune](" + page.replace("example/", "foreign/") + ")"))
            AuditedAssertions.assertEquals(unchanged, publicReleaseProse(unchanged, repository, published), unchanged);
    }

    @Test void libraryUsageGuidesAreGovernedAsLocalConsumerContracts() throws Exception {
        Path root = GovernanceProject.locateProjectRoot(); var files = GovernanceProject.markdown(root);
        for (String name : List.of("README.md", "README.Core.md", "README.Reporting.md", "README.Console.md", "README.Mock.md", "README.TestKit.md")) {
            Path file = root.resolve("lib").resolve(name); AuditedAssertions.assertTrue(files.contains(file), name);
            String source = Files.readString(file);
            AuditedAssertions.assertFalse(java.util.regex.Pattern.compile("(?i)\\batf[_-]j[0-9]+\\b").matcher(source).find(), name);
            AuditedAssertions.assertFalse(hasEnglishReadmeProse(source), name);
        }
    }

    @Test void feedbackPriorityRegisterUsesUniqueScoresFromHighestToLowest() throws Exception {
        Path root = GovernanceProject.locateProjectRoot();
        String source = Files.readString(root.resolve("docs/feedback/README.md"));
        int heading = source.indexOf("## ToDo-uri prioritizate");
        AuditedAssertions.assertTrue(heading >= 0, "The active feedback overview must expose a prioritized ToDo register");
        int nextHeading = source.indexOf("\n## ", heading + 1);
        String section = source.substring(heading, nextHeading < 0 ? source.length() : nextHeading);
        var rows = java.util.regex.Pattern.compile("(?m)^\\| ([A-Z]+-[0-9]{2}) \\| P[12] \\| ([0-9]{1,3}) \\| .+ \\|$")
                .matcher(section);
        var ids = new java.util.HashSet<String>();
        var scores = new ArrayList<Integer>();
        while (rows.find()) {
            AuditedAssertions.assertTrue(ids.add(rows.group(1)), () -> "Duplicate feedback priority ID: " + rows.group(1));
            int score = Integer.parseInt(rows.group(2));
            AuditedAssertions.assertTrue(score >= 1 && score <= 100, () -> "Priority score is outside 1–100: " + score);
            scores.add(score);
        }
        AuditedAssertions.assertTrue(scores.size() >= 10, "The overview must prioritize the open work, not a token sample");
        for (int index = 1; index < scores.size(); index++)
            AuditedAssertions.assertTrue(scores.get(index - 1) >= scores.get(index), "Priority scores must be sorted highest first");
    }

    /** Only the identity of a well-formed change control is technical data, not editorial prose. */
    static String deploymentProse(String source) {
        var fences = java.util.regex.Pattern.compile("(?ms)^```yaml[ \\t]*\\R(.*?)^```[ \\t]*(?=\\R|\\z)").matcher(source);
        var result = new StringBuffer();
        while (fences.find()) {
            String replacement = fences.group();
            var metadata = changeControlMetadata(fences.group(1));
            if (metadata != null) {
                for (var row : metadata.path("testChanges"))
                    ((com.fasterxml.jackson.databind.node.ObjectNode) row).put("id", "<identificator-tehnic>");
                replacement = "```yaml\n" + metadata + "\n```";
            }
            fences.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        return fences.appendTail(result).toString();
    }

    private static com.fasterxml.jackson.databind.JsonNode changeControlMetadata(String yaml) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory()
                    .enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION))
                    .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
            var root = mapper.readTree(yaml);
            if (root == null || !root.isObject() || root.size() != 1 || !root.path("testChanges").isArray()
                    || root.path("testChanges").isEmpty()) return null;
            for (var row : root.path("testChanges")) {
                if (!row.isObject() || row.size() != (row.has("allowDelete") ? 8 : 7)) return null;
                for (String field : List.of("id", "status", "base", "expires", "reason", "proof"))
                    if (!row.path(field).isTextual() || row.path(field).asText().isBlank()) return null;
                if (!row.path("id").asText().matches("[A-Z][A-Z0-9-]{3,79}")
                        || !java.util.Set.of("ACTIVE", "VERIFIED", "CLOSED", "DRAFT").contains(row.path("status").asText())
                        || !row.path("base").asText().matches("[a-f0-9]{40}|[a-f0-9]{64}")
                        || row.path("reason").asText().length() < 20
                        || !row.path("proof").asText().startsWith("mvn ")
                        || !(row.path("proof").asText().contains("test") || row.path("proof").asText().contains("verify"))) return null;
                java.time.LocalDate.parse(row.path("expires").asText());
                if (!row.path("paths").isArray() || row.path("paths").isEmpty()
                        || row.has("allowDelete") && !row.path("allowDelete").isBoolean()) return null;
                var paths = new java.util.HashSet<String>();
                for (var value : row.path("paths")) {
                    String file = value.isTextual() ? value.asText() : "";
                    if (file.isBlank() || file.startsWith("/") || file.matches("^[A-Za-z]:.*")
                            || file.contains("\\") || file.contains("..") || file.matches(".*[?*\\[\\]{}].*")
                            || !paths.add(file)) return null;
                }
            }
            return root;
        } catch (Exception invalidMetadata) {
            return null;
        }
    }

    @Test void deploymentGuardProtectsProseWithoutRewritingValidTechnicalControlIdentities() {
        String base = "a".repeat(40);
        String valid = "```yaml\ntestChanges:\n  - id: CHANGE-J4\n    status: ACTIVE\n    base: " + base
                + "\n    expires: 2030-01-01\n    reason: Clarifică proprietatea fără schimbarea verdictului."
                + "\n    proof: 'mvn -pl atf-tests test'\n    paths:\n      - src/test/java/ExampleTest.java\n```\n";
        AuditedAssertions.assertFalse(deploymentProse(valid).contains("CHANGE-J4"));
        AuditedAssertions.assertTrue(valid.contains("id: CHANGE-J4"), "The original control identity is unchanged");
        AuditedAssertions.assertTrue(deploymentProse("Context J4.\n" + valid).contains("Context J4."));
        AuditedAssertions.assertTrue(deploymentProse(valid.replace("base: " + base, "base: main")).contains("CHANGE-J4"));
        AuditedAssertions.assertTrue(deploymentProse(valid.replace("testChanges:", "settings:")).contains("CHANGE-J4"));
        AuditedAssertions.assertTrue(deploymentProse(valid.replace("paths:\n      - src/test/java/ExampleTest.java", "paths: []")).contains("CHANGE-J4"));
        AuditedAssertions.assertTrue(deploymentProse(valid.replace("schimbarea verdictului.", "schimbarea verdictului. J8")).contains("J8"),
                "Other metadata fields remain subject to the editorial identifier rule");
    }

    static boolean hasEnglishReadmeProse(String source) {
        String prose = source.replaceAll("(?s)```.*?```", "").replaceAll("`[^`]*`", "").replaceAll("\\]\\([^)]*\\)", "]");
        if (java.util.regex.Pattern.compile("(?im)^#{1,6} (?:Overview|Quick start|Getting started|Captured UI timing|Usage|Testing|Architecture|Requirements|Shared platform)\\b")
                .matcher(prose).find()) return true;
        var words = java.util.regex.Pattern.compile("(?i)\\b(the|this|these|that|with|without|and|must|should|their|each|after|before|from|where|when|which|remain|remains)\\b");
        for (String paragraph : prose.split("\\n\\s*\\n")) {
            long signals = words.matcher(paragraph).results().map(match -> match.group().toLowerCase(java.util.Locale.ROOT)).distinct().count();
            if (signals >= 4) return true;
        }
        return false;
    }

    @Test void languageGuardRejectsEnglishProseButKeepsQuotedApiAndCommands() {
        AuditedAssertions.assertTrue(hasEnglishReadmeProse("# Overview\n\nThis module must preserve the source and their contracts before publication."));
        AuditedAssertions.assertTrue(hasEnglishReadmeProse("# Mecanism\n\nThis component must preserve the source and their contracts before publication."));
        AuditedAssertions.assertFalse(hasEnglishReadmeProse("# Mecanism\n\nRegulile păstrează contractul. API-ul `with and before after must` își păstrează numele.\n\n```text\nThis and that must remain before the source.\n```"));
    }

    @Test void copiedValidatorsDetectInjectedDefectsAndAcceptAHealthyNeighbour(@TempDir Path root) throws Exception {
        Path broken = root.resolve("README.md");
        Files.writeString(broken, "# Control\n\n## Pe scurt\n\n[Lipsă](missing.md)\n\n```mermaid\nflowchart LR\nA --> B\nB --> C\n```\n");
        var failures = validate(broken, root);
        var codes = failures.stream().map(DocumentationGovernanceSupport.Violation::code).toList();
        AuditedAssertions.assertTrue(codes.contains("DOC-README-LINK"));
        AuditedAssertions.assertTrue(codes.contains("DOC-DIAG-EDGE"));
        AuditedAssertions.assertTrue(codes.contains("DOC-DIAG-LEGEND"));
        Path healthy = root.resolve("healthy/README.md");
        Files.createDirectories(healthy.getParent());
        Files.writeString(healthy, "# Control sănătos\n\n## Pe scurt\n\nUn contract stabil.\n\n[Acest document](README.md)\n");
        AuditedAssertions.assertTrue(validate(healthy, root).isEmpty(), "A healthy neighbouring document must remain valid");
    }

    @Test void generatedDirectoriesAreExcludedButNestedReadmesAndDocsRemainVisible(@TempDir Path root) throws Exception {
        for (String file : List.of("README.md", "nested/README.md", "docs/guide.md", "nested/build/README.md",
                "target/README.md", "node_modules/pkg/README.md", "dist/README.md", "unrelated/example.md")) {
            Path path = root.resolve(file); Files.createDirectories(path.getParent()); Files.writeString(path, "# Control\n");
        }
        AuditedAssertions.assertEquals(List.of("README.md", "docs/guide.md", "nested/README.md"), GovernanceProject.markdown(root).stream()
                .map(file -> GovernanceProject.relative(root, file)).sorted().toList());
    }

    private static List<DocumentationGovernanceSupport.Violation> validate(Path file, Path root) {
        var failures = new ArrayList<DocumentationGovernanceSupport.Violation>();
        DocumentationReadmeStructureGovernanceTest.validate(file, root, failures);
        DocumentationDiagramGovernanceTest.validate(file, root, failures);
        DocumentationStyleGovernanceTest.validate(file, root, failures);
        DocumentationProseGovernanceTest.validate(file, root, failures);
        DynamicPathDocumentationArchitectureTest.validatePaths(file, root, failures);
        DynamicPathDocumentationArchitectureTest.validateGeneratedReportNames(file, root, failures);
        DocumentationLinkGovernanceTest.validate(file, root, failures);
        return failures;
    }
}
