package ro.whistlab.app;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import ro.whistlab.core.*;

/** Debug-only recorded inputs. All outcomes are calculated by the normal game engine. */
final class OriginalMatchReplay {
    private final Properties recorded=new Properties();
    private final File observedFile;
    private final List<Integer> actualMoves=new ArrayList<>();
    private int hand;
    OriginalMatchReplay(File directory)throws IOException {
        try(InputStream in=new FileInputStream(new File(directory,"original-match.properties"))){recorded.load(in);}
        if(!"true".equals(recorded.getProperty("match.complete")))throw new IOException("Original match is incomplete");
        observedFile=new File(directory,"replay-observed.properties");
    }
    GameSession startHand(int index,ScoreHistory scores) {
        hand=index;actualMoves.clear();
        GameConfig config=new GameConfig(4,GameMode.OH_HELL,AiDifficulty.NORMAL,20261101L);
        List<List<Card>> hands=new ArrayList<>();
        for(int seat=0;seat<4;seat++)hands.add(numbers("cards."+seat).stream().map(OriginalMatchReplay::card).collect(Collectors.toList()));
        GameSession base=GameSession.fromDeal(config,hands,Trump.values()[number("trump")],number("dealer"));
        GameSession.SaveState saved=base.saveState();
        List<GameSession.PlayerState> players=new ArrayList<>();
        for(GameSession.PlayerState p:saved.players())players.add(new GameSession.PlayerState(p.seat(),p.name(),p.human(),
                p.hand(),null,0,scores.latestTotals().get(p.seat())));
        return GameSession.restore(new GameSession.SaveState(config,GamePhase.BIDDING,index,number("size"),
                saved.dealerSeat(),saved.biddingSeat(),saved.currentSeat(),0,0,saved.trump(),-1,players,
                Collections.emptyList(),Collections.emptyList(),Collections.emptyList(),null,scores));
    }
    int bid(int seat){return numbers("bids").get(seat);}
    Card nextCard(){return card(numbers("plays").get(actualMoves.size()));}
    void played(Card card){actualMoves.add(id(card));}
    void export(GameSession game) {
        GameSession.Snapshot s=game.snapshot();Properties p=new Properties();
        p.setProperty("hand",Integer.toString(s.handIndex()));p.setProperty("size",Integer.toString(s.handSize()));
        p.setProperty("phase",s.phase().name());p.setProperty("dealer",Integer.toString(s.dealerSeat()));
        p.setProperty("trump",Integer.toString(s.trump().ordinal()));p.setProperty("turn",Integer.toString(s.currentSeat()));
        p.setProperty("biddingSeat",Integer.toString(s.biddingSeat()));
        p.setProperty("moves",csv(actualMoves));
        p.setProperty("bids",csv(s.players().stream().map(v->v.bid()==null?-1:v.bid()).collect(Collectors.toList())));
        p.setProperty("won",csv(s.players().stream().map(GameSession.PlayerSnapshot::tricksWon).collect(Collectors.toList())));
        p.setProperty("totals",csv(s.players().stream().map(GameSession.PlayerSnapshot::totalScore).collect(Collectors.toList())));
        for(int seat=0;seat<4;seat++)p.setProperty("cards."+seat,csv(s.players().get(seat).hand().stream()
                .map(OriginalMatchReplay::id).collect(Collectors.toList())));
        File pending=new File(observedFile.getParentFile(),observedFile.getName()+".pending");
        try {
            try(OutputStream out=new FileOutputStream(pending)){p.store(out,"Actual Whist Lab debug observations");}
            java.nio.file.Files.move(pending.toPath(),observedFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch(IOException e){throw new IllegalStateException("Cannot export replay observation",e);}
    }
    private int number(String key){return Integer.parseInt(recorded.getProperty("hand."+hand+"."+key));}
    private List<Integer> numbers(String key){return Arrays.stream(recorded.getProperty("hand."+hand+"."+key).split(","))
            .map(Integer::parseInt).collect(Collectors.toList());}
    private static Card card(int id){return new Card(Suit.values()[id/13],id%13+2);}
    private static int id(Card card){return card.suit().ordinal()*13+card.rank()-2;}
    private static String csv(List<Integer> values){return values.stream().map(String::valueOf).collect(Collectors.joining(","));}
}
