package atf.tests.governance.documentation.release.rag.scope;

import atf.android.process.CommandProcess;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.governance.support.GovernanceProject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class NodeReleaseContracts {
    private NodeReleaseContracts() { }
    public static String runContracts(Duration timeout, String... scripts) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        var args = new ArrayList<>(List.of("node", "--test"));
        for (String script : scripts) args.add(root.resolve("atf-tests/tools").resolve(script).toString());
        return runNode(args, root, timeout);
    }

    public static String runNode(List<String> args, Path directory, Duration timeout) throws Exception {
        var result = CommandProcess.execute(args, directory, Map.of(), timeout, true);
        String output = new String(result.stdout(), StandardCharsets.UTF_8);
        AuditedAssertions.assertEquals(0, result.exitCode(),
                result.exitCode() == 0 ? "The Node child completed successfully" : String.join(" ", args) + "\n" + output);
        return output;
    }
}
