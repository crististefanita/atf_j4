package ro.whistlab.core;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
import com.fasterxml.jackson.databind.*;

public class LearnedBidEvidenceTest {
    private static Card c(String s){return LossMiningTest.card(s);}
    private static List<Card> hand(String... cards){return Arrays.stream(cards).map(LearnedBidEvidenceTest::c).toList();}
    private static List<List<Card>> world(boolean strong){return List.of(strong?hand("AS","KS"):hand("8H","9H"),hand("AH","AD"),hand("QC","QD"),hand("7C","7D"));}
    private static MonteCarloAi.View view(int actor,Integer[] bids,List<List<Card>> hands,List<PlayedCard> history,List<PlayedCard> table){
        var seats=new ArrayList<MonteCarloAi.Seat>();for(int s=0;s<4;s++)seats.add(new MonteCarloAi.Seat(hands.get(s).size(),bids[s],0));
        return new MonteCarloAi.View(GameMode.ROMANIAN_WHIST,Trump.SPADES,actor,3,2,hands.get(actor),seats,table,history,c("7S"));
    }
    @Test public void generatedJavaAgreesWithIndependentJavascriptFeaturesAndLikelihoods()throws Exception {
        var json=new ObjectMapper();var fixture=json.readTree(getClass().getResourceAsStream("/regressions/bid-evidence/golden.json"));int index=0;
        for(var row:fixture.path("cases")){
            var cards=new ArrayList<Card>();for(var item:row.path("cards"))cards.add(c(item.asText()));
            Trump trump=row.path("trump").asText().equals("NT")?Trump.NO_TRUMP:Trump.values()["SHCD".indexOf(row.path("trump").asText())];
            Card turned=row.path("turned").isNull()?null:c(row.path("turned").asText());
            int size=row.path("size").asInt(),position=row.path("position").asInt(),prefix=row.path("prefix").asInt();
            double[] features=LearnedBidEvidence.features(cards,size,trump,turned,position,prefix),p=LearnedBidEvidence.distribution(cards,size,trump,turned,position,prefix);
            double[] expected=json.treeToValue(row.path("features"),double[].class),probabilities=json.treeToValue(row.path("probabilities"),double[].class);
            double featureError=0,probabilityError=0;for(int i=0;i<features.length;i++)featureError=Math.max(featureError,Math.abs(features[i]-expected[i]));for(int i=0;i<p.length;i++)probabilityError=Math.max(probabilityError,Math.abs(p[i]-probabilities[i]));
            final double fe=featureError,pe=probabilityError;
            CapturedRegression.run("bid-evidence-golden-"+(index++),getClass().getName(),"generatedJavaAgreesWithIndependentJavascriptFeaturesAndLikelihoods",Map.of("rule","The generated runtime uses the independently measured bid-time feature schema and likelihood; source model digest must agree. This is model parity, not an optimal-bid label.","input",row,"actualFeatures",features,"actualProbabilities",p),r->{r.equal("Source model identity",fixture.path("sourceModelSha256").asText(),LearnedBidEvidence.SOURCE_SHA256);r.equal("Feature error within1e-12",true,fe<=1e-12);r.equal("Likelihood error within1e-12",true,pe<=1e-12);});
        }
    }
    @Test public void zeroBidCannotExcludeMasterTrumps()throws Exception {
        var p=LearnedBidEvidence.distribution(hand("AS","KS"),2,Trump.SPADES,c("7S"),0,0);
        CapturedRegression.run("bid-evidence-zero-is-soft",getClass().getName(),"zeroBidCannotExcludeMasterTrumps",Map.of("rule","A recorded zero is behavioral evidence, not proof of no high cards. Even two master trumps retain at least the declared0.75 uniform floor.","probabilities",p),r->r.equal("Zero remains possible",true,p[0]>=0.75/3));
    }
    @Test public void lastBidRestrictionIsRenormalizedAtEveryHandSize()throws Exception {
        CapturedRegression.run("bid-evidence-last-bid",getClass().getName(),"lastBidRestrictionIsRenormalizedAtEveryHandSize",Map.of("rule","At sizes2..8, the forbidden final total has zero likelihood and the remaining legal probabilities sum to one."),r->{
            for(int size=2;size<=8;size++){var cards=new ArrayList<>(GameRules.deck(GameMode.ROMANIAN_WHIST,4).subList(0,size));var p=LearnedBidEvidence.distribution(cards,size,Trump.NO_TRUMP,null,3,size-1);r.equal("Forbidden bid one /size"+size,0.0,p[1]);r.equal("Normalized legal likelihood /size"+size,true,Math.abs(Arrays.stream(p).sum()-1)<1e-12);}
        });
    }
    @Test public void noPreviousBidsMeansNoInferredEvidenceAtAnySeat()throws Exception {
        var world=world(true);
        CapturedRegression.run("bid-evidence-no-prior",getClass().getName(),"noPreviousBidsMeansNoInferredEvidenceAtAnySeat",Map.of("rule","Without observed opponent bids the model leaves each possible deal at weight one; it cannot invent future bids."),r->{for(int seat=0;seat<4;seat++)r.equal("No evidence at seat"+seat,1.0,LearnedBidEvidence.worldWeight(view(seat,new Integer[4],world,List.of(),List.of()),world));});
    }
    @Test public void ownCounterfactualLastBidIsNotEvidenceAboutEarlierPlayers()throws Exception {
        var world=world(true);var a=view(3,new Integer[]{0,1,0,null},world,List.of(),List.of());var b=view(3,new Integer[]{0,1,0,2},world,List.of(),List.of());
        CapturedRegression.run("bid-evidence-no-self-conditioning",getClass().getName(),"ownCounterfactualLastBidIsNotEvidenceAboutEarlierPlayers",Map.of("rule","A proposed final bid cannot retrospectively change the earlier players' observed bid likelihoods; the same weights compare every candidate own contract."),r->r.equal("No self-conditioning",LearnedBidEvidence.worldWeight(a,world),LearnedBidEvidence.worldWeight(b,world)));
    }
    @Test public void playedCardsReconstructTheOriginalHandWithoutDoubleCountingTheCurrentTable()throws Exception {
        var initial=world(true);var before=view(3,new Integer[]{0,1,0,0},initial,List.of(),List.of());var remaining=new ArrayList<List<Card>>(initial);remaining.set(0,hand("KS"));var played=List.of(new PlayedCard(0,c("AS")));var after=view(3,new Integer[]{0,1,0,0},remaining,played,played);
        CapturedRegression.run("bid-evidence-original-hand",getClass().getName(),"playedCardsReconstructTheOriginalHandWithoutDoubleCountingTheCurrentTable",Map.of("rule","The bid was made holding AS/KS. After AS is public and also on the current table, reconstruct AS/KS once, not KS alone and not AS/AS/KS. Later play is not used to promote honors at bid time."),r->r.equal("Original bid likelihood is preserved",LearnedBidEvidence.worldWeight(before,initial),LearnedBidEvidence.worldWeight(after,remaining)));
    }
    @Test public void laterScoresDoNotBecomeRetrospectiveBidTimeFeatures()throws Exception {
        var world=world(true);var a=view(1,new Integer[]{0,null,null,null},world,List.of(),List.of());var seats=a.seats().stream().map(s->new MonteCarloAi.Seat(s.remaining(),s.bid(),s.won(),s.total()+100,4)).toList();var b=new MonteCarloAi.View(a.mode(),a.trump(),a.seat(),a.dealer(),a.handSize(),a.hand(),seats,a.trick(),a.history(),a.turnedCard());
        CapturedRegression.run("bid-evidence-no-later-score",getClass().getName(),"laterScoresDoNotBecomeRetrospectiveBidTimeFeatures",Map.of("rule","This observed-bid model uses only its declared bid-time features. Later score/streak metadata does not change this likelihood; the accepted action utility remains independently score-aware."),r->r.equal("Bid-time likelihood stable",LearnedBidEvidence.worldWeight(a,world),LearnedBidEvidence.worldWeight(b,world)));
    }
    @Test public void concentratedWeightsFallBackToTheAcceptedSamplerEstimate()throws Exception {
        var weights=new LearnedBidEvidence.Weights();weights.add(4);for(int i=0;i<31;i++)weights.add(0.25);
        CapturedRegression.run("bid-evidence-low-ess",getClass().getName(),"concentratedWeightsFallBackToTheAcceptedSamplerEstimate",Map.of("rule","One dominant hypothesis among32 samples must not trigger a fragile posterior. ESS below half of sample count keeps the original estimate.","effectiveSampleSize",weights.effectiveSampleSize()),r->{r.equal("Insufficient effective support",true,weights.effectiveSampleSize()<16);r.equal("Fallback required",false,weights.usable());});
    }
    @Test public void equalWeightsKeepTheOriginalChoiceExactly()throws Exception {
        var weights=new LearnedBidEvidence.Weights();for(int i=0;i<32;i++)weights.add(1);
        CapturedRegression.run("bid-evidence-uniform-fallback",getClass().getName(),"equalWeightsKeepTheOriginalChoiceExactly",Map.of("rule","Unit evidence preserves the exact accepted path and random stream."),r->{r.equal("Effective samples",32.0,weights.effectiveSampleSize());r.equal("No posterior change",false,weights.usable());});
    }
    @Test public void knownBidsSoftlyRankPossibleOpponentHands()throws Exception {
        var strong=world(true);var weak=world(false);var low=view(1,new Integer[]{0,null,null,null},strong,List.of(),List.of());var high=view(1,new Integer[]{2,null,null,null},strong,List.of(),List.of());
        double strongLow=LearnedBidEvidence.worldWeight(low,strong),weakLow=LearnedBidEvidence.worldWeight(low,weak),strongHigh=LearnedBidEvidence.worldWeight(high,strong),weakHigh=LearnedBidEvidence.worldWeight(high,weak);
        CapturedRegression.run("bid-evidence-holding-order",getClass().getName(),"knownBidsSoftlyRankPossibleOpponentHands",Map.of("rule","Observed zero makes AS/KS less plausible than two low side cards under the learned behavior model; observed two reverses that ordering. All worlds remain possible; these are hypotheses, not actual private hands.","weights",List.of(strongLow,weakLow,strongHigh,weakHigh)),r->{r.equal("Zero favors the weaker holding",true,strongLow<weakLow);r.equal("Two favors the stronger holding",true,strongHigh>weakHigh);r.equal("No hard elimination",true,strongLow>0&&weakLow>0&&strongHigh>0&&weakHigh>0);});
    }
    @Test public void anEarlierDecisionCannotUseInjectedFutureBids()throws Exception {
        var world=world(true);var before=view(1,new Integer[]{0,null,null,null},world,List.of(),List.of());var future=view(1,new Integer[]{0,null,2,2},world,List.of(),List.of());
        CapturedRegression.run("bid-evidence-no-future-bids",getClass().getName(),"anEarlierDecisionCannotUseInjectedFutureBids",Map.of("rule","Within the bid-likelihood layer, an observer who has not bid can use only the actual preceding prefix. Injected later values cannot alter this likelihood; this helper control does not test the whole chooseBid API."),r->r.equal("Future values ignored by likelihood",LearnedBidEvidence.worldWeight(before,world),LearnedBidEvidence.worldWeight(future,world)));
    }
    @Test public void actualHiddenOpponentCardsCannotChangeTheDecisionWithAKnownBidPrefix()throws Exception {
        var a=world(true);var b=List.of(a.get(0),a.get(1),a.get(3),a.get(2));var games=new ArrayList<GameSession>();for(var hands:List.of(a,b)){var game=GameSession.fromDeal(new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.HARD,442),hands,Trump.SPADES,3);game.submitBid(0,0);games.add(game);}
        var x=MonteCarloAi.observe(games.get(0).snapshot(),1);var y=MonteCarloAi.observe(games.get(1).snapshot(),1);
        CapturedRegression.run("bid-evidence-no-private-leak",getClass().getName(),"actualHiddenOpponentCardsCannotChangeTheDecisionWithAKnownBidPrefix",Map.of("rule","Two real hidden deals share the same own hand and public bid prefix. The decision API must observe and choose identically; only hypothetical worlds are weighted."),r->{r.equal("Same public observation",x,y);r.equal("Same actual bid",MonteCarloAi.chooseBid(x),MonteCarloAi.chooseBid(y));});
    }
    @Test public void constantNonUnitLikelihoodCancelsInsteadOfInventingInformation()throws Exception {
        var weights=new LearnedBidEvidence.Weights();for(int i=0;i<32;i++)weights.add(0.8);
        CapturedRegression.run("bid-evidence-constant-factor",getClass().getName(),"constantNonUnitLikelihoodCancelsInsteadOfInventingInformation",Map.of("rule","Multiplying every possible world by the same non-unit likelihood leaves the prior unchanged. ESS alone cannot detect this non-information; retain the accepted estimate exactly."),r->{r.equal("Effective samples remain32",true,Math.abs(weights.effectiveSampleSize()-32)<1e-12);r.equal("Constant evidence must cancel",false,weights.usable());});
    }
    @Test public void aTurnedCardCannotAlsoBeHypotheticallyHeld(){assertThrows(IllegalArgumentException.class,()->LearnedBidEvidence.features(hand("AS","KS"),2,Trump.SPADES,c("AS"),0,0));}
}
