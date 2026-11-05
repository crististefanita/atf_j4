package atf.android.e2e.player;

import atf.android.e2e.oracle.romanian.RomanianMatchOracle;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import ro.whistlab.coach.IndependentPlayer;

/** Deliberate redaction boundary between privileged auditing and the player making decisions. */
public final class WhistChallenger {
    public static IndependentPlayer.Position position(JsonNode s,int hero){
        var own=new ArrayList<Integer>();for(JsonNode c:s.path("players").get(hero).path("hand"))own.add(RomanianMatchOracle.card(c));
        var seats=new ArrayList<IndependentPlayer.Seat>();for(JsonNode p:s.path("players"))seats.add(new IndependentPlayer.Seat(p.path("hand").size(),p.path("bid").isNull()?null:p.path("bid").asInt(),p.path("tricksWon").asInt(),p.path("totalScore").asInt(),p.path("streak").asInt()));
        return new IndependentPlayer.Position(hero,s.path("dealerSeat").asInt(),s.path("handSize").asInt(),List.of("SPADES","HEARTS","CLUBS","DIAMONDS","NO_TRUMP").indexOf(s.path("trump").asText()),own,seats,plays(s.path("currentTrick")),plays(s.path("history")),s.path("turnedCard").isNull()?-1:RomanianMatchOracle.card(s.path("turnedCard")),s.path("streakBonuses").asBoolean(),s.path("streakBonusPoints").asInt(5),s.path("streakPenaltyPoints").asInt(5));
    }
    private static List<IndependentPlayer.Play> plays(JsonNode a){var p=new ArrayList<IndependentPlayer.Play>();for(JsonNode x:a)p.add(new IndependentPlayer.Play(x.path("seat").asInt(),RomanianMatchOracle.card(x)));return p;}
}
