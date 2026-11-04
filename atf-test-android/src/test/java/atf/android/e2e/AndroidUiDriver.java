package atf.android.e2e;

import atf.android.AdbClient;
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
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Small UI Automator bridge; resolves visible labels and bounds from each fresh hierarchy. */
final class AndroidUiDriver {
    private static final String REMOTE_XML = "/sdcard/atf-j4-window.xml";
    private static final Pattern BOUNDS = Pattern.compile("\\[(\\d+),(\\d+)]\\[(\\d+),(\\d+)]");
    private static final Pattern OVERRIDE_SIZE = Pattern.compile("(?m)^Override size:\\s*(\\d+)x(\\d+)");
    private static final Pattern PHYSICAL_SIZE = Pattern.compile("(?m)^Physical size:\\s*(\\d+)x(\\d+)");
    private static final Pattern PLAYING_CARD = Pattern.compile("(?:[2-9]|10|J|Q|K|A)[♠♥♣♦]");
    interface Device {
        AdbClient.Result run(String... arguments) throws IOException, InterruptedException;
        AdbClient.BinaryResult runBinary(String... arguments) throws IOException, InterruptedException;
    }
    private final Device adb;
    private final Path output;
    private Consumer<AtfUiActionRecord> actionSink = ignored -> { };
    private int actionSequence;

    AndroidUiDriver(AdbClient adb, Path output) throws Exception {
        this(new Device() {
            public AdbClient.Result run(String... arguments) throws IOException, InterruptedException { return adb.run(arguments); }
            public AdbClient.BinaryResult runBinary(String... arguments) throws IOException, InterruptedException { return adb.runBinary(arguments); }
        }, output);
    }

    AndroidUiDriver(Device adb, Path output) throws Exception {
        this.adb = Objects.requireNonNull(adb);
        this.output = output.toAbsolutePath().normalize();
        Files.createDirectories(this.output);
    }

    void recordActionsAs(Consumer<AtfUiActionRecord> sink) {
        actionSink = Objects.requireNonNull(sink, "sink");
    }

    String dump() throws Exception {
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
        throw new IllegalStateException("UI Automator did not produce a hierarchy: " + local, lastFailure);
    }

    boolean hasText(String xml, String exactText) throws Exception {
        return nodes(xml).stream().anyMatch(node -> node.text().equals(exactText));
    }

    boolean hasTextContaining(String xml, String fragment) throws Exception {
        return nodes(xml).stream().anyMatch(node -> node.text().contains(fragment));
    }

    boolean hasDescription(String xml, String exactDescription) throws Exception {
        return nodes(xml).stream().anyMatch(node -> node.description().equals(exactDescription));
    }

    boolean hasDescriptionContaining(String xml, String fragment) throws Exception {
        return nodes(xml).stream().anyMatch(node -> node.description().contains(fragment));
    }

    List<String> descriptions(String xml) throws Exception {
        return nodes(xml).stream().map(Node::description).filter(value -> !value.isBlank()).toList();
    }

    Bounds boundsForText(String xml, String exactText) throws Exception {
        List<Node> matches = nodes(xml).stream().filter(node -> node.text().equals(exactText)).toList();
        if (matches.size() != 1) throw new IllegalStateException("Expected one UI element named '"
                + exactText + "', got " + matches.size());
        return toBounds(matches.getFirst().bounds());
    }

    Bounds boundsForDescription(String xml, String exactDescription) throws Exception {
        List<Node> matches = nodes(xml).stream()
                .filter(node -> node.description().equals(exactDescription)).toList();
        List<String> distinctBounds = matches.stream().map(Node::bounds).distinct().toList();
        if (distinctBounds.size() != 1) throw new IllegalStateException("Expected one UI region described as '"
                + exactDescription + "', got " + distinctBounds.size() + " distinct bounds across "
                + matches.size() + " matching nodes");
        return toBounds(distinctBounds.getFirst());
    }

    Bounds boundsForDescription(String xml, String exactDescription, String exactClassName) throws Exception {
        List<Node> matches = nodes(xml).stream()
                .filter(node -> node.description().equals(exactDescription))
                .filter(node -> node.className().equals(exactClassName)).toList();
        if (matches.size() != 1) throw new IllegalStateException("Expected one UI element described as '"
                + exactDescription + "' with class " + exactClassName + ", got " + matches.size());
        return toBounds(matches.getFirst().bounds());
    }

