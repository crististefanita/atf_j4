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

    @Test void signedAchievementsCatalogueOpensWithOrderedLocalProgress() throws Throwable {
        var c = new ReleaseBidContext(Path.of(System.getProperty("whist.records.runRoot")),
                "signed-release-achievements", "Signed release — achievements and local progress", getClass().getSimpleName());
        c.execute(WhistReleaseHomeE2eTest::achievements);
    }

    @Test void completedHandHistoryResumesWithRomanianAndEnglishHeadings() throws Throwable {
        var c = new ReleaseBidContext(Path.of(System.getProperty("whist.records.runRoot")),
                "history-release-sanity", "Signed release — completed hand history in both languages", getClass().getSimpleName());
        c.execute(WhistReleaseHomeE2eTest::completedHandHistory);
    }

    private static void completedHandHistory(ReleaseBidContext c) throws Exception {
        String installed = c.installedHash();
        if (!c.setup.required("requestedApkSha256").asText().equals(installed))
            throw new IllegalStateException("Installed APK differs from the signed candidate");

        c.ui.restartToActivity(ReleaseBidContext.PACKAGE, ".MainActivity");
        c.ui.selectNamedSpinnerOption("Language", "English");
        c.ui.awaitText("New game", Duration.ofSeconds(15));
        c.ui.tapText("New game");
        c.ui.selectNamedSpinnerOption("Players", "4");
        c.ui.selectNamedSpinnerOption("Game type", "Romanian (1–8–1)");
        c.ui.selectNamedSpinnerOption("AI level", "Hard");
        c.ui.tapText("Start game");
        c.awaitRecommendation("Place bid");
        c.ui.tapText("Auto play");
        c.awaitTextAndDismissAchievementNotice("Hand complete", Duration.ofSeconds(45));
        c.ui.tapText("Score history");
        c.ui.awaitText("CLOSE", Duration.ofSeconds(25));
        c.checkpoint("Completed one-card hand exposes English score history", xml -> List.of(
                ReleaseBidContext.equal("History dialog is visible", true, c.ui.hasText(xml, "Score history")),
                ReleaseBidContext.equal("Card-count heading is visible", true, c.ui.hasText(xml, "Cards")),
                ReleaseBidContext.equal("Bid heading is visible", true, c.ui.hasText(xml, "B")),
                ReleaseBidContext.equal("Total-points heading is visible", true, c.ui.hasText(xml, "P")),
                ReleaseBidContext.equal("A complete first-hand row is present", true, c.ui.hasText(xml, "1"))));
        c.ui.tapText("CLOSE");
        c.ui.tapText("Main menu");
        c.ui.awaitText("New game", Duration.ofSeconds(15));
        c.ui.selectNamedSpinnerOption("Language", "Română");
        c.ui.awaitText("Joc nou", Duration.ofSeconds(15));
        c.ui.tapText("Reia partida");
        c.ui.awaitText("Mână încheiată", Duration.ofSeconds(15));
        c.ui.tapText("Istoricul scorului");
        c.ui.awaitText("ÎNCHIDE", Duration.ofSeconds(25));
        c.checkpoint("The same completed hand resumes with Romanian score history", xml -> List.of(
                ReleaseBidContext.equal("History dialog is localized", true, c.ui.hasText(xml, "Istoricul scorului")),
                ReleaseBidContext.equal("Card-count heading is localized", true, c.ui.hasText(xml, "Cărți")),
                ReleaseBidContext.equal("Romanian bid heading is visible", true, c.ui.hasText(xml, "L")),
                ReleaseBidContext.equal("Total-points heading is visible", true, c.ui.hasText(xml, "P")),
                ReleaseBidContext.equal("Close action is localized", true, c.ui.hasText(xml, "ÎNCHIDE"))));
    }

    private static void achievements(ReleaseBidContext c) throws Exception {
        String installed = c.installedHash();
        if (!c.setup.required("requestedApkSha256").asText().equals(installed))
            throw new IllegalStateException("Installed APK differs from the signed candidate");

        c.ui.restartToActivity(ReleaseBidContext.PACKAGE, ".MainActivity");
        boolean ro = c.ui.hasText(c.ui.dump(), "Joc nou");
        String achievements = ro ? "Realizări" : "Achievements";
        String close = ro ? "ÎNCHIDE" : "CLOSE";
        String back = ro ? "Înapoi" : "Back";
        String newGame = ro ? "Joc nou" : "New game";

        String firstGoal = ro ? "Prima partidă" : "First match";

        c.ui.tapText(achievements);
        c.ui.awaitText(back, Duration.ofSeconds(15));
        c.checkpoint("Achievement catalogue opens on the installed APK", xml -> List.of(
                ReleaseBidContext.equal("Installed APK matches the candidate SHA-256",
                        c.setup.required("requestedApkSha256").asText(), installed),
                ReleaseBidContext.equal("Catalogue title is visible", true, c.ui.hasText(xml, achievements)),
                ReleaseBidContext.equal("The first goal opens from the visible list", true, c.ui.hasText(xml, firstGoal)),
                ReleaseBidContext.equal("The catalogue has a visible return action", true, c.ui.hasText(xml, back))));

        c.ui.tapText(firstGoal);
        c.ui.awaitText(close, Duration.ofSeconds(15));
        String condition = ro ? "Încheie o partidă validă." : "Finish one valid match.";
        c.checkpoint("The first goal exposes its own eligibility condition", xml -> List.of(
                ReleaseBidContext.equal("The selected goal opens its detail", true, c.ui.hasText(xml, firstGoal)),
                ReleaseBidContext.equal("Eligibility is explained in the active language", true,
                        hasVisibleTextContaining(xml, condition))));
        c.ui.tapText(close);
        c.ui.awaitText(back, Duration.ofSeconds(15));
        c.ui.tapText(back);
        c.checkpoint("Achievement catalogue returns to the home screen", xml -> List.of(
                ReleaseBidContext.equal("Home remains ready for a new match", true, c.ui.hasText(xml, newGame))));
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

    private static boolean hasVisibleTextContaining(String xml, String fragment) throws Exception {
        return HierarchyDocument.nodes(xml).stream().anyMatch(node -> node.text().contains(fragment));
    }
}
