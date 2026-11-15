package atf.tests.documentation;

import java.nio.file.Path;

/** Shared discovery and diagnostic vocabulary for documentation governance. */
public final class DocumentationGovernanceSupport {
    private DocumentationGovernanceSupport() {
    }

    public record Violation(String code, Path file, int line, String finding,
                     String rule, String fix) {
        @Override
        public String toString() {
            return "\n[" + code + "] " + file + ":" + line
                    + "\n  found: " + finding
                    + "\n  rule: " + rule
                    + "\n  fix: " + fix;
        }
    }

    public static Violation violation(String code, Path root, Path file, int line,
                               String finding, String rule, String fix) {
        Path relative = root.toAbsolutePath().normalize()
                .relativize(file.toAbsolutePath().normalize());
        return new Violation(code, relative, line, finding, rule, fix);
    }

}
