package ro.cardtable.ui.hand;

import java.util.List;
import org.junit.Test;
import ro.cardtable.ui.cards.CardFace;

import atf.gradle.audit.GradleAuditedTest;
import atf.gradle.audit.GradleInputs;

import static atf.gradle.audit.assertions.GradleAssertions.assertEquals;
import static atf.gradle.audit.assertions.GradleAssertions.assertFalse;
import static atf.gradle.audit.assertions.GradleAssertions.assertTrue;

public final class HandInputTest extends GradleAuditedTest {
    private static HandCard card(String id, boolean legal) {
        return new HandCard(id, new CardFace("J", "♣", false), id, legal);
    }

    @Test public void illegalTapSuggestsEveryLegalCardAndNeverPlays() {
        var first = card("first", true);
        var second = card("second", true);
        var wrong = card("wrong", false);
        var other = card("other", false);
        GradleInputs.fixture("Mixed legal and unavailable cards", "legal", List.of("first", "second"),
                "attempt", "wrong");
        var input = new HandInput(List.of(first, second, wrong, other), true);
        assertFalse("input.suggests(first)", input.suggests(first));
        assertEquals("HandInput.Action.HINT, input.tap(wrong)", HandInput.Action.HINT, input.tap(wrong));
        assertTrue("input.rejects(wrong)", input.rejects(wrong));
        assertFalse("input.rejects(other)", input.rejects(other));
        assertTrue("input.suggests(first)", input.suggests(first));
        assertTrue("input.suggests(second)", input.suggests(second));
        assertFalse("input.suggests(other)", input.suggests(other));
        assertEquals("HandInput.Action.PLAY, input.tap(second)", HandInput.Action.PLAY, input.tap(second));
        assertFalse("input.suggests(first)", input.suggests(first));
        assertEquals("HandInput.Action.IGNORE, input.tap(first)", HandInput.Action.IGNORE, input.tap(first));
        assertEquals("HandInput.Action.IGNORE, input.tap(wrong)", HandInput.Action.IGNORE, input.tap(wrong));
    }

    @Test public void onlyTheCurrentHintExpiresAndRebuildStartsClean() {
        var legal = card("legal", true);
        var wrong = card("wrong", false);
        var other = card("other", false);
        GradleInputs.fixture("Consecutive invalid taps", "legal", "legal", "attempts", List.of("wrong", "other"));
        var cards = List.of(legal, wrong, other);
        var input = new HandInput(cards, true);
        input.tap(wrong);
        long old = input.revision();
        input.tap(other);
        assertFalse("input.clearHint(old)", input.clearHint(old));
        assertTrue("input.rejects(other)", input.rejects(other));
        assertTrue("input.clearHint(input.revision())", input.clearHint(input.revision()));
        assertFalse("input.rejects(other)", input.rejects(other));
        assertFalse("input.suggests(legal)", input.suggests(legal));
        assertFalse("new HandInput(cards, true).rejects(other)", new HandInput(cards, true).rejects(other));
    }

    @Test public void disabledHintAndForcedPassIgnoreUnavailableCards() {
        var legal = card("legal", true);
        var wrong = card("wrong", false);
        assertEquals("A disabled hint ignores the illegal card", HandInput.Action.IGNORE, new HandInput(List.of(legal, wrong), false).tap(wrong));
        GradleInputs.fixture("Forced pass", "visible card", "wrong", "legal choices", 0);
        var pass = new HandInput(List.of(wrong), true);
        assertFalse("pass.acceptsTap(wrong)", pass.acceptsTap(wrong));
        assertEquals("HandInput.Action.IGNORE, pass.tap(wrong)", HandInput.Action.IGNORE, pass.tap(wrong));
        assertEquals("HandInput.Action.IGNORE, pass.tap(legal)", HandInput.Action.IGNORE, pass.tap(legal));
    }
}
