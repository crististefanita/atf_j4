package ro.whistlab.core.ai.forecast;

import ro.whistlab.core.rules.GameMode;


import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.*;
import org.junit.Test;

public class StreakForecastTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void allNineCountersMatchIndependentEnumerationIncludingOneCardBreaksAndRepeatedPayments(){
        for(int bonus:List.of(5,10))for(double p:new double[]{0,.25,.6,1})
            for(List<Integer> future:List.of(List.<Integer>of(),List.of(1),List.of(2),List.of(2,1,2),List.of(8,2,3,1,4,2),List.of(2,2,2,2,2,2))){
                double[] actual=StreakForecast.constantRate(future,bonus,p);double zero=enumerate(future,0,0,bonus,p);
                for(int streak=-4;streak<=4;streak++){
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "actual", actual, "bonus", bonus, "future", future, "p", p, "streak", streak, "zero", zero);
assertEquals("bonus="+bonus+" p="+p+" future="+future+" counter="+streak,enumerate(future,0,streak,bonus,p)-zero,actual[streak+4],1e-10); }
            }
    }
    // Enumerates every exact/miss sequence; intentionally does not call RomanianScoring or the DP.
    private static double enumerate(List<Integer> sizes,int index,int streak,int bonus,double p){
        if(index==sizes.size())return 0;double result=0;
        for(boolean exact:List.of(false,true)){int next,award=0;
            if(sizes.get(index)==1)next=exact?(streak>0?streak:0):(streak<0?streak:0);
            else {next=exact?(streak>0?streak+1:1):(streak<0?streak-1:-1);if(next==5){award=bonus;next=0;}if(next==-5){award=-5;next=0;}}
            result+=(exact?p:1-p)*(award+enumerate(sizes,index+1,next,bonus,p));
        }return result;
    }
    @Test public void noEligibleFutureMeansNoPhantomBonusAndBothRewardsUseTheSamePenalty(){

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 9, "declared constant 2", 1, "declared constant 3", 10, "declared constant 4", .7, "declared constant 5", 0, "declared constant 6", 6, "declared constant 7", 2, "declared constant 8", .6, "declared constant 9", 8, "declared constant 10", 1e-12, "declared constant 11", 3, "declared constant 12", 5, "declared constant 13", -2);
        assertArrayEquals(new double[9],StreakForecast.constantRate(List.of(1,1,1,1),10,.7),0);
        assertEquals(6,StreakForecast.constantRate(List.of(2),10,.6)[8],1e-12);
        assertEquals(3,StreakForecast.constantRate(List.of(2),5,.6)[8],1e-12);
        assertEquals(-2,StreakForecast.constantRate(List.of(2),10,.6)[0],1e-12);
        assertEquals(-2,StreakForecast.constantRate(List.of(2),5,.6)[0],1e-12);
    }
    @Test public void invalidInputsAreRejectedAndCalibratedRatesHaveAnExplicitDomain(){
        for(double p:new double[]{-.01,1.01,Double.NaN,Double.POSITIVE_INFINITY}){
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "p", p);
assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->StreakForecast.constantRate(List.of(2),10,p)); }
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->StreakForecast.constantRate(List.of(0),10,.6));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->StreakForecast.constantRate(List.of(9),10,.6));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->StreakForecast.constantRate(List.of(2),7,.6));
        assertEquals("ExactContractRates.TRAINING_DIGEST.length() must equal 64", 64,ExactContractRates.TRAINING_DIGEST.length());
        for(int size=1;size<=8;size++){
            double rate=ExactContractRates.probability(GameMode.ROMANIAN_WHIST,4,10,size);assertTrue("rate > 0 && rate < 1 must hold", rate>0&&rate<1);
            assertEquals(.6,ExactContractRates.probability(GameMode.LONG_ROMANIAN,4,10,size),0);
            assertEquals(.6,ExactContractRates.probability(GameMode.ROMANIAN_WHIST,3,10,size),0);
            assertEquals(.6,ExactContractRates.probability(GameMode.ROMANIAN_WHIST,4,5,size),0);
        }
    }
}
