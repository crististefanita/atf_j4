package ro.whistlab.app.seat;

import java.util.List;
import static atf.gradle.audit.assertions.GradleAssertions.*;
import org.junit.Test;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.core.rules.HowToPlay;

/** Inject wrapped glyph extents; actual Android font rendering keeps its native sanity check. */
public final class PlayerSeatMetricsComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void fullLocalizedActionsStayAboveNamesAcrossCommonResolutionsAndFontScales() {
        int[][] profiles = {{720, 1280, 360}, {1080, 1920, 440}, {1080, 2448, 440},
                {1200, 1920, 320}, {1440, 2560, 560}};
        List<BadgeLayoutCase> expected = new java.util.ArrayList<>(), observed = new java.util.ArrayList<>();
        for (int[] profile : profiles) for (int players = 3; players <= 6; players++) {
            float density = profile[2] / 160f;
            boolean compact = profile[1] / density < 760;
            int widthDp = Math.round(profile[0] / density) - 40;
            int seatWidthPx = Math.round(Math.min(116, Math.min(Math.round(widthDp * (compact ? 0.25f : 0.29f)),
                    (widthDp - 16) / 3)) * density);
            for (float fontScale : new float[]{1, 1.3f, 1.5f, 2}) {
                for (var language : HowToPlay.Language.values()) {
                    List<BadgePhaseCase> expectedPhases = new java.util.ArrayList<>(), observedPhases = new java.util.ArrayList<>();
                    int nameHeight = (int) Math.ceil(16 * fontScale * density);
                    int headingHeight = compact ? Math.round(20 * density) : nameHeight + Math.round(16 * density);
                    int naturalFrameHeight = (int) Math.ceil(42 * fontScale * density);
                    int referenceHeight = PlayerSeatMetrics.referenceFrameHeightPx(
                            naturalFrameHeight, nameHeight, headingHeight);
                    int gap = Math.round(3 * density);
                    for (boolean bidding : new boolean[]{true, false}) {
                        String action = new WhistUiText(language).seatTurnBadge(bidding);
                        int glyphExtent = (int) Math.ceil(action.length() * 7 * fontScale * density);
                        int lines = (glyphExtent + seatWidthPx - 1) / seatWidthPx;
                        int measuredBadgeHeight = lines * (int) Math.ceil(13 * fontScale * density);
                        var plan = PlayerSeatMetrics.arrange(referenceHeight, measuredBadgeHeight, gap);
                        int expectedFrameHeight = (int) Math.ceil(referenceHeight * 1.25);
                        var expectedPlan = new PlayerSeatMetrics.Plan(measuredBadgeHeight,
                                measuredBadgeHeight + gap, expectedFrameHeight,
                                measuredBadgeHeight + gap + expectedFrameHeight);
                        expectedPhases.add(new BadgePhaseCase(bidding, true, expectedPlan));
                        observedPhases.add(new BadgePhaseCase(bidding, !action.isBlank(), plan));
                    }
                    expected.add(new BadgeLayoutCase(profile[0], profile[1], profile[2], players,
                            fontScale, language, expectedPhases));
                    observed.add(new BadgeLayoutCase(profile[0], profile[1], profile[2], players,
                            fontScale, language, observedPhases));
                }
            }
        }
        atf.gradle.audit.GradleInputs.fixture("Localized action layout matrix", "profiles", profiles.length,
                "playerCounts", List.of(3, 4, 5, 6), "fontScales", List.of(1f, 1.3f, 1.5f, 2f),
                "languages", List.of(HowToPlay.Language.values()), "scenarios", observed.size());
        assertEquals("Every full phase label fits above its reference frame without clipping",
                expected, observed);
    }

    @Test public void multiLineActionExpandsItsOwnRegionAndCannotIntersectTheFrame() {
        var one = PlayerSeatMetrics.arrange(43, 13, 3);
        var three = PlayerSeatMetrics.arrange(43, 39, 3);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "one", one, "three", three);
        assertEquals("three.frameHeightPx() must equal one.frameHeightPx()", one.frameHeightPx(), three.frameHeightPx());
        assertEquals("three.frameTopPx() - one.frameTopPx() must equal 26", 26, three.frameTopPx() - one.frameTopPx());
        assertEquals("three.heightPx() - one.heightPx() must equal 26", 26, three.heightPx() - one.heightPx());
        assertTrue("three.badgeHeightPx() < three.frameTopPx() must hold", three.badgeHeightPx() < three.frameTopPx());
    }

    @Test public void repeatedMeasurementUsesFreshContentRatherThanThePreviousEnlargedFrame() {
        List<PlayerSeatMetrics.Plan> repeated = java.util.stream.IntStream.range(0, 10)
                .mapToObj(ignored -> PlayerSeatMetrics.arrange(48, 12, 3)).toList();
        var expected = new PlayerSeatMetrics.Plan(12, 15, 60, 75);
        atf.gradle.audit.GradleInputs.fixture("Repeated fresh measurements", "naturalHeights", List.of(48, 49),
                "measuredBadgeHeight", 12, "gap", 3, "repetitions", repeated.size());
        assertEquals("Repeated calls use fresh content and return the same complete plan",
                java.util.Collections.nCopies(10, expected), repeated);
        assertEquals("Natural height 48 rounds up to frame height 60", 60, PlayerSeatMetrics.frameHeightPx(48));
        assertEquals("Natural height 49 rounds up to frame height 62", 62, PlayerSeatMetrics.frameHeightPx(49));
    }

    @Test public void movingTheBadgeOutsideDoesNotShrinkTheReferenceFrameBeforeEnlargingIt() {
        int compact = PlayerSeatMetrics.referenceFrameHeightPx(43, 15, 20);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "compact", compact);
        assertEquals("compact must equal 48", 48, compact);
        assertEquals("PlayerSeatMetrics.arrange(compact, 12, 3).frameHeightPx() must equal 60", 60, PlayerSeatMetrics.arrange(compact, 12, 3).frameHeightPx());
        int roomy = PlayerSeatMetrics.referenceFrameHeightPx(53, 17, 17 + 16);
        assertEquals("roomy must equal 69", 69, roomy);
        assertEquals("PlayerSeatMetrics.arrange(roomy, 12, 3).frameHeightPx() must equal 87", 87, PlayerSeatMetrics.arrange(roomy, 12, 3).frameHeightPx());
        assertEquals("A larger name must never be squeezed into the minimum heading region",
                80, PlayerSeatMetrics.referenceFrameHeightPx(80, 28, 20));
    }

    @Test public void equalizedSeatSlotsKeepFramesAlignedForDifferentMeasuredContents() {
        int reserved = PlayerSeatMetrics.arrange(59, 26, 3).heightPx();
        var first = PlayerSeatMetrics.arrange(48, 26, 3, reserved);
        var second = PlayerSeatMetrics.arrange(59, 26, 3, reserved);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "first", first, "reserved", reserved, "second", second);
        assertEquals("second.heightPx() must equal first.heightPx()", first.heightPx(), second.heightPx());
        assertEquals("second.frameHeightPx() must equal first.frameHeightPx()", first.frameHeightPx(), second.frameHeightPx());
        assertEquals("second.frameTopPx() must equal first.frameTopPx()", first.frameTopPx(), second.frameTopPx());
    }

    @Test public void longestLocalizedPhaseBadgeKeepsSeatFrameStableAcrossProfiles() {
        int[][] profiles = {{720, 1280, 360}, {1080, 1920, 440}, {1080, 2448, 440},
                {1200, 1920, 320}, {1440, 2560, 560}};
        List<StableBadgeCase> expected = new java.util.ArrayList<>(), observed = new java.util.ArrayList<>();
        for (int[] profile : profiles) for (float fontScale : new float[]{1f, 1.3f, 1.5f, 2f})
            for (var language : HowToPlay.Language.values()) {
                float density = profile[2] / 160f;
                int tableWidthDp = Math.min(760, Math.round(profile[0] / density) - 40);
                int seatWidthPx = Math.round(ro.cardtable.ui.table.TableSizing.seatWidthDp(
                        tableWidthDp, profile[1] / density < 760 * Math.max(1f, fontScale)) * density);
                var copy = new WhistUiText(language);
                int biddingHeight = estimatedBadgeHeight(copy.seatTurnBadge(true), seatWidthPx,
                        density, fontScale);
                int playingHeight = estimatedBadgeHeight(copy.seatTurnBadge(false), seatWidthPx,
                        density, fontScale);
                int reservedHeight = PlayerSeatMetrics.stableBadgeHeightPx(biddingHeight, playingHeight);
                int naturalFrame = Math.round(43 * fontScale * density);
                int nameHeight = Math.round(16 * fontScale * density);
                boolean compact = profile[1] / density < 760 * Math.max(1f, fontScale);
                int headingHeight = compact ? Math.round(20 * density) : nameHeight + Math.round(16 * density);
                int reference = PlayerSeatMetrics.referenceFrameHeightPx(naturalFrame, nameHeight, headingHeight);
                int gap = Math.max(1, Math.round(3 * density));
                var bidding = PlayerSeatMetrics.arrange(reference, Math.max(biddingHeight, reservedHeight), gap);
                var playing = PlayerSeatMetrics.arrange(reference, Math.max(playingHeight, reservedHeight), gap);
                var expectedPlan = PlayerSeatMetrics.arrange(reference,
                        Math.max(biddingHeight, playingHeight), gap);
                expected.add(new StableBadgeCase(profile[0], profile[1], profile[2], fontScale, language,
                        Math.max(biddingHeight, playingHeight), expectedPlan, expectedPlan));
                observed.add(new StableBadgeCase(profile[0], profile[1], profile[2], fontScale, language,
                        reservedHeight, bidding, playing));
            }
        atf.gradle.audit.GradleInputs.fixture("Stable phase badge matrix", "profiles", profiles.length,
                "fontScales", List.of(1f, 1.3f, 1.5f, 2f), "languages",
                List.of(HowToPlay.Language.values()), "scenarios", observed.size());
        assertEquals("One measured slot keeps each seat frame identical between bidding and play",
                expected, observed);
    }

    private static int estimatedBadgeHeight(String text, int widthPx, float density, float fontScale) {
        int glyphWidth = (int) Math.ceil(text.length() * 7 * fontScale * density);
        int lines = Math.max(1, (glyphWidth + widthPx - 1) / widthPx);
        return lines * (int) Math.ceil(13 * fontScale * density);
    }

    private record BadgePhaseCase(boolean bidding, boolean labelPresent, PlayerSeatMetrics.Plan plan) { }
    private record BadgeLayoutCase(int widthPx, int heightPx, int densityDpi, int players,
            float fontScale, HowToPlay.Language language, List<BadgePhaseCase> phases) { }
    private record StableBadgeCase(int widthPx, int heightPx, int densityDpi, float fontScale,
            HowToPlay.Language language, int reservedHeight, PlayerSeatMetrics.Plan bidding,
            PlayerSeatMetrics.Plan playing) { }

    @Test public void invalidExtentsCannotSilentlyHideAnAction() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 0, "declared constant 2", 12, "declared constant 3", 3, "declared constant 4", 40);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> PlayerSeatMetrics.arrange(0, 12, 3));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> PlayerSeatMetrics.arrange(40, 0, 3));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> PlayerSeatMetrics.arrange(40, 12, 0));
    }
}
