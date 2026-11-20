package atf.tests.reporting.browser;

import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import atf.tests.reporting.GameReportAudit;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest
public class BrowserExtentPublicationComponentTest {
    @RegisterExtension static final GameReportAudit AUDIT = new GameReportAudit();
    private static URLClassLoader publisher;
    @TempDir Path root;
    @BeforeAll static void compilePublisher(@TempDir Path classes) throws Exception { publisher = BrowserReportFixture.compile(classes); }
    @AfterAll static void closePublisher() throws Exception { if (publisher != null) publisher.close(); }

    @Test void standardOverviewKeepsEnglishNarrativeAndScenarioOwnedCaptures(ComponentInputs inputs) throws Exception {
        BrowserReportFixture.write(root);
        inputs.add("Publisher fixture", "Two recorded synthetic cases: PASS and FAIL, three distinct PNGs, long original comparisons.");
        BrowserReportFixture.publish(publisher, root);
        String html = Files.readString(root.resolve("report.html"));
        Document doc = Jsoup.parse(html);
        Element overview = doc.selectFirst("#dashboard-view");
        assertNotNull(overview, "Standard Overview remains available");
        assertTrue(overview.children().first().hasClass("row"), "Overview begins with the native summary cards");
        assertTrue(overview.text().contains("Started"), "Native Overview keeps English summary captions");
        assertEquals(0, overview.select("#atf-ui-evidence, img, a[href^=data:image]").size(), "No custom capture gallery in Overview");
        assertFalse(overview.text().contains("First captured frame") || overview.text().contains("Last captured frame"),
                "Capture captions belong to their tests, independently of native Overview disclosures");
        assertFalse(html.contains("Playwright exploratory evidence"), "The global exploratory gallery is not injected");
        assertTrue(doc.text().contains("Browser components"), "Browser suite is named in English");
        assertTrue(doc.text().contains("Android components"), "Native consumer evidence has its own English suite");
        assertTrue(doc.text().contains("Recorded comparisons"), "Comparison phase is named in English");
        assertFalse(html.contains("Rezultatul verificării") || html.contains("Componente browser") || html.contains("Comparație cu Android"),
                "Publisher narration does not use the previous Romanian labels");
        Element first = scenario(doc, BrowserReportFixture.FIRST), last = scenario(doc, BrowserReportFixture.LAST);
        String a = BrowserReportFixture.payload(root.resolve("first.png")), b = BrowserReportFixture.payload(root.resolve("second.png")), c = BrowserReportFixture.payload(root.resolve("last.png"));
        assertTrue(first.outerHtml().contains(a) && first.outerHtml().contains(b), "Both frames stay in their original case");
        assertFalse(first.outerHtml().contains(c), "Another case's frame cannot be assigned to the first case");
        assertTrue(last.outerHtml().contains(c), "The final case retains its own frame");
        assertFalse(last.outerHtml().contains(a), "The final case does not inherit the first capture");
        assertTrue(last.text().contains("EXPECTED_FINAL_♠") && last.text().contains("OBSERVED_FINAL_♥"), "Long expected and observed values remain complete");
        assertTrue(last.text().contains("Mâna următoare"), "English report narration preserves original Romanian application evidence");
        assertTrue(last.outerHtml().contains("fail"), "A failed recorded comparison retains its outcome");
        assertEquals(0, doc.select("img[src]:not([src^=data:]), a[data-img]:not([data-img^=data:])").size(), "Captures need no external files");
    }

    @Test void missingCaptureIsRejectedBeforeReportCreation(ComponentInputs inputs) throws Exception {
        BrowserReportFixture.write(root); Files.delete(root.resolve("last.png"));
        inputs.add("Negative control", "The final case refers to a missing PNG after valid earlier cases.");
        assertThrows(java.io.IOException.class, () -> BrowserReportFixture.publish(publisher, root));
        assertFalse(Files.exists(root.resolve("report.html")), "Missing late evidence cannot leave an apparently qualified report");
    }

    @Test void externalCaptureAndInvalidPngCannotBePublished(ComponentInputs inputs) throws Exception {
        Path evidence = root.resolve("evidence"); BrowserReportFixture.write(evidence);
        Files.copy(evidence.resolve("first.png"), root.resolve("outside.png"));
        BrowserReportFixture.change(evidence, "image", "../outside.png");
        inputs.add("Negative controls", "An existing external PNG, then invalid PNG bytes inside the evidence directory.");
        assertThrows(IllegalArgumentException.class, () -> BrowserReportFixture.publish(publisher, evidence));
        assertFalse(Files.exists(evidence.resolve("report.html")), "External image fails preflight");
        BrowserReportFixture.change(evidence, "image", "first.png"); Files.writeString(evidence.resolve("first.png"), "not a PNG");
        assertThrows(IllegalArgumentException.class, () -> BrowserReportFixture.publish(publisher, evidence));
        assertFalse(Files.exists(evidence.resolve("report.html")), "Invalid image fails preflight");
    }

    @Test void invalidLayerAndImageShapeDoNotCreatePartialReports(ComponentInputs inputs) throws Exception {
        BrowserReportFixture.write(root); BrowserReportFixture.change(root, "layer", "unknown");
        inputs.add("Negative controls", "Unknown evidence ownership, then a malformed image inventory.");
        assertThrows(IllegalArgumentException.class, () -> BrowserReportFixture.publish(publisher, root));
        BrowserReportFixture.change(root, "layer", "component"); BrowserReportFixture.change(root, "images", "invalid");
        assertThrows(IllegalArgumentException.class, () -> BrowserReportFixture.publish(publisher, root));
        assertFalse(Files.exists(root.resolve("report.html")), "Malformed inventories cannot be partially published");
    }

    private static Element scenario(Document doc, String title) {
        Element node = doc.select("a.node").stream().filter(element -> element.text().equals(title)).findFirst().orElseThrow();
        while (!node.hasClass("card")) node = node.parent();
        return node;
    }
}
