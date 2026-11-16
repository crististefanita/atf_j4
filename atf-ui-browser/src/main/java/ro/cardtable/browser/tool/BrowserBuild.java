package ro.cardtable.browser.tool;

import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Compiles a game's adapter against the packaged runtime using a local JDK. */
public final class BrowserBuild {
    private BrowserBuild() { }

    public static void main(String[] args) throws IOException {
        if (args.length < 3) throw new IllegalArgumentException("Expected work directory, checkout and Java inputs");
        Path checkout = Path.of(args[1]).toRealPath();
        Path work = Path.of(args[0]).toAbsolutePath().normalize();
        Path existing = work;
        while (!Files.exists(existing)) existing = existing.getParent();
        if (existing.toRealPath().resolve(existing.relativize(work)).startsWith(checkout)) {
            throw new IllegalArgumentException("Use an external browser work directory");
        }
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("A JDK is required");
        var sources = new TreeSet<String>();
        for (int i = 2; i < args.length; i++) {
            var input = Path.of(args[i]).toRealPath();
            try (var paths = Files.walk(input)) {
                paths.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".java"))
                        .map(Path::toString).forEach(sources::add);
            }
        }
        if (sources.isEmpty()) throw new IllegalArgumentException("No Java sources found");
        Path classes = Files.createTempDirectory(Files.createDirectories(work), "browser-");
        String classpath = System.getProperty("java.class.path");
        var arguments = new ArrayList<>(List.of("--release", "17", "-encoding", "UTF-8",
                "-cp", classpath, "-d", classes.toString()));
        arguments.addAll(sources);
        if (compiler.run(null, System.err, System.err, arguments.toArray(String[]::new)) != 0) {
            throw new IllegalStateException("Browser adapter compilation failed");
        }
        System.out.println(classes + File.pathSeparator + classpath);
    }
}
