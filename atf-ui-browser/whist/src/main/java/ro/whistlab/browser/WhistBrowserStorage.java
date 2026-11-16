package ro.whistlab.browser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import ro.whistlab.app.achievements.WhistAchievementRepository;

final class WhistBrowserStorage implements WhistAchievementRepository.Storage {
    private final Path file;
    private String memory;

    WhistBrowserStorage(Path file) { this.file = file; }

    @Override public String read() throws IOException {
        if (file == null) return memory;
        return Files.notExists(file) ? null : Files.readString(file);
    }

    @Override public void write(String value) throws IOException {
        if (file == null) {
            memory = value;
            return;
        }
        Files.createDirectories(file.getParent());
        Path staging = Files.createTempFile(file.getParent(), "whist-", ".tmp");
        try {
            Files.writeString(staging, value);
            Files.move(staging, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(staging);
        }
    }
}
