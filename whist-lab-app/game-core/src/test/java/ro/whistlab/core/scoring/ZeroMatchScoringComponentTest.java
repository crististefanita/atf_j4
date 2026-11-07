package ro.whistlab.core.scoring;

import static atf.gradle.audit.assertions.GradleAssertions.*;

import atf.gradle.audit.GradleAuditedTest;
import atf.gradle.audit.GradleInputs;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

@RunWith(Parameterized.class)
public class ZeroMatchScoringComponentTest extends GradleAuditedTest {
    @Parameterized.Parameters(name = "{0}, {1} players, seed {2}")
    public static Object[][] matches() {
        return new Object[][] {
            {GameMode.ROMANIAN_WHIST, 3, 4L, List.of(-70, 0, -13), 0},
            {GameMode.ROMANIAN_WHIST, 4, 2727L, List.of(0, -19, -23, 0), 0},
            {GameMode.ROMANIAN_WHIST, 5, 8L, List.of(-43, -4, -51, -41, 0), 0},
            {GameMode.ROMANIAN_WHIST, 6, 5L, List.of(-33, -13, 0, 5, -22, -6), 0},
            {GameMode.LONG_ROMANIAN, 3, 476L, List.of(0, -27, -29), -10},
            {GameMode.LONG_ROMANIAN, 4, 1287L, List.of(-23, 0, 0, -49), 0},
            {GameMode.LONG_ROMANIAN, 5, 3L, List.of(-34, -41, -48, 0, -40), 0},
            {GameMode.LONG_ROMANIAN, 6, 284L, List.of(5, -70, -34, -33, 0, -52), 0}
        };
    }

    private final GameMode mode;
    private final int players;
    private final long seed;
    private final List<Integer> expectedRaw;
    private final int expectedZeroSeatSeries;

    public ZeroMatchScoringComponentTest(GameMode mode, int players, long seed,
            List<Integer> expectedRaw, int expectedZeroSeatSeries) {
        this.mode = mode;
        this.players = players;
        this.seed = seed;
        this.expectedRaw = expectedRaw;
        this.expectedZeroSeatSeries = expectedZeroSeatSeries;
    }

    @Test public void zeroIsConvertedOnceAfterEveryHandAndSeriesHasBeenScored() {
        var config = GameConfig.newMatch(players, mode, AiDifficulty.NORMAL, seed, true, 10);
        GradleInputs.fixture("Complete legal schedule and deterministic bids", "config", config,
                "expected raw totals", expectedRaw, "expected final series at a zero seat", expectedZeroSeatSeries);
        var game = GameSession.newGame(config);
        var random = new Random(seed ^ 0xC0FFEE);
        var prematureAwards = new ArrayList<Integer>();
        for (;;) {
            var state = game.snapshot();
            if (state.finalHand() && state.phase() == GamePhase.SCORE) break;
            if (state.players().stream().anyMatch(player -> state.earnedZeroGame(player.seat())))
                prematureAwards.add(state.handNumber());
            switch (state.phase()) {
                case SCORE -> game.startNextHand();
                case BIDDING -> {
                    var legal = GameRules.allowedBids(state.handSize(), state.bidTotal(),
                            state.bidsSubmitted() == players - 1);
                    game.submitBid(state.biddingSeat(), legal.get(random.nextInt(legal.size())));
                }
                case PLAYING -> {
                    var led = state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit();
                    var actor = state.players().get(state.currentSeat());
                    game.playCard(actor.seat(), GameRules.legalCards(actor.hand(), led, state.trump(), mode).get(0));
                }
            }
        }

        var finished = game.snapshot();
        var expectedFinal = expectedRaw.stream().map(total -> total == 0 ? 500 : total).toList();
        var history = game.scoreHistory();
        var checkpoint = game.saveState();
        assertEquals("No award during play, including the last hand", List.of(), prematureAwards);
        assertEquals("Every scheduled hand was scored", GameRules.handSizes(mode, players).size(), history.completedHands());
        assertEquals("Scoring includes the last hand and its series", expectedRaw, finished.players().stream()
                .map(GameSession.PlayerSnapshot::totalScore).toList());
        assertEquals("A series adjustment can be the reason the raw score reaches zero", expectedZeroSeatSeries,
                finished.players().get(expectedRaw.indexOf(0)).streakPoints());
        assertEquals("Chronological ledger keeps the raw cumulative scores", expectedRaw, finished.players().stream()
                .map(player -> history.total(finished.handNumber(), player.seat())).toList());
        assertEquals("Every exact zero earns 500; other totals are unchanged", expectedFinal, totals(finished));
        assertEquals("Restoring a complete checkpoint preserves the award without counting it twice",
                expectedFinal, totals(GameSession.restoreComplete(checkpoint).snapshot()));

        var classic = new GameSession.Snapshot(finished.phase(), finished.handIndex(), finished.handNumber(),
                finished.handSize(), finished.trump(), finished.dealerSeat(), finished.biddingSeat(),
                finished.currentSeat(), finished.bidsSubmitted(), finished.bidTotal(), finished.players(),
                finished.currentTrick(), finished.lastTrick(), finished.lastTrickWinnerSeat(), true, mode,
                finished.history(), finished.turnedCard(), true, 10, 10);
        assertEquals("A classic match keeps zero in the final ranking", expectedRaw, totals(classic));
    }

    private static List<Integer> totals(GameSession.Snapshot state) {
        return state.players().stream().map(player -> state.matchScore(player.seat())).toList();
    }
}
