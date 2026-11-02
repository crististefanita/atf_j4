package ro.whistlab.core;

import java.util.*;

/** Information-set sampling. The decision API cannot represent opponents' private cards. */
public final class MonteCarloAi {
    private static final int SAMPLES = 32;
    private MonteCarloAi() { }

    public record Seat(int remaining, Integer bid, int won) { }
    public record View(GameMode mode, Trump trump, int seat, int dealer, int handSize,
            List<Card> hand, List<Seat> seats, List<PlayedCard> trick, List<PlayedCard> history, Card turnedCard) {
        public View(GameMode mode, Trump trump, int seat, int dealer, int handSize,
                List<Card> hand, List<Seat> seats, List<PlayedCard> trick, List<PlayedCard> history) {
            this(mode,trump,seat,dealer,handSize,hand,seats,trick,history,null);
        }
        public View {
            hand=List.copyOf(hand);seats=List.copyOf(seats);trick=List.copyOf(trick);history=List.copyOf(history);
        }
    }
    public record Candidate(Card card, double meanContractPoints, int samples) { }
    public record Decision(Card card, List<Candidate> candidates, String method) {
        public Decision { candidates=List.copyOf(candidates); }
    }

    public static View observe(GameSession.Snapshot game,int seat) {
        return new View(game.mode(),game.trump(),seat,game.dealerSeat(),game.handSize(),
                game.players().get(seat).hand(),game.players().stream()
                        .map(p->new Seat(p.hand().size(),p.bid(),p.tricksWon())).collect(java.util.stream.Collectors.toList()),
                game.currentTrick(),game.history(),game.turnedCard());
    }

    public static int chooseBid(View view) {
        int prior=view.seats().stream().filter(s->s.bid()!=null).mapToInt(s->s.bid()).sum();
        boolean last=view.seats().stream().filter(s->s.bid()!=null).count()==view.seats().size()-1;
        List<Integer> allowed=GameRules.allowedBids(view.handSize(),prior,last);
        double[] values=new double[view.handSize()+1];int samples=0;
        for(int sample=0;sample<SAMPLES;sample++) {
            Random random=new Random(seed(view,sample));List<List<Card>> hands=sample(view,random);
            if(hands==null)continue;
            // During bidding, estimate taking potential with greedy opponents. Outcomes are sampled, not injected.
            int[] targets=new int[view.seats().size()];Arrays.fill(targets,view.handSize());
            int[] won=rollout(view,hands,null,targets,random);
            for(int bid:allowed)values[bid]+=GameRules.scoreDelta(won[view.seat()],bid,view.mode());
            samples++;
        }
        if(samples==0)return allowed.get(0);
        int best=allowed.get(0);
        for(int bid:allowed)if(values[bid]>values[best]+1e-9)best=bid;
        return best;
    }

    public static Decision chooseCard(View view) {
        Suit led=view.trick().isEmpty()?null:view.trick().get(0).card().suit();
        List<Card> legal=GameRules.legalCards(view.hand(),led,view.trump(),view.mode());
        if(legal.isEmpty())throw new IllegalArgumentException("No legal card for the observed player");
        List<Candidate> evaluations=new ArrayList<>();
        int[] targets=view.seats().stream().mapToInt(s->s.bid()==null?0:s.bid()).toArray();
        for(Card candidate:legal) {
            double value=0;int samples=0;
            for(int sample=0;sample<SAMPLES;sample++) {
                // Common random numbers compare candidates against the same possible hidden deals.
                Random random=new Random(seed(view,sample));List<List<Card>> hands=sample(view,random);
                if(hands==null)continue;
                int[] won=rollout(view,hands,candidate,targets,random);
                value+=GameRules.scoreDelta(won[view.seat()],targets[view.seat()],view.mode());samples++;
            }
            evaluations.add(new Candidate(candidate,samples==0?Double.NEGATIVE_INFINITY:value/samples,samples));
        }
        Candidate best=evaluations.stream().max(Comparator.comparingDouble(Candidate::meanContractPoints)
                .thenComparingInt(c->-c.card().rank()).thenComparingInt(c->-c.card().suit().ordinal())).orElseThrow();
        return new Decision(best.card(),evaluations,"32 sampled deals; public void inference; full remaining-hand rollout; expected contract points");
    }

