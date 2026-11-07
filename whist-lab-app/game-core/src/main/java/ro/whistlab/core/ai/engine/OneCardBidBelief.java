package ro.whistlab.core.ai.engine;

import ro.whistlab.core.model.Card;
import ro.whistlab.core.rules.GameRules;

import java.util.*;

/** Public bid evidence only. Calibrated from development games; no heldout or hidden runtime cards. */
public final class OneCardBidBelief {
    static final String TRAINING_DIGEST="e676fd9778a9459d8c3528e8f8193c24c07e2fecd0cf1009fb9c9e42b8e986f2";
    private static final double[][] BID_ONE={{0.50000000000000000,0.50000000000000000,0.01,0.01,0.01,0.01,0.099173553719008267,0.074074074074074070,0.16071428571428573,0.66666666666666663,0.50000000000000000,0.88095238095238093,0.94545454545454544,0.50000000000000000,0.93548387096774188,0.50000000000000000,0.99,0.50000000000000000,0.99,0.99},
        {0.01,0.01,0.01,0.50000000000000000,0.50000000000000000,0.50000000000000000,0.50000000000000000,0.50000000000000000,0.50000000000000000,0.77272727272727271,0.50000000000000000,0.94936708860759489,0.92857142857142860,0.50000000000000000,0.99,0.50000000000000000,0.99,0.50000000000000000,0.99,0.99}};
    private OneCardBidBelief(){}
    public static double probability(MonteCarloAi.View v){
        if(v.seats().size()!=4||!v.mode().isRomanian()||v.turnedCard()==null)return MonteCarloAi.oneCardWinProbability(v);
        int own=id(v.hand().get(0)),turned=id(v.turnedCard()),leader=(v.dealer()+1)%4;int[] actor=new int[3];int at=0;
        for(int s=0;s<4;s++)if(s!=v.seat())actor[at++]=s;
        double[][] likelihood=new double[4][52];boolean evidence=false;
        for(int s=0;s<4;s++){Arrays.fill(likelihood[s],1);Integer bid=v.seats().get(s).bid();if(s==v.seat()||bid==null||s==v.dealer()&&!isVoluntaryDealerBid(v))continue;evidence=true;
            for(Card c:GameRules.deck(v.mode(),4))if(id(c)!=turned){double p=prior(id(c),s==leader,turned);double q=BID_ONE[s==leader?0:1][Math.min(19,(int)(p*20))];likelihood[s][id(c)]=bid==1?q:1-q;}
        }
        if(!evidence)return MonteCarloAi.oneCardWinProbability(v);
        var pool=MonteCarloAi.unknownCards(v);double all=0,win=0;int[] world=new int[4];world[v.seat()]=own;
        for(int a=0;a<pool.size();a++)for(int b=0;b<pool.size();b++)if(b!=a)for(int c=0;c<pool.size();c++)if(c!=a&&c!=b){world[actor[0]]=id(pool.get(a));world[actor[1]]=id(pool.get(b));world[actor[2]]=id(pool.get(c));double weight=likelihood[actor[0]][world[actor[0]]]*likelihood[actor[1]][world[actor[1]]]*likelihood[actor[2]][world[actor[2]]];all+=weight;int best=leader;for(int s=0;s<4;s++)if(power(world[s],world[leader]/13,v.trump().ordinal())>power(world[best],world[leader]/13,v.trump().ordinal()))best=s;if(best==v.seat())win+=weight;}
        return win/all;
    }

    private static boolean isVoluntaryDealerBid(MonteCarloAi.View view) {
        int prior = 0;
        for (int seat = 0; seat < view.seats().size(); seat++) {
            if (seat == view.dealer()) continue;
            Integer bid = view.seats().get(seat).bid();
            if (bid == null) return false;
            prior += bid;
        }
        return GameRules.allowedBids(1, prior, true).size() > 1;
    }
    static double prior(int own,boolean leader,int turned){int trump=turned/13,suit=own/13,rank=own%13,pool=0,threat=0,lower=0;for(int s=0;s<4;s++)for(int r=5;r<13;r++){int c=s*13+r;if(c==own||c==turned)continue;pool++;if(s==suit&&r<rank)lower++;if(suit==trump?s==suit&&r>rank:s==trump||s==suit&&r>rank)threat++;}return suit==trump||leader?avoid(pool,threat,3):lower/(double)pool*avoid(pool-1,threat,2);}
    private static double avoid(int pool,int threats,int n){double p=1;for(int i=0;i<n;i++)p*=Math.max(0,pool-threats-i)/(double)(pool-i);return p;}
    private static int power(int c,int led,int trump){return (c/13==trump?200:c/13==led?100:0)+c%13;}
    private static int id(Card c){return c.suit().ordinal()*13+c.rank()-2;}
}
