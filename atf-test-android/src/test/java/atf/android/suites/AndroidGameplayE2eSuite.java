package atf.android.suites;

import atf.android.e2e.ContractWhistGameplayE2eTest;
import atf.android.e2e.WhistLabGameplayE2eTest;
import atf.testkit.AtfE2eTest;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/** Serializes both app adapters against the shared API 28 Android emulator. */
@Suite
@SelectClasses({ContractWhistGameplayE2eTest.class, WhistLabGameplayE2eTest.class})
@AtfE2eTest
@Execution(ExecutionMode.SAME_THREAD)
public class AndroidGameplayE2eSuite { }
