package atf.tests.governance.documentation.release;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Retains the current and immediately preceding release; older evidence lives outside the active catalog. */
final class ReleaseArtifactCatalog {
    private static final ObjectMapper JSON = new ObjectMapper();
    private ReleaseArtifactCatalog() { }

    static List<Path> folders(Path project) throws IOException {
        require(!Files.exists(project.resolve("dist")), "LEGACY-DIST", project.resolve("dist"));
        require(!Files.exists(project.resolve("artifacts")), "LEGACY-ARTIFACTS", project.resolve("artifacts"));
        Path root = project.resolve("release");
        require(Files.isDirectory(root) && root.toRealPath().startsWith(project.toRealPath()), "ROOT", root);
        var index = JSON.readTree(root.resolve("index.json").toFile());
        require(index.path("schemaVersion").isIntegralNumber() && index.path("schemaVersion").asInt() == 2, "SCHEMA", root);
        var versions = index.path("versions");
        require(versions.isArray() && !versions.isEmpty(), "POPULATION", root);
        var names = new HashSet<String>();
        var result = new ArrayList<Path>();
        for (var entry : versions) {
            String version = entry.asText();
            require(entry.isTextual() && version.matches("(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)(?:-[0-9A-Za-z.-]+)?"), "VERSION", root);
            require(names.add(version), "DUPLICATE", root);
            Path folder = root.resolve(version);
            require(Files.isDirectory(folder) && folder.toRealPath().getParent().equals(root.toRealPath()), "FOLDER", folder);
            for (String file : List.of("README.md", "reports/README.md", "release-validation.json"))
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
        require(names.size() >= 1 && names.size() <= 2, "RETENTION", root);
        require(Files.isRegularFile(root.resolve("README.md")), "README", root);
        Set<String> expected = new HashSet<>(names);
        expected.addAll(Set.of("README.md", "index.json"));
        var candidateVersion = index.path("candidateVersion");
        if (!candidateVersion.isMissingNode()) {
            String version = candidateVersion.asText();
            require(candidateVersion.isTextual()
                    && version.matches("(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)(?:-[0-9A-Za-z.-]+)?")
                    && !names.contains(version), "CANDIDATE-VERSION", root);
            Path receipt = root.resolve("candidate.json");
            require(Files.isRegularFile(receipt) && receipt.toRealPath().getParent().equals(root.toRealPath()),
                    "CANDIDATE-REQUIRED", receipt);
            var candidate = JSON.readTree(receipt.toFile());
            require(version.equals(candidate.path("release").path("versionName").asText()), "CANDIDATE-IDENTITY", receipt);
            expected.add("candidate.json");
        }
        try (var files = Files.list(root)) {
            var actual = files.map(path -> path.getFileName().toString()).collect(java.util.stream.Collectors.toSet());
            require(expected.equals(actual), "UNREGISTERED", root);
        }
        return List.copyOf(result);
    }

    private static void require(boolean condition, String code, Path path) throws IOException {
        if (!condition) throw new IOException("RELEASE-" + code + ": " + path);
    }
}
