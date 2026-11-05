package atf.android.e2e.gameplay.support;

import atf.android.e2e.driver.AndroidUiDriver;

import java.time.Duration;

/** Așteaptă tranzițiile observabile ale levatelor fără a schimba regulile scenariului. */
public final class TrickUiState {
    private TrickUiState() {}

    public static String awaitPlayedCardCount(
            AndroidUiDriver ui, int expectedCount, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        String latest;
        do {
            latest = ui.dump();
            if (playedCardCount(ui, latest) == expectedCount) return latest;
            Thread.sleep(100);
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException(
                "Expected "
                        + expectedCount
                        + " played cards together; descriptions="
                        + ui.descriptions(latest));
    }

    public static int playedCardCount(AndroidUiDriver ui, String xml) throws Exception {
        return (int)
                ui.descriptions(xml).stream()
                        .filter(description -> description.contains(" played "))
                        .filter(
                                description ->
                                        !description.startsWith("No cards have been played "))
                        .count();
    }

    public static String awaitNextTrickReady(AndroidUiDriver ui, Duration timeout)
            throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        String latest;
        do {
            latest = ui.dump();
            if (ui.hasDescription(latest, "Your hand, 1 cards.")
                    && ui.hasText(latest, "PLAY NOW")
                    && playedCardCount(ui, latest) == 0) return latest;
            Thread.sleep(100);
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException(
                "Next trick did not become interactive; texts="
                        + ui.texts(latest)
                        + "; descriptions="
                        + ui.descriptions(latest));
    }

    public static boolean hasPlayedCardText(AndroidUiDriver ui, String xml, String cardLabel)
            throws Exception {
        return ui.texts(xml).stream().anyMatch(text -> text.equals(cardLabel));
    }
}
