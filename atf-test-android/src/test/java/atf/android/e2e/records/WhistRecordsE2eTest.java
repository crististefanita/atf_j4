package atf.android.e2e.records;

import atf.android.e2e.activation.AndroidE2eTest;
import atf.android.e2e.support.AndroidUiFlowReportPublisher;
import atf.reporting.contract.AtfReportStatus;
import org.junit.jupiter.api.Test;

/** One real Android journey; controlled preconditions and actual card/UI actions remain separately identified. */
@AndroidE2eTest
public class WhistRecordsE2eTest {
    @Test void namesHistoryRankingsAndSymmetricSeriesSurviveAndroidUiAndRelaunch() throws Throwable {
        execute(new RecordsContext(),c -> {
            NameHistoryScenarios.names(c);
            NameHistoryScenarios.history(c);
            SeriesRankingScenarios.series(c,5);
            SeriesRankingScenarios.series(c,10);
            SeriesRankingScenarios.ranking(c);
        });
    }

    @Test void completeHistoryCellsAndRankingRowsRemainReadableAfterScrolling() throws Throwable {
        execute(new RecordsContext("history-cell-visibility",
                "Whist Lab — istoric RO/EN și clasamente integral lizibile","history-visibility"),c -> {
            c.phase("SETUP — isolated history visibility precondition");c.sandbox();c.home("EN");
            c.ui.awaitText("New game",java.time.Duration.ofSeconds(15));
            NameHistoryScenarios.history(c);
            NameHistoryScenarios.rankingVisibility(c);
        });
    }

    @FunctionalInterface private interface Journey { void run(RecordsContext context) throws Exception; }

    private static void execute(RecordsContext c,Journey journey) throws Throwable {
        Throwable failure=null;
        try {
            journey.run(c);
        } catch(Throwable error) {
            failure=error;c.trace.fail(error);
            try {
                c.verifyLease();
                c.phase("FAILURE — observed Android state");
                c.check("Execution stopped — observed Android state",
                        RecordsContext.equal("Journey completed without exception","no exception",error.toString()));
            } catch(Throwable capture) {if(!(capture instanceof AssertionError))error.addSuppressed(capture);}
        }
        finally {
            try {c.restore();}catch(Throwable cleanup){c.trace.fail(cleanup);if(failure==null)failure=cleanup;else failure.addSuppressed(cleanup);}
            c.trace.finish(failure==null?AtfReportStatus.PASS:AtfReportStatus.FAIL);
            c.evidence.add(c.trace.manifest());
            try {
                AndroidUiFlowReportPublisher.publish(c.output.resolve("final-extent-report.html"),
                        c.setup.path("installedPath").asText()+"; SHA-256="+c.setup.path("installedApkSha256").asText(),
                        c.setup.path("serial").asText(),c.setup.path("api").asText(),
                        "Declared journey: "+c.trace.name()+". Android API 28 at 720x1280@360. Full-history and final-card preconditions are prepared by the JVM engine; UI actions, comparisons and restoration have individual recorded verdicts. This is not a complete match played through UI. Overall outcome: "+c.trace.status()+".",
                        c.evidence,c.trace);
            }catch(Throwable reporting){if(failure==null)failure=reporting;else failure.addSuppressed(reporting);}
        }
        if(failure!=null)throw failure;
    }
}