    String spinnerValue(String xml, String exactDescription) throws Exception {
        List<Node> controls = nodes(xml).stream()
                .filter(node -> node.className().equals("android.widget.Spinner"))
                .filter(node -> node.description().equals(exactDescription)).toList();
        if (controls.size() != 1) {
            throw new IllegalStateException("Expected one spinner described as '" + exactDescription
                    + "', got " + controls.size());
        }
        Bounds control = toBounds(controls.getFirst().bounds());
        List<String> values = nodes(xml).stream()
                .filter(node -> !node.text().isBlank())
                .filter(node -> !node.className().equals("android.widget.Spinner"))
                .filter(node -> {
                    Bounds value = toBounds(node.bounds());
                    return value.left() >= control.left() && value.top() >= control.top()
                            && value.right() <= control.right() && value.bottom() <= control.bottom();
                })
                .map(Node::text).distinct().toList();
        if (values.size() != 1) {
            throw new IllegalStateException("Expected one visible value inside spinner '" + exactDescription
                    + "', got " + values);
        }
        return values.getFirst();
    }

    Bounds boundsForDescriptionContaining(String xml, String fragment) throws Exception {
        List<Node> matches = nodes(xml).stream()
                .filter(node -> node.description().contains(fragment)).toList();
        if (matches.size() != 1) throw new IllegalStateException("Expected one UI element described with '"
                + fragment + "', got " + matches.size());
        return toBounds(matches.getFirst().bounds());
    }

    String firstLegalCardDescription(String xml) throws Exception {
        return nodes(xml).stream().filter(Node::clickable)
                .filter(node -> node.description().contains("Legal card"))
                .map(Node::description).findFirst()
                .orElseThrow(() -> new IllegalStateException("No clickable legal card is exposed"));
    }

    List<String> legalCardDescriptions(String xml) throws Exception {
        return nodes(xml).stream().filter(Node::clickable)
                .filter(node -> node.description().contains("Legal card"))
                .map(Node::description).distinct().toList();
    }

    boolean hasClass(String xml, String exactClass) throws Exception {
        return nodes(xml).stream().anyMatch(node -> node.className().equals(exactClass));
    }

    boolean allContentFitsViewport(String xml) throws Exception {
        List<Node> elements = nodes(xml);
        Node viewport = elements.stream().filter(node -> node.className().equals("android.widget.ScrollView"))
                .findFirst().orElseThrow(() -> new IllegalStateException("Screen has no scroll viewport"));
        int[] viewportBounds = bounds(viewport.bounds());
        return elements.stream()
                .filter(node -> !node.text().isBlank() || !node.description().isBlank() || node.clickable())
                .map(node -> bounds(node.bounds()))
                .allMatch(element -> element[0] >= viewportBounds[0] && element[1] >= viewportBounds[1]
                        && element[2] <= viewportBounds[2] && element[3] <= viewportBounds[3]);
    }

    boolean isEnabled(String xml, String exactText) throws Exception {
        List<Node> matches = nodes(xml).stream().filter(node -> node.text().equals(exactText)).toList();
        if (matches.size() != 1) {
            throw new IllegalStateException("Expected one UI element named '" + exactText
                    + "', got " + matches.size());
        }
        return matches.getFirst().enabled();
    }

    boolean legalCardIsClickable(String xml, String cardLabel) throws Exception {
        String accessibleName = accessibleCardName(cardLabel);
        List<Node> cards = nodes(xml).stream()
                .filter(node -> node.className().equals("android.widget.FrameLayout"))
                .filter(node -> node.description().startsWith(accessibleName + ". "))
                .filter(node -> node.description().endsWith("Legal card")
                        || node.description().endsWith("Not legal now"))
                .toList();
        if (cards.size() != 1) {
            throw new IllegalStateException("Expected one accessible card slot for '" + cardLabel
                    + "', got " + cards.size());
        }
        Node card = cards.getFirst();
        boolean markedLegal = card.description().endsWith("Legal card");
        if (card.clickable() != markedLegal) {
            throw new IllegalStateException("Card accessibility state disagrees with clickability for "
                    + cardLabel + ": " + card.description() + ", clickable=" + card.clickable());
        }
        return card.clickable();
    }

