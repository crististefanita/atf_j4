package ro.whistlab.core;
import static org.junit.Assert.*;
import java.nio.file.*;import java.util.*;import com.fasterxml.jackson.databind.*;import org.junit.Test;
public class LossMiningTest {
 static final ObjectMapper JSON=new ObjectMapper();
 static Card card(String code){String r=code.substring(0,code.length()-1);return new Card(Suit.values()["SHCD".indexOf(code.charAt(code.length()-1))],switch(r){case "A"->14;case "K"->13;case "Q"->12;case "J"->11;default->Integer.parseInt(r);});}
 static String code(Card c){return c.rankLabel()+"SHCD".charAt(c.suit().ordinal());}
 @Test public void minesPublicInformationMistakesFromLostDevelopmentGames()throws Exception {
  Path source=Path.of(System.getProperty("whist.mine.receipts")),corpus=Path.of(System.getProperty("whist.mine.corpus")),out=Path.of(System.getProperty("whist.mine.output"));Files.createDirectories(out);int games=0,positions=0,proofs=0,mistakes=0;var summary=new ArrayList<Map<String,Object>>();
  for(Path receipt:Files.list(source).filter(f->f.getFileName().toString().startsWith("development-")).sorted().toList()){
   var r=JSON.readTree(Files.readAllBytes(receipt));var before=r.path("results").get(0);int hero=r.path("parameters").path("heroSeat").asInt();var totals=before.path("totals");int maximum=0;for(var total:totals)maximum=Math.max(maximum,total.asInt());if(before.path("points").asInt()>=maximum)continue;
   var game=JSON.readTree(Files.readAllBytes(corpus.resolve(before.path("gameId").asText()+".json")));games++;int[] total=new int[4],streak=new int[4];
   for(var hand:game.path("hands")){
    var held=new ArrayList<List<Card>>();for(var deal:hand.path("deal")){var cards=new ArrayList<Card>();for(var c:deal.path("cards"))cards.add(card(c.asText()));held.add(cards);}int[] bids=new int[4],won=new int[4];for(var bid:hand.path("bids"))bids[bid.path("seat").asInt()]=bid.path("bid").asInt();
    var history=new ArrayList<PlayedCard>();var trick=new ArrayList<PlayedCard>();int trump=hand.path("trump").asText().equals("NT")?4:"SHCD".indexOf(hand.path("trump").asText());
    for(var round:hand.path("tricks")){trick.clear();for(var play:round.path("plays")){int actor=play.path("seat").asInt();Card actual=card(play.path("card").asText());
     if(actor==hero&&held.get(hero).size()==2){var seats=new ArrayList<MonteCarloAi.Seat>();for(int s=0;s<4;s++)seats.add(new MonteCarloAi.Seat(held.get(s).size(),bids[s],won[s],total[s],streak[s]));var view=new MonteCarloAi.View(GameMode.ROMANIAN_WHIST,Trump.values()[trump],hero,hand.path("dealer").asInt(),hand.path("size").asInt(),held.get(hero),seats,trick,history,hand.path("turnedCard").isNull()?null:card(hand.path("turnedCard").asText()),true,10);
      positions++;var proof=ForcedContract.prove(view);if(proof.complete()&&!proof.cards().isEmpty()){proofs++;if(!proof.cards().contains(actual)){mistakes++;var data=new LinkedHashMap<String,Object>();data.put("gameId",game.path("id").asText());data.put("role",r.path("parameters").path("role").asText());data.put("handNumber",hand.path("number").asInt());data.put("before",view);data.put("oldChoice",code(actual));data.put("guaranteed",proof.cards().stream().map(LossMiningTest::code).toList());data.put("worlds",proof.worlds());data.put("nodes",proof.nodes());data.put("actualHandOutcome",hand.path("scores").get(hero));String id=String.format("mistake-%03d",mistakes);Files.writeString(out.resolve(id+".json"),JSON.writerWithDefaultPrettyPrinter().writeValueAsString(data)+"\n");summary.add(Map.of("id",id,"role",r.path("parameters").path("role").asText(),"size",hand.path("size").asInt(),"old",code(actual),"safe",proof.cards().stream().map(LossMiningTest::code).toList()));}}
     }
     held.get(actor).remove(actual);var p=new PlayedCard(actor,actual);history.add(p);trick.add(p);
    }won[round.path("winner").asInt()]++;}
    for(var score:hand.path("scores")){int s=score.path("seat").asInt();total[s]=score.path("total").asInt();streak[s]=score.path("streak").asInt();}
   }
  }
  Files.writeString(out.resolve("summary.json"),JSON.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of("lostGames",games,"positions",positions,"nonemptyProofs",proofs,"mistakes",mistakes,"cases",summary))+"\n");assertTrue(games>0);
 }
}
