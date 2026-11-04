// Research-only R12 proposal: not part of the accepted production policy.
package ro.whistlab.core.research.bidding;
import java.util.*;
/** A common opening is chosen after aggregation, never separately inside each hidden world. */
public final class OpeningBidEvidence {
 public record Plan(int bid,int lead,double exact,double points,int worlds) { }
 private final List<Integer> allowed;
 private final double[][] points,exact;
 private final boolean safety;
 private int worlds;
 public OpeningBidEvidence(List<Integer> allowed,int bids,int leads,boolean safety) {
  this.allowed=List.copyOf(allowed);this.safety=safety;
  if(allowed.isEmpty()||leads<1)throw new IllegalArgumentException("Empty opening plan");
  points=new double[bids][leads];exact=new double[bids][leads];
 }
 public void add(double[][] scores,double[][] made) {
  for(int bid:allowed)for(int lead=0;lead<points[bid].length;lead++) {
   if(!Double.isFinite(scores[bid][lead])||!Double.isFinite(made[bid][lead])||made[bid][lead]<0||made[bid][lead]>1)
    throw new IllegalArgumentException("Invalid opening utility");
   points[bid][lead]+=scores[bid][lead];exact[bid][lead]+=made[bid][lead];
  }
  worlds++;
 }
 public Plan best() {
  if(worlds==0)throw new IllegalStateException("An opening plan requires observed worlds");
  int bestBid=allowed.get(0),bestLead=0;
  for(int bid:allowed)for(int lead=0;lead<points[bid].length;lead++) {
   double value=safety?exact[bid][lead]:points[bid][lead],best=safety?exact[bestBid][bestLead]:points[bestBid][bestLead];
   if(value>best+1e-12||Math.abs(value-best)<1e-12&&points[bid][lead]>points[bestBid][bestLead]+1e-12) {
    bestBid=bid;bestLead=lead;
   }
  }
  return new Plan(bestBid,bestLead,exact[bestBid][bestLead]/worlds,points[bestBid][bestLead]/worlds,worlds);
 }
}
