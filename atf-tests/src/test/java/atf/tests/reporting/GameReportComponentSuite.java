package atf.tests.reporting;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/** Offline consumer policy, evidence and physical-JAR publication contracts. */
@Suite
@SelectClasses({GameReportPolicyContractTest.class, GameEvidenceBodiesContractTest.class, GameReportPublicationContractTest.class, GameReportTextAuditContractTest.class})
public class GameReportComponentSuite { }
