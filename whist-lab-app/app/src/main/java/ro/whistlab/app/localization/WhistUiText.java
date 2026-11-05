package ro.whistlab.app.localization;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.rules.HowToPlay;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;

/** Player-facing copy for the Android UI. Rule calculations stay in game-core. */
public final class WhistUiText {
    public static final List<GameMode> MENU_MODES = List.of(GameMode.values());
    private final HowToPlay.Language language;

    public WhistUiText(HowToPlay.Language language) {
        this.language = language == null ? HowToPlay.Language.EN : language;
    }

    public static HowToPlay.Language parseLanguage(String value) {
        if (value == null) return HowToPlay.Language.EN;
        try {
            return HowToPlay.Language.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException invalid) {
            return HowToPlay.Language.EN;
        }
    }

    public HowToPlay.Language language() { return language; }
    public boolean romanian() { return language == HowToPlay.Language.RO; }

    public String languageLabel() { return choose("Language", "Limba"); }
    public String homeTitle() { return "Whist"; }
    public String newGame() { return choose("New game", "Joc nou"); }
    public String resumeGame() { return choose("Resume game", "Reia partida"); }
    public String help() { return choose("How to play", "Reguli de joc"); }
    public String setupTitle() { return choose("Game setup", "Setări joc"); }
    public String rulesAndSchedule() { return choose("Rules & schedule", "Reguli și program"); }
    public String scoringDetails() { return choose("Scoring details", "Calculul punctajului"); }
    public String players() { return choose("Players", "Jucători"); }
    public String gameType() { return choose("Game type", "Varianta de joc"); }
    public String aiLevel() { return choose("AI level", "Nivelul adversarilor"); }
    public String streakToggle() { return choose("Romanian five-hand streaks (equal reward and penalty)",
            "Serii românești de cinci mâini (bonus și penalizare egale)"); }
    public String streakBonusLabel() { return choose("Five-hand reward / penalty", "Bonus / penalizare după cinci mâini"); }
    public String fivePointsOriginal() { return choose("±5 points", "±5 puncte"); }
    public String tenPoints() { return choose("±10 points", "±10 puncte"); }
    public String startGame() { return choose("Start game", "Începe partida"); }
    public String back() { return choose("Back", "Înapoi"); }
    public String placeBid() { return choose("Place bid", "Licitează"); }
    public String scoreSheet() { return choose("Score sheet", "Scorul partidei"); }
    public String scoreHistory() { return choose("Score history", "Istoricul scorului"); }
    public String unknownBid() { return choose("unrecorded", "neînregistrat"); }
    public String historyHand() { return choose("Hand", "Mână"); }
    public String historyPoints() { return choose("Points", "Puncte"); }
    public String earlierBalance(int hand, int total) {
        return choose("Total after hand " + hand + ": " + total + ". Earlier hand scores are unavailable.",
                "Total după mâna " + hand + ": " + total + ". Punctajele anterioare pe mâini nu sunt disponibile.");
    }
    public String mainMenu() { return choose("Main menu", "Meniu principal"); }
    public String gameMenu() { return choose("Game menu", "Meniu joc"); }
    public String nextHand() { return choose("Next hand", "Mâna următoare"); }
    public String playAgain() { return choose("Play again", "Joacă din nou"); }
    public String currentScores(int hand) { return choose("Current scores · Hand " + hand,
            "Scor curent · Mâna " + hand); }
    public String close() { return choose("Close", "Închide"); }
    public String gameComplete() { return choose("Game complete", "Partidă încheiată"); }
    public String handComplete() { return choose("Hand complete", "Mână încheiată"); }
    public String unsupportedSavedMode() { return choose("This saved game uses a retired mode. Starting a new game will replace it.",
            "Partida salvată folosește o variantă retrasă. O partidă nouă o va înlocui."); }
    public String noSavedGame() { return choose("Saved game could not be resumed and was cleared.",
            "Partida salvată nu a putut fi reluată și a fost ștearsă."); }
    public String invalidBid() { return choose("That bid is not available. Choose another value.",
            "Licitația nu este disponibilă. Alege altă valoare."); }
    public String invalidCard() { return choose("That card cannot be played now.",
            "Cartea nu poate fi jucată în acest moment."); }
    public String bidSelector() { return choose("Choose your bid", "Alege licitația"); }
    public String bidPickerDescription() { return choose("Bid value", "Valoarea licitației"); }
    public String bidChoiceHeading() { return choose("YOUR BID", "LICITAȚIA TA"); }
    public String bidChoiceDescription(int bid, boolean selected) {
        return selected ? choose("Bid " + bid + ", selected", "Licitația " + bid + ", selectată")
                : choose("Select bid " + bid, "Selectează licitația " + bid);
    }
    public String spinnerLabel(String id) {
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

    public String modeName(GameMode mode) {
        return switch (mode) {
            case OH_HELL -> "Oh Hell";
            case DESCENDING -> choose("Descending", "Descrescător");
            case ROMANIAN_WHIST -> choose("Romanian (1–8–1)", "Românesc (1–8–1)");
            case DOWN_AND_UP -> choose("Down & Up", "Coborâre și urcare");
            case LONG_ROMANIAN -> choose("Long Romanian (8-1-8)", "Whist românesc lung (8–1–8)");
            case REVERSE_OH_HELL -> choose("Reverse Oh Hell", "Oh Hell inversat");
        };
    }

    public String difficultyName(AiDifficulty difficulty) {
        return switch (difficulty.name()) {
            case "NORMAL" -> choose("Normal", "Normal");
            case "HARD" -> choose("Hard", "Dificil");
            case "EXPERT" -> choose("Expert (simulation)", "Expert (simulare)");
            default -> difficulty.label();
        };
    }

    public String modeDescription(GameMode mode) {
        return switch (mode) {
            case ROMANIAN_WHIST -> choose(
                    "Reduced deck; 1–8–1 schedule. Follow suit; if void, play trump when held. Eight-card hands have no trump. Exact bid: 5 + tricks; a miss loses the bid difference.",
                    "Pachet redus; program 1–8–1. Respectă culoarea; dacă nu ai culoarea cerută, joci atu dacă ai. Mâinile de opt cărți sunt fără atu. Contract exact: 5 + levate; la ratare pierzi diferența dintre licitat și câștigat.");
            case LONG_ROMANIAN -> choose(
                    "Reduced deck; 8–1–8 schedule. Romanian Whist follow-suit, mandatory-trump and scoring rules apply.",
                    "Pachet redus; program 8–1–8. Se aplică regulile de Whist românesc privind culoarea, atuul obligatoriu și punctajul.");
            case OH_HELL -> choose(
                    "Hand size decreases to one, then increases again (4 players: 10–1–10, 6 players: 8–1–8). Follow suit; when void, discard or trump. Each trick is one point; an exact bid adds ten.",
                    "Numărul de cărți scade până la una, apoi crește (4 jucători: 10–1–10, 6 jucători: 8–1–8). Respectă culoarea; când nu o ai, poți decarta sau tăia cu atu. Fiecare levată valorează un punct; contractul exact adaugă zece.");
            case DESCENDING -> choose(
                    "Starts at the largest hand that fits the deck and decreases to one. Trump changes each hand. Follow suit; when void, discard or trump.",
                    "Începe cu cea mai mare mână care încape în pachet și scade până la una. Atuul se schimbă la fiecare mână. Respectă culoarea; când nu o ai, poți decarta sau tăia cu atu.");
            case DOWN_AND_UP -> choose(
                    "Starts at the largest hand that fits the deck, decreases to one and rises again. Trump changes each hand; an exact bid adds ten.",
                    "Începe cu cea mai mare mână care încape în pachet, scade până la una și apoi crește. Atuul se schimbă la fiecare mână; contractul exact adaugă zece.");
            case REVERSE_OH_HELL -> choose(
                    "Starts with one card and grows to the table limit, then decreases. An undealt turned card sets trump; an exact bid adds ten.",
                    "Începe cu o carte, crește până la limita mesei, apoi scade. O carte întoarsă și neîmpărțită stabilește atuul; contractul exact adaugă zece.");
        };
    }

    public String schedulePreview(GameMode mode, int playerCount) {
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

    public String progress(GameSession.Snapshot state) {
        String label = romanian() ? "Mâna " : "Hand ";
        return label + state.handNumber() + " / "
                + GameRules.handSizes(state.mode(), state.players().size()).size();
    }

    public String gameTitle(GameMode mode) {
        return "Whist · " + (mode == GameMode.ROMANIAN_WHIST ? choose("Romanian", "Românesc") : modeName(mode));
    }

    public String bidSummary(GameSession.Snapshot state) {
        if (romanian()) {
            return "Licitații depuse " + state.bidsSubmitted() + "/" + state.players().size()
                    + "\nTotal licitat " + state.bidTotal() + "/" + state.handSize();
        }
        return "Bids submitted " + state.bidsSubmitted() + "/" + state.players().size()
                + "\nBid total " + state.bidTotal() + "/" + state.handSize();
    }

    public String roundStatus(GameSession.Snapshot state) {
        return progress(state) + " · " + bidSummary(state);
    }

    public String playHint(GameSession.Snapshot state) {
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

    public String winner(GameSession.Snapshot state) {
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

    public String scoreRule(GameSession.Snapshot state) {
        return state.mode().isRomanian()
                ? choose("Exact bid: 5 + tricks. Otherwise, lose the bid difference.",
                        "Contract exact: 5 + levate. Altfel, pierzi diferența dintre licitat și câștigat.")
                : choose("Each trick is one point. An exact bid adds ten points.",
                        "Fiecare levată valorează un punct. Contractul exact adaugă zece puncte.");
    }

    public String streakSummary(GameSession.Snapshot state) {
        return choose("Five eligible exact contracts: +" + state.streakBonusPoints()
                        + "; five misses: −" + state.streakPenaltyPoints() + ", then reset. One-card hands do not advance a streak; an opposite result breaks it.",
                "Cinci contracte exacte eligibile: +" + state.streakBonusPoints()
                        + "; cinci ratări: −" + state.streakPenaltyPoints() + ", apoi seria se resetează. Mâinile cu o carte nu avansează seria; un rezultat opus o întrerupe.");
    }

    public String playerName(GameSession.PlayerSnapshot player) {
        return displayName(player.name(), player.human());
    }

    public String displayName(String name, boolean human) {
        return human && (name.equals("You") || name.equals("Tu")) ? choose("You", "Tu") : name;
    }

    public String playerSettings() { return choose("Your name", "Numele tău"); }
    public String nameHint() { return choose("1–10 letters or digits", "1–10 litere sau cifre"); }
    public String nameError() { return choose("Use a short, unique name (1–10 characters).", "Alege un nume scurt și unic (1–10 caractere)."); }
    public String save() { return choose("Save", "Salvează"); }
    public String resetName() { return choose("Reset name", "Resetează numele"); }
    public String highestScores() { return choose("High scores", "Clasamente"); }
    public String sampleScore() { return choose("Sample", "Exemplu"); }
    public String highScoreHint() { return choose("Local top 20 · sample entries are marked · AI scores included",
            "Top 20 local · exemplele sunt marcate · include scoruri AI"); }
    public String storageError() { return choose("Could not save locally. Please try again.", "Salvarea locală a eșuat. Încearcă din nou."); }
    public String historyPending() { return choose("not completed", "neîncheiată"); }
    public String cardsHeading() { return choose("Cards", "Cărți"); }

    public String bidLabel() { return choose("Bid", "Licitat"); }
    public String tricksLabel() { return choose("Tricks", "Levate"); }
    public String tricksWonLabel() { return choose("Tricks won", "Levate câștigate"); }
    public String scoreLabel() { return choose("Score", "Scor"); }
    public String pointsLabel() { return choose("pts", "pct."); }
    public String scoreColumnPlayer() { return choose("Player", "Jucător"); }
    public String scoreColumnBid() { return choose("Bid", "Lic."); }
    public String scoreColumnTricks() { return choose("Won", "Lev."); }
    public String scoreColumnHand() { return choose("Hand", "Pct."); }
    public String scoreColumnTotal() { return choose("Total", "Total"); }
    public String bidNotPlaced() { return choose("Bid not placed", "Licitația nu a fost depusă"); }
    public String dealerLabel() { return choose("DEALER", "ÎMPĂRȚITOR"); }
    public String currentTurnLabel() { return choose("Current turn", "La rând"); }
    public String yourTurn() { return choose("Your turn", "Este rândul tău"); }
    public String waitingFor(String player) { return choose("Waiting for " + player + ".", "Îl așteaptă pe " + player + "."); }
    public String currentTrick() { return choose("Current trick", "Levată curentă"); }
    public String noCardsYet() { return choose("No cards have been played in this hand yet.", "Nu s-a jucat nicio carte în această mână."); }
    public String trumpHeading() { return choose("TRUMP", "ATU"); }
    public String noTrump() { return choose("No trump", "Fără atu"); }
    public String historyBidHeading() { return choose("B", "L"); }
    public String historyPointsHeading() { return "P"; }
    public String historyTotalHeading() { return "T"; }
    public String historyLegend() {
        return choose("B Bid · P Points · T Total\nDiagonal slash: missed contract",
                "L Licitat · P Puncte · T Total\nTăietură oblică: contract ratat");
    }
    public String historyContractOutcome(ro.whistlab.core.session.ScoreHistory.Outcome outcome) {
        return switch (outcome) {
            case EXACT -> choose("Contract made", "Contract realizat");
            case MISSED -> choose("Missed contract", "Contract ratat");
            case UNKNOWN -> choose("Contract outcome unknown", "Rezultat necunoscut");
        };
    }
    public String seatName(GameSession.PlayerSnapshot player) { return playerName(player); }
    public String seatWonLabel() { return choose("Won", "Levate"); }
    public String seatFacts(GameSession.PlayerSnapshot player) {
        return bidLabel() + ": " + (player.bid() == null ? "—" : player.bid())
                + "\n" + seatWonLabel() + ": " + player.tricksWon();
    }
    public String seatTurnAction(boolean bidding) {
        return bidding ? choose("BID NOW", "LICITEAZĂ ACUM")
                : choose("PLAY NOW", "JOACĂ ACUM");
    }
    public String seatTurnBadge(boolean bidding) {
        return bidding ? choose("BID NOW", "LICITEAZĂ")
                : choose("PLAY NOW", "JOACĂ ACUM");
    }
    public String seatAccessibility(GameSession.PlayerSnapshot player, boolean dealer, boolean active,
            boolean bidding) {
        String description = playerName(player) + ". " + bidLabel() + " "
                + (player.bid() == null ? choose("not placed", "nepusă") : player.bid())
                + ". " + tricksWonLabel() + " " + player.tricksWon() + ". "
                + scoreLabel() + " " + player.totalScore() + " " + pointsLabel();
        if (dealer) description += ". " + dealerLabel();
        if (active) description += ". " + currentTurnLabel() + ". " + seatTurnAction(bidding);
        return description;
    }
    public String trickPlayer(GameSession.PlayerSnapshot player) { return playerName(player); }
    public String trickCardAccessibility(GameSession.PlayerSnapshot player, Card card) {
        return playerName(player) + choose(" played ", " a jucat ") + cardLabel(card);
    }
    public String handAccessibility(int count) {
        return romanian() ? "Cărțile tale, " + count + "." : "Your hand, " + count + " cards.";
    }
    public String cardState(boolean bidding, boolean humanTurn, boolean playable) {
        if (bidding) return choose("Visible while bidding", "Vizibilă la licitare");
        if (!humanTurn) return choose("Waiting for your turn", "Așteaptă rândul tău");
        if (playable) return choose("Legal card", "Carte legală");
        return choose("Not legal now", "Nu poate fi jucată acum");
    }
    public String cardLabel(Card card) {
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
    public String suitName(Suit suit) {
        return switch (suit) {
            case SPADES -> choose("spades", "pică");
            case HEARTS -> choose("hearts", "cupă");
            case CLUBS -> choose("clubs", "treflă");
            case DIAMONDS -> choose("diamonds", "caro");
        };
    }
    public String trumpAccessibility(GameSession.Snapshot state) {
        if (state.turnedCard() != null) {
            return choose("Trump set by turned card: ", "Atu stabilit de cartea întoarsă: ")
                    + cardLabel(state.turnedCard());
        }
        return state.trump().suit().map(suit -> choose("Trump suit only, no turned card: ",
                        "Atu de culoare, fără carte întoarsă: ") + suitName(suit))
                .orElse(noTrump());
    }
    public String trumpIndicatorValue(GameSession.Snapshot state) {
        if (state.turnedCard() != null) return state.turnedCard().shortLabel();
        return state.trump().suit().map(Suit::symbol).orElse(choose("NO TRUMP", "FĂRĂ ATU"));
    }
    public String tableAccessibility(int seats) {
        return romanian() ? "Masa de joc, " + seats + " jucători." : "Game table, " + seats + " players.";
    }
    public String boardSurface() { return choose("Playing surface", "Suprafața de joc"); }
    public String scoreLine(GameSession.PlayerSnapshot player, int earned) {
        return scoreLine(player, (earned >= 0 ? "+" : "") + earned);
    }
    public String scoreLine(GameSession.PlayerSnapshot player, String handValue) {
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
    public String scoreAccessibility(GameSession.PlayerSnapshot player, int earned) {
        return scoreLine(player, earned);
    }

    private String choose(String english, String romanian) {
        return romanian() ? romanian : english;
    }
}
