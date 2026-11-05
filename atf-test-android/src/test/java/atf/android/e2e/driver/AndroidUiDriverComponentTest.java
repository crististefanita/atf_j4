package atf.android.e2e.driver;

import atf.testkit.AtfComponentTest;

import atf.android.AdbClient;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class AndroidUiDriverComponentTest {
    @Test
    void symmetricSeriesSelectorRetainsItsStableIdentityAndLegacyAlias() {
        assertTrue(UiHierarchy.matchesSpinnerLabel("romanian-streak-value", "Five-hand reward / penalty"));
        assertTrue(UiHierarchy.matchesSpinnerLabel("romanian-streak-value", "Bonus / penalizare după cinci mâini"));
        assertTrue(UiHierarchy.matchesSpinnerLabel("romanian-streak-value", "Five-hand exact-contract bonus"));
        assertFalse(UiHierarchy.matchesSpinnerLabel("romanian-streak-value", "Game type"));
    }
    @Test
    void clippedScrollViewDoesNotPassAsACompleteFirstViewport() throws Exception {
        String hierarchy = """
                <hierarchy>
                  <node class="android.widget.ScrollView" scrollable="true" bounds="[0,0][360,640]">
                    <node class="android.widget.TextView" text="Clipped content" bounds="[0,620][360,640]" />
                  </node>
                </hierarchy>
                """;

        assertFalse(UiHierarchy.allContentFitsViewport(hierarchy));
    }

    @Test
    void completeNonScrollableContentPassesTheFirstViewportContract() throws Exception {
        String hierarchy = """
                <hierarchy>
                  <node class="android.widget.ScrollView" scrollable="false" bounds="[0,0][360,640]">
                    <node class="android.widget.TextView" text="Visible content" bounds="[0,100][360,140]" />
                  </node>
                </hierarchy>
                """;

        assertTrue(UiHierarchy.allContentFitsViewport(hierarchy));
    }

    @Test
    void enabledNumericBidsAreResolvedAsButtonsDespiteRepeatedSeatLabels() throws Exception {
        String hierarchy = """
                <hierarchy>
                  <node text="0" class="android.widget.TextView" enabled="true" />
                  <node text="0" class="android.widget.Button" enabled="true" />
                  <node text="1" class="android.widget.Button" enabled="false" />
                  <node text="2" class="android.widget.Button" enabled="true" />
                </hierarchy>
                """;

        assertEquals(List.of(0, 2), AndroidUiDriver.enabledNumericButtonValues(hierarchy));
    }

    @Test
    void screenshotPointScalesLogicalBoundsToPhysicalCapturePixels() {
        BufferedImage physicalCapture = new BufferedImage(1080, 1920, BufferedImage.TYPE_INT_RGB);

        assertEquals(95, AndroidUiDriver.screenshotPoint(
                physicalCapture, 63, 768.5, 720, 1280)[0]);
        assertEquals(1153, AndroidUiDriver.screenshotPoint(
                physicalCapture, 63, 768.5, 720, 1280)[1]);
    }

    @Nested
    class Waiting {
        @ParameterizedTest
        @ValueSource(longs = {0, -1})
        void aMatchingFirstObservationDoesNotSleepEvenAtAnExpiredDeadline(long timeoutMillis,
                @TempDir Path output) throws Exception {
            String ready = xml("text=\"Ready\"");
            var fixture = new WaitingFixture(output, ready);
            assertEquals(ready, fixture.driver.awaitText("Ready", Duration.ofMillis(timeoutMillis)));
            assertEquals(1, fixture.observations);
            assertEquals(List.of(), fixture.pauses);
        }

        @Test
        void pollingReturnsTheFreshMatchingHierarchy(@TempDir Path output) throws Exception {
            String ready = xml("text=\"Ready\" content-desc=\"third observation\"");
            var fixture = new WaitingFixture(output, xml("text=\"Loading\""),
                    xml("text=\"Still loading\""), ready);
            assertEquals(ready, fixture.driver.awaitText("Ready", Duration.ofSeconds(2)));
            assertEquals(3, fixture.observations);
            assertEquals(List.of(300L, 300L), fixture.pauses);
        }

        @Test
        void timeoutUsesTheLastObservationWithoutReadingTheStateAtTheDeadline(@TempDir Path output)
                throws Exception {
            var fixture = new WaitingFixture(output, xml("content-desc=\"First\""),
                    xml("content-desc=\"Second\""), xml("content-desc=\"Target\""));
            var failure = assertThrows(IllegalStateException.class,
                    () -> fixture.driver.awaitDescriptionContaining("Target", Duration.ofMillis(500)));
            assertEquals("UI did not expose description containing 'Target' within PT0.5S; descriptions=[Second]",
                    failure.getMessage());
            assertEquals(2, fixture.observations);
            assertEquals(List.of(250L, 250L), fixture.pauses);
        }

        @Test
        void zeroTimeoutStillObservesOneNonmatchingStateAndUsesItsOriginalPoll(@TempDir Path output)
                throws Exception {
            var fixture = new WaitingFixture(output, xml("text=\"Loading\""));
            var failure = assertThrows(IllegalStateException.class,
                    () -> fixture.driver.awaitText("Ready", Duration.ZERO));
            assertEquals("UI did not show text 'Ready' within PT0S", failure.getMessage());
            assertEquals(1, fixture.observations);
            assertEquals(List.of(300L), fixture.pauses);
        }

        @Test
        void spinnerMustSettleAfterItsPopupDisappears(@TempDir Path output) throws Exception {
            String settled = xml("text=\"START\"", "text=\"6\"");
            var fixture = new WaitingFixture(output,
                    xml("text=\"START\"", "text=\"6\" class=\"android.widget.CheckedTextView\""),
                    xml("text=\"START\""), settled);
            assertEquals(settled, fixture.driver.awaitSpinnerSelection("6", Duration.ofSeconds(2)));
            assertEquals(3, fixture.observations);
            assertEquals(List.of(250L, 250L), fixture.pauses);
        }

        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        void absenceAndRawHierarchySearchRetainDifferentConditions(boolean raw, @TempDir Path output)
                throws Exception {
            String ready = raw ? xml("text=\"Ready\" content-desc=\"marker\"")
                    : xml("text=\"Ready\" content-desc=\"Pending\"");
            var fixture = new WaitingFixture(output, xml("text=\"Pending\""), ready);
            String observed = raw
                    ? fixture.driver.awaitContains("content-desc=\"marker\"", Duration.ofSeconds(2))
                    : fixture.driver.awaitAbsent("Pending", Duration.ofSeconds(2));
            assertEquals(ready, observed);
            assertEquals(2, fixture.observations);
            assertEquals(List.of(300L), fixture.pauses);
        }

        @Test
        void interruptedPollingDoesNotRetryOrReplaceTheCause(@TempDir Path output) throws Exception {
            var fixture = new WaitingFixture(output, xml("text=\"Loading\""));
            var interrupted = new InterruptedException("controlled polling interruption");
            fixture.pauseFailure = interrupted;
            assertSame(interrupted, assertThrows(InterruptedException.class,
                    () -> fixture.driver.awaitText("Ready", Duration.ofSeconds(2))));
            assertEquals(1, fixture.observations);
            assertEquals(List.of(300L), fixture.pauses);
        }
    }

    private static String xml(String... attributes) {
        var result = new StringBuilder("<hierarchy>");
        for (String value : attributes) result.append("<node ").append(value).append("/>");
        return result.append("</hierarchy>").toString();
    }

    private static final class WaitingFixture {
        private final AtomicLong ticks = new AtomicLong();
        private final List<Long> pauses = new ArrayList<>();
        private final AndroidUiDriver driver;
        private int observations;
        private InterruptedException pauseFailure;

        private WaitingFixture(Path output, String... states) throws Exception {
            var device = new AndroidUiDriver.Device() {
                public AdbClient.Result run(String... arguments) throws java.io.IOException {
                    String result = "";
                    if (arguments[0].equals("pull")) {
                        if (observations >= states.length)
                            throw new AssertionError("Unexpected hierarchy after the prescribed observations");
                        Files.writeString(Path.of(arguments[2]), states[observations++]);
                    } else if (arguments[1].equals("uiautomator")) {
                        result = "UI hierarchy dumped to: controlled.xml";
                    } else if (!arguments[1].equals("rm")) {
                        throw new AssertionError("Unexpected waiting command: " + List.of(arguments));
                    }
                    return new AdbClient.Result(0, result, List.of(arguments));
                }
                public AdbClient.BinaryResult runBinary(String... arguments) {
                    throw new AssertionError("Waiting must not capture a screenshot");
                }
            };
            driver = new AndroidUiDriver(device, output, ticks::get, millis -> {
                pauses.add(millis);
                ticks.addAndGet(millis * 1_000_000L);
                if (pauseFailure != null) throw pauseFailure;
            });
        }
    }
}
