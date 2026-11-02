package ro.whistlab.core;

import java.util.*;

/** Exact MaxN within one declared possible deal, bounded to at most two remaining cards per seat. */
final class ContractEndgame {
    private ContractEndgame() { }
    static int[] after(MonteCarloAi.View v,List<List<Card>> hands,Card first,int[] targets){
        long[] masks=new long[hands.size()];for(int seat=0;seat<masks.length;seat++)for(Card c:hands.get(seat))masks[seat]|=1L<<id(c);
        int[] won=v.seats().stream().mapToInt(MonteCarloAi.Seat::won).toArray();long table=0;int length=0;
        for(PlayedCard p:v.trick()){table|=(long)id(p.card())<<(6*length++);}
        int leader=length==0?(first==null?(v.dealer()+1)%hands.size():v.seat()):v.trick().get(0).seat();
        int actor=first==null?(v.dealer()+1)%hands.size():v.seat();
        if(first!=null)return play(v,masks,won,targets,actor,leader,table,length,id(first));
        return search(v,masks,won,targets,actor,leader,table,length);
    }
    private static int[] search(MonteCarloAi.View v,long[] hands,int[] won,int[] targets,int actor,int leader,long table,int length){
        boolean empty=true;for(long h:hands)if(h!=0){empty=false;break;}
        if(empty)return won.clone();
        long allowed=legal(hands[actor],length==0?-1:((int)table&63)/13,v.trump(),v.mode());
        int[] best=null;while(allowed!=0){int c=Long.numberOfTrailingZeros(allowed);allowed&=allowed-1;int[] value=play(v,hands,won,targets,actor,leader,table,length,c);
            int points=MonteCarloAi.points(v,actor,value[actor],targets[actor]);
            if(best==null||points>MonteCarloAi.points(v,actor,best[actor],targets[actor])||points==MonteCarloAi.points(v,actor,best[actor],targets[actor])&&rival(value,actor,v,targets)<rival(best,actor,v,targets))best=value;
        }if(best==null)throw new IllegalArgumentException("Endgame actor has no legal card");return best;
    }
    private static int[] play(MonteCarloAi.View v,long[] hands,int[] won,int[] targets,int actor,int leader,long table,int length,int c){
        long bit=1L<<c;if((hands[actor]&bit)==0)throw new IllegalArgumentException("Endgame forced card is not held");hands[actor]^=bit;
        long next=table|((long)c<<(6*length));int[] result;
        if(length+1==hands.length){int best=0,led=((int)next&63)/13;
            for(int i=1;i<hands.length;i++)if(power((int)(next>>>(6*i))&63,led,v.trump())>power((int)(next>>>(6*best))&63,led,v.trump()))best=i;
            int winner=(leader+best)%hands.length;won[winner]++;result=search(v,hands,won,targets,winner,winner,0,0);won[winner]--;
        }else result=search(v,hands,won,targets,(actor+1)%hands.length,leader,next,length+1);
        hands[actor]|=bit;return result;
    }
    private static int rival(int[] won,int actor,MonteCarloAi.View v,int[] targets){int value=Integer.MIN_VALUE;for(int s=0;s<won.length;s++)if(s!=actor)value=Math.max(value,v.seats().get(s).total()+MonteCarloAi.points(v,s,won[s],targets[s]));return value;}
    private static int id(Card c){return c.suit().ordinal()*13+c.rank()-2;}
    private static int power(int c,int led,Trump trump){return (c/13==trump.ordinal()?200:c/13==led?100:0)+c%13;}
    private static long suit(int index){return ((1L<<13)-1)<<(index*13);}
    private static long legal(long hand,int led,Trump trump,GameMode mode){
        if(led<0)return hand;long follows=hand&suit(led);if(follows!=0)return follows;
        if(mode.isRomanian()&&trump!=Trump.NO_TRUMP){long cuts=hand&suit(trump.ordinal());if(cuts!=0)return cuts;}return hand;
    }
}
