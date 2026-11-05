package ro.whistlab.coach;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Local legal observations; private fixture hands are never retained in Position. */
final class LocalPositions {
    private LocalPositions() { }
    static IndependentPlayer.Position initial(int size, int seed, boolean bidding, boolean last) {
        State state = new State(size, seed, last ? 0 : 3);
        if (!bidding && last) while (state.actor != 0) state.play();
        return state.position(bidding, last);
    }
    static IndependentPlayer.Position endgame() {
        for (int seed=0;seed<100;seed++) {
            State state = new State(8, seed, 3);
            while (state.completed < 6) state.play();
            while (state.actor != 0) state.play();
            if (state.hands.get(0).size()==2 && legal(state.hands.get(0),state.trick,state.trump).size()==2)
                return state.position(false,false);
        }
        throw new IllegalStateException("No eligible generated endgame");
    }
    static List<Integer> legal(List<Integer> hand, List<IndependentPlayer.Play> trick, int trump) {
        if(trick.isEmpty())return List.copyOf(hand);
        int led=trick.get(0).card()/13;
        List<Integer> follow=new ArrayList<>(),cuts=new ArrayList<>();
        for(int card:hand) {if(card/13==led)follow.add(card);if(card/13==trump)cuts.add(card);}
        return !follow.isEmpty()?follow:!cuts.isEmpty()?cuts:List.copyOf(hand);
    }
    private static final class State {
        final int size,dealer,trump,turned;
        final List<List<Integer>> hands=new ArrayList<>();
        final List<IndependentPlayer.Play> history=new ArrayList<>(),trick=new ArrayList<>();
        final int[] won=new int[4];
        int actor,completed;
        State(int size,int seed,int dealer) {
            this.size=size;this.dealer=dealer;actor=(dealer+1)%4;
            List<Integer> deck=new ArrayList<>();
            for(int suit=0;suit<4;suit++)for(int rank=5;rank<13;rank++)deck.add(13*suit+rank);
            Collections.shuffle(deck,new Random(seed));
            for(int seat=0;seat<4;seat++)hands.add(new ArrayList<>(deck.subList(seat*size,(seat+1)*size)));
            turned=size==8?-1:deck.get(4*size);trump=turned<0?-1:turned/13;
        }
        void play() {
            int card=legal(hands.get(actor),trick,trump).stream().min(Integer::compare).orElseThrow();
            hands.get(actor).remove(Integer.valueOf(card));
            var play=new IndependentPlayer.Play(actor,card);history.add(play);trick.add(play);
            actor=(actor+1)%4;
            if(trick.size()==4) {
                int led=trick.get(0).card()/13;
                var winner=trick.get(0);
                for(var candidate:trick)if(power(candidate.card(),led)>power(winner.card(),led))winner=candidate;
                actor=winner.seat();won[actor]++;completed++;trick.clear();
            }
        }
        int power(int card,int led) {return (card/13==trump?200:card/13==led?100:0)+card%13;}
        IndependentPlayer.Position position(boolean bidding,boolean last) {
            List<IndependentPlayer.Seat> seats=new ArrayList<>();
            for(int seat=0;seat<4;seat++) {
                Integer bid;
                if(bidding)bid=last&&seat!=0?Integer.valueOf(0):null;
                else bid=seat==0?won[0]:0;
                seats.add(new IndependentPlayer.Seat(hands.get(seat).size(),bid,won[seat],0));
            }
            return new IndependentPlayer.Position(0,dealer,size,trump,hands.get(0),seats,trick,history,turned,true,5,5);
        }
    }
}
