package atf.android.e2e.gameplay;

import atf.android.e2e.activation.AndroidE2eTest;
import atf.android.e2e.gameplay.configuration.DefaultGameScenario;
import atf.android.e2e.gameplay.persistence.SavedGameRecoveryScenario;
import atf.android.e2e.gameplay.presentation.SixPlayerTableScenario;
import atf.android.e2e.gameplay.presentation.ThirteenCardDisplayScenario;
import atf.android.e2e.gameplay.presentation.ThirteenCardPlayScenario;
import atf.android.e2e.gameplay.tricks.CompletedTrickScenario;

import org.junit.jupiter.api.Test;

/** Stable JUnit identities for focused Whist Lab acceptance scenarios. */
@AndroidE2eTest
public class WhistLabGameplayE2eTest {
    @Test
    void sixPlayerEightCardHandFitsOneRowAndMenuKeepsScoreAndHomeAccessible() throws Exception {
        SixPlayerTableScenario.run(getClass().getSimpleName());
    }

    @Test
    void thirteenCardBiddingTableFitsAcrossPortraitDisplayProfiles() throws Exception {
        ThirteenCardDisplayScenario.run(getClass().getSimpleName());
    }

    @Test
    void thirteenCardFanKeepsOneRowAndMovesTheChosenCardIntoTheTable() throws Exception {
        ThirteenCardPlayScenario.run(getClass().getSimpleName());
    }

    @Test
    void defaultFourPlayerOhHellMatchesTheObservedTenCardHandAndSuitedTrump() throws Exception {
        DefaultGameScenario.run(getClass().getSimpleName());
    }

    @Test
    void deterministicPracticeTrickScoresExactBidAndResumesAfterProcessDeathOnApi28()
            throws Exception {
        SavedGameRecoveryScenario.run(getClass().getSimpleName());
    }

    @Test
    void completedTrickRemainsRenderedBeforeTheNextTrickBecomesInteractive() throws Exception {
        CompletedTrickScenario.run(getClass().getSimpleName());
    }

    public static String effectiveDisplaySize(String commandOutput) {
        return atf.android.e2e.display.support.GameplayDisplay.effectiveDisplaySize(commandOutput);
    }
}
