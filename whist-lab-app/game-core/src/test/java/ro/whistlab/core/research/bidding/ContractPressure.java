// R13 rejected pressure model: test-only, never an active Hard continuation.
package ro.whistlab.core.research.bidding;

import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;

import java.util.*;

/** Public-information continuation model. Scores rank risk; they are not calibrated probabilities. */
final class ContractPressure {
    private ContractPressure() { }
    static final double TAKE_BASE=2.0, TAKE_URGENCY=8.0, DUCK_PRIORITY=10.0;
    static final double RESERVE_MASTER_COST=3.0, TABLE_BALANCE_WEIGHT=0.10, PRESSURE_MIX=0.25;
    private static final Map<Integer,List<Card>> DECKS=Map.of(3,GameRules.deck(GameMode.ROMANIAN_WHIST,3),4,GameRules.deck(GameMode.ROMANIAN_WHIST,4),5,GameRules.deck(GameMode.ROMANIAN_WHIST,5),6,GameRules.deck(GameMode.ROMANIAN_WHIST,6));
    record State(int needed, int opportunities, int tableDifference, boolean impossible, double density) { }

    static State state(int actor, int[] targets, int[] won, int[] remaining, int tableCards) {
        int n=targets.length, left=(Arrays.stream(remaining).sum()+tableCards)/n;
        int need=targets[actor]-won[actor], table=0;
        for(int s=0;s<n;s++)table+=targets[s]-won[s];
        return new State(need,left,table-left,need<0||need>left,
                left==0?0:Math.max(0,Math.min(1,need/(double)left)));
    }

    static boolean[] seen(MonteCarloAi.View v) {
        boolean[] result=new boolean[52];
        for(var p:v.history())result[id(p.card())]=true;
        for(var p:v.trick())result[id(p.card())]=true;
        if(v.turnedCard()!=null)result[id(v.turnedCard())]=true;
        return result;
    }

    static boolean[][] voids(MonteCarloAi.View v) {
        boolean[][] result=new boolean[v.seats().size()][4];
        Suit led=null;int i=0;
        for(var p:v.history()) {
            if(i++%result.length==0)led=p.card().suit();
            if(p.card().suit()!=led) {
                result[p.seat()][led.ordinal()]=true;
                if(v.mode().isRomanian()&&v.trump()!=Trump.NO_TRUMP
                        &&p.card().suit().ordinal()!=v.trump().ordinal())result[p.seat()][v.trump().ordinal()]=true;
            }
        }
        return result;
    }

    static Card choose(MonteCarloAi.View v,List<Card> hand,List<PlayedCard> trick,int actor,
                       int[] targets,int[] won,int[] remaining,boolean[] played,boolean[][] voids) {
        Suit led=trick.isEmpty()?null:trick.get(0).card().suit();
        var legal=GameRules.legalCards(hand,led,v.trump(),v.mode());
        if(legal.size()==1)return legal.get(0);
        var state=state(actor,targets,won,remaining,trick.size());
        boolean[] unavailable=played.clone();for(Card c:hand)unavailable[id(c)]=true;
        int masters=masterTrumps(v,hand,unavailable);
        boolean reserveSuffices=state.needed()>0&&masters>=state.needed();
        boolean mustTakeAll=state.needed()>=state.opportunities();
        double density=state.density();
        if(state.needed()>0&&!mustTakeAll){double completion=Math.max(0,1-state.opportunities()/(double)v.handSize());density=Math.max(0,Math.min(1,density+TABLE_BALANCE_WEIGHT*(1+completion)*state.tableDifference()/state.opportunities()));}
        Card best=null;double bestValue=Double.NEGATIVE_INFINITY;int bestCost=Integer.MAX_VALUE;
        for(Card c:legal) {
            var table=new ArrayList<>(trick);table.add(new PlayedCard(actor,c));
            boolean winning=GameRules.winningSeat(table,v.trump())==actor;
            double now=winning?survival(v,c,led==null?c.suit():led,actor,targets,won,remaining,unavailable,voids,trick):0;
            double future=survival(v,c,c.suit(),actor,targets,won,remaining,unavailable,voids,List.of());
            boolean master=isMasterTrump(v,c,unavailable);
            double value;
            if(state.needed()<=0) {
                // First avoid the current trick; among losing discards remove future forced winners.
                value=-DUCK_PRIORITY*now+(trick.isEmpty()?0:future);
            } else if(reserveSuffices&&!mustTakeAll&&!master) {
                // Do not spend the contract on a risky extra trick when retained master trumps cover it.
                value=-DUCK_PRIORITY*now+future;
            } else {
                // As the quota approaches all remaining tricks, taking the current trick dominates economy.
                value=(TAKE_BASE+TAKE_URGENCY*density)*now-(1-density)*future;
                if(reserveSuffices&&master&&!mustTakeAll)value-=RESERVE_MASTER_COST;
            }
            int cost=c.rank()+(v.trump().suit().filter(s->s==c.suit()).isPresent()?13:0);
            if(best==null||value>bestValue+1e-12||Math.abs(value-bestValue)<1e-12&&cost<bestCost) {
                best=c;bestValue=value;bestCost=cost;
            }
        }
        return best;
    }

