package atf.android.e2e.activation;

import atf.console.junit.AtfConsoleTestExtension;
import atf.testkit.AtfConsoleMessages;
import atf.testkit.AtfE2eTest;
import atf.testkit.reporting.AtfReportPath;
import atf.testkit.output.UiReportSubdirectory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Shared Android E2E metadata; emulator interaction remains explicitly opt-in. */
@AtfE2eTest
@UiReportSubdirectory("android")
@AtfConsoleMessages(AtfConsoleMessages.Policy.DETAILS)
@AtfReportPath
@ExtendWith({AtfConsoleTestExtension.class, AndroidE2eActivationExtension.class})
@Execution(ExecutionMode.SAME_THREAD)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AndroidE2eTest { }
