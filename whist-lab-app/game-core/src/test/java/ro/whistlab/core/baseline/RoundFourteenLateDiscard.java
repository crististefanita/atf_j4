package ro.whistlab.core.baseline;
import ro.whistlab.core.*;
import java.util.*;
/** Two cards of the same suit, one future forced card: retain the rank matching the remaining contract. */
final class RoundFourteenLateDiscard {
 private RoundFourteenLateDiscard() { }
 static Card choose(RoundFourteenMonteCarloAi.View v){
  if(!v.mode().isRomanian()||v.hand().size()!=2||v.trick().isEmpty()||v.seats().get(v.seat()).bid()==null)return null;
  Card low=v.hand().get(0),high=v.hand().get(1);if(low.suit()!=high.suit())return null;if(low.rank()>high.rank()){Card swap=low;low=high;high=swap;}
  boolean lowWins=wins(v,low),highWins=wins(v,high);int gain;
  if(!lowWins&&!highWins)gain=0;
  else if(lowWins&&highWins&&v.trick().size()==v.seats().size()-1)gain=1;
  else return null;
  int needed=v.seats().get(v.seat()).bid()-v.seats().get(v.seat()).won()-gain;
  return needed>0?low:high;
 }
 private static boolean wins(RoundFourteenMonteCarloAi.View v,Card card){var trick=new ArrayList<>(v.trick());trick.add(new PlayedCard(v.seat(),card));return GameRules.winningSeat(trick,v.trump())==v.seat();}
}
