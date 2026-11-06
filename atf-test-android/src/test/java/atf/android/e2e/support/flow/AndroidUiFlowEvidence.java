package atf.android.e2e.support.flow;

import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
import java.nio.file.Path;
import java.util.List;

/** Read-only publication contract shared by live capture and explicitly labelled offline republication. */
public interface AndroidUiFlowEvidence {
    Path manifest();
    String id();
    String name();
    String testClass();
    String testPackage();
    String appPackage();
    List<String> tags();
    AtfReportStatus status();
    String failure();
    boolean hasOpenPhase();
    List<AtfUiActionRecord> pendingActions();
    List<AndroidUiFlowTrace.PhaseSnapshot> phaseSnapshots();
}
