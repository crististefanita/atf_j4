package ro.whistlab.core.session;

import ro.whistlab.core.ai.policy.AiPolicy;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;

import static atf.gradle.audit.assertions.GradleAssertions.assertEquals;
import static atf.gradle.audit.assertions.GradleAssertions.assertFalse;
import static atf.gradle.audit.assertions.GradleAssertions.assertNotEquals;
import static atf.gradle.audit.assertions.GradleAssertions.assertThrows;
import static atf.gradle.audit.assertions.GradleAssertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;

public class GameSessionTest extends atf.gradle.audit.GradleAuditedTest {
    @Test
    public void ohHellTrumpAndInitialDealerVaryBySeedAndRemainReproducible() {
        Set<Trump> trumps = new java.util.HashSet<>();
        Set<Integer> dealers = new java.util.HashSet<>();
        for (int seed = 0; seed < 100; seed++) {
            GameConfig config = GameConfig.defaults(0x9E3779B9L * seed);
            GameSession.Snapshot first = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", config)).snapshot();
            GameSession.Snapshot repeated = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", config)).snapshot();

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "config", config, "dealers", dealers, "first", first, "repeated", repeated, "seed", seed, "trumps", trumps);
            assertEquals("repeated.trump() must equal first.trump()", first.trump(), repeated.trump());
            assertEquals("repeated.dealerSeat() must equal first.dealerSeat()", first.dealerSeat(), repeated.dealerSeat());
            assertTrue("first.trump() != Trump.NO_TRUMP must hold", first.trump() != Trump.NO_TRUMP);
            trumps.add(first.trump()); dealers.add(first.dealerSeat());
        }
        assertEquals("trumps.size() must equal 4", 4, trumps.size()); assertEquals("dealers.size() must equal 4", 4, dealers.size());
    }
    @Test
    public void seededNewGameDealsUniqueCardsAndCanBeRecreated() {
        GameConfig config = new GameConfig(4, GameMode.DESCENDING, AiDifficulty.NORMAL, 42L);
        GameSession.Snapshot first = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", config)).snapshot();
        GameSession.Snapshot second = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", config)).snapshot();
        List<Card> dealt = first.players().stream().flatMap(player -> player.hand().stream()).toList();



        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "config", config, "dealt", dealt, "first", first, "second", second);
        assertEquals("first.phase() must equal GamePhase.BIDDING", GamePhase.BIDDING, first.phase());
        assertEquals("first.handSize() must equal 13", 13, first.handSize());
        assertEquals("dealt.size() must equal 52", 52, dealt.size());
        assertEquals("Set.copyOf(dealt).size() must equal 52", 52, Set.copyOf(dealt).size());
        assertEquals("second.players().stream().map(GameSession.PlayerSnapshot::hand).toList() must equal first.players().stream().map(GameSession.PlayerSnapshot::hand).toList()", first.players().stream().map(GameSession.PlayerSnapshot::hand).toList(),
                second.players().stream().map(GameSession.PlayerSnapshot::hand).toList());
        assertNotEquals("first.players().stream().map(GameSession.PlayerSnapshot::hand).toList() must differ from the declared component operation", GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", new GameConfig(4, GameMode.DESCENDING, AiDifficulty.NORMAL, 43L)))
                .snapshot().players().stream().map(GameSession.PlayerSnapshot::hand).toList(),
                first.players().stream().map(GameSession.PlayerSnapshot::hand).toList());
    }

    @Test
    public void biddingRejectsWrongSeatOutOfRangeAndForbiddenFinalBid() {
        GameSession game = oneTrickFixture();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game);
        assertThrows("The operation rejects its input with IllegalStateException.class", IllegalStateException.class, () -> game.submitBid(1, 0));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> game.submitBid(0, 2));
        game.submitBid(0, 1);
        game.submitBid(1, 0);
        game.submitBid(2, 0);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> game.submitBid(3, 0));
        game.submitBid(3, 1);
        assertEquals("game.snapshot().phase() must equal GamePhase.PLAYING", GamePhase.PLAYING, game.snapshot().phase());
        assertEquals("game.snapshot().currentSeat() must equal 0", 0, game.snapshot().currentSeat());
    }

    @Test
    public void cannotPlayOutOfTurnOrPlayACardNotOwned() {
        GameSession game = oneTrickFixture();
        submitFixtureBids(game);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game);
        assertThrows("The operation rejects its input with IllegalStateException.class", IllegalStateException.class,
                () -> game.playCard(1, new Card(Suit.HEARTS, 13)));
        game.playCard(0, new Card(Suit.HEARTS, 14));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,
                () -> game.playCard(1, new Card(Suit.SPADES, 2)));
        assertEquals("game.snapshot().players().get(1).hand().size() must equal 1", 1, game.snapshot().players().get(1).hand().size());
    }

    @Test
    public void heldOffSuitCardIsRejectedWithoutMutatingTheTurnWhenLedSuitIsAvailable() {
        List<List<Card>> hands = List.of(
                List.of(new Card(Suit.HEARTS, 14), new Card(Suit.SPADES, 2)),
                List.of(new Card(Suit.HEARTS, 13), new Card(Suit.SPADES, 3)),
                List.of(new Card(Suit.HEARTS, 12), new Card(Suit.SPADES, 4)),
                List.of(new Card(Suit.HEARTS, 11), new Card(Suit.SPADES, 5)));
        GameSession game = GameSession.fromDeal(new GameConfig(4, GameMode.REVERSE_OH_HELL, AiDifficulty.NORMAL, 7L),
                hands, Trump.SPADES, 3);
        for (int seat = 0; seat < 4; seat++) game.submitBid(seat, 0);
        game.playCard(0, new Card(Suit.HEARTS, 14));
        GameSession.Snapshot before = game.snapshot();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "before", before, "game", game, "hands", hands);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> game.playCard(1, new Card(Suit.SPADES, 3)));
        assertEquals("game.snapshot() must equal before", before, game.snapshot());
        assertEquals("game.snapshot().currentSeat() must equal 1", 1, game.snapshot().currentSeat());
    }

    @Test
    public void exactBidFirstTrickUpdatesTheCorrectWinnerAndScoreSheet() {
        GameSession game = oneTrickFixture();
        submitFixtureBids(game);

        game.playCard(0, new Card(Suit.HEARTS, 14));
        game.playCard(1, new Card(Suit.HEARTS, 13));
        game.playCard(2, new Card(Suit.HEARTS, 12));
        game.playCard(3, new Card(Suit.HEARTS, 11));

        GameSession.Snapshot result = game.snapshot();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game, "result", result);
        assertEquals("result.phase() must equal GamePhase.SCORE", GamePhase.SCORE, result.phase());
        assertEquals("result.lastTrickWinnerSeat() must equal 0", 0, result.lastTrickWinnerSeat());
        assertEquals("result.players().get(0).totalScore() must equal 11", 11, result.players().get(0).totalScore());
        assertEquals("result.players().get(0).tricksWon() must equal 1", 1, result.players().get(0).tricksWon());
        assertEquals("result.players().stream().map(GameSession.PlayerSnapshot::totalScore).toList() must equal List.of(11, 10, 10, 0)", List.of(11, 10, 10, 0), result.players().stream()
                .map(GameSession.PlayerSnapshot::totalScore).toList());
        assertFalse("result.finalHand() must remain false", result.finalHand());
        game.startNextHand();
        assertEquals("game.snapshot().handSize() must equal 2", 2, game.snapshot().handSize());
    }

    @Test
    public void trickWinnerLeadsTheNextTrickAndAceBeatsLowerLedSuitCards() {
        List<List<Card>> hands = List.of(
                List.of(new Card(Suit.HEARTS, 14), new Card(Suit.CLUBS, 2)),
                List.of(new Card(Suit.HEARTS, 13), new Card(Suit.DIAMONDS, 2)),
                List.of(new Card(Suit.HEARTS, 12), new Card(Suit.SPADES, 2)),
                List.of(new Card(Suit.HEARTS, 11), new Card(Suit.CLUBS, 3)));
        GameSession game = GameSession.fromDeal(new GameConfig(4, GameMode.REVERSE_OH_HELL,
                AiDifficulty.NORMAL, 3L), hands, Trump.SPADES, 3);
        for (int seat = 0; seat < 4; seat++) game.submitBid(seat, 0);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game, "hands", hands);
        assertEquals("game.snapshot().currentSeat() must equal 0", 0, game.snapshot().currentSeat());

        game.playCard(0, new Card(Suit.HEARTS, 14));
        game.playCard(1, new Card(Suit.HEARTS, 13));
        game.playCard(2, new Card(Suit.HEARTS, 12));
        game.playCard(3, new Card(Suit.HEARTS, 11));

        assertEquals("game.snapshot().lastTrickWinnerSeat() must equal 0", 0, game.snapshot().lastTrickWinnerSeat());
        assertEquals("game.snapshot().currentSeat() must equal 0", 0, game.snapshot().currentSeat());
        assertEquals("game.snapshot().players().get(0).tricksWon() must equal 1", 1, game.snapshot().players().get(0).tricksWon());
    }

    @Test
    public void allSupportedPlayerCountsReceiveEqualLegalHands() {
        for (int count = 3; count <= 6; count++) {
            GameSession.Snapshot game = GameSession.newGame(
                    atf.gradle.audit.GradleInputs.created("Game configuration before creation", new GameConfig(count, GameMode.DESCENDING, AiDifficulty.NORMAL, count))).snapshot();

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "count", count, "game", game);
            assertEquals("game.players().size() must equal count", count, game.players().size());
            assertTrue("game.players().stream().allMatch((player)->player.hand().size() == game.handSize()) must hold", game.players().stream().allMatch(player -> player.hand().size() == game.handSize()));
            Set<Card> cards = game.players().stream().flatMap(player -> player.hand().stream()).collect(Collectors.toSet());
            assertEquals("cards.size() must equal count * game.handSize()", count * game.handSize(), cards.size());
        }
    }

    @Test
    public void aiCanChooseLegalBidsAndCardsAcrossDeterministicDeals() {
        for (int seed = 0; seed < 30; seed++) {
            GameSession game = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", new GameConfig(4, GameMode.REVERSE_OH_HELL,
                    AiDifficulty.NORMAL, seed)));
            while (game.snapshot().phase() == GamePhase.BIDDING) {
                GameSession.Snapshot state = game.snapshot();
                int seat = state.biddingSeat();
                game.submitBid(seat, seat == 0 ? 0 : AiPolicy.chooseBid(state, seat));
            }
            while (game.snapshot().phase() == GamePhase.PLAYING) {
                GameSession.Snapshot state = game.snapshot();
                int seat = state.currentSeat();
                Card choice = AiPolicy.chooseCard(state, seat);

                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "choice", choice, "game", game, "seat", seat, "seed", seed, "state", state);
                assertTrue("GameRules.legalCards(state.players().get(seat).hand(), state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit()).contains(choice) must hold", GameRules.legalCards(state.players().get(seat).hand(),
                        state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit())
                        .contains(choice));
                game.playCard(seat, choice);
            }
            assertEquals("game.snapshot().phase() must equal GamePhase.SCORE", GamePhase.SCORE, game.snapshot().phase());
        }
    }

    @Test
    public void normalPursuesItsContractWhileHardUsesAcceptedPlanner() {
        GameSession oneCard = oneTrickFixture();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "oneCard", oneCard);
        assertEquals("AiPolicy.chooseBid(oneCard.snapshot(), 1, AiDifficulty.NORMAL) must equal 0", 0, AiPolicy.chooseBid(oneCard.snapshot(), 1, AiDifficulty.NORMAL));
        assertEquals("AiPolicy.chooseBid(oneCard.snapshot(), 1, AiDifficulty.HARD) must equal ro.whistlab.core.baseline.v11.RoundElevenAiPolicy.chooseBid(oneCard.snapshot(), 1, AiDifficulty.EXPERT)", ro.whistlab.core.baseline.v11.RoundElevenAiPolicy.chooseBid(oneCard.snapshot(), 1, AiDifficulty.EXPERT),
                AiPolicy.chooseBid(oneCard.snapshot(), 1, AiDifficulty.HARD));

        List<List<Card>> hands = List.of(
                List.of(new Card(Suit.HEARTS, 10), new Card(Suit.CLUBS, 2)),
                List.of(new Card(Suit.HEARTS, 9), new Card(Suit.HEARTS, 13)),
                List.of(new Card(Suit.DIAMONDS, 2), new Card(Suit.DIAMONDS, 3)),
                List.of(new Card(Suit.SPADES, 2), new Card(Suit.SPADES, 3)));
        GameSession game = GameSession.fromDeal(
                new GameConfig(4, GameMode.DESCENDING, AiDifficulty.HARD, 7L), hands, Trump.SPADES, 3);
        game.submitBid(0, 0);
        game.submitBid(1, 1);
        game.submitBid(2, 0);
        game.submitBid(3, 0);
        game.playCard(0, new Card(Suit.HEARTS, 10));

        GameSession.Snapshot state = game.snapshot();
        assertEquals("Normal bid1 must take the reachable heart trick", new Card(Suit.HEARTS, 13),
                AiPolicy.chooseCard(state, 1, AiDifficulty.NORMAL));
        assertEquals("AiPolicy.chooseCard(state, 1, AiDifficulty.HARD) must equal ro.whistlab.core.baseline.v11.RoundElevenAiPolicy.chooseCard(state, 1, AiDifficulty.EXPERT)", ro.whistlab.core.baseline.v11.RoundElevenAiPolicy.chooseCard(state, 1, AiDifficulty.EXPERT),
                AiPolicy.chooseCard(state, 1, AiDifficulty.HARD));
    }

    @Test
    public void aiCanCompleteEveryDescendingHandForAllSupportedTableSizes() {
        for (int playerCount = 3; playerCount <= 6; playerCount++) {
            GameSession game = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", new GameConfig(playerCount,
                    GameMode.DESCENDING, AiDifficulty.HARD, 0x51A7L + playerCount)));
            int previousDealer = game.snapshot().dealerSeat();
            int expectedHandIndex = 0;
            while (true) {
                GameSession.Snapshot handStart = game.snapshot();

                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "expectedHandIndex", expectedHandIndex, "game", game, "handStart", handStart, "playerCount", playerCount, "previousDealer", previousDealer);
                assertEquals("handStart.handIndex() must equal expectedHandIndex", expectedHandIndex, handStart.handIndex());
                assertEquals("handStart.trump() must equal Trump.forHandIndex(expectedHandIndex)", Trump.forHandIndex(expectedHandIndex), handStart.trump());
                assertTrue("handStart.players().stream().allMatch((player)->player.hand().size() == handStart.handSize()) must hold", handStart.players().stream()
                        .allMatch(player -> player.hand().size() == handStart.handSize()));
                int[] scoresBeforeHand = handStart.players().stream()
                        .mapToInt(GameSession.PlayerSnapshot::totalScore).toArray();

                while (game.snapshot().phase() == GamePhase.BIDDING) {
                    GameSession.Snapshot state = game.snapshot();
                    int seat = state.biddingSeat();
                    game.submitBid(seat, AiPolicy.chooseBid(state, seat, AiDifficulty.HARD));
                }
                while (game.snapshot().phase() == GamePhase.PLAYING) {
                    GameSession.Snapshot state = game.snapshot();
                    int seat = state.currentSeat();
                    Suit ledSuit = state.currentTrick().isEmpty()
                            ? null : state.currentTrick().get(0).card().suit();
                    Card choice = AiPolicy.chooseCard(state, seat, AiDifficulty.HARD);
                    assertTrue("GameRules.legalCards(state.players().get(seat).hand(), ledSuit).contains(choice) must hold", GameRules.legalCards(state.players().get(seat).hand(), ledSuit).contains(choice));
                    game.playCard(seat, choice);
                }

                GameSession.Snapshot completed = game.snapshot();
                assertEquals("completed.phase() must equal GamePhase.SCORE", GamePhase.SCORE, completed.phase());
                assertEquals("completed.players().stream().mapToInt(GameSession.PlayerSnapshot::tricksWon).sum() must equal handStart.handSize()", handStart.handSize(), completed.players().stream()
                        .mapToInt(GameSession.PlayerSnapshot::tricksWon).sum());
                for (GameSession.PlayerSnapshot player : completed.players()) {
                    assertTrue("player.hand().isEmpty() must hold", player.hand().isEmpty());
                    assertEquals("player.totalScore() must equal scoresBeforeHand[player.seat()] + GameRules.scoreDelta(player.tricksWon(), player.bid())", scoresBeforeHand[player.seat()] + GameRules.scoreDelta(
                                    player.tricksWon(), player.bid()), player.totalScore());
                }
                if (completed.finalHand()) break;

                previousDealer = completed.dealerSeat();
                game.startNextHand();
                expectedHandIndex++;
                assertEquals("game.snapshot().dealerSeat() must equal (previousDealer + 1) % playerCount", (previousDealer + 1) % playerCount, game.snapshot().dealerSeat());
            }
        }
    }

    @Test
    public void ohHellCandidateGamesCompleteEveryHandForAllObservedPlayerCounts() {
        for (int playerCount = 3; playerCount <= 6; playerCount++) {
            GameSession game = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", new GameConfig(playerCount, GameMode.OH_HELL,
                    AiDifficulty.HARD, 0x0E311L + playerCount)));
            List<Integer> expectedHandSizes = GameRules.handSizes(GameMode.OH_HELL, playerCount);
            for (int handIndex = 0; handIndex < expectedHandSizes.size(); handIndex++) {
                int expectedHandSize = expectedHandSizes.get(handIndex);
                GameSession.Snapshot start = game.snapshot();

                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "expectedHandSize", expectedHandSize, "expectedHandSizes", expectedHandSizes, "game", game, "handIndex", handIndex, "playerCount", playerCount, "start", start);
                assertEquals("start.handIndex() must equal handIndex", handIndex, start.handIndex());
                assertEquals("start.handSize() must equal expectedHandSize", expectedHandSize, start.handSize());
                int previousDealer = start.dealerSeat();
                int[] scoresBefore = start.players().stream()
                        .mapToInt(GameSession.PlayerSnapshot::totalScore).toArray();
                Set<Card> dealtCards = start.players().stream().flatMap(player -> player.hand().stream())
                        .collect(Collectors.toSet());
                assertEquals("dealtCards.size() must equal playerCount * expectedHandSize", playerCount * expectedHandSize, dealtCards.size());
                assertTrue("start.players().stream().allMatch((player)->player.hand().size() == expectedHandSize) must hold", start.players().stream().allMatch(player -> player.hand().size() == expectedHandSize));

                while (game.snapshot().phase() == GamePhase.BIDDING) {
                    GameSession.Snapshot state = game.snapshot();
                    int seat = state.biddingSeat();
                    game.submitBid(seat, AiPolicy.chooseBid(state, seat, AiDifficulty.HARD));
                }
                while (game.snapshot().phase() == GamePhase.PLAYING) {
                    GameSession.Snapshot state = game.snapshot();
                    int seat = state.currentSeat();
                    Suit ledSuit = state.currentTrick().isEmpty()
                            ? null : state.currentTrick().getFirst().card().suit();
                    Card choice = AiPolicy.chooseCard(state, seat, AiDifficulty.HARD);
                    assertTrue("GameRules.legalCards(state.players().get(seat).hand(), ledSuit).contains(choice) must hold", GameRules.legalCards(state.players().get(seat).hand(), ledSuit).contains(choice));
                    game.playCard(seat, choice);
                }
                GameSession.Snapshot score = game.snapshot();
                assertEquals("score.phase() must equal GamePhase.SCORE", GamePhase.SCORE, score.phase());
                assertEquals("score.players().stream().mapToInt(GameSession.PlayerSnapshot::tricksWon).sum() must equal expectedHandSize", expectedHandSize, score.players().stream()
                        .mapToInt(GameSession.PlayerSnapshot::tricksWon).sum());
                for (GameSession.PlayerSnapshot player : score.players()) {
                    assertTrue("player.hand().isEmpty() must hold", player.hand().isEmpty());
                    assertEquals("player.totalScore() must equal scoresBefore[player.seat()] + GameRules.scoreDelta(player.tricksWon(), player.bid())", scoresBefore[player.seat()] + GameRules.scoreDelta(
                            player.tricksWon(), player.bid()), player.totalScore());
                }
                boolean finalHand = handIndex == expectedHandSizes.size() - 1;
                assertEquals("score.finalHand() must equal finalHand", finalHand, score.finalHand());
                if (finalHand) {
                    assertThrows("The operation rejects its input with IllegalStateException.class", IllegalStateException.class, game::startNextHand);
                } else {
                    game.startNextHand();
                    assertEquals("game.snapshot().dealerSeat() must equal (previousDealer + 1) % playerCount", (previousDealer + 1) % playerCount, game.snapshot().dealerSeat());
                }
            }
        }
    }

    @Test
    public void saveAndRestorePreservesBiddingMidTrickAndFinalScoreState() {
        GameSession game = oneTrickFixture();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game);
        assertEquals("GameSession.restore(game.saveState()).snapshot() must equal game.snapshot()", game.snapshot(), GameSession.restore(game.saveState()).snapshot());

        submitFixtureBids(game);
        game.playCard(0, new Card(Suit.HEARTS, 14));
        GameSession restored = GameSession.restore(game.saveState());
        assertEquals("restored.snapshot() must equal game.snapshot()", game.snapshot(), restored.snapshot());

        game.playCard(1, new Card(Suit.HEARTS, 13));
        restored.playCard(1, new Card(Suit.HEARTS, 13));
        game.playCard(2, new Card(Suit.HEARTS, 12));
        restored.playCard(2, new Card(Suit.HEARTS, 12));
        game.playCard(3, new Card(Suit.HEARTS, 11));
        restored.playCard(3, new Card(Suit.HEARTS, 11));
        assertEquals("restored.snapshot().phase() must equal GamePhase.SCORE", GamePhase.SCORE, restored.snapshot().phase());
        assertEquals("restored.snapshot() must equal game.snapshot()", game.snapshot(), restored.snapshot());
    }

    @Test
    public void restoreRejectsCheckpointWithMismatchedHandSize() {
        GameSession.SaveState saved = oneTrickFixture().saveState();
        GameSession.SaveState malformed = new GameSession.SaveState(saved.config(), saved.phase(),
                saved.handIndex(), saved.handSize() + 1, saved.dealerSeat(), saved.biddingSeat(),
                saved.currentSeat(), saved.bidsSubmitted(), saved.bidTotal(), saved.trump(),
                saved.lastTrickWinnerSeat(), saved.players(), saved.currentTrick(), saved.lastTrick());

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "malformed", malformed, "saved", saved);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> GameSession.restore(malformed));
    }

    private static GameSession oneTrickFixture() {
        GameConfig config = new GameConfig(4, GameMode.REVERSE_OH_HELL, AiDifficulty.NORMAL, 1L);
        List<List<Card>> hands = List.of(
                List.of(new Card(Suit.HEARTS, 14)),
                List.of(new Card(Suit.HEARTS, 13)),
                List.of(new Card(Suit.HEARTS, 12)),
                List.of(new Card(Suit.HEARTS, 11)));
        return GameSession.fromDeal(config, hands, Trump.SPADES, 3);
    }

    private static void submitFixtureBids(GameSession game) {
        game.submitBid(0, 1);
        game.submitBid(1, 0);
        game.submitBid(2, 0);
        game.submitBid(3, 1);
    }
}
