package atf.tests.documentation.policy;

import atf.tests.distribution.ApkDownloadTarget;
import atf.tests.distribution.ReleaseDownloadSelection;
import atf.tests.distribution.ReleasePaths;
import atf.tests.testchange.TestChangeControls;
import atf.tests.documentation.DocumentationGovernanceSupport.Violation;
import atf.tests.documentation.DocumentationInventory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static atf.tests.documentation.DocumentationGovernanceSupport.violation;

/** Content conventions that depend on the project's declared publication and stable documentation. */
public final class DocumentationContentPolicy {
    private static final String CONFIG = "config/atf-governance-policy.json";
    private static final Pattern HISTORICAL_VERIFICATION = Pattern.compile("ATF_Reporting_J[0-9]+_Verification\\.md");
    private static final Pattern PROJECT_IDENTIFIER = Pattern.compile("(?i)\\b(?:atf[_-]j[0-9]+|J[0-9]+)\\b");

    public record Context(String repository, ApkDownloadTarget advertised, String language) {
        public static Context load(Path root) throws IOException {
            ObjectMapper mapper = new ObjectMapper();
            var policy = mapper.readTree(root.resolve(CONFIG).toFile());
            String language = documentationLanguage(policy);
            String repository = policy.path("publicRepository").asText("");
            if (repository.isEmpty()) return new Context("", null, language);
            if (!repository.matches("https://github\\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+"))
                throw new IOException("DOC-CONTENT-CONFIG: repository must be a GitHub URL");
            var index = mapper.readTree(root.resolve("release/index.json").toFile());
            String version = ReleasePaths.relative(index.path("candidateVersion").asText(index.path("currentVersion").asText()));
            Path manifest = index.path("candidateVersion").isTextual() ? root.resolve("release/candidate.json")
                    : root.resolve("release").resolve(version).resolve("release-validation.json");
            return new Context(repository, ReleaseDownloadSelection.select("advertised",
                    repository.substring("https://github.com/".length()), index, mapper.readTree(manifest.toFile())), language);
        }
    }

    public static String documentationLanguage(Path root) throws IOException {
        Path policy = root.resolve(CONFIG);
        return Files.exists(policy) ? documentationLanguage(new ObjectMapper().readTree(policy.toFile())) : "ro";
    }

    private static String documentationLanguage(JsonNode policy) throws IOException {
        if (policy == null || !policy.isObject()) throw new IOException("DOC-CONTENT-CONFIG: expected object policy");
        JsonNode language = policy.get("documentationLanguage");
        if (language == null) return "ro";
        if (!language.isTextual() || !List.of("en", "ro").contains(language.textValue()))
            throw new IOException("DOC-CONTENT-CONFIG: invalid documentation language");
        return language.textValue();
    }

    public static void validate(Path file, Path root, List<String> lines, Context context, List<Violation> violations) throws IOException {
        if (HISTORICAL_VERIFICATION.matcher(file.getFileName().toString()).matches())
            violations.add(violation("DOC-RELEASE-VERIFICATION-NAME", root, file, 1,
                    "file name captures an individual release verification",
                    "release chronology belongs in the external archive", "move the verification beside the release evidence"));
        String source = String.join("\n", lines);
        boolean english = context.language().equals("en");
        boolean ownerGuide = DocumentationInventory.isReadme(file) || DocumentationInventory.isLibraryGuide(root, file);
        int foreignLine = DocumentationLanguageRules.firstForeignLine(source, context.language());
        if (foreignLine > 0) {
            String required = english ? "English" : "Romanian";
            violations.add(violation(ownerGuide ? "DOC-README-LANGUAGE" : "DOC-DOCUMENT-LANGUAGE", root, file, foreignLine,
                    "document contains prose outside " + required,
                    "all Markdown documents follow the configured branch language",
                    "translate the explanations and keep API names intact"));
        }
        String relative = root.relativize(file).toString().replace('\\', '/');
        String prose = historicalReleaseProse(relative, publicReleaseProse(deploymentProse(source), context.repository(), context.advertised()),
                context.repository());
        if (PROJECT_IDENTIFIER.matcher(prose).find())
            violations.add(violation("DOC-PROJECT-IDENTIFIER", root, file, 1, "prose contains an external project identifier",
                    "documentation refers to local contracts", "name the owning contract and link to its stable documentation"));
    }

    /** Stable publication URIs of the declared project are technical data. Link labels remain prose. */
    public static String publicReleaseProse(String source, String repository) {
        return publicReleaseProse(source, repository, null);
    }

    public static String publicReleaseProse(String source, String repository, atf.tests.distribution.ApkDownloadTarget published) {
        if (published != null && !repository.equals("https://github.com/" + published.repository()))
            throw new IllegalArgumentException("Published technical data must belong to the declared repository");
        if (repository.isEmpty()) return source;
        if (!repository.matches("https://github\\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")
                || !java.net.URI.create(repository).normalize().toString().equals(repository))
            throw new IllegalArgumentException("Expected a canonical public GitHub repository URI");
        var path = java.net.URI.create(repository).getPath().substring(1).split("/");
        String pages = "https://" + path[0].toLowerCase(java.util.Locale.ROOT) + ".github.io/" + path[1] + "/";
        var links = java.util.regex.Pattern.compile("(?<!!)\\[([^\\]\\r\\n]*)\\]\\(([^)\\s]+)\\)").matcher(source);
        var result = new StringBuffer();
        while (links.find()) {
            String replacement = (links.group(2).equals(repository + "/releases/latest")
                    || links.group(2).equals(pages)
                    || published != null && (links.group(2).equals(published.releasePageUrl())
                            || links.group(2).equals(published.publicUrl())
                            || links.group(2).equals(published.publicUrl() + ".sha256")))
                    ? "[" + links.group(1) + "](<resursă-publică>)" : links.group();
            links.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        return links.appendTail(result).toString();
    }

    /** Historical release READMEs may link to their own tagged asset; visible labels remain governed prose. */
    public static String historicalReleaseProse(String relative, String source, String repository) {
        var archive = java.util.regex.Pattern.compile(
                "^release/([0-9]+\\.[0-9]+(?:\\.[0-9]+)?)/README\\.md$").matcher(relative.replace('\\', '/'));
        if (!archive.matches() || repository.isEmpty()) return source;
        String version = archive.group(1);
        String page = repository + "/releases/tag/v" + version;
        String apk = repository + "/releases/download/v" + version + "/WhistLab-" + version + ".apk";
        String checksum = apk + ".sha256";
        var links = java.util.regex.Pattern.compile("(?<!!)\\[([^\\]\\r\\n]*)\\]\\(([^)\\s]+)\\)").matcher(source);
        var result = new StringBuffer();
        while (links.find()) {
            String replacement = links.group(2).equals(repository)
                    || links.group(2).equals(page) || links.group(2).equals(apk) || links.group(2).equals(checksum)
                    ? "[" + links.group(1) + "](<resursă-publică>)" : links.group();
            links.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        return links.appendTail(result).toString();
    }

    /** Only the identity of a well-formed change control is technical data, not editorial prose. */
    public static String deploymentProse(String source) {
        return TestChangeControls.editorialProse(source);
    }


    public static boolean hasEnglishReadmeProse(String source) {
        return DocumentationLanguageRules.firstForeignLine(source, "ro") > 0;
    }

    public static boolean hasRomanianReadmeProse(String source) {
        return DocumentationLanguageRules.firstForeignLine(source, "en") > 0;
    }

    private DocumentationContentPolicy() { }
}
