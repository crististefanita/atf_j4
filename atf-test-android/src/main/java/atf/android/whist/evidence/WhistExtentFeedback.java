package atf.android.whist.evidence;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

/** Publishes a generated execution index outside source checkouts. */
public final class WhistExtentFeedback {
    private WhistExtentFeedback() { }

    public static void afterAudit(Path report, String appPackage, String scenario, String status) throws IOException {
        if (!"ro.whistlab.app".equals(appPackage) || !Boolean.getBoolean("atf.android.enabled")) return;
        Path output = report.toAbsolutePath().normalize();
        boolean inRun = Stream.of("atf.android.artifacts", "atf.match.evidenceDir")
                .map(System::getProperty).filter(value -> value != null && !value.isBlank())
                .map(value -> Path.of(value).toAbsolutePath().normalize()).anyMatch(output::startsWith);
        if (!inRun) return;

        String configured = System.getProperty("atf.android.feedbackDirectory", System.getenv("ATF_J4_FEEDBACK_DIRECTORY"));
        Path indexRoot = configured != null && !configured.isBlank()
                ? Path.of(configured) : externalArchive().resolve("indexes");
        publish(indexRoot, output, scenario, status);
    }

    private static Path externalArchive() {
        String configured = System.getProperty("atf.android.handoffRoot", System.getenv("ATF_J4_UI_REPORTS"));
        return Path.of(configured == null || configured.isBlank()
                ? "C:/work/whist_back/ui-reports" : configured).toAbsolutePath().normalize().resolve("whist-lab");
    }

    private static void requireExternalDestination(Path destination) throws IOException {
        Path absolute = destination.toAbsolutePath().normalize();
        Path existing = absolute;
        while (existing != null && !Files.exists(existing)) existing = existing.getParent();
        if (existing != null) absolute = existing.toRealPath().resolve(existing.relativize(absolute));
        for (Path root = absolute; root != null; root = root.getParent()) {
            if (Files.exists(root.resolve(".git"))
                    || (Files.isRegularFile(root.resolve("pom.xml")) && Files.isDirectory(root.resolve("whist-lab-app")))) {
                throw new IOException("Generated run indexes must be outside source checkouts: " + destination);
            }
        }
    }

    private static String link(Path from, Path target, String label) throws IOException {
        Path directory = from.toAbsolutePath().normalize();
        Path file = target.toAbsolutePath().normalize();
        String encoded;
        try {
            if (directory.getRoot().equals(file.getRoot())) {
                String relative = directory.relativize(file).toString().replace(java.io.File.separatorChar, '/');
                encoded = new java.net.URI(null, null, relative, null).toASCIIString();
            } else {
                encoded = file.toUri().toASCIIString();
            }
        } catch (java.net.URISyntaxException invalidPath) {
            throw new IOException("Invalid report link", invalidPath);
        }
        return "[" + label + "](<" + encoded.replace("(", "%28").replace(")", "%29") + ">)";
    }

    static void publish(Path feedback, Path report, String scenario, String status) throws IOException {
        requireExternalDestination(feedback);
        Path html = report.toRealPath();
        var modified = Files.getLastModifiedTime(html).toInstant();
        Path folder = Files.createDirectories(feedback.resolve("whist-extent"));
        Path readme = folder.resolve("README.md");
        String title = scenario.replaceAll("[\\p{Cntrl}]+", " ").strip();
        Path archive = externalArchive();
        String markdown = "# Ultimul raport Extent Whist Lab\n\n## Pe scurt\n\n"
                + link(folder, archive.resolve("latest-accepted.html"), "Deschide direct ultimul Extent acceptat") + "\n\n"
                + link(folder, archive.resolve("LATEST.md"), "Ultima acceptanță și ultima încercare arhivate") + "\n\n"
                + link(folder, html, "Deschide ultimul raport de execuție") + "\n\n"
                + "**Scenariu** · " + title + "\n\n**Verdict** · " + status + "\n\n"
                + "**Raport scris la** · " + modified + " (UTC)\n\n"
                + "Indexul se actualizează după auditul raportului. "
                + "Un scenariu eșuat cu diagnostic valid poate fi cel mai recent. Arhiva stabilă păstrează HTML-ul și trace-ul după verificarea offline.\n\n"
                + "Acesta este un index generat al execuțiilor, păstrat în arhiva externă.\n";
        Path temp = Files.createTempFile(folder, ".whist-extent-", ".tmp");
        try {
            Files.writeString(temp, markdown);
            try {
                Files.move(temp, readme, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temp, readme, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
