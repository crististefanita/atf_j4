package atf.android.e2e.driver;

import static atf.android.e2e.driver.UiHierarchy.matchesSpinnerLabel;
import static atf.android.e2e.driver.UiHierarchy.nodes;

import atf.android.AdbClient;
import atf.android.e2e.driver.UiHierarchy.Node;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.regex.Matcher;


/** Small UI Automator bridge; resolves visible labels and bounds from each fresh hierarchy. */
public final class AndroidUiDriver {

    public interface Device {
        AdbClient.Result run(String... arguments) throws IOException, InterruptedException;

        AdbClient.BinaryResult runBinary(String... arguments)
                throws IOException, InterruptedException;
    }

    @FunctionalInterface
    interface Delay {
        void sleep(long millis) throws InterruptedException;
    }

    private final Device adb;
    private final Path output;
    private final UiDeviceCapture captures;
    private final LongSupplier nanoTime;
    private final Delay pause;
    private Consumer<AtfUiActionRecord> actionSink = ignored -> {};
    private java.util.function.Supplier<? extends AutoCloseable> captureScopes = () -> () -> {};
    private int actionSequence;

    public AndroidUiDriver(AdbClient adb, Path output) throws Exception {
        this(
                new Device() {
                    public AdbClient.Result run(String... arguments)
                            throws IOException, InterruptedException {
                        return adb.run(arguments);
                    }

                    public AdbClient.BinaryResult runBinary(String... arguments)
                            throws IOException, InterruptedException {
                        return adb.runBinary(arguments);
                    }
                },
                output);
    }

    public AndroidUiDriver(Device adb, Path output) throws Exception {
        this(adb, output, System::nanoTime, Thread::sleep);
    }

    AndroidUiDriver(Device adb, Path output, LongSupplier nanoTime, Delay pause) throws Exception {
        this.adb = Objects.requireNonNull(adb);
        this.output = output.toAbsolutePath().normalize();
        this.nanoTime = Objects.requireNonNull(nanoTime);
        this.pause = Objects.requireNonNull(pause);
        Files.createDirectories(this.output);
        captures = new UiDeviceCapture(this.adb, this.output);
    }

    public void recordActionsAs(Consumer<AtfUiActionRecord> sink) {
        actionSink = Objects.requireNonNull(sink, "sink");
    }

    public void recordCapturesAs(java.util.function.Supplier<? extends AutoCloseable> scopes) {
        captureScopes = Objects.requireNonNull(scopes, "scopes");
    }

    public String dump() throws Exception {
        return captures.dump();
    }

    public boolean hasText(String xml, String exactText) throws Exception {
        return UiHierarchy.hasText(xml, exactText);
    }

    public boolean hasTextContaining(String xml, String fragment) throws Exception {
        return UiHierarchy.hasTextContaining(xml, fragment);
    }

    public boolean hasDescription(String xml, String exactDescription) throws Exception {
        return UiHierarchy.hasDescription(xml, exactDescription);
    }

    public boolean hasDescriptionContaining(String xml, String fragment) throws Exception {
        return UiHierarchy.hasDescriptionContaining(xml, fragment);
    }

    public List<String> descriptions(String xml) throws Exception {
        return UiHierarchy.descriptions(xml);
    }

    public Bounds boundsForText(String xml, String exactText) throws Exception {
        return UiHierarchy.boundsForText(xml, exactText);
    }

    public Bounds boundsForDescription(String xml, String exactDescription) throws Exception {
        return UiHierarchy.boundsForDescription(xml, exactDescription);
    }

    public Bounds boundsForDescription(String xml, String exactDescription, String exactClassName)
            throws Exception {
        return UiHierarchy.boundsForDescription(xml, exactDescription, exactClassName);
    }

    public String spinnerValue(String xml, String exactDescription) throws Exception {
        return UiHierarchy.spinnerValue(xml, exactDescription);
    }

    public Bounds boundsForDescriptionContaining(String xml, String fragment) throws Exception {
        return UiHierarchy.boundsForDescriptionContaining(xml, fragment);
    }

    public String firstLegalCardDescription(String xml) throws Exception {
        return UiHierarchy.firstLegalCardDescription(xml);
    }

    public List<String> legalCardDescriptions(String xml) throws Exception {
        return UiHierarchy.legalCardDescriptions(xml);
    }

