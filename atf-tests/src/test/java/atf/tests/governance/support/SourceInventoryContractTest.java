package atf.tests.governance.support;

import atf.testkit.assertions.AuditedAssertions;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class SourceInventoryContractTest {
    @Test void mavenDiscoveryHonoursCustomRootsAndKeepsOutputNamedSourcePackages(@TempDir Path root) throws Exception {
        write(root, "module/pom.xml", "<project><build><sourceDirectory>src/main</sourceDirectory></build></project>");
        Path main = write(root, "module/src/main/legal/target/Visible.java", "package legal.target; class Visible {}");
        Path test = write(root, "module/src/test/java/legal/Test.java", "package legal; class Test {}");
        write(root, "module/target/generated/Hidden.java", "not compiled source");
        write(root, "artifacts/stale/pom.xml", "not XML");
        AuditedAssertions.assertEquals(Set.of(main, test), Set.copyOf(AtfJavaSourceInventory.allSources(root)));
    }

    @Test void emptyEscapingAndUnresolvedConfiguredRootsCannotHideSources(@TempDir Path root) throws Exception {
        Path pom = write(root, "module/pom.xml", "<project><build><sourceDirectory>../outside</sourceDirectory></build></project>");
        AuditedAssertions.assertThrows(IOException.class, () -> AtfJavaSourceInventory.allSources(root));
        Files.writeString(pom, "<project><build><sourceDirectory> </sourceDirectory></build></project>");
        AuditedAssertions.assertThrows(IOException.class, () -> AtfJavaSourceInventory.allSources(root));
        Files.writeString(pom, "<project><build><sourceDirectory>${external}</sourceDirectory></build></project>");
        AuditedAssertions.assertThrows(IOException.class, () -> AtfJavaSourceInventory.allSources(root));
        AuditedAssertions.assertThrows(IOException.class, () -> GovernanceProject.safeDirectory(root, "../outside"));
    }

    private static Path write(Path root, String relative, String text) throws IOException {
        Path file = root.resolve(relative); Files.createDirectories(file.getParent()); Files.writeString(file, text); return file;
    }

    @Test void projectInventoryKeepsDeclaredRootsAndNestedModuleOwnership(@TempDir Path root) throws Exception {
        write(root, "config/atf-governance-policy.json", "{\"additionalSourceRoots\":[]}");
        write(root, "modules/domain/pom.xml", """
                <project><build><sourceDirectory>sources</sourceDirectory>
                <testSourceDirectory>checks</testSourceDirectory></build></project>
                """);
        Path main=write(root,"modules/domain/sources/domain/Model.java","package domain; class Model {}");
        Path test=write(root,"modules/domain/checks/domain/ModelTest.java","package domain; class ModelTest {}");
        var files=GovernanceProject.sources(root);
        AuditedAssertions.assertEquals(2,files.size());
        for(var file:files) {
            AuditedAssertions.assertEquals("modules/domain",file.module());
            AuditedAssertions.assertEquals(file.test()?test:main,file.file());
            AuditedAssertions.assertEquals(root.resolve(file.test()?"modules/domain/checks":"modules/domain/sources"),file.sourceRoot());
        }
    }

    @Test void duplicatedOrOverlappingRootsCannotChangeSourceOwnership(@TempDir Path root) throws Exception {
        write(root,"domain/src/main/java/model/Visible.java","package model; class Visible {}");
        String entry="{\"module\":\"domain\",\"path\":\"domain/src/main/java\",\"test\":false}";
        Path policy=write(root,"config/atf-governance-policy.json","{\"additionalSourceRoots\":["+entry+"]}");
        AuditedAssertions.assertEquals(1,GovernanceProject.sources(root).size());
        for(String other:List.of(entry,entry.replace("false","true"),entry.replace("domain/src/main/java","domain/src/main"))) {
            Files.writeString(policy,"{\"additionalSourceRoots\":["+entry+","+other+"]}");
            AuditedAssertions.assertThrows(IOException.class,()->GovernanceProject.sources(root));
        }
    }

    @Test void additionalRootMetadataCannotBeCoercedOrAssignedToAnotherModule(@TempDir Path root) throws Exception {
        write(root,"domain/src/main/java/model/Visible.java","package model; class Visible {}");
        Files.createDirectories(root.resolve("other"));
        for(String entry:List.of(
                "{\"module\":\"domain\",\"path\":\"domain/src/main/java\"}",
                "{\"module\":\"domain\",\"path\":\"domain/src/main/java\",\"test\":\"true\"}",
                "{\"module\":\"other\",\"path\":\"domain/src/main/java\",\"test\":false}",
                "{\"module\":\"\",\"path\":\"domain/src/main/java\",\"test\":false}")) {
            write(root,"config/atf-governance-policy.json","{\"additionalSourceRoots\":["+entry+"]}");
            AuditedAssertions.assertThrows(IOException.class,()->GovernanceProject.sources(root));
        }
    }
}
