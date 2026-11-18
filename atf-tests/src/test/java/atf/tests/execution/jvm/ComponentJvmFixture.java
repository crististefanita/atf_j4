package atf.tests.execution.jvm;

import atf.android.process.CommandProcess;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/** A real local JVM and child process; no device, build tool or network is involved. */
public final class ComponentJvmFixture {
    private ComponentJvmFixture() { }

    public static void main(String[] args) throws Exception {
        if (args[0].equals("child")) {
            for (String property : List.of("atf.test.run.scope", "atf.component.input-part-characters", "atf.governance.gitRoot"))
                if (System.getProperty(property) != null) throw new IllegalStateException("Suite property leaked to child: " + property);
            System.out.write(new byte[] {(byte) 0xff, 0, 1, 13, 10});
            System.err.print("separate-error");
            System.exit(7);
        }
        if (args[0].equals("failure")) {
            System.err.print("controlled-suite-failure");
            System.exit(23);
        }
        var command = List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Xmx32m", "-cp", System.getProperty("java.class.path"), ComponentJvmFixture.class.getName(), "child");
        var child = CommandProcess.execute(command, Map.of(), Duration.ofSeconds(30), false);
        var result = Map.of("scope", System.getProperty("atf.test.run.scope", "missing"),
                "partCharacters", System.getProperty("atf.component.input-part-characters", "missing"),
                "gitRoot", System.getProperty("atf.governance.gitRoot", "missing"),
                "childExit", child.exitCode(), "stdoutBase64", Base64.getEncoder().encodeToString(child.stdout()),
                "stderr", child.stderr());
        new ObjectMapper().writeValue(Path.of(args[1]).toFile(), result);
    }
}
