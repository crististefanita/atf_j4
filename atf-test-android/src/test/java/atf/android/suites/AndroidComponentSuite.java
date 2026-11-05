package atf.android.suites;

import atf.android.AdbClientContractTest;
import atf.android.process.CommandProcessComponentTest;
import atf.android.evidence.AndroidScreenshotEvidenceComponentTest;
import atf.testkit.AtfComponentTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/** Local Android adapter contracts; this suite never needs an emulator. */
@Suite
@SelectClasses({AdbClientContractTest.class, CommandProcessComponentTest.class, AndroidScreenshotEvidenceComponentTest.class})
@AtfComponentTest
public class AndroidComponentSuite { }
