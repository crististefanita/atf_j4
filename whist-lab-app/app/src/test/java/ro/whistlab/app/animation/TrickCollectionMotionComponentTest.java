package ro.whistlab.app.animation;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import org.junit.Test;

public class TrickCollectionMotionComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void collectionUsesTheEndOfTheExistingPauseAndAccountsForLayoutDelay() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 1000, "declared constant 2", 500, "declared constant 3", 0, "declared constant 4", 1500, "declared constant 5", 700, "declared constant 6", 300, "declared constant 7", 1200, "declared constant 8", .4f, "declared constant 9", .00001f, "declared constant 10", 1);
        assertEquals("TrickCollectionMotion.timing(1500, 0) must equal new TrickCollectionMotion.Timing(1000, 500, 0)", new TrickCollectionMotion.Timing(1000, 500, 0), TrickCollectionMotion.timing(1500, 0));
        assertEquals("TrickCollectionMotion.timing(1500, 300) must equal new TrickCollectionMotion.Timing(700, 500, 0)", new TrickCollectionMotion.Timing(700, 500, 0), TrickCollectionMotion.timing(1500, 300));
        var resumed = TrickCollectionMotion.timing(1500, 1200);
        assertEquals("resumed.delayMillis() must equal 0", 0, resumed.delayMillis());
        assertEquals("resumed.durationMillis() must equal 300", 300, resumed.durationMillis());
        assertEquals(.4f, resumed.startFraction(), .00001f);
        assertEquals("TrickCollectionMotion.timing(1500, 1500) must equal new TrickCollectionMotion.Timing(0, 0, 1)", new TrickCollectionMotion.Timing(0, 0, 1), TrickCollectionMotion.timing(1500, 1500));
        assertEquals("TrickCollectionMotion.timing(300, 1500) must equal new TrickCollectionMotion.Timing(0, 0, 1)", new TrickCollectionMotion.Timing(0, 0, 1), TrickCollectionMotion.timing(300, 1500));
    }

    @Test public void noLayoutDelayOrResumeCanExtendTheOriginalDeadline() {
        for (long remaining = 0; remaining <= 1500; remaining += 25)
            for (long delay = 0; delay <= 1800; delay += 37) {
                var timing = TrickCollectionMotion.timing(remaining, delay);

                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "delay", delay, "remaining", remaining, "timing", timing);
                assertEquals("timing.delayMillis() + timing.durationMillis() must equal Math.max(0, remaining - delay)", Math.max(0, remaining - delay), timing.delayMillis() + timing.durationMillis());
                assertTrue("timing.durationMillis() >= 0 && timing.durationMillis() <= 500 must hold", timing.durationMillis() >= 0 && timing.durationMillis() <= 500);
                assertTrue("timing.startFraction() >= 0 && timing.startFraction() <= 1 must hold", timing.startFraction() >= 0 && timing.startFraction() <= 1);
            }
    }

    @Test public void everyCardConvergesOnEveryWinningSeatAtThreeToSixPlaces() {
        for (int players = 3; players <= 6; players++) for (float density : new float[]{1, 1.5f, 3})
            for (int winner = 0; winner < players; winner++) for (int card = 0; card < players; card++) {
                var source = new TrickCollectionMotion.Point((80 + card * 45) * density, 350 * density);
                var target = new TrickCollectionMotion.Point((30 + winner * 50) * density,
                        (winner == 0 ? 600 : 80) * density);
                var first = TrickCollectionMotion.frame(source, target, 0);

                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "card", card, "density", density, "first", first, "players", players, "source", source, "target", target, "winner", winner);
                assertEquals("first must equal new TrickCollectionMotion.Frame(0, 0, 1, 1)", new TrickCollectionMotion.Frame(0, 0, 1, 1), first);
                var last = TrickCollectionMotion.frame(source, target, 1);
                assertEquals(target.x(), source.x() + last.translationX(), .001f);
                assertEquals(target.y(), source.y() + last.translationY(), .001f);
                assertEquals(.18f, last.scale(), .00001f);
                assertEquals(0, last.alpha(), 0);
            }
    }

    @Test public void motionNeverOvershootsAndFadesOnlyNearTheDestination() {
        var source = new TrickCollectionMotion.Point(200, 300);
        for (var target : new TrickCollectionMotion.Point[]{new TrickCollectionMotion.Point(10, 50),
                new TrickCollectionMotion.Point(400, 600), source}) {
            float previousDistance = Float.MAX_VALUE;
            float previousScale = 1;
            for (int step = 0; step <= 100; step++) {
                var frame = TrickCollectionMotion.frame(source, target, step / 100f);
                float x = source.x() + frame.translationX(), y = source.y() + frame.translationY();

                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "frame", frame, "previousDistance", previousDistance, "previousScale", previousScale, "source", source, "step", step, "target", target, "x", x, "y", y);
                assertTrue("x >= Math.min(source.x(), target.x()) && x <= Math.max(source.x(), target.x()) must hold", x >= Math.min(source.x(), target.x()) && x <= Math.max(source.x(), target.x()));
                assertTrue("y >= Math.min(source.y(), target.y()) && y <= Math.max(source.y(), target.y()) must hold", y >= Math.min(source.y(), target.y()) && y <= Math.max(source.y(), target.y()));
                float distance = (target.x() - x) * (target.x() - x) + (target.y() - y) * (target.y() - y);
                assertTrue("distance <= previousDistance must hold", distance <= previousDistance);
                assertTrue("frame.scale() <= previousScale must hold", frame.scale() <= previousScale);
                assertTrue("frame.scale() >= 0.17999F && frame.scale() <= 1 must hold", frame.scale() >= .17999f && frame.scale() <= 1);
                assertTrue("frame.alpha() >= 0 && frame.alpha() <= 1 must hold", frame.alpha() >= 0 && frame.alpha() <= 1);
                if (step < 80) assertEquals(1, frame.alpha(), 0);
                previousDistance = distance;
                previousScale = frame.scale();
            }
        }
    }

    @Test public void fractionsOutsideTheWindowClampToTheVisibleOrCollectedEndpoint() {
        var source = new TrickCollectionMotion.Point(-50, 300);
        var target = new TrickCollectionMotion.Point(400, -20);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "source", source, "target", target);
        assertEquals("TrickCollectionMotion.frame(source, target, -1) must equal TrickCollectionMotion.frame(source, target, 0)", TrickCollectionMotion.frame(source, target, 0), TrickCollectionMotion.frame(source, target, -1));
        assertEquals("TrickCollectionMotion.frame(source, target, 2) must equal TrickCollectionMotion.frame(source, target, 1)", TrickCollectionMotion.frame(source, target, 1), TrickCollectionMotion.frame(source, target, 2));
    }

    @Test public void invalidClocksAndCoordinatesFailBeforeReachingAndroidProperties() {
        for (long remaining : new long[]{-1, 1501}) {
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "remaining", remaining);
assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,
                () -> TrickCollectionMotion.timing(remaining, 0)); }
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> TrickCollectionMotion.timing(500, -1));
        for (float value : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> new TrickCollectionMotion.Point(value, 0));
            assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> new TrickCollectionMotion.Point(0, value));
            assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> TrickCollectionMotion.frame(
                    new TrickCollectionMotion.Point(0, 0), new TrickCollectionMotion.Point(1, 1), value));
        }
    }
}
