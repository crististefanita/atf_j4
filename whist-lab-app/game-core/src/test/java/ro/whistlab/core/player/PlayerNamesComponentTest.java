package ro.whistlab.core.player;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.session.GameSession;

public class PlayerNamesComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void randomizedRostersAreShortUniqueAndAvoidTheHumanName() {
        var seen = new HashSet<List<String>>();
        for (int seats = 3; seats <= 6; seats++) for (int seed = 0; seed < 80; seed++) {
            var names = PlayerNames.forMatch(seats, "Mara", seed);

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "names", names, "seats", seats, "seed", seed, "seen", seen);
            assertEquals("names.size() must equal seats", seats, names.size());
            assertEquals("new HashSet<>(names).size() must equal seats", seats, new HashSet<>(names).size());
            assertEquals("names.get(0) must equal \"Mara\"", "Mara", names.get(0));
            assertTrue("names.stream().allMatch((name)->name.length() <= PlayerNames.MAX_LENGTH) must hold", names.stream().allMatch(name -> name.length() <= PlayerNames.MAX_LENGTH));
            seen.add(names);
        }
        assertTrue("seen.size() > 100 must hold", seen.size() > 100);
    }

    @Test public void displayRandomnessNeverChangesTheDealOrDealer() {
        for (int seed = 0; seed < 25; seed++) {
            var config = new GameConfig(4, GameMode.ROMANIAN_WHIST, AiDifficulty.HARD, seed, true, 10);
            var normal = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", config)).snapshot();
            var named = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", config), atf.gradle.audit.GradleInputs.created("Human name before creation", "Ștefan")).snapshot();

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "config", config, "named", named, "normal", normal, "seed", seed);
            assertEquals("named.dealerSeat() must equal normal.dealerSeat()", normal.dealerSeat(), named.dealerSeat());
            for (int seat = 0; seat < 4; seat++) assertEquals("named.players().get(seat).hand() must equal normal.players().get(seat).hand()", normal.players().get(seat).hand(), named.players().get(seat).hand());
            assertEquals("named.players().get(0).name() must equal \"\\u0218tefan\"", "Ștefan", named.players().get(0).name());
            assertEquals("named.players().stream().map(GameSession.PlayerSnapshot::name).toList() must equal PlayerNames.forMatch(4, \"\\u0218tefan\", seed)", PlayerNames.forMatch(4, "Ștefan", seed), named.players().stream().map(GameSession.PlayerSnapshot::name).toList());
        }
    }

    @Test public void renameAndResetSurviveCheckpointWithoutChangingTheGame() {
        var game = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", GameConfig.defaults(41)));
        var before = game.snapshot();
        game.renameHuman("Cristi");
        game = GameSession.restoreComplete(game.saveState());

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "before", before, "game", game);
        assertEquals("game.snapshot().players().get(0).name() must equal \"Cristi\"", "Cristi", game.snapshot().players().get(0).name());
        assertEquals("game.snapshot().players().get(0).hand() must equal before.players().get(0).hand()", before.players().get(0).hand(), game.snapshot().players().get(0).hand());
        assertEquals("game.snapshot().history() must equal before.history()", before.history(), game.snapshot().history());
        game.renameHuman(PlayerNames.DEFAULT_HUMAN);
        assertEquals("GameSession.restoreComplete(game.saveState()).snapshot().players().get(0).name() must equal \"You\"", "You", GameSession.restoreComplete(game.saveState()).snapshot().players().get(0).name());
        var active = game;
        var checkpoint = active.saveState();
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> active.renameHuman(active.snapshot().players().get(1).name()));
        assertEquals("active.saveState() must equal checkpoint", checkpoint, active.saveState());
    }

    @Test public void namesAreNormalizedAndRejectInvisibleOrOversizedInput() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", "Ștefan", "declared constant 2", "  S\u0326tefan  ", "declared constant 3", "Ana-Maria", "declared constant 4", "", "declared constant 5", " ", "declared constant 6", "abcdefghijk", "declared constant 7", "Ana\nIon", "declared constant 8", "A\u202eB", "declared constant 9", "<b>Ana</b>", "declared constant 10", "😀", "declared constant 11", 4, "declared constant 12", 0);
        assertEquals("PlayerNames.normalize(\" S\\u0326tefan \") must equal \"\\u0218tefan\"", "Ștefan", PlayerNames.normalize("  S\u0326tefan  "));
        assertEquals("PlayerNames.normalize(\"Ana-Maria\") must equal \"Ana-Maria\"", "Ana-Maria", PlayerNames.normalize("Ana-Maria"));
        for (String invalid : List.of("", " ", "abcdefghijk", "Ana\nIon", "A\u202eB", "<b>Ana</b>", "😀"))
            assertThrows(invalid, IllegalArgumentException.class, () -> PlayerNames.normalize(invalid));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> PlayerNames.forMatch(4, null, 0));
    }
}
