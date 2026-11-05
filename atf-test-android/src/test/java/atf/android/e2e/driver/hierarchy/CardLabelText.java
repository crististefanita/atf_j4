package atf.android.e2e.driver.hierarchy;

import java.util.regex.Pattern;

/** Convenția textuală a cărților, comună decodării și selectorilor de joc. */
final class CardLabelText {
    private CardLabelText() {}

    private static final Pattern PLAYING_CARD = Pattern.compile("(?:[2-9]|10|J|Q|K|A)[♠♥♣♦]");

    static boolean isPlayingCard(String text) {
        return PLAYING_CARD.matcher(text).matches();
    }

    static String normalize(String text) {
        String compact = text.replace("\n", "").replace(" ", "");
        return isPlayingCard(compact) ? compact : text;
    }

    static String accessibleName(String cardLabel) {
        if (cardLabel.length() < 2)
            throw new IllegalArgumentException("Invalid card label: " + cardLabel);
        String rank = cardLabel.substring(0, cardLabel.length() - 1);
        String rankName =
                switch (rank) {
                    case "J" -> "Jack";
                    case "Q" -> "Queen";
                    case "K" -> "King";
                    case "A" -> "Ace";
                    default -> rank;
                };
        String suit =
                switch (cardLabel.substring(cardLabel.length() - 1)) {
                    case "♠" -> "spades";
                    case "♥" -> "hearts";
                    case "♦" -> "diamonds";
                    case "♣" -> "clubs";
                    default ->
                            throw new IllegalArgumentException("Invalid card label: " + cardLabel);
                };
        return rankName + " of " + suit;
    }
}
