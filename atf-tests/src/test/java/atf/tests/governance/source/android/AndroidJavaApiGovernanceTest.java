package atf.tests.governance.source.android;

import atf.testkit.assertions.AuditedAssertions;

import atf.testkit.AtfComponentTest;
import atf.tests.governance.support.GovernanceProject;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Java language support does not supply newer Java library methods on Android. */
@AtfComponentTest
class AndroidJavaApiGovernanceTest {
    @Test void androidProductionAvoidsTheJava16StreamTerminal() throws Exception {
        Path root = GovernanceProject.locateProjectRoot().resolve("whist-lab-app");
        List<String> failures = new ArrayList<>();
        for (String module : List.of("app", "game-core")) {
            try (var files = Files.walk(root.resolve(module + "/src/main/java"))) {
                for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    for (String call : unsupportedCalls(Files.readString(file))) failures.add(root.relativize(file) + ": " + call);
                }
            }
        }
        AuditedAssertions.assertEquals(List.of(), failures, "Use Android-supported collectors or explicit iteration; JVM success does not qualify Android APIs");
    }

    @Test void fluentTerminalsAreRejectedAcrossWhitespaceAndLineBreaks() throws Exception {
        for (String call : List.of("values.stream().toList()", "values.stream().map(v -> v).\n toList()")) {
            AuditedAssertions.assertEquals(1, unsupportedCalls("class Example { Object run(java.util.List<String> values) { return " + call + "; } }").size());
        }
    }

    @Test void supportedCollectorsAndTextMentioningTheMethodRemainValid() throws Exception {
        AuditedAssertions.assertEquals(List.of(), unsupportedCalls("class Example { Object run(java.util.List<String> values) { "
                + "// values.stream().toList()\n String note = \"stream.toList()\"; "
                + "return values.stream().collect(java.util.stream.Collectors.toList()); } }"));
        AuditedAssertions.assertEquals(List.of(), unsupportedCalls("class Example { Object run(java.util.List<String> values) { "
                + "return values.stream().collect(Collectors.toList()); } }"));
    }

    private static List<String> unsupportedCalls(String source) throws Exception {
        JavaFileObject input = new SimpleJavaFileObject(URI.create("string:///Example.java"), JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return source; }
        };
        var compiler = ToolProvider.getSystemJavaCompiler();
        AuditedAssertions.assertNotNull(compiler, "Governance requires the documented JDK");
        List<String> calls = new ArrayList<>();
        try (var manager = compiler.getStandardFileManager(null, null, null)) {
            JavacTask task = (JavacTask) compiler.getTask(null, manager, null, List.of("-proc:none"), null, List.of(input));
            for (var unit : task.parse()) new TreeScanner<Void, Void>() {
                @Override public Void visitMethodInvocation(MethodInvocationTree invocation, Void unused) {
                    if (invocation.getArguments().isEmpty() && invocation.getMethodSelect() instanceof MemberSelectTree member
                            && member.getIdentifier().contentEquals("toList")) {
                        String receiver = member.getExpression().toString();
                        if (!receiver.equals("Collectors") && !receiver.equals("java.util.stream.Collectors")) calls.add(invocation.toString());
                    }
                    return super.visitMethodInvocation(invocation, unused);
                }
            }.scan(unit, null);
        }
        return calls;
    }
}
