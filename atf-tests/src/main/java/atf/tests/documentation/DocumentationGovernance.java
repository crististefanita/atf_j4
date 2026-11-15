package atf.tests.documentation;

import atf.tests.documentation.DocumentationGovernanceSupport.Violation;
import atf.tests.documentation.links.DocumentationLocalLinks;
import atf.tests.documentation.policy.DocumentationContentPolicy;
import atf.tests.documentation.policy.FeedbackIndexRules;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static atf.tests.documentation.DocumentationGovernanceSupport.violation;

/** Executes the same editorial rules for a project inventory or an explicit list of files. */
public final class DocumentationGovernance {
    public enum Rule { STRUCTURE, LINKS, DIAGRAM, STYLE, PROSE, PATHS, ARTIFACT_NAMES, CONTENT, FEEDBACK_ROWS, FEEDBACK_PRIORITIES }

    public record Result(List<String> files, List<Violation> violations) {
        public Result {
            files = List.copyOf(files);
            violations = List.copyOf(violations);
        }
    }

    public static Result scan(Path project, List<String> selectedFiles) throws IOException {
        return scan(project, selectedFiles, EnumSet.allOf(Rule.class));
    }

    public static Result scanProject(Path project, Rule first, Rule... rest) throws IOException {
        return scan(project, List.of(), EnumSet.of(first, rest));
    }

    public static Result scanFile(Path project, String file, Rule first, Rule... rest) throws IOException {
        return scan(project, List.of(file), EnumSet.of(first, rest));
    }

    private static Result scan(Path project, List<String> selectedFiles, EnumSet<Rule> rules) throws IOException {
        Path root = project.toRealPath();
        List<Path> documents = (selectedFiles.isEmpty() ? DocumentationInventory.documents(root) : select(root, selectedFiles))
                .stream().sorted(java.util.Comparator.comparing(file -> relative(root, file))).toList();
        if (documents.isEmpty()) throw new IOException("DOC-INVENTORY-EMPTY: " + root);

        var context = rules.contains(Rule.CONTENT) ? DocumentationContentPolicy.Context.load(root) : null;
        List<Violation> violations = new ArrayList<>();
        for (Path file : documents) {
            List<String> lines;
            try { lines = Files.readAllLines(file); }
            catch (IOException unreadable) {
                violations.add(violation("DOC-READ", root, file, 1, "documentation could not be read: " + unreadable.getMessage(),
                        "every governed document is readable UTF-8", "fix the file encoding or filesystem access"));
                continue;
            }
            for (Rule rule : rules) switch (rule) {
                case STRUCTURE -> DocumentationReadmeStructureRules.validate(file, root, lines, violations);
                case LINKS -> DocumentationLocalLinks.validate(file, root, lines, violations);
                case DIAGRAM -> DocumentationDiagramRules.validate(file, root, lines, violations);
                case STYLE -> DocumentationStyleRules.validate(file, root, lines, violations);
                case PROSE -> DocumentationProseRules.validate(file, root, lines, violations);
                case PATHS -> DocumentationPathRules.validatePaths(file, root, lines, violations);
                case ARTIFACT_NAMES -> DocumentationPathRules.validateGeneratedReportNames(file, root, lines, violations);
                case CONTENT -> DocumentationContentPolicy.validate(file, root, lines, context, violations);
                case FEEDBACK_ROWS -> { if (isFeedbackIndex(root, file)) FeedbackIndexRules.inspectRows(lines, file, root, violations); }
                case FEEDBACK_PRIORITIES -> { if (isFeedbackIndex(root, file)) FeedbackIndexRules.inspectPriorities(lines, file, root, violations); }
            }
        }
        return new Result(documents.stream().map(file -> relative(root, file)).toList(), violations);
    }

    private static List<Path> select(Path root, List<String> selectedFiles) throws IOException {
        var files = new ArrayList<Path>();
        for (String selection : selectedFiles) files.add(DocumentationInventory.selectedDocument(root, selection));
        return files.stream().distinct().sorted().toList();
    }

    private static String relative(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }

    private static boolean isFeedbackIndex(Path root, Path file) {
        return file.equals(root.resolve("docs/feedback/README.md"));
    }

    private DocumentationGovernance() { }
}
