package ro.whistlab.core.rules;

import atf.gradle.audit.GradleAuditedTest;
import atf.gradle.audit.GradleInputs;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;

import static atf.gradle.audit.assertions.GradleAssertions.assertEquals;
import static atf.gradle.audit.assertions.GradleAssertions.assertTrue;

/** Literal examples paired with their localized in-app help and observed engine results. */
@RunWith(Parameterized.class)
public class HowToPlayDocumentationTest extends GradleAuditedTest {
    private static final GameMode MODE = GameMode.ROMANIAN_WHIST;
    private static final List<String> TOPICS = List.of("goal", "deck", "schedule", "bidding", "following",
            "trumping", "winner", "scoring", "series", "zero-game", "variants");

    @Parameterized.Parameters(name = "language={0}, example={1}")
    public static List<Object[]> cases() {
        var cases = new ArrayList<Object[]>();
        for (var language : HowToPlay.Language.values()) {
            for (String example : List.of("follow-after-trump", "mandatory-undertrump", "void-discard",
                    "no-trump-discard", "trick-winner", "last-bid", "exact-zero", "over-contract", "under-contract",
                    "series-five", "series-ten", "series-missed", "one-card-preserve", "one-card-break",
                    "schedule", "deck")) {
                cases.add(new Object[] {language, example});
            }
        }
        return cases;
    }

    private final HowToPlay.Language language;
    private final String example;

    public HowToPlayDocumentationTest(HowToPlay.Language language, String example) {
        this.language = language;
        this.example = example;
    }

    @Test public void documentedExampleMatchesTheImplementedRule() {
        String topic = switch (example) {
            case "follow-after-trump" -> "following";
            case "mandatory-undertrump", "void-discard", "no-trump-discard" -> "trumping";
            case "trick-winner" -> "winner";
            case "last-bid" -> "bidding";
            case "exact-zero", "over-contract", "under-contract" -> "scoring";
            case "schedule" -> "schedule";
            case "deck" -> "deck";
            default -> "series";
        };
        var sections = HowToPlay.sections(language);
        var help = sections.stream().filter(section -> section.id().equals(topic)).findFirst().orElseThrow();
        GradleInputs.fixture("Localized help rule", "language", language, "profile", MODE,
                "heading", help.title(), "explanation", help.explanation());
        switch (example) {
            case "follow-after-trump" -> followAfterTrump();
            case "mandatory-undertrump" -> mandatoryUndertrump(help);
            case "void-discard" -> discardWhenVoid(Trump.SPADES);
            case "no-trump-discard" -> discardWhenVoid(Trump.NO_TRUMP);
            case "trick-winner" -> trickWinner();
            case "last-bid" -> lastBid();
            case "exact-zero" -> baseScore(0, 0, 5);
            case "over-contract" -> baseScore(2, 4, -2);
            case "under-contract" -> baseScore(4, 2, -2);
            case "series-five" -> series(5, 2, 4, 5);
            case "series-ten" -> series(10, 2, 4, 10);
            case "series-missed" -> series(10, 0, -4, -10);
            case "one-card-preserve" -> oneCard(0, 4);
            case "one-card-break" -> oneCard(1, 0);
            case "schedule" -> schedule();
            case "deck" -> reducedDeck();
            default -> throw new AssertionError("Unknown example: " + example);
        }
        assertEquals("Both languages cover the same stable rule topics", TOPICS,
                sections.stream().map(HowToPlay.Section::id).toList());
    }

    private static void followAfterTrump() {
        var heart = new Card(Suit.HEARTS, 7);
        var hand = List.of(heart, new Card(Suit.SPADES, 14));
        GradleInputs.fixture("Follow suit even after a trump", "hand", hand, "led suit", Suit.HEARTS,
                "earlier trump", new Card(Suit.SPADES, 9), "trump", Trump.SPADES);
        assertEquals("The led heart remains compulsory", List.of(heart),
                GameRules.legalCards(hand, Suit.HEARTS, Trump.SPADES, MODE));
    }

    private void mandatoryUndertrump(HowToPlay.Section help) {
        boolean romanian = language == HowToPlay.Language.RO;
        assertEquals("The localized rule heading", romanian ? "Atu obligatoriu când nu ai culoarea"
                : "Mandatory trump when void", help.title());
        assertTrue("The help requires trump when void", help.explanation().contains(
                romanian ? "trebuie să pui atu" : "you must play trump"));
        assertTrue("The help permits playing a lower trump", help.explanation().contains(
                romanian ? "nu este obligatoriu să supratai" : "overtrumping is not compulsory"));
        var hand = List.of(new Card(Suit.SPADES, 7), new Card(Suit.SPADES, 9), new Card(Suit.CLUBS, 14));
        GradleInputs.fixture("Void in hearts with two lower trumps", "hand", hand, "led suit", Suit.HEARTS,
                "earlier trump", new Card(Suit.SPADES, 13), "trump", Trump.SPADES);
        assertEquals("Both lower trumps are legal and a discard is forbidden", hand.subList(0, 2),
                GameRules.legalCards(hand, Suit.HEARTS, Trump.SPADES, MODE));
    }