    public boolean hasClass(String xml, String exactClass) throws Exception {
        return UiHierarchy.hasClass(xml, exactClass);
    }

    public boolean allContentFitsViewport(String xml) throws Exception {
        return UiHierarchy.allContentFitsViewport(xml);
    }

    public boolean isEnabled(String xml, String exactText) throws Exception {
        return UiHierarchy.isEnabled(xml, exactText);
    }

    public boolean legalCardIsClickable(String xml, String cardLabel) throws Exception {
        return UiHierarchy.legalCardIsClickable(xml, cardLabel);
    }

    public boolean isSelected(String xml, String exactText) throws Exception {
        return UiHierarchy.isSelected(xml, exactText);
    }

    List<String> disabledButtonTexts(String xml) throws Exception {
        return UiHierarchy.disabledButtonTexts(xml);
    }

    public List<String> texts(String xml) throws Exception {
        return UiHierarchy.texts(xml);
    }

    public void tapText(String exactText) throws Exception {
        String xml = awaitText(exactText, Duration.ofSeconds(8));
        tapTextIn(xml, exactText);
    }

    public void tapTextIn(String xml, String exactText) throws Exception {
        List<Node> matches =
                nodes(xml).stream().filter(node -> node.text().equals(exactText)).toList();
        if (matches.size() != 1)
            throw new IllegalStateException(
                    "Expected one UI element named '" + exactText + "', got " + matches.size());
        tap(matches.getFirst());
    }

    public void tapDescription(String exactDescription) throws Exception {
        List<Node> matches =
                nodes(dump()).stream()
                        .filter(node -> node.description().equals(exactDescription))
                        .toList();
        if (matches.size() != 1)
            throw new IllegalStateException(
                    "Expected one UI element described as '"
                            + exactDescription
                            + "', got "
                            + matches.size());
        tap(matches.getFirst());
    }

    public List<CardLabel> bottomRowCardLabels(String xml) throws Exception {
        return UiHierarchy.bottomRowCardLabels(xml);
    }

    public List<Bounds> playingCardLabelBounds(String xml) throws Exception {
        return UiHierarchy.playingCardLabelBounds(xml);
    }

    public List<ElementSize> numericButtonSizes(String xml) throws Exception {
        return UiHierarchy.numericButtonSizes(xml);
    }

    public ElementSize buttonSize(String xml, String text) throws Exception {
        return UiHierarchy.buttonSize(xml, text);
    }

    double buttonBottomGapFraction(String xml, Path screenshot, String text) throws Exception {
        return ScreenshotAnalysis.buttonBottomGapFraction(xml, screenshot, text);
    }

    public boolean sameBackgroundAcrossTableMargin(
            Path screenshot, double firstY, double secondY, String displaySize) throws Exception {
        return ScreenshotAnalysis.sameBackgroundAcrossTableMargin(
                screenshot, firstY, secondY, displaySize);
    }

    public int sampleHandFace(Path screenshot, Bounds hand, String displaySize) throws IOException {
        return ScreenshotAnalysis.sampleHandFace(screenshot, hand, displaySize);
    }

    public static int[] displayDimensions(String displaySize) throws IOException {
        return ScreenshotAnalysis.displayDimensions(displaySize);
    }

    public static int[] screenshotPoint(
            java.awt.image.BufferedImage image,
            double logicalX,
            double logicalY,
            int viewportWidth,
            int viewportHeight) {
        return ScreenshotAnalysis.screenshotPoint(
                image, logicalX, logicalY, viewportWidth, viewportHeight);
    }

    public List<Integer> numericButtonValues(String xml) throws Exception {
        return UiHierarchy.numericButtonValues(xml);
    }

    public static List<Integer> enabledNumericButtonValues(String xml) throws Exception {
        return UiHierarchy.enabledNumericButtonValues(xml);
    }

    public void tapSpinner(int index) throws Exception {
        List<Node> spinners =
                nodes(dump()).stream()
                        .filter(
                                node ->
                                        node.className().equals("android.widget.Spinner")
                                                && node.clickable())
                        .toList();
        if (index < 0 || index >= spinners.size())
            throw new IllegalStateException(
                    "Spinner " + index + " not found; count=" + spinners.size());
        tap(spinners.get(index));
    }

    public void selectSpinnerOption(int index, String expectedText) throws Exception {
        tapSpinner(index);
        selectOpenedSpinnerOption(expectedText);
    }

