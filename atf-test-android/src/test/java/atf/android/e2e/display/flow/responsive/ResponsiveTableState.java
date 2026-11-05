package atf.android.e2e.display.flow.responsive;

import static atf.android.e2e.display.flow.responsive.ResponsiveSettings.DEAL_NUMBER;
import static atf.android.e2e.display.flow.responsive.ResponsiveSettings.HAND_SIZE;
import static atf.android.e2e.display.support.ScreenGeometry.height;

import atf.android.AdbClient;
import atf.android.e2e.display.profile.ResponsiveDisplayProfile;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.reporting.contract.AtfReportStatus;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/** Rezolvă starea vizibilă și navigarea în viewport-ul mesei. */
public final class ResponsiveTableState {
    private ResponsiveTableState() {}

    public static String revealSeatCross(
            AdbClient adb,
            AndroidUiDriver ui,
            String cardDescription,
            ResponsiveDisplayProfile profile,
            String latest)
            throws Exception {
        int[] dimensions =
                AndroidUiDriver.displayDimensions(adb.run("shell", "wm", "size").output());
        for (int attempt = 0; attempt < 6; attempt++) {
            AndroidUiDriver.Bounds cardSlot = ui.boundsForDescription(latest, cardDescription);
            boolean fullCardRowVisible =
                    height(cardSlot) >= (int) Math.round(profile.minimumPlayedCardSlotHeightDp()
                            * profile.density() / 160.0);
            if (fullCardRowVisible && humanSeatFullyVisible(ui, latest)) return latest;
            int distance = Math.max(96, Math.round(dimensions[1] * 0.12f));
            int startY = dimensions[1] * 90 / 100;
            int endY = Math.max(1, startY - distance);
            long started = System.nanoTime();
            adb.run(
                            "shell",
                            "input",
                            "swipe",
                            Integer.toString(dimensions[0] / 2),
                            Integer.toString(startY),
                            Integer.toString(dimensions[0] / 2),
                            Integer.toString(endY),
                            "300")
                    .requireSuccess();
            ui.recordAction(
                    "Swipe",
                    "Scrollable game table",
                    "Expose the full player cross",
                    "Scrolled the table viewport by "
                            + distance
                            + " px for profile "
                            + profile.id(),
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    AtfReportStatus.PASS);
            Thread.sleep(250);
            latest = ui.dump();
            if (!ui.hasDescription(latest, cardDescription)) {
                throw new IllegalStateException(
                        "Played card left the visible table while exposing seats");
            }
        }
        return latest;
    }

    public static String awaitPlayableTurn(AdbClient adb, AndroidUiDriver ui) throws Exception {
        int[] dimensions =
                AndroidUiDriver.displayDimensions(adb.run("shell", "wm", "size").output());
        boolean playControlObserved = false;
        for (int attempt = 0; attempt < 8; attempt++) {
            String latest = ui.dump();
            playControlObserved |= ui.hasText(latest, "PLAY NOW");
            if (playControlObserved && !ui.legalCardDescriptions(latest).isEmpty()) return latest;
            int distance = Math.max(96, Math.round(dimensions[1] * 0.12f));
            int startY = dimensions[1] * 88 / 100;
            int endY = Math.max(1, startY - distance);
            long started = System.nanoTime();
            adb.run(
                            "shell",
                            "input",
                            "swipe",
                            Integer.toString(dimensions[0] / 2),
                            Integer.toString(startY),
                            Integer.toString(dimensions[0] / 2),
                            Integer.toString(endY),
                            "300")
                    .requireSuccess();
            ui.recordAction(
                    "Swipe",
                    "Scrollable game table",
                    "Reveal the active player and legal hand",
                    "Scrolled the compact viewport by " + distance + " px",
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    AtfReportStatus.PASS);
            Thread.sleep(250);
        }
        throw new IllegalStateException(
                "Active play control remained outside the viewport after scrolling");
    }

    public static String scrollUntilVisible(
            AdbClient adb,
            AndroidUiDriver ui,
            ResponsiveDisplayProfile profile,
            String expected,
            boolean description,
            int minimumVisibleHeightDp)
            throws Exception {
        int[] dimensions =
                AndroidUiDriver.displayDimensions(adb.run("shell", "wm", "size").output());
        int distance = Math.max(96, Math.round(dimensions[1] * 0.12f));
        for (int attempt = 0; attempt < 8; attempt++) {
            String latest = ui.dump();
            boolean visible =
                    description
                            ? ui.hasDescriptionContaining(latest, expected)
                            : ui.hasText(latest, expected);
            if (visible) {
                int requiredHeight =
                        (int) Math.round(minimumVisibleHeightDp * profile.density() / 160.0);
                if (description && HAND_SIZE.matcher(expected).matches()) {
                    int expectedCards = Integer.parseInt(
                            HAND_SIZE.matcher(expected).results().findFirst().orElseThrow().group(1));
                    AndroidUiDriver.Bounds hand = ui.boundsForDescriptionContaining(latest, expected);
                    List<AndroidUiDriver.Bounds> faces = ui.handCardFaceBounds(latest);
                    boolean completeHandVisible = faces.size() == expectedCards
                            && faces.stream().allMatch(face -> height(face) >= requiredHeight
                                    && face.top() >= hand.top() && face.bottom() <= hand.bottom());
                    if (completeHandVisible) return latest;
                } else {
                    int visibleHeight =
                            description
                                    ? height(ui.boundsForDescriptionContaining(latest, expected))
                                    : requiredHeight;
                    if (visibleHeight >= requiredHeight) return latest;
                }
            }
            if (attempt == 7) break;
            int startY = dimensions[1] * 90 / 100;
            int endY = Math.max(1, startY - distance);
            long started = System.nanoTime();
            adb.run(
                            "shell",
                            "input",
                            "swipe",
                            Integer.toString(dimensions[0] / 2),
                            Integer.toString(startY),
                            Integer.toString(dimensions[0] / 2),
                            Integer.toString(endY),
                            "300")
                    .requireSuccess();
            ui.recordAction(
                    "Swipe",
                    "Scrollable game table",
                    "Reveal " + expected,
                    "Scrolled the " + profile.id() + " viewport by " + distance + " px",
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    AtfReportStatus.PASS);
            Thread.sleep(250);
        }
        throw new IllegalStateException(
                "UI did not expose the requested game control after scrolling: " + expected);
    }

