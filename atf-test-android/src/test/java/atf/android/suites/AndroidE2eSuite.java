package atf.android.suites;

import atf.testkit.AtfE2eTest;
import atf.testkit.scope.TestRunScope;
import org.junit.platform.suite.api.*;

/** Primary e2e gate; focused suites are declared separately in the suite catalogue. */
@Suite
@SelectPackages("atf.android")
@IncludeClassNamePatterns("^[^$]+$")
@IncludeTags("e2e")
@IncludeEngines("junit-jupiter")
@ConfigurationParameter(key = TestRunScope.CONFIGURATION_PARAMETER, value = "suite")
@ConfigurationParameter(key = "junit.jupiter.execution.parallel.enabled", value = "false")
@AtfE2eTest
public class AndroidE2eSuite { }
