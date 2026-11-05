package ro.whistlab.core;

import java.util.*;
import org.junit.Test;
import ro.whistlab.core.baseline.RoundFourteenMonteCarloAi;

public class ConditionedBidForecastTest {
    private static Card c(String value){return LossMiningTest.card(value);}
    private static List<Card> hand(String... cards){return Arrays.stream(cards).map(ConditionedBidForecastTest::c).toList();}
    private static List<List<Card>> world(){return List.of(hand("AS","KS","QS","7H"),hand("AH","KH","7C","8C"),hand("AC","KC","7D","8D"),hand("AD","KD","9H","10H"));}
    private static MonteCarloAi.View view(GameMode mode,int size,int actor,int dealer,Integer[] bids,List<List<Card>> cards){
        var seats=new ArrayList<MonteCarloAi.Seat>();for(int s=0;s<cards.size();s++)seats.add(new MonteCarloAi.Seat(cards.get(s).size(),bids[s],0));
        return new MonteCarloAi.View(mode,Trump.SPADES,actor,dealer,size,cards.get(actor),seats,List.of(),List.of(),null);
    }
    private static List<Integer> targets(MonteCarloAi.View view,List<List<Card>> cards,int own){return Arrays.stream(MonteCarloAi.biddingTargets(view,cards,own)).boxed().toList();}
    private static RoundFourteenMonteCarloAi.View frozen(MonteCarloAi.View v){
        return new RoundFourteenMonteCarloAi.View(v.mode(),v.trump(),v.seat(),v.dealer(),v.handSize(),v.hand(),v.seats().stream().map(s->new RoundFourteenMonteCarloAi.Seat(s.remaining(),s.bid(),s.won(),s.total(),s.streak())).toList(),v.trick(),v.history(),v.turnedCard(),v.streakBonuses(),v.streakBonusPoints());
    }
    private static List<Integer> frozenTargets(MonteCarloAi.View view,List<List<Card>> cards,int own)throws Exception {
        // The frozen fixture lives in another package; reflect only its original package-private test seam.
        var method=RoundFourteenMonteCarloAi.class.getDeclaredMethod("biddingTargets",RoundFourteenMonteCarloAi.View.class,List.class,int.class);
        method.setAccessible(true);return Arrays.stream((int[])method.invoke(null,frozen(view),cards,own)).boxed().toList();
    }

    @Test public void forecastRespectsTheLastBidRestrictionAcrossSizesTwoThroughEight()throws Exception {
        CapturedRegression.run("forecast16-legal-last",getClass().getName(),"forecastRespectsTheLastBidRestrictionAcrossSizesTwoThroughEight",Map.of("rule","The forecast selects a most likely legal observed bid. It cannot restore the forbidden final total; an exact probability tie selects the lower bid. This is forecast semantics, not an optimal-action label."),r->{
            for(int size=2;size<=8;size++){
                var cards=new ArrayList<>(GameRules.deck(GameMode.ROMANIAN_WHIST,4).subList(0,size));
                for(int prefix=0;prefix<=3*size;prefix++){
                    int actual=BidForecast.forecast(cards,size,Trump.NO_TRUMP,null,3,prefix);
                    r.equal("Legal final forecast /size"+size+"/prefix"+prefix,true,GameRules.allowedBids(size,prefix,true).contains(actual));
                }
            }
            r.equal("Equal maximal likelihood keeps lower bid",0,BidForecast.mode(new double[]{0.4,0.4,0.2}));
        });
    }

    @Test public void actualPriorBidsAndTheProposedOwnContractStayAuthoritative()throws Exception {
        var cards=world();var v=view(GameMode.ROMANIAN_WHIST,4,2,3,new Integer[]{1,0,null,null},cards);
        CapturedRegression.run("forecast16-literal-targets",getClass().getName(),"actualPriorBidsAndTheProposedOwnContractStayAuthoritative",Map.of("rule","Observed prior contracts are literal facts. The hero target is the particular counterfactual being evaluated, not a model guess. Only the remaining unknown opponent target may be forecast."),r->{
            for(int own:List.of(0,2,4))r.equal("Known prefix and own target /own"+own,List.of(1,0,own),targets(v,cards,own).subList(0,3));
        });
    }