    static int guaranteedOwnTricks(MonteCarloAi.View v) {
        boolean[] unavailable=seen(v);for(Card c:v.hand())unavailable[id(c)]=true;
        return masterTrumps(v,v.hand(),unavailable);
    }
    static boolean irrecoverable(MonteCarloAi.View v) {
        if(v.seats().get(v.seat()).bid()==null)return false;
        int[] bids=v.seats().stream().mapToInt(s->s.bid()==null?0:s.bid()).toArray();
        int[] won=v.seats().stream().mapToInt(MonteCarloAi.Seat::won).toArray(),remaining=v.seats().stream().mapToInt(MonteCarloAi.Seat::remaining).toArray();
        var q=state(v.seat(),bids,won,remaining,v.trick().size());
        return q.impossible()||q.needed()<guaranteedOwnTricks(v);
    }
    static int masterTrumps(MonteCarloAi.View v,List<Card> hand,boolean[] unavailable) {
        int count=0;for(Card c:hand)if(isMasterTrump(v,c,unavailable))count++;return count;
    }
    private static boolean isMasterTrump(MonteCarloAi.View v,Card c,boolean[] unavailable) {
        if(c.suit().ordinal()!=v.trump().ordinal())return false;
        for(int r=c.rank()+1;r<=14;r++)if(!unavailable[c.suit().ordinal()*13+r-2])return false;
        return true;
    }

    /** Product of conditional marginal survival rates; ignores cross-seat dependence. */
    static double survival(MonteCarloAi.View v,Card own,Suit led,int actor,int[] targets,int[] won,
                           int[] remaining,boolean[] unavailable,boolean[][] voids,List<PlayedCard> table) {
        double result=1;boolean[] acted=new boolean[remaining.length];for(var p:table)acted[p.seat()]=true;
        int opportunities=(Arrays.stream(remaining).sum()+table.size())/remaining.length;
        for(int s=0;s<remaining.length;s++)if(s!=actor&&!acted[s]&&remaining[s]>0) {
            int pool=0,ledCount=0,lowLed=0,highLed=0,lowTrump=0,highTrump=0,trumps=0;
            for(Card c:DECKS.get(remaining.length)) {
                if(unavailable[id(c)]||voids[s][c.suit().ordinal()])continue;
                pool++;boolean trump=c.suit().ordinal()==v.trump().ordinal();
                if(trump){trumps++;if(c.rank()>own.rank())highTrump++;else lowTrump++;}
                if(c.suit()==led){ledCount++;if(c.rank()>own.rank())highLed++;else lowLed++;}
            }
            int count=remaining[s];double denominator=combinations(pool,count);if(denominator==0)return 0;
            double voluntary,forced;
            boolean ownTrump=own.suit().ordinal()==v.trump().ordinal();
            if(ownTrump&&own.suit()!=led) {
                voluntary=(combinations(pool-ledCount,count)-combinations(pool-ledCount-highTrump,count))/denominator;
                forced=(combinations(pool-ledCount-lowTrump,count)-combinations(pool-ledCount-trumps,count))/denominator;
            } else {
                voluntary=1-combinations(pool-highLed,count)/denominator;
                forced=(combinations(pool-lowLed,count)-combinations(pool-ledCount,count))/denominator;
                if(!ownTrump&&v.trump()!=Trump.NO_TRUMP) {
                    double cut=(combinations(pool-ledCount,count)-combinations(pool-ledCount-trumps,count))/denominator;
                    voluntary+=cut;forced+=cut;
                }
            }
            // A player at its contract tries to duck when legal; needing every trick maximizes contest.
            double demand=opportunities==0?0:Math.max(0,Math.min(1,(targets[s]-won[s])/(double)opportunities));
            double threat=forced+(voluntary-forced)*demand;
            result*=1-Math.max(0,Math.min(1,threat));
        }
        return result;
    }
    private static double combinations(int available,int count) {
        if(available<count||available<0)return 0;double result=1;
        for(int i=1;i<=count;i++)result=result*(available-i+1)/i;return result;
    }
    static boolean preferSalvage(double points,double misses,double rival,double bestPoints,double bestMisses,double bestRival){
        return points>bestPoints+1e-9||Math.abs(points-bestPoints)<1e-9&&(misses>bestMisses+1e-9||Math.abs(misses-bestMisses)<1e-9&&rival<bestRival);
    }
    static int id(Card c){return c.suit().ordinal()*13+c.rank()-2;}
}
