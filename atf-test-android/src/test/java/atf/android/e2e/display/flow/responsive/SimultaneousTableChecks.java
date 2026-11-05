package atf.android.e2e.display.flow.responsive;

import atf.android.AdbClient;
import atf.android.e2e.display.profile.ResponsiveDisplayProfile;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.android.whist.evidence.TableViewportAudit;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static atf.android.e2e.display.flow.responsive.ResponsiveEvidence.record;
import static atf.android.e2e.display.support.ResponsiveGeometry.box;
import static atf.android.e2e.display.flow.responsive.ResponsiveSettings.APP;
import static atf.android.e2e.display.flow.responsive.ResponsiveSettings.WAIT;
import static atf.android.e2e.display.flow.responsive.ResponsiveTableState.seatBounds;

/** Verifică o singură stare capturată; nu unește observații din viewport-uri diferite. */
public final class SimultaneousTableChecks {
    private SimultaneousTableChecks() {}

    public static void inspect(AdbClient adb, AndroidUiDriver ui, AndroidUiFlowTrace trace,
            List<Path> evidence, ResponsiveDisplayProfile profile) throws Exception {
        for (boolean completeTrick : new boolean[]{false, true}) {
            String fixture = completeTrick ? "debug_compact_table_fixture" : "debug_hand_fan_fixture";
            int handSize = completeTrick ? 12 : 13;
            ui.restartToActivity(APP, ".MainActivity", "--ez", fixture, "true",
                    "--ez", "debug_hold_ai_turns", "true", "--es", "debug_language", "EN");
            String xml = ui.awaitDescriptionContaining("Your hand, " + handSize + " cards.", WAIT);
            var checks = auditSnapshot(ui, xml, profile, handSize, completeTrick);
            record(ui, adb, trace, evidence, profile,
                    completeTrick ? "See all four trick cards, twelve hand cards and four seats without scrolling"
                            : "See thirteen hand cards and every seat without scrolling", checks);
        }
    }

    /** Decodes a recorded hierarchy without sending device commands. */
    public static List<atf.reporting.contract.AtfAssertion> auditSnapshot(AndroidUiDriver ui, String xml,
            ResponsiveDisplayProfile profile, int handSize, boolean completeTrick) throws Exception {
        var cards = new ArrayList<TableViewportAudit.PlayedRegion>();
        for (String description : ui.descriptions(xml).stream().filter(value -> value.contains(" played ")).toList()) {
            String label = description.substring(description.indexOf(" played ") + " played ".length());
            cards.add(new TableViewportAudit.PlayedRegion(label, box(ui.boundsForDescription(xml, description)),
                    box(ui.boundsForText(xml, shortLabel(label)))));
        }
        var snapshot = new TableViewportAudit.Snapshot(
                new atf.android.whist.evidence.HandIndexAudit.Box(0, 0, profile.width(), profile.height()),
                box(ui.boundsForDescription(xml, "Playing surface")),
                box(ui.boundsForDescriptionContaining(xml, "Your hand,")),
                seatBounds(ui, xml).stream().map(atf.android.e2e.display.support.ResponsiveGeometry::box).toList(),
                ui.handCardFaceBounds(xml).stream().map(atf.android.e2e.display.support.ResponsiveGeometry::box).toList(),
                cards, ui.hasClass(xml, "android.widget.ScrollView"),
                ui.hasText(xml, "Whist · Descending") && ui.hasDescriptionContaining(xml, "Trump"));
        double density = profile.density() / 160.0;
        var expected = new TableViewportAudit.Requirements(4, handSize, completeTrick ? 4 : 0,
                (int) Math.round(98 * density), (int) Math.round(profile.playedCardFaceHeightDp() * density),
                (int) Math.floor(44 * density));
        return TableViewportAudit.inspect(snapshot, expected);
    }

    private static String shortLabel(String card) {
        String[] parts = card.split(" of ", 2);
        String rank = switch (parts[0]) {
            case "Ace" -> "A";
            case "King" -> "K";
            case "Queen" -> "Q";
            case "Jack" -> "J";
            default -> parts[0];
        };
        String suit = switch (parts[1]) {
            case "spades" -> "♠";
            case "hearts" -> "♥";
            case "clubs" -> "♣";
            case "diamonds" -> "♦";
            default -> throw new IllegalArgumentException("Unknown fixture card: " + card);
        };
        return rank + suit;
    }
}
