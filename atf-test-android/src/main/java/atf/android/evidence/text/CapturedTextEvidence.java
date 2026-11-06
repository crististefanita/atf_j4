package atf.android.evidence.text;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;

/** One publication-time byte snapshot supplies both original fingerprint and complete UTF-8 body. */
public record CapturedTextEvidence(String name, String sha256, int bytes, String content) {
    public static CapturedTextEvidence read(Path file, String name) throws IOException {
        String filename = file.getFileName().toString();
        int dot = filename.lastIndexOf('.');
        String type = dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
        if (!Set.of("xml", "json", "properties", "txt", "log", "yaml", "yml").contains(type))
            throw new IOException("Unsupported supporting text evidence: " + file);
        byte[] data = Files.readAllBytes(file);
        String body = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(data)).toString();
        try {
            return new CapturedTextEvidence(name, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)), data.length, body);
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException("SHA-256 unavailable", impossible); }
    }
}
