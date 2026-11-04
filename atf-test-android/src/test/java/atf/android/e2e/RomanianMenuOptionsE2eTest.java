package atf.android.e2e;

import static org.junit.jupiter.api.Assertions.*;
import atf.android.AdbClient;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.reporting.contract.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;

@AndroidE2eTest
class RomanianMenuOptionsE2eTest {
    @Test void originalFiveNewDefaultTenAndClassicOptionPersistFromTheNormalSetupMenu()throws Exception{
        Path out=Path.of(System.getProperty("atf.match.evidenceDir")).toAbsolutePath();Files.createDirectories(out);
        var adb=AdbClient.fromSystemProperties();var ui=new AndroidUiDriver(adb,out);
        var trace=new AndroidUiFlowTrace(out,"romanian-menu-options","Romanian series: menu selects five/ten or classic, then exact resume",getClass().getSimpleName(),"atf.android.e2e","ro.whistlab.app",List.of("android","menu","series","persistence"));
        try{
            adb.run("shell","am","force-stop","ro.whistlab.app").requireSuccess();adb.run("shell","am","start","-W","-n","ro.whistlab.app/.MainActivity").requireSuccess();
            ui.selectNamedSpinnerOption("interface-language", "English");
            ui.tapText("How to play");
            for(String language:List.of("English","Română")){
                ui.selectNamedSpinnerOption("interface-language", language);
                String heading=language.equals("English")?"Mandatory trump when void":"Atu obligatoriu când nu ai culoarea";
                ui.revealText(heading);String help=ui.dump();String content=String.join(" ",ui.texts(help));var helpChecks=new ArrayList<AtfAssertion>();
                RomanianMatchOracle.truth(helpChecks,"Help language exposes mandatory trump rule",heading,content,ui.hasText(help,heading));
                String duty=language.equals("English")?"you must play trump":"trebuie să pui atu";
                String under=language.equals("English")?"overtrumping is not compulsory":"nu este obligatoriu să supratai";
                RomanianMatchOracle.truth(helpChecks,"Help distinguishes compulsory trump from compulsory overtrump",duty+"; "+under,content,content.contains(duty)&&content.contains(under));
                String label=language.equals("English")?"help-en":"help-ro";ui.capture(label);
                CompleteOriginalMatchE2eTest.checkpoint(trace,out,"SETUP","Read Romanian rules in "+language,label,"Normal Help menu; no injected state",helpChecks,List.of());
                ui.back();
                String homeNewGame=language.equals("English")?"New game":"Joc nou";
                String homeHelp=language.equals("English")?"How to play":"Reguli de joc";
                String home=ui.awaitText(homeNewGame,Duration.ofSeconds(12));
                var menuChecks=new ArrayList<AtfAssertion>();
                RomanianMatchOracle.truth(menuChecks,"Selected language also applies to the main menu",
                        homeNewGame,String.join(" ",ui.texts(home)),ui.hasText(home,homeNewGame));
                ui.tapText(homeHelp);
                String title=language.equals("English")?"HELP — HOW TO PLAY":"AJUTOR — CUM SE JOACĂ";String reopened=ui.awaitText(title,Duration.ofSeconds(12));
                helpChecks=new ArrayList<>(menuChecks);RomanianMatchOracle.truth(helpChecks,"Chosen help language survives reopening",title,String.join(" ",ui.texts(reopened)),ui.hasText(reopened,title));ui.capture(label+"-reopen");
                CompleteOriginalMatchE2eTest.checkpoint(trace,out,"SETUP","Reopen the selected help language",label+"-reopen","Back to menu and reopen Help",helpChecks,List.of());
            }
            ui.back();ui.selectNamedSpinnerOption("interface-language","English");ui.awaitText("New game",Duration.ofSeconds(12));
            for(int value:List.of(5,10,0)){
                ui.tapText("New game");ui.selectNamedSpinnerOption("game-type","Romanian (1–8–1)");ui.selectNamedSpinnerOption("ai-level","Hard");
                ui.revealText("10 points");var setup=ui.dump();var checks=new ArrayList<AtfAssertion>();
                RomanianMatchOracle.truth(checks,"New game defaults to positive series ten","10 points",ui.texts(setup).toString(),ui.hasText(setup,"10 points"));
                RomanianMatchOracle.truth(checks,"Series value is labeled separately from AI level","Five-hand streaks + named bonus selector",ui.texts(setup).toString(),ui.hasText(setup,"Five-hand streaks") && ui.hasDescription(setup,"Five-hand exact-contract bonus"));
                if(value==5)ui.selectNamedSpinnerOption("romanian-streak-value","5 points (original)");
                if(value==0){ui.back();ui.tapText("New game");ui.selectNamedSpinnerOption("game-type","Romanian (1–8–1)");ui.tapDescription("Romanian five-hand streaks (missed series: -5)");}
                String label="menu-series-"+value;ui.revealText("Start game");ui.capture(label+"-setup");
                CompleteOriginalMatchE2eTest.checkpoint(trace,out,"SETUP","Select normal Romanian series profile "+value,label+"-setup","No checkpoint editing or injected cards",checks,List.of());
                ui.tapText("Start game");var state=CompleteRomanianExpertE2eTest.stable(adb);checks=new ArrayList<>();
                RomanianMatchOracle.equal(checks,"Menu option reaches the engine",value!=0,state.path("streakBonuses").asBoolean());
                RomanianMatchOracle.equal(checks,"Selected positive series points",value==0?10:value,state.path("streakBonusPoints").asInt());
                RomanianMatchOracle.equal(checks,"Versioned checkpoint",3,state.path("version").asInt());
                ui.capture(label+"-game");Files.writeString(out.resolve(label+"-state.json"),state.toPrettyString());
                CompleteOriginalMatchE2eTest.checkpoint(trace,out,"TEST BODY","Normal menu option saved in the active game "+value,label+"-game",state.toPrettyString(),checks,List.of());
                adb.run("shell","am","force-stop","ro.whistlab.app").requireSuccess();adb.run("shell","am","start","-W","-n","ro.whistlab.app/.MainActivity").requireSuccess();ui.tapText("Resume game");var resumed=CompleteRomanianExpertE2eTest.stable(adb);
                checks=new ArrayList<>();RomanianMatchOracle.equal(checks,"Process restart preserves every card, score and rule option",state.toString(),resumed.toString());ui.capture(label+"-resume");
                CompleteOriginalMatchE2eTest.checkpoint(trace,out,"TEARDOWN","Resume the same selected profile "+value,label+"-resume",resumed.toPrettyString(),checks,List.of());
                ui.back();ui.awaitText("New game",Duration.ofSeconds(12));
            }
            trace.finish(AtfReportStatus.PASS);
        }catch(Throwable failure){trace.fail(failure);throw failure;}
    }
}
