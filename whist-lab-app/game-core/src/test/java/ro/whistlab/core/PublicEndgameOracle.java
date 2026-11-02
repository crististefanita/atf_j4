package ro.whistlab.core;

import java.util.*;

/** Independent list-based exhaustive oracle for two-card public positions; no GameRules/ForcedContract calls. */
final class PublicEndgameOracle {
 record Outcome(List<String> safe,int worlds,Map<String,Set<Integer>> finalWon,Map<String,Object> witness) { }
 static int id(Card c){return c.suit().ordinal()*13+c.rank()-2;}
 static String code(int c){int rank=c%13+2;return (rank==14?"A":rank==13?"K":rank==12?"Q":rank==11?"J":Integer.toString(rank))+"SHCD".charAt(c/13);}
 static Outcome audit(MonteCarloAi.View v){
  var pool=new ArrayList<Integer>();for(int suit=0;suit<4;suit++)for(int rank=7;rank<=14;rank++)pool.add(suit*13+rank-2);for(Card c:v.hand())pool.remove(Integer.valueOf(id(c)));for(var p:v.history())pool.remove(Integer.valueOf(id(p.card())));for(var p:v.trick())pool.remove(Integer.valueOf(id(p.card())));if(v.turnedCard()!=null)pool.remove(Integer.valueOf(id(v.turnedCard())));
  boolean[][] voids=new boolean[4][4];for(int i=0;i<v.history().size();i++){var p=v.history().get(i);int lead=v.history().get((i/4)*4).card().suit().ordinal(),suit=p.card().suit().ordinal();if(suit!=lead){voids[p.seat()][lead]=true;if(v.trump().ordinal()<4&&suit!=v.trump().ordinal())voids[p.seat()][v.trump().ordinal()]=true;}}
  var worlds=new ArrayList<List<List<Integer>>>();var hands=new ArrayList<List<Integer>>();for(int s=0;s<4;s++)hands.add(new ArrayList<>());hands.set(v.seat(),new ArrayList<>(v.hand().stream().map(PublicEndgameOracle::id).toList()));enumerate(v,voids,pool,hands,0,worlds);
  var table=v.trick().stream().map(p->new int[]{p.seat(),id(p.card())}).toList();var possible=new LinkedHashMap<String,Set<Integer>>();var witness=new LinkedHashMap<String,Object>();int target=v.seats().get(v.seat()).bid();
  for(int card:legal(hands.get(v.seat()),table.isEmpty()?-1:table.get(0)[1]/13,v.trump().ordinal())){var values=new TreeSet<Integer>();for(var world:worlds)walk(v,copy(world),new ArrayList<>(table),v.seat(),v.seats().get(v.seat()).won(),card,values,witness,new ArrayList<>(),target);possible.put(code(card),Set.copyOf(values));}
  return new Outcome(possible.entrySet().stream().filter(e->e.getValue().equals(Set.of(target))).map(Map.Entry::getKey).sorted().toList(),worlds.size(),possible,witness);
 }
 private static void enumerate(MonteCarloAi.View v,boolean[][] voids,List<Integer> pool,List<List<Integer>> hands,int seat,List<List<List<Integer>>> out){if(seat==4){out.add(copy(hands));return;}if(seat==v.seat()){enumerate(v,voids,pool,hands,seat+1,out);return;}choose(v,voids,pool,hands,seat,0,v.seats().get(seat).remaining(),new ArrayList<>(),out);}
 private static void choose(MonteCarloAi.View v,boolean[][] voids,List<Integer> pool,List<List<Integer>> hands,int seat,int start,int need,List<Integer> picked,List<List<List<Integer>>> out){if(need==0){hands.set(seat,new ArrayList<>(picked));var rest=new ArrayList<>(pool);rest.removeAll(picked);enumerate(v,voids,rest,hands,seat+1,out);return;}for(int i=start;i<=pool.size()-need;i++){int c=pool.get(i);if(voids[seat][c/13])continue;picked.add(c);choose(v,voids,pool,hands,seat,i+1,need-1,picked,out);picked.remove(picked.size()-1);}}
 private static void walk(MonteCarloAi.View v,List<List<Integer>> held,List<int[]> trick,int actor,int won,int forced,Set<Integer> results,Map<String,Object> witness,List<String> actions,int target){
  if(held.stream().allMatch(List::isEmpty)){results.add(won);if(won!=target&&witness.isEmpty()){witness.put("finalHeroTricks",won);witness.put("bid",target);witness.put("actions",List.copyOf(actions));}return;}
  var choices=forced>=0?List.of(forced):legal(held.get(actor),trick.isEmpty()?-1:trick.get(0)[1]/13,v.trump().ordinal());for(int card:choices){var next=copy(held);next.get(actor).remove(Integer.valueOf(card));var table=new ArrayList<>(trick);table.add(new int[]{actor,card});var path=new ArrayList<>(actions);path.add("seat "+actor+" plays "+code(card));int turn=(actor+1)%4,count=won;if(table.size()==4){int winner=winner(table,v.trump().ordinal());count+=winner==v.seat()?1:0;turn=winner;table.clear();}walk(v,next,table,turn,count,-1,results,witness,path,target);}
 }
 private static List<Integer> legal(List<Integer> hand,int led,int trump){if(led<0)return List.copyOf(hand);var same=hand.stream().filter(c->c/13==led).toList();if(!same.isEmpty())return same;var cuts=hand.stream().filter(c->c/13==trump).toList();return cuts.isEmpty()?List.copyOf(hand):cuts;}
 static int winner(List<int[]> trick,int trump){int led=trick.get(0)[1]/13;return trick.stream().max(Comparator.comparingInt(p->(p[1]/13==trump?1000:p[1]/13==led?100:0)+p[1]%13)).orElseThrow()[0];}
 private static List<List<Integer>> copy(List<List<Integer>> source){var result=new ArrayList<List<Integer>>();for(var hand:source)result.add(new ArrayList<>(hand));return result;}
}
