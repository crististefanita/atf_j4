package atf.tests.documentation.policy;

import atf.tests.documentation.DocumentationGovernanceSupport.Violation;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static atf.tests.documentation.DocumentationGovernanceSupport.violation;

/** Detects reviewed translation artifacts in visible English prose, not technical literals. */
public final class DocumentationTerminologyRules {
    private static final Pattern FENCE = Pattern.compile("^\\s*(`{3,}|~{3,})(.*)$");
    private static final Pattern ARTIFACT = Pattern.compile(
            "\\b(?:SHAA-256|SHA- 256|checksumes|READMEM|READMEL|Caache|assetates|assetses|assetons|assetas|assetos|"
            + "processor charger|classpathin|Lacquer delimited|source snack|Unbug|Quotts)\\b",
            Pattern.CASE_INSENSITIVE);

    private DocumentationTerminologyRules() { }

    public static void validate(Path file, Path root, List<String> lines, String language,
                                List<Violation> violations) {
        if (!"en".equals(language)) return;
        String marker = null;
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            var fence = FENCE.matcher(line);
            if (fence.matches()) {
                if (marker == null) marker = fence.group(1);
                else if (fence.group(1).charAt(0) == marker.charAt(0)
                        && fence.group(1).length() >= marker.length()) marker = null;
                continue;
            }
            if (marker != null) continue;
            String visible = line.replaceAll("`+[^`]*`+", "")
                    .replaceAll("\\]\\((?:[^()]|\\([^)]*\\))*\\)", "]")
                    .replaceAll("https?://\\S+", "");
            if (ARTIFACT.matcher(visible).find()) violations.add(violation("DOC-EN-TERMINOLOGY", root, file,
                    index + 1, "visible English contains a reviewed translation artifact",
                    "operational terms must describe the owning code accurately",
                    "use directory, checksum, asset, receipt, source snapshot or annotation processor as appropriate"));
        }
    }
}