    private static String accessibleCardName(String cardLabel) {
        if (cardLabel.length() < 2) throw new IllegalArgumentException("Invalid card label: " + cardLabel);
        String rank = cardLabel.substring(0, cardLabel.length() - 1);
        String rankName = switch (rank) {
            case "J" -> "Jack";
            case "Q" -> "Queen";
            case "K" -> "King";
            case "A" -> "Ace";
            default -> rank;
        };
        String suit = switch (cardLabel.substring(cardLabel.length() - 1)) {
            case "♠" -> "spades";
            case "♥" -> "hearts";
            case "♦" -> "diamonds";
            case "♣" -> "clubs";
            default -> throw new IllegalArgumentException("Invalid card label: " + cardLabel);
        };
        return rankName + " of " + suit;
    }

    boolean isSelected(String xml, String exactText) throws Exception {
        List<Node> matches = nodes(xml).stream().filter(node -> node.className().equals("android.widget.Button") && node.text().equals(exactText)).toList();
        if (matches.size() != 1) {
            throw new IllegalStateException("Expected one UI element named '" + exactText
                    + "', got " + matches.size());
        }
        return matches.getFirst().selected();
    }

    List<String> disabledButtonTexts(String xml) throws Exception {
        return nodes(xml).stream()
                .filter(node -> node.className().equals("android.widget.Button"))
                .filter(node -> !node.enabled())
                .map(Node::text)
                .filter(text -> !text.isBlank())
                .toList();
    }

    List<String> texts(String xml) throws Exception {
        return nodes(xml).stream().map(Node::text).filter(text -> !text.isBlank()).toList();
    }

    void tapText(String exactText) throws Exception {
        String xml = awaitText(exactText, Duration.ofSeconds(8));
        tapTextIn(xml, exactText);
    }

    void tapTextIn(String xml, String exactText) throws Exception {
        List<Node> matches = nodes(xml).stream().filter(node -> node.text().equals(exactText)).toList();
        if (matches.size() != 1) throw new IllegalStateException("Expected one UI element named '" + exactText + "', got " + matches.size());
        tap(matches.getFirst());
    }

    void tapDescription(String exactDescription) throws Exception {
        List<Node> matches = nodes(dump()).stream()
                .filter(node -> node.description().equals(exactDescription)).toList();
        if (matches.size() != 1) throw new IllegalStateException("Expected one UI element described as '"
                + exactDescription + "', got " + matches.size());
        tap(matches.getFirst());
    }

    List<CardLabel> bottomRowCardLabels(String xml) throws Exception {
        List<CardLabel> cards = nodes(xml).stream()
                .filter(node -> PLAYING_CARD.matcher(node.text()).matches())
                .map(node -> new CardLabel(node.text(), bounds(node.bounds())[1]))
                .toList();
        if (cards.isEmpty()) return List.of();
        int bottomRowTop = cards.stream().mapToInt(CardLabel::top).max().orElseThrow();
        return cards.stream().filter(card -> card.top() >= bottomRowTop - 15).toList();
    }

    List<Bounds> playingCardLabelBounds(String xml) throws Exception {
        return nodes(xml).stream()
                .filter(node -> PLAYING_CARD.matcher(node.text()).matches())
                .map(node -> toBounds(node.bounds())).toList();
    }

    List<ElementSize> numericButtonSizes(String xml) throws Exception {
        return nodes(xml).stream()
                .filter(node -> node.className().equals("android.widget.Button"))
                .filter(node -> node.text().matches("\\d+"))
                .map(node -> {
                    int[] value = bounds(node.bounds());
                    return new ElementSize(value[2] - value[0], value[3] - value[1]);
                }).toList();
    }

    ElementSize buttonSize(String xml, String text) throws Exception {
        List<Node> matches = nodes(xml).stream()
                .filter(node -> node.className().equals("android.widget.Button"))
                .filter(node -> node.text().equals(text)).toList();
        if (matches.size() != 1) {
            throw new IllegalStateException("Expected one button labeled '" + text + "', got " + matches.size());
        }
        int[] value = bounds(matches.getFirst().bounds());
        return new ElementSize(value[2] - value[0], value[3] - value[1]);
    }

