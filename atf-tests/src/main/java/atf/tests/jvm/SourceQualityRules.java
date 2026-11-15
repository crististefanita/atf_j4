package atf.tests.jvm;

import atf.core.files.AtfJavaSourceReferences;
import atf.tests.jvm.JvmSourceInventory.SourceFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

/** Lexical guards intentionally ignore comments, strings and text blocks through the Core parser. */
public final class SourceQualityRules {
    static final Pattern PACKAGE = Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;");
    private static final Pattern IMPORT = Pattern.compile(
            "\\bimport\\s+(static\\s+)?([\\w$]+(?:\\s*\\.\\s*(?:[\\w$]+|\\*))*)\\s*;");
    private SourceQualityRules() { }

    public static List<String> duplicateImports(String source) {
        var seen = new HashSet<String>();
        var duplicates = new ArrayList<String>();
        var imports = IMPORT.matcher(AtfJavaSourceReferences.codeOnly(source));
        while (imports.find()) {
            String canonical = (imports.group(1) == null ? "" : "static ") + imports.group(2).replaceAll("\\s+", "");
            if (!seen.add(canonical)) duplicates.add(canonical);
        }
        return List.copyOf(duplicates);
    }

    public static Observation inspect(SourceFile file, String source, List<String> forbidden) {
        var result = new ArrayList<String>();
        String code = AtfJavaSourceReferences.codeOnly(source);
        var declaration = PACKAGE.matcher(code);
        String expected = file.sourceRoot().relativize(file.file().getParent()).toString().replace('\\', '.').replace('/', '.');
        String packageName = declaration.find() ? declaration.group(1) : "";
        if (!packageName.equals(expected) || packageName.isEmpty()) result.add("GOV-PACKAGE-PATH: expected " + expected);
        duplicateImports(source).forEach(value -> result.add("GOV-IMPORT-DUPLICATE: " + value));
        var references = forbidden.stream().filter(prefix -> AtfJavaSourceReferences.containsNamespace(source, prefix)).toList();
        return new Observation(packageName, List.copyOf(result), references);
    }

    public record Observation(String packageName, List<String> findings, List<String> forbiddenReferences) { }
}
