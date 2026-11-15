package atf.tests.documentation.cli;

import atf.tests.documentation.DocumentationGovernance;
import atf.tests.documentation.DocumentationGovernanceSupport.Violation;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** UTF-8 command line for the project's editorial diagnostics. */
public final class DocumentationGovernanceCli {
    private static final String USAGE = "Use: DocumentationGovernanceCli --root <project> [--file <relative-document>]... [--json]";

    public static void main(String[] args) {
        var out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        var errors = new PrintStream(System.err, true, StandardCharsets.UTF_8);
        try {
            // The Windows launcher can lose non-ASCII arguments before they reach Java.
            String[] arguments = args.length == 1 && args[0].equals("--stdin")
                    ? new ObjectMapper().readValue(System.in, String[].class) : args;
            if (arguments == null || java.util.Arrays.stream(arguments).anyMatch(java.util.Objects::isNull))
                throw new IOException("expected a JSON array of argument strings");
            System.exit(run(arguments, out, errors));
        } catch (IOException invalid) {
            errors.println("DOC-ARGUMENTS: " + invalid.getMessage());
            System.exit(1);
        }
    }

    public static int run(String[] args, PrintStream out, PrintStream errors) {
        if (args.length == 1 && args[0].equals("--help")) {
            out.println(USAGE);
            return 0;
        }
        try {
            Request request = parse(args);
            Path root = request.root().toRealPath();
            if (!Files.isRegularFile(root.resolve("pom.xml"))
                    || !Files.isRegularFile(root.resolve("config/atf-governance-policy.json")))
                throw new IllegalArgumentException("DOC-ROOT: expected an explicit project with pom.xml and governance policy");
            var result = DocumentationGovernance.scan(root, request.files());
            boolean passed = result.violations().isEmpty();
            String status = passed ? "PASS" : "BLOCKED";
            if (request.json()) {
                var diagnostics = result.violations().stream().map(Diagnostic::from).toList();
                out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(
                        Map.of("schemaVersion", 1, "status", status, "files", result.files(), "diagnostics", diagnostics)));
            } else {
                out.printf("%s | documents=%d | diagnostics=%d%n", status, result.files().size(), result.violations().size());
                result.violations().forEach(out::println);
            }
            return passed ? 0 : 2;
        } catch (IOException | IllegalArgumentException invalid) {
            errors.println(invalid.getMessage());
            return 1;
        }
    }

    private static Request parse(String[] args) {
        Path root = null;
        List<String> files = new ArrayList<>();
        boolean json = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--root" -> {
                    if (root != null || i + 1 >= args.length || args[i + 1].startsWith("--") || args[i + 1].isBlank())
                        throw new IllegalArgumentException(USAGE);
                    root = Path.of(args[++i]);
                }
                case "--file" -> {
                    if (i + 1 >= args.length || args[i + 1].startsWith("--")) throw new IllegalArgumentException(USAGE);
                    files.add(args[++i]);
                }
                case "--json" -> {
                    if (json) throw new IllegalArgumentException(USAGE);
                    json = true;
                }
                default -> throw new IllegalArgumentException(USAGE);
            }
        }
        if (root == null) throw new IllegalArgumentException(USAGE);
        return new Request(root, files, json);
    }

    private record Request(Path root, List<String> files, boolean json) { }

    private record Diagnostic(String code, String file, int line, String finding, String rule, String fix) {
        private static Diagnostic from(Violation violation) {
            return new Diagnostic(violation.code(), violation.file().toString().replace('\\', '/'), violation.line(),
                    violation.finding(), violation.rule(), violation.fix());
        }
    }

    private DocumentationGovernanceCli() { }
}
