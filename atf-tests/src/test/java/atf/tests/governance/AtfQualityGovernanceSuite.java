package atf.tests.governance;

import atf.testkit.AtfIntegrationTest;
import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.ExcludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/** Local source/documentation governance; no device, training run or renderer harness. */
@AtfIntegrationTest
@Suite
@SelectPackages({"atf.tests.governance", "atf.libraries"})
@IncludeClassNamePatterns("^[^$]*Test$")
@ExcludeTags("release-download-live")
public class AtfQualityGovernanceSuite { }
