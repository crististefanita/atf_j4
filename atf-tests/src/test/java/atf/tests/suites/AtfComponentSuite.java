package atf.tests.suites;

import atf.testkit.AtfComponentTest;
import atf.testkit.scope.TestRunScope;
import org.junit.platform.suite.api.*;

/** Primary component gate; focused suites are declared separately in the suite catalogue. */
@Suite
@SelectPackages({"atf.tests", "atf.libraries"})
@IncludeClassNamePatterns("^[^$]+$")
@IncludeTags("component")
@IncludeEngines("junit-jupiter")
@ConfigurationParameter(key = TestRunScope.CONFIGURATION_PARAMETER, value = "suite")
@ConfigurationParameter(key = "junit.jupiter.execution.parallel.enabled", value = "false")
@AtfComponentTest
public class AtfComponentSuite { }
