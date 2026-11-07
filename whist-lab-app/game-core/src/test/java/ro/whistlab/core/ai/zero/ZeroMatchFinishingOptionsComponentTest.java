package ro.whistlab.core.ai.zero;

import static atf.gradle.audit.assertions.GradleAssertions.*;

import atf.gradle.audit.GradleAuditedTest;
import atf.gradle.audit.GradleInputs;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ro.whistlab.core.ai.engine.MonteCarloAi.Seat;
import ro.whistlab.core.ai.engine.MonteCarloAi.View;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.rules.RomanianScoring;

/** Brute-force final bids and trick counts, independent of the production counting formula. */
@RunWith(Parameterized.class)
public final class ZeroMatchFinishingOptionsComponentTest extends GradleAuditedTest {
    public record Series(boolean enabled, int bonus, int penalty) { }

    @Parameterized.Parameters(name = "{0}, {1} players, {2}")
    public static List<Object[]> cases() {
        var cases = new ArrayList<Object[]>();
        for (var mode : List.of(GameMode.ROMANIAN_WHIST, GameMode.LONG_ROMANIAN)) {
            for (int players = 3; players <= 6; players++) {
                for (var series : List.of(new Series(false, 5, 5), new Series(true, 5, 5),
                        new Series(true, 10, 10), new Series(true, 5, 10), new Series(true, 10, 5))) {
                    cases.add(new Object[]{mode, players, series});
                }
            }
        }
        return cases;
    }

    private final GameMode mode;
    private final int players;
    private final Series series;

    public ZeroMatchFinishingOptionsComponentTest(GameMode mode, int players, Series series) {
        this.mode = mode;
        this.players = players;
        this.series = series;
    }

    @Test public void finishingOptionsAndActivationMatchTheEnumeratedScorePaths() {
        var sizes = GameRules.handSizes(mode, players);
        int index = sizes.size() - 2;
        int currentSize = sizes.get(index);
        int finalSize = sizes.get(index + 1);
        var discrepancies = new ArrayList<String>();
        int compared = 0;
        int[] streaks = series.enabled() ? new int[]{-4, -3, 0, 3, 4} : new int[]{0};
        GradleInputs.fixture("Enumerated final contracts", "mode", mode, "players", players,
                "series", series, "scores", "-28 through 28", "streaks", streaks,
                "current size", currentSize, "final size", finalSize);
        for (int streak : streaks) {
            for (int score = -28; score <= 28; score++) {
                for (int bid = 0; bid <= currentSize; bid++) {
                    var objective = ZeroMatchObjective.forTurn(view(currentSize, score, bid, streak), index, List.of());
                    for (int won = 0; won <= currentSize; won++, compared++) {
                        var current = score(currentSize, bid, won, streak);
                        int expected = bruteForce(finalSize, score + current.totalPoints(), current.nextStreak());
                        int actual = objective.isPresent() ? objective.get().finishingOptions(bid, won) : 0;
                        boolean reachable = objective.isPresent() && objective.get().weight(bid, won) > 0;
                        if ((expected != actual || reachable != (expected > 0)) && discrepancies.size() < 8) {
                            discrepancies.add("score=" + score + ", streak=" + streak + ", bid=" + bid
                                    + ", won=" + won + ", expected=" + expected + ", actual=" + actual
                                    + ", reachable=" + reachable);
                        }
                    }
                }
            }
        }
        assertTrue("Enumerates every scored path in the configured matrix", compared > 100);
        assertEquals("Exact reachability and finishing options agree with enumeration", List.of(), discrepancies);
    }

    private int bruteForce(int size, int total, int streak) {
        int matches = 0;
        for (int bid = 0; bid <= size; bid++) {
            for (int won = 0; won <= size; won++) {
                if (total + score(size, bid, won, streak).totalPoints() == 0) matches++;
            }
        }
        return matches;
    }

    private RomanianScoring.Result score(int size, int bid, int won, int streak) {
        return series.enabled()
                ? RomanianScoring.score(won, bid, size, streak, series.bonus(), series.penalty())
                : new RomanianScoring.Result(GameRules.scoreDelta(won, bid, mode), 0, 0);
    }

    private View view(int size, int total, int bid, int streak) {
        var seats = new ArrayList<Seat>();
        seats.add(new Seat(size, bid, 0, total, streak));
        for (int i = 1; i < players; i++) seats.add(new Seat(size, 0, 0, 125, 0));
        return new View(mode, Trump.NO_TRUMP, 0, players - 1, size, List.of(), seats,
                List.of(), List.of(), null, series.enabled(), series.bonus(), series.penalty());
    }
}
