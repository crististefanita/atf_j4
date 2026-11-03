package ro.whistlab.core;

import java.util.*;

/** Generated observed-bid likelihood. It never receives actual private opponent hands. */
final class LearnedBidEvidence {
    static final String SOURCE_SHA256 = "@@MODEL_SHA256@@";
    static final double STRENGTH = 0.25;
    private static final double[] COEFFICIENTS = {@@COEFFICIENTS@@};
    private static final double[] SIGMA = {@@SIGMA@@};
    private LearnedBidEvidence() { }

    static double[] features(List<Card> hand, int size, Trump trump, Card turned, int position, int prefix) {
        if(size<2||size>8||hand.size()!=size||new HashSet<>(hand).size()!=size||turned!=null&&hand.contains(turned))
            throw new IllegalArgumentException("Invalid bid-time hand");
        boolean[][] held=new boolean[4][15];int[] counts=new int[4],lows=new int[4];
        for(Card c:hand){held[c.suit().ordinal()][c.rank()]=true;counts[c.suit().ordinal()]++;if(c.rank()<=10)lows[c.suit().ordinal()]++;}
        double[] x=new double[21];x[0]=1;x[1]=size/8.0;int sideVoids=0,aces=0,kings=0;
        for(int s=0;s<4;s++) {
            if(s==trump.ordinal()) {
                x[2]=counts[s]/8.0;
                for(Card c:hand)if(c.suit().ordinal()==s){boolean higher=false;for(int r=c.rank()+1;r<=14;r++)
                    if(!held[s][r]&&(turned==null||turned.suit().ordinal()!=s||turned.rank()!=r))higher=true;
                    if(!higher)x[3]+=1/8.0;
                }
            } else {
                if(counts[s]==0)sideVoids++;
                for(int r=14;r>=11;r--)if(held[s][r])x[18-r]+=1/8.0;
                if(held[s][13]&&!held[s][14]&&lows[s]>=1){x[8]+=1/8.0;kings++;}
                if(held[s][12]&&!held[s][14]&&!held[s][13]&&lows[s]>=2)x[9]+=1/8.0;
                if(held[s][14]&&held[s][13])x[12]+=1/4.0;
                if(held[s][14])aces++;
            }
        }
        x[10]=sideVoids*x[2]/3;x[11]=sideVoids/(trump==Trump.NO_TRUMP?4.0:3.0);
        x[13]=Arrays.stream(counts).max().orElse(0)/8.0;x[14]=trump==Trump.NO_TRUMP?1:0;
        x[15]=position/3.0;x[16]=prefix/(3.0*size);x[17]=Math.max(0,prefix-size)/(2.0*size);
        x[18]=aces*size/64.0;x[19]=kings*size/64.0;x[20]=x[2]*size/8.0;
        return x;
    }

    static double[] distribution(List<Card> hand,int size,Trump trump,Card turned,int position,int prefix) {
        double[] x=features(hand,size,trump,turned,position,prefix),result=new double[size+1];
        double mean=0;for(int i=0;i<x.length;i++)mean+=x[i]*COEFFICIENTS[i];mean=Math.max(0,Math.min(size,8*mean));
        double top=Double.NEGATIVE_INFINITY;int allowed=0;
        for(int b=0;b<=size;b++)if(position!=3||b+prefix!=size){double z=(b-mean)/SIGMA[size];result[b]=-0.5*z*z;top=Math.max(top,result[b]);allowed++;}
        double total=0;for(int b=0;b<=size;b++)if(position!=3||b+prefix!=size){result[b]=Math.exp(result[b]-top);total+=result[b];}
        for(int b=0;b<=size;b++)result[b]=position==3&&b+prefix==size?0:(1-STRENGTH)/allowed+STRENGTH*result[b]/total;
        return result;
    }

    static double worldWeight(MonteCarloAi.View view,List<List<Card>> remainingWorld) {
        if(!view.mode().isRomanian()||view.seats().size()!=4||view.handSize()<2||view.handSize()>8)return 1;
        double log=0;int leader=(view.dealer()+1)%4,ownPosition=(view.seat()-leader+4)%4;
        for(int s=0;s<4;s++) {
            Integer observed=view.seats().get(s).bid();if(s==view.seat()||observed==null)continue;
            int position=(s-leader+4)%4,prefix=0;
            if(view.seats().get(view.seat()).bid()==null&&position>=ownPosition)continue;
            for(int i=0;i<position;i++){Integer bid=view.seats().get((leader+i)%4).bid();if(bid==null)return 1;prefix+=bid;}
            var initial=new LinkedHashSet<Card>(remainingWorld.get(s));
            for(var play:view.history())if(play.seat()==s)initial.add(play.card());
            for(var play:view.trick())if(play.seat()==s)initial.add(play.card());
            if(initial.size()!=view.handSize()||observed<0||observed>view.handSize()||position==3&&observed+prefix==view.handSize())return 1;
            double[] p=distribution(new ArrayList<>(initial),view.handSize(),view.trump(),view.turnedCard(),position,prefix);
            int allowed=view.handSize()+1-(position==3&&prefix<=view.handSize()?1:0);
            log+=Math.log(allowed*p[observed]);
        }
        return Math.exp(Math.max(Math.log(0.25),Math.min(Math.log(4),log)));
    }

    static final class Weights {
        private double sum,squares,minimum=Double.POSITIVE_INFINITY,maximum=Double.NEGATIVE_INFINITY;private int samples;
        void add(double weight){if(!Double.isFinite(weight)||weight<=0)throw new IllegalArgumentException("Positive finite evidence required");sum+=weight;squares+=weight*weight;samples++;minimum=Math.min(minimum,weight);maximum=Math.max(maximum,weight);}
        double effectiveSampleSize(){return squares==0?0:sum*sum/squares;}
        boolean usable(){return samples>0&&maximum-minimum>1e-12&&effectiveSampleSize()>=0.5*samples;}
        double sum(){return sum;}
    }
}
