package ro.whistlab.core.baseline;
import ro.whistlab.core.*;

import java.util.*;

/** Information-set sampling. The decision API cannot represent opponents' private cards. */
public final class RoundSixMonteCarloAi {
    private static final int SAMPLES = 32;
    private RoundSixMonteCarloAi() { }

    public record Seat(int remaining, Integer bid, int won,int total,int streak) {
        public Seat(int remaining,Integer bid,int won){this(remaining,bid,won,0);}
        public Seat(int remaining,Integer bid,int won,int total){this(remaining,bid,won,total,0);}
    }
    public record View(GameMode mode, Trump trump, int seat, int dealer, int handSize,
            List<Card> hand, List<Seat> seats, List<PlayedCard> trick, List<PlayedCard> history, Card turnedCard, boolean streakBonuses,int streakBonusPoints) {
        public View(GameMode mode, Trump trump, int seat, int dealer, int handSize,
                List<Card> hand, List<Seat> seats, List<PlayedCard> trick, List<PlayedCard> history, Card turnedCard) {
            this(mode,trump,seat,dealer,handSize,hand,seats,trick,history,turnedCard,false,5);
        }
        public View(GameMode mode, Trump trump, int seat, int dealer, int handSize,
                List<Card> hand, List<Seat> seats, List<PlayedCard> trick, List<PlayedCard> history) {
            this(mode,trump,seat,dealer,handSize,hand,seats,trick,history,null);
        }
        public View {
            List<Card> ordered=new ArrayList<>(hand);for(Card c:ordered)Objects.requireNonNull(c,"hand card");
            ordered.sort(Comparator.comparing(Card::suit).thenComparingInt(Card::rank));
            hand=Collections.unmodifiableList(new ArrayList<>(ordered));seats=List.copyOf(seats);trick=List.copyOf(trick);history=List.copyOf(history);
        }
    }
    public record Candidate(Card card, double meanContractPoints, int samples) { }
    public record Decision(Card card, List<Candidate> candidates, String method) {
        public Decision { candidates=List.copyOf(candidates); }
    }

    public static View observe(GameSession.Snapshot game,int seat) {
        return new View(game.mode(),game.trump(),seat,game.dealerSeat(),game.handSize(),
                game.players().get(seat).hand(),game.players().stream()
                        .map(p->new Seat(p.hand().size(),p.bid(),p.tricksWon(),p.totalScore(),p.streak())).collect(java.util.stream.Collectors.toList()),
                game.currentTrick(),game.history(),game.turnedCard(),game.streakBonuses(),game.streakBonusPoints());
    }

    public static int chooseBid(View view) {
        int prior=view.seats().stream().filter(s->s.bid()!=null).mapToInt(s->s.bid()).sum();
        boolean last=view.seats().stream().filter(s->s.bid()!=null).count()==view.seats().size()-1;
        List<Integer> allowed=GameRules.allowedBids(view.handSize(),prior,last);
        if(view.handSize()==1&&view.hand().size()==1&&view.trick().isEmpty()&&view.history().isEmpty()){
            double win=RoundSixOneCardBidBelief.probability(view);int best=allowed.get(0);double value=Double.NEGATIVE_INFINITY;
            for(int bid:allowed){double expected=win*points(view,view.seat(),1,bid)+(1-win)*points(view,view.seat(),0,bid);if(expected>value){value=expected;best=bid;}}return best;
        }
        double[] values=new double[view.handSize()+1];int samples=0;
        for(int sample=0;sample<SAMPLES;sample++) {
            Random random=new Random(seed(view,sample));List<List<Card>> hands=sample(view,random);
            if(hands==null)continue;
            for(int bid:allowed){int[] targets=biddingTargets(view,hands,bid);
                values[bid]+=evaluate(view,hands,null,targets,seed(view,sample)^0xB1D2026L,.5)[0];}
            samples++;
        }
        if(samples==0)return allowed.get(0);
        int best=allowed.get(0);
        for(int bid:allowed)if(values[bid]>values[best]+1e-9)best=bid;
        return best;
    }

