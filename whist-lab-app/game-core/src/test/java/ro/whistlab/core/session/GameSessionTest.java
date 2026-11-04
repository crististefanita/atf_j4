package ro.whistlab.core.session;

import ro.whistlab.core.ai.policy.AiPolicy;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;

public class GameSessionTest {
    @Test
    public void ohHellTrumpAndInitialDealerVaryBySeedAndRemainReproducible() {
        Set<Trump> trumps = new java.util.HashSet<>();
        Set<Integer> dealers = new java.util.HashSet<>();
        for (int seed = 0; seed < 100; seed++) {
            GameConfig config = GameConfig.defaults(0x9E3779B9L * seed);
            GameSession.Snapshot first = GameSession.newGame(config).snapshot();
            GameSession.Snapshot repeated = GameSession.newGame(config).snapshot();
            assertEquals(first.trump(), repeated.trump());
            assertEquals(first.dealerSeat(), repeated.dealerSeat());
            assertTrue(first.trump() != Trump.NO_TRUMP);
            trumps.add(first.trump()); dealers.add(first.dealerSeat());
        }
        assertEquals(4, trumps.size()); assertEquals(4, dealers.size());
    }
    @Test
    public void seededNewGameDealsUniqueCardsAndCanBeRecreated() {
        GameConfig config = new GameConfig(4, GameMode.DESCENDING, AiDifficulty.NORMAL, 42L);
        GameSession.Snapshot first = GameSession.newGame(config).snapshot();
        GameSession.Snapshot second = GameSession.newGame(config).snapshot();
        List<Card> dealt = first.players().stream().flatMap(player -> player.hand().stream()).toList();

        assertEquals(GamePhase.BIDDING, first.phase());
        assertEquals(13, first.handSize());
        assertEquals(52, dealt.size());
        assertEquals(52, Set.copyOf(dealt).size());
        assertEquals(first.players().stream().map(GameSession.PlayerSnapshot::hand).toList(),
                second.players().stream().map(GameSession.PlayerSnapshot::hand).toList());
        assertNotEquals(GameSession.newGame(new GameConfig(4, GameMode.DESCENDING, AiDifficulty.NORMAL, 43L))
                .snapshot().players().stream().map(GameSession.PlayerSnapshot::hand).toList(),
                first.players().stream().map(GameSession.PlayerSnapshot::hand).toList());
    }

    @Test
    public void biddingRejectsWrongSeatOutOfRangeAndForbiddenFinalBid() {
        GameSession game = oneTrickFixture();
        assertThrows(IllegalStateException.class, () -> game.submitBid(1, 0));
        assertThrows(IllegalArgumentException.class, () -> game.submitBid(0, 2));
        game.submitBid(0, 1);
        game.submitBid(1, 0);
        game.submitBid(2, 0);
        assertThrows(IllegalArgumentException.class, () -> game.submitBid(3, 0));
        game.submitBid(3, 1);
        assertEquals(GamePhase.PLAYING, game.snapshot().phase());
        assertEquals(0, game.snapshot().currentSeat());
    }

    @Test
    public void cannotPlayOutOfTurnOrPlayACardNotOwned() {
        GameSession game = oneTrickFixture();
        submitFixtureBids(game);
        assertThrows(IllegalStateException.class,
                () -> game.playCard(1, new Card(Suit.HEARTS, 13)));
        game.playCard(0, new Card(Suit.HEARTS, 14));
        assertThrows(IllegalArgumentException.class,
                () -> game.playCard(1, new Card(Suit.SPADES, 2)));
        assertEquals(1, game.snapshot().players().get(1).hand().size());
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
        assertThrows(IllegalArgumentException.class, () -> game.playCard(1, new Card(Suit.SPADES, 3)));
        assertEquals(before, game.snapshot());
        assertEquals(1, game.snapshot().currentSeat());
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
        assertEquals(GamePhase.SCORE, result.phase());
        assertEquals(0, result.lastTrickWinnerSeat());
        assertEquals(11, result.players().get(0).totalScore());
        assertEquals(1, result.players().get(0).tricksWon());
        assertEquals(List.of(11, 10, 10, 0), result.players().stream()
                .map(GameSession.PlayerSnapshot::totalScore).toList());
        assertFalse(result.finalHand());
        game.startNextHand();
        assertEquals(2, game.snapshot().handSize());
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
        assertEquals(0, game.snapshot().currentSeat());

        game.playCard(0, new Card(Suit.HEARTS, 14));
        game.playCard(1, new Card(Suit.HEARTS, 13));
        game.playCard(2, new Card(Suit.HEARTS, 12));
        game.playCard(3, new Card(Suit.HEARTS, 11));

        assertEquals(0, game.snapshot().lastTrickWinnerSeat());
        assertEquals(0, game.snapshot().currentSeat());
        assertEquals(1, game.snapshot().players().get(0).tricksWon());
    }