    @Test public void futureForecastsUseTheEvolvingAuctionPrefixAndSeatOrder()throws Exception {
        var cards=world();var v=view(GameMode.ROMANIAN_WHIST,4,0,3,new Integer[4],cards);
        var rotated=List.of(cards.get(2),cards.get(3),cards.get(0),cards.get(1));var rotatedView=view(GameMode.ROMANIAN_WHIST,4,2,1,new Integer[4],rotated);
        CapturedRegression.run("forecast16-prefix-chain",getClass().getName(),"futureForecastsUseTheEvolvingAuctionPrefixAndSeatOrder",Map.of("rule","Every forecast sees the actual preceding bids, the hero counterfactual and earlier predicted future bids. The literal targets were independently computed with the frozen JavaScript distribution/model71b51b8a; they are model-parity expectations, not winning-bid labels.","hypotheticalHands",cards,"turnedCard","8S","sourceModelSha256",LearnedBidEvidence.SOURCE_SHA256),r->{
            // The independent references use turned8S; expose the same public card in all three observations.
            var exposed=new MonteCarloAi.View(v.mode(),v.trump(),v.seat(),v.dealer(),v.handSize(),v.hand(),v.seats(),v.trick(),v.history(),c("8S"));
            var exposedRotated=new MonteCarloAi.View(rotatedView.mode(),rotatedView.trump(),rotatedView.seat(),rotatedView.dealer(),rotatedView.handSize(),rotatedView.hand(),rotatedView.seats(),rotatedView.trick(),rotatedView.history(),c("8S"));
            r.equal("Own zero leaves final target one",List.of(0,1,1,1),targets(exposed,cards,0));
            r.equal("Own one and two prior guesses forbid final one",List.of(1,1,1,2),targets(exposed,cards,1));
            r.equal("Rotated table keeps auction position semantics",List.of(1,2,1,1),targets(exposedRotated,rotated,1));
        });
    }

    @Test public void unsupportedModesSizesAndInvalidOriginalHandsKeepTheFrozenForecast()throws Exception {
        var cards=world();var singleton=List.of(hand("AS"),hand("AH"),hand("AC"),hand("AD"));
        var h1=view(GameMode.ROMANIAN_WHIST,1,0,3,new Integer[4],singleton);var ohHell=view(GameMode.OH_HELL,4,0,3,new Integer[4],cards);
        var duplicate=new ArrayList<List<Card>>(cards);duplicate.set(3,hand("AD","AD","9H","10H"));var invalid=view(GameMode.ROMANIAN_WHIST,4,0,3,new Integer[4],duplicate);
        var shortened=new ArrayList<List<Card>>(cards);shortened.set(3,hand("AD","KD","9H"));var badSize=view(GameMode.ROMANIAN_WHIST,4,0,3,new Integer[4],shortened);
        CapturedRegression.run("forecast16-exact-fallback",getClass().getName(),"unsupportedModesSizesAndInvalidOriginalHandsKeepTheFrozenForecast",Map.of("rule","The new forecast is limited to a valid original Romanian4 hand of2..8 cards at bid time. The exact accepted honor estimate and final-bid adjustment remain for h1, OhHell, duplicate or incomplete hypothetical holdings."),r->{
            r.equal("Romanian one-card target baseline",frozenTargets(h1,singleton,0),targets(h1,singleton,0));
            r.equal("OhHell target baseline",frozenTargets(ohHell,cards,1),targets(ohHell,cards,1));
            r.equal("Invalid duplicate-hand target fallback",frozenTargets(invalid,duplicate,1),targets(invalid,duplicate,1));
            r.equal("Incomplete original-hand target fallback",frozenTargets(badSize,shortened,1),targets(badSize,shortened,1));
            r.equal("Romanian h1 actual bid baseline",RoundFourteenMonteCarloAi.chooseBid(frozen(h1)),MonteCarloAi.chooseBid(h1));
            r.equal("OhHell actual bid baseline",RoundFourteenMonteCarloAi.chooseBid(frozen(ohHell)),MonteCarloAi.chooseBid(ohHell));
        });
    }
}