    double buttonBottomGapFraction(String xml, Path screenshot, String text) throws Exception {
        List<Node> matches = nodes(xml).stream()
                .filter(node -> node.className().equals("android.widget.Button"))
                .filter(node -> node.text().equals(text)).toList();
        if (matches.size() != 1) {
            throw new IllegalStateException("Expected one button labeled '" + text + "', got " + matches.size());
        }
        var image = ImageIO.read(screenshot.toFile());
        if (image == null) throw new IllegalStateException("Cannot decode screenshot: " + screenshot);
        int bottom = bounds(matches.getFirst().bounds())[3];
        return (image.getHeight() - bottom) / (double) image.getHeight();
    }

    boolean sameBackgroundAcrossTableMargin(Path screenshot, double firstY, double secondY,
            String displaySize) throws Exception {
        var image = ImageIO.read(screenshot.toFile());
        if (image == null) throw new IllegalStateException("Cannot decode screenshot: " + screenshot);
        int[] viewport = displayDimensions(displaySize);
        // One percent stays beyond the oval stroke on both the narrow and wide viewport.
        int[] firstPoint = screenshotPoint(image, viewport[0] * 0.01, viewport[1] * firstY,
                viewport[0], viewport[1]);
        int[] secondPoint = screenshotPoint(image, viewport[0] * 0.01, viewport[1] * secondY,
                viewport[0], viewport[1]);
        int first = image.getRGB(firstPoint[0], firstPoint[1]);
        int second = image.getRGB(secondPoint[0], secondPoint[1]);
        return first == second;
    }

    int sampleHandFace(Path screenshot, Bounds hand, String displaySize) throws IOException {
        var image = ImageIO.read(screenshot.toFile());
        if (image == null) throw new IOException("Cannot decode hand screenshot: " + screenshot);
        int[] viewport = displayDimensions(displaySize);
        double logicalX = (hand.left() + hand.right()) / 2.0;
        double logicalY = hand.bottom() - Math.max(8, (hand.bottom() - hand.top()) / 20);
        int[] point = screenshotPoint(image, logicalX, logicalY, viewport[0], viewport[1]);
        return image.getRGB(point[0], point[1]) & 0xFFFFFF;
    }

    static int[] displayDimensions(String displaySize) throws IOException {
        Matcher size = OVERRIDE_SIZE.matcher(displaySize);
        if (!size.find()) {
            size = PHYSICAL_SIZE.matcher(displaySize);
            if (!size.find()) throw new IOException("Android display size is unavailable: " + displaySize);
        }
        int width = Integer.parseInt(size.group(1));
        int height = Integer.parseInt(size.group(2));
        if (width <= 0 || height <= 0) throw new IOException("Android reported invalid display size: " + displaySize);
        return new int[]{width, height};
    }

    private static int[] screenshotPoint(java.awt.image.BufferedImage image, double logicalX, double logicalY,
            int viewportWidth, int viewportHeight) {
        double scale = Math.min(image.getWidth() / (double) viewportWidth,
                image.getHeight() / (double) viewportHeight);
        double offsetX = (image.getWidth() - viewportWidth * scale) / 2.0;
        double offsetY = (image.getHeight() - viewportHeight * scale) / 2.0;
        int x = Math.max(0, Math.min(image.getWidth() - 1, (int) Math.round(offsetX + logicalX * scale)));
        int y = Math.max(0, Math.min(image.getHeight() - 1, (int) Math.round(offsetY + logicalY * scale)));
        return new int[]{x, y};
    }

    List<Integer> numericButtonValues(String xml) throws Exception {
        return nodes(xml).stream()
                .filter(node -> node.className().equals("android.widget.Button"))
                .filter(node -> node.text().matches("\\d+"))
                .map(node -> Integer.parseInt(node.text()))
                .toList();
    }

