package ro.whistlab.app.bidding;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ro.whistlab.app.lifecycle.AiTurnCoordinator;
import ro.whistlab.app.navigation.AppNavigation;
import ro.whistlab.app.persistence.GameSessionStore;
import ro.whistlab.core.session.GameSession;

@RunWith(Parameterized.class)
public final class HumanBidResumeComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Parameterized.Parameters(name = "{0} players, {1} cards")
    public static Collection<Object[]> configurations() { return BiddingMatrixFixtures.tableSizes(); }

    private final int players;
    private final int size;

    public HumanBidResumeComponentTest(int players, int size) {
        this.players = players;
        this.size = size;
    }

    @Test public void jsonRoundTripRecomputesTheSameAdviceWithoutPersistingItAsAContract() throws Exception {
        var game = fixture(); var selection = new HumanBidSelection();
        var before = game.saveState(); var request = selection.request(game).orElseThrow();
        int recommendation = request.evaluate();
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "before", before, "game", game, "recommendation", recommendation, "request", request, "selection", selection);
        assertTrue("selection.apply(game, request, recommendation) must hold", selection.apply(game, request, recommendation));
        var waiting = new HumanBidSelection(); var interrupted = waiting.request(game).orElseThrow();
        var restored = roundTrip(game);
        assertNotSame("restored is a distinct instance from game", game, restored);
        assertEquals("restored.saveState() must equal before", before, restored.saveState());
        assertFalse("waiting.apply(restored, interrupted, recommendation) must remain false", waiting.apply(restored, interrupted, recommendation));
        assertFalse("waiting.failed(restored, interrupted) must remain false", waiting.failed(restored, interrupted));
        assertTrue("selection.selected(restored).isEmpty() must hold", selection.selected(restored).isEmpty());
        assertFalse("selection.apply(restored, request, recommendation) must remain false", selection.apply(restored, request, recommendation));
        var resumed = selection.request(restored).orElseThrow();
        assertEquals("resumed.observation() must equal request.observation()", request.observation(), resumed.observation());
        assertEquals("resumed.evaluate() must equal recommendation", recommendation, resumed.evaluate());
        assertTrue("selection.apply(restored, resumed, recommendation) must hold", selection.apply(restored, resumed, recommendation));
        assertNull("restored.snapshot().players().get(0).bid() remains absent", restored.snapshot().players().get(0).bid());
        assertEquals("restored.saveState() must equal before", before, restored.saveState());
    }

    @Test public void suspendingTheSameSessionKeepsManualChoiceAndSuppressesQueuedAdvice() {
        var game = fixture();
        var before = game.saveState();
        var selection = new HumanBidSelection();
        var legal = BiddingMatrixFixtures.legalBids(game.snapshot());
        int manual = legal.get(legal.size() - 1);
        var worker = new BidSelectionFixtures.Worker();
        var owner = new ArrayDeque<Runnable>();
        var resumes = new AtomicInteger();
        try (var coordinator = new AiTurnCoordinator(worker, owner::add)) {
            var navigation = new AppNavigation(() -> {
                coordinator.cancel();
                selection.cancel();
            }, resumes::incrementAndGet);
            navigation.show(AppNavigation.Screen.GAME);
            var request = selection.request(game).orElseThrow();
            atf.gradle.audit.GradleInputs.fixture("Manual bid and queued advice before opening score and history",
                    "checkpoint", before, "legal bids", legal, "manual", manual, "advice", legal.get(0));
            assertTrue("The pending advice is accepted for evaluation", coordinator.submit(() -> legal.get(0),
                    bid -> selection.apply(game, request, bid), failure -> fail(failure.toString())));
            worker.runNext();
            selection.select(game, manual);
            var score = navigation.openDialog();
            var history = navigation.openDialog();
            owner.remove().run();
            assertFalse("Closing history keeps the score dialog active", navigation.dismiss(history));
            assertTrue("The remaining score dialog still blocks automation", navigation.blocksAutomation());
            assertEquals("History dismissal cannot resume the game", 0, resumes.get());
            assertEquals("The manual choice survives closing history", manual, selection.selected(game).orElseThrow());
            assertTrue("Closing the last dialog resumes the game", navigation.dismiss(score));
            assertFalse("A duplicate dismissal cannot resume twice", navigation.dismiss(history));
            assertEquals("Exactly one resume is emitted", 1, resumes.get());
            assertFalse("Stale advice cannot clear the manual choice", selection.failed(game, request));
            assertEquals("The manual choice is still selected", manual, selection.selected(game).orElseThrow());
            assertTrue("A selected manual bid suppresses new advice", selection.request(game).isEmpty());
            assertEquals("The checkpoint is unchanged until explicit confirmation", before, game.saveState());
        }
    }

    @Test public void aConfirmedManualContractSurvivesJsonAndCannotBeReplacedByAdvice() throws Exception {
        var game = fixture(); var selection = new HumanBidSelection();
        var request = selection.request(game).orElseThrow();
        var legal = BiddingMatrixFixtures.legalBids(game.snapshot());
        int manual = legal.get(legal.size() - 1);
        selection.select(game, manual);
        game.submitBid(0, selection.selected(game).orElseThrow());
        var committed = game.saveState(); var restored = roundTrip(game);
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "committed", committed, "game", game, "legal", legal, "manual", manual, "request", request, "restored", restored, "selection", selection);
        assertEquals("restored.saveState() must equal committed", committed, restored.saveState());
        assertEquals("restored.snapshot().players().get(0).bid() must equal Integer.valueOf(manual)", Integer.valueOf(manual), restored.snapshot().players().get(0).bid());
        assertFalse("selection.apply(restored, request, legal.get(0)) must remain false", selection.apply(restored, request, legal.get(0)));
        var afterRestart = new HumanBidSelection();
        assertTrue("afterRestart.selected(restored).isEmpty() must hold", afterRestart.selected(restored).isEmpty());
        assertTrue("afterRestart.request(restored).isEmpty() must hold", afterRestart.request(restored).isEmpty());
        assertThrows("The operation rejects its input with IllegalStateException.class", IllegalStateException.class, () -> afterRestart.select(restored, legal.get(0)));
        assertEquals("restored.saveState() must equal committed", committed, restored.saveState());
    }

    @Test public void anUnconfirmedManualChoiceIsNotSerializedAndCannotLeakIntoTheRestoredSession() throws Exception {
        var game = fixture(); var selection = new HumanBidSelection();
        int manual = BiddingMatrixFixtures.legalBids(game.snapshot()).get(0);
        selection.select(game, manual);
        var restored = roundTrip(game);
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game, "manual", manual, "restored", restored, "selection", selection);
        assertNull("restored.snapshot().players().get(0).bid() remains absent", restored.snapshot().players().get(0).bid());
        assertTrue("selection.selected(restored).isEmpty() must hold", selection.selected(restored).isEmpty());
        assertTrue("selection.request(restored).isPresent() must hold", selection.request(restored).isPresent());
        assertEquals("restored.saveState() must equal game.saveState()", game.saveState(), restored.saveState());
    }

    private GameSession fixture() {
        return BiddingMatrixFixtures.humanTurn(players, size, (players + size) % players);
    }

    private static GameSession roundTrip(GameSession game) throws Exception {
        return GameSessionStore.fromJson(GameSessionStore.encode(game.saveState()).toString());
    }
}
