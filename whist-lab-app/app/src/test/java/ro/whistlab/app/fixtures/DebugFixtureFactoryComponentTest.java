package ro.whistlab.app.fixtures;

import static atf.gradle.audit.assertions.GradleAssertions.assertEquals;
import static atf.gradle.audit.assertions.GradleAssertions.assertFalse;
import static atf.gradle.audit.assertions.GradleAssertions.assertTrue;

import atf.gradle.audit.GradleInputs;
import java.io.File;
import java.util.ArrayList;
import org.junit.Test;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.session.GamePhase;

/** Verifies retained startup scenarios independently of Android activity orchestration. */
public class DebugFixtureFactoryComponentTest extends atf.gradle.audit.GradleAuditedTest {
    private static final File UNUSED_DIRECTORY = new File("unused-debug-fixture-directory");

    @Test public void launchFixturesPreserveTheirDistinctInitialStates() throws Exception {
        var oneTrick = create(DebugFixtureFactory.Scenario.ONE_TRICK, false);
        var pause = create(DebugFixtureFactory.Scenario.TRICK_PAUSE, false);
        var playedCard = create(DebugFixtureFactory.Scenario.PLAYED_CARD, false);
        var hardBid = create(DebugFixtureFactory.Scenario.HARD_AI_BID, false);
        var handFan = create(DebugFixtureFactory.Scenario.HAND_FAN, false);
        var nineBids = create(DebugFixtureFactory.Scenario.NINE_BIDS, false);
        var compact = create(DebugFixtureFactory.Scenario.COMPACT_TABLE, false);
        GradleInputs.fixture("Fixture startup plans before assertions",
                "oneTrick", oneTrick, "pause", pause, "playedCard", playedCard,
                "hardBid", hardBid, "handFan", handFan, "nineBids", nineBids, "compact", compact);

        assertEquals("one-trick mode", GameMode.REVERSE_OH_HELL, oneTrick.game().snapshot().mode());
        assertEquals("one-trick initial phase", GamePhase.BIDDING, oneTrick.game().snapshot().phase());
        assertFalse("one-trick persistence is opt-in", oneTrick.persistBeforeDrive());

        assertEquals("pause phase before automated play", GamePhase.BIDDING, pause.game().snapshot().phase());
        assertTrue("pause fixture persists before automation", pause.persistBeforeDrive());

        assertEquals("played-card state has one visible card", 1, playedCard.game().snapshot().currentTrick().size());
        assertEquals("played-card fixture leads with hearts", Suit.HEARTS,
                playedCard.game().snapshot().currentTrick().get(0).card().suit());

        assertEquals("hard bid fixture keeps a hard game", AiDifficulty.HARD,
                hardBid.game().config().aiDifficulty());

        assertEquals("hand fan enters play", GamePhase.PLAYING, handFan.game().snapshot().phase());
        assertEquals("hand fan retains thirteen cards", 13, handFan.game().snapshot().players().get(0).hand().size());

        assertEquals("nine-bid fixture remains in bidding", GamePhase.BIDDING, nineBids.game().snapshot().phase());
        assertEquals("nine-bid fixture uses eight cards", 8, nineBids.game().snapshot().handSize());

        assertTrue("compact fixture renders its captured trick immediately", compact.renderImmediately());
        assertEquals("compact fixture exposes four captured cards", 4, compact.renderedCards().size());
        assertFalse("compact fixture does not persist before render", compact.persistBeforeDrive());
    }

    @Test public void onlyRequestedOneTrickFixtureIsPersisted() throws Exception {
        var transientFixture = create(DebugFixtureFactory.Scenario.ONE_TRICK, false);
        var persistedFixture = create(DebugFixtureFactory.Scenario.ONE_TRICK, true);
        var pauseFixture = create(DebugFixtureFactory.Scenario.TRICK_PAUSE, false);
        GradleInputs.fixture("Fixture persistence policy before assertions",
                "transientFixture", transientFixture, "persistedFixture", persistedFixture, "pauseFixture", pauseFixture);

        assertFalse("one-trick default remains transient", transientFixture.persistBeforeDrive());
        assertTrue("explicit one-trick persistence is retained", persistedFixture.persistBeforeDrive());
        assertTrue("trick-pause persistence is independent", pauseFixture.persistBeforeDrive());
    }

    @Test public void launchPlanDefensivelyCopiesCapturedCards() throws Exception {
        var compact = create(DebugFixtureFactory.Scenario.COMPACT_TABLE, false);
        var mutableCards = new ArrayList<>(compact.renderedCards());
        var plan = new DebugLaunch(compact.game(), null, mutableCards, true, false);
        GradleInputs.fixture("Launch plan before defensive-copy assertions",
                "mutableCards", mutableCards, "plan", plan);
        mutableCards.clear();

        assertEquals("plan retains its captured cards after caller mutation", 4, plan.renderedCards().size());
    }

    private static DebugLaunch create(DebugFixtureFactory.Scenario scenario, boolean persist) throws Exception {
        return DebugFixtureFactory.create(scenario, persist, UNUSED_DIRECTORY);
    }
}