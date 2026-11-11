package atf.android.e2e.release.autogame;

import atf.android.e2e.release.ReleaseBidContext;
import atf.android.e2e.driver.AndroidUiDriver.Bounds;
import atf.android.e2e.driver.hierarchy.HierarchyDocument;
import java.util.Map;
import java.util.TreeMap;
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
        c.ui.awaitText(labels.playBadge(), Duration.ofSeconds(25));
        c.checkpoint("AutoGame keeps the acting seat marked during play", xml -> List.of(
                ReleaseBidContext.equal("Exactly one seat identifies the current player", 1L,
                        badgeCount(xml, labels.playBadge())),
                ReleaseBidContext.equal("The visible table retains every seat", 4,
                        seatFrames(xml, labels).size()),
                ReleaseBidContext.equal("Autoplay remains active while the actor is marked", true,
                        c.ui.hasDescription(xml, labels.stopAutoGame()))));
        c.ui.awaitText(labels.handComplete(), Duration.ofSeconds(45));
        c.checkpoint("AutoGame completes the current hand at its real score boundary", xml -> List.of(
                ReleaseBidContext.equal("One-card hand reached the score screen", true,
                        c.ui.hasText(xml, labels.handComplete())),
                ReleaseBidContext.equal("The score screen has no stale actor badge", 0L,
                        badgeCount(xml, labels.playBadge()) + badgeCount(xml, labels.bidBadge()))));
        c.ui.revealText(labels.next());
        c.checkpoint("The score page exposes the continuation action after scrolling when needed", xml -> List.of(
                ReleaseBidContext.equal("The Next hand button is visible and enabled", true,
                        HierarchyDocument.nodes(xml).stream()
                                .anyMatch(node -> node.text().equals(labels.next()) && node.clickable() && node.enabled()))));

        c.ui.tapText(labels.next());
        c.awaitRecommendation(labels.submit());
        c.checkpoint("AutoGame does not leak into the next hand", xml -> List.of(
                ReleaseBidContext.equal("The next hand exposes the same visible bid control", true,
                        c.ui.hasText(xml, labels.autoGameToggle())),
                ReleaseBidContext.equal("The new hand returns to the unchecked state", true,
                        c.ui.hasDescription(xml, labels.startAutoGame())
                                && !c.ui.hasDescription(xml, labels.stopAutoGame()))));
        c.ui.revealDescriptionWithHeight(labels.handPrefix(), privateHandHeight(c));
        String bid = c.ui.dump();
        Map<String, Bounds> bidSeats = seatFrames(bid, labels);
        c.checkpoint("The new hand exposes the active bidder and its table positions", xml -> List.of(
                ReleaseBidContext.equal("All four seat frames are visible", 4, seatFrames(xml, labels).size()),
                ReleaseBidContext.equal("Exactly one player has the bidding indicator", 1L, badgeCount(xml, labels.bidBadge()))));

        c.ui.tapText(labels.submit());
        c.ui.awaitText(labels.playBadge(), Duration.ofSeconds(30));
        c.checkpoint("Play retains the same seat rectangles and shows the active player", xml -> List.of(
                ReleaseBidContext.equal("Each seat frame occupies the same pixel rectangle", bidSeats, seatFrames(xml, labels)),
                ReleaseBidContext.equal("Exactly one player has the play indicator", 1L, badgeCount(xml, labels.playBadge())),
                ReleaseBidContext.equal("The full private card remains visible below the seats", true,
                        privateHandIsVisible(c, xml, labels, bidSeats))));
        c.ui.back();
    }

    private static Map<String, Bounds> seatFrames(String xml, Labels labels) throws Exception {
        Map<String, Bounds> frames = new TreeMap<>();
        for (var node : HierarchyDocument.nodes(xml)) {
            int marker = node.description().indexOf(labels.seatMarker());
            if (marker > 0 && node.className().equals("android.widget.LinearLayout")) {
                frames.put(node.description().substring(0, marker), HierarchyDocument.toBounds(node.bounds()));
            }
        }
        return frames;
    }

    private static long badgeCount(String xml, String label) throws Exception {
        return HierarchyDocument.nodes(xml).stream().filter(node -> node.text().equals(label)).count();
    }

    private static boolean privateHandIsVisible(ReleaseBidContext c, String xml, Labels labels,
            Map<String, Bounds> seats) throws Exception {
        int expected = privateHandHeight(c);
        int seatBottom = seats.values().stream().mapToInt(Bounds::bottom).max().orElseThrow();
        Bounds hand = c.ui.boundsForDescriptionContaining(xml, labels.handPrefix());
        return HierarchyDocument.nodes(xml).stream()
                .filter(node -> node.className().equals("android.widget.FrameLayout"))
                .filter(node -> node.description().contains(labels.cardStateMarker()))
                .map(node -> HierarchyDocument.toBounds(node.bounds()))
                .anyMatch(card -> card.bottom() - card.top() == expected
                        && card.top() >= seatBottom && card.top() >= hand.top() && card.bottom() <= hand.bottom()
                        && card.left() >= hand.left() && card.right() <= hand.right());
    }

    private static int privateHandHeight(ReleaseBidContext context) {
        int densityDpi = context.setup.required("display").required("density").asInt();
        if (densityDpi <= 0) throw new IllegalStateException("Missing native display density");
        float density = densityDpi / 160f;
        return Math.round(88 * density) + Math.round(28 * density);
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
        String handComplete() { return ro ? "Mână încheiată" : "Hand complete"; }
        String bidBadge() { return ro ? "LICITEAZĂ" : "BID NOW"; }
        String playBadge() { return ro ? "JOACĂ ACUM" : "PLAY NOW"; }
        String seatMarker() { return ro ? ". Licitat " : ". Bid "; }
        String handPrefix() { return ro ? "Cărțile tale, " : "Your hand, "; }
        String cardStateMarker() { return ". "; }
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
