package atf.tests.contracts;

import atf.testkit.assertions.AuditedAssertions;

import atf.tests.publishing.DeepSuiteExtentReportTest;

import atf.testkit.AtfComponentTest;

import static org.junit.jupiter.api.Assertions.*;
import atf.tests.reporting.GameReportSession;
import atf.tests.reporting.GameReportTextAudit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Properties;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Consumer manifest bridge, not a new Android/device execution. */
@AtfComponentTest
class LegacyUiMediaReportContractTest {
    @TempDir Path folder;
    @Test void observationOnlyFramesKeepOneKeywordExactBytesAndZeroInventedComparisons() throws Exception {
        byte[] png=Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
        Files.write(folder.resolve("frame.png"),png);var flow=new Properties();
        flow.setProperty("format.version","1");flow.setProperty("flow.name","Captured instructions");
        flow.setProperty("flow.testClass","controlled.MediaComponentTest");flow.setProperty("flow.testPackage","controlled");
        flow.setProperty("flow.appPackage","controlled.synthetic");flow.setProperty("flow.status","PASS");flow.setProperty("flow.phaseCount","1");
        flow.setProperty("phase.0.name","TEST BODY");flow.setProperty("phase.0.stepCount","2");
        for(int i=0;i<2;i++){
            String key="phase.0.step."+i;
            flow.setProperty(key+".keyword","THEN");flow.setProperty(key+".title","Captured instruction panel "+i);
            flow.setProperty(key+".target","Synthetic media fixture");flow.setProperty(key+".status","PASS");
            flow.setProperty(key+".artifactCount","0");flow.setProperty(key+".assertionCount","0");flow.setProperty(key+".actionCount","1");
            flow.setProperty(key+".screenshot","frame.png");String action=key+".action.0";
            flow.setProperty(action+".sequence",Integer.toString(i+1));flow.setProperty(action+".operation","OBSERVE");
            flow.setProperty(action+".target","Synthetic frame");flow.setProperty(action+".expected","No business comparison claimed");
            flow.setProperty(action+".observed","first\r\nlast & &#13; <literal>");flow.setProperty(action+".durationMillis","0");
            flow.setProperty(action+".status","INFO");flow.setProperty(action+".source","OBSERVE");
        }
        Path manifest=folder.resolve("ui.properties");try(var out=Files.newOutputStream(manifest)){flow.store(out,"Controlled manifest");}
        Path report=folder.resolve("media.html");try(var session=GameReportSession.open(report)){
            DeepSuiteExtentReportTest.publishUiFlow(session,manifest,folder,true);
        }
        String html=Files.readString(report);var doc=Jsoup.parse(html);
        var canonical=doc.select("#test-view .test-list-item > li.test-item");AuditedAssertions.assertEquals(1,canonical.size());
        var labels=canonical.first().select(".node").eachText();
        AuditedAssertions.assertTrue(labels.contains("THEN Captured instruction panel 0"));AuditedAssertions.assertTrue(labels.contains("THEN Captured instruction panel 1"));
        AuditedAssertions.assertTrue(labels.stream().noneMatch(label->label.startsWith("THEN THEN ")));
        AuditedAssertions.assertEquals(0,canonical.select("table.helm-validation-table > tbody > tr").size());
        AuditedAssertions.assertEquals(2,canonical.select("a[data-atf-screenshot=base64],a[data-atf-screenshot-ref]").size());
        AuditedAssertions.assertTrue(html.contains(Base64.getEncoder().encodeToString(png)));
        AuditedAssertions.assertTrue(new GameReportTextAudit(html).contains("first\r\nlast & &#13; <literal>"));
    }
}
