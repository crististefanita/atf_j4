package atf.tests.execution.components;

import atf.testkit.assertions.AuditedAssertions;

import atf.android.evidence.component.AndroidComponentReports;
import atf.testkit.AtfComponentTest;
import atf.testkit.lifecycle.ScopedAcceptanceRun;
import atf.tests.reporting.aggregate.ComponentReportCoverage;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.launcher.core.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class ComponentAggregateAcceptanceTest {
    @TempDir Path temporary;
    @ParameterizedTest @ValueSource(strings={"extent","fallback"})
    void realParameterizedFailuresAndAbortsHaveExactlyOneCanonicalOwner(String renderer) throws Exception {
        var run = ScopedAcceptanceRun.execute(AndroidComponentReports.DIRECTORY, temporary.toString(), temporary,
                () -> {
                    try { return ScopedAcceptanceRun.execute(AndroidComponentReports.RENDERER, renderer, temporary, () -> {
                        var receipt = new ComponentExecutionListener();
                        var request = LauncherDiscoveryRequestBuilder.request().selectors(selectClass(Passing.class),selectClass(Failing.class),selectClass(Empty.class),selectClass(Aborted.class),selectClass(WithNested.class),selectClass(Explicit.class),selectClass(SubjectField.class))
                                .configurationParameter("atf.test.run.scope","suite")
                                .configurationParameter("atf.component.aggregate","true")
                                .configurationParameter("junit.jupiter.extensions.autodetection.enabled","true")
                                .configurationParameter("junit.jupiter.execution.parallel.enabled","false").build();
                        try(var session=LauncherFactory.openSession()){session.getLauncher().execute(request,receipt);}
                        return receipt;
                    },p->p.toString().endsWith(".html")).outcome();
                    } catch(java.io.IOException failure){throw new java.io.UncheckedIOException(failure);}
                },p->p.toString().endsWith(".html"));
        AuditedAssertions.assertEquals(1,run.artifacts().size());
        AuditedAssertions.assertEquals(8,run.outcome().invocations().size());
        AuditedAssertions.assertEquals(5L,run.outcome().counts().getOrDefault("PASS",0L).longValue());
        AuditedAssertions.assertEquals(2L,run.outcome().counts().getOrDefault("FAIL",0L).longValue());
        AuditedAssertions.assertEquals(1L,run.outcome().counts().getOrDefault("ABORTED",0L).longValue());
        String html=Files.readString(run.artifacts().getFirst());
        AuditedAssertions.assertEquals(java.util.List.of(), ComponentReportCoverage.violations(run.outcome().invocations(),html));
        AuditedAssertions.assertTrue(html.contains("left-value")&&html.contains("right-value"));
        AuditedAssertions.assertTrue(html.contains("Audited assertion coverage"));
        AuditedAssertions.assertTrue(html.contains("controlled abort"));
    }
    @AtfComponentTest static class Passing {
        @ParameterizedTest @ValueSource(ints={1,2}) void originalValues(int value){AuditedAssertions.assertEquals(value,value,"Controlled parameter remains distinct");}
    }
    @AtfComponentTest static class Failing {
        @Test void originalFailure(){AuditedAssertions.assertEquals("left-value","right-value","Controlled mismatch");}
    }
    @AtfComponentTest static class Empty {
        @Test void uncapturedBody(){org.junit.jupiter.api.Assertions.assertTrue(true);}
    }
    @AtfComponentTest static class Aborted {
        @Test void originalAbort(){org.junit.jupiter.api.Assumptions.assumeTrue(false,"controlled abort");}
    }
    @AtfComponentTest static class WithNested {
        @org.junit.jupiter.api.Nested class NestedInvocation {
            @Test void belongsToItsParent(){atf.testkit.assertions.AuditedAssertions.assertEquals("nested-value","nested-value","Nested invocation keeps its own audit");}
        }
    }
    @AtfComponentTest @org.junit.jupiter.api.extension.ExtendWith(atf.tests.reporting.GameReportAudit.class)
    static class Explicit {
        @Test void explicitAdapterIsNotDuplicated(){atf.testkit.assertions.AuditedAssertions.assertEquals("explicit","explicit","Explicit registration keeps one receipt");}
    }
    @AtfComponentTest static class SubjectField {
        atf.testkit.assertions.ComponentAuditExtension subject;
        @Test void aSubjectFieldIsNotAnExtensionRegistration(){atf.testkit.assertions.AuditedAssertions.assertNull(subject);}
    }
}
