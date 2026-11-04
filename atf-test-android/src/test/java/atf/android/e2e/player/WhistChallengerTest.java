package atf.android.e2e.player;

import static org.junit.jupiter.api.Assertions.*;
import atf.testkit.AtfComponentTest;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.Test;
import ro.whistlab.coach.IndependentPlayer;

@AtfComponentTest
class WhistChallengerTest {
    @Test void opponentsPrivateCardsAndShuffleSeedCannotAffectTheDecision(){
        ObjectNode a=position(),b=a.deepCopy();b.put("seed",999999999L);
        for(int s=1;s<4;s++)for(JsonNode c:b.path("players").get(s).path("hand"))((ObjectNode)c).put("rank",14-c.path("rank").asInt()+7);
        var first=WhistChallenger.position(a,0);var second=WhistChallenger.position(b,0);assertEquals(first,second);
        assertEquals(IndependentPlayer.bid(first),IndependentPlayer.bid(second));assertEquals(IndependentPlayer.card(first),IndependentPlayer.card(second));
    }
    @Test void independentPlayerFollowsSuitAndCannotMutateItsInput(){
        ObjectNode s=position();var trick=(ArrayNode)s.path("currentTrick");var card=trick.addObject();card.put("seat",3);card.put("suit","HEARTS");card.put("rank",14);s.set("history",trick.deepCopy());
        var p=WhistChallenger.position(s,0);assertEquals(18,IndependentPlayer.card(p).value());assertThrows(UnsupportedOperationException.class,()->p.hand().clear());
    }
    private static ObjectNode position(){
        ObjectMapper json=new ObjectMapper();ObjectNode s=json.createObjectNode();s.put("seed",123);s.put("dealerSeat",3);s.put("handSize",2);s.put("trump","SPADES");s.putNull("turnedCard");s.putArray("history");s.putArray("currentTrick");var players=s.putArray("players");
        for(int seat=0;seat<4;seat++){var p=players.addObject();p.put("bid",0);p.put("tricksWon",0);p.put("totalScore",0);var hand=p.putArray("hand");var first=hand.addObject();first.put("suit",seat==0?"HEARTS":"CLUBS");first.put("rank",7+seat);var second=hand.addObject();second.put("suit",seat==0?"SPADES":"DIAMONDS");second.put("rank",14-seat);}
        return s;
    }
}
