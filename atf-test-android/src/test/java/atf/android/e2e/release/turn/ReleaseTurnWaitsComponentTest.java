package atf.android.e2e.release.turn;

import atf.android.AdbClient;
import atf.android.component.AndroidComponentAudit;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest
class ReleaseTurnWaitsComponentTest {
    @RegisterExtension static final AndroidComponentAudit AUDIT = AndroidComponentAudit.extension();

    private static final String IDLE = xml("<node text='AI waiting' content-desc='AI waiting'/>");
    private static final String CARD = "Ace of hearts. Legal card";
    private static final String SCORE = xml("<node text='Hand complete'/>");

    @Test void recommendationReturnsTheMatchingObservationWithoutSendingAnInputCommand(@TempDir Path output, ComponentInputs inputs) throws Exception {
        String computer = bidScreen(false, false);
        String ready = bidScreen(true, true);
        var fixture = new Fixture(output, inputs, IDLE, computer, ready);

        assertEquals(ready, fixture.waits.awaitRecommendation("Place bid", 1));
        assertEquals(List.of("clear", "dump", "pull", "sleep:120", "clear", "dump", "pull", "sleep:120",
                "clear", "dump", "pull"), fixture.events);
    }

    @Test void timeoutKeepsTheLastObservationAndStopsAtTheDeadlineAcrossClockWrap(@TempDir Path output, ComponentInputs inputs) throws Exception {
        String last = xml("<node content-desc='Still waiting for AI'/>");
        var fixture = new Fixture(output, inputs, IDLE, last, legalCard(10));
        fixture.clock.set(Long.MAX_VALUE - 50_000_000);
        inputs.add("Monotonic clock start (ns)", Long.toString(fixture.clock.get()));
        inputs.add("Wait budget", "240 ms");

        var failure = assertThrows(IllegalStateException.class,
                () -> fixture.waits.awaitHumanCard(Duration.ofMillis(240)));

        assertEquals("The signed app did not reach the human's card turn within PT0.24S; descriptions=[Still waiting for AI]",
                failure.getMessage());
        assertEquals(List.of("clear", "dump", "pull", "sleep:120", "clear", "dump", "pull", "sleep:120"), fixture.events);
        assertEquals(1, fixture.frames.size());
    }

    @Test void playingAndDismissingANoticeOnlyActOnFreshHierarchiesInOrder(@TempDir Path output, ComponentInputs inputs) throws Exception {
        String notice = xml("<node text='Achievement unlocked'/><node text='CLOSE' bounds='[100,100][140,140]'/>");
        var fixture = new Fixture(output, inputs, IDLE, legalCard(10), legalCard(50), notice, SCORE);

        assertEquals(SCORE, fixture.waits.finishOneCardHand("Hand complete", fixture::dismissNotice));

        assertEquals(List.of("clear", "dump", "pull", "sleep:120", "clear", "dump", "pull", "clear", "dump", "pull",
                "tap:60,30", "sleep:500", "clear", "dump", "pull", "tap:120,120", "sleep:500", "clear", "dump", "pull"), fixture.events);
    }

    @Test void awaitingScoreRenewsTheBudgetOnceAfterDismissingANotice(@TempDir Path output, ComponentInputs inputs) throws Exception {
        String notice = xml("<node text='Achievement unlocked'/><node text='CLOSE' bounds='[100,100][140,140]'/>");
        var fixture = new Fixture(output, inputs, IDLE, notice, notice, SCORE);

        assertEquals(SCORE, fixture.waits.awaitScore("Hand complete", Duration.ofMillis(240), fixture::dismissNotice));

        assertEquals(List.of("clear", "dump", "pull", "sleep:120", "clear", "dump", "pull", "tap:120,120", "sleep:500",
                "clear", "dump", "pull", "sleep:120", "clear", "dump", "pull"), fixture.events);
    }

