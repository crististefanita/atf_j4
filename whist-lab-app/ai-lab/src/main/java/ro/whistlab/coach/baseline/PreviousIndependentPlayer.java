package ro.whistlab.coach.baseline;

import java.util.*;

/** Test-only independent player. No engine imports and no representation of opponents' private cards. */
public final class PreviousIndependentPlayer {
    public record Seat(int remaining,Integer bid,int won,int total,int streak) {
        public Seat(int remaining,Integer bid,int won,int total){this(remaining,bid,won,total,0);}
        @Override public int hashCode(){int h=((remaining*31+Objects.hashCode(bid))*31+won)*31+total;return streak==0?h:31*h+streak;}
    }
    public record Play(int seat,int card) { }
    public record Position(int seat,int dealer,int size,int trump,List<Integer> hand,List<Seat> seats,
            List<Play> trick,List<Play> history,int turnedCard,boolean streakBonuses,int streakBonusPoints) {
        public Position(int seat,int dealer,int size,int trump,List<Integer> hand,List<Seat> seats,
                List<Play> trick,List<Play> history,int turnedCard){this(seat,dealer,size,trump,hand,seats,trick,history,turnedCard,false,5);}
        public Position {hand=hand.stream().sorted().toList();seats=List.copyOf(seats);trick=List.copyOf(trick);history=List.copyOf(history);}
        // Preserve the frozen V1 random stream for the classic profile.
        @Override public int hashCode(){int h=((seat*31+dealer)*31+size)*31+trump;h=h*31+hand.hashCode();h=h*31+seats.hashCode();h=h*31+trick.hashCode();h=h*31+history.hashCode();h=h*31+turnedCard;return streakBonuses?h*31+streakBonusPoints:h;}
    }
    public record Choice(int value,String explanation) { }
    private PreviousIndependentPlayer() { }
    public static Choice bid(Position p){
        int prior=p.seats.stream().filter(s->s.bid!=null).mapToInt(s->s.bid).sum();boolean last=p.seats.stream().filter(s->s.bid!=null).count()==p.seats.size()-1;
        double[] values=new double[p.size+1];int samples=0;
        for(int k=0;k<24;k++){
            var world=deal(p,new Random(seed(p,k)));if(world==null)continue;samples++;
            for(int b=0;b<=p.size;b++)if(!last||b+prior!=p.size){int[] goals=goals(p,world,b);int[] won=roll(p,copy(world),-1,goals,new Random(seed(p,k)^0xC0AC4L));values[b]+=score(p,p.seat,won[p.seat],b);}
        }
        int best=-1;for(int b=0;b<=p.size;b++)if((!last||b+prior!=p.size)&&(best<0||values[b]>values[best]))best=b;
        return new Choice(best,"Independent planner; own cards/public facts; "+samples+" possible deals; counterfactual contract values="+Arrays.toString(values));
    }
    public static Choice card(Position p){
        var allowed=legal(p.hand,p.trick.isEmpty()?-1:p.trick.get(0).card/13,p.trump);if(allowed.isEmpty())throw new IllegalArgumentException("No playable card");
        if(allowed.size()==1)return new Choice(allowed.get(0),"Only legal card under independent follow-suit/mandatory-trump rules");
        double[] values=new double[allowed.size()],rivals=new double[allowed.size()];int samples=0;
        int[] goals=p.seats.stream().mapToInt(s->s.bid==null?0:s.bid).toArray();
        for(int k=0;k<48;k++){
            var world=deal(p,new Random(seed(p,k)));if(world==null)continue;samples++;
            for(int c=0;c<allowed.size();c++){
                int[] won=roll(p,copy(world),allowed.get(c),goals,new Random(seed(p,k)^0xC0AC4L));values[c]+=score(p,p.seat,won[p.seat],goals[p.seat]);
                int leader=Integer.MIN_VALUE;for(int s=0;s<won.length;s++)if(s!=p.seat)leader=Math.max(leader,p.seats.get(s).total+score(p,s,won[s],goals[s]));rivals[c]+=leader;
            }
        }
        int best=0;for(int c=1;c<allowed.size();c++)if(values[c]>values[best]+1e-8||Math.abs(values[c]-values[best])<1e-8&&rivals[c]<rivals[best])best=c;
        return new Choice(allowed.get(best),"Independent planner; "+samples+" possible deals; candidates="+allowed+"; contract sums="+Arrays.toString(values)+"; strongest-rival sums="+Arrays.toString(rivals));
    }
    private static int[] goals(Position p,List<List<Integer>> world,int own){
        int[] targets=new int[p.seats.size()];int sum=0;
        for(int i=1;i<=targets.length;i++){int s=(p.dealer+i)%targets.length;Integer known=p.seats.get(s).bid;
            int bid=s==p.seat?own:known!=null?known:estimate(world.get(s),p.trump);
            if(i==targets.length&&known==null&&s!=p.seat&&sum+bid==p.size)bid=bid>0?bid-1:1;
            targets[s]=bid;sum+=bid;
        }return targets;
    }
    private static int estimate(List<Integer> hand,int trump){double value=0;for(int c:hand)value+=(c%13==12?0.92:c%13==11?0.60:c%13==10?0.33:c%13==9?0.15:0.02)+(c/13==trump?0.19:0);return Math.min(hand.size(),(int)Math.round(value));}
    private static List<List<Integer>> deal(Position p,Random r){
        boolean[] used=new boolean[52];for(int c:p.hand)used[c]=true;for(Play x:p.history)used[x.card]=true;for(Play x:p.trick)used[x.card]=true;if(p.turnedCard>=0)used[p.turnedCard]=true;
        var pool=new ArrayList<Integer>();for(int suit=0;suit<4;suit++)for(int rank=13-2*p.seats.size();rank<13;rank++)if(!used[suit*13+rank])pool.add(suit*13+rank);
        boolean[][] voids=new boolean[p.seats.size()][4];for(int i=0;i<p.history.size();i++){Play play=p.history.get(i);int led=p.history.get(i-i%p.seats.size()).card/13;
            if(play.card/13!=led){voids[play.seat][led]=true;if(p.trump>=0&&p.trump<4&&play.card/13!=p.trump)voids[play.seat][p.trump]=true;}}
        for(int attempt=0;attempt<64;attempt++){
            Collections.shuffle(pool,r);var hands=new ArrayList<List<Integer>>();for(int s=0;s<p.seats.size();s++)hands.add(new ArrayList<>());hands.set(p.seat,new ArrayList<>(p.hand));
            var available=new ArrayList<>(pool);boolean valid=true;
            for(int s=0;s<p.seats.size()&&valid;s++)if(s!=p.seat)for(int j=0;j<p.seats.get(s).remaining;j++){
                int at=-1;for(int i=0;i<available.size();i++)if(!voids[s][available.get(i)/13]){at=i;break;}
                if(at<0){valid=false;break;}hands.get(s).add(available.remove(at));
            }if(valid)return hands;
        }return null;
    }
    private static int[] roll(Position p,List<List<Integer>> hands,int first,int[] goals,Random r){
        int n=hands.size(),actor=first<0?(p.dealer+1)%n:p.seat;int[] won=p.seats.stream().mapToInt(Seat::won).toArray();var trick=new ArrayList<>(p.trick);int left=hands.stream().mapToInt(List::size).sum();boolean initial=true;
        while(left-->0){int c=initial&&first>=0?first:greedy(hands.get(actor),trick,p.trump,actor,goals[actor]-won[actor],r);initial=false;
            if(!hands.get(actor).remove(Integer.valueOf(c)))throw new IllegalStateException("Independent player invented a card");trick.add(new Play(actor,c));
            if(trick.size()==n){actor=winner(trick,p.trump);won[actor]++;trick.clear();}else actor=(actor+1)%n;
        }return won;
    }
    private static int greedy(List<Integer> hand,List<Play> trick,int trump,int seat,int needed,Random r){
        var legal=legal(hand,trick.isEmpty()?-1:trick.get(0).card/13,trump);Comparator<Integer> power=Comparator.comparingInt(c->c%13+(c/13==trump?13:0));
        if(trick.isEmpty())return needed>0?legal.stream().max(power).orElseThrow():legal.stream().min(power).orElseThrow();
        var preferred=new ArrayList<Integer>();for(int c:legal){var test=new ArrayList<>(trick);test.add(new Play(seat,c));if((winner(test,trump)==seat)==(needed>0))preferred.add(c);}
        if(preferred.isEmpty())return needed>0?legal.stream().min(power).orElseThrow():legal.stream().max(power).orElseThrow();
        // When avoiding tricks, discard dangerous high losers while another player owns the trick.
        return needed>0?preferred.stream().min(power).orElseThrow():preferred.stream().max(power).orElseThrow();
    }
    public static List<Integer> legal(List<Integer> hand,int led,int trump){if(led<0)return hand;var follows=hand.stream().filter(c->c/13==led).toList();if(!follows.isEmpty())return follows;var cuts=hand.stream().filter(c->c/13==trump).toList();return cuts.isEmpty()?hand:cuts;}
    private static int winner(List<Play> t,int trump){Play best=t.get(0);int led=best.card/13;for(Play p:t)if(strength(p.card,led,trump)>strength(best.card,led,trump))best=p;return best.seat;}
    private static int strength(int c,int led,int trump){return (c/13==trump?200:c/13==led?100:0)+c%13;}
    private static int score(Position p,int seat,int won,int bid){
        int base=won==bid?5+won:-Math.abs(won-bid);
        if(!p.streakBonuses||p.size==1)return base;
        int previous=p.seats.get(seat).streak;
        if(won==bid&&previous==4)return base+p.streakBonusPoints;
        if(won!=bid&&previous==-4)return base-5;
        return base;
    }
    private static List<List<Integer>> copy(List<List<Integer>> w){var r=new ArrayList<List<Integer>>();for(var h:w)r.add(new ArrayList<>(h));return r;}
    private static long seed(Position p,int sample){
        int seats=1;for(Seat s:p.seats){int h=((s.remaining*31+Objects.hashCode(s.bid))*31+s.won)*31+s.total;seats=31*seats+h;}
        int h=((p.seat*31+p.dealer)*31+p.size)*31+p.trump;h=h*31+p.hand.hashCode();h=h*31+seats;h=h*31+p.trick.hashCode();h=h*31+p.history.hashCode();h=h*31+p.turnedCard;
        return 0xC0AC42026L+31L*h+sample*0x9e3779b97f4a7c15L;
    }
}
