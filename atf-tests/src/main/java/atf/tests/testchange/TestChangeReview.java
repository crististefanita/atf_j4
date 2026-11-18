package atf.tests.testchange;

import atf.tests.testchange.GitTestChangeSnapshot.Change;
import atf.tests.testchange.GitTestChangeSnapshot.Snapshot;
import atf.tests.testchange.TestChangeControls.Control;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.regex.Pattern;

/** Associates protected Git changes with one eligible control on the selected review commit. */
public final class TestChangeReview {
    public static final String BASE_PROPERTY = "atf.governance.testChanges.baseRef";
    public static final String BASE_ENV = "ATF_TEST_CHANGE_BASE_REF";

    private static final List<String> TEST_SETS = List.of("test", "androidTest", "rasterTest", "testFixtures");
    private static final Pattern SOURCE = Pattern.compile("(?s)^(.*?/)?src/(?:" + String.join("|", TEST_SETS) + ")/.+");
    private static final Pattern GRADLE = Pattern.compile("^whist-lab-app/(?:.*/)?(?:build|settings)\\.gradle(?:\\.kts)?$");
    private static final Set<String> OUTPUTS = Set.of(".git", "target", "build", ".gradle", "artifacts", "node_modules", "release");
    private static final List<String> GATES = List.of(
            "atf-tests/src/main/java/atf/tests/documentation/policy/DocumentationContentPolicy.java",
            "config/atf-governance-policy.json", "config/atf-test-suites.json", "config/gradle-app-collections.properties",
            "config/gradle-audit-collections.properties", "config/test-collections.json", "config/release-platform-rag-policy.json",
            "scripts/run-atf-governance.ps1", "scripts/run-component-suites.ps1",
            "scripts/AtfQualificationSnapshot.ps1", "scripts/AtfTestChangeBatch.ps1", "scripts/AtfComponentJvm.ps1",
            "scripts/Get-AtfSourceInputs.ps1", "scripts/Copy-AtfSourceInputs.ps1", "scripts/AtfSigningSourcePolicy.ps1",
            "atf-tests/tools/release-test-scope.mjs", "atf-tests/tools/component-evidence-scope.mjs",
            "atf-tests/tools/github-retention-seal.mjs", "atf-tests/tools/collect-github-release-retention.mjs",
            "atf-tests/tools/verify-release-git-snapshot.mjs",
            "atf-tests/tools/evaluate-release-rag.mjs");
    private static final List<String> GATE_DIRECTORIES = List.of(
            "atf-tests/src/main/java/atf/tests/testchange/", "whist-lab-app/gradle/", ".github/workflows/");

    private TestChangeReview() { }

    public static Result inspect(Path root, String explicitBase, Map<String, String> environment, LocalDate today)
            throws IOException, InterruptedException {
        return review(root, explicitBase, environment, today).result();
    }

    public static Result verify(Path root, String explicitBase, Map<String, String> environment, LocalDate today)
            throws IOException, InterruptedException {
        Review review = review(root, explicitBase, environment, today);
        if (!review.result().diagnostics().isEmpty() || review.verified().isEmpty()) return review.result();
        String configured = environment.get(TestChangeProof.ROOT_ENV);
        Path proofRoot = configured == null || configured.isBlank() ? null : Path.of(configured);
        String diff = GitTestChangeSnapshot.proofDiff(review.snapshot());
        for (Control control : review.verified().values()) {
            TestChangeProof.verify(plan(review.snapshot(), control, diff), review.snapshot().root(), proofRoot);
        }
        return review.result();
    }

    public static TestChangeProof.Plan proofPlan(Path root, String base, String id, LocalDate today)
            throws IOException, InterruptedException {
        Review review = proofReview(root, base, today);
        Control control = review.verified().get(id);
        if (control == null) throw TestChangeProof.failure("CONTROL", id + " must authorize this diff as VERIFIED");
        return plan(review.snapshot(), control, GitTestChangeSnapshot.proofDiff(review.snapshot()));
    }

    public static List<String> requiredProofs(Path root, String base, String expectedCommit, LocalDate today)
            throws IOException, InterruptedException {
        Review review = proofReview(root, base, today);
        if (!review.snapshot().head().equals(expectedCommit))
            throw TestChangeProof.failure("COMMIT", "staged checkout moved");
        String diff = GitTestChangeSnapshot.proofDiff(review.snapshot());
        var required = new ArrayList<String>();
        for (Control control : review.verified().values()) {
            required.add(plan(review.snapshot(), control, diff).identity().id());
        }
        return List.copyOf(required);
    }

    private static Review proofReview(Path root, String base, LocalDate today) throws IOException, InterruptedException {
        Review review = review(root, base, Map.of("CI", "true"), today);
        if (!review.result().diagnostics().isEmpty())
            throw TestChangeProof.failure("REVIEW", String.join("\n", review.result().diagnostics()));
        return review;
    }

