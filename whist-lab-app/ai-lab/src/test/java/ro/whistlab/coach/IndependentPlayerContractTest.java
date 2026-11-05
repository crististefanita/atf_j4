package ro.whistlab.coach;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;
import org.junit.Test;

/** Module-local components; no engine class or archived game is on the required input path. */
public class IndependentPlayerContractTest {
    @Test public void localContractsDoNotLoadEngineAndroidOrAtfClasses() {
        for(String name:List.of("ro.whistlab.core.session.GameSession","android.app.Activity","atf.testkit.AtfComponentTest"))
            assertThrows(ClassNotFoundException.class,()->Class.forName(name));
    }
    @Test public void observationsOwnTheirListsAndExposeOnlyOwnAndPublicInformation() {
        var hand=new ArrayList<>(List.of(12,5));
        var seats=new ArrayList<>(List.of(new IndependentPlayer.Seat(2,0,0,0),new IndependentPlayer.Seat(2,0,0,0),
                new IndependentPlayer.Seat(2,0,0,0),new IndependentPlayer.Seat(2,0,0,0)));
        var history=new ArrayList<IndependentPlayer.Play>();
        var p=new IndependentPlayer.Position(0,3,2,-1,hand,seats,history,history,-1);
        hand.clear();seats.clear();history.add(new IndependentPlayer.Play(1,25));
        assertEquals(List.of(5,12),p.hand());assertEquals(4,p.seats().size());assertTrue(p.history().isEmpty());
        assertThrows(UnsupportedOperationException.class,()->p.hand().clear());
        Set<String> fields=Arrays.stream(IndependentPlayer.Position.class.getRecordComponents()).map(c->c.getName()).collect(Collectors.toSet());
        assertEquals(Set.of("seat","dealer","size","trump","hand","seats","trick","history","turnedCard",
                "streakBonuses","streakBonusPoints","streakPenaltyPoints"),fields);
    }
    @Test public void bidsRespectTheRangeAndTheDealersForbiddenTotalAtTwoToEightCards() {
        for(int size=2;size<=8;size++)for(boolean last:List.of(false,true)) {
            var p=LocalPositions.initial(size,19,true,last);int bid=IndependentPlayer.bid(p).value();
            assertTrue(bid>=0&&bid<=size);if(last)assertNotEquals(size,bid);
        }
    }
    @Test public void cardsRespectTheIndependentFixtureOracleAtLeadAndFollowForTwoToEightCards() {
        for(int size=2;size<=8;size++)for(boolean follow:List.of(false,true)) {
            var p=LocalPositions.initial(size,23,false,follow);
            assertTrue(LocalPositions.legal(p.hand(),p.trick(),p.trump()).contains(IndependentPlayer.card(p).value()));
        }
    }
    @Test public void followSuitAndMandatoryTrumpDoNotRequireOvertrumping() {
        assertEquals(List.of(5,12),IndependentPlayer.legal(List.of(5,12,18),0,1));
        assertEquals(List.of(18,25),IndependentPlayer.legal(List.of(18,25,31),0,1));
        assertEquals(List.of(31,38,44),IndependentPlayer.legal(List.of(31,38,44),0,1));
    }
    @Test public void measurementPreservesDecisionsAndDoesNotMutateTheObservation() {
        for(int size=2;size<=8;size++) {
            var bid=LocalPositions.initial(size,31,true,false);var card=LocalPositions.initial(size,31,false,false);
            var expectedBid=IndependentPlayer.bid(bid);var expectedCard=IndependentPlayer.card(card);
            int before=card.hashCode();
            try(var scope=PlannerControl.measure(5_000_000)) {
                assertEquals(expectedBid,IndependentPlayer.bid(bid));assertEquals(expectedCard,IndependentPlayer.card(card));
                assertTrue(scope.snapshot().workUnits()>0);assertEquals("NONE",scope.snapshot().stopReason());
            }
            assertEquals(before,card.hashCode());assertEquals(expectedCard,IndependentPlayer.card(card));
        }
    }
    @Test public void cancellationAbortsEvenFastPathsAndPreservesTheInterruptFlag() {
        var p=LocalPositions.initial(2,3,false,false);
        try(var scope=PlannerControl.measure(100)) {
            Thread.currentThread().interrupt();
            assertThrows(CancellationException.class,()->IndependentPlayer.bid(p));
            assertThrows(CancellationException.class,()->IndependentPlayer.card(p));
            assertTrue(Thread.currentThread().isInterrupted());assertEquals("INTERRUPTED",scope.snapshot().stopReason());
        } finally { Thread.interrupted(); }
    }
    @Test public void exhaustedWorkNeverReturnsAPartialDecisionOrLeaksIntoTheNextCall() {
        var p=LocalPositions.initial(4,8,true,false);
        for(int limit:List.of(0,1,23))try(var scope=PlannerControl.measure(limit)) {
            assertThrows(PlannerControl.LimitExceeded.class,()->IndependentPlayer.bid(p));
            assertEquals(limit,scope.snapshot().workUnits());assertEquals("WORK_LIMIT",scope.snapshot().stopReason());
        }
        assertNotNull(IndependentPlayer.bid(p));
    }
    @Test public void samplingDetectsMidWorkCancellationWithoutMutatingPublicFacts() {
        var p=LocalPositions.initial(4,19,false,false);int before=p.hashCode();
        var random=new java.util.Random(17) {
            @Override public int nextInt(int bound){Thread.currentThread().interrupt();return super.nextInt(bound);}
        };
        try {
            assertThrows(CancellationException.class,()->IndependentDeals.deal(p,random));
            assertTrue(Thread.currentThread().isInterrupted());assertEquals(before,p.hashCode());
        } finally {Thread.interrupted();}
    }
    @Test public void incompleteUniversalProofKeepsItsLegalProposalWithinTheNodeBudget() {
        var p=LocalPositions.endgame();int selected=LocalPositions.legal(p.hand(),p.trick(),p.trump()).get(0);
        assertFalse(IndependentPlayer.smallDeals(p).isEmpty());
        for(int limit:List.of(0,1)) {
            var proof=ContractGuarantee.choose(p,selected,limit);
            assertFalse(proof.certified());assertEquals(selected,proof.card());assertTrue(proof.worlds()>0);
            assertTrue(proof.nodes()<=limit);
        }
        assertThrows(IllegalArgumentException.class,()->ContractGuarantee.choose(p,selected,-1));
    }
}
