package atf.tests.governance.source.changes;

import atf.android.process.CommandProcess;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class TestChangeGitFixture {
    static final String FEEDBACK = "docs/feedback/ATF_Example.md";
    static final String BASELINE = "docs/governance/ATF_TestChangesBaseline.md";
    static final String TEST = "nested/module/src/test/java/demo/Old Test.java";
    static final String RESOURCE = "nested/module/src/test/resources/settings.json";
    static final String IGNORED = "whist-lab-app/app/src/androidTest/java/demo/Ignored.java";

    final Path root;
    final String base;

    TestChangeGitFixture(Path root) throws Exception {
        this.root = root;
        git("init", "-q", "-b", "main");
        git("config", "user.name", "ATF fixture");
        git("config", "user.email", "fixture@example.invalid");
        git("config", "core.autocrlf", "false");
        put(".gitignore", IGNORED + "\nignored-owner.md\n");
        put("pom.xml", "<project/>\n");
        put(TEST, "package demo;\nclass Old {}\n");
        put(RESOURCE, "{}\n");
        put("README.md", "# Fixture\n");
        commit("baseline");
        base = git("rev-parse", "HEAD");
    }

    void put(String relative, String content) throws IOException {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    void commit(String label) throws Exception {
        git("add", "-A");
        git("commit", "-qm", label);
    }

    String git(String... arguments) throws Exception {
        var command = new ArrayList<>(List.of("git", "-C", root.toString()));
        command.addAll(List.of(arguments));
        var output = CommandProcess.execute(command, Map.of("GIT_TERMINAL_PROMPT", "0"), Duration.ofSeconds(30), false);
        if (output.exitCode() != 0) throw new IOException(command + ": " + output.stderr());
        return new String(output.stdout(), StandardCharsets.UTF_8).strip();
    }

    String control(String id, String status, String commit, String expires, boolean allowsDelete, String... paths) {
        var yaml = new StringBuilder("```yaml\ntestChanges:\n  - id: ").append(id)
                .append("\n    status: ").append(status).append("\n    base: ").append(commit)
                .append("\n    expires: ").append(expires)
                .append("\n    reason: Verifică responsabilitatea printr-un caz independent.")
                .append("\n    proof: 'mvn -pl atf-tests test'\n    allowDelete: ").append(allowsDelete)
                .append("\n    paths:\n");
        for (String path : paths) yaml.append("      - '").append(path.replace("'", "''")).append("'\n");
        return yaml.append("```\n").toString();
    }
}
