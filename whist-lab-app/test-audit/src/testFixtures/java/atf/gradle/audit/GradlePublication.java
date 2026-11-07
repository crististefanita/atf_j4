package atf.gradle.audit;

/** Bounded JUnit 4 lifecycle adapter; all public Reporting port calls stay on their session owner. */
final class GradlePublication {
    private static volatile Thread owner;
    private static final java.util.concurrent.ExecutorService WORKER = java.util.concurrent.Executors.newSingleThreadExecutor(action -> {
        var thread = new Thread(action, "whist-gradle-report-owner"); thread.setDaemon(true); owner = thread; return thread;
    });
    private GradlePublication() { }
    static void run(Runnable action) {
        if (Thread.currentThread() == owner) { action.run(); return; }
        try { WORKER.submit(action).get(); }
        catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IllegalStateException("Interrupted report publication", error); }
        catch (java.util.concurrent.ExecutionException error) {
            if (error.getCause() instanceof Error original) throw original;
            if (error.getCause() instanceof RuntimeException original) throw original;
            throw new IllegalStateException("Report publication failed", error.getCause());
        }
    }
    static void shutdown() { WORKER.shutdown(); }
}
