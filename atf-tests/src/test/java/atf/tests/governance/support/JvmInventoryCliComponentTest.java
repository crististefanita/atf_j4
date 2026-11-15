package atf.tests.governance.support;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.jvm.JvmArchitecture;
import atf.tests.jvm.cli.JvmInventoryCli;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

@AtfComponentTest
class JvmInventoryCliComponentTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String POLICY = "config/atf-governance-policy.json";
    private static final String SOURCE = "maven/src/main/java/example/Card.java";
    private static final String TEST = "android/src/androidTest/java/example/CardTest.java";

    @Test void externalCheckoutReportsLiteralMavenAndExplicitGradleSets(@TempDir Path base) throws Exception {
        Path root = base.resolve("copie externă cu spații");
        var policy = checkout(root);
        write(root, "maven/pom.xml", "<project><build><testSourceDirectory>checks</testSourceDirectory></build></project>");
        write(root, "maven/checks/example/ExampleTest.java", "package example; class ExampleTest {}");
        write(root, "target/old/pom.xml", "this file belongs to a generated tree");
        write(root, "maven/src/main/java/example/target/Real.java", "package example.target; class Real {}");
        save(root, policy);

        var result = invoke(root);
        AuditedAssertions.assertEquals(0, result.exit(), result.error());
        JsonNode report = JSON.readTree(result.output());
        AuditedAssertions.assertEquals(JSON.readTree("[\"android\",\"maven\"]"), report.get("modules"));
        AuditedAssertions.assertEquals(JSON.readTree("""
                [
                  {"module":"android","set":"test","root":"android/src/androidTest/java","files":1},
                  {"module":"maven","set":"test","root":"maven/checks","files":1},
                  {"module":"maven","set":"main","root":"maven/src/main/java","files":2}
                ]
                """), report.get("sourceSets"));
        AuditedAssertions.assertEquals(4, report.get("files").intValue());
        AuditedAssertions.assertEquals(4, report.get("packages").intValue());
        AuditedAssertions.assertEquals(result.output(), invoke(root).output());
    }

    @Test void rejectsMissingAndEscapingRootsAndMissingModule(@TempDir Path root) throws Exception {
        var policy = checkout(root);
        write(root, "maven/pom.xml", "<project><build><sourceDirectory>absent</sourceDirectory></build></project>");
        assertInvalid(root, "GOV-SOURCE-ROOT");
        write(root, "maven/pom.xml", "<project/>");
        ((ObjectNode) policy.get("additionalSourceRoots").get(0)).put("path", "android/../maven");
        save(root, policy);
        assertInvalid(root, "GOV-SOURCE-ROOT");

        checkout(root);
        write(root, "pom.xml", "<project/>");
        assertInvalid(root, "GOV-SOURCE-MODULES");
        write(root, "pom.xml", "<project><modules><module>maven</module></modules></project>");
        write(root, "forgotten/pom.xml", "<project/>");
        assertInvalid(root, "GOV-SOURCE-MODULES");
    }

    @Test void rejectsOverlappingRootsBeforeAnyJavaFileCanBeAssignedTwice(@TempDir Path root) throws Exception {
        var policy = checkout(root);
        for (String path : List.of("android/empty", "android/empty-sibling", "android/empty/nested")) {
            Files.createDirectories(root.resolve(path));
            policy.withArray("additionalSourceRoots").addObject().put("module", "android").put("path", path).put("test", true);
        }
        save(root, policy);
        assertInvalid(root, "GOV-SOURCE-OVERLAP");
    }

    @Test void localDiagnosticsAndCliMatchAndOnlyExactCurrentAdaptersAreAccepted(@TempDir Path root) throws Exception {
        var policy = checkout(root);
        write(root, SOURCE, "package example;\nimport org.junit.Test;\nclass Card {}");
        assertRules(root, List.of(SOURCE + ": GOV-BOUNDARY: forbidden org.junit"));
        var adapters = (ObjectNode) policy.get("productionBoundaryAdapters");
        var exception = adapters.putObject(SOURCE).put("reason", "reviewed bridge");
        exception.putArray("allows").add("org.junit");
        save(root, policy);
        AuditedAssertions.assertEquals(0, invoke(root).exit());

        exception.withArray("allows").add("java.util");
        adapters.set(TEST, exception.deepCopy());
        save(root, policy);
        var report = JSON.readTree(invoke(root).output());
        AuditedAssertions.assertEquals("BLOCKED", report.get("status").textValue());
        AuditedAssertions.assertEquals(3, report.get("diagnostics").size());
        AuditedAssertions.assertTrue(report.get("diagnostics").toString().contains("GOV-BOUNDARY-ADAPTER"));
        AuditedAssertions.assertTrue(report.get("diagnostics").toString().contains("GOV-BOUNDARY-STALE"));

        adapters.remove(TEST);
        exception.putArray("allows").add("org.junit");
        save(root, policy);
        write(root, SOURCE, "package wrong;\n// org.junit.Test\nclass Card {}");
        assertRules(root, List.of(SOURCE + ": GOV-BOUNDARY-ADAPTER: expected a reason and only observed forbidden namespaces",
                SOURCE + ": GOV-PACKAGE-PATH: expected example"));
    }

    @Test void cohesionExceptionsRequireAnExactOversizedInventory(@TempDir Path root) throws Exception {
        var policy = checkout(root);
        var files = IntStream.range(1, 9).mapToObj(index -> "Extra" + index + ".java").toList();
        for (String file : files) write(root, "maven/src/main/java/example/" + file, "package example; class Extra {}");
        var review = ((ObjectNode) policy.get("reviewedPackages")).putObject("maven:main:example");
        review.put("reason", "reviewed change").put("action", "split by responsibility");
        var members = review.putArray("files").add("Card.java");
        files.forEach(members::add);
        save(root, policy);
        var valid = invoke(root);
        AuditedAssertions.assertEquals(0, valid.exit(), valid.error());
        AuditedAssertions.assertEquals(policy.get("reviewedPackages"),
                JSON.readTree(valid.output()).get("exceptions").get("reviewedPackages"));
        members.add("Card.java");
        save(root, policy);
        AuditedAssertions.assertTrue(JSON.readTree(invoke(root).output()).get("diagnostics").get(0).asText().startsWith("GOV-COHESION:"));
        Files.delete(root.resolve(SOURCE));
        assertRules(root, List.of("GOV-COHESION-STALE: maven:main:example"));
    }

    private static ObjectNode checkout(Path root) throws IOException {
        write(root, "pom.xml", "<project><modules><module>maven</module></modules></project>");
        write(root, "maven/pom.xml", "<project/>");
        write(root, SOURCE, "package example; class Card {}");
        write(root, TEST, "package example; class CardTest {}");
        var policy = (ObjectNode) JSON.readTree("""
                {
                  "mavenModules":["maven"],
                  "additionalSourceRoots":[{"module":"android","path":"android/src/androidTest/java","test":true}],
                  "packageLimit":8,
                  "productionForbiddenNamespaces":{"maven":["org.junit"]},
                  "productionBoundaryAdapters":{},
                  "reviewedPackages":{}
                }
                """);
        save(root, policy);
        return policy;
    }

    private static void assertInvalid(Path root, String diagnostic) {
        var result = invoke(root);
        AuditedAssertions.assertEquals(1, result.exit(), result.error());
        AuditedAssertions.assertTrue(result.output().isEmpty());
        AuditedAssertions.assertTrue(result.error().contains(diagnostic), result.error());
    }

    private static void assertRules(Path root, List<String> diagnostics) throws IOException {
        var result = invoke(root);
        AuditedAssertions.assertEquals(2, result.exit(), result.error());
        var parsed = JSON.readTree(result.output());
        AuditedAssertions.assertEquals(JSON.valueToTree(diagnostics), parsed.get("diagnostics"));
        AuditedAssertions.assertEquals(JvmArchitecture.inspect(root).diagnostics(), diagnostics);
    }

    private static void save(Path root, ObjectNode policy) throws IOException {
        write(root, POLICY, JSON.writeValueAsString(policy));
    }

    private static void write(Path root, String path, String content) throws IOException {
        Path file = root.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private static Invocation invoke(Path root) {
        var output = new ByteArrayOutputStream();
        var error = new ByteArrayOutputStream();
        try (var out = new PrintStream(output, true, StandardCharsets.UTF_8);
             var errors = new PrintStream(error, true, StandardCharsets.UTF_8)) {
            int exit = JvmInventoryCli.run(new String[]{"--root", root.toString()}, out, errors);
            return new Invocation(exit, output.toString(StandardCharsets.UTF_8), error.toString(StandardCharsets.UTF_8));
        }
    }

    private record Invocation(int exit, String output, String error) { }
}
