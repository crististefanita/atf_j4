package ro.whistlab.core;

import java.util.*;

/** Test-only guide reference for the independent player. It does not select Expert production moves. */
final class RolloutPolicy {
    private RolloutPolicy() { }
    static Card choose(MonteCarloAi.View view,List<List<Card>> world,List<PlayedCard> trick,int actor,int needed,Random random) {
        Suit led=trick.isEmpty()?null:trick.get(0).card().suit();
        List<Card> legal=GameRules.legalCards(world.get(actor),led,view.trump(),view.mode());
        Comparator<Card> cost=Comparator.comparingInt(c->c.rank()+(c.suit().ordinal()==view.trump().ordinal()?13:0));
        boolean wants=needed>0||!view.mode().isRomanian()&&needed<0;
        var secure=new ArrayList<Card>();var possible=new ArrayList<Card>();
        for(Card card:legal){
            var table=new ArrayList<>(trick);table.add(new PlayedCard(actor,card));
            boolean leading=GameRules.winningSeat(table,view.trump())==actor;
            boolean canBeBeaten=false,mustBeBeaten=false;
            if(leading){Suit suit=table.get(0).card().suit();for(int next=1;next<world.size()-trick.size();next++){
                int seat=(actor+next)%world.size();var responses=GameRules.legalCards(world.get(seat),suit,view.trump(),view.mode());boolean any=false,all=!responses.isEmpty();
                for(Card reply:responses){var completed=new ArrayList<>(table);completed.add(new PlayedCard(seat,reply));boolean beats=GameRules.winningSeat(completed,view.trump())==seat;any|=beats;all&=beats;}
                canBeBeaten|=any;mustBeBeaten|=all;
            }}
            if(wants){if(leading&&!canBeBeaten)secure.add(card);if(leading&&!mustBeBeaten)possible.add(card);}
            else {if(!leading||mustBeBeaten)secure.add(card);if(!leading||canBeBeaten)possible.add(card);}
        }
        if(!secure.isEmpty())return wants?secure.stream().min(cost).orElseThrow():secure.stream().max(cost).orElseThrow();
        if(!possible.isEmpty())return wants?possible.stream().min(cost).orElseThrow():possible.stream().min(cost).orElseThrow();
        return wants?legal.stream().min(cost).orElseThrow():legal.stream().max(cost).orElseThrow();
    }
}
