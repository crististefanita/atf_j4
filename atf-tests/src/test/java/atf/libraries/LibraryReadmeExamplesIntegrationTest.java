package atf.libraries;

import atf.testkit.AtfIntegrationTest;
import atf.tests.governance.support.GovernanceProject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Compiles the delivered guide text and executes it on the physical consumer JARs. */
@AtfIntegrationTest
class LibraryReadmeExamplesIntegrationTest {
    @TempDir Path temporary;

    @Test void coreExampleBuildsTheDeclaredUrlAndExactDecimalAndRejectsInvalidJson() throws Exception {
        String checks = "if (!url.toString().equals(\"https://example.test/api/orders/42\")) throw new AssertionError(url);"
                + "if (total.compareTo(new java.math.BigDecimal(\"19.90\")) != 0) throw new AssertionError(total);"
                + "for (String invalid : java.util.List.of(\"{\\\"x\\\":1,\\\"x\\\":2}\", \"{} trailing\")) {"
                + "boolean rejected=false; try { JsonDocuments.read(invalid); } catch (Exception expected) { rejected=true; }"
                + "if (!rejected) throw new AssertionError(\"Invalid JSON accepted\"); }";
        assertEquals(0, execute("Core", checks).exit(), "Core guide must compile and execute its local-data contract");
    }

    @Test void consoleExampleWritesItsMessageAndDisabledConsoleWritesNothing() throws Exception {
        var result = execute("Console", "new AtfConsoleReporter(System.out, ConsoleSettings.disabled()).info(\"DISABLED_SENTINEL\");");
        assertEquals(0, result.exit(), result.error());
        assertTrue(result.output().contains("Catalogul de scenarii a fost încărcat"), result.output());
        assertFalse(result.output().contains("DISABLED_SENTINEL"), result.output());
    }

    @Test void mockExampleReadsTheDocumentedYamlAndUsesTheDeclaredFallback() throws Exception {
        String yaml = fence(guide("Mock"), "yaml");
        Files.writeString(temporary.resolve("mock.yaml"), yaml);
        var result = execute("Mock", "if (response.status()!=200) throw new AssertionError(response.status());"
                + "var missing=mock.send(ApiRequest.of(\"GET\",\"/missing-readme-contract\"),"
                + "\"https://offline.test/missing-readme-contract\",\"none\").join();"
                + "if (missing.status()!=500) throw new AssertionError(missing.status());");
        assertEquals(0, result.exit(), result.error());
    }

    @Test void reportingExamplePublishesBeforePropagatingItsFailedComparison() throws Exception {
        Map<Path, String> before = identity();
        compile("Reporting", "", false);
        Path passed = temporary.resolve("passed.html"), failed = temporary.resolve("failed.html");
        assertEquals(0, run(passed.toString(), "ready").exit());
        assertTrue(Files.isRegularFile(passed));
        var rejection = run(failed.toString(), "NULL");
        assertNotEquals(0, rejection.exit());
        assertTrue(rejection.error().contains("State differs; inspect the report"), rejection.error());
        assertTrue(Files.isRegularFile(failed), "Failure must not discard the closed report");
        assertEquals(before, identity(), "Reporting acceptance must preserve guide and JAR identity");
    }

    @Test void anUnknownApiMethodCannotPassTheCompilationControl() throws Exception {
        assertFalse(compile("Core", "", true), "A missing method must produce a compiler failure");
    }

    private Result execute(String library, String checks) throws Exception {
        Map<Path, String> before = identity();
        assertTrue(compile(library, checks, false));
        Result result = run();
        assertEquals(before, identity(), "Guides and physical JARs must remain stable during acceptance");
        return result;
    }

    private boolean compile(String library, String checks, boolean corrupt) throws Exception {
        String fragment = fence(guide(library), "java");
        StringBuilder imports = new StringBuilder(), body = new StringBuilder();
        fragment.lines().forEach(line -> {
            if (line.strip().startsWith("import ")) imports.append(line).append('\n');
            else body.append(line).append('\n');
        });
        String outputCodec = "System.setOut(new java.io.PrintStream(System.out,true,java.nio.charset.StandardCharsets.UTF_8));"
                + "System.setErr(new java.io.PrintStream(System.err,true,java.nio.charset.StandardCharsets.UTF_8));";
        String code = library.equals("Reporting") ? body + "\npublic static void main(String[] a) {" + outputCodec
                + "publish(java.nio.file.Path.of(a[0]), a[1].equals(\"NULL\")?null:a[1]);}"
                : "public static void main(String[] a) throws Exception {" + outputCodec + body + checks + "}";
        code = imports + "\npublic final class ReadmeProbe {\n" + code + "\n}";
        if (corrupt) code = code.replace("JsonDocuments.read", "JsonDocuments.missingReadmeMethod");
        Path source = temporary.resolve("ReadmeProbe.java"); Files.writeString(source, code);
        var compiler = ToolProvider.getSystemJavaCompiler(); assertNotNull(compiler, "JDK 21 is required");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var manager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            boolean success = compiler.getTask(null, manager, diagnostics,
                    List.of("-proc:none", "--release", "21", "-classpath", classpath(), "-d", temporary.toString()),
                    null, manager.getJavaFileObjects(source)).call();
            if (!corrupt) assertTrue(success, diagnostics.getDiagnostics().toString());
            return success;
        }
    }

    private Result run(String... arguments) throws Exception {
        Path stdout = temporary.resolve("stdout.txt"), stderr = temporary.resolve("stderr.txt");
        var command = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Xmx128m", "-Dfile.encoding=UTF-8", "-cp", temporary + java.io.File.pathSeparator + classpath(), "ReadmeProbe"));
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).directory(temporary.toFile())
                .redirectOutput(stdout.toFile()).redirectError(stderr.toFile()).start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) { process.destroyForcibly(); fail("Guide subprocess exceeded its deadline"); }
        return new Result(process.exitValue(), Files.readString(stdout), Files.readString(stderr));
    }

    private static Path root() { return GovernanceProject.locateProjectRoot(); }
    private static String guide(String name) throws Exception { return Files.readString(root().resolve("lib/README." + name + ".md")); }
    private static String classpath() throws Exception {
        PlatformLibrarySurface.assertFlatLayout(root());
        String inherited = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        String dependencies = java.util.Arrays.stream(inherited.split(Pattern.quote(java.io.File.pathSeparator)))
                .filter(entry -> !Path.of(entry).getFileName().toString()
                        .matches("atf-(core|reporting|console|mock)-.*\\.jar"))
                .collect(java.util.stream.Collectors.joining(java.io.File.pathSeparator));
        try (var files = Files.list(root().resolve("lib"))) {
            return files.filter(path -> path.toString().endsWith(".jar")).sorted().map(Path::toString)
                    .collect(java.util.stream.Collectors.joining(java.io.File.pathSeparator))
                    + java.io.File.pathSeparator + dependencies;
        }
    }
    private static String fence(String text, String language) {
        var match = Pattern.compile("(?s)```" + language + "\\R(.*?)\\R```").matcher(text);
        assertTrue(match.find(), "Missing " + language + " guide fragment"); return match.group(1);
    }
    private static Map<Path, String> identity() throws Exception {
        var result = new java.util.LinkedHashMap<Path, String>();
        try (var files = Files.list(root().resolve("lib"))) {
            for (Path file : files.filter(Files::isRegularFile).toList())
                result.put(file, java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))));
        }
        return result;
    }
    private record Result(int exit, String output, String error) { }
}
