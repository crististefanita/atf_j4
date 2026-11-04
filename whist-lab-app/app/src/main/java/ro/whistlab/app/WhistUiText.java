package ro.whistlab.app;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import ro.whistlab.core.AiDifficulty;
import ro.whistlab.core.Card;
import ro.whistlab.core.GameMode;
import ro.whistlab.core.GameRules;
import ro.whistlab.core.GameSession;
import ro.whistlab.core.HowToPlay;
import ro.whistlab.core.Suit;
import ro.whistlab.core.Trump;

/** Player-facing copy for the Android UI. Rule calculations stay in game-core. */
final class WhistUiText {
    private final HowToPlay.Language language;

    WhistUiText(HowToPlay.Language language) {
        this.language = language == null ? HowToPlay.Language.EN : language;
    }

    static HowToPlay.Language parseLanguage(String value) {
        if (value == null) return HowToPlay.Language.EN;
        try {
            return HowToPlay.Language.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException invalid) {
            return HowToPlay.Language.EN;
        }
    }

    HowToPlay.Language language() { return language; }
    boolean romanian() { return language == HowToPlay.Language.RO; }

    String languageLabel() { return choose("Language", "Limba"); }
    String homeTitle() { return "Whist"; }
    String homeTagline() { return choose("Bid exactly. Follow suit. Win the tricks you call.",
            "Licitează exact. Respectă culoarea. Câștigă levatele anunțate."); }
    String homeFootnote() { return choose("Play locally against computer opponents. Your game stays on this device.",
            "Joacă local împotriva adversarilor controlați de calculator. Partida rămâne pe acest dispozitiv."); }
    String newGame() { return choose("New game", "Joc nou"); }
    String resumeGame() { return choose("Resume game", "Reia partida"); }
    String help() { return choose("How to play", "Reguli de joc"); }
    String setupTitle() { return choose("Game setup", "Setări joc"); }
    String rulesAndSchedule() { return choose("Rules & schedule", "Reguli și program"); }
    String scoringDetails() { return choose("Scoring details", "Calculul punctajului"); }
    String players() { return choose("Players", "Jucători"); }
    String gameType() { return choose("Game type", "Varianta de joc"); }
    String aiLevel() { return choose("AI level", "Nivelul adversarilor"); }
    String streakToggle() { return choose("Romanian five-hand streaks (missed series: -5)",
            "Serii românești de cinci mâini (serie ratată: −5)"); }
    String streakBonusLabel() { return choose("Five-hand exact-contract bonus",
            "Bonus pentru cinci contracte exacte eligibile"); }
    String fivePointsOriginal() { return choose("5 points (original)", "5 puncte (original)"); }
    String tenPoints() { return choose("10 points", "10 puncte"); }
    String startGame() { return choose("Start game", "Începe partida"); }
    String back() { return choose("Back", "Înapoi"); }
    String placeBid() { return choose("Place bid", "Licitează"); }
    String scoreSheet() { return choose("Score sheet", "Scorul partidei"); }
    String scoreHistory() { return choose("Score history", "Istoricul scorului"); }
    String historyHand() { return choose("Hand", "Mână"); }
    String historyPoints() { return choose("Points", "Puncte"); }
    String earlierBalance(int hand, int total) {
        return choose("Total after hand " + hand + ": " + total + ". Earlier hand scores are unavailable.",
                "Total după mâna " + hand + ": " + total + ". Punctajele anterioare pe mâini nu sunt disponibile.");
    }
    String mainMenu() { return choose("Main menu", "Meniu principal"); }
    String gameMenu() { return choose("Game menu", "Meniu joc"); }
    String nextHand() { return choose("Next hand", "Mâna următoare"); }
    String playAgain() { return choose("Play again", "Joacă din nou"); }
    String currentScores(int hand) { return choose("Current scores · Hand " + hand,
            "Scor curent · Mâna " + hand); }
    String close() { return choose("Close", "Închide"); }
    String gameComplete() { return choose("Game complete", "Partidă încheiată"); }
    String handComplete() { return choose("Hand complete", "Mână încheiată"); }
    String noSavedGame() { return choose("Saved game could not be resumed and was cleared.",
            "Partida salvată nu a putut fi reluată și a fost ștearsă."); }
    String invalidBid() { return choose("That bid is not available. Choose another value.",
            "Licitația nu este disponibilă. Alege altă valoare."); }
    String invalidCard() { return choose("That card cannot be played now.",
            "Cartea nu poate fi jucată în acest moment."); }
    String bidSelector() { return choose("Choose your bid", "Alege licitația"); }
    String bidPickerDescription() { return choose("Bid value", "Valoarea licitației"); }
    String bidChoiceHeading() { return choose("YOUR BID", "LICITAȚIA TA"); }
    String bidChoiceDescription(int bid, boolean selected) {
        return selected ? choose("Bid " + bid + ", selected", "Licitația " + bid + ", selectată")
                : choose("Select bid " + bid, "Selectează licitația " + bid);
    }
    String spinnerLabel(String id) {
        return switch (id) {
            case "interface-language" -> languageLabel();
            case "player-count" -> players();
            case "history-player" -> scoreColumnPlayer();
            case "game-type" -> gameType();
            case "ai-level" -> aiLevel();
            case "romanian-streak-value" -> streakBonusLabel();
            case "bid-options" -> bidPickerDescription();
            default -> id;
        };
    }

