package atf.android.e2e.release;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;

import java.util.Set;
import org.junit.jupiter.api.Test;

@AtfComponentTest
class ReleaseAchievementNoticeComponentTest {
    @Test void recognizesEnglishSingularAndPluralNotices() {
        AuditedAssertions.assertEquals(new ReleaseAchievementNotice("Achievement unlocked", "CLOSE"),
                ReleaseAchievementNotice.find(Set.of("Achievement unlocked")).orElseThrow());
        AuditedAssertions.assertEquals(new ReleaseAchievementNotice("Achievements unlocked", "CLOSE"),
                ReleaseAchievementNotice.find(Set.of("Achievements unlocked")).orElseThrow());
    }

    @Test void recognizesRomanianSingularAndPluralNotices() {
        AuditedAssertions.assertEquals(new ReleaseAchievementNotice("Realizare nouă", "ÎNCHIDE"),
                ReleaseAchievementNotice.find(Set.of("Realizare nouă")).orElseThrow());
        AuditedAssertions.assertEquals(new ReleaseAchievementNotice("Realizări noi", "ÎNCHIDE"),
                ReleaseAchievementNotice.find(Set.of("Realizări noi")).orElseThrow());
    }

    @Test void ignoresUnrelatedTextAndAchievementProgressDetails() {
        AuditedAssertions.assertTrue(ReleaseAchievementNotice.find(Set.of("First exact contract", "Progress 1/1")).isEmpty());
    }
}
