package atf.android.e2e.oracle.romanian;

import atf.android.e2e.oracle.recorded.OriginalWhistState;

import atf.reporting.contract.AtfAssertion;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

/** Independent integer-card model. Never calls Whist Lab's engine, rules or AI. */
public final class RomanianMatchOracle {
    static final List<String> SUITS=List.of("SPADES","HEARTS","CLUBS","DIAMONDS");
    public static List<Integer> schedule(int n){var r=new ArrayList<Integer>();for(int i=0;i<n;i++)r.add(1);for(int i=2;i<=7;i++)r.add(i);for(int i=0;i<n;i++)r.add(8);for(int i=7;i>=2;i--)r.add(i);for(int i=0;i<n;i++)r.add(1);return r;}
    public static int card(JsonNode p){return SUITS.indexOf(p.path("suit").asText())*13+p.path("rank").asInt()-2;}
    public static List<Integer> cards(JsonNode a){var r=new ArrayList<Integer>();a.forEach(c->r.add(card(c)));return r;}
    public static String label(int c){return OriginalWhistState.card(c);}
    static int strength(int c,int led,int trump){return (c/13==trump?200:c/13==led?100:0)+c%13;}
    static List<Integer> legal(List<Integer> hand,int led,int trump){
        if(led<0)return List.copyOf(hand);
        var follows=hand.stream().filter(c->c/13==led).toList();if(!follows.isEmpty())return follows;
        var cuts=hand.stream().filter(c->c/13==trump).toList();return cuts.isEmpty()?List.copyOf(hand):cuts;
    }
    private final int n,size,index,dealer,trump;private final int[] prior,bids,won,priorStreaks;
    private final boolean series;private final int seriesReward,seriesPenalty;
    private final List<List<Integer>> hands=new ArrayList<>();
    private final List<Integer> history=new ArrayList<>(),actors=new ArrayList<>(),trick=new ArrayList<>(),trickSeats=new ArrayList<>();
    private List<Integer> last=List.of(),lastSeats=List.of();private int lastWinner=-1,actor;private boolean bidsKnown;
    public final Properties coverage=new Properties();
    private final String difficulty;
    private final List<String> events=new ArrayList<>();
    public String explanation(){return String.join("\n",events);}
    public RomanianMatchOracle(JsonNode start,int handIndex,int expectedDealer,int[] totals){
        this(start,handIndex,expectedDealer,totals,"EXPERT");
    }
    public RomanianMatchOracle(JsonNode start,int handIndex,int expectedDealer,int[] totals,String difficulty){
        this(start,handIndex,expectedDealer,totals,difficulty,new int[4],false,5);
    }
    public RomanianMatchOracle(JsonNode start,int handIndex,int expectedDealer,int[] totals,String difficulty,int[] streaks,boolean series,int seriesReward){
        this(start,handIndex,expectedDealer,totals,difficulty,streaks,series,seriesReward,5);
    }
    public RomanianMatchOracle(JsonNode start,int handIndex,int expectedDealer,int[] totals,String difficulty,int[] streaks,boolean series,int seriesReward,int seriesPenalty){
        if(seriesPenalty!=5&&seriesPenalty!=10)throw new IllegalArgumentException("Invalid series penalty");
        this.series=series;this.seriesReward=seriesReward;this.seriesPenalty=seriesPenalty;this.priorStreaks=streaks.clone();
        this.difficulty=difficulty;
        n=4;size=schedule(n).get(handIndex);index=handIndex;dealer=expectedDealer;
        trump=SUITS.indexOf(start.path("trump").asText());prior=totals.clone();bids=new int[n];won=new int[n];actor=(dealer+1)%n;
        for(JsonNode p:start.path("players"))hands.add(new ArrayList<>(cards(p.path("hand"))));
    }
    public List<AtfAssertion> start(JsonNode state){
        var a=new ArrayList<AtfAssertion>();equal(a,"RW01 profile","ROMANIAN_WHIST",state.path("mode").asText());
        equal(a,"Selected AI opponents",difficulty,state.path("difficulty").asText());equal(a,"Four seats",n,state.path("players").size());
        equal(a,"RW02 scheduled hand",size,state.path("handSize").asInt());equal(a,"RW02 hand index",index,state.path("handIndex").asInt());
        equal(a,"RW02 dealer rotation",dealer,state.path("dealerSeat").asInt());equal(a,"Fresh hand history",0,state.path("history").size());
        equal(a,"Fresh hand phase","BIDDING",state.path("phase").asText());Set<Integer> unique=new HashSet<>();
        for(int seat=0;seat<n;seat++){
            equal(a,"RW01 seat "+seat+" hand size",size,hands.get(seat).size());
            for(int c:hands.get(seat))truth(a,"RW01 distinct reduced-deck card "+label(c),"rank >= 7 and not duplicated",label(c),c>=0&&c<52&&c%13>=5&&unique.add(c));
        }
        if(size==8){equal(a,"RW02 eight cards means no trump","NO_TRUMP",state.path("trump").asText());truth(a,"No undealt turned card","null",state.path("turnedCard").toString(),state.path("turnedCard").isNull());}
        else {int turned=card(state.path("turnedCard"));truth(a,"RW02 undealt card determines trump","unique reduced-deck card matching trump",state.path("turnedCard").toString(),!state.path("turnedCard").isNull()&&turned>=0&&turned<52&&turned%13>=5&&!unique.contains(turned)&&turned/13==trump);}
        a.addAll(verify(state));return a;
    }
    public List<AtfAssertion> verify(JsonNode state){
        events.clear();var a=new ArrayList<AtfAssertion>();equal(a,"Hand identity",index,state.path("handIndex").asInt());
        equal(a,"Selected five-hand series option",series,state.path("streakBonuses").asBoolean());
        if(series){equal(a,"Selected positive series reward",seriesReward,state.path("streakBonusPoints").asInt());equal(a,"Selected negative series penalty",seriesPenalty,state.path("streakPenaltyPoints").asInt(5));}
        equal(a,"Fixed hand trump",trump,SUITS.indexOf(state.path("trump").asText()));
        int count=0,sum=0;
        for(int i=0;i<n;i++){int seat=(dealer+1+i)%n;JsonNode b=state.path("players").get(seat).path("bid");
            if(!b.isNull()){count++;int bid=b.asInt();truth(a,"RW03 seat "+seat+" bid range","0.."+size,""+bid,bid>=0&&bid<=size);
                if(bidsKnown)equal(a,"RW03 immutable bid seat "+seat,bids[seat],bid);else bids[seat]=bid;
                truth(a,"RW03 clockwise bid prefix","bid position "+i,"submitted "+count,count==i+1);sum+=bid;
            }
        }
        equal(a,"RW03 bid counter",count,state.path("bidsSubmitted").asInt());equal(a,"RW03 bid sum",sum,state.path("bidTotal").asInt());
        if(count==n){truth(a,"RW03 last bid excludes exact total","total != "+size,""+sum,sum!=size);bidsKnown=true;}
        else equal(a,"RW03 next bidding seat",(dealer+1+count)%n,state.path("biddingSeat").asInt());
        JsonNode publicHistory=state.path("history");truth(a,"RW07 history append only","at least "+history.size(),""+publicHistory.size(),publicHistory.size()>=history.size());
        for(int i=0;i<Math.min(history.size(),publicHistory.size());i++){
            equal(a,"RW07 retained card "+i,history.get(i),card(publicHistory.get(i)));equal(a,"RW07 retained actor "+i,actors.get(i),publicHistory.get(i).path("seat").asInt());
        }
        for(int i=history.size();i<publicHistory.size();i++){
            JsonNode p=publicHistory.get(i);int seat=p.path("seat").asInt(),c=card(p),led=trick.isEmpty()?-1:trick.get(0)/13;
            String prefix="Move "+(i+1)+" / "+(n*size)+" — seat "+seat+" "+label(c)+": ";
            equal(a,prefix+"RW03/RW05 actor",actor,seat);
            if(seat<0||seat>=n)break;
            List<Integer> options=legal(hands.get(seat),led,trump);boolean held=hands.get(seat).contains(c),allowed=options.contains(c);
            truth(a,prefix+"card ownership",hands.get(seat).toString(),""+c,held);
            truth(a,prefix+"RW04 legal choice",options.stream().map(RomanianMatchOracle::label).toList().toString(),label(c),allowed);
            if(!held||!allowed)break;
            if(led<0)hit("lead");else if(c/13==led)hit("follow-suit");else if(c/13==trump)hit("mandatory-trump");else hit("void-discard");
            if(trump<0)hit("no-trump-card");
            hands.get(seat).remove(Integer.valueOf(c));history.add(c);actors.add(seat);trick.add(c);trickSeats.add(seat);last=List.of();lastSeats=List.of();lastWinner=-1;
            if(trick.size()==n){int best=0;for(int j=1;j<n;j++)if(strength(trick.get(j),trick.get(0)/13,trump)>strength(trick.get(best),trick.get(0)/13,trump))best=j;
                actor=trickSeats.get(best);won[actor]++;last=List.copyOf(trick);lastSeats=List.copyOf(trickSeats);lastWinner=actor;
                events.add("RW05 completed trick "+(i/n+1)+": "+last.stream().map(RomanianMatchOracle::label).toList()+"; independently calculated winner seat "+actor+". Checked against next public leader and cumulative Android won counters.");
                hit("trick");trick.clear();trickSeats.clear();
            }else actor=(seat+1)%n;
        }
        boolean complete=history.size()==n*size;
        equal(a,"RW07 phase",complete?"SCORE":count==n?"PLAYING":"BIDDING",state.path("phase").asText());
        if(!complete&&count==n)equal(a,"RW05 next actor",actor,state.path("currentSeat").asInt());
        equal(a,"RW05 last winner",lastWinner,state.path("lastTrickWinnerSeat").asInt());
        equal(a,"RW05 current trick cards",trick.toString(),cards(state.path("currentTrick")).toString());
        equal(a,"RW05 last trick cards",last.toString(),cards(state.path("lastTrick")).toString());
        equal(a,"RW05 current trick actors",trickSeats.toString(),seats(state.path("currentTrick")).toString());
        equal(a,"RW05 last trick actors",lastSeats.toString(),seats(state.path("lastTrick")).toString());
        for(int seat=0;seat<n;seat++){
            JsonNode p=state.path("players").get(seat);equal(a,"RW07 seat "+seat+" remaining cards",hands.get(seat).toString(),cards(p.path("hand")).toString());
            equal(a,"RW05 seat "+seat+" won counter",won[seat],p.path("tricksWon").asInt());
            int delta=delta(seat);
            equal(a,"RW06 seat "+seat+" cumulative score",prior[seat]+(complete?delta:0),p.path("totalScore").asInt());
            equal(a,"RS seat "+seat+" consecutive counter",complete?nextStreak(seat):priorStreaks[seat],p.path("streak").asInt());
            equal(a,"RS seat "+seat+" awarded series points",complete?seriesBonus(seat):0,p.path("streakPoints").asInt());
        }
        return a;
    }
    private static List<Integer> seats(JsonNode a){var s=new ArrayList<Integer>();a.forEach(p->s.add(p.path("seat").asInt()));return s;}
    public int[] totals(){int[] t=prior.clone();for(int i=0;i<n;i++)t[i]+=delta(i);return t;}
    public int[] streaks(){int[] r=new int[n];for(int i=0;i<n;i++)r[i]=nextStreak(i);return r;}
    public int seriesBonus(int seat){if(!series||size==1)return 0;return won[seat]==bids[seat]&&priorStreaks[seat]==4?seriesReward:won[seat]!=bids[seat]&&priorStreaks[seat]==-4?-seriesPenalty:0;}
    private int nextStreak(int seat){if(!series)return 0;int wins=Math.max(priorStreaks[seat],0),losses=Math.max(-priorStreaks[seat],0);if(won[seat]==bids[seat]){losses=0;if(size>1)wins++;}else{wins=0;if(size>1)losses++;}return wins==5||losses==5?0:wins-losses;}
    private int delta(int seat){return (won[seat]==bids[seat]?5+won[seat]:-Math.abs(won[seat]-bids[seat]))+seriesBonus(seat);}
    public void scoreCoverage(){for(int i=0;i<n;i++)hit(won[i]==bids[i]?(bids[i]==0?"exact-zero":"exact-positive"):won[i]<bids[i]?"bid-too-high":"bid-too-low");}
    public List<Integer> humanLegal(){return legal(hands.get(0),trick.isEmpty()?-1:trick.get(0)/13,trump);}
    public int moves(){return history.size();}
    private void hit(String key){coverage.setProperty(key,""+(Integer.parseInt(coverage.getProperty(key,"0"))+1));}
    public static void equal(List<AtfAssertion>a,String name,Object expected,Object actual){a.add(new AtfAssertion(name,String.valueOf(expected),String.valueOf(actual),Objects.equals(String.valueOf(expected),String.valueOf(actual))));}
    public static void truth(List<AtfAssertion>a,String name,String expected,String actual,boolean pass){a.add(new AtfAssertion(name,expected,actual,pass));}
}
