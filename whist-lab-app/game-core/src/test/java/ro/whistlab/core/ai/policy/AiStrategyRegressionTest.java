package ro.whistlab.core.ai.policy;

import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.learning.reference.LegacySimpleHardReference;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.*;
import org.junit.Test;


public class AiStrategyRegressionTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void hardDoesNotLeadItsAceWhenTheZeroContractIsStillAchievable(){
        GameSession g=leadFixture();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "g", g);
        assertEquals("LegacySimpleHardReference.chooseCard(g.snapshot(), 0, AiDifficulty.HARD) must equal c(1, 14)", c(1,14),LegacySimpleHardReference.chooseCard(g.snapshot(),0,AiDifficulty.HARD));
        assertEquals("AiPolicy.chooseCard(g.snapshot(), 0, AiDifficulty.HARD) must equal c(2, 7)", c(2,7),AiPolicy.chooseCard(g.snapshot(),0,AiDifficulty.HARD));
        assertEquals("finishLead(true) must equal -1", -1,finishLead(true));assertEquals("finishLead(false) must equal 5", 5,finishLead(false));
    }
    @Test public void hardDiscardsTheDangerousKingUnderAnAlreadyPlayedAce(){
        GameSession g=GameSession.fromDeal(new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.HARD,1),List.of(
                List.of(c(1,7),c(1,13)),List.of(c(2,7),c(2,8)),List.of(c(3,7),c(3,8)),List.of(c(1,14),c(0,7))),Trump.SPADES,2);
        for(int i=0;i<4;i++)g.submitBid(g.snapshot().biddingSeat(),0);g.playCard(3,c(1,14));

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "g", g);
        assertEquals("LegacySimpleHardReference.chooseCard(g.snapshot(), 0, AiDifficulty.HARD) must equal c(1, 7)", c(1,7),LegacySimpleHardReference.chooseCard(g.snapshot(),0,AiDifficulty.HARD));
        assertEquals("AiPolicy.chooseCard(g.snapshot(), 0, AiDifficulty.HARD) must equal c(1, 13)", c(1,13),AiPolicy.chooseCard(g.snapshot(),0,AiDifficulty.HARD));
    }
    @Test public void biddingHypothesesPreserveObservedBidsAndChangeTheOwnTarget(){
        GameSession g=fixture(3);g.submitBid(0,2);var v=MonteCarloAi.observe(g.snapshot(),1);var hands=g.snapshot().players().stream().map(GameSession.PlayerSnapshot::hand).toList();
        int[] a=MonteCarloAi.biddingTargets(v,hands,0),b=MonteCarloAi.biddingTargets(v,hands,1);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "a", a, "b", b, "g", g, "hands", hands, "v", v);
        assertEquals("a[0] must equal 2", 2,a[0]);assertEquals("b[0] must equal 2", 2,b[0]);assertEquals("a[1] must equal 0", 0,a[1]);assertEquals("b[1] must equal 1", 1,b[1]);assertNotEquals("Arrays.stream(a).sum() must differ from 2", 2,Arrays.stream(a).sum());assertNotEquals("Arrays.stream(b).sum() must differ from 2", 2,Arrays.stream(b).sum());
    }
    @Test public void ownHandOrderCannotChangeTheInformationSetOrDecision(){
        GameSession g=fixture(3);for(int i=0;i<4;i++)g.submitBid(i,0);var a=MonteCarloAi.observe(g.snapshot(),0);var reverse=new ArrayList<>(a.hand());Collections.reverse(reverse);
        var b=new MonteCarloAi.View(a.mode(),a.trump(),a.seat(),a.dealer(),a.handSize(),reverse,a.seats(),a.trick(),a.history(),a.turnedCard());

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "a", a, "b", b, "g", g, "reverse", reverse);
        assertEquals("b must equal a", a,b);assertEquals("MonteCarloAi.chooseCard(b) must equal MonteCarloAi.chooseCard(a)", MonteCarloAi.chooseCard(a),MonteCarloAi.chooseCard(b));
    }
    private static GameSession fixture(int dealer){return GameSession.fromDeal(new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.HARD,1),List.of(List.of(c(1,7),c(1,14)),List.of(c(0,7),c(0,8)),List.of(c(2,7),c(2,8)),List.of(c(3,7),c(3,8))),Trump.SPADES,dealer);}
    private static GameSession leadFixture(){var g=GameSession.fromDeal(new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.HARD,1),List.of(List.of(c(1,14),c(2,7)),List.of(c(2,8),c(2,9)),List.of(c(2,10),c(2,11)),List.of(c(2,12),c(2,13))),Trump.SPADES,3);int[] b={0,1,0,2};for(int s=0;s<4;s++)g.submitBid(s,b[s]);return g;}
    private static int finishLead(boolean old){var g=leadFixture();while(g.snapshot().phase()==GamePhase.PLAYING){var s=g.snapshot();int actor=s.currentSeat();g.playCard(actor,actor==0&&!old?AiPolicy.chooseCard(s,actor,AiDifficulty.HARD):LegacySimpleHardReference.chooseCard(s,actor,AiDifficulty.HARD));}return g.snapshot().players().get(0).totalScore();}
    private static Card c(int suit,int rank){return new Card(Suit.values()[suit],rank);}
}
