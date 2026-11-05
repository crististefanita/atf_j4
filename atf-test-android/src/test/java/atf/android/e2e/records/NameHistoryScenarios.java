package atf.android.e2e.records;

import static atf.android.e2e.records.RecordsContext.equal;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

final class NameHistoryScenarios {
    private NameHistoryScenarios() { }

    static void names(RecordsContext c) throws Exception {
        c.phase("SETUP — installed APK and isolated compact Android session");
        c.sandbox(); c.home("EN");
        c.ui.awaitText("New game",Duration.ofSeconds(15));
        String installed=c.raw("shell","pm","path",RecordsContext.PACKAGE).replace("package:","").trim();
        String digest=c.raw("shell","sha256sum",installed).split("\\s+")[0];
        c.check("Installed requested APK on Android API 28",
                equal("Installed APK SHA-256",c.setup.path("requestedApkSha256").asText(),digest),
                equal("Android API","28",c.raw("shell","getprop","ro.build.version.sdk")),
                equal("Initial home is visible",true,c.ui.hasText(c.ui.dump(),"New game")));

        c.phase("TEST BODY — player name, reset and randomized short AI names");
        c.ui.tapDescription("Your name: You");
        c.typeName("E2EUser");
        c.check("Short name entered through the menu",
                equal("Name field contains supplied nickname",true,c.ui.hasText(c.ui.dump(),"E2EUser")));
        c.tap("Save"); c.ui.awaitText("New game",Duration.ofSeconds(10));
        startDefault(c);
        var first=c.state(); List<String> names=names(first);
        c.check("Configured name reaches the game table",
                equal("Human name","E2EUser",first.path("players").get(0).path("name").asText()),
                equal("Human name visible",true,c.ui.hasText(c.ui.dump(),"E2EUser")),
                equal("Four distinct names",4,new HashSet<>(names).size()),
                equal("AI names fit the short-name contract",true,names.subList(1,4).stream().allMatch(name -> name.length()<=4)));
        c.home("EN");
        var resumed=c.state();
        c.check("Name and table identities survive process relaunch",
                equal("Home profile label retained",true,c.ui.hasDescription(c.ui.dump(),"Your name: E2EUser")),
                equal("Player identities",names,names(resumed)),
                equal("Same saved match identity",first.path("matchId").asText(),resumed.path("matchId").asText()));
        c.tap("Resume game");c.ui.awaitText("Place bid",Duration.ofSeconds(15));
        c.ui.tapDescription("Game menu");c.tap("Your name");c.tap("Reset name");
        c.ui.awaitText("Place bid",Duration.ofSeconds(15));
        var reset=c.state();
        c.check("Name reset from the in-game menu preserves the match",
                equal("Reset human label","You",reset.path("players").get(0).path("name").asText()),
                equal("Match identity unchanged",first.path("matchId").asText(),reset.path("matchId").asText()),
                equal("Human hand unchanged",first.path("players").get(0).path("hand"),reset.path("players").get(0).path("hand")),
                equal("Scores unchanged",totals(first),totals(reset)));
        c.home("EN");startDefault(c);var second=c.state();
        List<String> secondNames=names(second);
        if(names.subList(1,4).equals(secondNames.subList(1,4))) { c.home("EN");startDefault(c);second=c.state();secondNames=names(second); }
        c.check("A new game assigns a fresh short AI roster",
                equal("New match identity differs",true,!first.path("matchId").asText().equals(second.path("matchId").asText())),
                equal("Observed AI rosters differ",true,!names.subList(1,4).equals(secondNames.subList(1,4))),
                equal("AI roster: first game",true,names.subList(1,4).stream().allMatch(name -> name.length()<=4)),
                equal("AI roster: new game",true,secondNames.subList(1,4).stream().allMatch(name -> name.length()<=4)),
                equal("Distinct new names",4,new HashSet<>(secondNames).size()));
        c.home("EN");c.tap("High scores");
        var book=c.scores();
        boolean seeded=true;for(var profile:book.path("profiles"))seeded&=profile.path("entries").size()==10
                && java.util.stream.StreamSupport.stream(profile.path("entries").spliterator(),false).allMatch(entry -> entry.path("sample").asBoolean());
        c.check("Initial local rankings identify their sample values",
                equal("Six baseline game variants",6,book.path("profiles").size()),
                equal("Ten explicitly marked samples per baseline profile",true,seeded),
                equal("Storage capacity",20,book.path("capacity").asInt()),
                equal("Visible sample label",true,c.ui.hasTextContaining(c.ui.dump(),"Sample")));
        c.tap("Close");
    }

