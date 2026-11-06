package atf.android.e2e.gameplay;

import atf.testkit.assertions.AuditedAssertions;
import static org.junit.jupiter.api.Assertions.*;
import atf.testkit.AtfComponentTest;
import java.nio.file.*;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@AtfComponentTest
class WhistLabContinuationTest {
    @TempDir Path out;
    @Test void oneCardUsesTheSingularContractWhileLargerHandsUsePlural() {
        AuditedAssertions.assertEquals("1 card in this hand",CompleteWhistLabMatchE2eTest.handSizeLabel(1));
        AuditedAssertions.assertEquals("2 cards in this hand",CompleteWhistLabMatchE2eTest.handSizeLabel(2));
        AuditedAssertions.assertEquals("10 cards in this hand",CompleteWhistLabMatchE2eTest.handSizeLabel(10));
    }
    @Test void onlyFullyRecordedScoreBoundariesCountAsRetainedHands()throws Exception {
        Properties recorded=new Properties();for(int h=0;h<19;h++)recorded.setProperty("hand."+h+".size","1");
        Files.writeString(out.resolve("lab-h01-turn01-lab-state.properties"),"phase=SCORE\nhand=0\nmoves=1,2,3,4\n");
        AuditedAssertions.assertEquals(0,CompleteWhistLabMatchE2eTest.retainedHands(out,recorded));
        Files.writeString(out.resolve("lab-h01-score-parity.xml"),"<hierarchy/>");
        AuditedAssertions.assertEquals(1,CompleteWhistLabMatchE2eTest.retainedHands(out,recorded));
        Files.writeString(out.resolve("lab-h02-score-parity.xml"),"<hierarchy/>");
        Files.writeString(out.resolve("lab-h02-turn01-lab-state.properties"),"phase=PLAYING\nhand=1\nmoves=1,2,3,4\n");
        AuditedAssertions.assertEquals(1,CompleteWhistLabMatchE2eTest.retainedHands(out,recorded));
    }
}
