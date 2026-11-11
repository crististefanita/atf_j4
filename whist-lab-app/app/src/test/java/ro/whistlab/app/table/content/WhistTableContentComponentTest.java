package ro.whistlab.app.table.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import ro.cardtable.ui.cards.CardFace;
import ro.cardtable.ui.hand.HandCard;
import ro.cardtable.ui.seat.SeatContent;
import ro.cardtable.ui.table.PlayedCardContent;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.rules.HowToPlay;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;
import static atf.gradle.audit.assertions.GradleAssertions.*;

/** Whist rules and copy remain consumer contracts, never shared renderer fixtures. */
public final class WhistTableContentComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void seatFactsAndBidderProjectionMatchEveryWhistModeAndLanguage() {
        List<SeatCase> expected = new ArrayList<>(), observed = new ArrayList<>();
        for (var mode : GameMode.values()) for (int count = 3; count <= 6; count++)
            for (var language : HowToPlay.Language.values()) {
                var state = game(mode, count).snapshot();
                var copy = new WhistUiText(language);
                List<SeatContent> expectedSeats = state.players().stream().map(player -> {
                    boolean active = player.seat() == state.biddingSeat();
                    return new SeatContent(player.seat(), copy.seatName(player), List.of(
                            new SeatContent.Fact(copy.bidLabel() + ":", "—"),
                            new SeatContent.Fact(copy.seatWonLabel() + ":", "0")), active,
                            copy.seatAccessibility(player, player.seat() == state.dealerSeat(), active, true));
                }).toList();
                List<SeatContent> projectedSeats = state.players().stream()
                        .map(player -> WhistTableContent.seat(player, state.biddingSeat(), state.dealerSeat(), true, copy))
                        .toList();
                expected.add(new SeatCase(mode, count, language, expectedSeats));
                observed.add(new SeatCase(mode, count, language, projectedSeats));
            }

