package ro.cardtable.ui.boundary;

import java.nio.file.*;
import org.junit.Test;
import static atf.gradle.audit.assertions.GradleAssertions.*;

public final class UiLibraryBoundaryComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void sharedTestsCannotImportConsumerRulesOrSessions() throws Exception {
        Path tests = Path.of(System.getProperty("cardui.sourceRoot")).resolve("src/test/java");
        int inspected = 0;
        try (var files = Files.walk(tests)) {
            for (var file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = new String(Files.readAllBytes(file), java.nio.charset.StandardCharsets.UTF_8);
                inspected++;
                for (String owner : new String[]{"ro.whistlab.", "ro.rentzlab.", "ro.atf.holdem."}) {
                    
                    atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "file", file, "inspected", inspected, "owner", owner, "source", source, "tests", tests);
                    assertFalse(file + " imports consumer rules", source.contains("import " + owner));
                }
            }
        }
        assertTrue("No shared test source was inspected", inspected >= 4);
    }

    @Test public void productLibraryHasNoGameEngineAtfOrStorageDependency() throws Exception {
        Path module=Path.of(System.getProperty("cardui.sourceRoot"));int classes=0;
        try(var files=Files.walk(module.resolve("src/main/java"))) {
            for(var file:files.filter(p->p.toString().endsWith(".java")).toList()) {
                String source=new String(Files.readAllBytes(file),java.nio.charset.StandardCharsets.UTF_8);classes++;
                for(String forbidden:new String[]{"ro.whistlab","ro.rentzlab","ro.atf.holdem","import atf.","SharedPreferences","extends Activity"})
                    { 
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "classes", classes, "file", file, "forbidden", forbidden, "module", module, "source", source);
assertFalse(file+" imports product ownership: "+forbidden,source.contains(forbidden)); }
            }
        }
        assertTrue("A missing source inventory cannot pass the boundary",classes>=10);
        String manifest=new String(Files.readAllBytes(module.resolve("src/main/AndroidManifest.xml")),java.nio.charset.StandardCharsets.UTF_8);
        assertFalse("manifest.contains(\"<application\") must remain false", manifest.contains("<application"));assertFalse("manifest.contains(\"uses-permission\") must remain false", manifest.contains("uses-permission"));
    }
}