    public void selectBid(int bid) throws Exception {
        String text = Integer.toString(bid);
        var matches =
                nodes(dump()).stream()
                        .filter(
                                node ->
                                        node.className().equals("android.widget.Button")
                                                && node.text().equals(text))
                        .toList();
        if (matches.size() != 1)
            throw new IllegalStateException("Expected one bid button: " + text);
        tap(matches.getFirst());
    }

    public void selectNamedSpinnerOption(String description, String expectedText) throws Exception {
        reveal(
                node ->
                        node.className().equals("android.widget.Spinner")
                                && matchesSpinnerLabel(description, node.description()));
        var matches =
                nodes(dump()).stream()
                        .filter(
                                node ->
                                        node.className().equals("android.widget.Spinner")
                                                && matchesSpinnerLabel(
                                                        description, node.description()))
                        .toList();
        if (matches.size() != 1)
            throw new IllegalStateException("Expected one spinner: " + description);
        tap(matches.getFirst());
        selectOpenedSpinnerOption(expectedText);
    }

    public void revealText(String expected) throws Exception {
        reveal(node -> node.text().equals(expected));
    }

    private void reveal(java.util.function.Predicate<Node> target) throws Exception {
        for (int attempt = 0; attempt < 8; attempt++) {
            var current = nodes(dump());
            if (current.stream().anyMatch(target)) return;
            int width = 0, height = 0;
            for (Node node : current) {
                var match = UiHierarchy.matchBounds(node.bounds());
                if (match.matches()) {
                    width = Math.max(width, Integer.parseInt(match.group(3)));
                    height = Math.max(height, Integer.parseInt(match.group(4)));
                }
            }
            if (width == 0 || height == 0)
                throw new IllegalStateException("No visible scroll bounds");
            int startX = width / 2, startY = height * 4 / 5, endX = width / 2, endY = height / 3;
            long started = nanoTime.getAsLong();
            try {
                adb.run(
                                "shell", "input", "swipe", "" + startX, "" + startY, "" + endX,
                                "" + endY, "350")
                        .requireSuccess();
            } catch (Exception failure) {
                record(
                        "Swipe",
                        "Android setup viewport",
                        "Reveal the configured setup control",
                        failure.toString(),
                        started,
                        AtfReportStatus.FAIL);
                throw failure;
            }
            record(
                    "Swipe",
                    "Android setup viewport",
                    "Reveal the configured setup control",
                    "ADB accepted swipe " + startX + "," + startY + " → " + endX + "," + endY,
                    started,
                    AtfReportStatus.PASS);
            pause.sleep(250);
        }
        throw new IllegalStateException("Setup control not found after scrolling");
    }

    private void selectOpenedSpinnerOption(String expectedText) throws Exception {
        long deadline = nanoTime.getAsLong() + Duration.ofSeconds(8).toNanos();
        String latest;
        do {
            latest = dump();
            List<Node> options =
                    nodes(latest).stream()
                            .filter(
                                    node ->
                                            node.className()
                                                    .equals("android.widget.CheckedTextView"))
                            .filter(node -> node.text().equals(expectedText))
                            .toList();
            if (options.size() == 1) {
                tap(options.get(0));
                return;
            }
            pause.sleep(250);
        } while (nanoTime.getAsLong() < deadline);
        throw new IllegalStateException(
                "Spinner option '"
                        + expectedText
                        + "' did not appear exactly once; visible texts="
                        + texts(latest));
    }

    String awaitSpinnerSelection(String expected, Duration timeout) throws Exception {
        long deadline = nanoTime.getAsLong() + timeout.toNanos();
        String latest;
        do {
            latest = dump();
            if (hasText(latest, "START")
                    && hasText(latest, expected)
                    && !hasClass(latest, "android.widget.CheckedTextView")) return latest;
            pause.sleep(250);
        } while (nanoTime.getAsLong() < deadline);
        throw new IllegalStateException(
                "Spinner did not settle on '"
                        + expected
                        + "' with the setup dialog still open; visible texts="
                        + texts(latest));
    }

    public String awaitText(String expected, Duration timeout) throws Exception {
        long deadline = nanoTime.getAsLong() + timeout.toNanos();
        String latest;
        do {
            latest = dump();
            if (hasText(latest, expected)) return latest;
            pause.sleep(300);
        } while (nanoTime.getAsLong() < deadline);
        throw new IllegalStateException(
                "UI did not show text '" + expected + "' within " + timeout);
    }

