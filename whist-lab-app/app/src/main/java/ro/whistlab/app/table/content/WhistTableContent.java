package ro.whistlab.app.table.content;

import java.util.List;
import java.util.Set;
import ro.cardtable.ui.hand.HandCard;
import ro.cardtable.ui.seat.SeatContent;
import ro.cardtable.ui.table.PlayedCardContent;
import ro.whistlab.app.cards.WhistCardFace;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.session.GamePhase;
/** Projects the original Whist snapshot and copy; no rule or point computation moves into UI. */
public final class WhistTableContent {
    private WhistTableContent() { }
    public static SeatContent seat(GameSession.PlayerSnapshot player, int activeSeat, int dealerSeat,
            boolean paused, boolean bidding, WhistUiText copy) {
        boolean active = !paused && player.seat() == activeSeat;
        return new SeatContent(player.seat(), copy.seatName(player), List.of(
                new SeatContent.Fact(copy.bidLabel() + ":", player.bid() == null ? "—" : player.bid().toString()),
                new SeatContent.Fact(copy.seatWonLabel() + ":", Integer.toString(player.tricksWon()))),
                active, copy.seatAccessibility(player, player.seat() == dealerSeat, active, bidding));
    }
    public static List<HandCard> hand(GameSession.Snapshot state, Set<Card> legalCards,
            boolean callbackAvailable, boolean bidding, WhistUiText copy) {
        boolean humanTurn = state.phase() != GamePhase.PLAYING || state.currentSeat() == 0;
        return state.players().get(0).hand().stream().map(card -> {
            boolean playable = !bidding && callbackAvailable && legalCards.contains(card);
            return new HandCard(id(card), WhistCardFace.from(card), copy.cardLabel(card) + ". "
                    + copy.cardState(bidding, humanTurn, playable), playable);
        }).collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(), List::copyOf));
    }
    public static List<PlayedCardContent> plays(GameSession.Snapshot state, List<PlayedCard> visible, WhistUiText copy) {
        return visible.stream().map(play -> {
            var player = state.players().get(play.seat());
            return new PlayedCardContent(play.seat(), play.card().shortLabel(), play.card().suit().red(),
                    copy.trickPlayer(player), copy.trickCardAccessibility(player, play.card()));
        }).collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(), List::copyOf));
    }
    public static String id(Card card) { return card.suit().name() + ":" + card.rank(); }
    public static Card selected(List<Card> hand, String id) {
        return hand.stream().filter(card -> id(card).equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown card in the displayed Whist snapshot"));
    }
}