    public static Decision chooseCard(View view) {
        return chooseCard(view,.5);
    }
    static Decision chooseCard(View view,double exactWeight) {
        Suit led=view.trick().isEmpty()?null:view.trick().get(0).card().suit();
        List<Card> legal=GameRules.legalCards(view.hand(),led,view.trump(),view.mode());
        if(legal.isEmpty())throw new IllegalArgumentException("No legal card for the observed player");
        if(legal.size()==1)return new Decision(legal.get(0),List.of(),
                "Only legal card; no hidden-deal sampling or score estimate required");
        Card dominant=RoundSixLateDiscard.choose(view);if(dominant!=null)return new Decision(dominant,List.of(),"Two-card same-suit dominance: current trick already lost, or last-seat win fixed; retain high only if one further trick is needed. No hidden cards or samples used.");
        List<Candidate> evaluations=new ArrayList<>();
        int[] targets=view.seats().stream().mapToInt(s->s.bid()==null?0:s.bid()).toArray();
        double[] values=new double[legal.size()],rivals=new double[legal.size()];int samples=0;
        List<List<List<Card>>> exactDeals=enumerateSmallDeals(view);
        int worlds=exactDeals.isEmpty()?SAMPLES:exactDeals.size();
        for(int sample=0;sample<worlds;sample++){
            List<List<Card>> hands=exactDeals.isEmpty()?sample(view,new Random(seed(view,sample))):exactDeals.get(sample);if(hands==null)continue;samples++;
            for(int c=0;c<legal.size();c++){
                double[] value=evaluate(view,hands,legal.get(c),targets,seed(view,sample)^0xCA4D2026L,exactWeight);values[c]+=value[0];rivals[c]+=value[1];
            }
        }
        int best=0;for(int c=0;c<legal.size();c++){
            evaluations.add(new Candidate(legal.get(c),samples==0?Double.NEGATIVE_INFINITY:values[c]/samples,samples));
            if(values[c]>values[best]+1e-9||Math.abs(values[c]-values[best])<1e-9&&rivals[c]<rivals[best])best=c;
        }
        return new Decision(legal.get(best),evaluations,samples+(exactDeals.isEmpty()?" sampled":" exhaustively enumerated")+" feasible common possible deals; public void inference; small-endgame exact/heuristic mixture weight="+exactWeight+"; contract points then strongest-rival tie-break");
    }
    static double oneCardWinProbability(View v){
        Card own=v.hand().get(0);List<Card> pool=unknownCards(v);int threats=0,lower=0;
        boolean ownTrump=own.suit().ordinal()==v.trump().ordinal();
        for(Card c:pool){if(c.suit()==own.suit()&&c.rank()<own.rank())lower++;
            if(ownTrump?c.suit()==own.suit()&&c.rank()>own.rank():c.suit().ordinal()==v.trump().ordinal()||c.suit()==own.suit()&&c.rank()>own.rank())threats++;}
        int opponents=v.seats().size()-1;
        if(ownTrump||v.seat()==(v.dealer()+1)%v.seats().size())return avoid(pool.size(),threats,opponents);
        return lower/(double)pool.size()*avoid(pool.size()-1,threats,opponents-1);
    }
    private static double avoid(int pool,int threats,int draws){double p=1;for(int i=0;i<draws;i++){if(pool-threats-i<=0)return 0;p*=(pool-threats-i)/(double)(pool-i);}return p;}
    static List<List<List<Card>>> enumerateSmallDeals(View v){
        List<Card> pool=unknownCards(v);if(pool.size()>7||v.seats().stream().anyMatch(s->s.remaining()>2))return List.of();
        boolean[][] voids=knownVoids(v);List<List<Card>> hands=new ArrayList<>();for(int s=0;s<v.seats().size();s++)hands.add(new ArrayList<>());hands.set(v.seat(),new ArrayList<>(v.hand()));
        List<List<List<Card>>> result=new ArrayList<>();enumerate(v,pool,voids,hands,0,result);return result.size()>256?List.of():result;
    }
    private static void enumerate(View v,List<Card> available,boolean[][] voids,List<List<Card>> hands,int seat,List<List<List<Card>>> result){
        if(result.size()>256)return;if(seat==hands.size()){result.add(copy(hands));return;}if(seat==v.seat()){enumerate(v,available,voids,hands,seat+1,result);return;}
        choose(v,available,voids,hands,seat,0,v.seats().get(seat).remaining(),new ArrayList<>(),result);
    }
    private static void choose(View v,List<Card> available,boolean[][] voids,List<List<Card>> hands,int seat,int at,int need,List<Card> chosen,List<List<List<Card>>> result){
        if(result.size()>256)return;if(need==0){hands.set(seat,new ArrayList<>(chosen));List<Card> rest=new ArrayList<>(available);rest.removeAll(chosen);enumerate(v,rest,voids,hands,seat+1,result);return;}
        for(int i=at;i<=available.size()-need;i++)if(!voids[seat][available.get(i).suit().ordinal()]){chosen.add(available.get(i));choose(v,available,voids,hands,seat,i+1,need-1,chosen,result);chosen.remove(chosen.size()-1);}
    }
    private static double[] evaluate(View view,List<List<Card>> hands,Card first,int[] targets,long seed,double exactWeight){
        int[] greedy=rollout(view,copy(hands),first,targets,new Random(seed));
        double[] result=utility(view,greedy,targets);
        if(exactWeight>0&&hands.stream().allMatch(h->h.size()<=(hands.size()==4?3:2))){
            double[] exact=utility(view,RoundSixContractEndgame.after(view,hands,first,targets),targets);
            for(int i=0;i<result.length;i++)result[i]=(1-exactWeight)*result[i]+exactWeight*exact[i];
        }return result;
    }
    private static double[] utility(View view,int[] won,int[] targets){int rival=Integer.MIN_VALUE;for(int s=0;s<won.length;s++)if(s!=view.seat())rival=Math.max(rival,view.seats().get(s).total()+points(view,s,won[s],targets[s]));return new double[]{points(view,view.seat(),won[view.seat()],targets[view.seat()]),rival};}