    private static int[] bounds(String value) {
        Matcher matcher = BOUNDS.matcher(value);
        if (!matcher.matches()) throw new IllegalArgumentException("Invalid UI bounds: " + value);
        return new int[]{Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3)), Integer.parseInt(matcher.group(4))};
    }

    private static Bounds toBounds(String value) {
        int[] parsed = bounds(value);
        return new Bounds(parsed[0], parsed[1], parsed[2], parsed[3]);
    }

    void tapSpinner(int index) throws Exception {
        List<Node> spinners = nodes(dump()).stream().filter(node -> node.className().equals("android.widget.Spinner") && node.clickable()).toList();
        if (index < 0 || index >= spinners.size()) throw new IllegalStateException("Spinner " + index + " not found; count=" + spinners.size());
        tap(spinners.get(index));
    }

    void selectSpinnerOption(int index, String expectedText) throws Exception {
        tapSpinner(index);
        selectOpenedSpinnerOption(expectedText);
    }

    void selectBid(int bid) throws Exception {
        String text = Integer.toString(bid);
        var matches = nodes(dump()).stream().filter(node -> node.className().equals("android.widget.Button") && node.text().equals(text)).toList();
        if (matches.size() != 1) throw new IllegalStateException("Expected one bid button: " + text);
        tap(matches.getFirst());
    }

    void selectNamedSpinnerOption(String description,String expectedText)throws Exception {
        reveal(node->node.className().equals("android.widget.Spinner")
                && matchesSpinnerLabel(description,node.description()));
        var matches=nodes(dump()).stream().filter(node->node.className().equals("android.widget.Spinner")
                && matchesSpinnerLabel(description,node.description())).toList();
        if(matches.size()!=1)throw new IllegalStateException("Expected one spinner: "+description);
        tap(matches.getFirst());selectOpenedSpinnerOption(expectedText);
    }

    private static boolean matchesSpinnerLabel(String selector,String label) {
        return switch(selector) {
            case "interface-language" -> List.of("Language", "Limba").contains(label);
            case "player-count" -> List.of("Players","Jucători").contains(label);
            case "game-type" -> List.of("Game type","Varianta de joc").contains(label);
            case "ai-level" -> List.of("AI level","Nivelul adversarilor").contains(label);
            case "romanian-streak-value" -> List.of("Five-hand exact-contract bonus",
                    "Bonus pentru cinci contracte exacte eligibile").contains(label);
            case "bid-options" -> List.of("Bid value","Valoarea licitației").contains(label);
            default -> selector.equals(label);
        };
    }

    void revealText(String expected)throws Exception {reveal(node->node.text().equals(expected));}

    private void reveal(java.util.function.Predicate<Node> target)throws Exception {
        for(int attempt=0;attempt<8;attempt++){
            var current=nodes(dump());if(current.stream().anyMatch(target))return;
            int width=0,height=0;for(Node node:current){var match=BOUNDS.matcher(node.bounds());if(match.matches()){width=Math.max(width,Integer.parseInt(match.group(3)));height=Math.max(height,Integer.parseInt(match.group(4)));}}
            if(width==0||height==0)throw new IllegalStateException("No visible scroll bounds");
            int startX=width/2,startY=height*4/5,endX=width/2,endY=height/3;
            long started=System.nanoTime();
            try {
                adb.run("shell","input","swipe",""+startX,""+startY,""+endX,""+endY,"350").requireSuccess();
            } catch(Exception failure) {
                record("Swipe", "Android setup viewport", "Reveal the configured setup control",
                        failure.toString(),started,AtfReportStatus.FAIL);
                throw failure;
            }
            record("Swipe", "Android setup viewport", "Reveal the configured setup control",
                    "ADB accepted swipe "+startX+","+startY+" → "+endX+","+endY,
                    started,AtfReportStatus.PASS);
            Thread.sleep(250);
        }
        throw new IllegalStateException("Setup control not found after scrolling");
    }

    private void selectOpenedSpinnerOption(String expectedText)throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(8).toNanos();
        String latest;
        do {
            latest = dump();
            List<Node> options = nodes(latest).stream()
                    .filter(node -> node.className().equals("android.widget.CheckedTextView"))
                    .filter(node -> node.text().equals(expectedText)).toList();
            if (options.size() == 1) {
                tap(options.get(0));
                return;
            }
            Thread.sleep(250);
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException("Spinner option '" + expectedText
                + "' did not appear exactly once; visible texts=" + texts(latest));
    }

    String awaitSpinnerSelection(String expected, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        String latest;
        do {
            latest = dump();
            if (hasText(latest, "START") && hasText(latest, expected)
                    && !hasClass(latest, "android.widget.CheckedTextView")) return latest;
            Thread.sleep(250);
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException("Spinner did not settle on '" + expected
                + "' with the setup dialog still open; visible texts=" + texts(latest));
    }

    String awaitText(String expected, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        String latest;
        do {
            latest = dump();
            if (hasText(latest, expected)) return latest;
            Thread.sleep(300);
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException("UI did not show text '" + expected + "' within " + timeout);
    }

    String awaitDescriptionContaining(String fragment, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        String latest;
        do {
            latest = dump();
            if (hasDescriptionContaining(latest, fragment)) return latest;
            Thread.sleep(250);
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException("UI did not expose description containing '" + fragment
                + "' within " + timeout + "; descriptions=" + descriptions(latest));
    }

    String awaitContains(String expected, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        String latest;
        do {
            latest = dump();
            if (latest.contains(expected)) return latest;
            Thread.sleep(300);
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException("UI hierarchy did not contain '" + expected + "' within " + timeout);
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

    void back() throws Exception {
        long started = System.nanoTime();
        try {
            adb.run("shell", "input", "keyevent", "4").requireSuccess();
        } catch (Exception failure) {
            record("Press Back", "Android navigation", "Current overlay or screen closes",
                    failure.toString(), started, AtfReportStatus.FAIL);
            throw failure;
        }
        record("Press Back", "Android navigation", "Current overlay or screen closes",
                "ADB accepted keyevent 4", started, AtfReportStatus.PASS);
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
        throw new IllegalStateException("Android app did not reach its main menu after relaunch: " + packageName,
                lastFailure);
    }

    String restartToActivity(String packageName, String activity, String... intentArguments) throws Exception {
        String target = packageName + "/" + activity;
        long stopStarted = System.nanoTime();
        try {
            adb.run("shell", "am", "force-stop", packageName).requireSuccess();
            record("Force-stop Android app", packageName, "The previous app process is stopped",
                    "ADB accepted force-stop", stopStarted, AtfReportStatus.PASS);
        } catch (Exception failure) {
            record("Force-stop Android app", packageName, "The previous app process is stopped",
                    failure.toString(), stopStarted, AtfReportStatus.FAIL);
            throw failure;
        }

        long launchStarted = System.nanoTime();
        try {
            List<String> command = new ArrayList<>(List.of("shell", "am", "start", "-W", "-n", target));
            command.addAll(List.of(intentArguments));
            adb.run(command.toArray(String[]::new)).requireSuccess();
            record("Launch Android activity", target,
                    "The requested app activity opens", "ADB accepted activity start",
                    launchStarted, AtfReportStatus.PASS);
        } catch (Exception failure) {
            record("Launch Android activity", target,
                    "The requested app activity opens", failure.toString(), launchStarted, AtfReportStatus.FAIL);
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
        throw new IllegalStateException("Android app did not reach an unobstructed main menu within "
                + timeout + "; visible texts=" + texts(latest));
    }

    private boolean isKeyboardShown() throws Exception {
        String state = adb.run("shell", "dumpsys", "input_method").output();
        return state.contains("mInputShown=true") || state.contains("mShowRequested=true");
    }

    void ensureMainMenu() throws Exception {
        String xml = dump();
        if (hasText(xml, "New Game") && hasText(xml, "Resume Game")
                && !hasText(xml, "CANCEL") && !hasText(xml, "START")
                && !hasClass(xml, "android.widget.EditText")) return;
        if (hasText(xml, "CANCEL")) tapText("CANCEL");
        else back();
        awaitMainMenu(Duration.ofSeconds(15));
    }

    List<Path> capture(String label) throws Exception {
        String xml = dump();
        Path xmlFile = output.resolve(label + ".xml");
        Path imageFile = output.resolve(label + ".png");
        Files.writeString(xmlFile, xml);
        capturePng(imageFile);
        return List.of(xmlFile, imageFile);
    }

    Path captureImage(String label) throws Exception {
        Path image = output.resolve(label + ".png");
        capturePng(image);
        return image;
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
                lastFailure = new IOException("Screenshot looks incomplete after ADB exec-out: " + imageFile);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw interrupted;
            } catch (IOException transferFailure) {
                lastFailure = transferFailure;
            }
            if (attempt < 2) Thread.sleep(250L * (attempt + 1));
        }
        throw lastFailure == null ? new IOException("Screenshot capture failed: " + imageFile) : lastFailure;
    }

    long countBrightNeutralPixels(Path image, double minVerticalFraction) throws Exception {
        var bitmap = ImageIO.read(image.toFile());
        if (bitmap == null) throw new IllegalStateException("Cannot decode screenshot: " + image);
        int firstRow = (int) (bitmap.getHeight() * minVerticalFraction);
        long count = 0;
        for (int y = firstRow; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                int rgb = bitmap.getRGB(x, y);
                int red = (rgb >>> 16) & 0xff;
                int green = (rgb >>> 8) & 0xff;
                int blue = rgb & 0xff;
                if (red >= 205 && green >= 205 && blue >= 205
                        && Math.max(red, Math.max(green, blue)) - Math.min(red, Math.min(green, blue)) <= 28) count++;
            }
        }
        return count;
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
                    if (visible.stream().anyMatch(label -> label.equalsIgnoreCase("Next Hand")
                            || label.equalsIgnoreCase("Play Again"))) {
                        return new CardPlayResult(List.copyOf(attempts), handChanged, tableChanged,
                                -1, -1, false, true, visible);
                    }
                }
                long started = System.nanoTime();
                adb.run("shell", "input", "tap", Integer.toString(x), Integer.toString(y)).requireSuccess();
                Thread.sleep(400);
                capturePng(probe);
                var firstFrame = ImageIO.read(probe.toFile());
                if (firstFrame == null) throw new IllegalStateException("Cannot decode screenshot: " + probe);
                handChanged = countChangedPixels(initial, firstFrame, 0.67, 0.94);
                tableChanged = countChangedPixels(initial, firstFrame, 0.20, 0.65);
                if (handChanged > 3_000 && tableChanged > 3_000) {
                    // Require a stable hand change and a matching table-area transition,
                    // so a card-selection highlight alone cannot count as a played card.
                    Thread.sleep(650);
                    capturePng(probe);
                    var stableFrame = ImageIO.read(probe.toFile());
                    if (stableFrame == null) throw new IllegalStateException("Cannot decode screenshot: " + probe);
                    handChanged = countChangedPixels(initial, stableFrame, 0.67, 0.94);
                    tableChanged = countChangedPixels(initial, stableFrame, 0.20, 0.65);
                    boolean accepted = handChanged > 3_000 && tableChanged > 3_000;
                    attempts.add(new CardTapAttempt(x, y, handChanged, tableChanged,
                            Duration.ofNanos(System.nanoTime() - started).toMillis(), accepted));
                    if (accepted) {
                        Files.copy(probe, after, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        Files.deleteIfExists(probe);
                        return new CardPlayResult(List.copyOf(attempts), handChanged, tableChanged,
                                x, y, true, false, List.of());
                    }
                } else {
                    attempts.add(new CardTapAttempt(x, y, handChanged, tableChanged,
                            Duration.ofNanos(System.nanoTime() - started).toMillis(), false));
                }
            }
        }
        Files.copy(probe, after, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(probe);
        return new CardPlayResult(List.copyOf(attempts), handChanged, tableChanged,
                -1, -1, false, false, List.of());
    }

    record CardTapAttempt(int x, int y, long handChangedPixels, long tableChangedPixels,
            long durationMillis, boolean accepted) { }

    record CardPlayResult(List<CardTapAttempt> attempts, long handChangedPixels,
            long tableChangedPixels, int acceptedX, int acceptedY, boolean accepted,
            boolean handBoundary, List<String> boundaryLabels) { }

    private long countChangedPixels(java.awt.image.BufferedImage left, java.awt.image.BufferedImage right,
            double firstRowFraction, double lastRowFraction) {
        if (left.getWidth() != right.getWidth() || left.getHeight() != right.getHeight())
            throw new IllegalStateException("Cannot compare gameplay screenshots with different dimensions");
        int width = left.getWidth(), height = left.getHeight();
        int firstRow = (int) (height * firstRowFraction);
        int lastRow = (int) (height * lastRowFraction);
        long count = 0;
        for (int y = firstRow; y < lastRow; y += 2) {
            for (int x = 0; x < width; x += 2) {
                int a = left.getRGB(x, y), b = right.getRGB(x, y);
                int difference = Math.abs(((a >>> 16) & 0xff) - ((b >>> 16) & 0xff))
                        + Math.abs(((a >>> 8) & 0xff) - ((b >>> 8) & 0xff))
                        + Math.abs((a & 0xff) - (b & 0xff));
                if (difference > 48) count++;
            }
        }
        return count;
    }

    private void tap(Node node) throws Exception {
        Matcher matcher = BOUNDS.matcher(node.bounds());
        if (!matcher.matches()) throw new IllegalStateException("Invalid UI bounds: " + node.bounds());
        int x = (Integer.parseInt(matcher.group(1)) + Integer.parseInt(matcher.group(3))) / 2;
        int y = (Integer.parseInt(matcher.group(2)) + Integer.parseInt(matcher.group(4))) / 2;
        String target = !node.description().isBlank() ? node.description()
                : !node.text().isBlank() ? node.text() : node.className() + " " + node.bounds();
        String operation = node.className().equals("android.widget.CheckedTextView") ? "Select option" : "Tap";
        String expected = operation.equals("Select option")
                ? "Dispatch a tap to the selected option at its current accessibility bounds"
                : "Dispatch a tap to the resolved target at its current accessibility bounds";
        long started = System.nanoTime();
        try {
            adb.run("shell", "input", "tap", Integer.toString(x), Integer.toString(y)).requireSuccess();
        } catch (Exception failure) {
            record(operation, target, expected, failure.toString(), started, AtfReportStatus.FAIL);
            throw failure;
        }
        record(operation, target, expected, "ADB accepted tap at " + x + "," + y,
                started, AtfReportStatus.PASS);
        Thread.sleep(500);
    }

    private void record(String operation, String target, String expected, String observed,
            long started, AtfReportStatus status) {
        recordAction(operation, target, expected, observed,
                Duration.ofNanos(System.nanoTime() - started).toMillis(), status);
    }

    void recordAction(String operation, String target, String expected, String observed,
            long durationMillis, AtfReportStatus status) {
        actionSink.accept(new AtfUiActionRecord(++actionSequence, operation, target, expected, observed,
                durationMillis, status, AtfUiEvidenceSource.ACTION));
    }

    private static List<Node> nodes(String xml) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        var document = factory.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        NodeList elements = document.getElementsByTagName("node");
        List<Node> result = new ArrayList<>(elements.getLength());
        for (int index = 0; index < elements.getLength(); index++) {
            Element element = (Element) elements.item(index);
            result.add(new Node(cardText(element.getAttribute("text")), element.getAttribute("class"),
                    element.getAttribute("bounds"), Boolean.parseBoolean(element.getAttribute("clickable")),
                    Boolean.parseBoolean(element.getAttribute("enabled")),
                    Boolean.parseBoolean(element.getAttribute("selected")), element.getAttribute("content-desc"),
                    element.getParentNode() instanceof Element parent ? parent.getAttribute("bounds") : ""));
        }
        return result;
    }

    private static String cardText(String text) {
        String compact = text.replace("\n", "").replace(" ", "");
        return PLAYING_CARD.matcher(compact).matches() ? compact : text;
    }

    boolean turnedTrumpIdentityMatchesVisibleCard(String xml) throws Exception {
        var entries = nodes(xml);
        var tiles = entries.stream().filter(n -> n.description().startsWith("Trump set by turned card: ")).toList();
        if (tiles.size() != 1) return false;
        var tile = tiles.getFirst();
        return entries.stream().filter(n -> PLAYING_CARD.matcher(n.text()).matches())
                .anyMatch(n -> n.parentBounds().equals(tile.bounds()) && tile.description().endsWith(accessibleCardName(n.text())));
    }

    Bounds buttonBounds(String xml, String text) throws Exception {
        var matches = nodes(xml).stream().filter(n -> n.className().equals("android.widget.Button") && n.text().equals(text)).toList();
        if (matches.size() != 1) throw new IllegalStateException("Expected one button: " + text);
        return toBounds(matches.getFirst().bounds());
    }

    List<Bounds> handCardFaceBounds(String xml) throws Exception {
        var labels = nodes(xml).stream().filter(node -> PLAYING_CARD.matcher(node.text()).matches()).toList();
        int bottom = labels.stream().mapToInt(node -> toBounds(node.bounds()).top()).max().orElse(0);
        return labels.stream().filter(node -> toBounds(node.bounds()).top() >= bottom - 15)
                .map(node -> toBounds(node.parentBounds())).toList();
    }
    private record Node(String text, String className, String bounds, boolean clickable,
            boolean enabled, boolean selected, String description, String parentBounds) { }

    record ElementSize(int widthPixels, int heightPixels) { }
    record CardLabel(String text, int top) { }
    record Bounds(int left, int top, int right, int bottom) { }
}
