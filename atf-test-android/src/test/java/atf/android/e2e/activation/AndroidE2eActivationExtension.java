package atf.android.e2e.activation;

import atf.testkit.lifecycle.AtfE2eActivationPolicy;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/** Android device boundary; live tests run only after an explicit property opt-in. */
public final class AndroidE2eActivationExtension implements ExecutionCondition {
    public static final String ENABLE_PROPERTY = "atf.android.enabled";

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        var decision = AtfE2eActivationPolicy.evaluate(ENABLE_PROPERTY,
                System.getProperty(ENABLE_PROPERTY), false, null, false);
        return decision.enabled()
                ? ConditionEvaluationResult.enabled(decision.reason())
                : ConditionEvaluationResult.disabled(decision.reason());
    }
}
