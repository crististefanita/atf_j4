package atf.android.e2e.oracle.romanian;

import atf.android.e2e.player.WhistChallenger;

import static org.junit.jupiter.api.Assertions.*;
import atf.testkit.AtfComponentTest;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.Test;

@AtfComponentTest
class RomanianMatchOracleSeriesTest {
    @Test void originalFiveAndSelectedTenReconcileTheSameCardsWithDifferentPositiveBonuses(){for(int value:List.of(5,10)){var start=start(value);var oracle=new RomanianMatchOracle(start,4,3,new int[4],"EXPERT",new int[]{4,4,-4,-4},true,value);assertTrue(oracle.start(start).stream().allMatch(a->a.passed()));assertTrue(oracle.verify(score(start,value)).stream().allMatch(a->a.passed()));assertArrayEquals(new int[]{7+value,5+value,5,-6},oracle.totals());assertArrayEquals(new int[]{0,0,1,0},oracle.streaks());}}
    @Test void wrongMenuValueCounterBonusOrCumulativeScoreIsRejectedByTheIndependentOracle(){for(String corruption:List.of("option","counter","bonus","total")){var start=start(10);var actual=score(start,10);switch(corruption){case "option"->actual.put("streakBonusPoints",5);case "counter"->((ObjectNode)actual.path("players").get(0)).put("streak",4);case "bonus"->((ObjectNode)actual.path("players").get(0)).put("streakPoints",5);case "total"->((ObjectNode)actual.path("players").get(0)).put("totalScore",12);}var oracle=new RomanianMatchOracle(start,4,3,new int[4],"EXPERT",new int[]{4,4,-4,-4},true,10);assertTrue(oracle.verify(actual).stream().anyMatch(a->!a.passed()),corruption);}}
    @Test void playerProjectionIncludesOnlyPublicSeriesConfigurationAndCounters(){var start=start(10);var p=WhistChallenger.position(start,0);assertTrue(p.streakBonuses());assertEquals(10,p.streakBonusPoints());assertEquals(List.of(4,4,-4,-4),p.seats().stream().map(ro.whistlab.coach.IndependentPlayer.Seat::streak).toList());}
    private static ObjectNode start(int value){var s=new ObjectMapper().createObjectNode();s.put("mode","ROMANIAN_WHIST");s.put("difficulty","EXPERT");s.put("phase","BIDDING");s.put("handIndex",4);s.put("handSize",2);s.put("dealerSeat",3);s.put("biddingSeat",0);s.put("currentSeat",0);s.put("bidsSubmitted",0);s.put("bidTotal",0);s.put("trump","SPADES");s.put("lastTrickWinnerSeat",-1);s.put("streakBonuses",true);s.put("streakBonusPoints",value);card(s.putObject("turnedCard"),"SPADES",7);s.putArray("history");s.putArray("currentTrick");s.putArray("lastTrick");var players=s.putArray("players");for(int seat=0;seat<4;seat++){var p=players.addObject();p.put("seat",seat);p.putNull("bid");p.put("tricksWon",0);p.put("totalScore",0);p.put("streak",new int[]{4,4,-4,-4}[seat]);p.put("streakPoints",0);var hand=p.putArray("hand");card(hand.addObject(),"HEARTS",13-2*seat);card(hand.addObject(),"HEARTS",14-2*seat);}return s;}
    private static ObjectNode score(ObjectNode start,int value){var s=start.deepCopy();s.put("phase","SCORE");s.put("bidsSubmitted",4);s.put("bidTotal",3);s.put("lastTrickWinnerSeat",0);var history=s.putArray("history");for(int round=0;round<2;round++)for(int seat=0;seat<4;seat++){var play=history.addObject();play.put("seat",seat);card(play,"HEARTS",13+round-2*seat);}var last=s.putArray("lastTrick");for(int i=4;i<8;i++)last.add(history.get(i).deepCopy());for(int seat=0;seat<4;seat++){var p=(ObjectNode)s.path("players").get(seat);p.putArray("hand");p.put("bid",new int[]{2,0,0,1}[seat]);p.put("tricksWon",seat==0?2:0);p.put("totalScore",new int[]{7+value,5+value,5,-6}[seat]);p.put("streak",new int[]{0,0,1,0}[seat]);p.put("streakPoints",new int[]{value,value,0,-5}[seat]);}return s;}
    private static void card(ObjectNode card,String suit,int rank){card.put("suit",suit);card.put("rank",rank);}
}
