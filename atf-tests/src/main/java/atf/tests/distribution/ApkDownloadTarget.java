package atf.tests.distribution;

/** Immutable expected identity declared before accessing GitHub. */
public record ApkDownloadTarget(String repository, String version, long bytes, String sha256, boolean mainBranch) {
    public ApkDownloadTarget(String repository, String version, long bytes, String sha256) {
        this(repository, version, bytes, sha256, false);
    }
    public ApkDownloadTarget {
        if (repository == null || !repository.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")
                || version == null || !version.matches("(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)(?:-[0-9A-Za-z.-]+)?")
                || bytes <= 0 || sha256 == null || !sha256.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("Invalid declared APK download identity");
    }
    public String fileName() { return "WhistLab-" + version + ".apk"; }
    public String releasePageUrl() {
        String url = "https://github.com/" + repository;
        return mainBranch ? url + "/blob/main/release/" + version + "/README.md" : url + "/releases/tag/v" + version;
    }
    public String publicUrl() {
        if (mainBranch) return "https://raw.githubusercontent.com/" + repository + "/refs/heads/main/release/"
                + version + "/" + fileName();
        return "https://github.com/" + repository + "/releases/download/v" + version + "/" + fileName();
    }
}
