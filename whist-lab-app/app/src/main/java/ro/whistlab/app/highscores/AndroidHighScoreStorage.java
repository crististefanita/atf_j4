package ro.whistlab.app.highscores;

import android.content.Context;
import android.util.AtomicFile;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** highestScores.json belongs to the app's private local files, with Android atomic-write recovery. */
public final class AndroidHighScoreStorage implements HighScoreRepository.Storage {
    private final AtomicFile file;
    public AndroidHighScoreStorage(Context context) {
        file = new AtomicFile(new File(context.getFilesDir(), "highestScores.json"));
    }
    @Override public String read() throws IOException {
        try { return new String(file.readFully(), StandardCharsets.UTF_8); }
        catch (FileNotFoundException absent) {
            if (!file.getBaseFile().exists() && !new File(file.getBaseFile() + ".bak").exists()) return null;
            throw absent;
        }
    }
    @Override public void write(String json) throws IOException {
        FileOutputStream stream = null;
        try {
            stream = file.startWrite();
            stream.write(json.getBytes(StandardCharsets.UTF_8));
            file.finishWrite(stream);
        } catch (IOException failure) {
            if (stream != null) file.failWrite(stream);
            throw failure;
        }
    }
}
