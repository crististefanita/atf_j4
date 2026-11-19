package atf.tests.reporting;

import atf.android.evidence.component.AndroidComponentPublication;
import atf.android.evidence.component.AndroidComponentReports;
import atf.android.evidence.component.AndroidComponentRun;
import atf.android.evidence.component.ComponentInputs;
import atf.reporting.audit.AssertionAuditReport;
import atf.testkit.assertions.ComponentAuditAdapter;
import atf.testkit.assertions.ComponentAuditExtension;
import java.util.List;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;

/** Framework-contract metadata; Kit owns capture and Android's neutral publisher owns HTML projection. */
public final class GameReportAudit implements ComponentAuditAdapter {
    private static final ExtensionContext.Namespace INPUTS = ExtensionContext.Namespace.create(GameReportAudit.class);
    private final ComponentAuditExtension extension = ComponentAuditExtension.scoped(AndroidComponentReports.class, AndroidComponentRun.class,
            context -> AndroidComponentReports.open(), (run, context) -> {
                var type = context.getRequiredTestClass();
                var input = new ComponentInputs();
                input.add("JUnit invocation identity", context.getUniqueId());
                context.getStore(INPUTS).put(ComponentInputs.class, input);
                var identity = new AssertionAuditReport.Identity(type.getName().substring(type.getPackageName().length() + 1),
                        type.getPackageName(), context.getDisplayName(), context.getTestMethod().map(java.lang.reflect.Method::getName).orElse("Class lifecycle"),
                        List.of(context.getTags().contains("unit") ? "unit" : "component", "framework-acceptance", "offline"));
                return new AndroidComponentPublication(run, identity, input);
            });
    @Override public ComponentAuditExtension auditExtension() { return extension; }
    @Override public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
        return parameter.getParameter().getType() == ComponentInputs.class || extension.supportsParameter(parameter, context);
    }
    @Override public Object resolveParameter(ParameterContext parameter, ExtensionContext context) {
        return parameter.getParameter().getType() == ComponentInputs.class
                ? java.util.Objects.requireNonNull(context.getStore(INPUTS).get(ComponentInputs.class, ComponentInputs.class))
                : extension.resolveParameter(parameter, context);
    }
}