    public String awaitDescriptionContaining(String fragment, Duration timeout) throws Exception {
        long deadline = nanoTime.getAsLong() + timeout.toNanos();
        String latest;
        do {
            latest = dump();
            if (hasDescriptionContaining(latest, fragment)) return latest;
            pause.sleep(250);
        } while (nanoTime.getAsLong() < deadline);
        throw new IllegalStateException(
                "UI did not expose description containing '"
                        + fragment
                        + "' within "
                        + timeout
                        + "; descriptions="
                        + descriptions(latest));
    }

    public String awaitContains(String expected, Duration timeout) throws Exception {
        long deadline = nanoTime.getAsLong() + timeout.toNanos();
        String latest;
        do {
            latest = dump();
            if (latest.contains(expected)) return latest;
            pause.sleep(300);
        } while (nanoTime.getAsLong() < deadline);
        throw new IllegalStateException(
                "UI hierarchy did not contain '" + expected + "' within " + timeout);
    }

    String awaitAbsent(String absent, Duration timeout) throws Exception {
        long deadline = nanoTime.getAsLong() + timeout.toNanos();
        String latest;
        do {
            latest = dump();
            if (!hasText(latest, absent)) return latest;
            pause.sleep(300);
        } while (nanoTime.getAsLong() < deadline);
        throw new IllegalStateException("UI still shows text '" + absent + "' after " + timeout);
    }

    public void back() throws Exception {
        long started = nanoTime.getAsLong();
        try {
            adb.run("shell", "input", "keyevent", "4").requireSuccess();
        } catch (Exception failure) {
            record(
                    "Press Back",
                    "Android navigation",
                    "Current overlay or screen closes",
                    failure.toString(),
                    started,
                    AtfReportStatus.FAIL);
            throw failure;
        }
        record(
                "Press Back",
                "Android navigation",
                "Current overlay or screen closes",
                "ADB accepted keyevent 4",
                started,
                AtfReportStatus.PASS);
        pause.sleep(350);
    }

