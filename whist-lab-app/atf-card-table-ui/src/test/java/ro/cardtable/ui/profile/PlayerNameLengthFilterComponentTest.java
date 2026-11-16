package ro.cardtable.ui.profile;

import android.text.Spanned;
import atf.gradle.audit.GradleAuditedTest;
import atf.gradle.audit.GradleInputs;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import static atf.gradle.audit.assertions.GradleAssertions.*;

public final class PlayerNameLengthFilterComponentTest extends GradleAuditedTest {
    @Test public void measuresOnlyTheProposedReplacementAndPreservesRejectedSelections() {
        var proposals = new ArrayList<String>();
        var filter = new PlayerNameLengthFilter(3, name -> {
            proposals.add(name);
            return name.codePointCount(0, name.length());
        });
        var field = mockField("abc");
        String letter = new String(Character.toChars(0x10400));
        String input = "?" + letter.repeat(2) + "!";
        GradleInputs.fixture("Android edit ranges", "destination", field.toString(),
                "source", input, "ranges in UTF-16", "source 1–3 or 1–5; destination 1–2");

        assertNull("One complete supplementary letter fits the selected position",
                filter.filter(input, 1, 3, field, 1, 2));
        var rejected = filter.filter(input, 1, 5, field, 1, 2);
        assertEquals("A rejected replacement puts the original text back", "b", rejected.toString());
        assertTrue("Returning the original subsequence carries its span type", rejected instanceof Spanned);
        assertEquals("The length strategy sees each full proposed field without the source sentinels",
                List.of("a" + letter + "c", "a" + letter.repeat(2) + "c"), proposals);
    }

    @Test public void emptyInputAlwaysAllowsDeletionEvenIfTheFieldIsStillOverBudget() {
        var field = mockField("abcd");
        var filter = new PlayerNameLengthFilter(2, name -> {
            throw new AssertionError("Deletion must skip length measurement");
        });
        GradleInputs.fixture("Deleting in an over-budget field", "destination", field.toString(),
                "selection", "1–2", "limit", 2);
        assertNull("Android can delete one selected character even before the name fits",
                filter.filter("", 0, 0, field, 1, 2));
    }

    private static Spanned mockField(String text) {
        return (Spanned) Proxy.newProxyInstance(Spanned.class.getClassLoader(), new Class<?>[]{Spanned.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "length" -> text.length();
                    case "charAt" -> text.charAt((int) arguments[0]);
                    case "subSequence" -> mockField(text.substring((int) arguments[0], (int) arguments[1]));
                    case "toString" -> text;
                    default -> throw new AssertionError(method);
                });
    }
}
