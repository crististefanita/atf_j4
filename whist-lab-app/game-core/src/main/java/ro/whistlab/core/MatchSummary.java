package ro.whistlab.core;

import java.util.List;
import java.util.stream.Collectors;

/** Player-facing explanations derived from the active rule profile and public state. */
public final class MatchSummary {
    private MatchSummary() { }
    public static String bidBalance(GameSession.Snapshot state){int difference=state.bidTotal()-state.handSize();return (difference>0?"+":"")+difference;}
    public static String modeDescription(GameMode mode) {
        return switch(mode) {
            case ROMANIAN_WHIST -> "Reduced deck, 1-8-1 schedule. Follow suit; when void, trump if held. Eight-card hands have no trump.";
            case LONG_ROMANIAN -> "Reduced deck, 8-1-8 schedule. Follow suit; when void, trump if held. Eight-card hands have no trump.";
            case OH_HELL -> "10-1-10 schedule (8-1-8 with six players). Follow suit; when void, discard or trump. Exact bid adds ten.";
            case DESCENDING -> "Hand sizes decrease to one. Trump cycles each hand. Follow suit; when void, discard or trump.";
            case DOWN_AND_UP -> "Maximum to one, then back to maximum. Trump cycles each hand. Exact bid adds ten.";
            case MAXIMUM -> "Ten hands at the maximum size allowed by 52 cards. Trump cycles each hand. Exact bid adds ten.";
            case REVERSE_OH_HELL -> "1-10-1 schedule (1-8-1 with six players). An undealt card determines trump. Exact bid adds ten.";
            case PRACTICE_HAND -> "One card per player: practise a complete trick, bidding and scoring.";
        };
    }
    public static String progress(GameSession.Snapshot state) {
        return "Hand "+state.handNumber()+" / "+GameRules.handSizes(state.mode(),state.players().size()).size();
    }
    public static String playHint(GameSession.Snapshot state) {
        if(state.currentTrick().isEmpty())return "Lead any card. The trick winner leads next.";
        Suit led=state.currentTrick().get(0).card().suit();
        List<Card> hand=state.players().get(0).hand();
        if(hand.stream().anyMatch(c->c.suit()==led))return "Follow "+led+". You must follow even if another player has trumped.";
        if(state.mode().isRomanian()&&state.trump()!=Trump.NO_TRUMP
                &&hand.stream().anyMatch(c->c.suit().ordinal()==state.trump().ordinal()))
            return "No led suit: you must play trump. Overtrumping is optional.";
        return state.mode().isRomanian()?"No led suit or required trump: discard any card.":"No led suit: discard or play trump.";
    }
    public static String winners(GameSession.Snapshot state) {
        int best=state.players().stream().mapToInt(GameSession.PlayerSnapshot::totalScore).max().orElseThrow();
        List<String> names=state.players().stream().filter(p->p.totalScore()==best).map(GameSession.PlayerSnapshot::name).collect(Collectors.toList());
        return (names.size()==1?"Winner: ":"Joint winners: ")+String.join(", ",names)+" ("+best+" points)";
    }
}