    public static boolean humanSeatFullyVisible(AndroidUiDriver ui, String xml) throws Exception {
        List<String> descriptions = seatDescriptions(ui, xml);
        String human =
                descriptions.stream()
                        .filter(description -> description.startsWith("You. Bid "))
                        .findFirst()
                        .orElse(null);
        if (human == null) return false;
        int tallestOtherSeat = 0;
        for (String description : descriptions) {
            if (!description.startsWith("You. Bid ")) {
                tallestOtherSeat =
                        Math.max(
                                tallestOtherSeat,
                                height(ui.boundsForDescription(xml, description)));
            }
        }
        return tallestOtherSeat > 0
                && height(ui.boundsForDescription(xml, human)) >= tallestOtherSeat;
    }

    public static String revealCompletePlayedCard(
            AdbClient adb,
            AndroidUiDriver ui,
            String description,
            ResponsiveDisplayProfile profile,
            String latest)
            throws Exception {
        int expectedFaceHeight =
                (int) Math.round(profile.playedCardFaceHeightDp() * profile.density() / 160.0);
        int[] dimensions =
                AndroidUiDriver.displayDimensions(adb.run("shell", "wm", "size").output());
        for (int attempt = 0; attempt < 3; attempt++) {
            AndroidUiDriver.Bounds face = ui.boundsForText(latest, "A♥");
            if (height(face) >= expectedFaceHeight) return latest;
            int distance = Math.max(64, Math.round(dimensions[1] * 0.05f));
            int startY = dimensions[1] * 88 / 100;
            int endY = Math.max(1, startY - distance);
            long started = System.nanoTime();
            adb.run(
                            "shell",
                            "input",
                            "swipe",
                            Integer.toString(dimensions[0] / 2),
                            Integer.toString(startY),
                            Integer.toString(dimensions[0] / 2),
                            Integer.toString(endY),
                            "300")
                    .requireSuccess();
            ui.recordAction(
                    "Swipe",
                    "Scrollable game table",
                    "Reveal the complete played-card face",
                    "Scrolled the table viewport by "
                            + distance
                            + " px for profile "
                            + profile.id(),
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    AtfReportStatus.PASS);
            Thread.sleep(250);
            latest = ui.dump();
            if (!ui.hasDescription(latest, description)) {
                throw new IllegalStateException(
                        "Played card left the visible table after scrolling");
            }
        }
        throw new IllegalStateException(
                "Played-card face remained clipped after three small table scrolls: "
                        + ui.boundsForText(latest, "A♥"));
    }

    public static int handSize(AndroidUiDriver ui, String xml) throws Exception {
        Matcher match =
                HAND_SIZE.matcher(
                        ui.descriptions(xml).stream()
                                .filter(value -> value.startsWith("Your hand, "))
                                .findFirst()
                                .orElseThrow(
                                        () ->
                                                new IllegalStateException(
                                                        "Own hand size is absent")));
        if (!match.matches()) throw new IllegalStateException("Unexpected hand description");
        return Integer.parseInt(match.group(1));
    }

    public static int dealNumber(AndroidUiDriver ui, String xml) throws Exception {
        Matcher match =
                DEAL_NUMBER.matcher(
                        ui.texts(xml).stream()
                                .filter(value -> value.startsWith("Hand "))
                                .findFirst()
                                .orElseThrow(
                                        () ->
                                                new IllegalStateException(
                                                        "Deal progress is absent")));
        if (!match.matches()) throw new IllegalStateException("Unexpected deal progress");
        return Integer.parseInt(match.group(1));
    }

    public static int romanianFourPlayerHandSize(int dealNumber) {
        if (dealNumber < 1 || dealNumber > 24) {
            throw new IllegalArgumentException("Romanian deal number must be between 1 and 24");
        }
        if (dealNumber <= 4 || dealNumber >= 21) return 1;
        if (dealNumber <= 10) return dealNumber - 3;
        if (dealNumber <= 14) return 8;
        return 22 - dealNumber;
    }

    public static int seatCount(AndroidUiDriver ui, String xml) throws Exception {
        return seatBounds(ui, xml).size();
    }

    public static List<AndroidUiDriver.Bounds> seatBounds(AndroidUiDriver ui, String xml)
            throws Exception {
        List<AndroidUiDriver.Bounds> seats = new ArrayList<>();
        for (String description : seatDescriptions(ui, xml)) {
            seats.add(ui.boundsForDescription(xml, description));
        }
        return seats;
    }

    public static List<String> seatDescriptions(AndroidUiDriver ui, String xml) throws Exception {
        return ui.descriptions(xml).stream()
                .filter(
                        description ->
                                description.contains(". Bid ") && description.contains(". Score "))
                .toList();
    }
}
