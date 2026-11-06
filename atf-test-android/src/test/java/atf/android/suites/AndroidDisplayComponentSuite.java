package atf.android.suites;

import atf.testkit.AtfComponentTest;
import atf.testkit.scope.TestRunScope;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/** JVM-only display contracts, including history fixtures, observation decoding and bounded native plans. */
@Suite
@SelectPackages({"atf.android.e2e.display","atf.android.e2e.driver","atf.android.e2e.records"})
@IncludeClassNamePatterns(".*ComponentTest")
@IncludeTags("component")
@ConfigurationParameter(key = TestRunScope.CONFIGURATION_PARAMETER, value = "suite")
@AtfComponentTest
public class AndroidDisplayComponentSuite { }
