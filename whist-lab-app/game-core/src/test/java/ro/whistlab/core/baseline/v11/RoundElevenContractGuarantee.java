package ro.whistlab.core.baseline.v11;

import ro.whistlab.core.model.Card;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.model.PlayedCard;

import java.util.*;
/** A proof over every feasible deal and every legal opponent continuation; never over a sample. */
final class RoundElevenContractGuarantee {
 record Proof(Card card,boolean certified,int worlds,int nodes){}
 private static final int LIMIT=40000;private static final class Exhausted extends RuntimeException{}
 private final RoundElevenMonteCarloAi.View v;private final int target,limit;private int nodes;
 private RoundElevenContractGuarantee(RoundElevenMonteCarloAi.View v,int limit){this.v=v;target=v.seats().get(v.seat()).bid();this.limit=limit;}
 static Proof choose(RoundElevenMonteCarloAi.View v,Card selected){return choose(v,selected,LIMIT);}
 static Proof choose(RoundElevenMonteCarloAi.View v,Card selected,int limit){
  Integer bid=v.seats().get(v.seat()).bid();if(!v.mode().isRomanian()||v.seats().size()!=4||v.hand().size()!=2||bid==null||bid<v.seats().get(v.seat()).won()||bid>v.seats().get(v.seat()).won()+2)return new Proof(selected,false,0,0);
  var legal=GameRules.legalCards(v.hand(),v.trick().isEmpty()?null:v.trick().get(0).card().suit(),v.trump(),v.mode());if(legal.size()<2)return new Proof(selected,false,0,0);
  var worlds=RoundElevenMonteCarloAi.enumerateSmallDeals(v);if(worlds.isEmpty())return new Proof(selected,false,0,0);var proof=new RoundElevenContractGuarantee(v,limit);var choices=new ArrayList<Card>();choices.add(selected);for(Card c:legal)if(!c.equals(selected))choices.add(c);
  try{for(Card card:choices){boolean certain=true;for(var world:worlds){var hands=new ArrayList<List<Card>>();for(var h:world)hands.add(new ArrayList<>(h));if(!proof.play(hands,new ArrayList<>(v.trick()),v.seat(),v.seats().get(v.seat()).won(),card)){certain=false;break;}}if(certain)return new Proof(card,true,worlds.size(),proof.nodes);}}catch(Exhausted budget){return new Proof(selected,false,worlds.size(),proof.nodes);}
  return new Proof(selected,false,worlds.size(),proof.nodes);
 }
 private boolean all(List<List<Card>> hands,List<PlayedCard> trick,int actor,int won){
  if(++nodes>limit)throw new Exhausted();if(won>target)return false;int left=hands.stream().mapToInt(List::size).sum();if(left==0)return trick.isEmpty()&&won==target;if(won+(left+trick.size())/4<target)return false;
  var legal=new ArrayList<>(GameRules.legalCards(hands.get(actor),trick.isEmpty()?null:trick.get(0).card().suit(),v.trump(),v.mode()));if(legal.isEmpty())return false;for(Card c:legal)if(!play(hands,trick,actor,won,c))return false;return true;
 }
 private boolean play(List<List<Card>> hands,List<PlayedCard> trick,int actor,int won,Card card){
  int at=hands.get(actor).indexOf(card);if(at<0)return false;hands.get(actor).remove(at);try{var next=new ArrayList<>(trick);next.add(new PlayedCard(actor,card));int turn=(actor+1)%4,count=won;if(next.size()==4){turn=GameRules.winningSeat(next,v.trump());if(turn==v.seat())count++;next.clear();}return all(hands,next,turn,count);}finally{hands.get(actor).add(at,card);}
 }
}
