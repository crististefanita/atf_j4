package atf.android.e2e.support;

import atf.testkit.AtfComponentTest;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.ConfigurationParameter;
import atf.testkit.scope.TestRunScope;

/** Deterministic timing, partial failure and publisher compatibility without a device. */
@Suite
@SelectClasses({AndroidUiFlowTimingComponentTest.class, AndroidUiFlowTraceTest.class, AndroidUiFlowReportPublisherTest.class,
        atf.android.e2e.support.replay.CapturedAndroidFlowComponentTest.class})
@AtfComponentTest
@ConfigurationParameter(key = TestRunScope.CONFIGURATION_PARAMETER, value = "suite")
public class AndroidUiFlowTimingComponentSuite { }
