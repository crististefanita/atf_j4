package atf.tests.governance.source.game;

import atf.android.process.CommandProcess;
import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.governance.support.GovernanceProject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@AtfComponentTest
class GameSessionBoundaryComponentTest {
    @TempDir
    static Path classes;

    @BeforeAll
    static void compileProductionProbe() throws Exception {
        Path root = GovernanceProject.locateProjectRoot();
        String sources = root.resolve("whist-lab-app/game-core/src/main/java") + File.pathSeparator
                + root.resolve("whist-lab-app/card-table-names/src/main/java");
        var result = CommandProcess.execute(List.of(tool("javac"), "--release", "17", "-proc:none",
                "-encoding", "UTF-8", "-sourcepath", sources, "-d", classes.toString(),
                root.resolve("atf-tests/src/test/resources/session/GameSessionProbe.java").toString()),
                Map.of(), Duration.ofMinutes(2), false);
        AuditedAssertions.assertEquals(0, result.exitCode(), result.stderr());
    }

    @Test void resumesBiddingPlayingAndScoreAndAdvancesTheSameMatch() throws Exception {
        AuditedAssertions.assertEquals("bidder=1 actor=1 close=SCORE winner=0 cards=3 totals=[6, 5, -1]"
                + " contracts=[[1, 0, 1]] tricks=[[1, 0, 0]] next=BIDDING/2/0/1", probe("advance"));
    }

    @Test void illegalCommandsAndCorruptCheckpointsAreRefused() throws Exception {
        AuditedAssertions.assertEquals("Rejected wrong bidder, forbidden sum, wrong actor, foreign card and corrupt checkpoint",
                probe("reject"));
    }

    private static String probe(String mode) throws Exception {
        var result = CommandProcess.execute(List.of(tool("java"), "-cp", classes.toString(), "GameSessionProbe", mode),
                Map.of(), Duration.ofSeconds(45), false);
        AuditedAssertions.assertEquals(0, result.exitCode(), result.stderr());
        return new String(result.stdout(), StandardCharsets.UTF_8).strip();
    }

    private static String tool(String name) {
        return Path.of(System.getProperty("java.home"), "bin", name).toString();
    }
}
