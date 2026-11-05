package atf.tests.governance.documentation.release;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** One active release; historical evidence lives outside the working artifact store. */
final class ReleaseArtifactCatalog {
    private static final ObjectMapper JSON = new ObjectMapper();
    private ReleaseArtifactCatalog() { }

    static List<Path> folders(Path project) throws IOException {
        Path root = project.resolve("artifacts");
        require(Files.isDirectory(root) && root.toRealPath().startsWith(project.toRealPath()), "ROOT", root);
        var index = JSON.readTree(root.resolve("index.json").toFile());
        require(index.path("schemaVersion").isIntegralNumber() && index.path("schemaVersion").asInt() == 2, "SCHEMA", root);
        Path reports = root.resolve("reports");
        require(Files.isDirectory(reports) && reports.toRealPath().getParent().equals(root.toRealPath()), "REPORTS", reports);
        require(Files.isRegularFile(reports.resolve("README.md")), "README", reports);
        var versions = index.path("versions");
        require(versions.isArray() && !versions.isEmpty(), "POPULATION", root);
        var names = new HashSet<String>();
        var result = new ArrayList<Path>();
        for (var entry : versions) {
            String version = entry.asText();
            require(entry.isTextual() && version.matches("(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)(?:-[0-9A-Za-z.-]+)?"), "VERSION", root);
            require(names.add(version), "DUPLICATE", root);
            Path folder = reports.resolve(version);
            require(Files.isDirectory(folder) && folder.toRealPath().getParent().equals(reports.toRealPath()), "FOLDER", folder);
            for (String file : List.of("README.md", "release-validation.json"))
                require(Files.isRegularFile(folder.resolve(file)) && folder.resolve(file).toRealPath().startsWith(folder.toRealPath()), "REQUIRED", folder.resolve(file));
            var validation = JSON.readTree(folder.resolve("release-validation.json").toFile());
            require(version.equals(validation.path("release").path("versionName").asText()), "IDENTITY", folder);
            if (Files.exists(folder.resolve("github-release.json"))) {
                var release = JSON.readTree(folder.resolve("github-release.json").toFile());
                require(("v" + version).equals(release.path("tag").asText()), "TAG", folder);
            }
            result.add(folder);
        }
        require(index.path("currentVersion").isTextual() && names.contains(index.path("currentVersion").asText()), "CURRENT", root);
        require(names.size() == 1, "RETENTION", reports);
        require(Files.isRegularFile(root.resolve("README.md")), "README", root);
        Set<String> expected = Set.of("README.md", "index.json", "reports");
        try (var files = Files.list(root)) {
            var actual = files.map(path -> path.getFileName().toString()).collect(java.util.stream.Collectors.toSet());
            require(expected.equals(actual), "UNREGISTERED", root);
        }
        Set<String> reportEntries = new HashSet<>(names);
        reportEntries.add("README.md");
        try (var files = Files.list(reports)) {
            var actual = files.map(path -> path.getFileName().toString()).collect(java.util.stream.Collectors.toSet());
            require(reportEntries.equals(actual), "UNREGISTERED", reports);
        }
        return List.copyOf(result);
    }

    private static void require(boolean condition, String code, Path path) throws IOException {
        if (!condition) throw new IOException("RELEASE-" + code + ": " + path);
    }
}
