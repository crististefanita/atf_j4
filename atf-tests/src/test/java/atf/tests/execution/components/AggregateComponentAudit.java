package atf.tests.execution.components;

import atf.tests.reporting.GameReportAudit;
import atf.testkit.assertions.ComponentAuditAdapter;
import atf.testkit.assertions.ComponentAuditExtension;
import java.lang.reflect.Method;
import org.junit.jupiter.api.extension.*;

/** Opt-in launcher adapter for components without an explicit consumer adapter; nested launches stay independent. */
public final class AggregateComponentAudit implements ComponentAuditAdapter {
    private final GameReportAudit delegate = new GameReportAudit();
    private boolean enabled(ExtensionContext context) {
        if (!context.getConfigurationParameter("atf.component.aggregate").orElse("false").equals("true")
                || !context.getTags().contains("component")) return false;
        for (Class<?> type = context.getRequiredTestClass(); type != null; type = type.getSuperclass())
            for (var field : type.getDeclaredFields()) if (field.isAnnotationPresent(RegisterExtension.class)
                    && adapterType(field.getType())) return false;
        if (org.junit.platform.commons.support.AnnotationSupport.findRepeatableAnnotations(context.getRequiredTestClass(), ExtendWith.class)
                .stream().flatMap(annotation -> java.util.Arrays.stream(annotation.value())).anyMatch(AggregateComponentAudit::adapterType)) return false;
        return true;
    }
    private static boolean adapterType(Class<?> type) {
        return ComponentAuditAdapter.class.isAssignableFrom(type) || ComponentAuditExtension.class.isAssignableFrom(type);
    }
    @Override public ComponentAuditExtension auditExtension() { return delegate.auditExtension(); }
    @Override public void beforeEach(ExtensionContext c) throws Exception { if (enabled(c)) delegate.beforeEach(c); }
    @Override public void afterEach(ExtensionContext c) throws Exception { if (enabled(c)) delegate.afterEach(c); }
    @Override public boolean supportsParameter(ParameterContext p, ExtensionContext c) { return enabled(c) && delegate.supportsParameter(p,c); }
    @Override public Object resolveParameter(ParameterContext p, ExtensionContext c) { return delegate.resolveParameter(p,c); }
    @Override public void interceptBeforeEachMethod(Invocation<Void> call, ReflectiveInvocationContext<Method> method, ExtensionContext c) throws Throwable {
        if (enabled(c)) delegate.interceptBeforeEachMethod(call,method,c); else call.proceed();
    }
    @Override public void interceptAfterEachMethod(Invocation<Void> call, ReflectiveInvocationContext<Method> method, ExtensionContext c) throws Throwable {
        if (enabled(c)) delegate.interceptAfterEachMethod(call,method,c); else call.proceed();
    }
    @Override public void interceptTestMethod(Invocation<Void> call, ReflectiveInvocationContext<Method> method, ExtensionContext c) throws Throwable {
        if (enabled(c)) delegate.interceptTestMethod(call,method,c); else call.proceed();
    }
    @Override public void interceptTestTemplateMethod(Invocation<Void> call, ReflectiveInvocationContext<Method> method, ExtensionContext c) throws Throwable {
        if (enabled(c)) delegate.interceptTestTemplateMethod(call,method,c); else call.proceed();
    }
    @Override public void interceptBeforeAllMethod(Invocation<Void> call, ReflectiveInvocationContext<Method> method, ExtensionContext c) throws Throwable {
        if (enabled(c)) delegate.interceptBeforeAllMethod(call,method,c); else call.proceed();
    }
    @Override public void interceptAfterAllMethod(Invocation<Void> call, ReflectiveInvocationContext<Method> method, ExtensionContext c) throws Throwable {
        if (enabled(c)) delegate.interceptAfterAllMethod(call,method,c); else call.proceed();
    }
    @Override public void handleBeforeAllMethodExecutionException(ExtensionContext c, Throwable failure) throws Throwable {
        if (enabled(c)) delegate.handleBeforeAllMethodExecutionException(c,failure); else throw failure;
    }
    @Override public void handleAfterAllMethodExecutionException(ExtensionContext c, Throwable failure) throws Throwable {
        if (enabled(c)) delegate.handleAfterAllMethodExecutionException(c,failure); else throw failure;
    }
}
