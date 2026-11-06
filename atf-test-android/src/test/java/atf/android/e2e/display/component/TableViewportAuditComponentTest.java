package atf.android.e2e.display.component;

import atf.android.component.AndroidComponentAudit;
import org.junit.jupiter.api.extension.RegisterExtension;

import atf.android.whist.evidence.HandIndexAudit.Box;
import atf.android.whist.evidence.TableViewportAudit;
import atf.android.whist.evidence.TableViewportAudit.PlayedRegion;
import atf.android.whist.evidence.TableViewportAudit.Requirements;
import atf.android.whist.evidence.TableViewportAudit.Snapshot;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.testkit.AtfComponentTest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest
class TableViewportAuditComponentTest {
    @RegisterExtension static final AndroidComponentAudit AUDIT = AndroidComponentAudit.extension();
    @Test void completeTrickAndThirteenCardBoundaryAreAcceptedAsSeparateObservedStates() {
        for (int count : new int[]{1, 12, 13}) assertTrue(passes(baseline(count, count == 12), requirements(count, count == 12)));
    }

    @Test void missingOrClippedHumanFrameCannotPassJustBecauseOtherPlayersAreVisible() {
        var state = baseline(13, false);
        var seats = new ArrayList<>(state.seats());
        seats.remove(0);
        assertFalse(passes(copy(state, seats, state.handFaces(), state.playedCards(), false, true), requirements(13, false)));
        seats = new ArrayList<>(state.seats());
        seats.set(0, new Box(300, 865, 420, 880));
        assertFalse(passes(copy(state, seats, state.handFaces(), state.playedCards(), false, true), requirements(13, false)));
    }

    @Test void aClippedCardFaceIsRejectedEvenWhenThirteenIndicesExist() {
        var state = baseline(13, false);
        var faces = new ArrayList<>(state.handFaces());
        var original = faces.get(0);
        faces.set(0, new Box(original.left(), original.top(), original.right(), original.top() + 220));
        assertFalse(passes(copy(state, state.seats(), faces, state.playedCards(), false, true), requirements(13, false)));
    }

    @Test void aSingleCardShiftedInsideTheViewportStillFailsTheCenteringContract() {
        var state = baseline(1, false);
        var face = state.handFaces().get(0);
        var moved = List.of(new Box(face.left() + 24, face.top(), face.right() + 24, face.bottom()));
        assertFalse(passes(copy(state, state.seats(), moved, state.playedCards(), false, true), requirements(1, false)));
    }

    @Test void fourPresentTrickCardsDoNotHideAnIntersection() {
        var state = baseline(12, true);
        var cards = new ArrayList<>(state.playedCards());
        var first = cards.get(0);
        cards.set(3, new PlayedRegion("A♣", first.slot(), first.face()));
        assertFalse(passes(copy(state, state.seats(), state.handFaces(), cards, false, true), requirements(12, true)));
    }

    @Test void aTrickCardCannotCoverAPlayerFrame() {
        var state = baseline(12, true);
        var cards = new ArrayList<>(state.playedCards());
        cards.set(0, new PlayedRegion("K♥", new Box(60, 380, 140, 600), new Box(60, 382, 140, 594)));
        assertFalse(passes(copy(state, state.seats(), state.handFaces(), cards, false, true), requirements(12, true)));
    }

    @Test void theWindowBoundaryCannotClipAnOtherwiseCompleteHand() {
        var state = baseline(13, false);
        var clipped = new Snapshot(new Box(0, 0, 720, 1140), state.board(), state.hand(),
                state.seats(), state.handFaces(), state.playedCards(), false, true);
        assertFalse(passes(clipped, requirements(13, false)));
    }

    @Test void missingTrickCardsOrAHiddenHeaderAreNotImplicitPasses() {
        var state = baseline(12, true);
        assertFalse(passes(copy(state, state.seats(), state.handFaces(), List.of(), false, true), requirements(12, true)));
        assertFalse(passes(copy(state, state.seats(), state.handFaces(), state.playedCards(), false, false), requirements(12, true)));
    }

    @Test void aScrollableContainerCannotRescueAnOtherwiseCompleteViewport() {
        var state = baseline(13, false);
        assertFalse(passes(copy(state, state.seats(), state.handFaces(), List.of(), true, true), requirements(13, false)));
    }

    @Test void summariesRespectTheEffectivePreviewPolicyWithoutDroppingTheObservation() {
        int limit = AtfReportConfigurationLoader.load().reportingSettings().assertionMaxDisplayCharacters();
        var checks = TableViewportAudit.inspect(baseline(12, true), requirements(12, true));
        assertTrue(checks.stream().allMatch(check -> check.actual().length() <= limit && check.expected().length() <= limit));
        assertTrue(checks.size() >= 12);
    }

    private static boolean passes(Snapshot state, Requirements requirements) {
        return TableViewportAudit.inspect(state, requirements).stream().allMatch(check -> check.passed());
    }
    private static Requirements requirements(int count, boolean complete) { return new Requirements(4, count, complete ? 4 : 0, 221, 212, 99); }
    private static Snapshot copy(Snapshot state, List<Box> seats, List<Box> faces, List<PlayedRegion> cards, boolean scroll, boolean header) {
        return new Snapshot(state.viewport(), state.board(), state.hand(), seats, faces, cards, scroll, header);
    }
    private static Snapshot baseline(int count, boolean complete) {
        var seats = List.of(new Box(300, 780, 420, 880), new Box(60, 420, 180, 520),
                new Box(300, 120, 420, 220), new Box(540, 420, 660, 520));
        var faces = new ArrayList<Box>();
        int start = 40 + (640 - 100 - (count - 1) * 45) / 2;
        for (int card = 0; card < count; card++) faces.add(new Box(start + 45 * card, 970, start + 45 * card + 100, 1191));
        var cards = complete ? List.of(
                new PlayedRegion("K♥", new Box(300, 250, 380, 470), new Box(300, 252, 380, 464)),
                new PlayedRegion("Q♠", new Box(200, 380, 280, 600), new Box(200, 382, 280, 594)),
                new PlayedRegion("2♠", new Box(300, 510, 380, 730), new Box(300, 512, 380, 724)),
                new PlayedRegion("A♣", new Box(440, 380, 520, 600), new Box(440, 382, 520, 594))) : List.<PlayedRegion>of();
        return new Snapshot(new Box(0, 0, 720, 1280), new Box(40, 100, 680, 900),
                new Box(40, 920, 680, 1220), seats, faces, cards, false, true);
    }
}