    static int points(View view,int seat,int won,int bid){
        return view.streakBonuses()?RomanianScoring.score(won,bid,view.handSize(),view.seats().get(seat).streak(),view.streakBonusPoints()).totalPoints()
                :GameRules.scoreDelta(won,bid,view.mode());
    }

    private static List<List<Card>> copy(List<List<Card>> hands){List<List<Card>> result=new ArrayList<>();for(List<Card> hand:hands)result.add(new ArrayList<>(hand));return result;}
    static int[] biddingTargets(View view,List<List<Card>> hands,int own){
        int[] targets=new int[hands.size()];int sum=0;
        for(int i=1;i<=hands.size();i++){int seat=(view.dealer()+i)%hands.size();Integer known=view.seats().get(seat).bid();
            int target=seat==view.seat()?own:known!=null?known:estimate(hands.get(seat),view.trump());
            if(i==hands.size()&&seat!=view.seat()&&known==null&&sum+target==view.handSize())target=target>0?target-1:1;
            targets[seat]=target;sum+=target;
        }return targets;
    }
    private static int estimate(List<Card> hand,Trump trump){double value=0;for(Card c:hand)value+=(c.rank()==14?1:c.rank()==13?.7:c.rank()==12?.4:c.rank()==11?.2:.03)+(c.suit().ordinal()==trump.ordinal()?.2:0);return Math.min(hand.size(),(int)Math.round(value));}

    private static long seed(View view,int sample) {
        // Neither wall-clock time nor hidden hands influence decisions. Replays are reproducible.
        long seed=0x51A7C0DEL+sample*0x9E3779B97F4A7C15L;
        seed=seed*31+view.mode().ordinal();seed=seed*31+view.trump().ordinal();seed=seed*31+view.seat();
        seed=seed*31+view.dealer();seed=seed*31+view.handSize();
        for(Card c:view.hand()){seed=seed*31+c.suit().ordinal();seed=seed*31+c.rank();}
        for(Seat s:view.seats()){seed=seed*31+s.remaining();seed=seed*31+(s.bid()==null?-1:s.bid());seed=seed*31+s.won();seed=seed*31+s.total();}
        for(PlayedCard p:view.history()){seed=seed*31+p.seat();seed=seed*31+p.card().suit().ordinal();seed=seed*31+p.card().rank();}
        if(view.turnedCard()!=null){seed=seed*31+view.turnedCard().suit().ordinal();seed=seed*31+view.turnedCard().rank();}
        // Rule rewards affect utility, not the distribution of cards in otherwise identical public states.
        return seed;
    }

    static List<Card> unknownCards(View view) {
        List<Card> pool=new ArrayList<>(GameRules.deck(view.mode(),view.seats().size()));
        pool.removeAll(view.hand());
        for(PlayedCard play:view.history())pool.remove(play.card());
        for(PlayedCard play:view.trick())pool.remove(play.card());
        if(view.turnedCard()!=null)pool.remove(view.turnedCard());
        return pool;
    }

