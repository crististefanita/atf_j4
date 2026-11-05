package atf.tests.reporting.replay;

import atf.reporting.model.ReportObservation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;

/** Citește sursele capturate și descrie artefactele dovezilor. */
public final class ReplayEvidenceFiles {
    private ReplayEvidenceFiles() {}

    public static Properties loadTrace(Path file) throws Exception {
        Properties properties = new Properties();
        try (var input = Files.newInputStream(file)) {
            properties.load(input);
        }
        return properties;
    }

    public static ReportObservation.Artifact artifactUnchecked(Path file) {
        try {
            return artifact(file);
        } catch (Exception failure) {
            throw new IllegalStateException("Could not attach report evidence " + file, failure);
        }
    }

    public static List<ReportObservation.Artifact> concatArtifacts(
            List<Path> first, List<Path> second) {
        List<Path> all = new ArrayList<>(first);
        all.addAll(second);
        return all.stream().map(ReplayEvidenceFiles::artifactUnchecked).toList();
    }

    public static List<Path> concatPaths(List<Path> first, List<Path> second) {
        List<Path> all = new ArrayList<>(first);
        all.addAll(second);
        return List.copyOf(all);
    }

    public static ReportObservation.Artifact artifact(Path file) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file));
        return new ReportObservation.Artifact(
                file.getFileName().toString(), file.toUri(), HexFormat.of().formatHex(digest));
    }
}