    static void history(RecordsContext c) throws Exception {
        history(c,"complete chronological table");
    }

    static void history(RecordsContext c,String scope) throws Exception {
        c.phase("TEST BODY — "+scope+", all seats and both languages");
        var before=c.scores();var fixture=RecordsFixtures.history();
        c.inject(fixture,"history-"+scope.replaceAll("[^A-Za-z0-9]+","-")+"-24-jvm-played-hands");
        c.home("EN");c.tap("Resume game");c.ui.awaitText("Game complete",Duration.ofSeconds(15));
        c.tap("Score history");
        String first=description(fixture.json(),1,0,false);
        var reference=c.ui.boundsForDescription(c.ui.dump(),first);
        var handHeading=c.ui.boundsForText(c.ui.dump(),"Hand");
        var playerHeading=c.ui.boundsForText(c.ui.dump(),"HistUser");
        String densityText=c.raw("shell","wm","density");
        var dpi=java.util.regex.Pattern.compile("(?:Override|Physical) density:\\s*(\\d+)").matcher(densityText);
        int density=0;while(dpi.find())density=Integer.parseInt(dpi.group(1));
        if(density==0)throw new IllegalStateException("No observed Android density");
        int cellWidth=reference.right()-reference.left(),cellHeight=reference.bottom()-reference.top();
        c.check("First recorded hand is readable in the shared table",
                equal("Declared precondition: 24 hands played by the JVM engine",24,fixture.game().scoreHistory().completedHands()),
                equal("First hand, first seat, exact bid points and total",true,c.ui.hasDescription(c.ui.dump(),first)),
                equal("Human column heading",true,c.ui.hasText(c.ui.dump(),"HistUser")),
                equal("Hand column contains only card counts",true,RecordsRunContract.historyCardCounts(c.ui.dump(),"Hand",8)),
                equal("Player separator width",Math.round(3*density/160f),playerHeading.left()-handHeading.right()),
                equal("Player heading and first cell left edge",playerHeading.left(),reference.left()),
                equal("Player heading and first cell right edge",playerHeading.right(),reference.right()),
                equal("Final diagnostic fixture does not enter rankings",before,c.scores()));
        String last=description(fixture.json(),24,0,false);
        c.revealDescription(last,false,cellHeight);
        c.check("Vertical scrolling reaches the final hand",
                equal("Hand 24 values visible",true,c.ui.hasDescription(c.ui.dump(),last)),
                equal("Ledger still contains 24 completed hands",24,c.state().path("scores").path("hands").size()));
        for(int seat=1;seat<4;seat++) {
            String lastSeat=description(fixture.json(),24,seat,false);
            c.revealDescription(lastSeat,true,cellWidth);
            String name=fixture.json().path("players").get(seat).path("name").asText();
            c.check("Horizontal scrolling verifies player column "+(seat+1)+" without losing row ownership",
                    equal("Observed player heading",true,c.ui.hasText(c.ui.dump(),name)),
                    equal("Hand 24 bid points and total",true,c.ui.hasDescription(c.ui.dump(),lastSeat)),
                    equal("Fixture match identity preserved",fixture.json().path("matchId").asText(),c.state().path("matchId").asText()));
        }
        c.home("RO");c.tap("Reia partida");c.ui.awaitText("Partidă încheiată",Duration.ofSeconds(15));c.tap("Istoricul scorului");
        c.check("Romanian history uses the same complete table and values",
                equal("Romanian hand heading",true,c.ui.hasText(c.ui.dump(),"Mână")),
                equal("Romanian point heading",true,c.ui.hasText(c.ui.dump(),"Puncte")),
                equal("Romanian hand column contains only card counts",true,RecordsRunContract.historyCardCounts(c.ui.dump(),"Mână",8)),
                equal("First-hand values localized",true,c.ui.hasDescription(c.ui.dump(),description(fixture.json(),1,0,true))),
                equal("History exploration does not change totals",totals(fixture.json()),totals(c.state())));
        c.revealDescription(description(fixture.json(),24,0,true),false,cellHeight);
        c.revealDescription(description(fixture.json(),24,3,true),true,cellWidth);
        c.check("Romanian scrolling reaches the final hand and final player",
                equal("Romanian final-hand values localized",true,c.ui.hasDescription(c.ui.dump(),description(fixture.json(),24,3,true))),
                equal("History remains unchanged",fixture.json().path("scores"),c.state().path("scores")));
    }

