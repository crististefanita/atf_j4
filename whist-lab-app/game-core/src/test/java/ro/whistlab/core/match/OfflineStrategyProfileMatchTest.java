package ro.whistlab.core.match;

import ro.whistlab.core.ai.profile.HardStrategyProfile;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.rules.GameMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** Compares alternative decisions from every seat over identical deals, without exposing future hands. */
public final class OfflineStrategyProfileMatchTest {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final List<HardStrategyProfile> PROFILES=List.of(HardStrategyProfile.CURRENT,
            HardStrategyProfile.PRUDENT_BID_04,HardStrategyProfile.PRUDENT_BID,
            HardStrategyProfile.PRUDENT_BID_12,HardStrategyProfile.SCORE_FIRST,HardStrategyProfile.BURN_IF_DOOMED);

    record ProfileResult(String profile,int focalSeat,String roster,int focalPoints,int normalBaselinePoints,
            int pointsDeltaVsNormalBaseline,int pointsDeltaFromCurrent,int exactContracts,
            int normalBaselineExactContracts,int exactDeltaVsNormalBaseline,int exactDeltaFromCurrent,
            int overtricks,int undertricks,int sumOfBids,int bidChangesFromCurrent,
            int cardChangesFromCurrent,int impossibleQuotaActions,int salvageChoiceActions,
            int certifiedSalvageActions,OfflineMockMatchTest.MockMatch match) { }
    record BaselineSeat(int seat,int points,int exactContracts) { }
    record ProfileMatrix(int schemaVersion,String mode,long seed,int runs,int handsPerRun,
            int opaqueDealCommitmentsCompared,boolean sameDistributionAndOrder,
            boolean privateDealsIncluded,List<BaselineSeat> normalBaseline,List<ProfileResult> profiles) { }

    @Test public void comparesBidAndCardProfilesOnTheSameDealsAtEverySeat() throws Exception {
        long seed=Long.getLong("whist.offlineStrategy.seed",20261104L);
        List<ProfileSpec> specs=new ArrayList<>();
        specs.add(new ProfileSpec("NORMAL_BASELINE",-1,
                List.of(AiDifficulty.NORMAL,AiDifficulty.NORMAL,AiDifficulty.NORMAL,AiDifficulty.NORMAL),HardStrategyProfile.CURRENT));
        for(int seat=0;seat<4;seat++)for(HardStrategyProfile profile:PROFILES){
            List<AiDifficulty> roster=new ArrayList<>(List.of(AiDifficulty.NORMAL,AiDifficulty.NORMAL,AiDifficulty.NORMAL,AiDifficulty.NORMAL));
            roster.set(seat,AiDifficulty.HARD);
            specs.add(new ProfileSpec("seat"+seat+"_HARD_"+profile.name(),seat,List.copyOf(roster),profile));
        }

        List<OfflineMockMatchTest.MatchRun> runs=new ArrayList<>();
        List<OfflineMockMatchTest.MockMatch> matches=new ArrayList<>();
        List<String> reference=null;int compared=0;boolean sameDeals=true;
        for(ProfileSpec spec:specs){
            OfflineMockMatchTest.MatchRun run=OfflineMockMatchTest.playMatch(seed,spec.roster(),spec.profile());
            assertTrue(run.match().complete());assertEquals(24,run.privateDealCommitments().size());
            if(reference==null)reference=run.privateDealCommitments();
            else {assertEquals(reference,run.privateDealCommitments());compared+=reference.size();}
            sameDeals&=reference.equals(run.privateDealCommitments());
            runs.add(run);matches.add(run.match());
        }
        assertTrue(sameDeals);assertEquals(24*24,compared);assertEquals(25,matches.size());
        OfflineMockMatchTest.MockMatch normalBaseline=matches.get(0);
        List<BaselineSeat> baselineSeats=new ArrayList<>();
        for(int seat=0;seat<4;seat++)baselineSeats.add(metrics(normalBaseline,seat));

        List<ProfileResult> results=new ArrayList<>();
        for(int runIndex=1;runIndex<specs.size();runIndex++){
            ProfileSpec spec=specs.get(runIndex);OfflineMockMatchTest.MockMatch match=matches.get(runIndex);
            int focal=spec.focalSeat();BaselineSeat normal=baselineSeats.get(focal);
            OfflineMockMatchTest.MockMatch current=findMatch(matches,specs,"seat"+focal+"_HARD_CURRENT");
            SeatMetrics actual=seatMetrics(match,focal),currentMetrics=seatMetrics(current,focal);
            int changedBids=0,changedCards=0;
            if(spec.profile()!=HardStrategyProfile.CURRENT){
                for(int hand=0;hand<24;hand++){
                    int currentBid=bid(current.hands().get(hand),focal).bid();
                    if(actual.bids().get(hand)!=currentBid)changedBids++;
                    List<String> baselineCards=cards(current.hands().get(hand),focal),candidateCards=actual.cards().get(hand);
                    for(int action=0;action<Math.min(baselineCards.size(),candidateCards.size());action++)
                        if(!baselineCards.get(action).equals(candidateCards.get(action)))changedCards++;
                }
            }
            results.add(new ProfileResult(spec.name(),focal,OfflineMockMatchTest.rosterLabel(spec.roster()),
                    actual.points(),normal.points(),actual.points()-normal.points(),actual.points()-currentMetrics.points(),
                    actual.exact(),normal.exactContracts(),actual.exact()-normal.exactContracts(),actual.exact()-currentMetrics.exact(),
                    actual.over(),actual.under(),actual.bidSum(),changedBids,changedCards,
                    actual.impossibleQuota(),actual.salvageChoices(),actual.certifiedSalvage(),match));
        }
        ProfileMatrix matrix=new ProfileMatrix(1,GameMode.ROMANIAN_WHIST.name(),seed,results.size()+1,24,
                compared,sameDeals,false,List.copyOf(baselineSeats),List.copyOf(results));
        writeIfRequested(matrix);
    }

