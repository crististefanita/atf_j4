package atf.android.e2e.release;

import atf.android.e2e.driver.hierarchy.HierarchyDocument;
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
        for (int hand = 1; hand <= 4; hand++) {
            c.awaitRecommendation(labels.submit());
            c.ui.tapText(labels.submit());
            finishOneCardHand(c, labels);
            final int ordinal = hand;
            c.checkpoint("Real one-card hand " + ordinal + " completed", xml -> List.of(
                    ReleaseBidContext.equal("Hand prefix reached its real score screen", true, c.ui.hasText(xml, labels.next()))));
            c.ui.tapText(labels.next());
        }
        var recommended = c.awaitRecommendation(labels.submit());
        int selected = recommended.selected().orElseThrow();
        int manual = recommended.choices().stream().filter(value -> value != selected).findFirst().orElseThrow();
        c.checkpoint("Two-card recommendation waits for confirmation", xml -> {
            var state = BidChoiceObservation.read(xml, labels.submit());
            return List.of(ReleaseBidContext.equal("Two cards are visible to the human", true,
                    c.ui.hasDescription(xml, ro ? "Cărțile tale, 2." : "Your hand, 2 cards.")),
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
        c.ui.tapText(labels.history().toUpperCase(java.util.Locale.ROOT));
        c.ui.awaitText(labels.history(), Duration.ofSeconds(15));
        c.checkpoint("Real score history exposes the cumulative points legend", xml -> List.of(
                ReleaseBidContext.equal("The real history dialog is open", true, c.ui.hasText(xml, labels.history())),
                ReleaseBidContext.equal("The history legend explains cumulative points", true,
                        c.ui.hasTextContaining(xml, ro ? "P Punctaj total" : "P Total score")),
                ReleaseBidContext.equal("History remains dismissible", true, c.ui.hasText(xml, labels.close()))));
        c.ui.tapText(labels.close());
        c.ui.awaitText(labels.close(), Duration.ofSeconds(15));
        c.ui.tapText(labels.close());
        c.ui.awaitText(labels.submit(), Duration.ofSeconds(15));
        assertManual(c, labels, human, manual, "Closing the dialog preserves manual choice");
        c.ui.tapText(labels.submit());
        awaitHumanCard(c);
        c.checkpoint("Explicit confirmation commits the manual contract", xml -> List.of(
                ReleaseBidContext.equal("The human seat shows the manually confirmed value", true,
                        c.ui.hasDescriptionContaining(xml, human + manual + ".")),
                ReleaseBidContext.equal("Real legal cards are available", true, !c.ui.legalCardDescriptions(xml).isEmpty())));
        var seats = seatDescriptions(c, labels);
        c.ui.restartToActivity(ReleaseBidContext.PACKAGE, ".MainActivity");
        c.ui.awaitText(labels.resume(), Duration.ofSeconds(15)); c.ui.tapText(labels.resume());
        awaitHumanCard(c);
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

    private static void finishOneCardHand(ReleaseBidContext c, Labels labels) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(45).toNanos();
        do {
            String xml = c.ui.dump();
            if (c.ui.hasText(xml, labels.next())) return;
            var legal = c.ui.legalCardDescriptions(xml);
            if (!legal.isEmpty()) c.ui.tapDescription(legal.getFirst());
            else Thread.sleep(120);
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException("Real one-card hand did not reach its score screen");
    }

    private static void awaitHumanCard(ReleaseBidContext c) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        do {
            if (!c.ui.legalCardDescriptions(c.ui.dump()).isEmpty()) return;
            Thread.sleep(120);
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException("The signed app did not reach the human's card turn");
    }

    private static List<String> seatDescriptions(ReleaseBidContext c, Labels labels) throws Exception {
        return c.ui.descriptions(c.ui.dump()).stream().filter(value -> value.contains(". " + labels.bid() + " ")).distinct().toList();
    }
    private record Labels(boolean ro) {
        String newGame() { return ro ? "Joc nou" : "New game"; }
        String submit() { return ro ? "Licitează" : "Place bid"; }
        String next() { return ro ? "Mâna următoare" : "Next hand"; }
        String close() { return (ro ? "Închide" : "Close").toUpperCase(java.util.Locale.ROOT); }
        String history() { return ro ? "Istoricul scorului" : "Score history"; }
        String resume() { return ro ? "Reia partida" : "Resume game"; }
        String bid() { return ro ? "Licitat" : "Bid"; }
    }
    private ReleaseBidScenario() { }
}
