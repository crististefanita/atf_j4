package atf.android.e2e.release;

import atf.android.e2e.driver.hierarchy.HierarchyDocument;
import atf.android.e2e.release.turn.BidChoiceObservation;
import java.time.Duration;
import java.util.List;

/** UI-only Romanian match prefix; no debug extras, checkpoint injection or hidden-hand access. */
final class ReleaseBidScenario {
    static void run(ReleaseBidContext c) throws Exception {
        String installed = c.installedHash();
        if (!c.setup.required("requestedApkSha256").asText().equals(installed))
            throw new IllegalStateException("Installed APK differs from the signed candidate");
        c.ui.restartToActivity(ReleaseBidContext.PACKAGE, ".MainActivity");
        String home = c.ui.dump(); boolean ro = c.ui.hasText(home, "Joc nou");
        var labels = new Labels(ro);
        String profile = ro ? "Numele tău: " : "Your name: ";
        var names = HierarchyDocument.nodes(home).stream().filter(node -> node.className().equals("android.widget.Button")
                && node.text().startsWith(profile)).map(node -> node.text().substring(profile.length())).toList();
        if (names.size() != 1 || names.getFirst().isBlank()) throw new IllegalStateException("Visible human profile is ambiguous");
        String human = names.getFirst() + ". " + labels.bid() + " ";
        c.checkpoint("Signed APK and real home", xml -> List.of(
                ReleaseBidContext.equal("Installed candidate SHA-256", c.setup.required("requestedApkSha256").asText(), installed),
                ReleaseBidContext.equal("Real home offers a new match", true, c.ui.hasText(xml, labels.newGame()))));
        c.ui.tapText(labels.newGame());
        c.ui.selectNamedSpinnerOption(ro ? "Jucători" : "Players", "4");
        c.ui.selectNamedSpinnerOption(ro ? "Varianta de joc" : "Game type", ro ? "Românesc (1–8–1)" : "Romanian (1–8–1)");
        c.ui.selectNamedSpinnerOption(ro ? "Nivelul adversarilor" : "AI level", ro ? "Dificil" : "Hard");
        c.checkpoint("Romanian four-player Hard configuration", xml -> List.of(
                ReleaseBidContext.equal("Four players are selected", "4", c.ui.spinnerValue(xml, ro ? "Jucători" : "Players")),
                ReleaseBidContext.equal("Romanian schedule is selected", ro ? "Românesc (1–8–1)" : "Romanian (1–8–1)",
                        c.ui.spinnerValue(xml, ro ? "Varianta de joc" : "Game type")),
                ReleaseBidContext.equal("Hard opponents are selected", ro ? "Dificil" : "Hard",
                        c.ui.spinnerValue(xml, ro ? "Nivelul adversarilor" : "AI level"))));
        c.ui.tapText(ro ? "Începe partida" : "Start game");
        var recommended = c.awaitRecommendation(labels.submit(), 1);
        // A dealer may have one legal bid. The next deal rotates the dealer and opens both choices.
        if (recommended.choices().size() == 1) {
            c.ui.tapText(labels.submit());
            c.finishOneCardHand(labels.handComplete(), labels.next());
            c.ui.tapText(labels.next());
            recommended = c.awaitRecommendation(labels.submit(), 1);
        }
        int selected = recommended.selected().orElseThrow();
        int manual = recommended.choices().stream().filter(value -> value != selected).findFirst().orElseThrow();
        c.checkpoint("Opening recommendation waits for confirmation", xml -> {
            var state = BidChoiceObservation.read(xml, labels.submit());
            return List.of(ReleaseBidContext.equal("Both opening contracts are visible and legal", List.of(0, 1), state.choices()),
                    ReleaseBidContext.equal("Recommended choice remains selected", selected, state.selected().orElseThrow()),
                    ReleaseBidContext.equal("Human contract has not been submitted automatically", true,
                            c.ui.hasDescriptionContaining(xml, human + (ro ? "nepusă" : "not placed") + ".")));
        });
        c.ui.selectBid(manual);
        assertManual(c, labels, human, manual, "Manual choice replaces the recommendation");
        c.ui.tapDescription(ro ? "Meniu joc" : "Game menu");
        c.ui.tapText(ro ? "Scorul partidei" : "Score sheet");
        c.ui.awaitText(labels.close(), Duration.ofSeconds(15));
        c.checkpoint("Score dialog suspends bidding", xml -> List.of(
                ReleaseBidContext.equal("Score dialog can be closed", true, c.ui.hasText(xml, labels.close()))));
        c.ui.tapText(labels.close());
        c.ui.awaitText(labels.submit(), Duration.ofSeconds(15));
        assertManual(c, labels, human, manual, "Closing the dialog preserves manual choice");
        c.ui.tapText(labels.submit());
        c.awaitHumanCard(Duration.ofSeconds(30));
        c.checkpoint("Explicit confirmation commits the manual contract", xml -> List.of(
                ReleaseBidContext.equal("The human seat shows the manually confirmed value", true,
                        c.ui.hasDescriptionContaining(xml, human + manual + ".")),
                ReleaseBidContext.equal("Real legal cards are available", true, !c.ui.legalCardDescriptions(xml).isEmpty())));
        var seats = seatDescriptions(c, labels);
        c.ui.restartToActivity(ReleaseBidContext.PACKAGE, ".MainActivity");
        c.ui.awaitText(labels.resume(), Duration.ofSeconds(15)); c.ui.tapText(labels.resume());
        c.awaitHumanCard(Duration.ofSeconds(30));
        c.checkpoint("Process restart preserves the confirmed contract", xml -> List.of(
                ReleaseBidContext.equal("Confirmed manual bid survives process restart", true,
                        c.ui.hasDescriptionContaining(xml, human + manual + ".")),
                ReleaseBidContext.equal("All public seat contracts and scores are restored", seats,
                        c.ui.descriptions(xml).stream().filter(value -> value.contains(". " + labels.bid() + " ")).distinct().toList())));
    }

    private static void assertManual(ReleaseBidContext c, Labels labels, String human, int manual, String title) throws Exception {
        c.checkpoint(title, xml -> {
            var state = BidChoiceObservation.read(xml, labels.submit());
            return List.of(ReleaseBidContext.equal("Manual selection remains authoritative", manual, state.selected().orElseThrow()),
                    ReleaseBidContext.equal("Confirmation is enabled for the human", true, state.submitEnabled()),
                    ReleaseBidContext.equal("Selection alone does not place a contract", true,
                            c.ui.hasDescriptionContaining(xml, human + (labels.ro() ? "nepusă" : "not placed") + ".")));
        });
    }

    private static List<String> seatDescriptions(ReleaseBidContext c, Labels labels) throws Exception {
        return c.ui.descriptions(c.ui.dump()).stream().filter(value -> value.contains(". " + labels.bid() + " ")).distinct().toList();
    }
    private record Labels(boolean ro) {
        String newGame() { return ro ? "Joc nou" : "New game"; }
        String submit() { return ro ? "Licitează" : "Place bid"; }
        String handComplete() { return ro ? "Mână încheiată" : "Hand complete"; }
        String next() { return ro ? "Mâna următoare" : "Next hand"; }
        String close() { return (ro ? "Închide" : "Close").toUpperCase(java.util.Locale.ROOT); }
        String resume() { return ro ? "Reia partida" : "Resume game"; }
        String bid() { return ro ? "Licitat" : "Bid"; }
    }
    private ReleaseBidScenario() { }
}
