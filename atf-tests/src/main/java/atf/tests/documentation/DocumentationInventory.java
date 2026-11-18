package atf.tests.documentation;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Shared document perimeter for the editorial API and the project governance suite. */
public final class DocumentationInventory {
    private static final Set<String> EXCLUDED = Set.of(".git", ".locks", ".gradle", ".idea", "target", "build",
            "artifacts", "dist", "node_modules", ".presentation-build", "migration-copy-rejected", "migration-quarantine");
    private static final Set<String> LIBRARY_GUIDES = Set.of("README.md", "README.Core.md", "README.Reporting.md",
            "README.Console.md", "README.Mock.md", "README.TestKit.md");

    public static List<Path> documents(Path root) throws IOException {
        var files = new ArrayList<Path>();
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) throws IOException {
                if (!directory.equals(root) && (excluded(directory.getFileName().toString())
                        || !directory.toRealPath().equals(directory))) return FileVisitResult.SKIP_SUBTREE;
                return FileVisitResult.CONTINUE;
            }

            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                if (attributes.isRegularFile() && governed(root, file) && file.toRealPath().equals(file)) files.add(file);
                return FileVisitResult.CONTINUE;
            }
        });
        return files.stream().sorted().toList();
    }

    static Path selectedDocument(Path root, String input) throws IOException {
        String relative = input.replace('\\', '/');
        if (relative.isBlank() || relative.startsWith("/") || relative.matches(".*[\\p{Cntrl}:].*"))
            throw new IllegalArgumentException("DOC-TARGET-PATH: expected a project-relative file: " + input);
        Path file = root;
        for (String part : relative.split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals(".."))
                throw new IllegalArgumentException("DOC-TARGET-PATH: non-canonical project-relative path: " + input);
            file = file.resolve(part);
            if (excluded(part) || Files.isSymbolicLink(file))
                throw new IllegalArgumentException("DOC-TARGET-SCOPE: outside the document inventory: " + input);
        }
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
            throw new IllegalArgumentException("DOC-TARGET-PATH: document does not exist or is not a file: " + input);
        Path canonical = file.toRealPath();
        if (!file.equals(canonical) || !canonical.startsWith(root) || !governed(root, canonical))
            throw new IllegalArgumentException("DOC-TARGET-SCOPE: outside the document inventory: " + input);
        return canonical;
    }

    public static boolean isReadme(Path file) {
        return file.getFileName().toString().equalsIgnoreCase("README.md");
    }

    public static boolean isLibraryGuide(Path root, Path file) {
        return file.getParent().equals(root.resolve("lib")) && LIBRARY_GUIDES.contains(file.getFileName().toString());
    }

    public static boolean isReleaseEvidence(Path root, Path file) throws IOException {
        Path relative = root.relativize(file);
        if (relative.getNameCount() < 3 || !relative.getName(0).toString().equals("release")) return false;
        Path manifest = root.resolve(relative.subpath(0, 2)).resolve("release-validation.json");
        if (!Files.isRegularFile(manifest)) return false;
        String artifact = relative.subpath(2, relative.getNameCount()).toString().replace('\\', '/');
        var files = new ObjectMapper().readTree(manifest.toFile()).at("/integrity/files");
        for (var entry : files) if (entry.path("path").asText().equals(artifact)) return true;
        return false;
    }

    private static boolean governed(Path root, Path file) {
        return file.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".md");
    }

    private static boolean excluded(String part) {
        return EXCLUDED.contains(part.toLowerCase(java.util.Locale.ROOT));
    }

    private DocumentationInventory() { }
}
