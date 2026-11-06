package atf.android.suites;

import atf.testkit.AtfComponentTest;
import atf.testkit.scope.TestRunScope;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/** Audited history contracts share one report; native rendering remains a separate E2E qualification. */
@Suite
@SelectPackages("atf.android.e2e.records.legend")
@IncludeClassNamePatterns("^[^$]*ComponentTest$")
@ConfigurationParameter(key = TestRunScope.CONFIGURATION_PARAMETER, value = "suite")
@AtfComponentTest
public class AndroidHistoryComponentSuite { }
