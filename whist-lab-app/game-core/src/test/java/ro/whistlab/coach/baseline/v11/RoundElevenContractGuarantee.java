package ro.whistlab.coach.baseline.v11;
import java.util.*;
/** Independent numeric universal proof, restricted to two own remaining cards and complete worlds. */
final class RoundElevenContractGuarantee {
 record Proof(int card,boolean certified,int worlds,int nodes){}private static final int LIMIT=40000;private static final class Exhausted extends RuntimeException{}
 private final RoundElevenIndependentPlayer.Position p;private final int target,limit;private int nodes;
 private RoundElevenContractGuarantee(RoundElevenIndependentPlayer.Position p,int limit){this.p=p;target=p.seats().get(p.seat()).bid();this.limit=limit;}
 static Proof choose(RoundElevenIndependentPlayer.Position p,int selected){return choose(p,selected,LIMIT);}
 static Proof choose(RoundElevenIndependentPlayer.Position p,int selected,int limit){
  Integer target=p.seats().get(p.seat()).bid();if(p.seats().size()!=4||p.hand().size()!=2||target==null||target<p.seats().get(p.seat()).won()||target>p.seats().get(p.seat()).won()+2)return new Proof(selected,false,0,0);
  var legal=RoundElevenIndependentPlayer.legal(p.hand(),p.trick().isEmpty()?-1:p.trick().get(0).card()/13,p.trump());if(legal.size()<2)return new Proof(selected,false,0,0);var worlds=RoundElevenIndependentPlayer.smallDeals(p);if(worlds.isEmpty())return new Proof(selected,false,0,0);var search=new RoundElevenContractGuarantee(p,limit);var choices=new ArrayList<Integer>();choices.add(selected);for(int c:legal)if(c!=selected)choices.add(c);
  try{for(int card:choices){boolean certain=true;for(var world:worlds){var hands=new ArrayList<List<Integer>>();for(var h:world)hands.add(new ArrayList<>(h));if(!search.play(hands,new ArrayList<>(p.trick()),p.seat(),p.seats().get(p.seat()).won(),card)){certain=false;break;}}if(certain)return new Proof(card,true,worlds.size(),search.nodes);}}catch(Exhausted budget){return new Proof(selected,false,worlds.size(),search.nodes);}
  return new Proof(selected,false,worlds.size(),search.nodes);
 }
 private boolean all(List<List<Integer>> hands,List<RoundElevenIndependentPlayer.Play> trick,int actor,int won){
  if(++nodes>limit)throw new Exhausted();if(won>target)return false;int left=hands.stream().mapToInt(List::size).sum();if(left==0)return trick.isEmpty()&&won==target;if(won+(left+trick.size())/4<target)return false;var legal=new ArrayList<>(RoundElevenIndependentPlayer.legal(hands.get(actor),trick.isEmpty()?-1:trick.get(0).card()/13,p.trump()));if(legal.isEmpty())return false;for(int card:legal)if(!play(hands,trick,actor,won,card))return false;return true;
 }
 private boolean play(List<List<Integer>> hands,List<RoundElevenIndependentPlayer.Play> trick,int actor,int won,int card){
  int at=hands.get(actor).indexOf(card);if(at<0)return false;hands.get(actor).remove(at);try{var next=new ArrayList<>(trick);next.add(new RoundElevenIndependentPlayer.Play(actor,card));int turn=(actor+1)%4,count=won;if(next.size()==4){turn=RoundElevenIndependentPlayer.winner(next,p.trump());if(turn==p.seat())count++;next.clear();}return all(hands,next,turn,count);}finally{hands.get(actor).add(at,card);}
 }
}
