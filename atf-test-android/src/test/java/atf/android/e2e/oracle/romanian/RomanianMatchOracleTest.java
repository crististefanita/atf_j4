package atf.android.e2e.oracle.romanian;

import atf.testkit.assertions.AuditedAssertions;

import static org.junit.jupiter.api.Assertions.*;
import atf.reporting.contract.AtfAssertion;
import atf.testkit.AtfComponentTest;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;
import org.junit.jupiter.api.Test;

@AtfComponentTest
class RomanianMatchOracleTest {
    private static final ObjectMapper JSON=new ObjectMapper();
    @Test void acceptsIndependentExactBonusZeroBonusAndNegativePenalty(){AuditedAssertions.assertTrue(audit(score()).stream().allMatch(AtfAssertion::passed));}
    @Test void rejectsOhHellBonusSubstitutedForRomanianScore(){ObjectNode s=score();((ObjectNode)s.path("players").get(0)).put("totalScore",11);rejected(s,"cumulative score");}
    @Test void rejectsWrongWinnerEvenWhenTotalWonIsConserved(){ObjectNode s=score();((ObjectNode)s.path("players").get(0)).put("tricksWon",0);((ObjectNode)s.path("players").get(1)).put("tricksWon",1);rejected(s,"won counter");}
    @Test void rejectsPlayedCardThatWasNeverHeld(){ObjectNode s=score();((ObjectNode)s.path("history").get(0)).put("suit","SPADES");rejected(s,"card ownership");}
    @Test void rejectsChangedActor(){ObjectNode s=score();((ObjectNode)s.path("history").get(0)).put("seat",2);rejected(s,"actor");}
    @Test void rejectsForbiddenLastBid(){ObjectNode s=score();((ObjectNode)s.path("players").get(3)).put("bid",0);s.put("bidTotal",1);rejected(s,"last bid excludes");}
    @Test void rejectsIncompleteEndPhase(){ObjectNode s=score();s.put("phase","PLAYING");rejected(s,"RW07 phase");}
    @Test void eightCardHandsRequireNoTrumpAndNoTurnedCard(){
        ObjectNode start=deal();start.put("handIndex",10);start.put("handSize",8);start.put("trump","NO_TRUMP");start.putNull("turnedCard");
        for(int seat=0;seat<4;seat++){ArrayNode hand=JSON.createArrayNode();for(int rank=7;rank<=14;rank++)hand.add(card(seat,rank));((ObjectNode)start.path("players").get(seat)).set("hand",hand);}
        AuditedAssertions.assertTrue(new RomanianMatchOracle(start,10,3,new int[4]).start(start).stream().allMatch(AtfAssertion::passed));
        start.put("trump","SPADES");AuditedAssertions.assertTrue(new RomanianMatchOracle(start,10,3,new int[4]).start(start).stream().anyMatch(a->!a.passed()&&a.name().contains("no trump")));
    }
    @Test void changingAnAlreadyObservedCardIsRejected(){var o=new RomanianMatchOracle(deal(),0,3,new int[4]);o.start(deal());o.verify(score());ObjectNode s=score();((ObjectNode)s.path("history").get(1)).put("rank",7);AuditedAssertions.assertTrue(o.verify(s).stream().anyMatch(a->!a.passed()&&a.name().contains("retained card")));}
    private static void rejected(ObjectNode s,String name){AuditedAssertions.assertTrue(audit(s).stream().anyMatch(a->!a.passed()&&a.name().contains(name)));}
    private static List<AtfAssertion> audit(ObjectNode s){var o=new RomanianMatchOracle(deal(),0,3,new int[4]);AuditedAssertions.assertTrue(o.start(deal()).stream().allMatch(AtfAssertion::passed));return o.verify(s);}
    private static ObjectNode card(int suit,int rank){var c=JSON.createObjectNode();c.put("suit",RomanianMatchOracle.SUITS.get(suit));c.put("rank",rank);return c;}
    private static ObjectNode deal(){
        var s=JSON.createObjectNode();s.put("mode","ROMANIAN_WHIST");s.put("difficulty","EXPERT");s.put("playerCount",4);s.put("handIndex",0);s.put("handSize",1);s.put("dealerSeat",3);s.put("biddingSeat",0);s.put("currentSeat",0);s.put("phase","BIDDING");s.put("bidsSubmitted",0);s.put("bidTotal",0);s.put("lastTrickWinnerSeat",-1);s.put("trump","SPADES");s.set("turnedCard",card(0,7));
        s.set("history",JSON.createArrayNode());s.set("lastTrick",JSON.createArrayNode());s.set("currentTrick",JSON.createArrayNode());var players=JSON.createArrayNode();
        for(int seat=0;seat<4;seat++){var p=JSON.createObjectNode();p.put("seat",seat);p.putNull("bid");p.put("tricksWon",0);p.put("totalScore",0);p.set("hand",JSON.createArrayNode().add(card(1,14-seat)));players.add(p);}s.set("players",players);return s;
    }
    private static ObjectNode score(){
        ObjectNode s=deal();s.put("phase","SCORE");s.put("bidsSubmitted",4);s.put("bidTotal",2);s.put("currentSeat",3);s.put("lastTrickWinnerSeat",0);var history=JSON.createArrayNode();
        int[] bids={1,0,0,1},totals={6,5,5,-1};for(int seat=0;seat<4;seat++){var p=(ObjectNode)s.path("players").get(seat);p.put("bid",bids[seat]);p.put("tricksWon",seat==0?1:0);p.put("totalScore",totals[seat]);p.set("hand",JSON.createArrayNode());var c=card(1,14-seat);c.put("seat",seat);history.add(c);}
        s.set("history",history);s.set("lastTrick",history.deepCopy());return s;
    }
}