    String modeName(GameMode mode) {
        return switch (mode) {
            case OH_HELL -> "Oh Hell";
            case DESCENDING -> choose("Descending", "Descrescător");
            case PRACTICE_HAND -> choose("Practice hand", "Mână de antrenament");
            case ROMANIAN_WHIST -> choose("Romanian (1–8–1)", "Românesc (1–8–1)");
            case DOWN_AND_UP -> choose("Down & Up", "Coborâre și urcare");
            case MAXIMUM -> choose("Maximum", "Maxim");
            case LONG_ROMANIAN -> choose("Long Romanian (8-1-8)", "Whist românesc lung (8–1–8)");
            case REVERSE_OH_HELL -> choose("Reverse Oh Hell", "Oh Hell inversat");
        };
    }

    String difficultyName(AiDifficulty difficulty) {
        return switch (difficulty.name()) {
            case "NORMAL" -> choose("Normal", "Normal");
            case "HARD" -> choose("Hard", "Dificil");
            case "EXPERT" -> choose("Expert (simulation)", "Expert (simulare)");
            default -> difficulty.label();
        };
    }

    String modeDescription(GameMode mode) {
        return switch (mode) {
            case ROMANIAN_WHIST -> choose(
                    "Reduced deck; 1–8–1 schedule. Follow suit; if void, play trump when held. Eight-card hands have no trump. Exact bid: 5 + tricks; a miss loses the bid difference.",
                    "Pachet redus; program 1–8–1. Respectă culoarea; dacă nu ai culoarea cerută, joci atu dacă ai. Mâinile de opt cărți sunt fără atu. Contract exact: 5 + levate; la ratare pierzi diferența dintre licitat și câștigat.");
            case LONG_ROMANIAN -> choose(
                    "Reduced deck; 8–1–8 schedule. Romanian Whist follow-suit, mandatory-trump and scoring rules apply.",
                    "Pachet redus; program 8–1–8. Se aplică regulile de Whist românesc privind culoarea, atuul obligatoriu și punctajul.");
            case OH_HELL -> choose(
                    "Builds up to the table limit, then comes back down. Follow suit; when void, discard or trump. Each trick is one point; an exact bid adds ten.",
                    "Mâinile cresc până la limita mesei, apoi scad. Respectă culoarea; când nu o ai, poți decarta sau tăia cu atu. Fiecare levată valorează un punct; contractul exact adaugă zece.");
            case DESCENDING -> choose(
                    "Starts at the largest hand that fits the deck and decreases to one. Trump changes each hand. Follow suit; when void, discard or trump.",
                    "Începe cu cea mai mare mână care încape în pachet și scade până la una. Atuul se schimbă la fiecare mână. Respectă culoarea; când nu o ai, poți decarta sau tăia cu atu.");
            case DOWN_AND_UP -> choose(
                    "Starts at the largest hand that fits the deck, decreases to one and rises again. Trump changes each hand; an exact bid adds ten.",
                    "Începe cu cea mai mare mână care încape în pachet, scade până la una și apoi crește. Atuul se schimbă la fiecare mână; contractul exact adaugă zece.");
            case MAXIMUM -> choose(
                    "Ten hands at the largest size that fits the deck and table. Trump changes each hand; an exact bid adds ten.",
                    "Zece mâini cu cel mai mare număr de cărți care încape la masă. Atuul se schimbă la fiecare mână; contractul exact adaugă zece.");
            case REVERSE_OH_HELL -> choose(
                    "Starts with one card and grows to the table limit, then decreases. An undealt turned card sets trump; an exact bid adds ten.",
                    "Începe cu o carte, crește până la limita mesei, apoi scade. O carte întoarsă și neîmpărțită stabilește atuul; contractul exact adaugă zece.");
            case PRACTICE_HAND -> choose(
                    "One card per player: practise a complete trick, bidding and scoring.",
                    "O carte pentru fiecare jucător: exersează licitarea, o levată completă și punctajul.");
        };
    }