    static void rankingVisibility(RecordsContext c) throws Exception {
        c.phase("TEST BODY — complete ranking rows at both display limits");
        var book=c.scores();RecordsFixtures.visibleTopTwenty(book);
        c.writeScores(book,"ranking-visibility-10-samples-plus-10-synthetic-retained");
        c.home("EN");c.tap("High scores");c.ui.selectSpinnerOption(1,"Top 20");
        var rows=RecordsFixtures.rankingProfile(book).path("entries");
        String first=SeriesRankingScenarios.rankingDescription(rows.get(0),1);
        var reference=c.ui.boundsForDescription(c.ui.dump(),first);int height=reference.bottom()-reference.top();
        String last=SeriesRankingScenarios.rankingDescription(rows.get(19),20);
        String visible=c.revealDescription(last,false,height);
        c.check("Rank 20 is a complete visible row rather than a clipped accessible node",
                equal("Entire rank 20 row extent",true,RecordsRunContract.visibleDescription(visible,last,false,height)),
                equal("Twenty stored entries",20,RecordsFixtures.rankingProfile(c.scores()).path("entries").size()),
                equal("Visibility does not mutate rankings",book,c.scores()));
        c.ui.selectSpinnerOption(1,"Top 10");
        String tenth=SeriesRankingScenarios.rankingDescription(rows.get(9),10);
        visible=c.revealDescription(tenth,false,height);
        c.check("Rank 10 is fully visible and the other ten entries remain stored",
                equal("Entire rank 10 row extent",true,RecordsRunContract.visibleDescription(visible,tenth,false,height)),
                equal("Display limit preserves the book",book,c.scores()));
        c.tap("Close");
    }

    static void startDefault(RecordsContext c) throws Exception {
        c.tap("New game");c.ui.awaitText("Start game",Duration.ofSeconds(10));c.tap("Start game");
        c.ui.awaitText("Place bid",Duration.ofSeconds(20));
    }
    static List<String> names(JsonNode state) { var values=new ArrayList<String>();state.path("players").forEach(p->values.add(p.path("name").asText()));return values; }
    static List<Integer> totals(JsonNode state) { var values=new ArrayList<Integer>();state.path("players").forEach(p->values.add(p.path("totalScore").asInt()));return values; }
    static String description(JsonNode state,int hand,int seat,boolean romanian) {
        JsonNode scores=state.path("scores");int offset=hand-scores.path("priorHands").asInt()-1;
        int total=scores.path("hands").get(offset).get(seat).asInt();
        int prior=offset==0?scores.path("opening").get(seat).asInt():scores.path("hands").get(offset-1).get(seat).asInt();
        int points=total-prior,bid=scores.path("bids").get(offset).get(seat).asInt();
        String name=state.path("players").get(seat).path("name").asText();
        if(seat==0&&name.equals("You"))name=romanian?"Tu":"You";
        return name+", "+(romanian?"Mână":"Hand")+" "+hand+", "+(romanian?"Licitat":"Bid")+" "+bid
                +", "+(romanian?"Puncte":"Points")+" "+(points>0?"+":"")+points+", Total "+total;
    }
}
