package ro.whistlab.app.lifecycle;

import static org.junit.Assert.*;

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

public class GameCommandFlowComponentTest {
    @Test public void acceptedBidIsCommittedBeforeBookkeepingSaveAndContinuation() {
        var game = deal();
        var events = new ArrayList<String>();
        var flow = new GameCommandFlow(() -> {
            assertEquals(Integer.valueOf(1), game.snapshot().players().get(0).bid());
            events.add("save");
        }, () -> events.add("advance"));

        assertTrue(flow.execute(() -> {
            game.submitBid(0, 1);
            events.add("bid");
        }, () -> events.add("accepted"), failure -> fail("Unexpected refusal: " + failure)));

        assertEquals(List.of("bid", "accepted", "save", "advance"), events);
        assertEquals(1, game.snapshot().bidsSubmitted());
    }

    @Test public void illegalBidDoesNotRunAnyAcceptedStageOrChangeTheSession() {
        var game = deal();
        var before = game.snapshot();
        var failures = new ArrayList<RuntimeException>();
        var flow = forbiddenAcceptedStages();
        assertFalse(flow.execute(() -> game.submitBid(0, 3), () -> fail("Bookkeeping"), failures::add));
        assertEquals(before, game.snapshot());
        assertEquals(1, failures.size());
        assertTrue(failures.get(0) instanceof IllegalArgumentException);
    }

    @Test public void phaseRefusalKeepsItsOriginalCauseAndNeverSaves() {
        var game = deal();
        var before = game.snapshot();
        var failures = new ArrayList<RuntimeException>();
        assertFalse(forbiddenAcceptedStages().execute(
                () -> game.playCard(0, new Card(Suit.HEARTS, 14)), () -> fail("Bookkeeping"), failures::add));
        assertEquals(before, game.snapshot());
        assertTrue(failures.get(0) instanceof IllegalStateException);
    }

    @Test public void acceptedCardReachesReplayAndCheckpointExactlyOnce() {
        var game = deal();
        for (int seat = 0; seat < 4; seat++) game.submitBid(seat, 0);
        assertEquals(GamePhase.PLAYING, game.snapshot().phase());
        var events = new ArrayList<String>();
        var flow = new GameCommandFlow(() -> {
            assertEquals(1, game.snapshot().history().size());
            events.add("save");
        }, () -> events.add("advance"));
        assertTrue(flow.execute(() -> game.playCard(0, new Card(Suit.HEARTS, 14)),
                () -> events.add("replay"), failure -> fail(failure.toString())));
        assertEquals(List.of("replay", "save", "advance"), events);
        assertEquals(1, game.snapshot().history().size());
    }

    @Test public void replayFailureAfterAValidCardIsNeverReportedAsAnIllegalCard() {
        var game = deal();
        for (int seat = 0; seat < 4; seat++) game.submitBid(seat, 0);
        var failure = new IllegalStateException("Replay unavailable");
        var thrown = assertThrows(IllegalStateException.class, () -> forbiddenAcceptedStages().execute(
                () -> game.playCard(0, new Card(Suit.HEARTS, 14)), () -> { throw failure; },
                refusal -> fail("A committed card cannot be refused")));
        assertSame(failure, thrown);
        assertEquals(1, game.snapshot().history().size());
    }

    @Test public void persistenceFailureKeepsTheAcceptedBidAndDoesNotRepeatOrAdvanceIt() {
        var game = deal();
        var mutations = new AtomicInteger();
        var failure = new IllegalArgumentException("Checkpoint defect");
        var flow = new GameCommandFlow(() -> { throw failure; }, () -> fail("Advance"));
        assertSame(failure, assertThrows(IllegalArgumentException.class, () -> flow.execute(() -> {
            mutations.incrementAndGet();
            game.submitBid(0, 0);
        }, () -> { }, refusal -> fail("A checkpoint defect is not a bid refusal"))));
        assertEquals(1, mutations.get());
        assertEquals(1, game.snapshot().bidsSubmitted());
    }

    @Test public void continuationFailureIsNotConvertedToARefusalOrAnExtraSave() {
        var saves = new AtomicInteger();
        var mutations = new AtomicInteger();
        var failure = new IllegalStateException("Scheduling defect");
        var flow = new GameCommandFlow(saves::incrementAndGet, () -> { throw failure; });
        assertSame(failure, assertThrows(IllegalStateException.class, () -> flow.execute(
                mutations::incrementAndGet, () -> { }, refusal -> fail("Unexpected refusal"))));
        assertEquals(1, mutations.get());
        assertEquals(1, saves.get());
    }

    @Test public void anUnexpectedCommandDefectIsNotTreatedAsADomainRefusal() {
        var failure = new NullPointerException("Programming defect");
        assertSame(failure, assertThrows(NullPointerException.class, () -> forbiddenAcceptedStages().execute(
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
