package ro.cardtable.ui.profile;

import java.util.ArrayList;
import org.junit.Test;
import static atf.gradle.audit.assertions.GradleAssertions.*;

public final class NameEditComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void saveNormalizesBeforeCallingTheProductExactlyOnce() {
        var events=new ArrayList<String>();
        var editor=new NameEdit(raw->{events.add("normalize:"+raw);return "Ștefan";},name->events.add("save:"+name));
        editor.save(atf.gradle.audit.GradleInputs.created("Persistence or profile input before save", "  Ștefan  "));
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "editor", editor, "events", events);
        assertEquals("events must equal java.util.List.of(\"normalize: \\u0218tefan \", \"save:\\u0218tefan\")", java.util.List.of("normalize:  Ștefan  ","save:Ștefan"),events);
    }

    @Test public void validationAndStorageFailuresKeepTheirOriginalIdentityWithoutRetry() {
        var invalid=new IllegalArgumentException("invalid");var saved=new ArrayList<String>();
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "invalid", invalid, "saved", saved);
        assertSame("the declared component operation keeps the identity of invalid", invalid,assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->new NameEdit(raw->{throw invalid;},saved::add).save(atf.gradle.audit.GradleInputs.created("Persistence or profile input before save", "bad"))));
        assertTrue("saved.isEmpty() must hold", saved.isEmpty());
        var failed=new IllegalStateException("storage");var attempts=new ArrayList<String>();
        assertSame("the declared component operation keeps the identity of failed", failed,assertThrows("The operation rejects its input with IllegalStateException.class", IllegalStateException.class,()->new NameEdit(raw->raw,name->{attempts.add(name);throw failed;}).save(atf.gradle.audit.GradleInputs.created("Persistence or profile input before save", "Ana"))));
        assertEquals("attempts must equal java.util.List.of(\"Ana\")", java.util.List.of("Ana"),attempts);
    }

    @Test public void resetUsesTheExistingEmptyPreferenceWithoutCallingNameValidation() {
        var saved=new ArrayList<String>();
        new NameEdit(raw->{throw new AssertionError("Reset must not normalize an empty display name");},saved::add).reset();
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "saved", saved);
        assertEquals("saved must equal java.util.List.of(\"\")", java.util.List.of(""),saved);
    }
}
