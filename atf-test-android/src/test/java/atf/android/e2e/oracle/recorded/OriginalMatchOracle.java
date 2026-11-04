package atf.android.e2e.oracle.recorded;

import atf.reporting.contract.AtfAssertion;
import java.util.*;

/** Independent rules oracle: no original implementation or Whist Lab engine is used for calculations. */
public final class OriginalMatchOracle {
    final OriginalWhistState deal;
    final List<List<Integer>> initial;
    public List<Integer> plays=List.of();
    int[] bids;
    public int[] won;
    public int[] totals;
    int nextSeat;
    public int leadSuit=-1;
    List<Integer> trick=new ArrayList<>();
    List<Integer> trickSeats=new ArrayList<>();

    public OriginalMatchOracle(OriginalWhistState deal) {
        this.deal=deal;
        initial=new ArrayList<>();for(int p=0;p<deal.players;p++)initial.add(new ArrayList<>(deal.hand(p)));
        if(initial.stream().mapToInt(List::size).sum()!=deal.players*deal.handSize)
            throw new IllegalStateException("Initial backup must precede all card plays");
    }

    public List<AtfAssertion> verify(OriginalWhistState s, int[] previousTotals) {
        List<AtfAssertion> checks=new ArrayList<>();
        if(deal.bids!=null) {
            List<Integer> preceding=new ArrayList<>();
            for(int seat=(deal.dealer+1)%deal.players;seat!=0;seat=(seat+1)%deal.players)preceding.add(seat);
            List<Integer> submitted=java.util.stream.IntStream.range(0,deal.players).filter(p->deal.bids[p]>=0).boxed().toList();
            check(checks,"Bidders before the human are exactly the seats after the dealer",
                    new TreeSet<>(preceding).toString(),new TreeSet<>(submitted).toString(),new TreeSet<>(preceding).equals(new TreeSet<>(submitted)));
        }
        check(checks,"Hand size, dealer and trump remain stable",deal.handSize+"/"+deal.dealer+"/"+deal.trump,
                s.handSize+"/"+s.dealer+"/"+s.trump,deal.handSize==s.handSize&&deal.dealer==s.dealer&&deal.trump==s.trump);
        check(checks,"Deal contains distinct standard cards",Integer.toString(deal.players*deal.handSize),
                Long.toString(initial.stream().flatMap(List::stream).distinct().count()),
                initial.stream().flatMap(List::stream).distinct().count()==deal.players*deal.handSize
                        &&initial.stream().flatMap(List::stream).allMatch(c->c>=0&&c<52));
        check(checks,"Turned trump card was not dealt","undealt card with suit "+deal.trump,
                OriginalWhistState.card(deal.trumpCard),initial.stream().noneMatch(h->h.contains(deal.trumpCard))
                        &&deal.trumpCard/13==deal.trump);
        if(!s.bidding) {
            if(bids==null)bids=s.bids.clone();
            else check(checks,"Bids remain unchanged throughout the hand",Arrays.toString(bids),Arrays.toString(s.bids),Arrays.equals(bids,s.bids));
            if(!s.handOver)check(checks,"Cumulative score stays unchanged until hand completion",Arrays.toString(previousTotals),
                    Arrays.toString(s.scores[s.handIndex]),Arrays.equals(previousTotals,s.scores[s.handIndex]));
        }
        if(!s.bidding)check(checks,"Last bid cannot make total bids equal hand size","total bids != "+s.handSize,
                Arrays.toString(bids),Arrays.stream(bids).allMatch(b->b>=0&&b<=s.handSize)
                        &&Arrays.stream(bids).sum()!=s.handSize);
        List<List<Integer>> hands=new ArrayList<>();for(var h:initial)hands.add(new ArrayList<>(h));
        nextSeat=(deal.dealer+1)%deal.players;won=new int[deal.players];
        trick=new ArrayList<>();trickSeats=new ArrayList<>();leadSuit=-1;
        List<Integer> observed=s.plays();
        check(checks,"Observed history preserves every earlier move",plays.toString(),observed.toString(),
                observed.size()>=plays.size()&&observed.subList(0,Math.min(plays.size(),observed.size())).equals(plays));
        for(int i=0;i<observed.size();i++) {
            int card=observed.get(i),seat=nextSeat;
            boolean owned=hands.get(seat).contains(card);
            boolean mustFollow=leadSuit>=0&&hands.get(seat).stream().anyMatch(c->c/13==leadSuit);
            boolean legal=!mustFollow||card/13==leadSuit;
            List<String> legalChoices=hands.get(seat).stream().filter(c->!mustFollow||c/13==leadSuit)
                    .map(OriginalWhistState::card).toList();
            if(i>=plays.size())check(checks,String.format("Move %03d: seat %d plays %s",i+1,seat,OriginalWhistState.card(card)),
                    "Act in seat order; play a held card; follow "+suit(leadSuit)+" when held; legal choices="+legalChoices,
                    "hand before="+hands.get(seat).stream().map(OriginalWhistState::card).toList()
                            +"; played="+OriginalWhistState.card(card)+"; led="+suit(leadSuit)+"; trump="+suit(deal.trump)
                            +"; must follow="+mustFollow+"; owned="+owned+"; legal="+legal,owned&&legal);
            hands.get(seat).remove(Integer.valueOf(card));
            if(trick.isEmpty())leadSuit=card/13;
            trick.add(card);trickSeats.add(seat);
            if(trick.size()==deal.players) {
                int winnerIndex=0;
                for(int j=1;j<trick.size();j++)if(strength(trick.get(j),leadSuit,deal.trump)>
                        strength(trick.get(winnerIndex),leadSuit,deal.trump))winnerIndex=j;
                nextSeat=trickSeats.get(winnerIndex);won[nextSeat]++;
                if(i>=plays.size())check(checks,"Trick "+((i+1)/deal.players)+" winner and next leader",
                        "highest trump, otherwise highest led suit",
                        "cards="+trick.stream().map(OriginalWhistState::card).toList()+"; seats="+trickSeats+"; winner="+nextSeat,true);
                trick.clear();trickSeats.clear();leadSuit=-1;
            }else nextSeat=(seat+1)%deal.players;
        }
        for(int p=0;p<deal.players;p++) {
            check(checks,"Seat "+p+" remaining cards agree with every observed move",new TreeSet<>(hands.get(p)).toString(),
                    new TreeSet<>(s.hand(p)).toString(),new TreeSet<>(hands.get(p)).equals(new TreeSet<>(s.hand(p))));
            if(!s.bidding)check(checks,"Seat "+p+" trick counter agrees with independent winners",Integer.toString(won[p]),
                    Integer.toString(s.won[p]),won[p]==s.won[p]);
        }
        if(!s.handOver&&!s.bidding)check(checks,"Next player follows independent trick winner / rotation",Integer.toString(nextSeat),
                Integer.toString(s.turn),nextSeat==s.turn);
        plays=List.copyOf(observed);
        if(s.handOver) {
            check(checks,"Every card was played once and every trick resolved",Integer.toString(deal.handSize*deal.players),
                    Integer.toString(plays.size()),plays.size()==deal.handSize*deal.players&&trick.isEmpty()
                            &&Arrays.stream(won).sum()==deal.handSize);
            totals=s.scores[s.handIndex].clone();
            for(int p=0;p<deal.players;p++) {
                int bonus=won[p]==bids[p]?10:0,expected=previousTotals[p]+won[p]+bonus;
                check(checks,"Seat "+p+" cumulative score: prior + tricks + exact-bid bonus",
                        previousTotals[p]+" + "+won[p]+" + "+bonus+" = "+expected,
                        Integer.toString(totals[p]),totals[p]==expected);
            }
        }
        return checks;
    }
    int chooseHumanCard(OriginalWhistState s) {
        return s.hand(0).stream().filter(c->leadSuit<0||c/13==leadSuit||s.hand(0).stream().noneMatch(n->n/13==leadSuit))
                .min(Integer::compareTo).orElseThrow();
    }
    public static int strength(int c,int lead,int trump){return (c/13==trump?200:c/13==lead?100:0)+c%13;}
    static String suit(int suit){return suit<0?"no suit led":List.of("Spades","Hearts","Clubs","Diamonds","No trump").get(suit);}
    public static void check(List<AtfAssertion> list,String name,String expected,String actual,boolean passed){
        list.add(new AtfAssertion(name,expected,actual,passed));
    }
}