    String schedulePreview(GameMode mode, int playerCount) {
        List<Integer> sizes = GameRules.handSizes(mode, playerCount);
        List<String> runs = new ArrayList<>();
        for (int index = 0; index < sizes.size();) {
            int size = sizes.get(index);
            int end = index + 1;
            while (end < sizes.size() && sizes.get(end) == size) end++;
            runs.add(size + (end - index > 1 ? "×" + (end - index) : ""));
            index = end;
        }
        String prefix = choose("Cards per hand (× means consecutive hands): ",
                "Cărți pe mână (× indică mâini consecutive): ");
        String total = romanian()
                ? " · " + sizes.size() + (sizes.size() == 1 ? " mână" : " de mâini")
                : " · " + sizes.size() + (sizes.size() == 1 ? " hand" : " hands");
        return prefix + String.join(" → ", runs) + total;
    }

    String progress(GameSession.Snapshot state) {
        String label = romanian() ? "Mâna " : "Hand ";
        return label + state.handNumber() + " / "
                + GameRules.handSizes(state.mode(), state.players().size()).size();
    }

    String gameTitle(GameMode mode) {
        return "Whist · " + (mode == GameMode.ROMANIAN_WHIST ? choose("Romanian", "Românesc") : modeName(mode));
    }

    String bidSummary(GameSession.Snapshot state) {
        if (romanian()) {
            return "Licitații depuse " + state.bidsSubmitted() + "/" + state.players().size()
                    + " · Total licitat " + state.bidTotal() + "/" + state.handSize();
        }
        return "Bids submitted " + state.bidsSubmitted() + "/" + state.players().size()
                + " · Bid total " + state.bidTotal() + "/" + state.handSize();
    }

    String roundStatus(GameSession.Snapshot state) {
        return progress(state) + " · " + bidSummary(state);
    }

    String playHint(GameSession.Snapshot state) {
        if (state.currentTrick().isEmpty()) {
            return choose("Lead any card. The trick winner leads next.",
                    "Poți începe cu orice carte. Câștigătorul levatei începe următoarea.");
        }
        Suit led = state.currentTrick().get(0).card().suit();
        List<Card> hand = state.players().get(0).hand();
        if (hand.stream().anyMatch(card -> card.suit() == led)) {
            return romanian()
                    ? "Respectă culoarea cerută (" + suitName(led) + "). Trebuie să o urmezi chiar dacă altcineva a tăiat cu atu."
                    : "Follow " + led.label().toLowerCase(java.util.Locale.ROOT)
                            + ". You must follow even if another player has trumped.";
        }
        boolean holdsTrump = state.trump() != Trump.NO_TRUMP
                && hand.stream().anyMatch(card -> state.trump().suit().orElse(null) == card.suit());
        if (state.mode().isRomanian() && holdsTrump) {
            return choose("No led suit: you must play trump. Overtrumping is optional.",
                    "Nu ai culoarea cerută: trebuie să joci atu. Supratatuarea este opțională.");
        }
        if (state.mode().isRomanian()) {
            return choose("No led suit or held trump: discard any card.",
                    "Nu ai nici culoarea cerută, nici atu: poți decarta orice carte.");
        }
        return choose("No led suit: discard or play trump.",
                "Nu ai culoarea cerută: poți decarta sau tăia cu atu.");
    }

