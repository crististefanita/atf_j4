package ro.whistlab.app.lifecycle;

import static atf.gradle.audit.assertions.GradleAssertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

public class GameCommandFlowComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void acceptedBidIsCommittedBeforeBookkeepingSaveAndContinuation() {
        var game = deal();
        var events = new ArrayList<String>();
        var flow = new GameCommandFlow(() -> {

            assertEquals("game.snapshot().players().get(0).bid() must equal Integer.valueOf(1)", Integer.valueOf(1), game.snapshot().players().get(0).bid());
            events.add("save");
        }, () -> events.add("advance"));

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "events", events, "flow", flow, "game", game);
        assertTrue("flow.execute(()->{ game.submitBid(0, 1); events.add(\"bid\"); }, ()->events.add(\"accepted\"), (failure)->fail(\"Unexpected refusal: \" + failure)) must hold", flow.execute(() -> {
            game.submitBid(0, 1);
            events.add("bid");
        }, () -> events.add("accepted"), failure -> fail("Unexpected refusal: " + failure)));

        assertEquals("events must equal List.of(\"bid\", \"accepted\", \"save\", \"advance\")", List.of("bid", "accepted", "save", "advance"), events);
        assertEquals("game.snapshot().bidsSubmitted() must equal 1", 1, game.snapshot().bidsSubmitted());
    }

    @Test public void illegalBidDoesNotRunAnyAcceptedStageOrChangeTheSession() {
        var game = deal();
        var before = game.snapshot();
        var failures = new ArrayList<RuntimeException>();
        var flow = forbiddenAcceptedStages();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "before", before, "failures", failures, "flow", flow, "game", game);
        assertFalse("flow.execute(()->game.submitBid(0, 3), ()->fail(\"Bookkeeping\"), failures::add) must remain false", flow.execute(() -> game.submitBid(0, 3), () -> fail("Bookkeeping"), failures::add));
        assertEquals("game.snapshot() must equal before", before, game.snapshot());
        assertEquals("failures.size() must equal 1", 1, failures.size());
        assertTrue("failures.get(0) instanceof IllegalArgumentException must hold", failures.get(0) instanceof IllegalArgumentException);
    }

    @Test public void phaseRefusalKeepsItsOriginalCauseAndNeverSaves() {
        var game = deal();
        var before = game.snapshot();
        var failures = new ArrayList<RuntimeException>();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "before", before, "failures", failures, "game", game);
        assertFalse("forbiddenAcceptedStages().execute(()->game.playCard(0, new Card(Suit.HEARTS, 14)), ()->fail(\"Bookkeeping\"), failures::add) must remain false", forbiddenAcceptedStages().execute(
                () -> game.playCard(0, new Card(Suit.HEARTS, 14)), () -> fail("Bookkeeping"), failures::add));
        assertEquals("game.snapshot() must equal before", before, game.snapshot());
        assertTrue("failures.get(0) instanceof IllegalStateException must hold", failures.get(0) instanceof IllegalStateException);
    }

    @Test public void acceptedCardReachesReplayAndCheckpointExactlyOnce() {
        var game = deal();
        for (int seat = 0; seat < 4; seat++) game.submitBid(seat, 0);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game);
        assertEquals("game.snapshot().phase() must equal GamePhase.PLAYING", GamePhase.PLAYING, game.snapshot().phase());
        var events = new ArrayList<String>();
        var flow = new GameCommandFlow(() -> {
            assertEquals("game.snapshot().history().size() must equal 1", 1, game.snapshot().history().size());
            events.add("save");
        }, () -> events.add("advance"));
        assertTrue("flow.execute(()->game.playCard(0, new Card(Suit.HEARTS, 14)), ()->events.add(\"replay\"), (failure)->fail(failure.toString())) must hold", flow.execute(() -> game.playCard(0, new Card(Suit.HEARTS, 14)),
                () -> events.add("replay"), failure -> fail(failure.toString())));
        assertEquals("events must equal List.of(\"replay\", \"save\", \"advance\")", List.of("replay", "save", "advance"), events);
        assertEquals("game.snapshot().history().size() must equal 1", 1, game.snapshot().history().size());
    }

    @Test public void replayFailureAfterAValidCardIsNeverReportedAsAnIllegalCard() {
        var game = deal();
        for (int seat = 0; seat < 4; seat++) game.submitBid(seat, 0);
        var failure = new IllegalStateException("Replay unavailable");
        var thrown = assertThrows("The operation rejects its input with IllegalStateException.class", IllegalStateException.class, () -> forbiddenAcceptedStages().execute(
                () -> game.playCard(0, new Card(Suit.HEARTS, 14)), () -> { throw failure; },
                refusal -> fail("A committed card cannot be refused")));

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "failure", failure, "game", game, "thrown", thrown);
        assertSame("thrown keeps the identity of failure", failure, thrown);
        assertEquals("game.snapshot().history().size() must equal 1", 1, game.snapshot().history().size());
    }

    @Test public void persistenceFailureKeepsTheAcceptedBidAndDoesNotRepeatOrAdvanceIt() {
        var game = deal();
        var mutations = new AtomicInteger();
        var failure = new IllegalArgumentException("Checkpoint defect");
        var flow = new GameCommandFlow(() -> { throw failure; }, () -> fail("Advance"));

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "failure", failure, "flow", flow, "game", game, "mutations", mutations);
        assertSame("the declared component operation keeps the identity of failure", failure, assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> flow.execute(() -> {
            mutations.incrementAndGet();
            game.submitBid(0, 0);
        }, () -> { }, refusal -> fail("A checkpoint defect is not a bid refusal"))));
        assertEquals("mutations.get() must equal 1", 1, mutations.get());
        assertEquals("game.snapshot().bidsSubmitted() must equal 1", 1, game.snapshot().bidsSubmitted());
    }

    @Test public void continuationFailureIsNotConvertedToARefusalOrAnExtraSave() {
        var saves = new AtomicInteger();
        var mutations = new AtomicInteger();
        var failure = new IllegalStateException("Scheduling defect");
        var flow = new GameCommandFlow(saves::incrementAndGet, () -> { throw failure; });

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "failure", failure, "flow", flow, "mutations", mutations, "saves", saves);
        assertSame("assertThrows(IllegalStateException.class, ()->flow.execute(mutations::incrementAndGet, ()->{ }, (refusal)->fail(\"Unexpected refusal\"))) keeps the identity of failure", failure, assertThrows("The operation rejects its input with IllegalStateException.class", IllegalStateException.class, () -> flow.execute(
                mutations::incrementAndGet, () -> { }, refusal -> fail("Unexpected refusal"))));
        assertEquals("mutations.get() must equal 1", 1, mutations.get());
        assertEquals("saves.get() must equal 1", 1, saves.get());
    }

    @Test public void anUnexpectedCommandDefectIsNotTreatedAsADomainRefusal() {
        var failure = new NullPointerException("Programming defect");

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "failure", failure);
        assertSame("assertThrows(NullPointerException.class, ()->forbiddenAcceptedStages().execute(()->{ throw failure; }, ()->fail(\"Accepted\"), (refusal)->fail(\"Unexpected refusal\"))) keeps the identity of failure", failure, assertThrows("The operation rejects its input with NullPointerException.class", NullPointerException.class, () -> forbiddenAcceptedStages().execute(
                () -> { throw failure; }, () -> fail("Accepted"), refusal -> fail("Unexpected refusal"))));
    }

    private static GameCommandFlow forbiddenAcceptedStages() {
        return new GameCommandFlow(() -> fail("Save"), () -> fail("Advance"));
    }

    private static GameSession deal() {
        return GameSession.fromDeal(new GameConfig(4, GameMode.OH_HELL, AiDifficulty.NORMAL, 31),
                List.of(List.of(new Card(Suit.HEARTS, 14), new Card(Suit.CLUBS, 2)),
                        List.of(new Card(Suit.HEARTS, 13), new Card(Suit.CLUBS, 3)),
                        List.of(new Card(Suit.HEARTS, 12), new Card(Suit.CLUBS, 4)),
                        List.of(new Card(Suit.HEARTS, 11), new Card(Suit.CLUBS, 5))), Trump.SPADES, 3);
    }
}
