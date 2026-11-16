package ro.whistlab.app.profile;

import android.text.Spanned;
import atf.gradle.audit.GradleAuditedTest;
import atf.gradle.audit.GradleInputs;
import java.lang.reflect.Proxy;
import org.junit.Test;
import ro.whistlab.core.player.PlayerNames;

import static atf.gradle.audit.assertions.GradleAssertions.*;

public final class PlayerNameInputComponentTest extends GradleAuditedTest {
    private static final String LETTER = new String(Character.toChars(0x10400));

    @Test public void typingAcceptsTenSupplementaryLettersAndBlocksTheEleventh() {
        var filter = PlayerSettingsDialog.nameLengthFilter();
        String draft = "";
        GradleInputs.fixture("Successive typed supplementary letters", "letter", LETTER,
                "initial field", draft, "attempts", 11, "rule limit", PlayerNames.MAX_LENGTH);
        for (int count = 1; count <= 10; count++) {
            assertNull("Typed letter " + count + " must be accepted as a whole",
                    filter.filter(LETTER, 0, LETTER.length(), mockField(draft), draft.length(), draft.length()));
            draft += LETTER;
        }

        assertEquals("An additional letter is refused without writing a surrogate",
                "", filter.filter(LETTER, 0, LETTER.length(), mockField(draft), draft.length(), draft.length()).toString());
        assertEquals("The ten-letter draft is accepted verbatim on save", draft, PlayerNames.normalize(draft));
        assertEquals("Ten non-BMP letters occupy twenty UTF-16 units", 20, draft.length());
    }

    @Test public void pasteAcceptsTenAndRefusesElevenWithoutDestroyingSelectedText() {
        var filter = PlayerSettingsDialog.nameLengthFilter();
        var selected = mockField("Ana");
        String ten = LETTER.repeat(10);
        String eleven = LETTER.repeat(11);
        GradleInputs.fixture("Paste replacing the entire prior name", "selected name", selected.toString(),
                "valid paste", ten, "invalid paste", eleven, "selection in UTF-16", "0–3");

        assertNull("Pasting ten complete letters fits", filter.filter(ten, 0, ten.length(), selected, 0, 3));
        assertEquals("An eleven-letter paste preserves the prior selection", "Ana",
                filter.filter(eleven, 0, eleven.length(), selected, 0, 3).toString());
        assertEquals("An eleven-letter paste into an empty field contributes no partial input", "",
                filter.filter(eleven, 0, eleven.length(), mockField(""), 0, 0).toString());
        assertThrows("Direct storage validation refuses the same eleven letters",
                IllegalArgumentException.class, () -> PlayerNames.normalize(eleven));
    }

    @Test public void inputCountsTheSameTrimmedNfcFormThatProfileSaveUses() {
        var filter = PlayerSettingsDialog.nameLengthFilter();
        String composedAfterSave = "Ș".repeat(10);
        String draft = "  " + "S\u0326".repeat(10) + "  ";
        var field = mockField("");
        GradleInputs.fixture("Raw draft and canonical form", "draft", draft, "after save", composedAfterSave,
                "destination", field.toString(), "limit", PlayerNames.MAX_LENGTH);

        assertNull("NFC and surrounding spaces are accounted for before refusing a draft",
                filter.filter(draft, 0, draft.length(), field, 0, 0));
        assertEquals("The saved canonical value uses the same ten letters", composedAfterSave, PlayerNames.normalize(draft));
        assertEquals("The measured canonical length matches the save boundary", 10, PlayerNames.normalizedLength(draft));
        String excessive = draft.stripTrailing() + "S\u0326";
        assertEquals("An eleventh decomposed letter is also rejected", "",
                filter.filter(excessive, 0, excessive.length(), field, 0, 0).toString());
    }

    private static Spanned mockField(String value) {
        return (Spanned) Proxy.newProxyInstance(Spanned.class.getClassLoader(), new Class<?>[]{Spanned.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "length" -> value.length();
                    case "charAt" -> value.charAt((int) arguments[0]);
                    case "subSequence" -> value.substring((int) arguments[0], (int) arguments[1]);
                    case "toString" -> value;
                    default -> throw new AssertionError(method);
                });
    }
}
