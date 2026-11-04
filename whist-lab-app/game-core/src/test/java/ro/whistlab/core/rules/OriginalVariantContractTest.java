package ro.whistlab.core.rules;

import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.session.GameSession;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;

/** Remaining offline variants explicitly described below the generic Instructions section. */
public class OriginalVariantContractTest {
    @Test public void oi21DescendingStartsAtTheFullDeckMaximumIncludingSeventeenForThree(){
        for(int n=3;n<=6;n++){var expected=new ArrayList<Integer>();for(int h=52/n;h>=1;h--)expected.add(h);assertEquals(expected,GameRules.handSizes(GameMode.DESCENDING,n));}
    }
    @Test public void oi22DownAndUpHasOneMinimumAndTwoMaximumHands(){
        for(int n=3;n<=6;n++){int max=52/n;var sizes=GameRules.handSizes(GameMode.DOWN_AND_UP,n);assertEquals(2*max-1,sizes.size());assertEquals(1,Collections.frequency(sizes,1));assertEquals(2,Collections.frequency(sizes,max));for(int h=0;h<sizes.size();h++)assertEquals(Math.abs(h-max+1)+1,sizes.get(h).intValue());}
    }
    @Test public void oi24RomanianPlateausHaveOneRoundPerSeatAtBothEndsAndTheTurningPoint(){
        for(int n=3;n<=6;n++){var sizes=GameRules.handSizes(GameMode.ROMANIAN_WHIST,n);assertEquals(3*n+12,sizes.size());assertEquals(Collections.nCopies(n,1),sizes.subList(0,n));assertEquals(Collections.nCopies(n,8),sizes.subList(n+6,2*n+6));assertEquals(Collections.nCopies(n,1),sizes.subList(2*n+12,3*n+12));for(int h=0;h<sizes.size();h++)assertEquals(sizes.get(h),sizes.get(sizes.size()-1-h));}
    }
    @Test public void oi25LongRomanianReversesThePlateausAndUsesTheSameReducedDeck(){
        for(int n=3;n<=6;n++){var normal=GameRules.handSizes(GameMode.ROMANIAN_WHIST,n);var longer=GameRules.handSizes(GameMode.LONG_ROMANIAN,n);assertEquals(normal.size(),longer.size());for(int h=0;h<normal.size();h++)assertEquals(9-normal.get(h),longer.get(h).intValue());assertEquals(GameRules.deck(GameMode.ROMANIAN_WHIST,n),GameRules.deck(GameMode.LONG_ROMANIAN,n));assertEquals(8*n,new HashSet<>(GameRules.deck(GameMode.LONG_ROMANIAN,n)).size());}
    }
    @Test public void oi26OhHellIsCappedAtTenExceptSixSeatsAtEight(){for(int n=3;n<=6;n++){int max=n==6?8:10;var sizes=GameRules.handSizes(GameMode.OH_HELL,n);assertEquals(2*max-1,sizes.size());for(int h=0;h<sizes.size();h++)assertEquals(Math.abs(h-max+1)+1,sizes.get(h).intValue());}}
    @Test public void oi27ReverseOhHellHasOneMaximumAndTwoOneCardHands(){for(int n=3;n<=6;n++){int max=n==6?8:10;var sizes=GameRules.handSizes(GameMode.REVERSE_OH_HELL,n);assertEquals(2*max-1,sizes.size());for(int h=0;h<sizes.size();h++)assertEquals(max-Math.abs(h-max+1),sizes.get(h).intValue());assertEquals(1,Collections.frequency(sizes,max));assertEquals(2,Collections.frequency(sizes,1));}}
    @Test public void oi28EveryVariantCanDealEveryScheduledHandWithoutExceedingItsDeck(){for(GameMode mode:GameMode.values())for(int n=3;n<=6;n++){var sizes=GameRules.handSizes(mode,n);for(int size:sizes)assertTrue(size>0&&size*n<=GameRules.deck(mode,n).size());assertThrows(UnsupportedOperationException.class,()->sizes.clear());}}
    @Test public void oi29LongRomanianUsesFollowBeforeMandatoryTrumpWithoutForcedOvertrumping(){
        var heart=new Card(Suit.HEARTS,9);var low=new Card(Suit.SPADES,9);var high=new Card(Suit.SPADES,14);var club=new Card(Suit.CLUBS,14);
        assertEquals(List.of(heart),GameRules.legalCards(List.of(heart,low,club),Suit.HEARTS,Trump.SPADES,GameMode.LONG_ROMANIAN));
        assertEquals(List.of(low,high),GameRules.legalCards(List.of(low,high,club),Suit.HEARTS,Trump.SPADES,GameMode.LONG_ROMANIAN));
        assertEquals(List.of(low,club),GameRules.legalCards(List.of(low,club),Suit.HEARTS,Trump.NO_TRUMP,GameMode.LONG_ROMANIAN));
    }
    @Test public void oi30BothRomanianVariantsUseExactFiveAndSymmetricBidErrorBeforeOptionalBonuses(){for(GameMode mode:List.of(GameMode.ROMANIAN_WHIST,GameMode.LONG_ROMANIAN))for(int bid=0;bid<=8;bid++)for(int won=0;won<=8;won++)assertEquals(won==bid?5+bid:-Math.abs(won-bid),GameRules.scoreDelta(won,bid,mode));}
    @Test public void oi31AllNonRomanianVariantsUseTricksPlusTenForExactContracts(){for(GameMode mode:GameMode.values())if(!mode.isRomanian())for(int bid=0;bid<=17;bid++)for(int won=0;won<=17;won++)assertEquals(won+(won==bid?10:0),GameRules.scoreDelta(won,bid,mode));}
    @Test public void oi32InvalidTableSizesAreRejectedAcrossAllVariants(){for(GameMode mode:GameMode.values())for(int n:List.of(-1,0,1,2,7,52)){assertThrows(IllegalArgumentException.class,()->GameRules.handSizes(mode,n));assertThrows(IllegalArgumentException.class,()->new GameConfig(n,mode,AiDifficulty.NORMAL,0));}}
    @Test public void oi33OnlyTurnedTrumpVariantsSelectAnUndealtCard(){for(GameMode mode:GameMode.values())for(int n=3;n<=6;n++){
        var s=GameSession.newGame(new GameConfig(n,mode,AiDifficulty.NORMAL,17)).snapshot();
        if(mode.usesTurnedTrump()&&!(mode.isRomanian()&&s.handSize()==8)){assertNotNull(s.turnedCard());assertEquals(s.trump().ordinal(),s.turnedCard().suit().ordinal());assertTrue(s.players().stream().noneMatch(p->p.hand().contains(s.turnedCard())));}
        else{assertNull(s.turnedCard());assertEquals(mode.isRomanian()?Trump.NO_TRUMP:Trump.SPADES,s.trump());}
    }}
}
