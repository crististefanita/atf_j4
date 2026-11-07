package ro.whistlab.core.ai.engine;

import ro.whistlab.core.ai.policy.AiPolicy;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.*;
import org.junit.Test;

public class MonteCarloAiTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void ai08TurnedCardAndEveryPubliclyPlayedCardAreExcludedFromHiddenDeals() {
        GameSession game=GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.EXPERT,72)));
        var before=MonteCarloAi.observe(game.snapshot(),0);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "before", before, "game", game);
        assertNotNull("before.turnedCard() is available", before.turnedCard());
        List<Card> unknown=MonteCarloAi.unknownCards(before);
        assertEquals("unknown.size() must equal 30", 30,unknown.size());assertFalse("unknown.contains(before.turnedCard()) must remain false", unknown.contains(before.turnedCard()));
        assertTrue("Collections.disjoint(unknown, before.hand()) must hold", Collections.disjoint(unknown,before.hand()));
        while(game.snapshot().phase()==GamePhase.BIDDING) {
            var s=game.snapshot();game.submitBid(s.biddingSeat(),AiPolicy.chooseBid(s,s.biddingSeat(),AiDifficulty.NORMAL));
        }
        var s=game.snapshot();game.playCard(s.currentSeat(),s.players().get(s.currentSeat()).hand().get(0));
        var after=MonteCarloAi.observe(game.snapshot(),game.snapshot().currentSeat());
        unknown=MonteCarloAi.unknownCards(after);
        assertFalse("unknown.contains(after.history().get(0).card()) must remain false", unknown.contains(after.history().get(0).card()));
        assertFalse("unknown.contains(after.turnedCard()) must remain false", unknown.contains(after.turnedCard()));
        assertEquals("MonteCarloAi.observe(GameSession.restore(game.saveState()).snapshot(), game.snapshot().currentSeat()) must equal after", after,MonteCarloAi.observe(GameSession.restore(game.saveState()).snapshot(),game.snapshot().currentSeat()));
    }
    @Test public void ai01PrivateOpponentsCardsCannotAffectObservationOrDecision() {
        GameSession first=fixture(false),second=fixture(true);
        var view=MonteCarloAi.observe(first.snapshot(),0);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "first", first, "second", second, "view", view);
        assertEquals("MonteCarloAi.observe(second.snapshot(), 0) must equal view", view,MonteCarloAi.observe(second.snapshot(),0));
        assertEquals("MonteCarloAi.chooseBid(MonteCarloAi.observe(second.snapshot(), 0)) must equal MonteCarloAi.chooseBid(view)", MonteCarloAi.chooseBid(view),MonteCarloAi.chooseBid(MonteCarloAi.observe(second.snapshot(),0)));
        for(GameSession game:List.of(first,second))for(int seat=0;seat<4;seat++)game.submitBid(seat,0);
        assertEquals("MonteCarloAi.observe(second.snapshot(), 0) must equal MonteCarloAi.observe(first.snapshot(), 0)", MonteCarloAi.observe(first.snapshot(),0),MonteCarloAi.observe(second.snapshot(),0));
        assertEquals("MonteCarloAi.chooseCard(MonteCarloAi.observe(second.snapshot(), 0)) must equal MonteCarloAi.chooseCard(MonteCarloAi.observe(first.snapshot(), 0))", MonteCarloAi.chooseCard(MonteCarloAi.observe(first.snapshot(),0)),
                MonteCarloAi.chooseCard(MonteCarloAi.observe(second.snapshot(),0)));
    }

    @Test public void ai02LastPlayerAvoidsWinningWhenZeroContractCanStillBeMade() {
        var view=view(List.of(c(Suit.HEARTS,7),c(Suit.HEARTS,14)),0,
                List.of(new PlayedCard(1,c(Suit.HEARTS,10)),new PlayedCard(2,c(Suit.HEARTS,11)),new PlayedCard(3,c(Suit.HEARTS,12))));
        var decision=MonteCarloAi.chooseCard(view);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "decision", decision, "view", view);
        assertEquals("decision.card() must equal c(Suit.HEARTS, 7)", c(Suit.HEARTS,7),decision.card());
        assertTrue("decision.candidates().stream().allMatch((v)->v.samples() > 0) must hold", decision.candidates().stream().allMatch(v->v.samples()>0));
    }

    @Test public void ai03LastPlayerTakesRequiredTrickWhenThatCompletesTheContract() {
        var view=view(List.of(c(Suit.HEARTS,7),c(Suit.HEARTS,14)),1,
                List.of(new PlayedCard(1,c(Suit.HEARTS,10)),new PlayedCard(2,c(Suit.HEARTS,11)),new PlayedCard(3,c(Suit.HEARTS,12))));

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "view", view);
        assertEquals("MonteCarloAi.chooseCard(view).card() must equal c(Suit.HEARTS, 14)", c(Suit.HEARTS,14),MonteCarloAi.chooseCard(view).card());
    }

    @Test public void ai04MandatoryTrumpHasPriorityEvenWhenLosingIsStrategicallyPreferred() {
        var view=view(List.of(c(Suit.CLUBS,14),c(Suit.SPADES,7)),0,
                List.of(new PlayedCard(1,c(Suit.HEARTS,14)),new PlayedCard(2,c(Suit.HEARTS,13)),new PlayedCard(3,c(Suit.HEARTS,12))));

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "view", view);
        assertEquals("MonteCarloAi.chooseCard(view).card() must equal c(Suit.SPADES, 7)", c(Suit.SPADES,7),MonteCarloAi.chooseCard(view).card());
    }

    @Test public void ai05SamplingIsRepeatableAndDoesNotMutateTheObservedGame() {
        GameSession game=fixture(false);for(int seat=0;seat<4;seat++)game.submitBid(seat,0);
        var before=game.snapshot();var view=MonteCarloAi.observe(before,0);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "before", before, "game", game, "view", view);
        assertEquals("MonteCarloAi.chooseCard(view) must equal MonteCarloAi.chooseCard(view)", MonteCarloAi.chooseCard(view),MonteCarloAi.chooseCard(view));
        assertEquals("game.snapshot() must equal before", before,game.snapshot());
        assertThrows("The operation rejects its input with UnsupportedOperationException.class", UnsupportedOperationException.class,()->view.hand().clear());
    }

    @Test public void ai09BidSampleOptionsRemainOrderedAndImmutable() {
        MonteCarloAi.BidSamples samples = new MonteCarloAi.BidSamples(List.of(0, 1, 2), 3, false);
        samples.add(new double[]{3.0, 5.0, 7.0}, new double[]{0.25, 0.5, 0.75});

        List<MonteCarloAi.BidOption> options = samples.options();



        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "options", options, "samples", samples);
        assertEquals("options.stream().map(MonteCarloAi.BidOption::bid).collect(java.util.stream.Collectors.toList()) must equal List.of(0, 1, 2)", List.of(0, 1, 2), options.stream().map(MonteCarloAi.BidOption::bid)
                .collect(java.util.stream.Collectors.toList()));
        assertThrows("The operation rejects its input with UnsupportedOperationException.class", UnsupportedOperationException.class, options::clear);
    }

    private static GameSession realTwoCardHand() {
        GameSession game=GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.NORMAL,3)));
        while(game.snapshot().handSize()==1) {
            while(game.snapshot().phase()==GamePhase.BIDDING) {
                var s=game.snapshot();game.submitBid(s.biddingSeat(),AiPolicy.chooseBid(s,s.biddingSeat(),AiDifficulty.NORMAL));
            }
            while(game.snapshot().phase()==GamePhase.PLAYING) {
                var s=game.snapshot();game.playCard(s.currentSeat(),AiPolicy.chooseCard(s,s.currentSeat(),AiDifficulty.NORMAL));
            }
            game.startNextHand();
        }
        while(game.snapshot().phase()==GamePhase.BIDDING) {
            var s=game.snapshot();game.submitBid(s.biddingSeat(),AiPolicy.chooseBid(s,s.biddingSeat(),AiDifficulty.NORMAL));
        }
        return game;
    }

    @Test public void ai06PublicHistoryIsPreservedAcrossSaveAndRestore() {
        GameSession game=realTwoCardHand();
        while(game.snapshot().history().size()<5) {
            var s=game.snapshot();game.playCard(s.currentSeat(),AiPolicy.chooseCard(s,s.currentSeat(),AiDifficulty.HARD));
        }

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game);
        assertEquals("game.snapshot().history().size() must equal 5", 5,game.snapshot().history().size());
        GameSession restored=GameSession.restore(game.saveState());
        assertEquals("MonteCarloAi.observe(restored.snapshot(), restored.snapshot().currentSeat()) must equal MonteCarloAi.observe(game.snapshot(), game.snapshot().currentSeat())", MonteCarloAi.observe(game.snapshot(),game.snapshot().currentSeat()),
                MonteCarloAi.observe(restored.snapshot(),restored.snapshot().currentSeat()));
    }

    private static MonteCarloAi.View view(List<Card> hand,int bid,List<PlayedCard> trick) {
        return new MonteCarloAi.View(GameMode.ROMANIAN_WHIST,Trump.SPADES,0,0,2,hand,
                List.of(new MonteCarloAi.Seat(2,bid,0),new MonteCarloAi.Seat(1,0,0),
                        new MonteCarloAi.Seat(1,0,0),new MonteCarloAi.Seat(1,0,0)),trick,trick);
    }
    private static GameSession fixture(boolean swap) {
        List<Card> a=List.of(c(Suit.HEARTS,7),c(Suit.CLUBS,14));
        List<Card> b=List.of(c(Suit.HEARTS,14),c(Suit.SPADES,7));
        return GameSession.fromDeal(new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.EXPERT,7),
                List.of(List.of(c(Suit.HEARTS,13),c(Suit.SPADES,14)),swap?b:a,swap?a:b,
                        List.of(c(Suit.DIAMONDS,14),c(Suit.CLUBS,7))),Trump.SPADES,3);
    }
    private static Card c(Suit suit,int rank){return new Card(suit,rank);}
}
