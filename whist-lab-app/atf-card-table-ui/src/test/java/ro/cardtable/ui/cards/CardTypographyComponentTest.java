package ro.cardtable.ui.cards;

import atf.gradle.audit.GradleAuditedTest;
import atf.gradle.audit.GradleInputs;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static atf.gradle.audit.assertions.GradleAssertions.assertEquals;

/** Independent expected measurements, shared with the packaged browser adapter checks. */
public final class CardTypographyComponentTest extends GradleAuditedTest {
    @Test public void everyFaceRoleUsesTheApprovedRankSizeWithoutEnlargingSuitsOrBounds() throws Exception {
        try (var input = getClass().getResourceAsStream("/card-typography.tsv")) {
            if (input == null) throw new IllegalStateException("Missing typography contract");
            String fixture = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            GradleInputs.fixture("APK/Web face typography contract", "measurements", fixture);
            for (String line : fixture.lines().filter(row -> !row.startsWith("#")).toList()) {
                String[] values = line.split("\t");
                var face = CardFaceMetrics.atHeight(Integer.parseInt(values[0]));
                assertEquals("Face width at " + values[0], Integer.parseInt(values[1]), face.widthDp());
                assertEquals("Rank SP at " + values[0], Float.parseFloat(values[2]), face.rankSizeSp(), .00001f);
                assertEquals("Suit SP at " + values[0], Float.parseFloat(values[3]), face.indexSizeSp(), .00001f);
                assertEquals("Inverted rank at " + values[0], Float.parseFloat(values[4]),
                        CardFaceMetrics.INVERTED_RANK_SIZE_DP * face.scale(), .00001f);
                assertEquals("Inverted suit at " + values[0], Float.parseFloat(values[5]),
                        CardFaceMetrics.INVERTED_SIZE_DP * face.scale(), .00001f);
                assertEquals("Central suit at " + values[0], Float.parseFloat(values[6]),
                        CardFaceMetrics.CENTER_SUIT_SIZE_DP * face.scale(), .00001f);
            }
        }
    }
}
