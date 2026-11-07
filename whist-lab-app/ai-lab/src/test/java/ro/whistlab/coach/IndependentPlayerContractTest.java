package ro.whistlab.coach;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;
import org.junit.Test;

/** Module-local components; no engine class or archived game is on the required input path. */
public class IndependentPlayerContractTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void localContractsDoNotLoadEngineAndroidOrAtfClasses() throws Exception {
        String runtime = System.getProperty("whist.coach.runtimeClasspath", "");
        var excluded = List.of("ro.whistlab.core.session.GameSession", "android.app.Activity",
                "atf.testkit.AtfComponentTest", "atf.reporting.extent.session.ExtentReportSession");
        atf.gradle.audit.GradleInputs.fixture("Production runtime classpath boundary", "classpath", runtime,
                "planner", IndependentPlayer.class.getName(), "excluded dependencies", excluded);
        assertFalse("Gradle must provide the real production runtime classpath", runtime.isBlank());
        var entries = runtime.split(java.util.regex.Pattern.quote(File.pathSeparator));
        var urls = new URL[entries.length];
        for (int i = 0; i < entries.length; i++) urls[i] = Path.of(entries[i]).toUri().toURL();
        try (var loader = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader())) {
            var planner = Class.forName(IndependentPlayer.class.getName(), true, loader);
            assertSame("The planner must load from its production runtime", loader, planner.getClassLoader());
            for (String name : excluded) {
                assertThrows("Production runtime cannot resolve " + name, ClassNotFoundException.class,
                        () -> Class.forName(name, false, loader));
            }
        }
    }
    @Test public void observationsOwnTheirListsAndExposeOnlyOwnAndPublicInformation() {
        var hand=new ArrayList<>(List.of(12,5));
        var seats=new ArrayList<>(List.of(new IndependentPlayer.Seat(2,0,0,0),new IndependentPlayer.Seat(2,0,0,0),
                new IndependentPlayer.Seat(2,0,0,0),new IndependentPlayer.Seat(2,0,0,0)));
        var history=new ArrayList<IndependentPlayer.Play>();
        var p=new IndependentPlayer.Position(0,3,2,-1,hand,seats,history,history,-1);
        hand.clear();seats.clear();history.add(new IndependentPlayer.Play(1,25));

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "hand", hand, "history", history, "p", p, "seats", seats);
        assertEquals("p.hand() must equal List.of(5, 12)", List.of(5,12),p.hand());assertEquals("p.seats().size() must equal 4", 4,p.seats().size());assertTrue("p.history().isEmpty() must hold", p.history().isEmpty());
        assertThrows("The operation rejects its input with UnsupportedOperationException.class", UnsupportedOperationException.class,()->p.hand().clear());
        Set<String> fields=Arrays.stream(IndependentPlayer.Position.class.getRecordComponents()).map(c->c.getName()).collect(Collectors.toSet());
        assertEquals("fields must equal Set.of(\"seat\", \"dealer\", \"size\", \"trump\", \"hand\", \"seats\", \"trick\", \"history\", \"turnedCard\", \"streakBonuses\", \"streakBonusPoints\", \"streakPenaltyPoints\")", Set.of("seat","dealer","size","trump","hand","seats","trick","history","turnedCard",
                "streakBonuses","streakBonusPoints","streakPenaltyPoints"),fields);
    }
    @Test public void bidsRespectTheRangeAndTheDealersForbiddenTotalAtTwoToEightCards() {
        for(int size=2;size<=8;size++)for(boolean last:List.of(false,true)) {
            var p=LocalPositions.initial(size,19,true,last);int bid=IndependentPlayer.bid(p).value();

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "bid", bid, "last", last, "p", p, "size", size);
            assertTrue("bid >= 0 && bid <= size must hold", bid>=0&&bid<=size);if(last)assertNotEquals("bid must differ from size", size,bid);
        }
    }
    @Test public void cardsRespectTheIndependentFixtureOracleAtLeadAndFollowForTwoToEightCards() {
        for(int size=2;size<=8;size++)for(boolean follow:List.of(false,true)) {
            var p=LocalPositions.initial(size,23,false,follow);

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "follow", follow, "p", p, "size", size);
            assertTrue("LocalPositions.legal(p.hand(), p.trick(), p.trump()).contains(IndependentPlayer.card(p).value()) must hold", LocalPositions.legal(p.hand(),p.trick(),p.trump()).contains(IndependentPlayer.card(p).value()));
        }
    }
    @Test public void followSuitAndMandatoryTrumpDoNotRequireOvertrumping() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 5, "declared constant 2", 12, "declared constant 3", 18, "declared constant 4", 0, "declared constant 5", 1, "declared constant 6", 25, "declared constant 7", 31, "declared constant 8", 38, "declared constant 9", 44);
        assertEquals("IndependentPlayer.legal(List.of(5, 12, 18), 0, 1) must equal List.of(5, 12)", List.of(5,12),IndependentPlayer.legal(List.of(5,12,18),0,1));
        assertEquals("IndependentPlayer.legal(List.of(18, 25, 31), 0, 1) must equal List.of(18, 25)", List.of(18,25),IndependentPlayer.legal(List.of(18,25,31),0,1));
        assertEquals("IndependentPlayer.legal(List.of(31, 38, 44), 0, 1) must equal List.of(31, 38, 44)", List.of(31,38,44),IndependentPlayer.legal(List.of(31,38,44),0,1));
    }
    @Test public void measurementPreservesDecisionsAndDoesNotMutateTheObservation() {
        for(int size=2;size<=8;size++) {
            var bid=LocalPositions.initial(size,31,true,false);var card=LocalPositions.initial(size,31,false,false);
            var expectedBid=IndependentPlayer.bid(bid);var expectedCard=IndependentPlayer.card(card);
            int before=card.hashCode();
            try(var scope=PlannerControl.measure(5_000_000)) {

                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "before", before, "bid", bid, "card", card, "expectedBid", expectedBid, "expectedCard", expectedCard, "size", size);
                assertEquals("IndependentPlayer.bid(bid) must equal expectedBid", expectedBid,IndependentPlayer.bid(bid));assertEquals("IndependentPlayer.card(card) must equal expectedCard", expectedCard,IndependentPlayer.card(card));
                assertTrue("scope.snapshot().workUnits() > 0 must hold", scope.snapshot().workUnits()>0);assertEquals("scope.snapshot().stopReason() must equal \"NONE\"", "NONE",scope.snapshot().stopReason());
            }
            assertEquals("card.hashCode() must equal before", before,card.hashCode());assertEquals("IndependentPlayer.card(card) must equal expectedCard", expectedCard,IndependentPlayer.card(card));
        }
    }
    @Test public void cancellationAbortsEvenFastPathsAndPreservesTheInterruptFlag() {
        var p=LocalPositions.initial(2,3,false,false);
        try(var scope=PlannerControl.measure(100)) {
            Thread.currentThread().interrupt();

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "p", p);
            assertThrows("The operation rejects its input with CancellationException.class", CancellationException.class,()->IndependentPlayer.bid(p));
            assertThrows("The operation rejects its input with CancellationException.class", CancellationException.class,()->IndependentPlayer.card(p));
            assertTrue("Thread.currentThread().isInterrupted() must hold", Thread.currentThread().isInterrupted());assertEquals("scope.snapshot().stopReason() must equal \"INTERRUPTED\"", "INTERRUPTED",scope.snapshot().stopReason());
        } finally { Thread.interrupted(); }
    }
    @Test public void exhaustedWorkNeverReturnsAPartialDecisionOrLeaksIntoTheNextCall() {
        var p=LocalPositions.initial(4,8,true,false);
        for(int limit:List.of(0,1,23))try(var scope=PlannerControl.measure(limit)) {

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "limit", limit, "p", p);
            assertThrows("The operation rejects its input with PlannerControl.LimitExceeded.class", PlannerControl.LimitExceeded.class,()->IndependentPlayer.bid(p));
            assertEquals("scope.snapshot().workUnits() must equal limit", limit,scope.snapshot().workUnits());assertEquals("scope.snapshot().stopReason() must equal \"WORK_LIMIT\"", "WORK_LIMIT",scope.snapshot().stopReason());
        }
        assertNotNull("IndependentPlayer.bid(p) is available", IndependentPlayer.bid(p));
    }
    @Test public void samplingDetectsMidWorkCancellationWithoutMutatingPublicFacts() {
        var p=LocalPositions.initial(4,19,false,false);int before=p.hashCode();
        var random=new java.util.Random(17) {
            @Override public int nextInt(int bound){Thread.currentThread().interrupt();return super.nextInt(bound);}
        };
        try {

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "before", before, "p", p, "random", random);
            assertThrows("The operation rejects its input with CancellationException.class", CancellationException.class,()->IndependentDeals.deal(p,random));
            assertTrue("Thread.currentThread().isInterrupted() must hold", Thread.currentThread().isInterrupted());assertEquals("p.hashCode() must equal before", before,p.hashCode());
        } finally {Thread.interrupted();}
    }
    @Test public void incompleteUniversalProofKeepsItsLegalProposalWithinTheNodeBudget() {
        var p=LocalPositions.endgame();int selected=LocalPositions.legal(p.hand(),p.trick(),p.trump()).get(0);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "p", p, "selected", selected);
        assertFalse("IndependentPlayer.smallDeals(p).isEmpty() must remain false", IndependentPlayer.smallDeals(p).isEmpty());
        for(int limit:List.of(0,1)) {
            var proof=ContractGuarantee.choose(p,selected,limit);
            assertFalse("proof.certified() must remain false", proof.certified());assertEquals("proof.card() must equal selected", selected,proof.card());assertTrue("proof.worlds() > 0 must hold", proof.worlds()>0);
            assertTrue("proof.nodes() <= limit must hold", proof.nodes()<=limit);
        }
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->ContractGuarantee.choose(p,selected,-1));
    }
}
