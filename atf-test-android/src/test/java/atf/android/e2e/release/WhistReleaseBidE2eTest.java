package atf.android.e2e.release;

import atf.android.e2e.activation.AndroidE2eTest;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.release.autogame.AutoGameReleaseScenario;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

@AndroidE2eTest
class WhistReleaseBidE2eTest {
    @Test void signedRecommendationManualChoiceAndSavedContractFollowTheRealRomanianFlow() throws Throwable {
        var context = new ReleaseBidContext(Path.of(System.getProperty("whist.records.runRoot")));
        context.execute(ReleaseBidScenario::run);
    }

    @Test void autoGamePlaysOnlyTheCurrentHandAndReturnsControlForTheNext() throws Throwable {
        var context = new ReleaseBidContext(Path.of(System.getProperty("whist.records.runRoot")),
                "signed-release-auto-game", "Signed release — AutoGame bounded to one hand", getClass().getSimpleName());
        context.execute(AutoGameReleaseScenario::run);
    }

    @Test void signedThreePlayerDownAndUpKeepsTheSeventeenCardHandAndSeatAnchorsVisible() throws Throwable {
        var context = new ReleaseBidContext(Path.of(System.getProperty("whist.records.runRoot")),
                "signed-release-three-player-down-up", "Signed release — three-player maximum hand geometry",
                getClass().getSimpleName());
        context.execute(ThreePlayerDownAndUpLayoutScenario::run);
    }

    /** Focused signed-app geometry flow, kept beside its owning release acceptance tests. */
    private static final class ThreePlayerDownAndUpLayoutScenario {
        private static void run(ReleaseBidContext c) throws Exception {
            String installed = c.installedHash();
            if (!c.setup.required("requestedApkSha256").asText().equals(installed))
                throw new IllegalStateException("Installed APK differs from the signed candidate");

            c.ui.restartToActivity(ReleaseBidContext.PACKAGE, ".MainActivity");
            boolean ro = c.ui.hasText(c.ui.dump(), "Joc nou");
            String newGame = ro ? "Joc nou" : "New game";
            String modeLabel = ro ? "Varianta de joc" : "Game type";
            String mode = ro ? "Coborâre și urcare" : "Down & Up";
            String players = ro ? "Jucători" : "Players";
            String level = ro ? "Nivelul adversarilor" : "AI level";
            String start = ro ? "Începe partida" : "Start game";
            String bid = ro ? "Licitează" : "Place bid";
            String bidLabel = ro ? "Licitat" : "Bid";
            String scoreLabel = ro ? "Scor" : "Score";
            String hand = ro ? "Cărțile tale, 17." : "Your hand, 17 cards.";

            c.ui.tapText(newGame);
            c.ui.selectNamedSpinnerOption(players, "3");
            c.ui.selectNamedSpinnerOption(modeLabel, mode);
            c.ui.selectNamedSpinnerOption(level, ro ? "Dificil" : "Hard");
            c.checkpoint("Three-player Down & Up configuration", xml -> List.of(
                    ReleaseBidContext.equal("Three players selected", "3", c.ui.spinnerValue(xml, players)),
                    ReleaseBidContext.equal("Maximum-hand mode selected", mode, c.ui.spinnerValue(xml, modeLabel)),
                    ReleaseBidContext.equal("Start action remains visible", true, c.ui.hasText(xml, start))));

            c.ui.tapText(start);
            c.awaitRecommendation(bid);
            List<AndroidUiDriver.Bounds> bidBounds = seatBounds(c.ui, c.ui.dump(), bidLabel, scoreLabel);
            c.checkpoint("All seventeen cards and three seats fit at the first bid", xml -> {
                var faces = c.ui.handCardFaceBounds(xml);
                var labels = c.ui.bottomRowCardLabels(xml);
                return List.of(
                        ReleaseBidContext.equal("Full maximum hand is described", true, c.ui.hasDescription(xml, hand)),
                        ReleaseBidContext.equal("Seventeen card faces are visible", 17, faces.size()),
                        ReleaseBidContext.equal("Seventeen exposed card labels are visible", 17, labels.size()),
                        ReleaseBidContext.equal("All three bid/score seats are present", 3,
                                seatBounds(c.ui, xml, bidLabel, scoreLabel).size()),
                        ReleaseBidContext.equal("Board content stays inside the viewport", true,
                                c.ui.allContentFitsViewport(xml)));
            });

            c.ui.tapText(bid);
            awaitHumanCard(c);
            c.checkpoint("Seat anchors stay fixed when bidding changes to play", xml -> List.of(
                    ReleaseBidContext.equal("Three-player seat rectangles are unchanged", bidBounds,
                            seatBounds(c.ui, xml, bidLabel, scoreLabel)),
                    ReleaseBidContext.equal("The human has a legal card action", true,
                            !c.ui.legalCardDescriptions(xml).isEmpty()),
                    ReleaseBidContext.equal("The table still fits the viewport", true,
                            c.ui.allContentFitsViewport(xml))));
        }

        private static List<AndroidUiDriver.Bounds> seatBounds(AndroidUiDriver ui, String xml, String bid, String score)
                throws Exception {
            var seats = new java.util.ArrayList<AndroidUiDriver.Bounds>();
            for (String value : ui.descriptions(xml)) {
                if (value.contains(". " + bid + " ") && value.contains(". " + score + " "))
                    seats.add(ui.boundsForDescription(xml, value));
            }
            seats.sort(Comparator.comparingInt(AndroidUiDriver.Bounds::top)
                    .thenComparingInt(AndroidUiDriver.Bounds::left));
            return List.copyOf(seats);
        }

        private static void awaitHumanCard(ReleaseBidContext c) throws Exception {
            long deadline = System.nanoTime() + Duration.ofSeconds(45).toNanos();
            do {
                if (!c.ui.legalCardDescriptions(c.ui.dump()).isEmpty()) return;
                Thread.sleep(120);
            } while (System.nanoTime() < deadline);
            throw new IllegalStateException("Down & Up did not reach the human's first card turn");
        }

        private ThreePlayerDownAndUpLayoutScenario() { }
    }
}