    static List<List<Card>> sample(View view,Random random) {
        List<Card> pool=unknownCards(view);
        boolean[][] voids=knownVoids(view);
        Collections.shuffle(pool,random);List<Integer> slots=new ArrayList<>();
        for(int seat=0;seat<view.seats().size();seat++)if(seat!=view.seat())for(int i=0;i<view.seats().get(seat).remaining();i++)slots.add(seat);
        Collections.shuffle(slots,random);
        int[] matched=new int[pool.size()];Arrays.fill(matched,-1);
        for(int slot=0;slot<slots.size();slot++)if(!augment(slot,slots,pool,voids,matched,new boolean[pool.size()]))return null;
        List<List<Card>> hands=new ArrayList<>();for(int s=0;s<view.seats().size();s++)hands.add(new ArrayList<>());hands.set(view.seat(),new ArrayList<>(view.hand()));
        for(int i=0;i<pool.size();i++)if(matched[i]>=0)hands.get(slots.get(matched[i])).add(pool.get(i));return hands;
    }
    private static boolean[][] knownVoids(View view){
        boolean[][] voids=new boolean[view.seats().size()][4];
        int index=0;Suit led=null;
        for(PlayedCard play:view.history()) {
            if(index%view.seats().size()==0)led=play.card().suit();
            if(play.card().suit()!=led) {
                voids[play.seat()][led.ordinal()]=true;
                if(view.mode().isRomanian()&&view.trump()!=Trump.NO_TRUMP
                        &&play.card().suit().ordinal()!=view.trump().ordinal())voids[play.seat()][view.trump().ordinal()]=true;
            }
            index++;
        }
        return voids;
    }
    private static boolean augment(int slot,List<Integer> owners,List<Card> pool,boolean[][] voids,int[] matched,boolean[] seen){
        for(int c=0;c<pool.size();c++)if(!seen[c]&&!voids[owners.get(slot)][pool.get(c).suit().ordinal()]){
            seen[c]=true;if(matched[c]<0||augment(matched[c],owners,pool,voids,matched,seen)){matched[c]=slot;return true;}
        }return false;
    }

    private static int[] rollout(View view,List<List<Card>> hands,Card first,int[] targets,Random random) {
        int n=hands.size();int[] won=view.seats().stream().mapToInt(Seat::won).toArray();
        List<PlayedCard> trick=new ArrayList<>(view.trick());
        int seat=first==null?(view.dealer()+1)%n:view.seat();
        boolean initial=true;
        int remaining=hands.stream().mapToInt(List::size).sum();
        while(remaining-->0) {
            Card choice=initial&&first!=null?first:greedy(hands.get(seat),trick,view,seat,targets[seat]-won[seat],random);
            if(!hands.get(seat).remove(choice))throw new IllegalStateException("Simulation attempted an unheld card");
            trick.add(new PlayedCard(seat,choice));initial=false;
            if(trick.size()==n){seat=GameRules.winningSeat(trick,view.trump());won[seat]++;trick.clear();}
            else seat=(seat+1)%n;
        }
        if(!trick.isEmpty())throw new IllegalStateException("Simulation ended before a complete trick");
        return won;
    }

    private static Card greedy(List<Card> hand,List<PlayedCard> trick,View view,int seat,int needed,Random random) {
        Suit led=trick.isEmpty()?null:trick.get(0).card().suit();
        List<Card> legal=GameRules.legalCards(hand,led,view.trump(),view.mode());
        Comparator<Card> cost=Comparator.comparingInt((Card c)->c.rank()+(view.trump().suit().filter(s->s==c.suit()).isPresent()?13:0));
        boolean wants=needed>0||!view.mode().isRomanian()&&needed<0;
        if(trick.isEmpty())return wants?legal.stream().max(cost).orElseThrow():legal.stream().min(cost).orElseThrow();
        List<Card> preferred=new ArrayList<>();
        for(Card c:legal) {
            List<PlayedCard> test=new ArrayList<>(trick);test.add(new PlayedCard(seat,c));
            if((GameRules.winningSeat(test,view.trump())==seat)==wants)preferred.add(c);
        }
        List<Card> options=preferred.isEmpty()?legal:preferred;
        // Small rollout-policy diversity prevents every sampled opponent using one fixed tie order.
        if(random.nextInt(8)==0)return options.get(random.nextInt(options.size()));
        return wants?options.stream().min(cost).orElseThrow():options.stream().max(cost).orElseThrow();
    }
}