    @Test
    public void allSupportedPlayerCountsReceiveEqualLegalHands() {
        for (int count = 3; count <= 6; count++) {
            GameSession.Snapshot game = GameSession.newGame(
                    new GameConfig(count, GameMode.DESCENDING, AiDifficulty.NORMAL, count)).snapshot();
            assertEquals(count, game.players().size());
            assertTrue(game.players().stream().allMatch(player -> player.hand().size() == game.handSize()));
            Set<Card> cards = game.players().stream().flatMap(player -> player.hand().stream()).collect(Collectors.toSet());
            assertEquals(count * game.handSize(), cards.size());
        }
    }

    @Test
    public void aiCanChooseLegalBidsAndCardsAcrossDeterministicDeals() {
        for (int seed = 0; seed < 30; seed++) {
            GameSession game = GameSession.newGame(new GameConfig(4, GameMode.REVERSE_OH_HELL,
                    AiDifficulty.NORMAL, seed));
            while (game.snapshot().phase() == GamePhase.BIDDING) {
                GameSession.Snapshot state = game.snapshot();
                int seat = state.biddingSeat();
                game.submitBid(seat, seat == 0 ? 0 : AiPolicy.chooseBid(state, seat));
            }
            while (game.snapshot().phase() == GamePhase.PLAYING) {
                GameSession.Snapshot state = game.snapshot();
                int seat = state.currentSeat();
                Card choice = AiPolicy.chooseCard(state, seat);
                assertTrue(GameRules.legalCards(state.players().get(seat).hand(),
                        state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit())
                        .contains(choice));
                game.playCard(seat, choice);
            }
            assertEquals(GamePhase.SCORE, game.snapshot().phase());
        }
    }

