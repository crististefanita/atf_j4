package atf.gradle.audit;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.ExternalResource;

/** JUnit 4 lifecycle only. This fixture is never a dependency of an Android runtime variant. */
public abstract class GradleAuditedTest {
    @Rule public final GradleAuditRule componentAudit = new GradleAuditRule(this);
    @ClassRule public static final ExternalResource componentPublication = new ExternalResource() {
        @Override protected void after() { GradleReport.flush(); }
    };
}
