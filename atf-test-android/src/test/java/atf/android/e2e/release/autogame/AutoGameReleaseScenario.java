package atf.android.e2e.release.autogame;

import atf.android.e2e.release.ReleaseBidContext;
import atf.android.e2e.driver.AndroidUiDriver.Bounds;
import java.time.Duration;
import java.util.List;

/** Verifies the bid-screen AutoGame control and its one-hand boundary on the signed APK. */
public final class AutoGameReleaseScenario {
    public static void run(ReleaseBidContext c) throws Exception {
        String installed = c.installedHash();
        if (!c.setup.required("requestedApkSha256").asText().equals(installed))
            throw new IllegalStateException("Installed APK differs from the signed candidate");

        c.ui.restartToActivity(ReleaseBidContext.PACKAGE, ".MainActivity");
        boolean ro = c.ui.hasText(c.ui.dump(), "Joc nou");
        Labels labels = new Labels(ro);
        c.checkpoint("Signed APK home before AutoGame", xml -> List.of(
                ReleaseBidContext.equal("Installed APK matches the candidate SHA-256",
                        c.setup.required("requestedApkSha256").asText(), installed),
                ReleaseBidContext.equal("A new match can be started", true, c.ui.hasText(xml, labels.newGame()))));

        c.ui.tapText(labels.newGame());
        c.ui.selectNamedSpinnerOption(labels.players(), "4");
        c.ui.selectNamedSpinnerOption(labels.mode(), ro ? "Românesc (1–8–1)" : "Romanian (1–8–1)");
        c.ui.selectNamedSpinnerOption(labels.difficulty(), ro ? "Dificil" : "Hard");
        c.ui.tapText(labels.start());
        c.awaitRecommendation(labels.submit());
        c.checkpoint("Current hand is awaiting the human bid", xml -> List.of(
                ReleaseBidContext.equal("Real bid control is visible", true, c.ui.hasText(xml, labels.submit())),
                ReleaseBidContext.equal("AutoGame is in the upper right of the header, above trump and bidding", true,
                        headerPlacement(c, xml, labels)),
                ReleaseBidContext.equal("AutoGame starts unchecked with an explicit accessible action", true,
                        c.ui.hasDescription(xml, labels.startAutoGame()))));

        c.ui.tapText(labels.autoGameToggle());
        c.ui.awaitText(labels.next(), Duration.ofSeconds(45));
        c.checkpoint("AutoGame completes the current hand at its real score boundary", xml -> List.of(
                ReleaseBidContext.equal("One-card hand reached the score screen", true, c.ui.hasText(xml, labels.next())),
                ReleaseBidContext.equal("Next hand is offered only after the current hand completes", true,
                        c.ui.hasText(xml, labels.next()))));

        c.ui.tapText(labels.next());
        c.awaitRecommendation(labels.submit());
        c.checkpoint("AutoGame does not leak into the next hand", xml -> List.of(
                ReleaseBidContext.equal("The next hand exposes the same visible bid control", true,
                        c.ui.hasText(xml, labels.autoGameToggle())),
                ReleaseBidContext.equal("The new hand returns to the unchecked state", true,
                        c.ui.hasDescription(xml, labels.startAutoGame())
                                && !c.ui.hasDescription(xml, labels.stopAutoGame()))));
        c.ui.back();
    }

    private static boolean headerPlacement(ReleaseBidContext c, String xml, Labels labels) throws Exception {
        if (!c.ui.hasText(xml, labels.autoGameToggle()) || !c.ui.hasText(xml, labels.title())) return false;
        Bounds title = c.ui.boundsForText(xml, labels.title());
        Bounds toggle = c.ui.boundsForText(xml, labels.autoGameToggle());
        Bounds trump = c.ui.hasDescriptionContaining(xml, labels.trumpPrefix())
                ? c.ui.boundsForDescriptionContaining(xml, labels.trumpPrefix())
                : c.ui.boundsForDescription(xml, labels.noTrump());
        boolean sameLine = toggle.top() < title.bottom() && title.top() < toggle.bottom();
        boolean separateLine = toggle.top() >= title.bottom() && toggle.right() >= title.right();
        return (sameLine && toggle.left() >= title.right() || separateLine)
                && toggle.bottom() <= trump.top() && toggle.bottom() <= c.ui.boundsForText(xml, labels.submit()).top();
    }

    private record Labels(boolean ro) {
        String newGame() { return ro ? "Joc nou" : "New game"; }
        String players() { return ro ? "Jucători" : "Players"; }
        String mode() { return ro ? "Varianta de joc" : "Game type"; }
        String difficulty() { return ro ? "Nivelul adversarilor" : "AI level"; }
        String start() { return ro ? "Începe partida" : "Start game"; }
        String submit() { return ro ? "Licitează" : "Place bid"; }
        String next() { return ro ? "Mâna următoare" : "Next hand"; }
        String title() { return ro ? "Whist · Românesc" : "Whist · Romanian"; }
        String trumpPrefix() { return ro ? "Atu " : "Trump "; }
        String noTrump() { return ro ? "Fără atu" : "No trump"; }
        String autoGameToggle() { return ro ? "Automat" : "Auto play"; }
        String startAutoGame() { return ro ? "Joacă automat mâna curentă" : "Auto-play this hand"; }
        String stopAutoGame() { return ro ? "Oprește jocul automat" : "Stop auto-play"; }
    }

    private AutoGameReleaseScenario() { }
}