    private static void discardWhenVoid(Trump trump) {
        var hand = List.of(new Card(Suit.CLUBS, 7), new Card(Suit.DIAMONDS, 14));
        GradleInputs.fixture("Void with no required card", "hand", hand, "led suit", Suit.HEARTS, "trump", trump);
        assertEquals("Any held card may be discarded", hand, GameRules.legalCards(hand, Suit.HEARTS, trump, MODE));
    }

    private static void trickWinner() {
        var trick = List.of(new PlayedCard(2, new Card(Suit.HEARTS, 14)),
                new PlayedCard(3, new Card(Suit.SPADES, 7)), new PlayedCard(0, new Card(Suit.CLUBS, 14)),
                new PlayedCard(1, new Card(Suit.SPADES, 9)));
        GradleInputs.fixture("One trick under two trump profiles", "trick", trick,
                "trump options", List.of(Trump.SPADES, Trump.NO_TRUMP));
        assertEquals("The highest trump beats the led ace", 1, GameRules.winningSeat(trick, Trump.SPADES));
        assertEquals("Only the led suit counts at no trump", 2, GameRules.winningSeat(trick, Trump.NO_TRUMP));
    }

    private static void lastBid() {
        GradleInputs.fixture("Four-card bidding with two already bid", "hand size", 4, "previous total", 2);
        assertEquals("The final bid of two is forbidden", List.of(0, 1, 3, 4), GameRules.allowedBids(4, 2, true));
        assertEquals("Intermediate equality is legal", List.of(0, 1, 2, 3, 4), GameRules.allowedBids(4, 2, false));
    }

    private static void baseScore(int bid, int won, int expected) {
        GradleInputs.fixture("Romanian hand score", "bid", bid, "tricks won", won);
        assertEquals("Exact zero earns five and a miss loses the absolute difference", expected,
                GameRules.scoreDelta(won, bid, MODE));
    }

    private static void series(int points, int won, int previous, int expectedAdjustment) {
        GradleInputs.fixture("Fifth eligible result", "hand size", 4, "bid", 2,
                "tricks won", won, "previous streak", previous, "configured magnitude", points);
        var result = RomanianScoring.score(won, 2, 4, previous, points, points);
        assertEquals("Fifth result awards the selected magnitude", expectedAdjustment, result.streakPoints());
        assertEquals("A paid series resets", 0, result.nextStreak());
        assertEquals("Base points stay separate", won == 2 ? 7 : -2, result.basePoints());
    }

    private static void oneCard(int won, int expectedStreak) {
        GradleInputs.fixture("A one-card result during a series", "hand size", 1, "bid", 0,
                "tricks won", won, "previous streak", 4);
        var result = RomanianScoring.score(won, 0, 1, 4, 10);
        assertEquals("One-card result does not award a series", 0, result.streakPoints());
        assertEquals("A matching result preserves four and an opposite result clears the series",
                expectedStreak, result.nextStreak());
    }

    private static void schedule() {
        GradleInputs.fixture("Four-player standard Romanian schedule", "mode", MODE, "player count", 4);
        var sizes = GameRules.handSizes(MODE, 4);
        assertEquals("All 24 hands in exact order",
                List.of(1, 1, 1, 1, 2, 3, 4, 5, 6, 7, 8, 8, 8, 8, 7, 6, 5, 4, 3, 2, 1, 1, 1, 1), sizes);
        assertEquals("Total tricks", 94, sizes.stream().mapToInt(Integer::intValue).sum());
    }

    private static void reducedDeck() {
        var minimum = List.of(9, 7, 5, 3);
        GradleInputs.fixture("Reduced decks", "player counts", List.of(3, 4, 5, 6), "minimum ranks", minimum);
        for (int players = 3; players <= 6; players++) {
            var deck = GameRules.deck(MODE, players);
            assertEquals("Deck count at " + players + " players", players * 8, deck.size());
            assertEquals("Minimum rank at " + players + " players", minimum.get(players - 3).intValue(),
                    deck.stream().mapToInt(Card::rank).min().orElseThrow());
        }
    }
}