    private static long seed(View view,int sample) {
        // Neither wall-clock time nor hidden hands influence decisions. Replays are reproducible.
        long seed=0x51A7C0DEL+sample*0x9E3779B97F4A7C15L;
        seed=seed*31+view.mode().ordinal();seed=seed*31+view.trump().ordinal();seed=seed*31+view.seat();
        seed=seed*31+view.dealer();seed=seed*31+view.handSize();
        for(Card c:view.hand()){seed=seed*31+c.suit().ordinal();seed=seed*31+c.rank();}
        for(Seat s:view.seats()){seed=seed*31+s.remaining();seed=seed*31+(s.bid()==null?-1:s.bid());seed=seed*31+s.won();}
        for(PlayedCard p:view.history()){seed=seed*31+p.seat();seed=seed*31+p.card().suit().ordinal();seed=seed*31+p.card().rank();}
        if(view.turnedCard()!=null){seed=seed*31+view.turnedCard().suit().ordinal();seed=seed*31+view.turnedCard().rank();}
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

    private static List<List<Card>> sample(View view,Random random) {
        List<Card> pool=unknownCards(view);
        boolean[][] voids=new boolean[view.seats().size()][4];
        int index=0;Suit led=null;
        for(PlayedCard play:view.history()) {
            if(index%view.seats().size()==0)led=play.card().suit();
            if(play.card().suit()!=led) {
                voids[play.seat()][led.ordinal()]=true;
                if(view.mode()==GameMode.ROMANIAN_WHIST&&view.trump()!=Trump.NO_TRUMP
                        &&play.card().suit().ordinal()!=view.trump().ordinal())voids[play.seat()][view.trump().ordinal()]=true;
            }
            index++;
        }
        List<Integer> order=new ArrayList<>();
        for(int seat=0;seat<view.seats().size();seat++)if(seat!=view.seat())order.add(seat);
        order.sort(Comparator.comparingInt(s->{int choices=0;for(Card c:pool)if(!voids[s][c.suit().ordinal()])choices++;return choices;}));
        for(int attempt=0;attempt<48;attempt++) {
            List<Card> available=new ArrayList<>(pool);List<List<Card>> hands=new ArrayList<>();
            for(int seat=0;seat<view.seats().size();seat++)hands.add(new ArrayList<>());
            hands.set(view.seat(),new ArrayList<>(view.hand()));boolean valid=true;
            for(int seat:order) {
                for(int slot=0;slot<view.seats().get(seat).remaining();slot++) {
                    List<Card> eligible=new ArrayList<>();
                    for(Card card:available)if(!voids[seat][card.suit().ordinal()])eligible.add(card);
                    if(eligible.isEmpty()){valid=false;break;}
                    Card choice=eligible.get(random.nextInt(eligible.size()));hands.get(seat).add(choice);available.remove(choice);
                }
                if(!valid)break;
            }
            if(valid)return hands;
        }
        // A failed constrained sample is skipped; no impossible hidden deal is substituted.
        return null;
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
        if(trick.isEmpty())return needed>0?legal.stream().max(cost).orElseThrow():legal.stream().min(cost).orElseThrow();
        List<Card> preferred=new ArrayList<>();
        for(Card c:legal) {
            List<PlayedCard> test=new ArrayList<>(trick);test.add(new PlayedCard(seat,c));
            if((GameRules.winningSeat(test,view.trump())==seat)==(needed>0))preferred.add(c);
        }
        List<Card> options=preferred.isEmpty()?legal:preferred;
        // Small rollout-policy diversity prevents every sampled opponent using one fixed tie order.
        if(random.nextInt(8)==0)return options.get(random.nextInt(options.size()));
        return options.stream().min(cost).orElseThrow();
    }
}
