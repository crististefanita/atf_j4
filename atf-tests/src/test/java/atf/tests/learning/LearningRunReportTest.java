package atf.tests.learning;

import atf.testkit.assertions.AuditedAssertions;

import atf.testkit.AtfComponentTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import atf.tests.learning.*;
import atf.tests.learning.LearningReportHtmlAudit;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

@AtfComponentTest
class LearningRunReportTest {
    @Test void publishesOnlyCapturedInvocationsThroughThePhysicalReportingJar()throws Exception{
        assumeTrue(Boolean.getBoolean("atf.learning.publish"));Path folder=Path.of(System.getProperty("atf.learning.folder")),manifest=Path.of(System.getProperty("atf.learning.manifest")),report=Path.of(System.getProperty("atf.learning.report"));
        var population=LearningRunReport.load(folder,manifest);var selection=EvidenceSelection.choose(population,EvidencePolicyYaml.capture());var inputs=selection.inputs();var result=Boolean.getBoolean("atf.learning.auditOnly")?LearningRunReport.capturedSummary(inputs):LearningRunReport.publish(report,selection);AuditedAssertions.assertTrue(result.comparisons()>0);AuditedAssertions.assertTrue(Files.size(report)>1000);AuditedAssertions.assertEquals(result.comparisons(),LearningReportHtmlAudit.verify(report,inputs));LearningReportHtmlAudit.verifyScope(report,selection);
        var proof=new Properties();proof.setProperty("status","STRUCTURAL_AUDIT_PASS_BROWSER_PENDING");proof.setProperty("invocations",""+result.invocations());proof.setProperty("executedPopulation",""+population.size());proof.setProperty("omittedInvocations",""+(population.size()-inputs.size()));proof.setProperty("comparisons",""+result.comparisons());proof.setProperty("capturedExecutionNanos",""+result.executionNanos());proof.setProperty("capturedStatus",result.capturedStatus());proof.setProperty("report.sha256",LearningRunReport.hash(Files.readAllBytes(report)));proof.setProperty("report.bytes",""+Files.size(report));Path jar=Path.of(atf.reporting.extent.session.ExtentReportSession.class.getProtectionDomain().getCodeSource().getLocation().toURI());proof.setProperty("reporting.runtimeJar",jar.toString());proof.setProperty("reporting.runtimeJar.sha256",LearningRunReport.hash(Files.readAllBytes(jar)));Path proofFile=Boolean.getBoolean("atf.learning.auditOnly")?Files.createTempFile(report.getParent(),report.getFileName()+"-audit-",".properties"):report.resolveSibling(report.getFileName()+".properties");try(var out=Files.newOutputStream(proofFile)){proof.store(out,"Composition is separate from source outcomes and rendering acceptance");}
    }
}
