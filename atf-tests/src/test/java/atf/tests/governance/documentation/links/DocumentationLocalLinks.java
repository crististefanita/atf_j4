package atf.tests.governance.documentation.links;

import atf.tests.governance.documentation.DocumentationGovernanceSupport;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** One local-link parser shared by README structure, the docs scan and explicit targets. */
public final class DocumentationLocalLinks {
    private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[[^\\]]+\\]\\(([^)]+)\\)");
    private static final Pattern DRIVE_PATH = Pattern.compile("^[A-Za-z]:");
    private DocumentationLocalLinks() { }

    public static void validate(Path path, Path root, List<String> lines,
                                           List<DocumentationGovernanceSupport.Violation> violations) {
        Path checkout = root.toAbsolutePath().normalize();
        lines = MarkdownAnchorIndex.withoutFences(lines);
        for (int index = 0; index < lines.size(); index++) {
            Matcher matcher = MARKDOWN_LINK.matcher(lines.get(index));
            while (matcher.find()) {
                if (matcher.start() > 0 && lines.get(index).charAt(matcher.start() - 1) == '!') continue;
                String destination = matcher.group(1).trim();
                String target = destination.replaceFirst("^<", "").replaceFirst(">$", "");
                if (isExternalOrConfigured(target)) continue;
                int fragment = target.indexOf('#');
                String anchor = fragment < 0 ? "" : target.substring(fragment + 1);
                String file = fragment < 0 ? target : target.substring(0, fragment);
                Path resolved;
                try {
                    String local = decode(file);
                    if (isMachinePath(local)) {
                        nonPortable(violations, root, path, index + 1, destination);
                        continue;
                    }
                    resolved = (file.isBlank() ? path : path.getParent().resolve(local)).toAbsolutePath().normalize();
                    if (!resolved.startsWith(checkout)) {
                        nonPortable(violations, root, path, index + 1, destination);
                        continue;
                    }
                    anchor = decode(anchor);
                } catch (IllegalArgumentException invalid) {
                    violations.add(DocumentationGovernanceSupport.violation(
                            "DOC-README-LINK", root, path, index + 1,
                            "local link has an invalid path or percent escape: " + destination,
                            "local link destinations must be valid URI paths and fragments",
                            "correct the path or encode the literal percent sign as %25"));
                    continue;
                }
                if (!Files.exists(resolved)) {
                    violations.add(DocumentationGovernanceSupport.violation(
                            "DOC-README-LINK", root, path, index + 1,
                            "local link points to missing target: " + destination,
                            "owner README links must resolve from the file that declares them",
                            "correct the relative path, or remove the link if the target is no longer an owner"));
                } else {
                    try {
                        if (!resolved.toRealPath().startsWith(checkout.toRealPath())) {
                            nonPortable(violations, root, path, index + 1, destination);
                            continue;
                        }
                        if (!anchor.isEmpty() && resolved.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".md")
                                && !MarkdownAnchorIndex.anchors(Files.readAllLines(resolved)).contains(anchor)) {
                            violations.add(DocumentationGovernanceSupport.violation(
                                    "DOC-README-ANCHOR", root, path, index + 1,
                                    "local link points to missing Markdown fragment: " + destination,
                                    "the destination heading or explicit HTML anchor must exist outside fenced examples",
                                    "correct the fragment, restore the destination heading, or add its stable HTML anchor"));
                        }
                    } catch (java.io.IOException unreadable) {
                        violations.add(DocumentationGovernanceSupport.violation(
                                "DOC-README-LINK", root, path, index + 1,
                                "local link target could not be read: " + destination,
                                "local paths and fragments must be readable inside the checkout",
                                "fix the target encoding or filesystem access"));
                    }
                }
            }
        }
    }

    private static void nonPortable(List<DocumentationGovernanceSupport.Violation> violations,
                                    Path root, Path source, int line, String destination) {
        violations.add(DocumentationGovernanceSupport.violation(
                "DOC-LINK-PORTABILITY", root, source, line,
                "local destination depends on this machine or another checkout: " + destination,
                "use a relative path inside the checkout or a versioned remote reference",
                "move the local contract into the checkout or link to the external owner by URL"));
    }

    private static boolean isMachinePath(String path) {
        return DRIVE_PATH.matcher(path).find() || path.startsWith("/") || path.startsWith("\\")
                || path.regionMatches(true, 0, "file:", 0, 5);
    }

    private static String decode(String text) {
        return java.net.URLDecoder.decode(text.replace("+", "%2B"), java.nio.charset.StandardCharsets.UTF_8);
    }

    private static boolean isExternalOrConfigured(String destination) {
        String normalized = destination.toLowerCase(java.util.Locale.ROOT);
        return normalized.startsWith("http://")
                || normalized.startsWith("https://")
                || normalized.startsWith("mailto:")
                || destination.contains("${")
                || destination.contains("<configured");
    }
}
