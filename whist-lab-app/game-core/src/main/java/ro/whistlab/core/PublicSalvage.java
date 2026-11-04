package ro.whistlab.core;

import java.util.*;

/** A rare, public dominance correction; no fitted opponent behavior or sampled confidence. */
final class PublicSalvage {
    static final int NODE_LIMIT = 40000;
    private PublicSalvage() { }
    record Quota(int needed, int opportunities, int forcedMasterTrumps, boolean impossible) { }
    record Proof(Card card, boolean certified, int worlds, int nodes,
                 int ownFloor, int rivalMissFloor, int baselineRivalMissCeiling) { }

    static Quota quota(MonteCarloAi.View v) {
        var own=v.seats().get(v.seat());
        int left=(v.seats().stream().mapToInt(MonteCarloAi.Seat::remaining).sum()+v.trick().size())/v.seats().size();
        int need=own.bid()==null?0:own.bid()-own.won();
        var masters=new ArrayList<Card>();
        if(v.trump()!=Trump.NO_TRUMP) {
            var unseen=MonteCarloAi.unknownCards(v);
            for(Card c:v.hand())if(c.suit().ordinal()==v.trump().ordinal()
                    &&unseen.stream().noneMatch(u->u.suit()==c.suit()&&u.rank()>c.rank()))masters.add(c);
        }
        int forced=masters.size();
        if(forced>0&&!v.trick().isEmpty()) {
            var legal=GameRules.legalCards(v.hand(),v.trick().get(0).card().suit(),v.trump(),v.mode());
            // A trump on the unfinished table is removed from the unknown pool, but is still active.
            // One legally playable master may be spent losing beneath it; the retained masters remain wins.
            boolean canLoseOne=masters.stream().filter(legal::contains).anyMatch(c->v.trick().stream()
                    .anyMatch(p->p.card().suit()==c.suit()&&p.card().rank()>c.rank()));
            if(canLoseOne)forced--;
        }
        return new Quota(need,left,forced,own.bid()!=null&&(need<forced||need>left));
    }

    static Proof choose(MonteCarloAi.View v,Card selected) { return choose(v,selected,NODE_LIMIT); }
    static Proof choose(MonteCarloAi.View v,Card selected,int limit) {
        if(!v.mode().isRomanian()||v.seats().size()!=4||v.hand().size()!=2||!quota(v).impossible())return none(selected,0,0);
        var legal=GameRules.legalCards(v.hand(),v.trick().isEmpty()?null:v.trick().get(0).card().suit(),v.trump(),v.mode());
        if(legal.size()<2||!legal.contains(selected))return none(selected,0,0);
        var worlds=MonteCarloAi.enumerateSmallDeals(v);
        if(worlds.isEmpty())return none(selected,0,0);
        var search=new Search(v,limit);
        try {
            var baseline=new ArrayList<Bounds>();
            for(var world:worlds)baseline.add(search.after(world,selected));
            for(Card card:legal)if(!card.equals(selected)) {
                boolean dominates=true,strict=false;int floor=Integer.MAX_VALUE,rivalFloor=Integer.MAX_VALUE,oldCeiling=0;
                for(int i=0;i<worlds.size();i++) {
                    Bounds old=baseline.get(i),next=search.after(worlds.get(i),card);
                    if(next.ownMin<old.ownMax||next.missMin<old.missMax){dominates=false;break;}
                    strict|=next.ownMin>old.ownMax||next.missMin>old.missMax;
                    floor=Math.min(floor,next.ownMin);rivalFloor=Math.min(rivalFloor,next.missMin);oldCeiling=Math.max(oldCeiling,old.missMax);
                }
                if(dominates&&strict)return new Proof(card,true,worlds.size(),search.nodes,floor,rivalFloor,oldCeiling);
            }
        } catch(Exhausted incomplete) { return none(selected,worlds.size(),search.nodes); }
        return none(selected,worlds.size(),search.nodes);
    }
    private static Proof none(Card selected,int worlds,int nodes) { return new Proof(selected,false,worlds,nodes,0,0,0); }
    private static final class Exhausted extends RuntimeException { }
    private static final class Bounds {
        int ownMin=Integer.MAX_VALUE,ownMax=Integer.MIN_VALUE,missMin=Integer.MAX_VALUE,missMax=Integer.MIN_VALUE;
        void add(int own,int misses){ownMin=Math.min(ownMin,own);ownMax=Math.max(ownMax,own);missMin=Math.min(missMin,misses);missMax=Math.max(missMax,misses);}
    }
    private static final class Search {
        final MonteCarloAi.View view;final int limit;int nodes;
        Search(MonteCarloAi.View view,int limit){this.view=view;this.limit=limit;}
        Bounds after(List<List<Card>> world,Card first) {
            var hands=new ArrayList<List<Card>>();for(var hand:world)hands.add(new ArrayList<>(hand));
            int[] won=view.seats().stream().mapToInt(MonteCarloAi.Seat::won).toArray();
            var result=new Bounds();play(hands,new ArrayList<>(view.trick()),view.seat(),won,first,result);return result;
        }
        void all(List<List<Card>> hands,List<PlayedCard> table,int actor,int[] won,Bounds result) {
            if(++nodes>limit)throw new Exhausted();
            if(hands.stream().allMatch(List::isEmpty)) {
                if(!table.isEmpty())throw new IllegalStateException("Incomplete terminal trick in public salvage proof");
                int misses=0;for(int s=0;s<won.length;s++)if(s!=view.seat()&&won[s]!=view.seats().get(s).bid())misses++;
                result.add(MonteCarloAi.points(view,view.seat(),won[view.seat()],view.seats().get(view.seat()).bid()),misses);return;
            }
            var legal=new ArrayList<>(GameRules.legalCards(hands.get(actor),table.isEmpty()?null:table.get(0).card().suit(),view.trump(),view.mode()));
            if(legal.isEmpty())throw new IllegalStateException("No legal response in public salvage proof");
            for(Card card:legal)play(hands,table,actor,won,card,result);
        }
        void play(List<List<Card>> hands,List<PlayedCard> table,int actor,int[] won,Card card,Bounds result) {
            int at=hands.get(actor).indexOf(card);if(at<0)throw new IllegalArgumentException("Unheld proof card");
            hands.get(actor).remove(at);int winner=-1;
            try {
                var next=new ArrayList<>(table);next.add(new PlayedCard(actor,card));int turn=(actor+1)%4;
                if(next.size()==4){winner=GameRules.winningSeat(next,view.trump());won[winner]++;turn=winner;next.clear();}
                all(hands,next,turn,won,result);
            } finally {if(winner>=0)won[winner]--;hands.get(actor).add(at,card);}
        }
    }
}
