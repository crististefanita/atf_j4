package ro.whistlab.core;

import java.util.*;

/** Bounded information-set AND/OR proof. Unknown deals are never replaced by the real hidden hand. */
final class ForcedContract {
    record Proof(List<Card> cards,int worlds,int nodes,boolean complete) { Proof {cards=List.copyOf(cards);} }
    private static final int WORLD_LIMIT=512,NODE_LIMIT=40000;
    private static final class Budget extends RuntimeException { }
    private final int hero,target,trump,n;private int nodes;private final Map<String,Boolean> memo=new HashMap<>();
    private ForcedContract(MonteCarloAi.View view){hero=view.seat();target=view.seats().get(hero).bid();trump=view.trump().ordinal();n=view.seats().size();}
    static Proof prove(MonteCarloAi.View view){
        if(!view.mode().isRomanian()||view.seats().size()!=4||view.hand().size()!=2||view.seats().stream().anyMatch(s->s.bid()==null||s.remaining()>2)||MonteCarloAi.unknownCards(view).size()>10)return new Proof(List.of(),0,0,false);
        List<long[]> worlds=worlds(view);if(worlds.isEmpty()||worlds.size()>WORLD_LIMIT)return new Proof(List.of(),worlds.size(),0,false);
        var search=new ForcedContract(view);long table=0;for(int i=0;i<view.trick().size();i++)table|=(long)id(view.trick().get(i).card())<<(6*i);int leader=view.trick().isEmpty()?view.seat():view.trick().get(0).seat();
        var safe=new ArrayList<Card>();try{for(Card card:GameRules.legalCards(view.hand(),view.trick().isEmpty()?null:view.trick().get(0).card().suit(),view.trump(),view.mode()))if(search.branch(worlds,view.seat(),leader,table,view.trick().size(),view.seats().get(view.seat()).won(),id(card)))safe.add(card);return new Proof(safe,worlds.size(),search.nodes,true);}catch(Budget exhausted){return new Proof(List.of(),worlds.size(),search.nodes,false);}
    }
    private boolean force(List<long[]> worlds,int actor,int leader,long table,int length,int won){
        if(++nodes>NODE_LIMIT)throw new Budget();if(won>target)return false;
        int remaining=0;for(long mask:worlds.get(0))remaining+=Long.bitCount(mask);if(remaining==0)return length==0&&won==target;if(won+(remaining+length)/n<target)return false;
        String key=actor+"/"+leader+"/"+table+"/"+length+"/"+won+"/"+worldKey(worlds);Boolean cached=memo.get(key);if(cached!=null)return cached;
        long choices=0;for(var world:worlds)choices|=legal(world[actor],length==0?-1:((int)table&63)/13,trump);
        boolean answer=actor!=hero;while(choices!=0){int card=Long.numberOfTrailingZeros(choices);choices&=choices-1;boolean value=branch(worlds,actor,leader,table,length,won,card);if(actor==hero&&value){answer=true;break;}if(actor!=hero&&!value){answer=false;break;}}
        memo.put(key,answer);return answer;
    }
    private boolean branch(List<long[]> worlds,int actor,int leader,long table,int length,int won,int card){
        var consistent=new ArrayList<long[]>();long bit=1L<<card;for(var world:worlds)if((legal(world[actor],length==0?-1:((int)table&63)/13,trump)&bit)!=0){var next=world.clone();next[actor]^=bit;consistent.add(next);}if(consistent.isEmpty())return false;
        consistent=deduplicate(consistent);long nextTable=table|((long)card<<(6*length));if(length+1<n)return force(consistent,(actor+1)%n,leader,nextTable,length+1,won);
        int best=0,led=((int)nextTable&63)/13;for(int i=1;i<n;i++)if(power((int)(nextTable>>>(6*i))&63,led,trump)>power((int)(nextTable>>>(6*best))&63,led,trump))best=i;int winner=(leader+best)%n;
        return force(consistent,winner,winner,0,0,won+(winner==hero?1:0));
    }
    private static ArrayList<long[]> deduplicate(List<long[]> worlds){var unique=new TreeMap<String,long[]>();for(var world:worlds)unique.put(Arrays.toString(world),world);return new ArrayList<>(unique.values());}
    private static String worldKey(List<long[]> worlds){var out=new StringBuilder();for(var world:worlds)out.append(Arrays.toString(world));return out.toString();}
    private static long legal(long hand,int led,int trump){if(led<0)return hand;long follows=hand&(((1L<<13)-1)<<(13*led));if(follows!=0)return follows;long cuts=trump<4?hand&(((1L<<13)-1)<<(13*trump)):0;return cuts==0?hand:cuts;}
    private static int power(int c,int led,int trump){return(c/13==trump?200:c/13==led?100:0)+c%13;}
    private static int id(Card c){return c.suit().ordinal()*13+c.rank()-2;}
    private static List<long[]> worlds(MonteCarloAi.View v){
        var pool=MonteCarloAi.unknownCards(v);boolean[][] voids=new boolean[4][4];for(int i=0;i<v.history().size();i++){var play=v.history().get(i);int led=v.history().get(i-i%4).card().suit().ordinal(),suit=play.card().suit().ordinal();if(suit!=led){voids[play.seat()][led]=true;if(v.trump().ordinal()<4&&suit!=v.trump().ordinal())voids[play.seat()][v.trump().ordinal()]=true;}}
        long available=0,own=0;for(Card card:pool)available|=1L<<id(card);for(Card card:v.hand())own|=1L<<id(card);long[] masks=new long[4];masks[v.seat()]=own;var results=new ArrayList<long[]>();enumerate(v,voids,available,masks,0,results);return results;
    }
    private static void enumerate(MonteCarloAi.View v,boolean[][] voids,long available,long[] hands,int seat,List<long[]> result){
        if(result.size()>WORLD_LIMIT)return;if(seat==4){result.add(hands.clone());return;}if(seat==v.seat()){enumerate(v,voids,available,hands,seat+1,result);return;}long eligible=available;for(int suit=0;suit<4;suit++)if(voids[seat][suit])eligible&=~(((1L<<13)-1)<<(13*suit));choose(v,voids,available,eligible,hands,seat,v.seats().get(seat).remaining(),0,result);
    }
    private static void choose(MonteCarloAi.View v,boolean[][] voids,long available,long eligible,long[] hands,int seat,int need,long chosen,List<long[]> result){
        if(result.size()>WORLD_LIMIT)return;if(need==0){hands[seat]=chosen;enumerate(v,voids,available&~chosen,hands,seat+1,result);return;}while(Long.bitCount(eligible)>=need){int card=Long.numberOfTrailingZeros(eligible);long bit=1L<<card;eligible&=eligible-1;choose(v,voids,available,eligible,hands,seat,need-1,chosen|bit,result);}
    }
}
