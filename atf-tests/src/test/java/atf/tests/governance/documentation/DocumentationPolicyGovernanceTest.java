package atf.tests.governance.documentation;

import atf.testkit.assertions.AuditedAssertions;

import atf.testkit.AtfComponentTest;
import atf.tests.documentation.DocumentationGovernanceSupport;
import atf.tests.documentation.DocumentationGovernance;
import atf.tests.governance.documentation.support.DocumentationAssertions;
import atf.tests.governance.support.GovernanceProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static atf.tests.documentation.policy.DocumentationContentPolicy.deploymentProse;
import static atf.tests.documentation.policy.DocumentationContentPolicy.hasEnglishReadmeProse;
import static atf.tests.documentation.policy.DocumentationContentPolicy.historicalReleaseProse;
import static atf.tests.documentation.policy.DocumentationContentPolicy.publicReleaseProse;

@AtfComponentTest
class DocumentationPolicyGovernanceTest {
    @Test void theOwnPagesDestinationIsPublicationDataAndExternalPagesStillCountAsProse() {
        String repository = "https://github.com/owner/atf_j4";
        String pages = "https://owner.github.io/atf_j4/";
        AuditedAssertions.assertEquals("[Joacă](<resursă-publică>)",
                publicReleaseProse("[Joacă](" + pages + ")", repository));
        AuditedAssertions.assertEquals("[atf_j4](<resursă-publică>)",
                publicReleaseProse("[atf_j4](" + pages + ")", repository));
        for (String url : List.of("https://other.github.io/atf_j4/", "https://owner.github.io/atf_j5/",
                pages + "other", pages + "?preview=1")) {
            String source = "[Joacă](" + url + ")";
            AuditedAssertions.assertEquals(source, publicReleaseProse(source, repository));
        }
    }

    @Test void theInventoryContainsAllModuleReadmesAndNoPerReleaseVerificationNotes() throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        var files = GovernanceProject.markdown(root);
        AuditedAssertions.assertTrue(files.contains(root.resolve("README.md")));
        var modules = GovernanceProject.policy(root).required("mavenModules");
        for (var module : modules) AuditedAssertions.assertTrue(files.contains(root.resolve(module.asText()).resolve("README.md")),
                () -> "Missing governed module README: " + module);
        var result = DocumentationGovernance.scanProject(root, DocumentationGovernance.Rule.CONTENT);
        DocumentationAssertions.assertNoViolations("local documentation content", result.violations());
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
        var result = DocumentationGovernance.scanFile(GovernanceProject.locateProjectRoot(), "docs/feedback/README.md", DocumentationGovernance.Rule.FEEDBACK_PRIORITIES);
        DocumentationAssertions.assertNoViolations("prioritized feedback", result.violations());
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

    @Test void languageGuardRejectsEnglishProseButKeepsQuotedApiAndCommands() {
        AuditedAssertions.assertTrue(hasEnglishReadmeProse("# Overview\n\nThis module must preserve the source and their contracts before publication."));
        AuditedAssertions.assertTrue(hasEnglishReadmeProse("# Mecanism\n\nThis component must preserve the source and their contracts before publication."));
        AuditedAssertions.assertFalse(hasEnglishReadmeProse("# Mecanism\n\nRegulile păstrează contractul. API-ul `with and before after must` își păstrează numele.\n\n```text\nThis and that must remain before the source.\n```"));
    }

    @Test void copiedValidatorsDetectInjectedDefectsAndAcceptAHealthyNeighbour(@TempDir Path root) throws Exception {
        Files.writeString(Files.createDirectories(root.resolve("config")).resolve("atf-governance-policy.json"), "{}");
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

    private static List<DocumentationGovernanceSupport.Violation> validate(Path file, Path root) throws Exception {
        return DocumentationGovernance.scan(root, List.of(root.relativize(file).toString())).violations();
    }
}
