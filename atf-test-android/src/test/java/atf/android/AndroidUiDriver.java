package atf.android;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    private static final String REMOTE_PNG = "/sdcard/atf-j4-screen.png";
    private static final Pattern BOUNDS = Pattern.compile("\\[(\\d+),(\\d+)]\\[(\\d+),(\\d+)]");
    private final AdbClient adb;
    private final Path output;

    AndroidUiDriver(AdbClient adb, Path output) throws Exception {
        this.adb = Objects.requireNonNull(adb);
        this.output = output.toAbsolutePath().normalize();
        Files.createDirectories(this.output);
    }

    String dump() throws Exception {
        Path local = output.resolve("current-window.xml");
        for (int attempt = 0; attempt < 4; attempt++) {
            var dump = adb.run("shell", "uiautomator", "dump", REMOTE_XML);
            if (dump.succeeded()) {
                var pull = adb.run("pull", REMOTE_XML, local.toString());
                if (pull.succeeded() && Files.isRegularFile(local)) {
                    String xml = Files.readString(local);
                    if (xml.contains("<hierarchy")) return xml;
                }
            }
            Thread.sleep(350L * (attempt + 1));
        }
        throw new IllegalStateException("UI Automator did not produce a hierarchy: " + local);
    }

    boolean hasText(String xml, String exactText) throws Exception {
        return nodes(xml).stream().anyMatch(node -> node.text().equals(exactText));
    }

    boolean hasClass(String xml, String exactClass) throws Exception {
        return nodes(xml).stream().anyMatch(node -> node.className().equals(exactClass));
    }

    List<String> texts(String xml) throws Exception {
        return nodes(xml).stream().map(Node::text).filter(text -> !text.isBlank()).toList();
    }

    void tapText(String exactText) throws Exception {
        String xml = awaitText(exactText, Duration.ofSeconds(8));
        List<Node> matches = nodes(xml).stream().filter(node -> node.text().equals(exactText)).toList();
        if (matches.size() != 1) throw new IllegalStateException("Expected one UI element named '" + exactText + "', got " + matches.size());
        tap(matches.getFirst());
    }

    void tapSpinner(int index) throws Exception {
        List<Node> spinners = nodes(dump()).stream().filter(node -> node.className().equals("android.widget.Spinner") && node.clickable()).toList();
        if (index < 0 || index >= spinners.size()) throw new IllegalStateException("Spinner " + index + " not found; count=" + spinners.size());
        tap(spinners.get(index));
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
        adb.run("shell", "input", "keyevent", "4").requireSuccess();
        Thread.sleep(350);
    }

    String restartToMainMenu(String packageName) throws Exception {
        adb.run("shell", "am", "force-stop", packageName).requireSuccess();
        adb.run("shell", "am", "start", "-W", "-n", packageName + "/.Whist").requireSuccess();
        if (isKeyboardShown()) back();
        return awaitText("New Game", Duration.ofSeconds(15));
    }

    private boolean isKeyboardShown() throws Exception {
        String state = adb.run("shell", "dumpsys", "input_method").output();
        return state.contains("mInputShown=true") || state.contains("mShowRequested=true");
    }

    void ensureMainMenu() throws Exception {
        String xml = dump();
        if (hasText(xml, "New Game") && hasText(xml, "Resume Game")) return;
        back();
        awaitText("New Game", Duration.ofSeconds(15));
        awaitText("Resume Game", Duration.ofSeconds(5));
    }

    List<Path> capture(String label) throws Exception {
        String xml = dump();
        Path xmlFile = output.resolve(label + ".xml");
        Path imageFile = output.resolve(label + ".png");
        Files.writeString(xmlFile, xml);
        adb.run("shell", "screencap", "-p", REMOTE_PNG).requireSuccess();
        adb.run("pull", REMOTE_PNG, imageFile.toString()).requireSuccess();
        if (Files.size(imageFile) < 10_000) throw new IllegalStateException("Screenshot looks incomplete: " + imageFile);
        return List.of(xmlFile, imageFile);
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
        var initial = ImageIO.read(before.toFile());
        if (initial == null) throw new IllegalStateException("Cannot decode screenshot: " + before);
        int width = initial.getWidth(), height = initial.getHeight();
        Path probe = output.resolve("card-probe.png");
        long changed = 0;
        // At the tested 13-card hand size the app wraps six cards over seven cards.
        // Try each card center: off-suit cards are correctly ignored by the game.
        for (int row = 0; row < 2; row++) {
            int cards = row == 0 ? 6 : 7;
            int y = (int) (height * (row == 0 ? 0.74 : 0.89));
            for (int card = 0; card < cards; card++) {
                int x = (int) (width * (card + 0.5) / cards);
                adb.run("shell", "input", "tap", Integer.toString(x), Integer.toString(y)).requireSuccess();
                Thread.sleep(1_000);
                adb.run("shell", "screencap", "-p", REMOTE_PNG).requireSuccess();
                adb.run("pull", REMOTE_PNG, probe.toString()).requireSuccess();
                changed = countChangedPixels(before, probe, 0.67, 0.94);
                if (changed > 3_000) {
                    // Ignore animations or transient selection flashes; the hand must
                    // remain changed in a second settled screenshot.
                    Thread.sleep(1_500);
                    adb.run("shell", "screencap", "-p", REMOTE_PNG).requireSuccess();
                    adb.run("pull", REMOTE_PNG, probe.toString()).requireSuccess();
                    changed = countChangedPixels(before, probe, 0.67, 0.94);
                    if (changed > 3_000) {
                        Files.copy(probe, after, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        Files.deleteIfExists(probe);
                        return changed;
                    }
                }
            }
        }
        Files.copy(probe, after, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(probe);
        return changed;
    }

    private long countChangedPixels(Path before, Path after, double firstRowFraction, double lastRowFraction) throws Exception {
        var left = ImageIO.read(before.toFile());
        var right = ImageIO.read(after.toFile());
        if (left == null || right == null || left.getWidth() != right.getWidth() || left.getHeight() != right.getHeight())
            throw new IllegalStateException("Cannot compare gameplay screenshots: " + before + " and " + after);
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
        adb.run("shell", "input", "tap", Integer.toString(x), Integer.toString(y)).requireSuccess();
        Thread.sleep(500);
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
            result.add(new Node(element.getAttribute("text"), element.getAttribute("class"),
                    element.getAttribute("bounds"), Boolean.parseBoolean(element.getAttribute("clickable"))));
        }
        return result;
    }

    private record Node(String text, String className, String bounds, boolean clickable) { }
}