    @Test void aFailedTapPropagatesTheOriginalTransportExceptionAndStopsPolling(@TempDir Path output, ComponentInputs inputs) throws Exception {
        var fixture = new Fixture(output, inputs, legalCard(10), legalCard(50), SCORE);
        var refused = new IOException("ADB transport broke during the card tap");
        fixture.tapFailure = refused;
        inputs.add("Injected transport failure", refused.toString());

        assertSame(refused, assertThrows(IOException.class,
                () -> fixture.waits.finishOneCardHand("Hand complete", xml -> false)));

        assertEquals(List.of("clear", "dump", "pull", "clear", "dump", "pull", "tap:60,30"), fixture.events);
        assertEquals(1, fixture.frames.size());
    }

    @Test void anInvalidBidObservationFailsImmediatelyAndKeepsItsDiagnostic(@TempDir Path output, ComponentInputs inputs) throws Exception {
        String invalid = xml("<node class='android.widget.Button' text='Place bid' enabled='true'/>");
        var fixture = new Fixture(output, inputs, invalid, bidScreen(true, true));

        var failure = assertThrows(IllegalStateException.class, () -> fixture.waits.awaitRecommendation("Place bid", 1));

        assertEquals("No bid choices in the current hierarchy", failure.getMessage());
        assertEquals(List.of("clear", "dump", "pull"), fixture.events);
    }

    private static String xml(String nodes) { return "<hierarchy>" + nodes + "</hierarchy>"; }

    private static String legalCard(int left) {
        return xml("<node clickable='true' content-desc='" + CARD + "' bounds='[" + left + ",20]["
                + (left + 20) + ",40]'/>");
    }

    private static String bidScreen(boolean human, boolean selected) {
        return xml("""
                <node class='android.widget.Button' text='Place bid' enabled='%s'/>
                <node class='android.widget.Button' text='0' enabled='%s' selected='%s'/>
                <node class='android.widget.Button' text='1' enabled='%s'/>
                """.formatted(selected, human, selected, human));
    }

    private static final class Fixture implements AndroidUiDriver.Device {
        private final AtomicLong clock = new AtomicLong();
        private final ArrayDeque<String> frames;
        private final List<String> events = new ArrayList<>();
        private final AndroidUiDriver driver;
        private final ReleaseTurnWaits waits;
        private IOException tapFailure;

        Fixture(Path output, ComponentInputs inputs, String... observations) throws Exception {
            for (int i = 0; i < observations.length; i++) {
                inputs.add("Mock hierarchy " + (i + 1), observations[i]);
            }
            frames = new ArrayDeque<>(List.of(observations));
            driver = new AndroidUiDriver(this, output, clock::get, millis -> {
                events.add("sleep:" + millis);
                clock.addAndGet(millis * 1_000_000);
            });
            waits = new ReleaseTurnWaits(driver);
        }

        boolean dismissNotice(String observed) throws Exception {
            if (!driver.hasText(observed, "Achievement unlocked")) return false;
            driver.tapTextIn(observed, "CLOSE");
            return true;
        }

        @Override public AdbClient.Result run(String... arguments) throws IOException {
            String message = "";
            switch (arguments[0].equals("pull") ? "pull" : arguments[1]) {
                case "rm" -> events.add("clear");
                case "uiautomator" -> {
                    events.add("dump");
                    message = "UI hierarchy dumped to: controlled.xml";
                }
                case "pull" -> {
                    events.add("pull");
                    Files.writeString(Path.of(arguments[2]), frames.removeFirst());
                }
                case "input" -> {
                    if (!arguments[2].equals("tap")) throw new AssertionError(List.of(arguments));
                    events.add("tap:" + arguments[3] + "," + arguments[4]);
                    if (tapFailure != null) throw tapFailure;
                }
                default -> throw new AssertionError(List.of(arguments));
            }
            return new AdbClient.Result(0, message, List.of(arguments));
        }

        @Override public AdbClient.BinaryResult runBinary(String... arguments) {
            throw new AssertionError("The polling loop does not capture PNGs");
        }
    }
}
