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
                        var request = LauncherDiscoveryRequestBuilder.request().selectors(selectClass(Passing.class),selectClass(Failing.class),selectClass(Empty.class),selectClass(Aborted.class),selectClass(WithNested.class),selectClass(Explicit.class),selectClass(SubjectField.class),selectClass(RegisteredEnclosing.class),selectClass(DeclarativeEnclosing.class),selectClass(MethodExplicit.class),selectClass(RegisteredEnclosing.Independent.class),selectClass(MultilineLabels.class))
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
        AuditedAssertions.assertEquals(13,run.outcome().invocations().size());
        AuditedAssertions.assertEquals(10L,run.outcome().counts().getOrDefault("PASS",0L).longValue());
        AuditedAssertions.assertEquals(2L,run.outcome().counts().getOrDefault("FAIL",0L).longValue());
        AuditedAssertions.assertEquals(1L,run.outcome().counts().getOrDefault("ABORTED",0L).longValue());
        AuditedAssertions.assertEquals(java.util.List.of(),run.outcome().containerFailures(),"The shared owner must close and pass its declared artifact profile");
        String html=Files.readString(run.artifacts().getFirst());
        AuditedAssertions.assertEquals(java.util.List.of(), ComponentReportCoverage.violations(run.outcome().invocations(),html));
        AuditedAssertions.assertTrue(html.contains("left-value")&&html.contains("right-value"));
        AuditedAssertions.assertTrue(html.contains("Audited assertion coverage"));
        AuditedAssertions.assertTrue(html.contains("controlled abort"));
        AuditedAssertions.assertTrue(org.jsoup.Jsoup.parse(html).select("pre").stream()
                .anyMatch(body -> body.wholeText().contains("label-value\n" + "x".repeat(320))), "Visible label normalization must not alter full comparison data");
        var document = org.jsoup.Jsoup.parse(html);
        AuditedAssertions.assertTrue(document.select("pre").stream().anyMatch(body -> body.wholeText().contains("release/<version>/reports/")),
                "The original JUnit display name remains literal input");
        AuditedAssertions.assertTrue(document.select("version,#j4-display-canary").isEmpty(), "Argument text must not become report metadata markup");
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
    @AtfComponentTest static class RegisteredEnclosing {
        @org.junit.jupiter.api.extension.RegisterExtension final atf.tests.reporting.GameReportAudit audit = new atf.tests.reporting.GameReportAudit();
        @org.junit.jupiter.api.Nested class Child {
            @Test void enclosingRegistrationIsInherited(){AuditedAssertions.assertTrue(true,"The enclosing registered adapter owns this invocation");}
        }
        @AtfComponentTest static class Independent {
            @Test void aStaticFixtureDoesNotInheritEnclosingInstances(){AuditedAssertions.assertTrue(true,"The independent static fixture still needs automatic capture");}
        }
    }
    @AtfComponentTest @org.junit.jupiter.api.extension.ExtendWith(atf.tests.reporting.GameReportAudit.class)
    static class DeclarativeEnclosing {
        @org.junit.jupiter.api.Nested class Child {
            @Test void enclosingAnnotationIsInherited(){AuditedAssertions.assertTrue(true,"The enclosing declarative adapter owns this invocation");}
        }
    }
    @AtfComponentTest static class MethodExplicit {
        @Test @org.junit.jupiter.api.extension.ExtendWith(atf.tests.reporting.GameReportAudit.class)
        void methodRegistrationIsNotDuplicated(){AuditedAssertions.assertTrue(true,"The method registration owns this invocation");}
    }
    @AtfComponentTest static class MultilineLabels {
        @Test @org.junit.jupiter.api.DisplayName("  Input\nlabel\t release/<version>/reports/ & </pre><script id=\"j4-display-canary\">unexpected</script> ")
        void labelsAndValuesKeepDifferentWhitespaceContracts() {
            String value = "label-value\n" + "x".repeat(320);
            AuditedAssertions.assertEquals(value, value, "  Assertion\nlabel\t ");
        }
    }
}
