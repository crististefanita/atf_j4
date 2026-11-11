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
import java.util.regex.Pattern;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Java language support does not supply newer Java library methods on Android. */
@AtfComponentTest
class AndroidJavaApiGovernanceTest {
    // ai-lab is a game-core test dependency and is not packaged in the Android application.
    private static final List<String> ANDROID_RUNTIME_MODULES =
            List.of("app", "game-core", "atf-card-table-ui", "card-table-names");

    @Test void androidProductionAvoidsTheJava16StreamTerminal() throws Exception {
        Path root = GovernanceProject.locateProjectRoot().resolve("whist-lab-app");
        List<String> failures = new ArrayList<>();
        for (String module : ANDROID_RUNTIME_MODULES) {
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

    @Test void newerCollectionFactoriesAreDesugaredForTheDeclaredMinimumSdk() throws Exception {
        Path project = GovernanceProject.locateProjectRoot().resolve("whist-lab-app");
        Path appBuild = project.resolve("app/build.gradle");
        Path uiBuild = project.resolve("atf-card-table-ui/build.gradle");
        String app = Files.readString(appBuild);
        String ui = Files.readString(uiBuild);
        Pattern factory = Pattern.compile("\\b(?:List|Set|Map)\\s*\\.\\s*(?:of|copyOf)\\s*\\(");
        boolean usesNewCollectionApi = false;
        for (String module : ANDROID_RUNTIME_MODULES) {
            Path sourceRoot = project.resolve(module + "/src/main/java");
            if (!Files.isDirectory(sourceRoot)) continue;
            try (var files = Files.walk(sourceRoot)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    usesNewCollectionApi |= factory.matcher(Files.readString(file)).find();
                }
            }
        }
        if (!usesNewCollectionApi) return;
        AuditedAssertions.assertTrue(hasCollectionDesugaring(app),
                "App must backport Java collection factories below their platform API level");
        AuditedAssertions.assertTrue(hasCollectionDesugaring(ui),
                "Android UI library lint and instrumentation must use the same backport");
    }

    private static boolean hasCollectionDesugaring(String buildFile) {
        return buildFile.contains("coreLibraryDesugaringEnabled true")
                && buildFile.contains("com.android.tools:desugar_jdk_libs:2.1.5");
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
