package atf.tests.reporting.text;

import atf.tests.reporting.GameReportTextAudit;

/** Compatibility for focused legacy matcher contracts; publishers index each report only once. */
final class ReportTextValueMatcher {
    private ReportTextValueMatcher() { }
    static boolean containsCompleteValue(String html, String value) { return new GameReportTextAudit(html).contains(value); }
}
