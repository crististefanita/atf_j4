package atf.android.whist.evidence;

import atf.testkit.AtfComponentTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class WhistExtentFeedbackTest {
    @Test void refusesToOverwriteProjectDocumentation(@TempDir Path temp) throws Exception {
        Path report = Files.writeString(temp.resolve("report.html"), "report");
        for (String marker : new String[]{"git-checkout", "staged-project"}) {
            Path project = Files.createDirectories(temp.resolve(marker));
            if (marker.equals("git-checkout")) Files.writeString(project.resolve(".git"), "gitdir: elsewhere");
            else {
                Files.writeString(project.resolve("pom.xml"), "<project/>");
                Files.createDirectory(project.resolve("whist-lab-app"));
            }
            Path feedback = Files.createDirectories(project.resolve("docs/feedback"));
            Path readme = Files.createDirectories(feedback.resolve("whist-extent")).resolve("README.md");
            Files.writeString(readme, "Stable architectural documentation");
            assertThrows(java.io.IOException.class, () -> WhistExtentFeedback.publish(feedback, report, "Run", "PASS"));
            assertEquals("Stable architectural documentation", Files.readString(readme));
            assertThrows(java.io.IOException.class, () -> WhistExtentFeedback.publish(
                    feedback.resolve("new/nested/path"), report, "Run", "PASS"));
            assertFalse(Files.exists(feedback.resolve("new")));
        }
    }

    @Test @ResourceLock(Resources.SYSTEM_PROPERTIES)
    void defaultsToAnExternalIndexWithoutCheckoutDiscovery(@TempDir Path temp) throws Exception {
        Path runs = Files.createDirectory(temp.resolve("runs"));
        Path report = Files.writeString(runs.resolve("report.html"), "report");
        Path archive = temp.resolve("archive");
        var values = Map.of("atf.android.enabled", "true", "atf.android.artifacts", runs.toString(),
                "atf.android.handoffRoot", archive.toString(), "atf.android.feedbackDirectory", "");
        var previous = new HashMap<String, String>();
        values.forEach((name, value) -> previous.put(name, System.setProperty(name, value)));
        try {
            WhistExtentFeedback.afterAudit(report, "ro.whistlab.app", "External run", "PASS");
            Path index = archive.resolve("whist-lab/indexes/whist-extent/README.md");
            assertTrue(Files.readString(index).contains("External run"));
            assertFalse(Files.readString(index).contains("WHIST_LAB_User_Experience_Feedback.md"));
        } finally {
            previous.forEach((key, value) -> { if (value == null) System.clearProperty(key); else System.setProperty(key, value); });
        }
    }

    @Test void publishesTheLatestAuditedFailureWithAnEscapedLocalLink(@TempDir Path temp) throws Exception {
        Path feedback = temp.resolve("feedback");
        Path first = Files.writeString(temp.resolve("pilot.html"), "pilot");
        Path second = Files.writeString(temp.resolve("scor nou (RO).html"), "scor");
        WhistExtentFeedback.publish(feedback, first, "Pilot", "PASS");
        WhistExtentFeedback.publish(feedback, second, "Scor", "FAIL");
        String index = Files.readString(feedback.resolve("whist-extent/README.md"));
        assertTrue(index.contains("<../../scor%20nou%20%28RO%29.html>"));
        assertFalse(index.contains("[Deschide ultimul raport de execuție](<file:"));
        assertTrue(index.contains("## Pe scurt"));
        assertTrue(index.contains("Scor") && index.contains("FAIL"));
        assertFalse(index.contains("../../pilot.html"));
        assertThrows(java.io.IOException.class, () -> WhistExtentFeedback.publish(feedback,
                temp.resolve("absent.html"), "Incomplet", "PASS"));
        assertEquals(index, Files.readString(feedback.resolve("whist-extent/README.md")));
    }

    @Test @ResourceLock(Resources.SYSTEM_PROPERTIES)
    void onlyListsLiveWhistReportsUnderTheDeclaredEvidenceRoot(@TempDir Path temp) throws Exception {
        Path runs = Files.createDirectory(temp.resolve("runs"));
        Path report = Files.writeString(runs.resolve("dense.html"), "dense");
        Path foreign = Files.writeString(temp.resolve("other.html"), "other");
        Path feedback = temp.resolve("feedback");
        Path archive = Path.of("Z:/Whist archive (UI)");
        var values = Map.of("atf.android.handoffRoot", archive.toString(), "atf.android.enabled", "true", "atf.android.artifacts", runs.toString(),
                "atf.match.evidenceDir", runs.toString(), "atf.android.feedbackDirectory", feedback.toString());
        var previous = new HashMap<String, String>();
        values.forEach((name, value) -> previous.put(name, System.setProperty(name, value)));
        try {
            WhistExtentFeedback.afterAudit(report, "com.qsoft.android.whist", "Original", "PASS");
            WhistExtentFeedback.afterAudit(foreign, "ro.whistlab.app", "Component", "PASS");
            assertFalse(Files.exists(feedback));
            WhistExtentFeedback.afterAudit(report, "ro.whistlab.app", "Layout", "PASS");
            Path index = feedback.resolve("whist-extent/README.md");
            String expected = Files.readString(index);
            if (archive.isAbsolute() && !archive.getRoot().equals(temp.getRoot())) {
                String uri = archive.resolve("whist-lab/latest-accepted.html").toUri().toASCIIString();
                assertTrue(expected.contains("<" + uri.replace("(", "%28").replace(")", "%29") + ">"));
            }
            System.setProperty("atf.android.enabled", "false");
            WhistExtentFeedback.afterAudit(report, "ro.whistlab.app", "Offline", "FAIL");
            assertEquals(expected, Files.readString(index));
        } finally {
            previous.forEach((key, value) -> { if (value == null) System.clearProperty(key); else System.setProperty(key, value); });
        }
    }
}
