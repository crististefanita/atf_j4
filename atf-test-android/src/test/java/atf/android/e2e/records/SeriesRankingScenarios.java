package atf.android.e2e.records;

import static atf.android.e2e.records.RecordsContext.equal;
import static atf.android.e2e.records.NameHistoryScenarios.names;
import static atf.android.e2e.records.NameHistoryScenarios.totals;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class SeriesRankingScenarios {
    private SeriesRankingScenarios() { }

    static void series(RecordsContext c,int magnitude) throws Exception {
        c.phase("TEST BODY — menu and actual Android series payment ±"+magnitude);
        c.home("EN");c.tap("New game");c.ui.awaitText("Start game",Duration.ofSeconds(12));
        c.ui.selectNamedSpinnerOption("Game type","Romanian (1–8–1)");
        c.ui.selectNamedSpinnerOption("Five-hand reward / penalty","±"+magnitude+" points");
        c.check("Menu selects symmetric "+magnitude+" point series",
                equal("Selected series value","±"+magnitude+" points",c.ui.spinnerValue(c.ui.dump(),"Five-hand reward / penalty")),
                equal("Selected mode","Romanian (1–8–1)",c.ui.spinnerValue(c.ui.dump(),"Game type")));
        c.tap("Start game");c.ui.awaitText("Place bid",Duration.ofSeconds(20));
        var selected=c.state();
        c.check("Menu applies both configured magnitudes to a new match",
                equal("Five-hand series enabled",true,selected.path("streakBonuses").asBoolean()),
                equal("Positive reward",magnitude,selected.path("streakBonusPoints").asInt()),
                equal("Negative penalty magnitude",magnitude,selected.path("streakPenaltyPoints").asInt()));
        var fixture=RecordsFixtures.series(magnitude);c.inject(fixture,"series-"+magnitude+"-controlled-precondition");
        c.home("EN");c.tap("Resume game");c.ui.awaitDescriptionContaining("Legal card",Duration.ofSeconds(15));
        c.check("Controlled two-trick precondition — prior counters 4,4,-4,-4",
                equal("Controlled hand number",20,c.state().path("handIndex").asInt()+1),
                equal("Cards per seat",2,c.state().path("handSize").asInt()),
                equal("Prior counters",List.of(4,4,-4,-4),values(c.state(),"streak")),
                equal("No bonus before completing the hand",List.of(0,0,0,0),values(c.state(),"streakPoints")));
        c.ui.tapDescription(c.ui.firstLegalCardDescription(c.ui.dump()));
        c.awaitState(s -> s.path("players").get(0).path("hand").size()==1&&s.path("currentSeat").asInt()==0,Duration.ofSeconds(20));
        c.ui.awaitDescriptionContaining("Legal card",Duration.ofSeconds(15));
        c.ui.tapDescription(c.ui.firstLegalCardDescription(c.ui.dump()));
        c.ui.awaitText("Hand complete",Duration.ofSeconds(25));var scored=c.state();
        c.observation("series-"+magnitude+"-actual-android-state",scored);
        var expectedTotals=List.of(7+magnitude,5+magnitude,5,-1-magnitude);
        c.check("Android pays +"+magnitude+" and −"+magnitude+" exactly at hand completion",
                equal("Independent contract totals",expectedTotals,totals(scored)),
                equal("Series payments",List.of(magnitude,magnitude,0,-magnitude),values(scored,"streakPoints")),
                equal("Series counters reset or reverse",List.of(0,0,1,0),values(scored,"streak")),
                equal("Eight actual card plays recorded",8,scored.path("history").size()),
                equal("Negative total visible",true,c.ui.hasText(c.ui.dump(),Integer.toString(-1-magnitude))));
        c.home("EN");c.tap("Resume game");c.ui.awaitText("Hand complete",Duration.ofSeconds(15));
        c.check("Relaunch preserves the paid series without adding it again",
                equal("Totals after process relaunch",expectedTotals,totals(c.state())),
                equal("Same match identity",scored.path("matchId").asText(),c.state().path("matchId").asText()),
                equal("Only one scored hand in the controlled ledger",1,c.state().path("scores").path("hands").size()));
    }

    static void ranking(RecordsContext c) throws Exception {
        c.phase("TEST BODY — a real final UI card updates and persists the local top 20");
        var book=c.scores();RecordsFixtures.addRetainedRankingPrecondition(book);
        c.writeScores(book,"ranking-precondition-10-samples-plus-8-synthetic-retained");
        var fixture=RecordsFixtures.finalHumanCard();var before=fixture.game().snapshot();
        var cards=new ArrayList<>(before.currentTrick());
        var human=before.players().get(0).hand().get(0);
        cards.add(new ro.whistlab.core.model.PlayedCard(0,human));
        int led=cards.get(0).card().suit().ordinal(),trump=before.trump().ordinal(),winner=cards.get(0).seat(),best=-1;
        for(var play:cards){int suit=play.card().suit().ordinal();int strength=(suit==trump?200:suit==led?100:0)+play.card().rank();if(strength>best){best=strength;winner=play.seat();}}
        var expectedTotals=new ArrayList<Integer>();
        for(int seat=0;seat<4;seat++)expectedTotals.add(before.players().get(seat).totalScore()+(seat==winner?-1:5));
        c.inject(fixture,"final-card-after-23-jvm-played-hands");
        c.home("EN");c.tap("Resume game");c.ui.awaitDescriptionContaining("Legal card",Duration.ofSeconds(15));
        var declared=RecordsFixtures.rankingProfile(c.scores());
        c.check("Given retained ranking data and one human card before match completion",
                equal("Retained ranking precondition: 10 samples and 8 synthetic previous entries",18,declared.path("entries").size()),
                equal("Completed prior hands were prepared by JVM",23,c.state().path("scores").path("hands").size()),
                equal("Last scheduled hand",24,c.state().path("handIndex").asInt()+1),
                equal("Current actor is human",0,c.state().path("currentSeat").asInt()),
                equal("Exactly one card remains to be played",1,c.state().path("players").get(0).path("hand").size()));
        c.ui.tapDescription(c.ui.firstLegalCardDescription(c.ui.dump()));
        c.ui.awaitText("Game complete",Duration.ofSeconds(25));var scored=c.state();var ranked=c.scores();
        var board=RecordsFixtures.rankingProfile(ranked);String match=fixture.json().path("matchId").asText();
        var expected=new ArrayList<JsonNode>();RecordsFixtures.rankingProfile(book).path("entries").forEach(expected::add);
        List<String> playerNames=names(scored);
        for(int seat=0;seat<4;seat++)expected.add(RecordsFixtures.JSON.createObjectNode().put("name",playerNames.get(seat))
                .put("score",expectedTotals.get(seat)).put("ai",seat!=0).put("sample",false).put("seat",seat)
                .put("matchId","ROMANIAN_WHIST:4:NORMAL:10:10:"+match).put("recordedAt",Long.MAX_VALUE));
        expected.sort(order());expected.subList(20,expected.size()).clear();
        c.observation("final-actual-android-state",scored);c.observation("final-actual-highest-scores",ranked);
        var comparisons=new ArrayList<atf.reporting.contract.AtfAssertion>();
        comparisons.add(equal("Final totals from independent one-trick scoring",expectedTotals,totals(scored)));
        comparisons.add(equal("Exactly twenty retained entries",20,board.path("entries").size()));
        for(int rank=0;rank<20;rank++)comparisons.add(equal("Rank "+(rank+1)+": name, score, AI, sample, seat",
                summary(List.of(expected.get(rank))),summary(List.of(board.path("entries").get(rank)))));
        comparisons.add(equal("All four participants are considered",4,(int)java.util.stream.StreamSupport.stream(board.path("entries").spliterator(),false).filter(entry->entry.path("matchId").asText().endsWith(match)).count()));
        comparisons.add(equal("Completed history contains all hands",24,scored.path("scores").path("hands").size()));
        c.check("One real final card commits the finished match and recomputes the top",comparisons.toArray(atf.reporting.contract.AtfAssertion[]::new));
        c.tap("Main menu");c.tap("High scores");
        c.ui.selectSpinnerOption(1,"Top 20");
        JsonNode first=board.path("entries").get(0),last=board.path("entries").get(19);
        c.check("Top 20 is selected and the highest score is visible",
                equal("Top row matches retained score and role",true,c.ui.hasDescription(c.ui.dump(),rankingDescription(first,1))),
                equal("Selected display limit","Top 20",c.ui.spinnerValue(c.ui.dump(),"High scores · Top 10 / Top 20")));
        c.revealDescription(rankingDescription(last,20),false);
        c.check("The twentieth ranking entry is reachable by scrolling",
                equal("Rank 20 visible",true,c.ui.hasDescription(c.ui.dump(),rankingDescription(last,20))));
        c.ui.selectSpinnerOption(1,"Top 10");
        c.revealDescription(rankingDescription(board.path("entries").get(9),10),false);
        c.check("Top 10 changes presentation without deleting the top 20",
                equal("Rank 10 is reachable",true,c.ui.hasDescription(c.ui.dump(),rankingDescription(board.path("entries").get(9),10))),
                equal("All twenty entries stay stored",20,RecordsFixtures.rankingProfile(c.scores()).path("entries").size()),
                equal("Ranking file unchanged by display limit",ranked,c.scores()));
        c.tap("Close");c.home("EN");c.tap("High scores");
        c.check("Relaunch is idempotent for an already committed match",
                equal("Ranking entries and processed identities unchanged",ranked,c.scores()),
                equal("Match identity preserved",match,c.state().path("matchId").asText()));
        c.ui.selectSpinnerOption(0,"Oh Hell · 4 · Normal");
        c.check("Different game types keep isolated ranking data",
                equal("Oh Hell profile selected","Oh Hell · 4 · Normal",c.ui.spinnerValue(c.ui.dump(),"Game type")),
                equal("A different profile exposes its sample labels",true,c.ui.hasTextContaining(c.ui.dump(),"Sample")),
                equal("Changing visible profile does not mutate stored rankings",ranked,c.scores()));
        c.tap("Close");
    }
    private static List<Integer> values(JsonNode state,String field) {var values=new ArrayList<Integer>();state.path("players").forEach(player->values.add(player.path(field).asInt()));return values;}
    private static Comparator<JsonNode> order() {
        return Comparator.<JsonNode>comparingInt(entry->entry.path("score").asInt()).reversed()
                .thenComparing(entry->entry.path("sample").asBoolean()).thenComparingLong(entry->entry.path("recordedAt").asLong())
                .thenComparing(entry->entry.path("matchId").asText()).thenComparingInt(entry->entry.path("seat").asInt());
    }
    private static List<String> summary(Iterable<JsonNode> entries){var values=new ArrayList<String>();entries.forEach(entry->values.add(entry.path("name").asText()+" | "+entry.path("score").asInt()+" | AI="+entry.path("ai").asBoolean()+" | sample="+entry.path("sample").asBoolean()+" | seat="+entry.path("seat").asInt()));return values;}
    static String rankingDescription(JsonNode row,int rank){String name=row.path("name").asText();if(!row.path("ai").asBoolean()&&name.equals("You"))name="You";return rank+". "+name+(row.path("ai").asBoolean()?" · AI":"")+(row.path("sample").asBoolean()?" · Sample":"")+", "+row.path("score").asInt()+" pts";}
}
