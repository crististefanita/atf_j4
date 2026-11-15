package atf.tests.documentation;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

/** Keeps project documentation relocatable and free of machine-specific paths. */
final class DocumentationPathRules {
    private static final Pattern ABSOLUTE_WINDOWS_PATH =
            Pattern.compile("(?i)(?<![a-z0-9])(?:file:///)?[a-z]:[\\\\/]");
    private static final Pattern CONCRETE_TIMESTAMPED_REPORT =
            Pattern.compile("(?i)(?:api|ui)-atf-extent-report[^\\s`)\\]]*20\\d{2}-\\d{2}-\\d{2}[^\\s`)\\]]*\\.html");
    static void validatePaths(Path path, Path root,
                         List<String> lines, List<DocumentationGovernanceSupport.Violation> violations) {
        int lineNumber = 0;
        for (String line : lines) {
            lineNumber++;
            if (ABSOLUTE_WINDOWS_PATH.matcher(line).find()) {
                violations.add(DocumentationGovernanceSupport.violation(
                        "DOC-PATH-ABSOLUTE", root, path, lineNumber,
                        "documentation exposes a machine-specific absolute path",
                        "normative documentation must be relocatable",
                        "replace the local path with a relative link or a configurable placeholder"));
            }
        }
    }

    static void validateGeneratedReportNames(Path path, Path root,
                         List<String> lines, List<DocumentationGovernanceSupport.Violation> violations) {
        int lineNumber = 0;
        for (String line : lines) {
            lineNumber++;
            if (CONCRETE_TIMESTAMPED_REPORT.matcher(line).find()) {
                violations.add(DocumentationGovernanceSupport.violation(
                        "DOC-ARTIFACT-NAME", root, path, lineNumber,
                        "documentation contains a concrete generated report name",
                        "normative docs describe report structure, not one run",
                        "use a placeholder such as <configured-report-file>"));
            }
        }
    }
    private DocumentationPathRules() { }
}