    private static OfflineMockMatchTest.MockMatch findMatch(List<OfflineMockMatchTest.MockMatch> matches,
            List<ProfileSpec> specs,String name){
        for(int i=0;i<specs.size();i++)if(specs.get(i).name().equals(name))return matches.get(i);
        throw new IllegalStateException("Missing strategy profile match: "+name);
    }

    private static BaselineSeat metrics(OfflineMockMatchTest.MockMatch match,int seat){
        SeatMetrics values=seatMetrics(match,seat);return new BaselineSeat(seat,values.points(),values.exact());
    }

    private static SeatMetrics seatMetrics(OfflineMockMatchTest.MockMatch match,int seat){
        int points=0,exact=0,over=0,under=0,bidSum=0,impossible=0,salvage=0,certified=0;
        List<Integer> bids=new ArrayList<>();List<List<String>> cards=new ArrayList<>();
        for(OfflineMockMatchTest.HandRecord hand:match.hands()){
            OfflineMockMatchTest.SeatResult result=hand.results().stream().filter(row->row.seat()==seat).findFirst().orElseThrow();
            points+=result.points();if(result.bid()==result.tricksWon())exact++;
            over+=Math.max(0,result.tricksWon()-result.bid());under+=Math.max(0,result.bid()-result.tricksWon());bidSum+=result.bid();
            bids.add(bid(hand,seat).bid());cards.add(cards(hand,seat));
            for(OfflineMockMatchTest.CardEvent event:hand.plays())if(event.seat()==seat){
                if(Boolean.TRUE.equals(event.hardContractImpossible()))impossible++;
                if(Boolean.TRUE.equals(event.hardContractImpossible())&&event.legalOptionCount()>1)salvage++;
                if(event.decision().contains("public salvage dominance certificate"))certified++;
            }
        }
        return new SeatMetrics(points,exact,over,under,bidSum,impossible,salvage,certified,List.copyOf(bids),List.copyOf(cards));
    }

    private static OfflineMockMatchTest.BidEvent bid(OfflineMockMatchTest.HandRecord hand,int seat){
        return hand.bids().stream().filter(event->event.seat()==seat).findFirst().orElseThrow();
    }

    private static List<String> cards(OfflineMockMatchTest.HandRecord hand,int seat){
        return hand.plays().stream().filter(event->event.seat()==seat)
                .map(OfflineMockMatchTest.CardEvent::card).toList();
    }

    private static void writeIfRequested(ProfileMatrix matrix)throws Exception{
        String configured=System.getProperty("whist.offlineStrategy.output","").strip();if(configured.isEmpty())return;
        Path output=Path.of(configured);Files.createDirectories(output.toAbsolutePath().getParent());
        Files.writeString(output,JSON.writerWithDefaultPrettyPrinter().writeValueAsString(matrix)+"\n",StandardOpenOption.CREATE_NEW);
    }

    private record ProfileSpec(String name,int focalSeat,List<AiDifficulty> roster,HardStrategyProfile profile){
        ProfileSpec { roster=List.copyOf(roster); }
    }
    private record SeatMetrics(int points,int exact,int over,int under,int bidSum,int impossibleQuota,
            int salvageChoices,int certifiedSalvage,List<Integer> bids,List<List<String>> cards) { }
}
