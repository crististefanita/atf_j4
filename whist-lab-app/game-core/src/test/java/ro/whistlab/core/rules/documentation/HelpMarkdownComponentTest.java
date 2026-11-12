package ro.whistlab.core.rules.documentation;

import atf.gradle.audit.GradleAuditedTest;
import atf.gradle.audit.GradleInputs;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ro.whistlab.core.rules.HowToPlay;

import static atf.gradle.audit.assertions.GradleAssertions.assertEquals;

@RunWith(Parameterized.class)
public class HelpMarkdownComponentTest extends GradleAuditedTest {
    @Parameterized.Parameters(name = "language={0}")
    public static HowToPlay.Language[] languages() {
        return HowToPlay.Language.values();
    }

    private final HowToPlay.Language language;

    public HelpMarkdownComponentTest(HowToPlay.Language language) {
        this.language = language;
    }

    @Test public void thePublishedGuideMatchesEveryRuleInTheAppHelp() throws Exception {
        var path = guide();
        var published = Files.readAllLines(path);
        var canonical = HowToPlay.markdown(language).lines().toList();
        GradleInputs.fixture("Published guide and app help", "document", path.toString(),
                "language", language, "published lines", published);
        assertEquals("No rule, heading or example differs from the app help", List.of(),
                differences(canonical, published));
    }

    @Test public void independentChangesToTheTrumpObligationAndZeroAwardAreIdentified() throws Exception {
        var path = guide();
        var canonical = HowToPlay.markdown(language).lines().toList();
        var altered = new ArrayList<>(Files.readAllLines(path));
        String trump = language == HowToPlay.Language.RO ? "trebuie să pui atu" : "you must play trump";
        String optional = language == HowToPlay.Language.RO ? "poți arunca" : "you may discard";
        int trumpLine = change(altered, trump, optional);
        int awardLine = change(altered, "500", "50");
        GradleInputs.fixture("Two changes in the published rule text", "document", path.toString(),
                "language", language, "trump line", altered.get(trumpLine - 1), "award line", altered.get(awardLine - 1));
        var differences = differences(canonical, altered);
        assertEquals("Both altered lines are reported independently", List.of(trumpLine, awardLine),
                differences.stream().map(Difference::line).toList());
        assertEquals("The diagnostic preserves the changed trump and award claims",
                List.of(altered.get(trumpLine - 1), altered.get(awardLine - 1)),
                differences.stream().map(Difference::published).toList());
    }

    private Path guide() {
        return Path.of("..", "..", "docs", "game",
                language == HowToPlay.Language.RO ? "ATF_RulesRo.md" : "ATF_RulesEn.md");
    }

    private static int change(List<String> lines, String before, String after) {
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.contains(before)) {
                lines.set(index, line.replace(before, after));
                return index + 1;
            }
        }
        throw new AssertionError("The guide is missing the claim to mutate: " + before);
    }

    private static List<Difference> differences(List<String> canonical, List<String> published) {
        var differences = new ArrayList<Difference>();
        for (int index = 0; index < Math.max(canonical.size(), published.size()); index++) {
            String expected = index < canonical.size() ? canonical.get(index) : null;
            String actual = index < published.size() ? published.get(index) : null;
            if (!java.util.Objects.equals(expected, actual)) {
                differences.add(new Difference(index + 1, expected, actual));
            }
        }
        return differences;
    }

    private record Difference(int line, String appHelp, String published) { }
}
