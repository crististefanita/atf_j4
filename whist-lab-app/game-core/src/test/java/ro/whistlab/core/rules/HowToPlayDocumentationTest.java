package ro.whistlab.core.rules;

import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;

import static org.junit.Assert.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** Small executable rule examples, with independent literal expectations and captured comparisons. */
@RunWith(Parameterized.class)
public class HowToPlayDocumentationTest {
    @Parameterized.Parameters(name="language={0}, example={1}") public static Collection<Object[]> cases(){
        var cases=new ArrayList<Object[]>();for(var language:HowToPlay.Language.values())for(String example:List.of("follow-after-trump","mandatory-undertrump","void-discard","no-trump-discard","trick-winner","last-bid","exact-zero","over-contract","under-contract","series-five","series-ten","series-missed","one-card-preserve","one-card-break","schedule","deck"))cases.add(new Object[]{language,example});return cases;
    }
    private final HowToPlay.Language language;private final String example;private final List<Map<String,Object>> comparisons=new ArrayList<>();
    public HowToPlayDocumentationTest(HowToPlay.Language language,String example){this.language=language;this.example=example;}
    private void check(String name,Object expected,Object actual){boolean passed=Objects.equals(expected,actual);var row=new LinkedHashMap<String,Object>();row.put("name",name);row.put("expected",expected);row.put("actual",actual);row.put("passed",passed);comparisons.add(row);assertEquals(name,expected,actual);}
    private static Card c(Suit suit,int rank){return new Card(suit,rank);}
    @Test public void documentedExampleMatchesTheImplementedRule()throws Exception{
        long begin=System.nanoTime();String start=Instant.now().toString(),status="PASS",failure="";var context=new LinkedHashMap<String,Object>();
        String section=switch(example){case "follow-after-trump"->"following";case "mandatory-undertrump","void-discard","no-trump-discard"->"trumping";case "trick-winner"->"winner";case "last-bid"->"bidding";case "exact-zero","over-contract","under-contract"->"scoring";case "schedule"->"schedule";case "deck"->"deck";default->"series";};
        var help=HowToPlay.sections(language).stream().filter(s->s.id().equals(section)).findFirst().orElseThrow();
        context.put("purpose",help.title());context.put("rule",help.explanation());context.put("profile","Standard Romanian Whist; four players; series bonus explicitly supplied per example");context.put("notation","SPADES=pică; HEARTS=cupă; CLUBS=treflă; DIAMONDS=caro. Rank 14=Ace. Seat indices are 0-based clockwise. Expected values below are literal independent examples, not policy predictions.");
        try{
            switch(example){
                case "follow-after-trump" -> {var hand=List.of(c(Suit.HEARTS,7),c(Suit.SPADES,14));context.put("before","Hearts led; spades trump; another player may already have trumped. Holding 7 hearts and ace spades.");check("Follow hearts even after a trump",List.of(c(Suit.HEARTS,7)),GameRules.legalCards(hand,Suit.HEARTS,Trump.SPADES,GameMode.ROMANIAN_WHIST));}
                case "mandatory-undertrump" -> {var hand=List.of(c(Suit.SPADES,7),c(Suit.SPADES,9),c(Suit.CLUBS,14));context.put("before","Hearts led; already a king of spades in the trick. Holding spades 7 and 9, clubs ace; no hearts.");check("Both lower trumps remain legal; discard is forbidden",hand.subList(0,2),GameRules.legalCards(hand,Suit.HEARTS,Trump.SPADES,GameMode.ROMANIAN_WHIST));}
                case "void-discard","no-trump-discard" -> {var hand=List.of(c(Suit.CLUBS,7),c(Suit.DIAMONDS,14));var trump=example.equals("void-discard")?Trump.SPADES:Trump.NO_TRUMP;context.put("before","Hearts led, holding clubs 7 and diamonds ace; trump="+trump);check("Any held card may be discarded",hand,GameRules.legalCards(hand,Suit.HEARTS,trump,GameMode.ROMANIAN_WHIST));}
                case "trick-winner" -> {var trick=List.of(new PlayedCard(2,c(Suit.HEARTS,14)),new PlayedCard(3,c(Suit.SPADES,7)),new PlayedCard(0,c(Suit.CLUBS,14)),new PlayedCard(1,c(Suit.SPADES,9)));context.put("before",trick.toString());check("Highest spade wins over ace of hearts and off-suit ace",1,GameRules.winningSeat(trick,Trump.SPADES));check("Without trump only led hearts count",2,GameRules.winningSeat(trick,Trump.NO_TRUMP));}
                case "last-bid" -> {context.put("before","Four-card hand; previous bids 0,1,1 give total 2.");check("Dealer may bid 0,1,3,4; final 2 forbidden",List.of(0,1,3,4),GameRules.allowedBids(4,2,true));check("Intermediate equality is legal",List.of(0,1,2,3,4),GameRules.allowedBids(4,2,false));}
                case "exact-zero","over-contract","under-contract" -> {int bid=example.equals("under-contract")?4:example.equals("exact-zero")?0:2,won=example.equals("over-contract")?4:example.equals("exact-zero")?0:2;context.put("before",Map.of("bid",bid,"won",won));check("Exact 0 gives five; misses lose absolute difference",example.equals("exact-zero")?5:-2,GameRules.scoreDelta(won,bid,GameMode.ROMANIAN_WHIST));}
                case "series-five","series-ten","series-missed" -> {int bonus=example.equals("series-five")?5:10,won=example.equals("series-missed")?0:2,bid=example.equals("series-missed")?2:2,streak=example.equals("series-missed")?-4:4;context.put("before",Map.of("handSize",4,"won",won,"bid",bid,"previousStreak",streak,"configuredPositiveBonus",bonus));var actual=RomanianScoring.score(won,bid,4,streak,bonus,bonus);check("Fifth eligible result awards the selected magnitude in either direction",example.equals("series-missed")?-bonus:bonus,actual.streakPoints());check("Paid series resets",0,actual.nextStreak());check("Base points stay separate",example.equals("series-missed")?-2:7,actual.basePoints());}
                case "one-card-preserve","one-card-break" -> {int won=example.equals("one-card-preserve")?0:1;context.put("before",Map.of("handSize",1,"bid",0,"won",won,"previousStreak",4));var result=RomanianScoring.score(won,0,1,4,10);check("One-card result never pays a series",0,result.streakPoints());check("Matching result preserves four; opposite clears",example.equals("one-card-preserve")?4:0,result.nextStreak());}
                case "schedule" -> {context.put("before","Four-player standard Romanian schedule");var sizes=GameRules.handSizes(GameMode.ROMANIAN_WHIST,4);check("All 24 hands in exact order",List.of(1,1,1,1,2,3,4,5,6,7,8,8,8,8,7,6,5,4,3,2,1,1,1,1),sizes);check("Total tricks",94,sizes.stream().mapToInt(Integer::intValue).sum());}
                case "deck" -> {context.put("before","Reduced decks for three through six players");for(int players=3;players<=6;players++){var deck=GameRules.deck(GameMode.ROMANIAN_WHIST,players);check("Deck count with "+players+" players",players*8,deck.size());check("Minimum rank with "+players+" players",new int[]{9,7,5,3}[players-3],deck.stream().mapToInt(Card::rank).min().orElseThrow());}}
                default -> throw new AssertionError("Unknown example");
            }
            check("Both languages cover the same ten stable rule topics",List.of("goal","deck","schedule","bidding","following","trumping","winner","scoring","series","variants"),HowToPlay.sections(language).stream().map(HowToPlay.Section::id).toList());
            String docs=System.getProperty("whist.documentationMarkdown","");if(!docs.isBlank()){Files.createDirectories(Path.of(docs));Files.writeString(Path.of(docs,"WHIST_"+language+".md"),HowToPlay.markdown(language));}
        }catch(AssertionError e){status="FAIL";failure=e.toString();throw e;}catch(Exception e){status="ERROR";failure=e.toString();throw e;}
        finally{String output=System.getProperty("whist.documentationEvidence","");if(!output.isBlank()){var receipt=new LinkedHashMap<String,Object>();String id="help-"+language+"-"+example;receipt.put("schemaVersion",1);receipt.put("runId",System.getProperty("whist.documentationRunId","documentation"));receipt.put("invocationId",id);receipt.put("className",getClass().getName());receipt.put("methodName","documentedExampleMatchesTheImplementedRule");receipt.put("parameters",Map.of("language",language,"example",example,"collection","documentation"));receipt.put("startedAt",start);receipt.put("endedAt",Instant.now().toString());receipt.put("durationNanos",System.nanoTime()-begin);receipt.put("status",status);receipt.put("failure",failure);receipt.put("checkpoints",List.of(Map.of("owner",id+"/"+section,"title",help.title()+" — "+example,"context",context,"comparisons",comparisons)));receipt.put("results",List.of());Files.createDirectories(Path.of(output,"invocations"));Files.writeString(Path.of(output,"invocations",id+".json"),new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(receipt)+"\n");}}
    }
}