    @Test
    public void normalPursuesItsContractWhileHardUsesAcceptedPlanner() {
        GameSession oneCard = oneTrickFixture();
        assertEquals(0, AiPolicy.chooseBid(oneCard.snapshot(), 1, AiDifficulty.NORMAL));
        assertEquals(ro.whistlab.core.baseline.v11.RoundElevenAiPolicy.chooseBid(oneCard.snapshot(), 1, AiDifficulty.EXPERT),
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
        assertEquals(ro.whistlab.core.baseline.v11.RoundElevenAiPolicy.chooseCard(state, 1, AiDifficulty.EXPERT),
                AiPolicy.chooseCard(state, 1, AiDifficulty.HARD));
    }

    @Test
    public void aiCanCompleteEveryDescendingHandForAllSupportedTableSizes() {
        for (int playerCount = 3; playerCount <= 6; playerCount++) {
            GameSession game = GameSession.newGame(new GameConfig(playerCount,
                    GameMode.DESCENDING, AiDifficulty.HARD, 0x51A7L + playerCount));
            int previousDealer = game.snapshot().dealerSeat();
            int expectedHandIndex = 0;
            while (true) {
                GameSession.Snapshot handStart = game.snapshot();
                assertEquals(expectedHandIndex, handStart.handIndex());
                assertEquals(Trump.forHandIndex(expectedHandIndex), handStart.trump());
                assertTrue(handStart.players().stream()
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
                    assertTrue(GameRules.legalCards(state.players().get(seat).hand(), ledSuit).contains(choice));
                    game.playCard(seat, choice);
                }

                GameSession.Snapshot completed = game.snapshot();
                assertEquals(GamePhase.SCORE, completed.phase());
                assertEquals(handStart.handSize(), completed.players().stream()
                        .mapToInt(GameSession.PlayerSnapshot::tricksWon).sum());
                for (GameSession.PlayerSnapshot player : completed.players()) {
                    assertTrue(player.hand().isEmpty());
                    assertEquals(scoresBeforeHand[player.seat()] + GameRules.scoreDelta(
                                    player.tricksWon(), player.bid()), player.totalScore());
                }
                if (completed.finalHand()) break;

                previousDealer = completed.dealerSeat();
                game.startNextHand();
                expectedHandIndex++;
                assertEquals((previousDealer + 1) % playerCount, game.snapshot().dealerSeat());
            }
        }
    }

    @Test
    public void ohHellCandidateGamesCompleteEveryHandForAllObservedPlayerCounts() {
        for (int playerCount = 3; playerCount <= 6; playerCount++) {
            GameSession game = GameSession.newGame(new GameConfig(playerCount, GameMode.OH_HELL,
                    AiDifficulty.HARD, 0x0E311L + playerCount));
            List<Integer> expectedHandSizes = GameRules.handSizes(GameMode.OH_HELL, playerCount);
            for (int handIndex = 0; handIndex < expectedHandSizes.size(); handIndex++) {
                int expectedHandSize = expectedHandSizes.get(handIndex);
                GameSession.Snapshot start = game.snapshot();
                assertEquals(handIndex, start.handIndex());
                assertEquals(expectedHandSize, start.handSize());
                int previousDealer = start.dealerSeat();
                int[] scoresBefore = start.players().stream()
                        .mapToInt(GameSession.PlayerSnapshot::totalScore).toArray();
                Set<Card> dealtCards = start.players().stream().flatMap(player -> player.hand().stream())
                        .collect(Collectors.toSet());
                assertEquals(playerCount * expectedHandSize, dealtCards.size());
                assertTrue(start.players().stream().allMatch(player -> player.hand().size() == expectedHandSize));

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
                    assertTrue(GameRules.legalCards(state.players().get(seat).hand(), ledSuit).contains(choice));
                    game.playCard(seat, choice);
                }
                GameSession.Snapshot score = game.snapshot();
                assertEquals(GamePhase.SCORE, score.phase());
                assertEquals(expectedHandSize, score.players().stream()
                        .mapToInt(GameSession.PlayerSnapshot::tricksWon).sum());
                for (GameSession.PlayerSnapshot player : score.players()) {
                    assertTrue(player.hand().isEmpty());
                    assertEquals(scoresBefore[player.seat()] + GameRules.scoreDelta(
                            player.tricksWon(), player.bid()), player.totalScore());
                }
                boolean finalHand = handIndex == expectedHandSizes.size() - 1;
                assertEquals(finalHand, score.finalHand());
                if (finalHand) {
                    assertThrows(IllegalStateException.class, game::startNextHand);
                } else {
                    game.startNextHand();
                    assertEquals((previousDealer + 1) % playerCount, game.snapshot().dealerSeat());
                }
            }
        }
    }

    @Test
    public void saveAndRestorePreservesBiddingMidTrickAndFinalScoreState() {
        GameSession game = oneTrickFixture();
        assertEquals(game.snapshot(), GameSession.restore(game.saveState()).snapshot());

        submitFixtureBids(game);
        game.playCard(0, new Card(Suit.HEARTS, 14));
        GameSession restored = GameSession.restore(game.saveState());
        assertEquals(game.snapshot(), restored.snapshot());

        game.playCard(1, new Card(Suit.HEARTS, 13));
        restored.playCard(1, new Card(Suit.HEARTS, 13));
        game.playCard(2, new Card(Suit.HEARTS, 12));
        restored.playCard(2, new Card(Suit.HEARTS, 12));
        game.playCard(3, new Card(Suit.HEARTS, 11));
        restored.playCard(3, new Card(Suit.HEARTS, 11));
        assertEquals(GamePhase.SCORE, restored.snapshot().phase());
        assertEquals(game.snapshot(), restored.snapshot());
    }

    @Test
    public void restoreRejectsCheckpointWithMismatchedHandSize() {
        GameSession.SaveState saved = oneTrickFixture().saveState();
        GameSession.SaveState malformed = new GameSession.SaveState(saved.config(), saved.phase(),
                saved.handIndex(), saved.handSize() + 1, saved.dealerSeat(), saved.biddingSeat(),
                saved.currentSeat(), saved.bidsSubmitted(), saved.bidTotal(), saved.trump(),
                saved.lastTrickWinnerSeat(), saved.players(), saved.currentTrick(), saved.lastTrick());
        assertThrows(IllegalArgumentException.class, () -> GameSession.restore(malformed));
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
