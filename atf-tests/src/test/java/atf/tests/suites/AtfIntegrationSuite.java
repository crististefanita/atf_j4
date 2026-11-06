package atf.tests.suites;

import atf.testkit.AtfIntegrationTest;
import atf.testkit.scope.TestRunScope;
import org.junit.platform.suite.api.*;

/** Primary integration gate; focused suites are declared separately in the suite catalogue. */
@Suite
@SelectPackages({"atf.tests", "atf.libraries"})
@IncludeClassNamePatterns("^[^$]+$")
@IncludeTags("integration")
@IncludeEngines("junit-jupiter")
@ConfigurationParameter(key = TestRunScope.CONFIGURATION_PARAMETER, value = "suite")
@ConfigurationParameter(key = "junit.jupiter.execution.parallel.enabled", value = "false")
@AtfIntegrationTest
public class AtfIntegrationSuite { }
