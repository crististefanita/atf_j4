package ro.whistlab.core.ai.policy;

import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.testing.CapturedRegression;
import static atf.gradle.audit.assertions.GradleAssertions.*;import java.util.*;import java.lang.reflect.*;import org.junit.Test;
public class SafetyPreferenceTest extends atf.gradle.audit.GradleAuditedTest {
 @Test public void expertExactChanceOutranksMoreExpectedPoints()throws Exception{
  CapturedRegression.run("safety-before-points",getClass().getName(),"higherExactChanceOutranksMoreExpectedPointsInBothImplementations",Map.of("purpose","Encode the user’s safety-first requirement","rule","Prefer the higher chance of exactly making the contract. Expected points and strongest-rival score break probability ties.","scope","Constructed objective test, not evidence of stronger play."),c->{

   c.equal("Expert prefers80% exact with4 points over60% exact with8 points",true,MonteCarloAi.prefer(true,.8,4,0,.6,8,0));
   c.equal("Points decide when exact chance is tied",false,MonteCarloAi.prefer(true,.8,4,0,.8,8,0));c.equal("Rival score decides a complete own-utility tie",true,MonteCarloAi.prefer(true,.8,4,1,.8,4,5));c.equal("Other game variants keep point-first selection",false,MonteCarloAi.prefer(false,.8,4,0,.6,8,0));
  });
 }
 @Test public void oneCardSafetyCanPreferZeroAlthoughOneHasMoreExpectedPoints()throws Exception{
  var samples=new MonteCarloAi.BidSamples(List.of(0,1),2,true);var pointsOnly=new MonteCarloAi.BidSamples(List.of(0,1),2,false);
  for(int i=0;i<32;i++){samples.add(new double[]{2.12,2.36},new double[]{.52,.48});pointsOnly.add(new double[]{2.12,2.36},new double[]{.52,.48});}

  atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "pointsOnly", pointsOnly, "samples", samples);
  assertEquals("samples.best() must equal 0", 0,samples.best());assertEquals("pointsOnly.best() must equal 1", 1,pointsOnly.best());assertTrue("samples.enough() must hold", samples.enough());
 }
 @Test public void commonWorldVarianceAvoidsSpendingExtraSamplesOnSharedNoise(){
  var samples=new MonteCarloAi.BidSamples(List.of(0,1),2,true);for(int i=0;i<31;i++)samples.add(new double[]{i,i+1},new double[]{1,.5});
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "samples", samples);
assertFalse("samples.enough() must remain false", samples.enough());samples.add(new double[]{999,1000},new double[]{1,.5});assertTrue("samples.enough() must hold", samples.enough());assertEquals("samples.samples() must equal 32", 32,samples.samples());
 }
 @Test public void uncertainPairedDifferencesUseMoreEvidenceButStopAtTheBudget(){
  var samples=new MonteCarloAi.BidSamples(List.of(0,1),2,true);for(int i=0;i<32;i++)samples.add(new double[]{0,0},i%2==0?new double[]{1,0}:new double[]{0,1});
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "samples", samples);
assertFalse("samples.enough() must remain false", samples.enough());
  for(int i=32;i<128;i++)samples.add(new double[]{0,0},i%2==0?new double[]{1,0}:new double[]{0,1});assertTrue("samples.enough() must hold", samples.enough());assertEquals("samples.samples() must equal 128", 128,samples.samples());assertEquals("samples.best() must equal 0", 0,samples.best());
 }
 @Test public void everyAlternativeMustBeResolvedNotOnlyTheRunnerUp(){
  var samples=new MonteCarloAi.BidSamples(List.of(0,1,2),3,true);for(int i=0;i<32;i++)samples.add(new double[]{6,5,4},new double[]{.6,.59,i%2});
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "samples", samples);
assertEquals("samples.best() must equal 0", 0,samples.best());assertFalse("samples.enough() must remain false", samples.enough());
 }
 @Test public void invalidSampledUtilitiesAreRejected(){

  atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 0, "declared constant 2", true, "declared constant 3", 1);
  assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->new MonteCarloAi.BidSamples(List.of(),0,true));var samples=new MonteCarloAi.BidSamples(List.of(0),1,true);assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->samples.add(new double[]{0},new double[]{Double.NaN}));
 }
}