    private static TestChangeProof.Plan plan(Snapshot snapshot, Control control, String diff) throws IOException {
        if (control.checks() == null) throw TestChangeProof.failure("CHECKS", control.id() + " requires positive and negative checks");
        return new TestChangeProof.Plan(
                new TestChangeProof.Identity(control.id(), snapshot.base(), snapshot.head(), diff), control.checks());
    }

    private static Review review(Path root, String explicitBase, Map<String, String> environment, LocalDate today)
            throws IOException, InterruptedException {
        boolean ci = environment.containsKey("CI") && !"false".equalsIgnoreCase(environment.get("CI"));
        String ref = explicitBase == null || explicitBase.isBlank() ? environment.get(BASE_ENV) : explicitBase;
        if ((ref == null || ref.isBlank()) && ci)
            throw new IOException("TEST-CHANGE-BASE: CI requires " + BASE_PROPERTY + " or " + BASE_ENV);
        Snapshot snapshot = GitTestChangeSnapshot.read(root, ref == null || ref.isBlank() ? "HEAD" : ref.strip(), ci);
        var controls = controls(snapshot);
        var diagnostics = new TreeSet<String>();
        var distinct = new HashSet<String>();
        var verified = new TreeMap<String, Control>();
        for (Change change : snapshot.changes()) {
            if (!protectedPath(change.path())) continue;
            distinct.add(change.path());
            String violation = violation(change, controls, snapshot.base(), today, verified);
            if (violation != null) diagnostics.add(violation);
        }
        return new Review(snapshot, new Result(snapshot.base(), distinct.size(), List.copyOf(diagnostics)), verified);
    }

    private static List<Control> controls(Snapshot snapshot) throws IOException {
        var controls = new ArrayList<Control>();
        var ids = new HashSet<String>();
        for (String relative : snapshot.documents()) {
            Path file = snapshot.root().resolve(relative);
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || !file.toRealPath().equals(file))
                throw new IOException("TEST-CHANGE-DOC: expected a regular project document " + relative);
            for (Control control : TestChangeControls.parse(Files.readString(file), relative)) {
                if (!ids.add(control.id())) throw new IOException("TEST-CHANGE-ID: duplicate " + control.id() + " in " + relative);
                controls.add(control);
            }
        }
        return List.copyOf(controls);
    }

    private static String violation(Change change, List<Control> controls, String base, LocalDate today,
                                    Map<String, Control> verified) {
        String key = change.kind() + " " + change.path();
        if (!TestChangeControls.exactPath(change.path())) return "TEST-CHANGE-PATH: " + key;
        if (!Set.of("A", "M", "D").contains(change.kind())) return "TEST-CHANGE-GIT-STATE: " + key;
        var matching = controls.stream().filter(control -> control.paths().contains(change.path())
                && control.authorizesReview() && control.base().equals(base)).toList();
        if (matching.isEmpty()) return "TEST-CHANGE-UNCOVERED: " + key + " (base " + base + ")";
        var current = matching.stream().filter(control -> !control.expires().isBefore(today)).toList();
        if (current.isEmpty()) return "TEST-CHANGE-EXPIRED: " + key + " " + ids(matching);
        var eligible = change.kind().equals("D") ? current.stream().filter(Control::allowDelete).toList() : current;
        if (eligible.isEmpty()) return "TEST-CHANGE-DELETE: " + key + " needs allowDelete: true";
        if (eligible.size() > 1) return "TEST-CHANGE-AMBIGUOUS: " + key + " " + ids(eligible);
        Control control = eligible.getFirst();
        if (control.status().equals("VERIFIED")) verified.put(control.id(), control);
        return null;
    }

    private static List<String> ids(List<Control> controls) {
        return controls.stream().map(Control::id).sorted().toList();
    }

    static boolean protectedPath(String path) {
        var source = SOURCE.matcher(path);
        if (source.matches()) return !generated(source.group(1));
        if (generated(path)) return false;
        return path.equals("pom.xml") || path.endsWith("/pom.xml") || GRADLE.matcher(path).matches()
                || GATES.contains(path) || GATE_DIRECTORIES.stream().anyMatch(path::startsWith)
                || path.startsWith("atf-tests/tools/") && path.endsWith(".test.mjs");
    }

    private static boolean generated(String path) {
        if (path == null) return false;
        for (String segment : path.split("/")) if (OUTPUTS.contains(segment)) return true;
        return false;
    }

    static List<String> protectedPathspecs() {
        var specs = new ArrayList<>(GATES);
        specs.addAll(GATE_DIRECTORIES);
        specs.addAll(List.of("pom.xml", ":(glob)**/pom.xml", ":(glob)whist-lab-app/**/build.gradle*",
                ":(glob)whist-lab-app/**/settings.gradle*", ":(glob)atf-tests/tools/**/*.test.mjs"));
        for (String set : TEST_SETS) specs.add(":(glob)**/src/" + set + "/**");
        return specs;
    }

    private record Review(Snapshot snapshot, Result result, Map<String, Control> verified) { }

    public record Result(String base, int protectedPaths, List<String> diagnostics) { }
}
