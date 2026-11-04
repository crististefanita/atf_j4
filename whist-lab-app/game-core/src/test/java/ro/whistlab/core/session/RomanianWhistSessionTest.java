package ro.whistlab.core.session;

import ro.whistlab.core.ai.policy.AiPolicy;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;

public class RomanianWhistSessionTest {
    private static final GameMode RO = GameMode.ROMANIAN_WHIST;

    @Test public void rw04DiscardIsRejectedWithoutChangingStateWhenTrumpIsHeld() {
        GameSession game = twoTricks();
        game.playCard(0, c(Suit.HEARTS,14));
        var before = game.snapshot();
        assertThrows(IllegalArgumentException.class, () -> game.playCard(1, c(Suit.CLUBS,14)));
        assertEquals(before, game.snapshot());
        game.playCard(1, c(Suit.SPADES,7));
        assertEquals(2, game.snapshot().currentSeat());
    }

    @Test public void rw04FollowingSuitIsRequiredEvenWhenTheTrickWasAlreadyTrumped() {
        GameSession game = twoTricks();
        game.playCard(0, c(Suit.HEARTS,14));
        game.playCard(1, c(Suit.SPADES,7));
        var before = game.snapshot();
        assertThrows(IllegalArgumentException.class, () -> game.playCard(2, c(Suit.SPADES,14)));
        assertEquals(before, game.snapshot());
        game.playCard(2, c(Suit.HEARTS,13));
    }

    @Test public void rw04AnyHeldTrumpMayBeUsedWithoutAnOvertrumpRequirement() {
        List<List<Card>> hands = List.of(List.of(c(Suit.HEARTS,14), c(Suit.CLUBS,7)),
                List.of(c(Suit.SPADES,10), c(Suit.CLUBS,14)),
                List.of(c(Suit.SPADES,7), c(Suit.SPADES,13)),
                List.of(c(Suit.HEARTS,7), c(Suit.DIAMONDS,14)));
        GameSession game = ready(hands, 3, Trump.SPADES);
        game.playCard(0, c(Suit.HEARTS,14));
        game.playCard(1, c(Suit.SPADES,10));
        game.playCard(2, c(Suit.SPADES,7));
        game.playCard(3, c(Suit.HEARTS,7));
        assertEquals(1, game.snapshot().lastTrickWinnerSeat());
        assertEquals(1, game.snapshot().currentSeat());
        assertEquals(1, game.snapshot().players().get(1).tricksWon());
        game.playCard(1, c(Suit.CLUBS,14));
        assertEquals(2, game.snapshot().currentSeat());
    }

    @Test public void rw06ScoresOnlyAfterLastCardAndOnlyOnce() {
        GameSession game = oneCard();
        game.submitBid(0,1); game.submitBid(1,0); game.submitBid(2,0); game.submitBid(3,1);
        game.playCard(0,c(Suit.HEARTS,14));
        game.playCard(1,c(Suit.HEARTS,13));
        game.playCard(2,c(Suit.HEARTS,12));
        assertEquals(List.of(0,0,0,0), totals(game));
        game.playCard(3,c(Suit.HEARTS,11));
        assertEquals(List.of(6,5,5,-1), totals(game));
        var scored = game.snapshot();
        assertThrows(IllegalStateException.class, () -> game.playCard(3,c(Suit.HEARTS,11)));
        assertThrows(IllegalStateException.class, () -> game.submitBid(0,0));
        assertEquals(scored, game.snapshot());
        assertEquals(scored, GameSession.restore(game.saveState()).snapshot());
        game.startNextHand();
        assertEquals(List.of(6,5,5,-1), totals(game));
        assertTrue(game.snapshot().players().stream().allMatch(p -> p.tricksWon()==0 && p.bid()==null));
        assertEquals(0, game.snapshot().dealerSeat());
    }

    @Test public void rw03DealerBidsLastAndForbiddenInputDoesNotAdvanceTheTurn() {
        for (int dealer=0; dealer<4; dealer++) {
            GameSession game = GameSession.fromDeal(config(), oneCardHands(), Trump.SPADES, dealer);
            for (int i=1; i<4; i++) {
                int expected=(dealer+i)%4;
                assertEquals(expected, game.snapshot().biddingSeat());
                game.submitBid(expected,0);
            }
            var before=game.snapshot();
            final int seat=dealer;
            assertThrows(IllegalArgumentException.class, () -> game.submitBid(seat,1));
            assertEquals(before,game.snapshot());
            game.submitBid(dealer,0);
            assertEquals((dealer+1)%4,game.snapshot().currentSeat());
        }
    }

    @Test public void rw07WrongPhaseWrongSeatAndOutOfRangeBidLeaveStateUnchanged() {
        GameSession game=oneCard(); var before=game.snapshot();
        assertThrows(IllegalStateException.class, () -> game.playCard(0,c(Suit.HEARTS,14)));
        assertThrows(IllegalStateException.class, game::startNextHand);
        assertThrows(IllegalStateException.class, () -> game.submitBid(1,0));
        assertThrows(IllegalArgumentException.class, () -> game.submitBid(0,-1));
        assertThrows(IllegalArgumentException.class, () -> game.submitBid(0,2));
        assertEquals(before,game.snapshot());
    }

    @Test public void rw04BothAiDifficultiesRespectMandatoryTrumpEvenAfterMeetingTheirBid() {
        GameSession game=twoTricks(); game.playCard(0,c(Suit.HEARTS,14));
        for (AiDifficulty difficulty : AiDifficulty.values())
            assertEquals(c(Suit.SPADES,7), AiPolicy.chooseCard(game.snapshot(),1,difficulty));
    }

    private static GameSession twoTricks() {
        return ready(List.of(List.of(c(Suit.HEARTS,14),c(Suit.CLUBS,7)),
                List.of(c(Suit.SPADES,7),c(Suit.CLUBS,14)),
                List.of(c(Suit.HEARTS,13),c(Suit.SPADES,14)),
                List.of(c(Suit.HEARTS,7),c(Suit.DIAMONDS,14))),3,Trump.SPADES);
    }
    private static GameSession ready(List<List<Card>> hands,int dealer,Trump trump) {
        GameSession game=GameSession.fromDeal(config(),hands,trump,dealer);
        for (int i=0;i<4;i++) game.submitBid(game.snapshot().biddingSeat(),0);
        return game;
    }
    private static GameSession oneCard() { return GameSession.fromDeal(config(),oneCardHands(),Trump.SPADES,3); }
    private static List<List<Card>> oneCardHands() { return List.of(List.of(c(Suit.HEARTS,14)),
            List.of(c(Suit.HEARTS,13)),List.of(c(Suit.HEARTS,12)),List.of(c(Suit.HEARTS,11))); }
    private static GameConfig config() { return new GameConfig(4,RO,AiDifficulty.NORMAL,11L); }
    private static Card c(Suit suit,int rank) { return new Card(suit,rank); }
    private static List<Integer> totals(GameSession game) {
        return game.snapshot().players().stream().map(GameSession.PlayerSnapshot::totalScore).toList();
    }
}
