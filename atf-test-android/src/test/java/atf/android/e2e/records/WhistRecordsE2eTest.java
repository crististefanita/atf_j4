package atf.android.e2e.records;

import atf.android.e2e.activation.AndroidE2eTest;
import atf.android.e2e.support.AndroidUiFlowReportPublisher;
import atf.reporting.contract.AtfReportStatus;
import org.junit.jupiter.api.Test;

/** One real Android journey; controlled preconditions and actual card/UI actions remain separately identified. */
@AndroidE2eTest
public class WhistRecordsE2eTest {
    @Test void releaseHistoryResumeKeepsTheCompletedLedgerAndBothLanguages() throws Throwable {
        execute(new RecordsContext("release-history-resume",
                "Whist Lab — reluarea istoricului pe binarul curent", "history-release-sanity"), c -> {
            c.phase("SETUP — preserved data and current APK");
            c.sandbox();
            c.command("Set portrait resolution", "1080x2448", "shell", "wm", "size", "1080x2448");
            c.command("Set portrait density", "440 dpi", "shell", "wm", "density", "440");
            c.command("Enlarge text", "1.3", "shell", "settings", "put", "system", "font_scale", "1.3");
            var fixture = RecordsFixtures.history();
            c.inject(fixture, "release-history-24-jvm-played-hands");
            var original = fixture.json().path("scores");
            for (boolean romanian : new boolean[]{false, true}) {
                c.phase("TEST BODY — resumed history " + (romanian ? "RO" : "EN"));
                c.home(romanian ? "RO" : "EN");
                c.tap(romanian ? "Reia partida" : "Resume game");
                c.ui.awaitText(romanian ? "Partidă încheiată" : "Game complete", java.time.Duration.ofSeconds(15));
                c.tap(romanian ? "Istoricul scorului" : "Score history");
                String observed = c.ui.dump();
                c.check("Current APK resumes the same completed ledger: " + (romanian ? "RO" : "EN"),
                        RecordsContext.equal("Installed requested APK", c.setup.path("requestedApkSha256").asText(),
                                c.raw("shell", "sha256sum", c.setup.path("installedPath").asText()).split("\\s+")[0]),
                        RecordsContext.equal("All settled hands retained", original, c.state().path("scores")),
                        RecordsContext.equal("Card-count heading", true, c.ui.hasText(observed, romanian ? "Cărți" : "Cards")),
                        RecordsContext.equal("Bid heading", true, c.ui.hasText(observed, romanian ? "L" : "B")),
                        RecordsContext.equal("Points heading", true, c.ui.hasText(observed, "P")),
                        RecordsContext.equal("No third total heading", false, c.ui.hasText(observed, "T")),
                        RecordsContext.equal("Human column remains readable", true, c.ui.hasText(observed, "HistUser")));
            }
        });
    }

    @Test void compactHistoryAndSlashedContractsRemainReadable() throws Throwable {
        execute(new RecordsContext("compact-history-sanity",
                "Whist Lab — istoric compact, contracte ratate și text mărit RO/EN", "compact-history"),c -> {
            c.phase("SETUP — preserved data and identified compact-history APK");
            c.sandbox();c.home("EN");c.ui.awaitText("New game",java.time.Duration.ofSeconds(15));
            c.check("Installed APK is the component-qualified build",
                    RecordsContext.equal("APK SHA-256",c.setup.path("requestedApkSha256").asText(),
                            c.raw("shell","sha256sum",c.setup.path("installedPath").asText()).split("\\s+")[0]));
            c.command("Set portrait resolution","1080x2448","shell","wm","size","1080x2448");
            c.command("Set portrait density","440 dpi","shell","wm","density","440");
            c.command("Select normal text","1.0","shell","settings","put","system","font_scale","1.0");
            NameHistoryScenarios.history(c,"compact normal text");
            c.command("Enlarge history text","1.3","shell","settings","put","system","font_scale","1.3");
            NameHistoryScenarios.history(c,"compact enlarged text");
        });
    }

    @Test void historyPresentationAcrossDeclaredPortraitProfiles() throws Throwable {
        execute(new RecordsContext("history-portrait-profiles",
                "Whist Lab — masă, acțiuni și istoric RO/EN pe profiluri portret","history-profiles"),c -> {
            c.phase("SETUP — preserved data and isolated history precondition");
            c.sandbox();c.home("EN");
            c.ui.awaitText("New game",java.time.Duration.ofSeconds(15));
            c.check("Verified installed APK and isolated precondition",
                    RecordsContext.equal("Installed APK SHA-256",c.setup.path("requestedApkSha256").asText(),
                            c.raw("shell","sha256sum",c.setup.path("installedPath").asText()).split("\\s+")[0]),
                    RecordsContext.equal("Observed Android API",c.setup.path("api").asText(),c.raw("shell","getprop","ro.build.version.sdk")));
            for(var profile:atf.android.e2e.display.profile.ResponsiveDisplayProfile.parse(
                    System.getProperty("atf.android.sanity.profiles")))table(c,profile,"1.0");
            activeLabels(c);
            for(var profile:atf.android.e2e.display.profile.ResponsiveDisplayProfile.parse(
                    System.getProperty("atf.android.sanity.profiles"))) {
                c.phase("SETUP — history viewport "+profile.id());
                c.command("Restore normal text for history",c.setup.path("fontScale").asText("1.0"),
                        "shell","settings","put","system","font_scale",c.setup.path("fontScale").asText("1.0"));
                c.command("Apply history resolution",profile.id(),"shell","wm","size",profile.width()+"x"+profile.height());
                c.command("Apply history density",profile.id(),"shell","wm","density",""+profile.density());
                c.check("History viewport prepared: "+profile.id(),
                        RecordsContext.equal("Logical resolution",true,c.raw("shell","wm","size").contains(profile.width()+"x"+profile.height())),
                        RecordsContext.equal("Logical density",true,c.raw("shell","wm","density").contains(""+profile.density())));
                NameHistoryScenarios.history(c,profile.id());
            }
        });
    }

