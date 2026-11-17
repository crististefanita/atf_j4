package atf.tests.governance.documentation.cli;

import atf.testkit.AtfComponentTest;
import atf.tests.documentation.cli.DocumentationGovernanceCli;
import atf.tests.documentation.DocumentationGovernance;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest
class DocumentationGovernanceCliComponentTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String BROKEN = "docs/Reguli și dovezi.md";
    @TempDir Path temporary;

    @ParameterizedTest
    @CsvSource({"en,Overview,Pe scurt", "ro,Pe scurt,Overview"})
    void localizedHeadingFailuresHaveTheSameFullAndSelectedDiagnostics(
            String language, String expected, String wrong) throws Exception {
        Path root = project();
        write(root, "config/atf-governance-policy.json", "{\"documentationLanguage\":\"" + language + "\"}");
        write(root, "README.md", "# Contract\n\n## " + expected + "\n\nContract.\n");
        String file = "module/README.md";
        write(root, file, "# Module\n\n## " + wrong + "\n\nContract.\n");
        Command full = run(root);
        Command selected = run(root, "--file", file);
        assertEquals(2, full.code());
        assertEquals(2, selected.code());
        var findings = values(selected.json().path("diagnostics"));
        assertEquals(values(full.json().path("diagnostics")).stream()
                .filter(row -> file.equals(row.path("file").asText())).toList(), findings);
        JsonNode structure = findings.stream().filter(row -> row.path("code").asText()
                .equals("DOC-README-OVERALL")).findFirst().orElseThrow();
        assertTrue(structure.path("fix").asText().contains("## " + expected));
    }

    @Test void aSelectedDocumentHasExactlyTheFullScanDiagnosticsForThatFile() throws Exception {
        Path root = project();
        Path broken = write(root, BROKEN, """
                # Control
                ## Pe scurt

                [Secțiune](../README.md#absentă)

                Prima propoziție. A doua propoziție. A treia propoziție. A patra propoziție.

                <span style="color:red">Text</span>

                C:\\local\\dovezi
                ui-atf-extent-report-2026-11-15.html

                ```mermaid
                flowchart LR
                A --> B
                B --> C
                ```
                """);
        byte[] original = Files.readAllBytes(broken);
        Command full = run(root);
        Command selected = run(root, "--file", BROKEN.replace('/', '\\'));
        assertEquals(2, full.code());
        assertEquals(2, selected.code());
        assertEquals(List.of(BROKEN), values(selected.json().path("files")).stream().map(JsonNode::asText).toList());
        var filtered = values(full.json().path("diagnostics")).stream()
                .filter(row -> BROKEN.equals(row.path("file").asText())).toList();
        var reported = values(selected.json().path("diagnostics"));
        assertEquals(filtered, reported);
        var codes = reported.stream().map(row -> row.path("code").asText()).toList();
        for (String code : List.of("DOC-README-ANCHOR", "DOC-PROSE-DENSE", "DOC-STYLE-PRESENTATION",
                "DOC-PATH-ABSOLUTE", "DOC-ARTIFACT-NAME", "DOC-DIAG-EDGE", "DOC-DIAG-LEGEND")) {
            assertTrue(codes.contains(code), codes.toString());
        }
        var suite = DocumentationGovernance.scanProject(root, DocumentationGovernance.Rule.LINKS).violations();
        assertEquals(suite.getFirst().line(), reported.stream().filter(row -> row.path("code").asText().equals("DOC-README-ANCHOR"))
                .findFirst().orElseThrow().path("line").asInt());
        assertArrayEquals(original, Files.readAllBytes(broken));
    }

    @Test void acceptsSeveralExplicitFilesAndExcludesAnUnselectedDefect() throws Exception {
        Path root = project();
        write(root, "lib/README.Core.md", "# Bibliotecă\n\n## Pe scurt\n\nContractul rămâne local.\n");
        write(root, BROKEN, "# Document\n\n## Pe scurt\n\n[Greșit](lipsă.md)\n");
        Command selected = run(root, "--file", "lib/README.Core.md", "--file", "README.md", "--file", "README.md");
        assertEquals(0, selected.code());
        assertEquals("PASS", selected.json().path("status").asText());
        assertEquals(List.of("README.md", "lib/README.Core.md"),
                values(selected.json().path("files")).stream().map(JsonNode::asText).toList());
        assertEquals(2, run(root).code());
    }

    @Test void rejectsWrongRootsAndMissingOrOutOfScopeFiles() throws Exception {
        Path root = project();
        write(root, "target/README.md", "# Ieșire\n");
        write(root, "other/fragment.md", "# Fragment\n");
        for (String input : List.of("../README.md", "docs/../README.md", "docs//note.md",
                "C:\\outside\\README.md", root.resolve("README.md").toString(), "missing.md")) {
            Command result = run(root, "--file", input);
            assertEquals(1, result.code(), input);
            assertTrue(result.errors().contains("DOC-TARGET-PATH"), result.errors());
            assertTrue(result.out().isEmpty(), result.out());
        }
        for (String input : List.of("target/README.md", "other/fragment.md")) {
            Command result = run(root, "--file", input);
            assertEquals(1, result.code(), input);
            assertTrue(result.errors().contains("DOC-TARGET-SCOPE"), result.errors());
        }
        assertTrue(run(temporary).errors().contains("DOC-ROOT"));
        assertTrue(run(root, "--file").errors().contains("Use:"));
        assertTrue(run(fixtureRoot("proiect gol")).errors().contains("DOC-INVENTORY-EMPTY"));
    }

    @Test void usesTheSameContentAndFeedbackPoliciesOnExplicitFiles() throws Exception {
        Path root = project();
        write(root, "module/README.md", "# Usage\n\nThis module must preserve the source and their contracts before publication.\n");
        write(root, "docs/feedback/README.md", """
                # Feedback
                ## Pe scurt
                ## ToDo-uri prioritizate

                | ID | Prioritate | Scor | Criteriu |
                |---|---|---|---|
                | ABC-01 | P1 | 99 | [Document](../../README.md) |
                | ABC-01 | P1 | 100 | [Exterior](https://example.invalid) |
                """);
        var full = values(run(root).json().path("diagnostics"));
        for (String file : List.of("module/README.md", "docs/feedback/README.md")) {
            Command selection = run(root, "--file", file);
            assertEquals(2, selection.code());
            var findings = values(selection.json().path("diagnostics"));
            assertEquals(full.stream().filter(row -> file.equals(row.path("file").asText())).toList(), findings);
            String rule = file.startsWith("module") ? "DOC-README-LANGUAGE" : "DOC-FEEDBACK-INDEX";
            assertTrue(findings.stream().anyMatch(row -> row.path("code").asText().equals(rule)));
        }
    }

    @Test void theStandaloneEntrypointPreservesUnicodePathsFromStdin() throws Exception {
        Path root = project();
        write(root, BROKEN, "# Reguli\n\n## Pe scurt\n\nDocumentul păstrează diacriticele.\n");
        Path java = Path.of(System.getProperty("java.home"), "bin", "java");
        Path stdout = temporary.resolve("cli-output.json");
        Path stderr = temporary.resolve("cli-error.log");
        Process process = new ProcessBuilder(java.toString(), "-cp", System.getProperty("java.class.path"),
                DocumentationGovernanceCli.class.getName(), "--stdin")
                .redirectOutput(stdout.toFile()).redirectError(stderr.toFile()).start();
        try {
            try (var input = process.getOutputStream()) {
                input.write(JSON.writeValueAsBytes(List.of("--root", root.toString(), "--file", BROKEN, "--json")));
            }
            assertTrue(process.waitFor(20, TimeUnit.SECONDS), "CLI subprocess exceeded its deadline");
            assertEquals(0, process.exitValue(), Files.readString(stderr));
            var result = JSON.readTree(stdout.toFile());
            assertEquals(List.of(BROKEN), values(result.path("files")).stream().map(JsonNode::asText).toList());
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }

    private Path project() throws Exception {
        Path root = fixtureRoot("proiect cu spații");
        write(root, "README.md", "# Proiect\n\n## Pe scurt\n\nVerificarea citește reguli locale.\n");
        return root;
    }

    private Path fixtureRoot(String name) throws Exception {
        Path root = Files.createDirectory(temporary.resolve(name));
        write(root, "pom.xml", "<project/>");
        write(root, "config/atf-governance-policy.json", "{}");
        return root;
    }

    private static Path write(Path root, String relative, String content) throws Exception {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        return Files.writeString(file, content);
    }

    private static List<JsonNode> values(JsonNode array) {
        var rows = new ArrayList<JsonNode>();
        array.forEach(rows::add);
        return rows;
    }

    private static Command run(Path root, String... arguments) {
        var out = new ByteArrayOutputStream();
        var errors = new ByteArrayOutputStream();
        var args = new ArrayList<>(List.of("--root", root.toString(), "--json"));
        args.addAll(List.of(arguments));
        int code = DocumentationGovernanceCli.run(args.toArray(String[]::new),
                new PrintStream(out, true, StandardCharsets.UTF_8), new PrintStream(errors, true, StandardCharsets.UTF_8));
        return new Command(code, out.toString(StandardCharsets.UTF_8), errors.toString(StandardCharsets.UTF_8));
    }

    private record Command(int code, String out, String errors) {
        JsonNode json() throws Exception { return JSON.readTree(out); }
    }
}
