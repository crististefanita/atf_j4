package atf.android.e2e.release;

import atf.android.e2e.activation.AndroidE2eTest;
import atf.android.e2e.display.profile.ResponsiveDisplayProfile;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.driver.hierarchy.HierarchyDocument;
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

    @Test void signedReleaseKeepsTheMaximumHandVisibleAcrossPortraitProfiles() throws Throwable {
        var context = new ReleaseBidContext(Path.of(System.getProperty("whist.records.runRoot")),
                "responsive-ui-sanity", "Signed release — maximum hand across portrait profiles", getClass().getSimpleName());
        context.execute(c -> {
            var profiles = ResponsiveDisplayProfile.parse(System.getProperty("atf.android.sanity.profiles"));
            c.withPortraitProfiles(profiles, (profile, active) -> ThreePlayerDownAndUpLayoutScenario.run(active, profile.id() + " "));
        });
    }

    /** Focused signed-app geometry flow, kept beside its owning release acceptance tests. */
    private static final class ThreePlayerDownAndUpLayoutScenario {
        private static final int MAXIMUM_HAND_SIZE = 17;
        private static final int MAX_SWIPES_PER_DIRECTION = 3;

        private static void run(ReleaseBidContext c) throws Exception {
            run(c, "");
        }

        private static void run(ReleaseBidContext c, String label) throws Exception {
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
            String biddingHint = ro ? "Vizibilă la licitare" : "Visible while bidding";
            String hand = ro ? "Cărțile tale, 17." : "Your hand, 17 cards.";

            c.ui.tapText(newGame);
            c.ui.selectNamedSpinnerOption(players, "3");
            c.ui.selectNamedSpinnerOption(modeLabel, mode);
            c.ui.selectNamedSpinnerOption(level, ro ? "Dificil" : "Hard");
            c.checkpoint(label + "Three-player Down and Up configuration", xml -> List.of(
                    ReleaseBidContext.equal("Three players selected", "3", c.ui.spinnerValue(xml, players)),
                    ReleaseBidContext.equal("Maximum-hand mode selected", mode, c.ui.spinnerValue(xml, modeLabel)),
                    ReleaseBidContext.equal("Start action remains visible", true, c.ui.hasText(xml, start))));

            c.ui.tapText(start);
            c.awaitRecommendation(bid, 17);
            List<AndroidUiDriver.Bounds> bidBounds = seatBounds(c.ui, c.ui.dump(), bidLabel, scoreLabel);
            var seen = new java.util.LinkedHashSet<String>();
            var atStart = c.ui.dump();
            c.swipeHand(c.ui.boundsForDescription(atStart, hand), false);
            c.checkpoint(label + "First cards and all three seats are visible", xml -> {
                var visible = visibleBidCards(xml, biddingHint);
                seen.addAll(visible);
                var bounds = c.ui.boundsForDescription(xml, hand);
                return List.of(
                        ReleaseBidContext.equal("At least one card is exposed", true, !visible.isEmpty()),
                        ReleaseBidContext.equal("All three bid and score seats are present", 3,
                                seatBounds(c.ui, xml, bidLabel, scoreLabel).size()),
                        ReleaseBidContext.equal("Player hand has a positive viewport", true,
                                bounds.right() > bounds.left() && bounds.bottom() > bounds.top()));
            });

            if (seen.size() < MAXIMUM_HAND_SIZE) {
                collectAcrossHand(c, hand, biddingHint, true, seen);
                if (seen.size() < MAXIMUM_HAND_SIZE) collectAcrossHand(c, hand, biddingHint, false, seen);
                c.checkpoint(label + "Horizontal scroll exposes the complete seventeen-card hand", xml -> {
                    seen.addAll(visibleBidCards(xml, biddingHint));
                    return List.of(ReleaseBidContext.equal("All seventeen card identities can be viewed",
                                    MAXIMUM_HAND_SIZE, seen.size()),
                            ReleaseBidContext.equal("Scrolling the hand keeps the seat rectangles fixed", bidBounds,
                                    seatBounds(c.ui, xml, bidLabel, scoreLabel)));
                });
                c.swipeHand(c.ui.boundsForDescription(c.ui.dump(), hand), false);
            } else {
                c.checkpoint(label + "All seventeen cards are visible together", xml -> List.of(
                        ReleaseBidContext.equal("The full hand remains exposed", MAXIMUM_HAND_SIZE,
                                visibleBidCards(xml, biddingHint).size())));
            }

            c.ui.revealText(bid);
            c.ui.tapText(bid);
            c.awaitHumanCard(Duration.ofSeconds(45));
            c.checkpoint(label + "Seat anchors stay fixed when bidding changes to play", xml -> List.of(
                    ReleaseBidContext.equal("Three-player seat rectangles are unchanged", bidBounds,
                            seatBounds(c.ui, xml, bidLabel, scoreLabel)),
                    ReleaseBidContext.equal("The human has a legal card action", true,
                            !c.ui.legalCardDescriptions(xml).isEmpty()),
                    ReleaseBidContext.equal("The table still fits the viewport", true,
                            c.ui.allContentFitsViewport(xml))));
        }

        private static void collectAcrossHand(ReleaseBidContext c, String description, String biddingHint,
                boolean towardEnd, java.util.Set<String> seen) throws Exception {
            String xml = c.ui.dump();
            List<String> visible = visibleBidCards(xml, biddingHint);
            for (int attempt = 0; attempt < MAX_SWIPES_PER_DIRECTION && seen.size() < MAXIMUM_HAND_SIZE; attempt++) {
                c.swipeHand(c.ui.boundsForDescription(xml, description), towardEnd);
                xml = c.ui.dump();
                List<String> after = visibleBidCards(xml, biddingHint);
                seen.addAll(after);
                if (after.equals(visible)) break;
                visible = after;
            }
        }

        private static List<String> visibleBidCards(String xml, String biddingHint) throws Exception {
            return HierarchyDocument.nodes(xml).stream()
                    .filter(node -> node.description().endsWith(". " + biddingHint))
                    .map(HierarchyDocument.Node::description).toList();
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

        private ThreePlayerDownAndUpLayoutScenario() { }
    }
}
