package atf.tests.testchange;

import atf.android.process.CommandProcess;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** Reads local Git evidence without rename heuristics, filters, fetch or shell expansion. */
final class GitTestChangeSnapshot {
    private static final Map<String, String> ENV = Map.of(
            "GIT_TERMINAL_PROMPT", "0", "GIT_OPTIONAL_LOCKS", "0");

    private GitTestChangeSnapshot() { }

    static Snapshot read(Path checkout, String ref, boolean ci) throws IOException, InterruptedException {
        Path root = checkout.toRealPath();
        if (!Files.exists(root.resolve(".git"), LinkOption.NOFOLLOW_LINKS)
                || !Path.of(output(root, "rev-parse", "--show-toplevel").strip()).toRealPath().equals(root))
            throw new IOException("TEST-CHANGE-CHECKOUT: use the root of the reviewed Git checkout");

        String head = commit(root, "HEAD");
        String base = commit(root, ref);
        if (ci && base.equals(head))
            throw new IOException("TEST-CHANGE-BASE: CI requires a review base earlier than HEAD");
        if (git(root, "merge-base", "--is-ancestor", base, head).exitCode() != 0)
            throw new IOException("TEST-CHANGE-BASE: " + base + " is not an ancestor of " + head);

        var changes = new LinkedHashSet<Change>();
        changes.addAll(diff(root, base, false));
        changes.addAll(diff(root, base, true));
        for (String path : fields(output(root, "ls-files", "--others", "--exclude-standard", "-z", "--", "."))) {
            changes.add(new Change("A", path));
        }
        var ignored = new ArrayList<>(List.of("ls-files", "--others", "--ignored", "--exclude-standard", "-z", "--"));
        ignored.addAll(TestChangeReview.protectedPathspecs());
        var ignoredPaths = fields(output(root, ignored.toArray(String[]::new)));
        for (String path : ignoredPaths) changes.add(new Change("A", path));
        boolean ignoredProtected = ignoredPaths.stream().anyMatch(TestChangeReview::protectedPath);

        var candidateDocs = fields(output(root, "ls-files", "--cached", "--others", "--exclude-standard",
                "-z", "--", TestChangeControls.FEEDBACK, TestChangeControls.BASELINE));
        var documents = new TreeSet<String>();
        for (String path : candidateDocs) {
            if (path.endsWith(".md") && Files.exists(root.resolve(path), LinkOption.NOFOLLOW_LINKS)) documents.add(path);
        }
        if (!commit(root, "HEAD").equals(head)) throw new IOException("TEST-CHANGE-CHECKOUT: HEAD changed during review");
        return new Snapshot(root, base, head, List.copyOf(changes), List.copyOf(documents), ignoredProtected);
    }

    private static String commit(Path root, String ref) throws IOException, InterruptedException {
        var result = git(root, "rev-parse", "--verify", "--end-of-options", ref + "^{commit}");
        String value = new String(result.stdout(), StandardCharsets.UTF_8).strip();
        if (result.exitCode() != 0 || !TestChangeControls.commitId(value))
            throw new IOException("TEST-CHANGE-BASE: cannot resolve commit " + ref + " in the reviewed checkout");
        return value;
    }

    private static List<Change> diff(Path root, String base, boolean staged) throws IOException, InterruptedException {
        var args = new ArrayList<>(List.of("diff", "--no-ext-diff", "--no-textconv", "--no-renames",
                "--ignore-submodules=none", "--name-status", "-z"));
        if (staged) args.add("--cached");
        args.addAll(List.of(base, "--", "."));
        var fields = fields(output(root, args.toArray(String[]::new)));
        if (fields.size() % 2 != 0) throw new IOException("TEST-CHANGE-GIT: truncated name-status pair");
        var changes = new ArrayList<Change>();
        for (int i = 0; i < fields.size(); i += 2) changes.add(new Change(fields.get(i), fields.get(i + 1)));
        return changes;
    }

    private static List<String> fields(String data) throws IOException {
        if (data.isEmpty()) return List.of();
        if (!data.endsWith("\u0000")) throw new IOException("TEST-CHANGE-GIT: truncated NUL-delimited output");
        return List.of(data.substring(0, data.length() - 1).split("\u0000", -1));
    }

    private static String output(Path root, String... args) throws IOException, InterruptedException {
        var result = git(root, args);
        if (result.exitCode() != 0)
            throw new IOException("TEST-CHANGE-GIT: " + args[0] + " exited " + result.exitCode() + ": " + result.stderr().strip());
        return StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(result.stdout())).toString();
    }

    private static CommandProcess.Output git(Path root, String... args) throws IOException, InterruptedException {
        var command = new ArrayList<>(List.of("git", "--no-replace-objects", "--no-lazy-fetch", "-C", root.toString()));
        command.addAll(List.of(args));
        return CommandProcess.execute(command, ENV, Duration.ofSeconds(30), false);
    }

    static String proofDiff(Snapshot snapshot) throws IOException, InterruptedException {
        Path root = snapshot.root();
        if (snapshot.ignoredProtected() || !output(root, "status", "--porcelain=v1", "-z", "--untracked-files=all", "--ignore-submodules=none").isEmpty()
                || !snapshot.head().equals(commit(root, "HEAD"))) {
            throw new IOException("TEST-CHANGE-PROOF-DIRTY: evidence requires the clean reviewed commit");
        }
        var diff = git(root, "diff", "--raw", "-z", "--no-abbrev", "--no-renames", "--no-ext-diff",
                "--no-textconv", "--ignore-submodules=none", snapshot.base(), snapshot.head(), "--");
        if (diff.exitCode() != 0) throw new IOException("TEST-CHANGE-PROOF-DIFF: " + diff.stderr().strip());
        return TestChangeProof.digest(diff.stdout());
    }

    record Change(String kind, String path) { }
    record Snapshot(Path root, String base, String head, List<Change> changes, List<String> documents, boolean ignoredProtected) { }
}
