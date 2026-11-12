package ro.whistlab.core.rules.documentation;

import atf.gradle.audit.GradleAuditedTest;
import atf.gradle.audit.GradleInputs;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.Test;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

import static atf.gradle.audit.assertions.GradleAssertions.assertEquals;
import static atf.gradle.audit.assertions.GradleAssertions.assertThrows;

public class DocumentedSessionComponentTest extends GradleAuditedTest {
    @Test public void biddingMovesClockwiseAndOnlyTheDealerCannotTieTheHandSize() {
        var game = twoCardDeal();
        GradleInputs.fixture("Licitarea / Bidding", "initial deal", game.snapshot(), "accepted bids", List.of(2, 0, 1));
        assertEquals("Bidding begins to the dealer's left", 2, game.snapshot().biddingSeat());
        assertThrows("A bid below zero is rejected", IllegalArgumentException.class, () -> game.submitBid(2, -1));
        assertThrows("A bid above the hand size is rejected", IllegalArgumentException.class, () -> game.submitBid(2, 3));
        game.submitBid(2, 2);
        assertEquals("An intermediate bid total may equal the hand size", 2, game.snapshot().bidTotal());
        assertEquals("Bidding wraps clockwise to seat zero", 0, game.snapshot().biddingSeat());
        game.submitBid(0, 0);
        assertEquals("The dealer is the last bidder", 1, game.snapshot().biddingSeat());
        var beforeForbidden = game.snapshot();
        assertThrows("A final zero would leave the total at two", IllegalArgumentException.class,
                () -> game.submitBid(1, 0));
        assertEquals("Rejecting a bid preserves the turn and all accepted bids", beforeForbidden, game.snapshot());
        game.submitBid(1, 1);
        assertEquals("Completing the bids starts play", GamePhase.PLAYING, game.snapshot().phase());
        assertEquals("The seat left of the dealer leads first", 2, game.snapshot().currentSeat());
    }

    @Test public void theWinnerLeadsNextAndEachCardAndHandIsCountedOnce() {
        var game = twoCardDeal();
        game.submitBid(2, 2);
        game.submitBid(0, 0);
        game.submitBid(1, 1);
        var aceHearts = new Card(Suit.HEARTS, 14);
        var lowTrump = new Card(Suit.SPADES, 9);
        GradleInputs.fixture("Levata și punctajul / Trick and scoring", "bid deal", game.snapshot(),
                "led card", aceHearts, "winning trump", lowTrump, "expected totals", List.of(5, -1, -2));
        assertEquals("The first leader may play either card", List.of(9, 14),
                GameRules.legalCards(game.snapshot().players().get(2).hand(), null, Trump.SPADES,
                        GameMode.ROMANIAN_WHIST).stream().map(Card::rank).sorted().toList());
        game.playCard(2, aceHearts);
        assertThrows("A player must follow hearts while holding a lower heart", IllegalArgumentException.class,
                () -> game.playCard(0, new Card(Suit.CLUBS, 14)));
        game.playCard(0, new Card(Suit.HEARTS, 10));
        assertEquals("Scoring waits for a card from every seat", -1, game.snapshot().lastTrickWinnerSeat());
        game.playCard(1, lowTrump);
        var firstTrick = game.snapshot();
        assertEquals("A completed trick has one contribution per seat", List.of(2, 0, 1),
                firstTrick.lastTrick().stream().map(play -> play.seat()).toList());
        assertEquals("A low trump beats the led ace", 1, firstTrick.lastTrickWinnerSeat());
        assertEquals("The winner leads the next trick", 1, firstTrick.currentSeat());
        assertThrows("The winner cannot reuse the spent trump", IllegalArgumentException.class,
                () -> game.playCard(1, lowTrump));
        assertEquals("Rejecting a replay does not spend another card", firstTrick, game.snapshot());
        game.playCard(1, new Card(Suit.DIAMONDS, 14));
        game.playCard(2, new Card(Suit.HEARTS, 9));
        game.playCard(0, new Card(Suit.CLUBS, 14));
        var scored = game.snapshot();
        assertEquals("An off-suit ace does not take the trick", 1, scored.lastTrickWinnerSeat());
        assertEquals("Six distinct cards were played", 6L,
                scored.history().stream().map(play -> play.card()).distinct().count());
        assertEquals("A hand creates exactly one ledger entry", 1, game.scoreHistory().completedHands());
        assertEquals("Exact zero, surplus and shortfall are scored separately", List.of(5, -1, -2),
                scored.players().stream().map(GameSession.PlayerSnapshot::totalScore).toList());
        assertThrows("Play cannot reopen the scored hand", IllegalStateException.class,
                () -> game.playCard(0, new Card(Suit.CLUBS, 14)));
        assertEquals("The scored hand remains a single entry", 1, game.scoreHistory().completedHands());
    }

