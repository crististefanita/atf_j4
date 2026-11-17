# Reporting - evidence and auditable reports

## Overview

Reporting publishes the data, comparisons and verdict of the caller in Extent/fallback, without Core or Kit.

| Nevoie | API |
|---|---|
| Individual rolling / class / suite | `AssertionAuditRun` |
| Invocare | `run.open(Identity)` → `AssertionAuditReport` |
| Ports / fallback | `AtfReportPorts` / `HtmlReportSession` |
| HTTP observat | `request.execution(ReportExecutionEvidence)`, cu `ReportResponse` |
| Capturi | `UiExtentReportSession`; PNG integral |

## Exemplu minim

Individual: `try (var run = open(output)) { publish(run, actual); }`. The suite keeps the run and opens a scenario per invoicing:

```java
import atf.reporting.audit.AssertionAuditRun;
import atf.reporting.audit.AssertionAuditReport;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.security.SensitiveDataRedactor;
import java.nio.file.Path;
import java.util.List;

public static AssertionAuditRun open(Path output) {
    var settings = ReportingSettings.defaults();
    var redactor = new SensitiveDataRedactor();
    return new AssertionAuditRun(new ExtentReportSession(output, settings, redactor), redactor, settings);
}

public static void publish(AssertionAuditRun run, String actual) {
    boolean passed = "ready".equals(actual);
    var status = passed ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
    var identity = new AssertionAuditReport.Identity("HealthContract", "example", "state is ready", "state", List.of("component"));
    try (var report = run.open(identity)) {
        report.phase("TEST BODY", List.of(new AtfAssertion("State", "ready", String.valueOf(actual), passed)), status, "");
        report.complete(status);
    }
    if (!passed) throw new AssertionError("State differs; inspect the report");
}
```

## Purpose and integration

| Owner | Closure and limit |
|---|---|
| AssertionAuditRun | He holds the session; close publishes his final HTML. |
| run.open(identity) | Borrowed session; close close close closing the invoicing scenario. |
| Constructorul AssertionAuditReport(session, ...) | Owns and closes his own session. |

- Use the same settings and redactor in the run and renderer. The default preview is **240**, as returned by assertionMaxDisplayCharacters(). Full values come before the preview.
- The example follows the thrall of the session; the AssertionAuditRun.extent (...) serializes the publication for competing callers.
- Kit collects the JUnit comparison evaluated once; Reporting publicly. The callier keeps the verdict, cause and timeout through phase (..., timing).
- Fails indexes the terminal cause in the line ACTION or OBSERVES that failed. Tests also retain the collapsible diagnosis, and independent comparisons remain distinct.
- The NGP keeps the entire viewport/display; the capture and the zoome belong to the adapter. The same catch at the same step shall retain one single proof and the first caption.
- After close, the ReportArtifactInspector.inspect (path, profiles) qualifies inventory and Emended resources; diagnosis localalexternal dependencies. The text paths remain valid. The inspector shall be static; the rendering and interactions shall be checked separately in the browser.
- The browser belongs to the caller: separate the diagnosis from JUnit Discovery of execution, close the context before the browser and keep the original cause to the cleanup.

## Verification of use

| Example method input | Rezultat observabil |
|---|---|
| `actual` este `ready`. | The method shall be terminated normally and the HTML file shall be in place after the end of the session. |
| `actual` differs or is `null`. | The ratio keeps the comparison failed, then the method throws `AssertionError`. |
| The source of the example is compiled on another JAR. | Any incompatibility of manufacturers or methods shall be detected prior to publication. |

The example verifies the use of API and the propagation of the verdict. HTML autonomy, interactive expansion and native catches have their own acceptances; the existence of the file does not demonstrate them itself.

The duration of HTTP comes from `ReportResponse.duration()`; `detail(...)` retains the text of the calleric without converting it into a transport measurement.

---

The reporting pair is an external binary fixed in the POM and the register of provenance. This guide checks the API consumed and the result of the report in the application; an observed limitation is attached to the version, hash and a reproducible example. [Local procedure](README.md#maintenance-and-feedback)
