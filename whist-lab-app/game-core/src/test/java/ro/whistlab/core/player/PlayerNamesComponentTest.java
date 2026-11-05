package ro.whistlab.core.player;

import static org.junit.Assert.*;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.session.GameSession;

public class PlayerNamesComponentTest {
    @Test public void randomizedRostersAreShortUniqueAndAvoidTheHumanName() {
        var seen = new HashSet<List<String>>();
        for (int seats = 3; seats <= 6; seats++) for (int seed = 0; seed < 80; seed++) {
            var names = PlayerNames.forMatch(seats, "Mara", seed);
            assertEquals(seats, names.size());
            assertEquals(seats, new HashSet<>(names).size());
            assertEquals("Mara", names.get(0));
            assertTrue(names.stream().allMatch(name -> name.length() <= PlayerNames.MAX_LENGTH));
            seen.add(names);
        }
        assertTrue(seen.size() > 100);
    }

    @Test public void displayRandomnessNeverChangesTheDealOrDealer() {
        for (int seed = 0; seed < 25; seed++) {
            var config = new GameConfig(4, GameMode.ROMANIAN_WHIST, AiDifficulty.HARD, seed, true, 10);
            var normal = GameSession.newGame(config).snapshot();
            var named = GameSession.newGame(config, "Ștefan").snapshot();
            assertEquals(normal.dealerSeat(), named.dealerSeat());
            for (int seat = 0; seat < 4; seat++) assertEquals(normal.players().get(seat).hand(), named.players().get(seat).hand());
            assertEquals("Ștefan", named.players().get(0).name());
            assertEquals(PlayerNames.forMatch(4, "Ștefan", seed), named.players().stream().map(GameSession.PlayerSnapshot::name).toList());
        }
    }

    @Test public void renameAndResetSurviveCheckpointWithoutChangingTheGame() {
        var game = GameSession.newGame(GameConfig.defaults(41));
        var before = game.snapshot();
        game.renameHuman("Cristi");
        game = GameSession.restoreComplete(game.saveState());
        assertEquals("Cristi", game.snapshot().players().get(0).name());
        assertEquals(before.players().get(0).hand(), game.snapshot().players().get(0).hand());
        assertEquals(before.history(), game.snapshot().history());
        game.renameHuman(PlayerNames.DEFAULT_HUMAN);
        assertEquals("You", GameSession.restoreComplete(game.saveState()).snapshot().players().get(0).name());
        var active = game;
        var checkpoint = active.saveState();
        assertThrows(IllegalArgumentException.class, () -> active.renameHuman(active.snapshot().players().get(1).name()));
        assertEquals(checkpoint, active.saveState());
    }

    @Test public void namesAreNormalizedAndRejectInvisibleOrOversizedInput() {
        assertEquals("Ștefan", PlayerNames.normalize("  S\u0326tefan  "));
        assertEquals("Ana-Maria", PlayerNames.normalize("Ana-Maria"));
        for (String invalid : List.of("", " ", "abcdefghijk", "Ana\nIon", "A\u202eB", "<b>Ana</b>", "😀"))
            assertThrows(invalid, IllegalArgumentException.class, () -> PlayerNames.normalize(invalid));
        assertThrows(IllegalArgumentException.class, () -> PlayerNames.forMatch(4, null, 0));
    }
}