    private static void table(RecordsContext c,atf.android.e2e.display.profile.ResponsiveDisplayProfile profile,String scale) throws Exception {
        c.phase("TEST BODY — table "+profile.id()+", font "+scale);
        c.command("Select table font scale",scale,"shell","settings","put","system","font_scale",scale);
        var ui=c.scopedUi("table-"+profile.id()+"-font-"+scale);
        atf.android.e2e.display.flow.responsive.ResponsiveDisplayActions.applyProfile(c.adb,ui,profile);
        atf.android.e2e.display.flow.responsive.SimultaneousTableChecks.inspect(c.adb,ui,c.trace,c.evidence,profile);
        atf.android.e2e.display.flow.responsive.ResponsiveMenuChecks.inspectNineBidTable(c.adb,ui,c.trace,c.evidence,profile);
    }

    private static void activeLabels(RecordsContext c) throws Exception {
        var profile=atf.android.e2e.display.profile.ResponsiveDisplayProfile.parse("1080x2448@440").getFirst();
        c.phase("SETUP — tall portrait with enlarged action text");
        c.command("Set tall portrait resolution",profile.id(),"shell","wm","size","1080x2448");
        c.command("Set tall portrait density","440 dpi","shell","wm","density","440");
        c.command("Enlarge text","font scale 1.3","shell","settings","put","system","font_scale","1.3");
        c.check("Tall portrait and enlarged text prepared",RecordsContext.equal("Font scale","1.3",c.raw("shell","settings","get","system","font_scale")));
        table(c,profile,"1.3");
        c.phase("TEST BODY — action above each bidding and playing seat");
        for(int seat=0;seat<4;seat++)for(boolean bidding:new boolean[]{true,false}) {
            boolean romanian=(seat+(bidding?1:0))%2==0;String language=romanian?"RO":"EN";
            var fixture=RecordsFixtures.activeTurn(seat,bidding);
            c.inject(fixture,"active-seat-"+seat+"-"+bidding+"-"+language);c.home(language,true);
            c.tap(romanian?"Reia partida":"Resume game");
            String action=romanian?(bidding?"LICITEAZĂ":"JOACĂ ACUM"):(bidding?"BID NOW":"PLAY NOW");
            String xml=c.ui.awaitText(action,java.time.Duration.ofSeconds(15));
            var checks=new java.util.ArrayList<>(atf.android.e2e.display.support.TableViewportObservation.inspectActions(c.ui,xml,profile,1,romanian));
            String name=fixture.json().path("players").get(seat).path("name").asText();
            checks.add(RecordsContext.equal("Requested active player",true,c.ui.descriptions(xml).stream()
                    .anyMatch(text->text.startsWith(name+". ")&&text.contains(romanian?". La rând.":". Current turn."))));
            var faces=c.ui.handCardFaceBounds(xml);
            checks.add(RecordsContext.equal("Whole human hand remains visible",true,
                    faces.size()==fixture.game().snapshot().players().get(0).hand().size()
                    &&faces.stream().allMatch(face->face.bottom()-face.top()>=Math.round(98*profile.density()/160f))));
            if(bidding&&seat!=0) {
                var nodes=RecordsContext.xml(xml).getElementsByTagName("node");boolean disabled=true;int buttons=0;
                for(int index=0;index<nodes.getLength();index++) {
                    var node=(org.w3c.dom.Element)nodes.item(index);
                    if(node.getAttribute("class").equals("android.widget.Button")) {buttons++;disabled&=!node.getAttribute("enabled").equals("true");}
                }
                checks.add(RecordsContext.equal("Human bidding controls stay disabled during an AI bid",true,buttons>0&&disabled));
            }
            c.check("Active seat "+seat+", "+(bidding?"bid":"play")+", "+language+", font 1.3",checks.toArray(atf.reporting.contract.AtfAssertion[]::new));
        }
    }

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
                        "Declared journey: "+c.trace.name()+". Android API "+c.setup.path("api").asText()
                                +"; viewports are recorded with each setup action. Full-history and final-card preconditions are prepared by the JVM engine;"
                                +" UI actions, comparisons and restoration have individual recorded verdicts."
                                +" This is not a complete match played through UI. Overall outcome: "+c.trace.status()+".",
                        c.evidence,c.trace);
            }catch(Throwable reporting){if(failure==null)failure=reporting;else failure.addSuppressed(reporting);}
        }
        if(failure!=null)throw failure;
    }
}
