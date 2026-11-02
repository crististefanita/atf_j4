package ro.whistlab.core;

import java.util.List;
import java.util.function.IntToDoubleFunction;

/** Exact nine-state expectation of future series payments under declared contract-rate priors. */
public final class StreakForecast {
    private StreakForecast() { }
    public static double[] calibrated(List<Integer> future,int bonus,GameMode mode,int players){
        return values(future,bonus,size->ExactContractRates.probability(mode,players,bonus,size));
    }
    public static double[] constantRate(List<Integer> future,int bonus,double exactProbability){
        if(!Double.isFinite(exactProbability)||exactProbability<0||exactProbability>1)throw new IllegalArgumentException("Probability must be in[0,1]");
        return values(future,bonus,size->exactProbability);
    }
    private static double[] values(List<Integer> future,int bonus,IntToDoubleFunction rate){
        if(bonus!=5&&bonus!=10)throw new IllegalArgumentException("Series reward must be5or10");
        double[] next=new double[9];
        for(int t=future.size()-1;t>=0;t--){int size=future.get(t);if(size<1||size>8)throw new IllegalArgumentException("Future Romanian hand size");double p=rate.applyAsDouble(size);double[] now=new double[9];
            for(int prior=-4;prior<=4;prior++){
                var exact=RomanianScoring.score(0,0,size,prior,bonus);var missed=RomanianScoring.score(0,1,size,prior,bonus);
                now[prior+4]=p*(exact.streakPoints()+next[exact.nextStreak()+4])+(1-p)*(missed.streakPoints()+next[missed.nextStreak()+4]);
            }next=now;
        }
        double baseline=next[4];for(int i=0;i<next.length;i++)next[i]-=baseline;
        return next;
    }
}