    @Test public void zeroChangesOnlyTheFinalRomanianResultWhenEnabled() {
        var players = List.of(player(0, 0), player(1, -1), player(2, 0), player(3, 1));
        var raw = List.of(0, -1, 0, 1);
        var awarded = List.of(500, -1, 500, 1);
        GradleInputs.fixture("Joc la 0 / Playing for zero", "raw seat totals", raw,
                "expected final totals", awarded, "phases tested", List.of(GamePhase.PLAYING, GamePhase.SCORE));
        for (var mode : List.of(GameMode.ROMANIAN_WHIST, GameMode.LONG_ROMANIAN)) {
            var config = GameConfig.newMatch(4, mode, AiDifficulty.NORMAL, 31, true, 10);
            assertEquals(mode + " enables zero by default in a new match", true, config.allowZeroGame());
            assertEquals(mode + " leaves zero unchanged while playing the last hand", raw,
                    matchTotals(scoreState(players, mode, GamePhase.PLAYING, true, true)));
            assertEquals(mode + " leaves zero unchanged after an earlier hand", raw,
                    matchTotals(scoreState(players, mode, GamePhase.SCORE, false, true)));
            assertEquals(mode + " leaves zero unchanged when the option is disabled", raw,
                    matchTotals(scoreState(players, mode, GamePhase.SCORE, true, false)));
            var finished = scoreState(players, mode, GamePhase.SCORE, true, true);
            assertEquals(mode + " promotes every exact zero together and preserves other totals", awarded,
                    matchTotals(finished));
            assertEquals(mode + " leaves the raw totals in the snapshot", raw,
                    finished.players().stream().map(GameSession.PlayerSnapshot::totalScore).toList());
        }
        assertEquals("Oh Hell starts without the Romanian option", false,
                GameConfig.newMatch(4, GameMode.OH_HELL, AiDifficulty.NORMAL, 31, false, 10).allowZeroGame());
        assertThrows("Oh Hell cannot enable the Romanian option", IllegalArgumentException.class,
                () -> GameConfig.newMatch(4, GameMode.OH_HELL, AiDifficulty.NORMAL, 31, false, 10, true));
    }

    @Test public void otherProfilesKeepTheirOwnTrumpAndScoringRules() {
        var voidHand = List.of(new Card(Suit.SPADES, 7), new Card(Suit.DIAMONDS, 14));
        var cycle = List.of(Trump.SPADES, Trump.HEARTS, Trump.CLUBS, Trump.DIAMONDS, Trump.NO_TRUMP, Trump.SPADES);
        GradleInputs.fixture("Variante / Variants", "void hand", voidHand, "led suit", Suit.HEARTS,
                "trump", Trump.SPADES, "expected Descending cycle", cycle);
        assertEquals("Oh Hell lets a void player discard or trump", voidHand,
                GameRules.legalCards(voidHand, Suit.HEARTS, Trump.SPADES, GameMode.OH_HELL));
        assertEquals("An exact two in Oh Hell earns twelve", 12, GameRules.scoreDelta(2, 2, GameMode.OH_HELL));
        assertEquals("A miss in Oh Hell earns only the tricks won", 2, GameRules.scoreDelta(2, 3, GameMode.OH_HELL));
        assertEquals("Descending cycles independently of a turned card", false, GameMode.DESCENDING.usesTurnedTrump());
        assertEquals("The fixed cycle wraps after no trump", cycle,
                IntStream.range(0, 6).mapToObj(Trump::forHandIndex).toList());
    }

    private static GameSession twoCardDeal() {
        var config = GameConfig.newMatch(3, GameMode.ROMANIAN_WHIST, AiDifficulty.NORMAL, 31, false, 10);
        return GameSession.fromDeal(config, List.of(
                List.of(new Card(Suit.HEARTS, 10), new Card(Suit.CLUBS, 14)),
                List.of(new Card(Suit.SPADES, 9), new Card(Suit.DIAMONDS, 14)),
                List.of(new Card(Suit.HEARTS, 9), new Card(Suit.HEARTS, 14))), Trump.SPADES, 1);
    }

    private static GameSession.PlayerSnapshot player(int seat, int total) {
        return new GameSession.PlayerSnapshot(seat, "Seat " + seat, seat == 0, List.of(), 0, 0, total);
    }

    private static GameSession.Snapshot scoreState(List<GameSession.PlayerSnapshot> players, GameMode mode,
            GamePhase phase, boolean finalHand, boolean allowZero) {
        var schedule = GameRules.handSizes(mode, players.size());
        int handNumber = finalHand ? schedule.size() : schedule.size() - 1;
        int handSize = schedule.get(handNumber - 1);
        var trump = handSize == 8 ? Trump.NO_TRUMP : Trump.HEARTS;
        return new GameSession.Snapshot(phase, handNumber - 1, handNumber, handSize, trump, 0, 0, 0, 4, 0,
                players, List.of(), List.of(), -1, finalHand, mode, List.of(), null, true, 10, 10, allowZero);
    }

    private static List<Integer> matchTotals(GameSession.Snapshot snapshot) {
        return snapshot.players().stream().map(player -> snapshot.matchScore(player.seat())).toList();
    }
}