    String restartToMainMenu(String packageName) throws Exception {
        adb.run("shell", "am", "force-stop", packageName).requireSuccess();
        IllegalStateException lastFailure = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            adb.run("shell", "am", "start", "-W", "-n", packageName + "/.Whist").requireSuccess();
            if (isKeyboardShown()) back();
            try {
                return awaitMainMenu(Duration.ofSeconds(5));
            } catch (IllegalStateException menuNotReady) {
                lastFailure = menuNotReady;
                String current = dump();
                if (current.contains("package=\"" + packageName + "\"")) {
                    back();
                    try {
                        return awaitMainMenu(Duration.ofSeconds(15));
                    } catch (IllegalStateException restoredMenuNotReady) {
                        lastFailure = restoredMenuNotReady;
                    }
                }
            }
        }
        throw new IllegalStateException(
                "Android app did not reach its main menu after relaunch: " + packageName,
                lastFailure);
    }

    public String restartToActivity(String packageName, String activity, String... intentArguments)
            throws Exception {
        String target = packageName + "/" + activity;
        long stopStarted = nanoTime.getAsLong();
        try {
            adb.run("shell", "am", "force-stop", packageName).requireSuccess();
            record(
                    "Force-stop Android app",
                    packageName,
                    "The previous app process is stopped",
                    "ADB accepted force-stop",
                    stopStarted,
                    AtfReportStatus.PASS);
        } catch (Exception failure) {
            record(
                    "Force-stop Android app",
                    packageName,
                    "The previous app process is stopped",
                    failure.toString(),
                    stopStarted,
                    AtfReportStatus.FAIL);
            throw failure;
        }

        long launchStarted = nanoTime.getAsLong();
        try {
            List<String> command =
                    new ArrayList<>(List.of("shell", "am", "start", "-W", "-n", target));
            command.addAll(List.of(intentArguments));
            adb.run(command.toArray(String[]::new)).requireSuccess();
            record(
                    "Launch Android activity",
                    target,
                    "The requested app activity opens",
                    "ADB accepted activity start",
                    launchStarted,
                    AtfReportStatus.PASS);
        } catch (Exception failure) {
            record(
                    "Launch Android activity",
                    target,
                    "The requested app activity opens",
                    failure.toString(),
                    launchStarted,
                    AtfReportStatus.FAIL);
            throw failure;
        }
        return dump();
    }

    private String awaitMainMenu(Duration timeout) throws Exception {
        long deadline = nanoTime.getAsLong() + timeout.toNanos();
        String latest;
        do {
            latest = dump();
            if (hasText(latest, "New Game") && hasText(latest, "Resume Game")) {
                if (hasText(latest, "CANCEL")) {
                    tapText("CANCEL");
                    continue;
                }
                if (hasText(latest, "START") || hasClass(latest, "android.widget.EditText")) {
                    back();
                    continue;
                }
                return latest;
            }
            pause.sleep(300);
        } while (nanoTime.getAsLong() < deadline);
        throw new IllegalStateException(
                "Android app did not reach an unobstructed main menu within "
                        + timeout
                        + "; visible texts="
                        + texts(latest));
    }

    private boolean isKeyboardShown() throws Exception {
        String state = adb.run("shell", "dumpsys", "input_method").output();
        return state.contains("mInputShown=true") || state.contains("mShowRequested=true");
    }

    void ensureMainMenu() throws Exception {
        String xml = dump();
        if (hasText(xml, "New Game")
                && hasText(xml, "Resume Game")
                && !hasText(xml, "CANCEL")
                && !hasText(xml, "START")
                && !hasClass(xml, "android.widget.EditText")) return;
        if (hasText(xml, "CANCEL")) tapText("CANCEL");
        else back();
        awaitMainMenu(Duration.ofSeconds(15));
    }

    public List<Path> capture(String label) throws Exception {
        return captures.capture(label, captureScopes);
    }

    Path captureImage(String label) throws Exception {
        return captures.captureImage(label, captureScopes);
    }

    private void tap(Node node) throws Exception {
        Matcher matcher = UiHierarchy.matchBounds(node.bounds());
        if (!matcher.matches())
            throw new IllegalStateException("Invalid UI bounds: " + node.bounds());
        int x = (Integer.parseInt(matcher.group(1)) + Integer.parseInt(matcher.group(3))) / 2;
        int y = (Integer.parseInt(matcher.group(2)) + Integer.parseInt(matcher.group(4))) / 2;
        String target =
                !node.description().isBlank()
                        ? node.description()
                        : !node.text().isBlank()
                                ? node.text()
                                : node.className() + " " + node.bounds();
        String operation =
                node.className().equals("android.widget.CheckedTextView") ? "Select option" : "Tap";
        String expected =
                operation.equals("Select option")
                        ? "Dispatch a tap to the selected option at its current accessibility bounds"
                        : "Dispatch a tap to the resolved target at its current accessibility bounds";
        long started = nanoTime.getAsLong();
        try {
            adb.run("shell", "input", "tap", Integer.toString(x), Integer.toString(y))
                    .requireSuccess();
        } catch (Exception failure) {
            record(operation, target, expected, failure.toString(), started, AtfReportStatus.FAIL);
            throw failure;
        }
        record(
                operation,
                target,
                expected,
                "ADB accepted tap at " + x + "," + y,
                started,
                AtfReportStatus.PASS);
        pause.sleep(500);
    }

    private void record(
            String operation,
            String target,
            String expected,
            String observed,
            long started,
            AtfReportStatus status) {
        recordAction(
                operation,
                target,
                expected,
                observed,
                Duration.ofNanos(nanoTime.getAsLong() - started).toMillis(),
                status);
    }

    public void recordAction(
            String operation,
            String target,
            String expected,
            String observed,
            long durationMillis,
            AtfReportStatus status) {
        actionSink.accept(
                new AtfUiActionRecord(
                        ++actionSequence,
                        operation,
                        target,
                        expected,
                        observed,
                        durationMillis,
                        status,
                        AtfUiEvidenceSource.ACTION));
    }

    public boolean turnedTrumpIdentityMatchesVisibleCard(String xml) throws Exception {
        return UiHierarchy.turnedTrumpIdentityMatchesVisibleCard(xml);
    }

    public Bounds buttonBounds(String xml, String text) throws Exception {
        return UiHierarchy.buttonBounds(xml, text);
    }

    public List<Bounds> handCardFaceBounds(String xml) throws Exception {
        return UiHierarchy.handCardFaceBounds(xml);
    }

    public record ElementSize(int widthPixels, int heightPixels) {}

    public record CardLabel(String text, int top) {}

    public record Bounds(int left, int top, int right, int bottom) {}
}
