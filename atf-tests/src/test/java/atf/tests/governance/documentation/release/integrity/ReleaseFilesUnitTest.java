package atf.tests.governance.documentation.release.integrity;

import atf.testkit.AtfUnitTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class ReleaseFilesUnitTest {
    @ParameterizedTest @ValueSource(strings = {"", "/absolute", "C:/outside", "a\\b", "../a", "a/../b", "a/./b", "a//b", "a/", "a\nb"})
    void nonCanonicalPathsAreRejected(String path) {
        assertThrows(java.io.IOException.class, () -> ReleaseFiles.relative(path));
    }
    @ParameterizedTest @ValueSource(strings = {"file.xml", "reports/rezultate și spații.xml", "reports/test-results/TEST-a$Nested.xml"})
    void regularRelativeNamesRemainUsable(String path) throws Exception { assertEquals(path, ReleaseFiles.relative(path)); }
    @ParameterizedTest @ValueSource(strings = {"1.5", "-1", "\"1\"", "true", "null", "9223372036854775808"})
    void jsonNumbersCannotBeCoercedOrOverflowed(String value) throws Exception {
        var json = new ObjectMapper().readTree("{\"value\":" + value + "}");
        assertThrows(java.io.IOException.class, () -> ReleaseFiles.integer(json, "value"));
    }
    @Test void exactZeroAndLongBoundaryRemainValid() throws Exception {
        var json = new ObjectMapper().readTree("{\"zero\":0,\"maximum\":9223372036854775807}");
        assertEquals(0, ReleaseFiles.integer(json, "zero"));
        assertEquals(Long.MAX_VALUE, ReleaseFiles.integer(json, "maximum"));
    }
}
