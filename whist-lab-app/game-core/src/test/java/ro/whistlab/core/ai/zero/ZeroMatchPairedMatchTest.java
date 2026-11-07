package ro.whistlab.core.ai.zero;

import static atf.gradle.audit.assertions.GradleAssertions.*;

import atf.gradle.audit.GradleAuditedTest;
import atf.gradle.audit.GradleInputs;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;

/** Opt-in same-deal evaluation, with an optional frozen reference from the same seed range. */
@RunWith(Parameterized.class)
public final class ZeroMatchPairedMatchTest extends GradleAuditedTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final ZeroMatchPair.Case scenario;

    @Parameterized.Parameters(name = "{0}")
    public static List<ZeroMatchPair.Case> cases() {
        long first = Long.getLong("whist.zero.firstSeed", 202611070100L);
        int repeats = Integer.getInteger("whist.zero.repeats", 2);
        if (repeats < 1 || repeats > 32) throw new IllegalArgumentException("Expected 1–32 deals per cell");
        boolean pressure = Boolean.getBoolean("whist.zero.pressure");
        var cases = new ArrayList<ZeroMatchPair.Case>();
        int cell = 0;
        for (var mode : List.of(GameMode.ROMANIAN_WHIST, GameMode.LONG_ROMANIAN)) {
            for (int players = 3; players <= 6; players++, cell++) {
                for (int trial = 0; trial < repeats; trial++) {
                    int hard = (cell + trial) % players;
                    Integer focal = pressure ? (trial % 4 < 2 ? (hard + 1) % players : hard) : null;
                    cases.add(new ZeroMatchPair.Case(mode, players, first + 32 * cell + trial,
                            hard, trial % 2 == 0, focal));
                }
            }
        }
        return cases;
    }

    public ZeroMatchPairedMatchTest(ZeroMatchPair.Case scenario) { this.scenario = scenario; }

    @Test public void playsTheSameDealsWithAndWithoutTheZeroRule() throws Exception {
        GradleInputs.fixture("Offline paired match input", "rules and seed", scenario,
                "arms", List.of("classic control", "zero enabled"),
                "visibility", "Policies observe own cards and public play; report stores deal digests");
        var pair = ZeroMatchPair.play(scenario);
        write(pair);
        assertEquals("Identical distributions, dealers and trumps in both arms", pair.off().deals(), pair.on().deals());
        assertEquals("No strategic zero activation in the disabled arm", List.of(), pair.off().pivots());
        if (scenario.pressureSeat() != null) {
            int prefix = pair.off().hands().size() - 5;
            assertEquals("Pressure prefix is the same legal play in both arms",
                    digest(JSON.valueToTree(pair.off().hands().subList(0, prefix))),
                    digest(JSON.valueToTree(pair.on().hands().subList(0, prefix))));
        }
        for (var arm : List.of(pair.off(), pair.on())) {
            var scheduled = GameRules.handSizes(scenario.mode(), scenario.players());
            assertEquals("Every scheduled hand is played in order", scheduled,
                    arm.hands().stream().map(ZeroMatchPair.Hand::size).toList());
            assertEquals("Every card of every dealt hand is played",
                    scenario.players() * scheduled.stream().mapToInt(Integer::intValue).sum(),
                    arm.hands().stream().mapToInt(hand -> hand.plays().size()).sum());
            assertEquals("Apply 500 only to exact raw zero in the enabled arm",
                    arm.seats().stream().map(seat -> arm.enabled() && seat.raw() == 0 ? 500 : seat.raw()).toList(),
                    arm.seats().stream().map(ZeroMatchPair.SeatResult::finalScore).toList());
        }
        GradleInputs.fixture("Outcome by seat", "control", pair.off().seats(), "zero", pair.on().seats(),
                "eligible decisions and selections", pair.on().pivots());
        compareReference(pair);
    }

    private void compareReference(ZeroMatchPair.Pair pair) throws Exception {
        String reference = System.getProperty("whist.zero.reference", "");
        if (reference.isBlank()) return;
        Path path = Path.of(reference, scenario.id() + ".json");
        var frozen = JSON.readTree(path.toFile());
        assertEquals("Frozen reference uses the same rules, seats and seed",
                JSON.treeToValue(frozen.path("scenario"), ZeroMatchPair.Case.class), scenario);
        assertEquals("The control replay is identical across policy revisions",
                digest(frozen.path("off")), digest(JSON.valueToTree(pair.off())));
        GradleInputs.fixture("Same-deal comparison of enabled policies", "reference file", path.getFileName().toString(),
                "reference seats", frozen.path("on").path("seats"), "candidate seats", pair.on().seats());
    }

    private static String digest(com.fasterxml.jackson.databind.JsonNode arm) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(JSON.writeValueAsBytes(arm)));
    }

    private void write(ZeroMatchPair.Pair pair) throws Exception {
        String configured = System.getProperty("whist.zero.output", "");
        if (configured.isBlank()) return;
        Path folder = Path.of(configured);
        Files.createDirectories(folder);
        Files.writeString(folder.resolve(scenario.id() + ".json"),
                JSON.writerWithDefaultPrettyPrinter().writeValueAsString(pair) + "\n", StandardOpenOption.CREATE_NEW);
    }
}
