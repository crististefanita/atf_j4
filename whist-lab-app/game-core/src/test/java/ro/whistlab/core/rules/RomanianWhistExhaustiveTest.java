package ro.whistlab.core.rules;

import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;

import atf.gradle.audit.matrix.ExhaustiveMatrixChecks;
import java.util.*;
import org.junit.Test;

public class RomanianWhistExhaustiveTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void rw04EveryNonemptySubsetOfEightRepresentativeCardsHasExactlyTheLegalChoices() {
        List<Card> universe=new ArrayList<>();
        for(Suit suit:Suit.values()){universe.add(new Card(suit,7));universe.add(new Card(suit,14));}
        var checks = new ExhaustiveMatrixChecks();
        for(int mask=1;mask<256;mask++) {
            List<Card> hand=new ArrayList<>();for(int i=0;i<8;i++)if((mask&(1<<i))!=0)hand.add(universe.get(i));
            for(int ledIndex=-1;ledIndex<4;ledIndex++)for(Trump trump:Trump.values()) {
                Suit led=ledIndex<0?null:Suit.values()[ledIndex];List<Card> expected=new ArrayList<>();
                if(led==null)expected.addAll(hand);
                else {
                    for(Card c:hand)if(c.suit()==led)expected.add(c);
                    if(expected.isEmpty()&&trump!=Trump.NO_TRUMP)
                        for(Card c:hand)if(c.suit().ordinal()==trump.ordinal())expected.add(c);
                    if(expected.isEmpty())expected.addAll(hand);
                }

                checks.at("hand " + hand + " / led " + led + " / trump " + trump);
                checks.equal("legal cards", expected, GameRules.legalCards(hand, led, trump, GameMode.ROMANIAN_WHIST));
            }
        }
        atf.gradle.audit.assertions.GradleAssertions.assertEquals("Exhaustive case count", 6375L, checks.checkedPairs());
        checks.publish();
    }

    @Test public void rw05EveryOrderedPairFromTheFourPlayerDeckHasTheCorrectWinnerForEveryTrump() {
        List<Card> cards=new ArrayList<>();for(Suit s:Suit.values())for(int rank=7;rank<=14;rank++)cards.add(new Card(s,rank));
        var checks = new ExhaustiveMatrixChecks();
        for(Card lead:cards)for(Card reply:cards)if(!lead.equals(reply))for(Trump trump:Trump.values()) {
            boolean replyWins=reply.suit()==lead.suit()?reply.rank()>lead.rank():
                    trump!=Trump.NO_TRUMP&&reply.suit().ordinal()==trump.ordinal();

            checks.at("lead " + lead + " / reply " + reply + " / trump " + trump);
            checks.equal("winning seat", replyWins ? 1 : 0,
                    GameRules.winningSeat(List.of(new PlayedCard(0, lead), new PlayedCard(1, reply)), trump));
        }
        atf.gradle.audit.assertions.GradleAssertions.assertEquals("Exhaustive case count", 4960L, checks.checkedPairs());
        checks.publish();
    }
}