    String winner(GameSession.Snapshot state) {
        int best = state.players().stream().mapToInt(GameSession.PlayerSnapshot::totalScore)
                .max().orElseThrow();
        List<String> names = state.players().stream()
                .filter(player -> player.totalScore() == best)
                .map(this::playerName).collect(Collectors.toList());
        return names.size() == 1
                ? choose("Winner: ", "Câștigător: ") + names.get(0) + " (" + best
                        + choose(" points)", " puncte)")
                : choose("Joint winners: ", "Câștigători la egalitate: ")
                        + String.join(", ", names) + " (" + best
                        + choose(" points)", " puncte)");
    }

    String scoreRule(GameSession.Snapshot state) {
        return state.mode().isRomanian()
                ? choose("Exact bid: 5 + tricks. Otherwise, lose the bid difference.",
                        "Contract exact: 5 + levate. Altfel, pierzi diferența dintre licitat și câștigat.")
                : choose("Each trick is one point. An exact bid adds ten points.",
                        "Fiecare levată valorează un punct. Contractul exact adaugă zece puncte.");
    }

    String streakSummary(GameSession.Snapshot state) {
        return choose("Five eligible exact contracts: +" + state.streakBonusPoints()
                        + "; five misses: −5, then reset. One-card hands do not advance a streak; an opposite result breaks it.",
                "Cinci contracte exacte eligibile: +" + state.streakBonusPoints()
                        + "; cinci ratări: −5, apoi seria se resetează. Mâinile cu o carte nu avansează seria; un rezultat opus o întrerupe.");
    }

    String playerName(GameSession.PlayerSnapshot player) {
        return player.human() ? choose("You", "Tu") : player.name();
    }

