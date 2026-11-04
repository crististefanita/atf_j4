package atf.tests.governance.source;

import atf.core.files.AtfJavaSourceReferences;
import atf.tests.governance.support.GovernanceProject;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

/** Lexical guards intentionally ignore comments, strings and text blocks through the Core parser. */
final class SourceQualityRules {
    static final Pattern PACKAGE = Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;");
    private static final Pattern IMPORT = Pattern.compile(
            "\\bimport\\s+(static\\s+)?([\\w$]+(?:\\s*\\.\\s*(?:[\\w$]+|\\*))*)\\s*;");
    private SourceQualityRules() { }

    static List<String> duplicateImports(String source) {
        var seen = new HashSet<String>();
        var duplicates = new ArrayList<String>();
        var imports = IMPORT.matcher(AtfJavaSourceReferences.codeOnly(source));
        while (imports.find()) {
            String canonical = (imports.group(1) == null ? "" : "static ") + imports.group(2).replaceAll("\\s+", "");
            if (!seen.add(canonical)) duplicates.add(canonical);
        }
        return List.copyOf(duplicates);
    }

    static List<String> inspect(GovernanceProject.SourceFile file, String source, List<String> forbidden) {
        var result = new ArrayList<String>();
        String code = AtfJavaSourceReferences.codeOnly(source);
        var declaration = PACKAGE.matcher(code);
        String expected = file.sourceRoot().relativize(file.file().getParent()).toString().replace('\\', '.').replace('/', '.');
        if (!declaration.find() || !declaration.group(1).equals(expected)) result.add("GOV-PACKAGE-PATH: expected " + expected);
        duplicateImports(source).forEach(value -> result.add("GOV-IMPORT-DUPLICATE: " + value));
        for (String prefix : forbidden) {
            if (AtfJavaSourceReferences.containsNamespace(source, prefix)) result.add("GOV-BOUNDARY: forbidden " + prefix);
        }
        return List.copyOf(result);
    }

    static java.util.Map<String, List<String>> packageInventory(List<GovernanceProject.SourceFile> files) throws Exception {
        var result = new java.util.TreeMap<String, List<String>>();
        for (var file : files) {
            var declaration = PACKAGE.matcher(AtfJavaSourceReferences.codeOnly(Files.readString(file.file())));
            if (!declaration.find()) throw new IllegalStateException("GOV-PACKAGE-MISSING: " + file.file());
            result.computeIfAbsent(file.scope() + ":" + declaration.group(1), ignored -> new ArrayList<>())
                    .add(file.file().getFileName().toString());
        }
        result.values().forEach(values -> values.sort(String::compareTo));
        return result;
    }
}
