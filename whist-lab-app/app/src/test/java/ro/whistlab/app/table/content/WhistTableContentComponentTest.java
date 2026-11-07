package ro.whistlab.app.table.content;

import java.util.List;
import java.util.Set;
import org.junit.Test;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.rules.HowToPlay;
import ro.whistlab.core.session.GameSession;
import static atf.gradle.audit.assertions.GradleAssertions.*;

/** Whist rules and copy remain consumer contracts, never shared renderer fixtures. */
public final class WhistTableContentComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void originalSeatFactsDescriptionsAndPausedActorArePreservedAcrossAllWhistModes() {
        for (var mode : GameMode.values()) for (int count = 3; count <= 6; count++)
            for (var language : HowToPlay.Language.values()) {
                var state = game(mode, count).snapshot();
                var copy = new WhistUiText(language);
                for (var player : state.players()) for (boolean paused : new boolean[]{false, true}) {
                    var seat = WhistTableContent.seat(player, state.biddingSeat(), state.dealerSeat(), paused, true, copy);
                    boolean active = !paused && player.seat() == state.biddingSeat();
                    
                    atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "active", active, "copy", copy, "count", count, "language", language, "mode", mode, "paused", paused, "player", player, "seat", seat, "state", state);
                    assertEquals("seat.seat() must equal player.seat()", player.seat(), seat.seat());
                    assertEquals("seat.name() must equal copy.seatName(player)", copy.seatName(player), seat.name());
                    assertEquals("seat.facts().stream().map((f)->f.label()).toList() must equal List.of(copy.bidLabel() + \":\", copy.seatWonLabel() + \":\")", List.of(copy.bidLabel() + ":", copy.seatWonLabel() + ":"), seat.facts().stream().map(f -> f.label()).toList());
                    assertEquals("seat.facts().stream().map((f)->f.value()).toList() must equal List.of(\"\\u2014\", \"0\")", List.of("—", "0"), seat.facts().stream().map(f -> f.value()).toList());
                    assertEquals("seat.active() must equal active", active, seat.active());
                    assertEquals("seat.description() must equal copy.seatAccessibility(player, player.seat() == state.dealerSeat(), active, true)", copy.seatAccessibility(player, player.seat() == state.dealerSeat(), active, true), seat.description());
                }
            }
    }

    @Test public void snapshotBoundHandKeepsOrderMarksLegalityAndOriginalCardIdentity() {
        for (var mode : GameMode.values()) for (int count = 3; count <= 6; count++) {
            var game = game(mode, count);
            var copy = new WhistUiText(HowToPlay.Language.EN);
            var bidding = game.snapshot();
            
            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "bidding", bidding, "copy", copy, "count", count, "game", game, "mode", mode);
            assertTrue("WhistTableContent.hand(bidding, Set.copyOf(bidding.players().get(0).hand()), true, true, copy).stream().noneMatch((card)->card.playable()) must hold", WhistTableContent.hand(bidding, Set.copyOf(bidding.players().get(0).hand()), true, true, copy)
                    .stream().noneMatch(card -> card.playable()));
            while (game.snapshot().players().stream().anyMatch(player -> player.bid() == null)) game.submitBid(game.snapshot().biddingSeat(), 0);
            var state = game.snapshot();
            var cards = state.players().get(0).hand();
            var allowed = Set.of(cards.get(0));
            var projected = WhistTableContent.hand(state, allowed, true, false, copy);
            assertEquals("projected.size() must equal cards.size()", cards.size(), projected.size());
            assertThrows("The operation rejects its input with UnsupportedOperationException.class", UnsupportedOperationException.class, projected::clear);
            for (int index = 0; index < cards.size(); index++) {
                Card original = cards.get(index);
                var item = projected.get(index);
                assertEquals("item.id() must equal original.suit().name() + \":\" + original.rank()", original.suit().name() + ":" + original.rank(), item.id());
                assertEquals("item.face().rankLabel() must equal original.rankLabel()", original.rankLabel(), item.face().rankLabel());
                assertEquals("item.face().suitSymbol() must equal original.suit().symbol()", original.suit().symbol(), item.face().suitSymbol());
                assertEquals("item.playable() must equal allowed.contains(original)", allowed.contains(original), item.playable());
                assertEquals("item.description() must equal copy.cardLabel(original) + \". \" + copy.cardState(false, state.currentSeat() == 0, item.playable())", copy.cardLabel(original) + ". " + copy.cardState(false, state.currentSeat() == 0, item.playable()), item.description());
                assertSame("WhistTableContent.selected(cards, item.id()) keeps the identity of original", original, WhistTableContent.selected(cards, item.id()));
            }
            assertTrue("WhistTableContent.hand(state, allowed, false, false, copy).stream().noneMatch((card)->card.playable()) must hold", WhistTableContent.hand(state, allowed, false, false, copy).stream().noneMatch(card -> card.playable()));
            assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> WhistTableContent.selected(cards, "unknown"));
        }
    }

    @Test public void depositedCardsKeepActualActorAndCompletedTrickWithoutExposingOtherHands() {
        for (var mode : GameMode.values()) for (int count = 3; count <= 6; count++) {
            var game = game(mode, count);
            var copy = new WhistUiText(HowToPlay.Language.RO);
            while (game.snapshot().players().stream().anyMatch(player -> player.bid() == null)) game.submitBid(game.snapshot().biddingSeat(), 0);
            for (int turn = 0; turn < count; turn++) {
                var state = game.snapshot();
                var led = state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit();
                var legal = GameRules.legalCards(state.players().get(state.currentSeat()).hand(), led, state.trump(), state.mode());
                game.playCard(state.currentSeat(), legal.get(0));
                var after = game.snapshot();
                var plays = turn == count - 1 ? after.lastTrick() : after.currentTrick();
                var visible = WhistTableContent.plays(after, plays, copy);
                
                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "after", after, "copy", copy, "count", count, "game", game, "led", led, "legal", legal, "mode", mode, "plays", plays, "state", state, "turn", turn, "visible", visible);
                assertEquals("visible.size() must equal turn + 1", turn + 1, visible.size());
                assertThrows("The operation rejects its input with UnsupportedOperationException.class", UnsupportedOperationException.class, visible::clear);
                for (int index = 0; index < visible.size(); index++) {
                    var play = plays.get(index);
                    var player = after.players().get(play.seat());
                    var actual = visible.get(index);
                    assertEquals("actual.seat() must equal play.seat()", play.seat(), actual.seat());
                    assertEquals("actual.faceLabel() must equal play.card().shortLabel()", play.card().shortLabel(), actual.faceLabel());
                    assertEquals("actual.red() must equal play.card().suit().red()", play.card().suit().red(), actual.red());
                    assertEquals("actual.playerLabel() must equal copy.trickPlayer(player)", copy.trickPlayer(player), actual.playerLabel());
                    assertEquals("actual.description() must equal copy.trickCardAccessibility(player, play.card())", copy.trickCardAccessibility(player, play.card()), actual.description());
                }
                if (turn == count - 1) {
                    assertEquals("after.players().stream().mapToInt((player)->player.tricksWon()).sum() must equal 1", 1, after.players().stream().mapToInt(player -> player.tricksWon()).sum());
                    for (var player : after.players()) {
                        var seat = WhistTableContent.seat(player, after.currentSeat(), after.dealerSeat(), false, false, copy);
                        assertEquals("seat.facts().get(0).value() must equal player.bid().toString()", player.bid().toString(), seat.facts().get(0).value());
                        assertEquals("seat.facts().get(1).value() must equal Integer.toString(player.tricksWon())", Integer.toString(player.tricksWon()), seat.facts().get(1).value());
                    }
                }
            }
        }
    }

    private GameSession game(GameMode mode, int count) {
        return GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", new GameConfig(count, mode, AiDifficulty.NORMAL, 49)));
    }
}
