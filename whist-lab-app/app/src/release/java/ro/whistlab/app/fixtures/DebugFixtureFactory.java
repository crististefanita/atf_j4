package ro.whistlab.app.fixtures;

import android.content.Intent;
import java.io.File;

/** Release implementation deliberately ignores debug-only launch requests. */
public final class DebugFixtureFactory {
    private DebugFixtureFactory() { }

    public static DebugLaunch fromIntent(Intent intent, File filesDirectory) throws java.io.IOException {
        return null;
    }
}