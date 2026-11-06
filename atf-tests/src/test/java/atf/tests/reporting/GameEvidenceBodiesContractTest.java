package atf.tests.reporting;

import org.junit.jupiter.api.extension.RegisterExtension;

import atf.testkit.AtfComponentTest;

import static atf.testkit.assertions.AuditedAssertions.*;
import atf.reporting.model.ReportObservation;
import atf.reporting.security.SensitiveDataRedactor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

@AtfComponentTest
class GameEvidenceBodiesContractTest {
    @RegisterExtension static final GameReportAudit AUDIT = new GameReportAudit();
    private static final ObjectMapper JSON = new ObjectMapper();
    @Test void exactTextBoundariesRetainEveryCharacterAndUnicodeScalar() {
        for (int length : new int[] { 15, 16, 17 }) {
            String original = "x".repeat(length) + "\r\n♠😀last & &#13;";
            var parts = GameEvidenceBodies.prepare(body(original, ReportObservation.Format.TEXT), 16, new SensitiveDataRedactor());
            assertEquals(original, parts.stream().map(ReportObservation.Body::text).reduce("", String::concat));
            assertTrue(parts.stream().allMatch(p -> p.text().length() <= 16));
            assertTrue(parts.stream().noneMatch(p -> !p.text().isEmpty() && Character.isHighSurrogate(p.text().charAt(p.text().length() - 1))));
        }
    }
    @Test void jsonFragmentsRemainWholeTypedDocumentsWithEveryOrderedArrayValue() throws Exception {
        var expected = JSON.createObjectNode(); expected.put("integer", "9007199254740993");
        var values = expected.putObject("game").putArray("scores"); for (int i = 0; i < 31; i++) values.add(-i);
        var parts = GameEvidenceBodies.prepare(body(expected.toString(), ReportObservation.Format.JSON), 100, new SensitiveDataRedactor());
        assertTrue(parts.size() > 1); var combined = JSON.createObjectNode(); var actualScores = JSON.createArrayNode();
        for (var part : parts) {
            assertTrue(part.text().length() <= 100); var value = JSON.readTree(part.text());
            if (value.has("integer")) combined.set("integer", value.get("integer"));
            if (value.has("game")) value.path("game").path("scores").forEach(actualScores::add);
        }
        combined.putObject("game").set("scores", actualScores); assertEquals(expected, combined);
        assertEquals(-30, actualScores.get(30).asInt());
    }
    @Test void anIndivisibleJsonValueIsRefusedInsteadOfPublishingHalfJsonOrAnExternalEscape() {
        assertThrows(IllegalArgumentException.class, () -> GameEvidenceBodies.prepare(
                body("{\"state\":\"" + "x".repeat(600) + "\"}", ReportObservation.Format.JSON), 100, new SensitiveDataRedactor()));
    }
    @Test void emptyContainersCannotDisappearWhenTheBudgetIsTooSmall() {
        for (String value : List.of("{}", "[]")) assertThrows(IllegalArgumentException.class,
                () -> GameEvidenceBodies.prepare(body(value, ReportObservation.Format.JSON), 1, new SensitiveDataRedactor()));
    }
    @Test void secretsAreRedactedBeforeSplittingAndBudgeting() {
        var parts = GameEvidenceBodies.prepare(body("{\"password\":\"keep-secret-out\",\"value\":-7}", ReportObservation.Format.JSON),
                100, new SensitiveDataRedactor());
        assertFalse(parts.toString().contains("keep-secret-out"));
        assertTrue(parts.toString().contains("***REDACTED***"));
    }
    @Test void nativeNumericSpellingsSurviveWholeAndFragmentedJsonBodies() {
        String value="{\"negative\":-0,\"decimal\":9007199254740993.0100,\"exponent\":1E+400,\"values\":["
                +java.util.stream.IntStream.range(0,31).mapToObj(Integer::toString).collect(java.util.stream.Collectors.joining(","))+"]}";
        for(int limit:List.of(4096,128)){
            var parts=GameEvidenceBodies.prepare(body(value,ReportObservation.Format.JSON),limit,new SensitiveDataRedactor());
            String serialized=parts.stream().map(ReportObservation.Body::text).reduce("",String::concat);
            assertTrue(serialized.contains("9007199254740993.0100"));assertTrue(serialized.contains("1E+400"));
            assertTrue(java.util.regex.Pattern.compile("\\\"negative\\\"\\s*:\\s*-0(?:\\s|,|})").matcher(serialized).find());
            assertTrue(parts.stream().allMatch(part->part.text().length()<=limit));
            assertEquals(limit==128,parts.size()>1);
        }
    }
    @Test void ambiguousOrTrailingJsonCannotBeSilentlyNormalizedAway() {
        for(String value:List.of("{\"n\":1,\"n\":2}","{} {}"," "))assertThrows(IllegalArgumentException.class,
                ()->GameEvidenceBodies.prepare(body(value,ReportObservation.Format.JSON),512,new SensitiveDataRedactor()));
    }
    private static ReportObservation.Body body(String value, ReportObservation.Format format) {
        return new ReportObservation.Body("Captured state", value, format);
    }
}
