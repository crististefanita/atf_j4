package atf.android.e2e.support;

import atf.testkit.AtfComponentTest;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/** Deterministic timing, partial failure and publisher compatibility without a device. */
@Suite
@SelectClasses({AndroidUiFlowTimingComponentTest.class, AndroidUiFlowTraceTest.class, AndroidUiFlowReportPublisherTest.class})
@AtfComponentTest
public class AndroidUiFlowTimingComponentSuite { }
