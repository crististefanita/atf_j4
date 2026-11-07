package atf.android.e2e.release;

import atf.android.e2e.activation.AndroidE2eTest;
import atf.android.e2e.driver.hierarchy.HierarchyDocument;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Shared menu and profile widgets retain Whist routes, localization and persistence. */
@AndroidE2eTest
class WhistReleaseHomeE2eTest {
    @Test void sharedHomePreservesLanguageProfileCancellationAndRestart() throws Throwable {
        var c = new ReleaseBidContext(Path.of(System.getProperty("whist.records.runRoot")), "signed-release-home-profile",
                "Signed release — shared home, profile and language", getClass().getSimpleName());
        c.execute(WhistReleaseHomeE2eTest::home);
    }

    private static void home(ReleaseBidContext c) throws Exception {
        String hash = c.installedHash();
        if (!hash.equals(c.setup.required("requestedApkSha256").asText())) throw new IllegalStateException("Different installed APK");
        c.ui.restartToActivity(ReleaseBidContext.PACKAGE, ".MainActivity");
        boolean ro = c.ui.hasText(c.ui.dump(), "Joc nou");
        c.ui.selectNamedSpinnerOption(ro ? "Limba" : "Language", "English");
        c.ui.awaitText("New game", Duration.ofSeconds(15));
        String original = profileButton(c.ui.dump(), "Your name: ");
        c.checkpoint("Shared home preserves the English control surface", xml -> List.of(
                ReleaseBidContext.equal("English language is selected", "English", c.ui.spinnerValue(xml, "Language")),
                ReleaseBidContext.equal("The configured profile remains visible", true, c.ui.hasText(xml, original)),
                ReleaseBidContext.equal("New game and Help remain accessible", true,
                        c.ui.hasText(xml, "New game") && c.ui.hasText(xml, "How to play"))));
        c.ui.tapText(original);
        c.ui.awaitText("SAVE", Duration.ofSeconds(10));
        c.profileText("Your name", "Audit");
        c.checkpoint("Shared profile editor retains the typed value", xml -> List.of(
                ReleaseBidContext.equal("The name field exposes the controlled value", true, c.ui.hasText(xml, "Audit")),
                ReleaseBidContext.equal("Save and Close remain available", true,
                        c.ui.hasText(xml, "SAVE") && c.ui.hasText(xml, "CLOSE"))));
        c.ui.tapText("CLOSE");
        c.ui.awaitText(original, Duration.ofSeconds(10));
        c.checkpoint("Cancelling profile editing preserves the saved name", xml -> List.of(
                ReleaseBidContext.equal("The original profile was not overwritten", true, c.ui.hasText(xml, original))));
        c.ui.tapText(original);
        c.ui.awaitText("SAVE", Duration.ofSeconds(10));
        c.profileText("Your name", "Audit");
        c.ui.tapText("SAVE");
        c.ui.awaitText("Your name: Audit", Duration.ofSeconds(10));
        c.ui.selectNamedSpinnerOption("Language", "Română");
        c.ui.awaitText("Joc nou", Duration.ofSeconds(10));
        c.checkpoint("Romanian localization retains the saved custom name", xml -> List.of(
                ReleaseBidContext.equal("Romanian language is selected", "Română", c.ui.spinnerValue(xml, "Limba")),
                ReleaseBidContext.equal("The custom name is not translated", true, c.ui.hasText(xml, "Numele tău: Audit")),
                ReleaseBidContext.equal("The shared menu fits the viewport", true, c.ui.allContentFitsViewport(xml))));
        c.ui.restartToActivity(ReleaseBidContext.PACKAGE, ".MainActivity");
        c.ui.awaitText("Numele tău: Audit", Duration.ofSeconds(15));
        c.checkpoint("Restart restores language and profile through the Whist adapter", xml -> List.of(
                ReleaseBidContext.equal("Romanian preference survives restart", true, c.ui.hasText(xml, "Joc nou")),
                ReleaseBidContext.equal("Profile preference survives restart", true, c.ui.hasText(xml, "Numele tău: Audit"))));
        c.ui.selectNamedSpinnerOption("Limba", "English");
        c.ui.awaitText("New game", Duration.ofSeconds(10));
    }

    private static String profileButton(String xml, String prefix) throws Exception {
        var buttons = HierarchyDocument.nodes(xml).stream().filter(node -> node.className().equals("android.widget.Button"))
                .map(HierarchyDocument.Node::text).filter(text -> text.startsWith(prefix)).toList();
        if (buttons.size() != 1) throw new IllegalStateException("Exactly one profile control is required");
        return buttons.getFirst();
    }
}
