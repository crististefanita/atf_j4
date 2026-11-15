package atf.tests.jvm.cli;

import atf.tests.jvm.JvmArchitecture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/** A read-only architecture inventory for an explicitly selected checkout. */
public final class JvmInventoryCli {
    private static final String USAGE = "Use: JvmInventoryCli --root <checkout>";

    private JvmInventoryCli() { }

    public static void main(String[] args) {
        var out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        var errors = new PrintStream(System.err, true, StandardCharsets.UTF_8);
        try {
            // PowerShell supplies escaped JSON to preserve non-ASCII paths on Windows.
            String[] arguments = args.length == 1 && args[0].equals("--stdin")
                    ? new ObjectMapper().readValue(System.in, String[].class) : args;
            System.exit(run(arguments, out, errors));
        } catch (IOException invalid) {
            errors.println("GOV-SOURCE-ARGUMENTS: " + invalid.getMessage());
            System.exit(1);
        }
    }

    public static int run(String[] args, PrintStream out, PrintStream errors) {
        if (args != null && args.length == 1 && "--help".equals(args[0])) {
            out.println(USAGE);
            return 0;
        }
        if (args == null || args.length != 2 || !"--root".equals(args[0])
                || args[1] == null || args[1].isBlank() || args[1].startsWith("--")) {
            errors.println("GOV-SOURCE-ARGUMENTS: " + USAGE);
            return 1;
        }
        try {
            var inventory = JvmArchitecture.inspect(Path.of(args[1]));
            boolean passed = inventory.diagnostics().isEmpty();
            var result = new Result(1, passed ? "PASS" : "BLOCKED", inventory.modules(), inventory.sourceSets(),
                    inventory.files(), inventory.packages(), inventory.exceptions(), inventory.diagnostics());
            out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result));
            return passed ? 0 : 2;
        } catch (IOException | IllegalArgumentException invalid) {
            errors.println("GOV-SOURCE-INPUT: " + invalid.getMessage());
            return 1;
        }
    }

    private record Result(int schemaVersion, String status, List<String> modules,
                          List<atf.tests.jvm.JvmSourceInventory.SourceSet> sourceSets, int files, int packages,
                          JvmArchitecture.Exceptions exceptions, List<String> diagnostics) { }
}