    String bidLabel() { return choose("Bid", "Licitat"); }
    String tricksLabel() { return choose("Tricks", "Levate"); }
    String tricksWonLabel() { return choose("Tricks won", "Levate câștigate"); }
    String scoreLabel() { return choose("Score", "Scor"); }
    String pointsLabel() { return choose("pts", "pct."); }
    String scoreColumnPlayer() { return choose("Player", "Jucător"); }
    String scoreColumnBid() { return choose("Bid", "Lic."); }
    String scoreColumnTricks() { return choose("Won", "Lev."); }
    String scoreColumnHand() { return choose("Hand", "Pct."); }
    String scoreColumnTotal() { return choose("Total", "Total"); }
    String bidNotPlaced() { return choose("Bid not placed", "Licitația nu a fost depusă"); }
    String dealerLabel() { return choose("DEALER", "ÎMPĂRȚITOR"); }
    String currentTurnLabel() { return choose("Current turn", "La rând"); }
    String yourTurn() { return choose("Your turn", "Este rândul tău"); }
    String waitingFor(String player) { return choose("Waiting for " + player + ".", "Îl așteaptă pe " + player + "."); }
    String currentTrick() { return choose("Current trick", "Levată curentă"); }
    String noCardsYet() { return choose("No cards have been played in this hand yet.", "Nu s-a jucat nicio carte în această mână."); }
    String trumpHeading() { return choose("TRUMP", "ATU"); }
    String noTrump() { return choose("No trump", "Fără atu"); }
    String seatName(GameSession.PlayerSnapshot player) { return playerName(player); }
    String seatWonLabel() { return choose("Won", "Levate"); }
    String seatFacts(GameSession.PlayerSnapshot player) {
        return bidLabel() + ": " + (player.bid() == null ? "—" : player.bid())
                + "\n" + seatWonLabel() + ": " + player.tricksWon();
    }
    String seatTurnAction(boolean bidding) {
        return bidding ? choose("BID NOW", "LICITEAZĂ ACUM")
                : choose("PLAY NOW", "JOACĂ ACUM");
    }
    String seatTurnBadge(boolean bidding) {
        return bidding ? choose("BID NOW", "LICITEAZĂ")
                : choose("PLAY NOW", "JOACĂ ACUM");
    }
    String seatAccessibility(GameSession.PlayerSnapshot player, boolean dealer, boolean active,
            boolean bidding) {
        String description = playerName(player) + ". " + bidLabel() + " "
                + (player.bid() == null ? choose("not placed", "nepusă") : player.bid())
                + ". " + tricksWonLabel() + " " + player.tricksWon() + ". "
                + scoreLabel() + " " + player.totalScore() + " " + pointsLabel();
        if (dealer) description += ". " + dealerLabel();
        if (active) description += ". " + currentTurnLabel() + ". " + seatTurnAction(bidding);
        return description;
    }
    String trickPlayer(GameSession.PlayerSnapshot player) { return playerName(player); }
    String trickCardAccessibility(GameSession.PlayerSnapshot player, Card card) {
        return playerName(player) + choose(" played ", " a jucat ") + cardLabel(card);
    }
    String handAccessibility(int count) {
        return romanian() ? "Cărțile tale, " + count + "." : "Your hand, " + count + " cards.";
    }
    String cardState(boolean bidding, boolean humanTurn, boolean playable) {
        if (bidding) return choose("Visible while bidding", "Vizibilă la licitare");
        if (!humanTurn) return choose("Waiting for your turn", "Așteaptă rândul tău");
        if (playable) return choose("Legal card", "Carte legală");
        return choose("Not legal now", "Nu poate fi jucată acum");
    }
    String cardLabel(Card card) {
        String rank = switch (card.rank()) {
            case 14 -> choose("Ace", "As");
            case 13 -> choose("King", "Rege");
            case 12 -> choose("Queen", "Damă");
            case 11 -> choose("Jack", "Valet");
            default -> Integer.toString(card.rank());
        };
        return romanian() ? rank + " de " + suitName(card.suit())
                : rank + " of " + card.suit().label().toLowerCase(java.util.Locale.ROOT);
    }
    String suitName(Suit suit) {
        return switch (suit) {
            case SPADES -> choose("spades", "pică");
            case HEARTS -> choose("hearts", "cupă");
            case CLUBS -> choose("clubs", "treflă");
            case DIAMONDS -> choose("diamonds", "caro");
        };
    }
    String trumpAccessibility(GameSession.Snapshot state) {
        if (state.turnedCard() != null) {
            return choose("Trump set by turned card: ", "Atu stabilit de cartea întoarsă: ")
                    + cardLabel(state.turnedCard());
        }
        return state.trump().suit().map(suit -> choose("Trump suit only, no turned card: ",
                        "Atu de culoare, fără carte întoarsă: ") + suitName(suit))
                .orElse(noTrump());
    }
    String trumpIndicatorStatus(GameSession.Snapshot state) {
        if (state.turnedCard() != null) return choose("TURNED CARD", "CARTE ÎNTOARSĂ");
        if (state.trump().suit().isPresent()) return choose("NO TURNED CARD", "FĂRĂ CARTE");
        return choose("NO TRUMP", "FĂRĂ ATU");
    }
    String tableAccessibility(int seats) {
        return romanian() ? "Masa de joc, " + seats + " jucători." : "Game table, " + seats + " players.";
    }
    String boardSurface() { return choose("Playing surface", "Suprafața de joc"); }
    String scoreLine(GameSession.PlayerSnapshot player, int earned) {
        return scoreLine(player, (earned >= 0 ? "+" : "") + earned);
    }
    String scoreLine(GameSession.PlayerSnapshot player, String handValue) {
        String line = playerName(player) + " | " + bidLabel() + ": "
                + (player.bid() == null ? "—" : player.bid()) + " | "
                + tricksWonLabel() + ": " + player.tricksWon() + " | "
                + choose("Hand", "Puncte în această mână") + ": " + handValue;
        if (player.streakPoints() != 0) {
            line += choose(" (series ", " (serie ")
                    + (player.streakPoints() > 0 ? "+" : "") + player.streakPoints() + ")";
        }
        return line + " | " + choose("Total", "Total") + ": " + player.totalScore();
    }
    String scoreAccessibility(GameSession.PlayerSnapshot player, int earned) {
        return scoreLine(player, earned);
    }

    private String choose(String english, String romanian) {
        return romanian() ? romanian : english;
    }
}
