package atf.android.evidence.component;

import atf.reporting.configuration.AtfReportConfigurationLoader;
import java.nio.file.Path;
import java.util.UUID;

/** Consumer metadata only; Kit owns JUnit capture and Reporting owns the shared session. */
public final class AndroidComponentReports {
    public static final String DIRECTORY = "atf.android.component.report-directory";
    public static final String RENDERER = "atf.android.component.renderer";
    public static final String PROVENANCE = "atf.component.execution-provenance";
    public static final String INPUT_PART_CHARACTERS = "atf.component.input-part-characters";
    private AndroidComponentReports() { }

    public static AndroidComponentRun open() {
        Path directory = Path.of(System.getProperty(DIRECTORY,
                Path.of(System.getProperty("java.io.tmpdir"), "whist-component-reports").toString()));
        if (!directory.isAbsolute()) throw new IllegalArgumentException(DIRECTORY + " must be absolute");
        String renderer = System.getProperty(RENDERER, "extent");
        if (!renderer.equals("extent") && !renderer.equals("fallback"))
            throw new IllegalArgumentException(RENDERER + " must be extent or fallback");
        var settings = inputSettings(AtfReportConfigurationLoader.load().reportingSettings(), System.getProperty(INPUT_PART_CHARACTERS));
        Path file = directory.resolve("component-whist-" + renderer + "-" + UUID.randomUUID() + ".html");
        return new AndroidComponentRun(file, renderer.equals("fallback"), settings);
    }
    static atf.reporting.configuration.ReportingSettings inputSettings(atf.reporting.configuration.ReportingSettings base, String configured) {
        if (configured == null) return base;
        int characters = Integer.parseInt(configured);
        if (characters < 2) throw new IllegalArgumentException(INPUT_PART_CHARACTERS + " must preserve a complete UTF-16 pair (at least 2)");
        return new atf.reporting.configuration.ReportingSettings(base.assertionMaxDisplayCharacters(), characters);
    }
}
