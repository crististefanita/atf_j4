package ro.whistlab.app.cards;

import org.junit.Test;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Suit;
import static atf.gradle.audit.assertions.GradleAssertions.*;

public final class WhistCardFaceComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void allRanksAndSuitsKeepTheVisibleMarksAcrossTheLibraryBoundary() {
        String[] ranks={"2","3","4","5","6","7","8","9","10","J","Q","K","A"};
        for(var suit:Suit.values())for(int rank=2;rank<=14;rank++) {
            var face=WhistCardFace.from(new Card(suit,rank));
            
            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "face", face, "rank", rank, "ranks", ranks, "suit", suit);
            assertEquals("face.rankLabel() must equal ranks[rank - 2]", ranks[rank-2],face.rankLabel());
            assertEquals("face.suitSymbol() must equal switch (suit) { case SPADES -> yield \"\\u2660\"; case HEARTS -> yield \"\\u2665\"; case CLUBS -> yield \"\\u2663\"; case DIAMONDS -> yield \"\\u2666\"; }", switch(suit){case SPADES->"♠";case HEARTS->"♥";case CLUBS->"♣";case DIAMONDS->"♦";},face.suitSymbol());
            assertEquals("face.red() must equal suit == Suit.HEARTS || suit == Suit.DIAMONDS", suit==Suit.HEARTS||suit==Suit.DIAMONDS,face.red());
        }
    }
}
