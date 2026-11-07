package atf.android.e2e.release;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import java.util.List;
import org.junit.jupiter.api.Test;

@AtfComponentTest
class BidChoiceObservationComponentTest {
    @Test void pendingRecommendationKeepsManualChoicesAvailableWithoutInventingZero() throws Exception {
        var state = BidChoiceObservation.read(xml(false, true, false, false, "Place bid"), "Place bid");
        AuditedAssertions.assertEquals(List.of(0, 1), state.choices(), "Observed legal choices remain editable");
        AuditedAssertions.assertTrue(state.humanTurn(), "The human can make a manual choice while calculation is pending");
        AuditedAssertions.assertTrue(state.selected().isEmpty(), "Pending recommendation has no artificial default zero");
        AuditedAssertions.assertFalse(state.submitEnabled(), "An absent selection cannot be confirmed");
    }
    @Test void oneSelectedLegalValueIsReadInBothLanguages() throws Exception {
        for (String label : List.of("Place bid", "Licitează")) {
            var state = BidChoiceObservation.read(xml(true, true, false, true, label), label);
            AuditedAssertions.assertEquals(1, state.selected().orElseThrow(), "The selected value comes from the Android state");
            AuditedAssertions.assertTrue(state.submitEnabled(), "A human selection enables explicit confirmation");
        }
    }
    @Test void anOpponentsDisabledChoiceIsNotReportedAsAHumanRecommendation() throws Exception {
        var state = BidChoiceObservation.read(xml(false, false, true, false, "Place bid"), "Place bid");
        AuditedAssertions.assertFalse(state.humanTurn(), "Disabled numeric choices belong to the opponent presentation");
        AuditedAssertions.assertFalse(state.submitEnabled(), "The human cannot submit an opponent's bid");
    }
    @Test void duplicateSelectedChoicesAndContradictoryConfirmationAreRejected() {
        for (String invalid : List.of(xml(true, true, true, true, "Place bid"),
                xml(true, true, false, false, "Place bid"), xml(false, true, false, true, "Place bid"))) {
            AuditedAssertions.assertThrows(IllegalStateException.class, () -> BidChoiceObservation.read(invalid, "Place bid"));
        }
    }
    @Test void aForbiddenValueIsNotInventedWhenOnlyOneChoiceExists() throws Exception {
        var input = "<hierarchy>" + button("1", true, true) + button("Place bid", true, false) + "</hierarchy>";
        var state = BidChoiceObservation.read(input, "Place bid");
        AuditedAssertions.assertEquals(List.of(1), state.choices(), "Only values actually present in the legal selector are offered");
        AuditedAssertions.assertEquals(1, state.selected().orElseThrow());
    }
    @Test void nonButtonsCannotImpersonateASelectionAndMixedTurnControlsAreRejected() throws Exception {
        String valid = xml(true, true, false, true, "Place bid").replace("</hierarchy>",
                "<node text=\"0\" class=\"android.widget.TextView\" selected=\"true\"/></hierarchy>");
        AuditedAssertions.assertEquals(1, BidChoiceObservation.read(valid, "Place bid").selected().orElseThrow());
        String mixed = "<hierarchy>" + button("0", false, false) + button("1", true, true)
                + button("Place bid", true, false) + "</hierarchy>";
        AuditedAssertions.assertThrows(IllegalStateException.class, () -> BidChoiceObservation.read(mixed, "Place bid"));
    }
    @Test void duplicateConfirmationAndOutOfRangeBidsAreRejectedInsteadOfReadingAnotherScreen() {
        for (String invalid : List.of(xml(true, true, false, true, "Place bid").replace("</hierarchy>",
                button("Place bid", true, false) + "</hierarchy>"),
                "<hierarchy>" + button("9", true, true) + button("Place bid", true, false) + "</hierarchy>",
                "<hierarchy>" + button("-1", true, true) + button("Place bid", true, false) + "</hierarchy>")) {
            AuditedAssertions.assertThrows(IllegalStateException.class, () -> BidChoiceObservation.read(invalid, "Place bid"));
        }
    }
    private static String xml(boolean submit, boolean enabled, boolean zero, boolean one, String label) {
        return "<hierarchy>" + button("0", enabled, zero) + button("1", enabled, one)
                + button(label, submit, false) + "</hierarchy>";
    }
    private static String button(String text, boolean enabled, boolean selected) {
        return "<node text=\"" + text + "\" class=\"android.widget.Button\" bounds=\"[0,0][100,50]\""
                + " enabled=\"" + enabled + "\" selected=\"" + selected + "\"/>";
    }
}