        atf.gradle.audit.GradleInputs.fixture("Bidder seat matrix", "modes", List.of(GameMode.values()),
                "playerCounts", List.of(3, 4, 5, 6), "languages", List.of(HowToPlay.Language.values()),
                "scenarios", observed.size());
        assertEquals("Seat names, facts, active state and accessible action match the bidding snapshot",
                expected, observed);
    }

    @Test public void actorMarkerStaysWithTheCurrentPlayerDuringManualAndAutomaticPlay() {
        List<ActorObservation> expected = new ArrayList<>(), observed = new ArrayList<>();
        for (var mode : GameMode.values()) for (int count = 3; count <= 6; count++)
            for (var language : HowToPlay.Language.values()) {
                var game = game(mode, count);
                var copy = new WhistUiText(language);
                while (game.snapshot().phase() == GamePhase.BIDDING) {
                    var state = game.snapshot();
                    addActorCases(expected, observed, state, state.biddingSeat(), true, mode, count, language, copy);
                    var allowed = GameRules.allowedBids(state.handSize(), state.bidTotal(),
                            state.bidsSubmitted() == state.players().size() - 1);
                    game.submitBid(state.biddingSeat(), allowed.get(0));
                }
                while (game.snapshot().phase() == GamePhase.PLAYING) {
                    var state = game.snapshot();
                    int actor = state.currentSeat();
                    addActorCases(expected, observed, state, actor, false, mode, count, language, copy);
                    addActorCases(expected, observed, state, -1, false, mode, count, language, copy, true);
                    var led = state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit();
                    var legal = GameRules.legalCards(state.players().get(actor).hand(), led,
                            state.trump(), state.mode());
                    game.playCard(actor, legal.get(0));
                }
                addActorCases(expected, observed, game.snapshot(), -1, false, mode, count, language, copy);
            }

        atf.gradle.audit.GradleInputs.fixture("Public actor-state matrix", "modes", List.of(GameMode.values()),
                "playerCounts", List.of(3, 4, 5, 6), "languages", List.of(HowToPlay.Language.values()),
                "observations", observed.size());
        assertEquals("Exactly the current actor is marked; AutoGame blocks input without hiding that actor",
                expected, observed);
    }

    private static void addActorCases(List<ActorObservation> expected, List<ActorObservation> observed,
            GameSession.Snapshot state, int expectedSeat, boolean bidding, GameMode mode, int playerCount,
            HowToPlay.Language language, WhistUiText copy) {
        addActorCases(expected, observed, state, expectedSeat, bidding, mode, playerCount, language, copy, false);
    }

    private static void addActorCases(List<ActorObservation> expected, List<ActorObservation> observed,
            GameSession.Snapshot state, int expectedSeat, boolean bidding, GameMode mode, int playerCount,
            HowToPlay.Language language, WhistUiText copy, boolean collecting) {
        for (boolean autoGame : new boolean[]{false, true}) {
            var turn = WhistTableContent.turn(state, collecting, autoGame);
            List<Integer> expectedBadge = expectedSeat < 0 ? List.of() : List.of(expectedSeat);
            boolean expectedManual = expectedSeat == 0 && !collecting && !autoGame;
            List<Integer> activeSeats = new ArrayList<>(), announcedSeats = new ArrayList<>();
            for (var player : state.players()) {
                var seat = WhistTableContent.seat(player, turn.seat(), state.dealerSeat(), bidding, copy);
                if (seat.active()) activeSeats.add(player.seat());
                if (seat.description().contains(copy.seatTurnAction(bidding))) announcedSeats.add(player.seat());
            }
            expected.add(new ActorObservation(mode, playerCount, language, state.phase(), autoGame, collecting,
                    expectedSeat, expectedManual, expectedBadge, expectedBadge));
            observed.add(new ActorObservation(mode, playerCount, language, state.phase(), autoGame, collecting,
                    turn.seat(), turn.manual(), activeSeats, announcedSeats));
        }
    }

    @Test public void handProjectionKeepsCardOrderFacesLegalityAndSnapshotIdentity() {
        List<HandCase> expected = new ArrayList<>(), observed = new ArrayList<>();
        List<Boolean> selectedIdentity = new ArrayList<>();
        for (var mode : GameMode.values()) for (int count = 3; count <= 6; count++) {
            var game = game(mode, count);
            var copy = new WhistUiText(HowToPlay.Language.EN);
            var bidding = game.snapshot();
            var initialHand = bidding.players().get(0).hand();
            var bidProjection = WhistTableContent.hand(bidding, Set.copyOf(initialHand),
                    WhistTableContent.turn(bidding, false, false), true, copy);
            boolean biddingHasPlayableCard = bidProjection.stream().anyMatch(HandCard::playable);
            while (game.snapshot().phase() == GamePhase.BIDDING) {
                var state = game.snapshot();
                var allowed = GameRules.allowedBids(state.handSize(), state.bidTotal(),
                        state.bidsSubmitted() == state.players().size() - 1);
                game.submitBid(state.biddingSeat(), allowed.get(0));
            }

            var state = game.snapshot();
            List<Card> cards = state.players().get(0).hand();
            Set<Card> legalCards = Set.of(cards.get(0));
            List<HandCard> expectedCards = cards.stream().map(card -> new HandCard(
                    card.suit().name() + ":" + card.rank(),
                    new CardFace(card.rankLabel(), card.suit().symbol(), card.suit().red()),
                    copy.cardLabel(card) + ". " + copy.cardState(false, true, legalCards.contains(card)),
                    legalCards.contains(card))).toList();
            List<HandCard> manualCards = WhistTableContent.hand(state, legalCards,
                    new WhistTableContent.Turn(0, true), false, copy);
            expected.add(new HandCase(mode, count, expectedCards, false));
            observed.add(new HandCase(mode, count, manualCards, biddingHasPlayableCard));
            for (int index = 0; index < cards.size(); index++) {
                selectedIdentity.add(WhistTableContent.selected(cards, manualCards.get(index).id()) == cards.get(index));
            }

            List<HandCard> expectedAutoplay = cards.stream().map(card -> new HandCard(
                    card.suit().name() + ":" + card.rank(),
                    new CardFace(card.rankLabel(), card.suit().symbol(), card.suit().red()),
                    copy.cardLabel(card) + ". " + copy.cardState(false, false, false), false)).toList();
            List<HandCard> autoplay = WhistTableContent.hand(state, legalCards,
                    new WhistTableContent.Turn(0, false), false, copy);
            expected.add(new HandCase(mode, count, expectedAutoplay, false));
            observed.add(new HandCase(mode, count, autoplay, false));
        }

        atf.gradle.audit.GradleInputs.fixture("Snapshot-bound hand matrix", "modes", List.of(GameMode.values()),
                "playerCounts", List.of(3, 4, 5, 6), "cardCases", observed.size());
        assertEquals("Bidding and AutoGame expose no manual card; manual cards preserve their projection",
                expected, observed);
        assertTrue("Selected card IDs resolve to the same objects in the source hand",
                selectedIdentity.stream().allMatch(Boolean::booleanValue));
        assertThrows("A projected hand is immutable", UnsupportedOperationException.class,
                () -> WhistTableContent.hand(game(GameMode.values()[0], 3).snapshot(), Set.of(),
                        new WhistTableContent.Turn(0, false), true,
                        new WhistUiText(HowToPlay.Language.EN)).clear());
        assertThrows("An unknown card ID is rejected", IllegalArgumentException.class,
                () -> WhistTableContent.selected(List.of(), "unknown"));
    }

    @Test public void depositedCardsPreservePublicOrderFacesAndTrickResults() {
        List<PlayCase> expected = new ArrayList<>(), observed = new ArrayList<>();
        List<Integer> trickTotals = new ArrayList<>();
        for (var mode : GameMode.values()) for (int count = 3; count <= 6; count++) {
            var game = game(mode, count);
            var copy = new WhistUiText(HowToPlay.Language.RO);
            while (game.snapshot().phase() == GamePhase.BIDDING) {
                var state = game.snapshot();
                var allowed = GameRules.allowedBids(state.handSize(), state.bidTotal(),
                        state.bidsSubmitted() == state.players().size() - 1);
                game.submitBid(state.biddingSeat(), allowed.get(0));
            }
            for (int played = 0; played < count; played++) {
                var state = game.snapshot();
                var led = state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit();
                int actor = state.currentSeat();
                var legal = GameRules.legalCards(state.players().get(actor).hand(), led,
                        state.trump(), state.mode());
                game.playCard(actor, legal.get(0));
                var after = game.snapshot();
                List<PlayedCard> publicPlays = played == count - 1 ? after.lastTrick() : after.currentTrick();
                List<PlayedCardContent> expectedPlays = publicPlays.stream().map(play -> {
                    var player = after.players().get(play.seat());
                    return new PlayedCardContent(play.seat(), play.card().shortLabel(), play.card().suit().red(),
                            copy.trickPlayer(player), copy.trickCardAccessibility(player, play.card()));
                }).toList();
                List<PlayedCardContent> projectedPlays = WhistTableContent.plays(after, publicPlays, copy);
                expected.add(new PlayCase(mode, count, played + 1, expectedPlays));
                observed.add(new PlayCase(mode, count, played + 1, projectedPlays));
                if (played == 0) {
                    assertThrows("Deposited-card projection is immutable", UnsupportedOperationException.class,
                            projectedPlays::clear);
                }
                if (played == count - 1) {
                    trickTotals.add(after.players().stream().mapToInt(GameSession.PlayerSnapshot::tricksWon).sum());
                }
            }
        }

        atf.gradle.audit.GradleInputs.fixture("Public trick projection matrix", "modes", List.of(GameMode.values()),
                "playerCounts", List.of(3, 4, 5, 6), "partialTricks", observed.size());
        assertEquals("Deposited cards preserve actor order, marks, color and accessible player names",
                expected, observed);
        assertEquals("Every completed one-trick hand awards exactly one trick",
                java.util.Collections.nCopies(trickTotals.size(), 1), trickTotals);
    }

    private GameSession game(GameMode mode, int count) {
        return GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation",
                new GameConfig(count, mode, AiDifficulty.NORMAL, 49)));
    }

    private record SeatCase(GameMode mode, int playerCount, HowToPlay.Language language,
            List<SeatContent> seats) { }
    private record ActorObservation(GameMode mode, int playerCount, HowToPlay.Language language,
            GamePhase phase, boolean autoGame, boolean collecting, int actorSeat, boolean manual,
            List<Integer> activeSeats, List<Integer> announcedSeats) { }
    private record HandCase(GameMode mode, int playerCount, List<HandCard> cards,
            boolean biddingHasPlayableCard) { }
    private record PlayCase(GameMode mode, int playerCount, int playedCount,
            List<PlayedCardContent> plays) { }
}
