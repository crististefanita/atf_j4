package ro.whistlab.core.testing;

import ro.whistlab.core.testing.match.OfflineMatchHarness;



import java.util.*;

/** Post-game independent integer oracle. Does not call GameRules, GameSession or either policy. */
public final class RoundFourGameOracle {
    public record Comparison(String name,Object expected,Object actual,boolean passed) { }
    public record Checkpoint(String owner,String title,Object context,List<Comparison> comparisons) { }
    private RoundFourGameOracle(){};
    public @SuppressWarnings("unchecked") static List<Checkpoint> audit(Map<String,Object> game){
        var checks=new ArrayList<Checkpoint>();String id=(String)game.get("id");var hands=(List<Map<String,Object>>)game.get("hands");
        List<Integer> sizes=new ArrayList<>(List.of(1,1,1,1,2,3,4,5,6,7,8,8,8,8,7,6,5,4,3,2,1,1,1,1));
        checks.add(new Checkpoint(id+"/setup","Complete Romanian schedule",Map.of("seed",game.get("seed"),"rules",game.get("rules")),List.of(compare("Hand sizes",sizes,hands.stream().map(h->h.get("size")).toList()))));
        int[] total=new int[4],streak=new int[4];int initial=integer(hands.get(0).get("dealer"));
        for(int h=0;h<hands.size();h++){
            var hand=hands.get(h);int size=integer(hand.get("size")),trump="SHCD".indexOf((String)hand.get("trump")),dealer=(initial+h)%4;
            var rows=new ArrayList<Comparison>();rows.add(compare("Dealer rotation",dealer,hand.get("dealer")));var remaining=new ArrayList<List<String>>();var unique=new HashSet<String>();boolean deck=true;
            for(var p:(List<Map<String,Object>>)hand.get("deal")){var cards=new ArrayList<>((List<String>)p.get("cards"));remaining.add(cards);for(String c:cards)deck&=rank(c)>=7&&unique.add(c);deck&=cards.size()==size;}
            if(size==8)deck&=hand.get("turnedCard")==null&&hand.get("trump").equals("NT");else {String turned=(String)hand.get("turnedCard");deck&=turned!=null&&rank(turned)>=7&&!unique.contains(turned)&&suit(turned)==trump;}
            rows.add(compare("Reduced deck and turned trump",true,deck));
            var bids=(List<Map<String,Object>>)hand.get("bids");var order=new ArrayList<Integer>();int[] bid=new int[4];int sum=0;boolean legalBids=bids.size()==4;
            for(int b=0;b<bids.size();b++){var x=bids.get(b);int seat=integer(x.get("seat")),value=integer(x.get("bid"));order.add(seat);legalBids&=value>=0&&value<=size&&(b!=3||sum+value!=size);bid[seat]=value;sum+=value;}
            rows.add(compare("Bidding order",List.of((dealer+1)%4,(dealer+2)%4,(dealer+3)%4,dealer),order));rows.add(compare("Bid range and forbidden final sum",true,legalBids));
            int actor=(dealer+1)%4;var expectedActors=new ArrayList<Integer>();var actualActors=new ArrayList<Integer>();var winners=new ArrayList<Integer>();var actualWinners=new ArrayList<Integer>();int[] won=new int[4];boolean legal=true;
            var tricks=(List<Map<String,Object>>)hand.get("tricks");
            for(var trick:tricks){var plays=(List<Map<String,Object>>)trick.get("plays");int lead=-1,best=-1,bestPower=-1;
                for(var play:plays){int seat=integer(play.get("seat"));String card=(String)play.get("card");expectedActors.add(actor);actualActors.add(seat);List<String> held=remaining.get(seat);int ledSuit=lead;var follows=held.stream().filter(c->suit(c)==ledSuit).toList();var cuts=held.stream().filter(c->suit(c)==trump).toList();var allowed=lead<0?held:!follows.isEmpty()?follows:!cuts.isEmpty()?cuts:held;legal&=allowed.contains(card);legal&=held.remove(card);if(lead<0)lead=suit(card);int power=(suit(card)==trump?200:suit(card)==lead?100:0)+rank(card);if(power>bestPower){bestPower=power;best=seat;}actor=(actor+1)%4;}
                winners.add(best);actualWinners.add(integer(trick.get("winner")));if(best>=0)won[best]++;actor=best;
            }
            rows.add(compare("Every held-card, follow-suit and mandatory-trump choice",true,legal));rows.add(compare("Clockwise actors and next leaders",expectedActors,actualActors));rows.add(compare("Trick winners",winners,actualWinners));rows.add(compare("Complete trick count",size,tricks.size()));
            rows.add(compare("Cards remaining",List.of(0,0,0,0),remaining.stream().map(List::size).toList()));if(hand.containsKey("remainingCards"))rows.add(compare("Engine observed remaining cards",List.of(0,0,0,0),hand.get("remainingCards")));
            var expectedScores=new ArrayList<Map<String,Object>>();
            for(int s=0;s<4;s++){boolean exact=won[s]==bid[s];int base=exact?5+won[s]:-Math.abs(won[s]-bid[s]),award=0;streak[s]=exact?Math.max(0,streak[s]):Math.min(0,streak[s]);if(size!=1)streak[s]+=exact?1:-1;if(streak[s]==5){award=10;streak[s]=0;}if(streak[s]==-5){award=-5;streak[s]=0;}total[s]+=base+award;expectedScores.add(OfflineMatchHarness.map("seat",s,"bid",bid[s],"won",won[s],"base",base,"series",award,"total",total[s],"streak",streak[s]));}
            rows.add(compare("Per-seat bid, tricks, base, series, total and reset",expectedScores,hand.get("scores")));
            checks.add(new Checkpoint(id+"/hand-"+(h+1),"Hand "+(h+1)+" / "+size+" cards / trump "+hand.get("trump"),hand,List.copyOf(rows)));
        }
        checks.add(new Checkpoint(id+"/final","Final scores",Map.of("games",1,"hands",hands.size()),List.of(compare("Final score vector",Arrays.stream(total).boxed().toList(),game.get("finalScores")))));return checks;
    }
    private static Comparison compare(String name,Object expected,Object actual){return new Comparison(name,expected,actual,Objects.equals(expected,actual));}
    private static int integer(Object x){return ((Number)x).intValue();}
    private static int suit(String c){return "SHCD".indexOf(c.charAt(c.length()-1));}
    private static int rank(String c){return switch(c.substring(0,c.length()-1)){case "A"->14;case "K"->13;case "Q"->12;case "J"->11;default->Integer.parseInt(c.substring(0,c.length()-1));};}
}
