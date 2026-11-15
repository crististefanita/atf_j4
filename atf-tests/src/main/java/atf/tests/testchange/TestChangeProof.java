package atf.tests.testchange;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

public final class TestChangeProof {
    public static final String ROOT_ENV = "ATF_TEST_CHANGE_PROOF_ROOT";
    public static final String REPORT = "component-report.html";
    public static final String RECEIPT = "proof.json";
    public static final String PENDING = "proof.pending.json";
    private static final String KIND = "ATF_TEST_CHANGE_PROOF";
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
                    DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES,
                    DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES);

    private TestChangeProof() { }

    static void verify(Plan plan, Path reviewRoot, Path proofRoot) throws IOException {
        if (proofRoot == null) throw failure("MISSING", plan.identity().id() + " requires " + ROOT_ENV);
        Path root = proofRoot.toRealPath();
        if (root.startsWith(reviewRoot) || !Files.isDirectory(root))
            throw failure("PATH", "proof root must be an external directory");
        Path directory = root.resolve(plan.identity().id());
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS) || !directory.toRealPath().equals(directory))
            throw failure("MISSING", plan.identity().id() + " evidence directory");
        Receipt receipt = JSON.readValue(file(directory, RECEIPT).toFile(), Receipt.class);
        verifyReceipt(plan, receipt);
        if (!digest(Files.readAllBytes(file(directory, REPORT))).equals(receipt.reportSha256()))
            throw failure("REPORT", plan.identity().id() + " Extent SHA-256 differs");
    }

    public static void stage(Plan plan, List<Outcome> positive, List<Outcome> negative, Path folder) throws IOException {
        var receipt = new Receipt(1, KIND, plan.identity(), positive, negative,
                digest(Files.readAllBytes(file(folder, REPORT))));
        verifyReceipt(plan, receipt);
        JSON.writerWithDefaultPrettyPrinter().writeValue(folder.resolve(PENDING).toFile(), receipt);
    }

    private static void verifyReceipt(Plan plan, Receipt receipt) throws IOException {
        if (receipt.schemaVersion() != 1 || !KIND.equals(receipt.kind())) throw failure("SCHEMA", plan.identity().id());
        var wanted = plan.identity();
        var observed = receipt.identity();
        if (!wanted.id().equals(observed.id())) throw failure("ID", wanted.id());
        if (!wanted.base().equals(observed.base())) throw failure("BASE", wanted.id());
        if (!wanted.commit().equals(observed.commit())) throw failure("COMMIT", wanted.id());
        if (!wanted.diffSha256().equals(observed.diffSha256())) throw failure("DIFF", wanted.id());
        verifyCases(plan.checks().negative(), receipt.negative(), "NEGATIVE", wanted.id());
        verifyCases(plan.checks().positive(), receipt.positive(), "POSITIVE", wanted.id());
    }

    private static void verifyCases(List<String> required, List<Outcome> observed, String kind, String id) throws IOException {
        var passed = new HashSet<String>();
        for (Outcome row : observed) {
            if (row == null || !"PASS".equals(row.verdict()) || !passed.add(row.selector()))
                throw failure(kind, id + " contains a missing, duplicate or nonpassing check");
        }
        if (!passed.equals(Set.copyOf(required)))
            throw failure(kind, id + " must contain exactly the declared passing checks");
    }

    private static Path file(Path folder, String name) throws IOException {
        Path path = folder.resolve(name);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || !path.toRealPath().equals(path))
            throw failure("MISSING", name + " in " + folder);
        return path;
    }

    static String digest(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    static IOException failure(String code, String message) {
        return new IOException("TEST-CHANGE-PROOF-" + code + ": " + message);
    }

    public record Plan(Identity identity, TestChangeProofChecks checks) { }
    public record Identity(String id, String base, String commit, String diffSha256) { }
    public record Outcome(String selector, String verdict) { }
    private record Receipt(int schemaVersion, String kind, Identity identity,
                           List<Outcome> positive, List<Outcome> negative, String reportSha256) { }
}
