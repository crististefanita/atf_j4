package atf.android.whist.evidence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import static org.junit.jupiter.api.Assertions.*;

class WhistExtentFeedbackTest {
    @Test void publishesTheLatestAuditedFailureWithAnEscapedLocalLink(@TempDir Path temp) throws Exception {
        Path feedback = temp.resolve("feedback");
        Path first = Files.writeString(temp.resolve("pilot.html"), "pilot");
        Path second = Files.writeString(temp.resolve("scor nou (RO).html"), "scor");
        WhistExtentFeedback.publish(feedback, first, "Pilot", "PASS");
        WhistExtentFeedback.publish(feedback, second, "Scor", "FAIL");
        String index = Files.readString(feedback.resolve("whist-extent/README.md"));
        assertTrue(index.contains("<" + second.toUri().toASCIIString() + ">"));
        assertTrue(index.contains("Scor") && index.contains("FAIL"));
        assertFalse(index.contains(first.toUri().toString()));
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
        var values = Map.of("atf.android.enabled", "true", "atf.android.artifacts", runs.toString(),
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
            System.setProperty("atf.android.enabled", "false");
            WhistExtentFeedback.afterAudit(report, "ro.whistlab.app", "Offline", "FAIL");
            assertEquals(expected, Files.readString(index));
        } finally {
            previous.forEach((key, value) -> { if (value == null) System.clearProperty(key); else System.setProperty(key, value); });
        }
    }
}
