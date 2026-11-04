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
import java.util.regex.Matcher;

import javax.imageio.ImageIO;

/** Small UI Automator bridge; resolves visible labels and bounds from each fresh hierarchy. */
public final class AndroidUiDriver {
    private static final String REMOTE_XML = "/sdcard/atf-j4-window.xml";

    public interface Device {
        AdbClient.Result run(String... arguments) throws IOException, InterruptedException;

        AdbClient.BinaryResult runBinary(String... arguments)
                throws IOException, InterruptedException;
    }

    private final Device adb;
    private final Path output;
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
        this.adb = Objects.requireNonNull(adb);
        this.output = output.toAbsolutePath().normalize();
        Files.createDirectories(this.output);
    }

    public void recordActionsAs(Consumer<AtfUiActionRecord> sink) {
        actionSink = Objects.requireNonNull(sink, "sink");
    }

    public void recordCapturesAs(java.util.function.Supplier<? extends AutoCloseable> scopes) {
        captureScopes = Objects.requireNonNull(scopes, "scopes");
    }

    public String dump() throws Exception {
        Path local = output.resolve("current-window.xml");
        IOException lastFailure = null;
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                // uiautomator can exit 0 after "null root node" without rewriting the file.
                // Never pull a hierarchy left behind by a previous screen.
                adb.run("shell", "rm", "-f", REMOTE_XML).requireSuccess();
                var dump = adb.run("shell", "uiautomator", "dump", REMOTE_XML);
                if (dump.succeeded() && dump.output().contains("dumped to:")) {
                    var pull = adb.run("pull", REMOTE_XML, local.toString());
                    if (pull.succeeded() && Files.isRegularFile(local)) {
                        String xml = Files.readString(local);
                        if (xml.contains("<hierarchy")) return xml;
                    }
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw interrupted;
            } catch (IOException transferFailure) {
                lastFailure = transferFailure;
            }
            Thread.sleep(350L * (attempt + 1));
        }
        throw new IllegalStateException(
                "UI Automator did not produce a hierarchy: " + local, lastFailure);
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
            long started = System.nanoTime();
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
            Thread.sleep(250);
        }
        throw new IllegalStateException("Setup control not found after scrolling");
    }

    private void selectOpenedSpinnerOption(String expectedText) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(8).toNanos();
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
            Thread.sleep(250);
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException(
                "Spinner option '"
                        + expectedText
                        + "' did not appear exactly once; visible texts="
                        + texts(latest));
    }

    String awaitSpinnerSelection(String expected, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        String latest;
        do {
            latest = dump();
            if (hasText(latest, "START")
                    && hasText(latest, expected)
                    && !hasClass(latest, "android.widget.CheckedTextView")) return latest;
            Thread.sleep(250);
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException(
                "Spinner did not settle on '"
                        + expected
                        + "' with the setup dialog still open; visible texts="
                        + texts(latest));
    }

    public String awaitText(String expected, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        String latest;
        do {
            latest = dump();
            if (hasText(latest, expected)) return latest;
            Thread.sleep(300);
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException(
                "UI did not show text '" + expected + "' within " + timeout);
    }

    public String awaitDescriptionContaining(String fragment, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        String latest;
        do {
            latest = dump();
            if (hasDescriptionContaining(latest, fragment)) return latest;
            Thread.sleep(250);
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException(
                "UI did not expose description containing '"
                        + fragment
                        + "' within "
                        + timeout
                        + "; descriptions="
                        + descriptions(latest));
    }

    public String awaitContains(String expected, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        String latest;
        do {
            latest = dump();
            if (latest.contains(expected)) return latest;
            Thread.sleep(300);
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException(
                "UI hierarchy did not contain '" + expected + "' within " + timeout);
    }

    String awaitAbsent(String absent, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        String latest;
        do {
            latest = dump();
            if (!hasText(latest, absent)) return latest;
            Thread.sleep(300);
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException("UI still shows text '" + absent + "' after " + timeout);
    }

    public void back() throws Exception {
        long started = System.nanoTime();
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
        Thread.sleep(350);
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
        long stopStarted = System.nanoTime();
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

        long launchStarted = System.nanoTime();
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
        long deadline = System.nanoTime() + timeout.toNanos();
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
            Thread.sleep(300);
        } while (System.nanoTime() < deadline);
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
        try (var capture = captureScopes.get()) {
            String xml = dump();
            Path xmlFile = output.resolve(label + ".xml");
            Path imageFile = output.resolve(label + ".png");
            Files.writeString(xmlFile, xml);
            capturePng(imageFile);
            return List.of(xmlFile, imageFile);
        }
    }

    Path captureImage(String label) throws Exception {
        try (var capture = captureScopes.get()) {
            Path image = output.resolve(label + ".png");
            capturePng(image);
            return image;
        }
    }

    private void capturePng(Path imageFile) throws Exception {
        IOException lastFailure = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                Files.deleteIfExists(imageFile);
                var capture = adb.runBinary("exec-out", "screencap", "-p");
                capture.requireSuccess();
                byte[] png = capture.output();
                if (png.length >= 10_000) {
                    Files.write(imageFile, png);
                    return;
                }
                lastFailure =
                        new IOException(
                                "Screenshot looks incomplete after ADB exec-out: " + imageFile);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw interrupted;
            } catch (IOException transferFailure) {
                lastFailure = transferFailure;
            }
            if (attempt < 2) Thread.sleep(250L * (attempt + 1));
        }
        throw lastFailure == null
                ? new IOException("Screenshot capture failed: " + imageFile)
                : lastFailure;
    }

    long countBrightNeutralPixels(Path image, double minVerticalFraction) throws Exception {
        return ScreenshotAnalysis.countBrightNeutralPixels(image, minVerticalFraction);
    }

    long tapUntilHandChanges(Path before, Path after) throws Exception {
        return tapUntilHandChangesDetailed(before, after).handChangedPixels();
    }

    CardPlayResult tapUntilHandChangesDetailed(Path before, Path after) throws Exception {
        var initial = ImageIO.read(before.toFile());
        if (initial == null) throw new IllegalStateException("Cannot decode screenshot: " + before);
        int width = initial.getWidth(), height = initial.getHeight();
        Path probe = output.resolve("card-probe.png");
        List<CardTapAttempt> attempts = new ArrayList<>();
        long handChanged = 0, tableChanged = 0;
        // Observed 4-player hand: ten cards laid out as five slots on each row.
        // The custom canvas exposes no accessible card nodes, so probe visible slot centers.
        for (int row = 0; row < 2; row++) {
            int y = (int) (height * (row == 0 ? 0.74 : 0.89));
            for (int card = 0; card < 5; card++) {
                int x = (int) (width * (card + 0.5) / 5.0);
                // The score-sheet Next Hand button occupies this calibrated slot.
                // Check the accessible hierarchy before probing it so a card search
                // can never advance to another hand and mislabel that as card play.
                if (row == 1 && card == 2) {
                    List<String> visible = texts(dump());
                    if (visible.stream()
                            .anyMatch(
                                    label ->
                                            label.equalsIgnoreCase("Next Hand")
                                                    || label.equalsIgnoreCase("Play Again"))) {
                        return new CardPlayResult(
                                List.copyOf(attempts),
                                handChanged,
                                tableChanged,
                                -1,
                                -1,
                                false,
                                true,
                                visible);
                    }
                }
                long started = System.nanoTime();
                adb.run("shell", "input", "tap", Integer.toString(x), Integer.toString(y))
                        .requireSuccess();
                Thread.sleep(400);
                capturePng(probe);
                var firstFrame = ImageIO.read(probe.toFile());
                if (firstFrame == null)
                    throw new IllegalStateException("Cannot decode screenshot: " + probe);
                handChanged = countChangedPixels(initial, firstFrame, 0.67, 0.94);
                tableChanged = countChangedPixels(initial, firstFrame, 0.20, 0.65);
                if (handChanged > 3_000 && tableChanged > 3_000) {
                    // Require a stable hand change and a matching table-area transition,
                    // so a card-selection highlight alone cannot count as a played card.
                    Thread.sleep(650);
                    capturePng(probe);
                    var stableFrame = ImageIO.read(probe.toFile());
                    if (stableFrame == null)
                        throw new IllegalStateException("Cannot decode screenshot: " + probe);
                    handChanged = countChangedPixels(initial, stableFrame, 0.67, 0.94);
                    tableChanged = countChangedPixels(initial, stableFrame, 0.20, 0.65);
                    boolean accepted = handChanged > 3_000 && tableChanged > 3_000;
                    attempts.add(
                            new CardTapAttempt(
                                    x,
                                    y,
                                    handChanged,
                                    tableChanged,
                                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                                    accepted));
                    if (accepted) {
                        Files.copy(probe, after, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        Files.deleteIfExists(probe);
                        return new CardPlayResult(
                                List.copyOf(attempts),
                                handChanged,
                                tableChanged,
                                x,
                                y,
                                true,
                                false,
                                List.of());
                    }
                } else {
                    attempts.add(
                            new CardTapAttempt(
                                    x,
                                    y,
                                    handChanged,
                                    tableChanged,
                                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                                    false));
                }
            }
        }
        Files.copy(probe, after, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(probe);
        return new CardPlayResult(
                List.copyOf(attempts), handChanged, tableChanged, -1, -1, false, false, List.of());
    }

    record CardTapAttempt(
            int x,
            int y,
            long handChangedPixels,
            long tableChangedPixels,
            long durationMillis,
            boolean accepted) {}

    record CardPlayResult(
            List<CardTapAttempt> attempts,
            long handChangedPixels,
            long tableChangedPixels,
            int acceptedX,
            int acceptedY,
            boolean accepted,
            boolean handBoundary,
            List<String> boundaryLabels) {}

    private long countChangedPixels(
            java.awt.image.BufferedImage left,
            java.awt.image.BufferedImage right,
            double firstRowFraction,
            double lastRowFraction) {
        return ScreenshotAnalysis.countChangedPixels(
                left, right, firstRowFraction, lastRowFraction);
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
        long started = System.nanoTime();
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
        Thread.sleep(500);
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
                Duration.ofNanos(System.nanoTime() - started).toMillis(),
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
