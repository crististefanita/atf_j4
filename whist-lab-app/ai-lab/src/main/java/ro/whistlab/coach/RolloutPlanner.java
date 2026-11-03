package ro.whistlab.coach;

import java.util.*;

/** Independent numeric trick-bound policy for simulated deals only. */
final class RolloutPlanner {
    private RolloutPlanner() { }
    static int choose(IndependentPlayer.Position p,List<List<Integer>> hands,List<IndependentPlayer.Play> trick,int actor,int needed,Random random){
        int trump=p.trump(),led=trick.isEmpty()?-1:trick.get(0).card()/13;
        var cards=IndependentPlayer.legal(hands.get(actor),led,trump);var strong=new ArrayList<Integer>();var viable=new ArrayList<Integer>();
        for(int c:cards){int suit=led<0?c/13:led;int strength=power(c,suit,trump);boolean owns=true;for(var play:trick)if(power(play.card(),suit,trump)>strength)owns=false;
            boolean overtaken=false,forced=false;
            if(owns)for(int offset=1;offset<hands.size()-trick.size();offset++){
                var replies=IndependentPlayer.legal(hands.get((actor+offset)%hands.size()),suit,trump);int beating=0;for(int reply:replies)if(power(reply,suit,trump)>strength)beating++;
                overtaken|=beating>0;forced|=!replies.isEmpty()&&beating==replies.size();
            }
            if(needed>0){if(owns&&!overtaken)strong.add(c);if(owns&&!forced)viable.add(c);}else{if(!owns||forced)strong.add(c);if(!owns||overtaken)viable.add(c);}
        }
        Comparator<Integer> cost=Comparator.comparingInt(c->c%13+(c/13==trump?13:0));
        if(!strong.isEmpty())return needed>0?strong.stream().min(cost).orElseThrow():strong.stream().max(cost).orElseThrow();
        if(!viable.isEmpty())return viable.stream().min(cost).orElseThrow();
        return needed>0?cards.stream().min(cost).orElseThrow():cards.stream().max(cost).orElseThrow();
    }
    private static int power(int c,int led,int trump){return(c/13==trump?200:c/13==led?100:0)+c%13;}
}
