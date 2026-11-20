package atf.tests.reporting.browser;

import atf.tests.governance.support.GovernanceProject;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.tools.ToolProvider;

/** Explicit synthetic publisher inputs; no game or browser execution is implied. */
final class BrowserReportFixture {
    static final String FIRST = "First recorded component";
    static final String LAST = "Last recorded component";
    static final String EXPECTED = "expected ".repeat(70) + "EXPECTED_FINAL_♠ · Mâna următoare";
    static final String ACTUAL = "observed ".repeat(70) + "OBSERVED_FINAL_♥";
    private BrowserReportFixture() { }

    static URLClassLoader compile(Path classes) throws Exception {
        Path source = GovernanceProject.locateProjectRoot().resolve("atf-ui-browser/tests/BrowserExtent.java");
        int result = ToolProvider.getSystemJavaCompiler().run(null, null, null, "-proc:none", "-encoding", "UTF-8",
                "-cp", System.getProperty("java.class.path"), "-d", classes.toString(), source.toString());
        if (result != 0) throw new IllegalStateException("Browser publisher compilation failed: " + result);
        return new URLClassLoader(new java.net.URL[]{classes.toUri().toURL()}, BrowserReportFixture.class.getClassLoader());
    }

    static void write(Path root) throws Exception {
        Files.createDirectories(root);
        png(root.resolve("first.png"), 0xffdd1111);
        png(root.resolve("second.png"), 0xff1155dd);
        png(root.resolve("last.png"), 0xff11aa11);
        var first = Map.of("id", "FIXTURE-FIRST", "name", FIRST, "status", "PASS", "expected", EXPECTED,
                "observed", EXPECTED, "images", List.of(image("first.png", "First captured frame"),
                image("second.png", "Second captured frame")));
        var last = Map.of("id", "FIXTURE-LAST", "name", LAST, "status", "FAIL", "expected", EXPECTED,
                "observed", ACTUAL, "layer", "android-component", "images", List.of(image("last.png", "Last captured frame")));
        new ObjectMapper().writeValue(root.resolve("browser-checks.json").toFile(), List.of(first, last));
    }

    static void publish(URLClassLoader loader, Path root) throws Exception {
        var method = Class.forName("BrowserExtent", true, loader).getDeclaredMethod("main", String[].class);
        method.setAccessible(true);
        try { method.invoke(null, (Object) new String[]{root.toString(), root.resolve("report.html").toString()}); }
        catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof Exception original) throw original;
            if (failure.getCause() instanceof Error original) throw original;
            throw failure;
        }
    }

    static void change(Path root, String field, String value) throws Exception {
        var json = new ObjectMapper();
        var cases = json.readTree(root.resolve("browser-checks.json").toFile());
        if (field.equals("image")) ((com.fasterxml.jackson.databind.node.ObjectNode) cases.get(0).get("images").get(0)).put("file", value);
        else ((com.fasterxml.jackson.databind.node.ObjectNode) cases.get(0)).put(field, value);
        json.writeValue(root.resolve("browser-checks.json").toFile(), cases);
    }

    static String payload(Path image) throws Exception { return Base64.getEncoder().encodeToString(Files.readAllBytes(image)); }
    private static Map<String, String> image(String file, String title) { return Map.of("file", file, "title", title); }
    private static void png(Path file, int color) throws Exception {
        var image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, color);
        try (var bytes = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", bytes); Files.write(file, bytes.toByteArray());
        }
    }
}
