package ro.whistlab.coach.baseline;

import java.util.*;

/** Independent numerical implementation of a calibrated public-bid posterior. */
final class RoundSixIndependentOneCardBidBelief {
    static final String TRAINING_DIGEST="e676fd9778a9459d8c3528e8f8193c24c07e2fecd0cf1009fb9c9e42b8e986f2";
    private static final double[][] BID_ONE={{0.50000000000000000,0.50000000000000000,0.01,0.01,0.01,0.01,0.099173553719008267,0.074074074074074070,0.16071428571428573,0.66666666666666663,0.50000000000000000,0.88095238095238093,0.94545454545454544,0.50000000000000000,0.93548387096774188,0.50000000000000000,0.99,0.50000000000000000,0.99,0.99},
        {0.01,0.01,0.01,0.50000000000000000,0.50000000000000000,0.50000000000000000,0.50000000000000000,0.50000000000000000,0.50000000000000000,0.77272727272727271,0.50000000000000000,0.94936708860759489,0.92857142857142860,0.50000000000000000,0.99,0.50000000000000000,0.99,0.50000000000000000,0.99,0.99}};
    private RoundSixIndependentOneCardBidBelief(){}
    static double probability(RoundSixIndependentPlayer.Position p){
        int lead=(p.dealer()+1)%4;double[][] weights=new double[4][52];boolean informative=false;
        for(int s=0;s<4;s++){Arrays.fill(weights[s],1);Integer bid=p.seats().get(s).bid();if(s==p.seat()||bid==null)continue;int order=(s-lead+4)%4,sum=0;for(int i=0;i<order;i++){Integer b=p.seats().get((lead+i)%4).bid();if(b!=null)sum+=b;}if(order==3&&sum<2)continue;informative=true;
            for(int suit=0;suit<4;suit++)for(int rank=5;rank<13;rank++){int card=suit*13+rank;if(card==p.turnedCard())continue;var view=new RoundSixIndependentPlayer.Position(s,p.dealer(),1,p.trump(),List.of(card),p.seats(),List.of(),List.of(),p.turnedCard());double prior=RoundSixIndependentPlayer.oneCardWinChance(view),q=BID_ONE[s==lead?0:1][Math.min(19,(int)(prior*20))];weights[s][card]=bid==1?q:1-q;}
        }
        if(!informative)return RoundSixIndependentPlayer.oneCardWinChance(p);
        var pool=new ArrayList<Integer>();for(int suit=0;suit<4;suit++)for(int r=5;r<13;r++){int c=suit*13+r;if(c!=p.hand().get(0)&&c!=p.turnedCard())pool.add(c);}int[] world=new int[4];world[p.seat()]=p.hand().get(0);double[] totals=new double[2];enumerate(p,pool,world,weights,0,1,totals);return totals[1]/totals[0];
    }
    private static void enumerate(RoundSixIndependentPlayer.Position p,List<Integer> available,int[] cards,double[][] weight,int seat,double likelihood,double[] totals){
        if(seat==4){int lead=(p.dealer()+1)%4,winner=lead;for(int s=0;s<4;s++)if(strength(cards[s],cards[lead]/13,p.trump())>strength(cards[winner],cards[lead]/13,p.trump()))winner=s;totals[0]+=likelihood;if(winner==p.seat())totals[1]+=likelihood;return;}
        if(seat==p.seat()){enumerate(p,available,cards,weight,seat+1,likelihood,totals);return;}for(int i=0;i<available.size();i++){int c=available.remove(i);cards[seat]=c;enumerate(p,available,cards,weight,seat+1,likelihood*weight[seat][c],totals);available.add(i,c);}
    }
    private static int strength(int c,int lead,int trump){return (c/13==trump?200:c/13==lead?100:0)+c%13;}
}
