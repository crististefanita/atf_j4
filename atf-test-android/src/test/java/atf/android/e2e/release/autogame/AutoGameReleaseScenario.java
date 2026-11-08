package atf.android.e2e.release.autogame;

import atf.android.e2e.release.ReleaseBidContext;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/** Verifies the menu-driven AutoGame boundary on the signed APK using a real one-card hand. */
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
                ReleaseBidContext.equal("Game menu is accessible", true, c.ui.hasDescription(xml, labels.gameMenu()))));

        c.ui.tapDescription(labels.gameMenu());
        c.ui.tapText(labels.autoGame());
        c.ui.awaitText(labels.next(), Duration.ofSeconds(45));
        c.checkpoint("AutoGame completes the current hand at its real score boundary", xml -> List.of(
                ReleaseBidContext.equal("One-card hand reached the score screen", true, c.ui.hasText(xml, labels.next())),
                ReleaseBidContext.equal("Next hand is offered only after the current hand completes", true,
                        c.ui.hasText(xml, labels.next()))));

        c.ui.tapText(labels.next());
        c.awaitRecommendation(labels.submit());
        c.ui.tapDescription(labels.gameMenu());
        c.checkpoint("AutoGame does not leak into the next hand", xml -> List.of(
                ReleaseBidContext.equal("Start AutoGame is available again", true, c.ui.hasText(xml, labels.autoGame())),
                ReleaseBidContext.equal("Stop AutoGame is absent after the score boundary", false,
                        c.ui.hasText(xml, labels.stopAutoGame()))));
        c.ui.back();
    }

    private record Labels(boolean ro) {
        String newGame() { return ro ? "Joc nou" : "New game"; }
        String players() { return ro ? "Jucători" : "Players"; }
        String mode() { return ro ? "Varianta de joc" : "Game type"; }
        String difficulty() { return ro ? "Nivelul adversarilor" : "AI level"; }
        String start() { return ro ? "Începe partida" : "Start game"; }
        String submit() { return ro ? "Licitează" : "Place bid"; }
        String next() { return ro ? "Mâna următoare" : "Next hand"; }
        String gameMenu() { return ro ? "Meniu joc" : "Game menu"; }
        String autoGame() { return ro ? "Joacă automat mâna curentă" : "Auto-play this hand"; }
        String stopAutoGame() { return ro ? "Oprește jocul automat" : "Stop auto-play"; }
    }

    private AutoGameReleaseScenario() { }
}
